package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.nonNull;

import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.config.EvidenceShareConfig;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.factory.DocumentRequestFactory;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Component
@Slf4j
public class EvidenceShareService {

    private static final String DM_STORE_USER_ID = "sscs";

    private final SscsCaseCallbackDeserializer sscsCaseCallbackDeserializer;

    private final DocumentManagementServiceWrapper documentManagementServiceWrapper;

    private final DocumentRequestFactory documentRequestFactory;

    private final EvidenceManagementService evidenceManagementService;

    private final BulkPrintService bulkPrintService;

    private final EvidenceShareConfig evidenceShareConfig;

    private final CcdService ccdService;

    private final IdamService idamService;

    private final RoboticsHandler roboticsHandler;

    @Autowired
    public EvidenceShareService(
        SscsCaseCallbackDeserializer sscsDeserializer,
        DocumentManagementServiceWrapper documentManagementServiceWrapper,
        DocumentRequestFactory documentRequestFactory,
        EvidenceManagementService evidenceManagementService,
        BulkPrintService bulkPrintService,
        EvidenceShareConfig evidenceShareConfig,
        CcdService ccdService,
        IdamService idamService,
        RoboticsHandler roboticsHandler
    ) {
        this.sscsCaseCallbackDeserializer = sscsDeserializer;
        this.documentManagementServiceWrapper = documentManagementServiceWrapper;
        this.documentRequestFactory = documentRequestFactory;
        this.evidenceManagementService = evidenceManagementService;
        this.bulkPrintService = bulkPrintService;
        this.evidenceShareConfig = evidenceShareConfig;
        this.ccdService = ccdService;
        this.idamService = idamService;
        this.roboticsHandler = roboticsHandler;
    }

    public Optional<UUID> processMessage(final String message) {
        Callback<SscsCaseData> sscsCaseDataCallback = sscsCaseCallbackDeserializer.deserialize(message);

        roboticsHandler.sendCaseToRobotics(sscsCaseDataCallback.getCaseDetails().getCaseData());

        if (readyToSendToDwp(sscsCaseDataCallback.getCaseDetails().getCaseData())) {

            log.info("Processing callback event {} for case id {}", sscsCaseDataCallback.getEvent(),
                sscsCaseDataCallback.getCaseDetails().getId());

            DocumentHolder holder = documentRequestFactory.create(
                sscsCaseDataCallback.getCaseDetails().getCaseData(),
                sscsCaseDataCallback.getCaseDetails().getCreatedDate());

            final SscsCaseData caseData = sscsCaseDataCallback.getCaseDetails().getCaseData();

            if (holder.getTemplate() != null) {
                log.info("Generating document for case id {}", sscsCaseDataCallback.getCaseDetails().getId());

                documentManagementServiceWrapper.generateDocumentAndAddToCcd(holder, caseData);
                List<Pdf> existingCasePdfs = toPdf(caseData.getSscsDocument());

                Optional<UUID> uuid = bulkPrintService.sendToBulkPrint(existingCasePdfs, caseData);

                caseData.setDateSentToDwp(LocalDate.now().toString());
                String description = buildEventDescription(existingCasePdfs);
                ccdService.updateCase(caseData, Long.valueOf(caseData.getCcdCaseId()), EventType.SENT_TO_DWP.getCcdType(), "Sent to DWP", description, idamService.getIdamTokens());

                return uuid;
            }
        } else {
            log.warn("case id {} is not ready to send to dwp", sscsCaseDataCallback.getCaseDetails().getId());
        }
        return Optional.empty();
    }

    private String buildEventDescription(List<Pdf> pdfs) {
        List<String> arr = new ArrayList<>();

        for (Pdf pdf : pdfs) {
            arr.add(pdf.getName());
        }

        return "Case has been sent to the DWP with documents: " + String.join(", ", arr);
    }

    private boolean readyToSendToDwp(SscsCaseData caseData) {
        return nonNull(caseData)
            && nonNull(caseData.getAppeal())
            && nonNull(caseData.getAppeal().getReceivedVia())
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
