package uk.gov.hmcts.reform.sscs.service;

import static java.util.Optional.*;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AbstractDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;

@Service
public class SscsDocumentService {
    @Autowired
    private EvidenceManagementService evidenceManagementService;

    public List<Pdf> getPdfsForGivenDocTypeNotIssued(List<? extends AbstractDocument> sscsDocuments, DocumentType documentType) {
        Objects.requireNonNull(sscsDocuments);
        Objects.requireNonNull(documentType);
        return sscsDocuments.stream()
            .filter(doc -> documentType.getValue().equals(doc.getValue().getDocumentType())
                && "No".equals(doc.getValue().getEvidenceIssued()))
            .map(this::toPdf)
            .collect(Collectors.toList());
    }

    private Pdf toPdf(AbstractDocument sscsDocument) {
        return new Pdf(getContentForGivenDoc(sscsDocument), sscsDocument.getValue().getDocumentFileName());
    }

    private byte[] getContentForGivenDoc(AbstractDocument sscsDocument) {
        final DocumentLink documentLink = ofNullable(sscsDocument.getValue().getEditedDocumentLink())
            .orElse(sscsDocument.getValue().getDocumentLink());
        return evidenceManagementService.download(URI.create(documentLink.getDocumentUrl()), "sscs");
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
