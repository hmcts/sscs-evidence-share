package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.nonNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocumentManagementService;
import uk.gov.hmcts.reform.sscs.domain.Pdf;
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

    @Autowired
    public EvidenceShareService(
        SscsCaseCallbackDeserializer sscsCaseCallbackDeserializer,
        DocumentManagementService documentManagementService,
        DocumentRequestFactory documentRequestFactory,
        EvidenceManagementService evidenceManagementService,
        BulkPrintService bulkPrintService
    ) {

        this.sscsCaseCallbackDeserializer = sscsCaseCallbackDeserializer;
        this.documentManagementService = documentManagementService;
        this.documentRequestFactory = documentRequestFactory;
        this.evidenceManagementService = evidenceManagementService;
        this.bulkPrintService = bulkPrintService;
    }

    public long processMessage(final String message) {
        Callback<SscsCaseData> sscsCaseDataCallback = sscsCaseCallbackDeserializer.deserialize(message);

        log.info("Processing callback event {} for case id {}", sscsCaseDataCallback.getEvent(),
            sscsCaseDataCallback.getCaseDetails().getId());

        final SscsCaseData caseData = sscsCaseDataCallback.getCaseDetails().getCaseData();
        DocumentHolder holder = documentRequestFactory.create(caseData);

        if (holder.getTemplate() != null) {
            Pdf newPdfAddedToTheCase = documentManagementService.generateDocumentAndAddToCcd(holder, caseData);
            List<Pdf> existingCasePdfs = toPdf(caseData.getSscsDocument());
            bulkPrintService.sendToBulkPrint(getAllCasePdfs(newPdfAddedToTheCase, existingCasePdfs), caseData);
        }

        return sscsCaseDataCallback.getCaseDetails().getId();
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
            .filter(doc -> nonNull(doc) && nonNull(doc.getValue()) && nonNull(doc.getValue().getDocumentFileName()))
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
