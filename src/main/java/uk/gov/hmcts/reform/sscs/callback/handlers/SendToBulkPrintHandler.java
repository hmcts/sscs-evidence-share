package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.config.EvidenceShareConfig;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.exception.BulkPrintException;
import uk.gov.hmcts.reform.sscs.factory.DocumentRequestFactory;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.BulkPrintInfo;
import uk.gov.hmcts.reform.sscs.service.BulkPrintService;
import uk.gov.hmcts.reform.sscs.service.DocumentManagementServiceWrapper;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;

@Component
@Slf4j

@Service
public class SendToBulkPrintHandler implements CallbackHandler<SscsCaseData> {

    private static final String DM_STORE_USER_ID = "sscs";
    private static final String SENT_TO_DWP = "Sent to DWP";

    private final DocumentManagementServiceWrapper documentManagementServiceWrapper;

    private final DocumentRequestFactory documentRequestFactory;

    private final EvidenceManagementService evidenceManagementService;

    private final BulkPrintService bulkPrintService;

    private final EvidenceShareConfig evidenceShareConfig;

    private final CcdService ccdService;

    private final IdamService idamService;

    @Value("${send-letter.enabled}")
    private Boolean bulkPrintFeature;

    @Autowired
    public SendToBulkPrintHandler(
        DocumentManagementServiceWrapper documentManagementServiceWrapper,
        DocumentRequestFactory documentRequestFactory,
        EvidenceManagementService evidenceManagementService,
        BulkPrintService bulkPrintService,
        EvidenceShareConfig evidenceShareConfig,
        CcdService ccdService,
        IdamService idamService
    ) {
        this.documentManagementServiceWrapper = documentManagementServiceWrapper;
        this.documentRequestFactory = documentRequestFactory;
        this.evidenceManagementService = evidenceManagementService;
        this.bulkPrintService = bulkPrintService;
        this.evidenceShareConfig = evidenceShareConfig;
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback, DispatchPriority priority) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && priority == DispatchPriority.LATEST
            && (callback.getEvent() == EventType.SEND_TO_DWP
            || callback.getEvent() == EventType.VALID_APPEAL
            || callback.getEvent() == EventType.INTERLOC_VALID_APPEAL);
    }

    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback, DispatchPriority priority) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        BulkPrintInfo bulkPrintInfo = null;

        log.info("Processing event {} for case id {} in evidence share service", callback.getEvent(),
            callback.getCaseDetails().getId());

        try {
            bulkPrintInfo = bulkPrintCase(callback);
        } catch (Exception e) {
            log.info("Error when bulk-printing caseId: {}", callback.getCaseDetails().getId(), e);
            updateCaseToFlagError(caseData);
        }
        updateCaseToSentToDwp(callback, caseData, bulkPrintInfo);
    }

    private void updateCaseToFlagError(SscsCaseData caseData) {
        caseData.setHmctsDwpState("failedSending");
        ccdService.updateCase(caseData,
            Long.valueOf(caseData.getCcdCaseId()),
            EventType.SENT_TO_DWP_ERROR.getCcdType(),
            "Send to DWP Error",
            "Send to DWP Error event has been triggered from Evidence Share service",
            idamService.getIdamTokens());
    }

    private void updateCaseToSentToDwp(Callback<SscsCaseData> sscsCaseDataCallback, SscsCaseData caseData,
                                       BulkPrintInfo bulkPrintInfo) {
        if (bulkPrintInfo != null) {
            if (bulkPrintInfo.isAllowedTypeForBulkPrint()) {
                ccdService.updateCase(caseData, Long.valueOf(caseData.getCcdCaseId()),
                    EventType.SENT_TO_DWP.getCcdType(), SENT_TO_DWP, bulkPrintInfo.getDesc(),
                    idamService.getIdamTokens());
                log.info("Case sent to dwp for case id {} with returned value {}",
                    sscsCaseDataCallback.getCaseDetails().getId(), bulkPrintInfo.getUuid());
            } else {
                log.info("Skipping bulk print. Sending straight to {} state for case id {}",
                    EventType.SENT_TO_DWP.getCcdType(), sscsCaseDataCallback.getCaseDetails().getId());
                ccdService.updateCase(caseData, Long.valueOf(caseData.getCcdCaseId()),
                    EventType.SENT_TO_DWP.getCcdType(), SENT_TO_DWP, bulkPrintInfo.getDesc(),
                    idamService.getIdamTokens());
            }
        }
    }

    private BulkPrintInfo bulkPrintCase(Callback<SscsCaseData> sscsCaseDataCallback) {
        SscsCaseData caseData = sscsCaseDataCallback.getCaseDetails().getCaseData();
        if (isAllowedReceivedTypeForBulkPrint(sscsCaseDataCallback.getCaseDetails().getCaseData())) {

            log.info("Processing bulk print tasks for case id {}", sscsCaseDataCallback.getCaseDetails().getId());

            DocumentHolder holder = documentRequestFactory.create(sscsCaseDataCallback.getCaseDetails().getCaseData(),
                sscsCaseDataCallback.getCaseDetails().getCreatedDate());

            if (holder.getTemplate() != null) {
                log.info("Generating DL document for case id {}", sscsCaseDataCallback.getCaseDetails().getId());

                IdamTokens idamTokens = idamService.getIdamTokens();
                documentManagementServiceWrapper.generateDocumentAndAddToCcd(holder, caseData, idamTokens);
                List<Pdf> existingCasePdfs = toPdf(caseData.getSscsDocument());

                log.info("Sending to bulk print for case id {}", sscsCaseDataCallback.getCaseDetails().getId());
                caseData.setDateSentToDwp(LocalDate.now().toString());
                return BulkPrintInfo.builder()
                    .uuid(bulkPrintService.sendToBulkPrint(existingCasePdfs, caseData).orElse(null))
                    .allowedTypeForBulkPrint(true)
                    .desc(buildEventDescription(existingCasePdfs))
                    .build();
            }
            throw new BulkPrintException(
                format("Failed to send to bulk print for case %s because no template was found",
                    caseData.getCcdCaseId()));
        } else {
            log.info("Case not valid to send to bulk print for case id {}", sscsCaseDataCallback.getCaseDetails().getId());

            return BulkPrintInfo.builder()
                .uuid(null)
                .allowedTypeForBulkPrint(false)
                .desc("Case state is now sent to DWP")
                .build();
        }
    }

    private String buildEventDescription(List<Pdf> pdfs) {
        List<String> arr = new ArrayList<>();

        for (Pdf pdf : pdfs) {
            arr.add(pdf.getName());
        }

        return "Case has been sent to the DWP via Bulk Print with documents: " + String.join(", ", arr);
    }

    private boolean isAllowedReceivedTypeForBulkPrint(SscsCaseData caseData) {
        return nonNull(caseData)
            && nonNull(caseData.getAppeal())
            && nonNull(caseData.getAppeal().getReceivedVia())
            && bulkPrintFeature
            && evidenceShareConfig.getSubmitTypes().stream()
            .anyMatch(caseData.getAppeal().getReceivedVia()::equalsIgnoreCase);
    }

    private List<Pdf> toPdf(List<SscsDocument> sscsDocument) {
        if (sscsDocument == null) {
            return Collections.emptyList();
        }

        Supplier<Stream<SscsDocument>> sscsDocumentStream = () -> sscsDocument.stream()
            .filter(doc -> nonNull(doc)
                && nonNull(doc.getValue())
                && nonNull(doc.getValue().getDocumentFileName())
                && nonNull(doc.getValue().getDocumentType())
                && nonNull(doc.getValue().getDocumentLink())
                && nonNull(doc.getValue().getDocumentLink().getDocumentUrl())
            );

        Stream<SscsDocument> allDocs = buildStreamOfDocuments(sscsDocumentStream);

        return allDocs.flatMap(doc -> StringUtils.containsIgnoreCase(doc.getValue().getDocumentFileName(), "pdf")
            ? Stream.of(new Pdf(toBytes(doc), doc.getValue().getDocumentFileName()))
            : Stream.empty()
        ).collect(Collectors.toList());
    }

    private Stream<SscsDocument> buildStreamOfDocuments(Supplier<Stream<SscsDocument>> sscsDocumentStream) {
        Stream<SscsDocument> dlDocs = sscsDocumentStream.get().filter(doc -> doc.getValue().getDocumentType().equals("dl6") || doc.getValue().getDocumentType().equals("dl16"));

        Stream<SscsDocument> appealDocs = sscsDocumentStream.get().filter(doc -> doc.getValue().getDocumentType().equals("sscs1"));

        Stream<SscsDocument> allOtherDocs = sscsDocumentStream.get().filter(doc -> !doc.getValue().getDocumentType().equals("dl6")
            && !doc.getValue().getDocumentType().equals("dl16")
            && !doc.getValue().getDocumentType().equals("sscs1"));

        return Stream.concat(Stream.concat(dlDocs, appealDocs), allOtherDocs);
    }

    private byte[] toBytes(SscsDocument sscsDocument) {
        return evidenceManagementService.download(
            URI.create(sscsDocument.getValue().getDocumentLink().getDocumentUrl()),
            DM_STORE_USER_ID
        );
    }
}
