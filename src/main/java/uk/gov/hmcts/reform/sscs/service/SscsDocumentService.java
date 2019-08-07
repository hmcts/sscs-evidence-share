package uk.gov.hmcts.reform.sscs.service;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;

@Service
public class SscsDocumentService {
    @Autowired
    private EvidenceManagementService evidenceManagementService;

    public List<Pdf> getPdfsForGivenDocTypeNotIssued(List<SscsDocument> sscsDocuments, DocumentType documentType) {
        Objects.requireNonNull(sscsDocuments);
        Objects.requireNonNull(documentType);
        return sscsDocuments.stream()
            .filter(doc -> documentType.getValue().equals(doc.getValue().getDocumentType())
                && "No".equals(doc.getValue().getEvidenceIssued()))
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

    public void filterByDocTypeAndApplyAction(List<SscsDocument> sscsDocument, DocumentType documentType,
                                              Consumer<SscsDocument> action) {
        Objects.requireNonNull(sscsDocument);
        Objects.requireNonNull(documentType);
        Objects.requireNonNull(action);
        sscsDocument.stream()
            .filter(doc -> documentType.getValue().equals(doc.getValue().getDocumentType()))
            .forEach(action);
    }

}
