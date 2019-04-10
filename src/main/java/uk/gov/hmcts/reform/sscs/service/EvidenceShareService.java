package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.nonNull;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.bundling.SscsBundlingAndStitchingService;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.Bundle;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.EvidenceShareConfig;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocumentManagementService;
import uk.gov.hmcts.reform.sscs.factory.DocumentRequestFactory;

@Component
@Slf4j
public class EvidenceShareService {

    private static final String DM_STORE_USER_ID = "sscs";

    private final SscsCaseCallbackDeserializer sscsCaseCallbackDeserializer;

    private final DocumentManagementService documentManagementService;

    private final DocumentRequestFactory documentRequestFactory;

    private final EvidenceManagementService evidenceManagementService;

    private final BulkPrintService bulkPrintService;

    private final EvidenceShareConfig evidenceShareConfig;

    private final SscsBundlingAndStitchingService sscsAddCaseBundleService;

    @Autowired
    public EvidenceShareService(
        SscsCaseCallbackDeserializer sscsDeserializer,
        DocumentManagementService documentManagementService,
        DocumentRequestFactory documentRequestFactory,
        EvidenceManagementService evidenceManagementService,
        BulkPrintService bulkPrintService,
        EvidenceShareConfig evidenceShareConfig,
        SscsBundlingAndStitchingService sscsAddCaseBundleService
    ) {

        this.sscsCaseCallbackDeserializer = sscsDeserializer;
        this.documentManagementService = documentManagementService;
        this.documentRequestFactory = documentRequestFactory;
        this.evidenceManagementService = evidenceManagementService;
        this.bulkPrintService = bulkPrintService;
        this.evidenceShareConfig = evidenceShareConfig;
        this.sscsAddCaseBundleService = sscsAddCaseBundleService;
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
                log.info("Generating document for case id {}", sscsCaseDataCallback.getCaseDetails().getId());

                documentManagementService.generateDocumentAndAddToCcd(holder, caseData);

                List<Bundle> bundles = sscsAddCaseBundleService.bundleAndStitch(caseData);

                if (null != bundles && bundles.size() > 0 && null != bundles.get(0).getValue().getStitchedDocument()) {
                    return bulkPrintService.sendToBulkPrint(stitchedPdf(bundles.get(0).getValue().getStitchedDocument()), caseData);
                }
            }
        } else {
            log.warn("case id {} is not ready to send to dwp", sscsCaseDataCallback.getCaseDetails().getId());
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

    private Pdf stitchedPdf(DocumentLink doc) {
        return (new Pdf(evidenceManagementService.download(URI.create(doc.getDocumentUrl()), DM_STORE_USER_ID), doc.getDocumentFilename()));
    }
}
