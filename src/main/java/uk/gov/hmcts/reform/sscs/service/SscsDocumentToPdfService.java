package uk.gov.hmcts.reform.sscs.service;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;

@Service
public class SscsDocumentToPdfService {
    @Autowired
    private EvidenceManagementService evidenceManagementService;

    public List<Pdf> getPdfsForGivenDocType(List<SscsDocument> sscsDocuments, DocumentType documentType) {
        return sscsDocuments.stream()
            .filter(doc -> documentType.getValue().equals(doc.getValue().getDocumentType()))
            .map(this::toPdf)
            .collect(Collectors.toList());
    }

    private Pdf toPdf(SscsDocument sscsDocument) {
        return new Pdf(getContentForGivenDoc(sscsDocument), sscsDocument.getValue().getDocumentFileName());
    }

    private byte[] getContentForGivenDoc(SscsDocument sscsDocument) {
        return evidenceManagementService.download(URI.create(
            sscsDocument.getValue().getDocumentLink().getDocumentUrl()), "sscs");
    }

}
