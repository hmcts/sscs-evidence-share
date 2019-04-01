package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.nonNull;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.config.EvidenceShareConfig;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocumentManagementService;
import uk.gov.hmcts.reform.sscs.factory.DocumentRequestFactory;

@Component
@Slf4j
public class EvidenceShareService {

    private static final String DM_STORE_USER_ID = "sscs";
    public static final String PAPER = "Paper";

    private final SscsCaseCallbackDeserializer sscsCaseCallbackDeserializer;

    private final DocumentManagementService documentManagementService;

    private final DocumentRequestFactory documentRequestFactory;

    private final EvidenceManagementService evidenceManagementService;

    private final BulkPrintService bulkPrintService;

    private final EvidenceShareConfig evidenceShareConfig;

    @Autowired
    public EvidenceShareService(
        SscsCaseCallbackDeserializer sscsDeserializer,
        DocumentManagementService documentManagementService,
        DocumentRequestFactory documentRequestFactory,
        EvidenceManagementService evidenceManagementService,
        BulkPrintService bulkPrintService,
        EvidenceShareConfig evidenceShareConfig
    ) {

        this.sscsCaseCallbackDeserializer = sscsDeserializer;
        this.documentManagementService = documentManagementService;
        this.documentRequestFactory = documentRequestFactory;
        this.evidenceManagementService = evidenceManagementService;
        this.bulkPrintService = bulkPrintService;
        this.evidenceShareConfig = evidenceShareConfig;
    }

    public Optional<UUID> processMessage(final String message) {
        Callback<SscsCaseData> sscsCaseDataCallback = sscsCaseCallbackDeserializer.deserialize(message);

        if (readyToSendToDwp(sscsCaseDataCallback.getCaseDetails().getCaseData())) {

            log.info("Processing callback event {} for case id {}", sscsCaseDataCallback.getEvent(),
                sscsCaseDataCallback.getCaseDetails().getId());

            DocumentHolder holder = documentRequestFactory.create(
                sscsCaseDataCallback.getCaseDetails().getCaseData(),
                sscsCaseDataCallback.getCaseDetails().getCreatedDate());

            final SscsCaseData caseData = sscsCaseDataCallback.getCaseDetails().getCaseData();

            if (holder.getTemplate() != null) {
                Pdf newPdfAddedToTheCase = documentManagementService.generateDocumentAndAddToCcd(holder, caseData);
                List<Pdf> existingCasePdfs = toPdf(caseData.getSscsDocument());
                return bulkPrintService.sendToBulkPrint(getAllCasePdfs(newPdfAddedToTheCase, existingCasePdfs), caseData);
            }
        }
        return Optional.empty();
    }

    private boolean readyToSendToDwp(SscsCaseData caseData) {
        return nonNull(caseData)
            && nonNull(caseData.getAppeal())
            && nonNull(caseData.getAppeal().getBenefitType())
            && nonNull(caseData.getAppeal().getBenefitType().getCode())
            && nonNull(caseData.getAppeal().getReceivedVia())
            && evidenceShareConfig.getSubmitTypes().stream()
                .anyMatch(caseData.getAppeal().getReceivedVia()::equalsIgnoreCase)
            && evidenceShareConfig.getAllowedBenefitTypes().stream()
                .anyMatch(caseData.getAppeal().getBenefitType().getCode()::equalsIgnoreCase);
    }

    private List<Pdf> getAllCasePdfs(Pdf newPdfAddedToTheCase, List<Pdf> existingCasePdfs) {
        List<Pdf> allPdfs = new ArrayList<>(existingCasePdfs);
        allPdfs.add(0, newPdfAddedToTheCase);
        return allPdfs;
    }

    private List<Pdf> toPdf(List<SscsDocument> sscsDocument) {
        if (sscsDocument == null) {
            return Collections.emptyList();
        }
        return sscsDocument.stream()
            .filter(doc -> nonNull(doc)
                && nonNull(doc.getValue())
                && nonNull(doc.getValue().getDocumentFileName())
                && nonNull(doc.getValue().getDocumentLink())
                && nonNull(doc.getValue().getDocumentLink().getDocumentUrl())
            )
            .flatMap(doc -> StringUtils.containsIgnoreCase(doc.getValue().getDocumentFileName(), "pdf")
                ? Stream.of(new Pdf(toBytes(doc), doc.getValue().getDocumentFileName()))
                : Stream.empty()
        ).collect(Collectors.toList());
    }

    private byte[] toBytes(SscsDocument sscsDocument) {
        return evidenceManagementService.download(
            URI.create(sscsDocument.getValue().getDocumentLink().getDocumentUrl()),
            DM_STORE_USER_ID
        );
    }
}
