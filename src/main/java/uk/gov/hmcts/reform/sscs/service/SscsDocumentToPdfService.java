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

    //todo: unit test
    public List<Pdf> getPdfsForGivenDocType(List<SscsDocument> sscsDocument, DocumentType documentType) {
        return sscsDocument.stream()
            .filter(doc -> documentType.getValue().equals(doc.getValue().getDocumentType()))
            .map(this::toPdf)
            .collect(Collectors.toList());
    }

    private Pdf toPdf(SscsDocument sscsDocument) {
        return new Pdf(toBytes(sscsDocument), sscsDocument.getValue().getDocumentFileName());
    }

    private byte[] toBytes(SscsDocument sscsDocument) {
        return evidenceManagementService.download(URI.create(
            sscsDocument.getValue().getDocumentLink().getDocumentUrl()),
            "sscs");
    }

}
