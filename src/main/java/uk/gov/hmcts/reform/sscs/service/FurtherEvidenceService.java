package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REPRESENTATIVE_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.*;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;

@Service
public class FurtherEvidenceService {

    @Autowired
    private CoverLetterService coverLetterService;

    @Autowired
    private SscsDocumentService sscsDocumentService;

    @Autowired
    private BulkPrintService bulkPrintService;

    public void issue(SscsCaseData caseData, DocumentType documentType) {
        List<Pdf> pdfs = sscsDocumentService.getPdfsForGivenDocType(caseData.getSscsDocument(), documentType);

        send609_97_OriginalSender(caseData, documentType, pdfs);
        send609_98_OtherParty(caseData, documentType, pdfs);
        send609_98_Dwp(caseData, pdfs);
    }

    private void send609_97_OriginalSender(SscsCaseData caseData, DocumentType documentType, List<Pdf> pdfs) {
        byte[] bulkPrintList609_97 = buildPdfsFor609_97(caseData, findLetterType(documentType));
        bulkPrintService.sendToBulkPrint(buildPdfs(bulkPrintList609_97, pdfs), caseData);
    }

    private void send609_98_OtherParty(SscsCaseData caseData, DocumentType documentType, List<Pdf> pdfs) {
        byte[] bulkPrintList609_98 = buildPdfsFor609_98(caseData, switchLetterType(documentType));

        List<Pdf> pdfs609_98 = buildPdfs(bulkPrintList609_98, pdfs);

        if (documentType == REPRESENTATIVE_EVIDENCE
            || (documentType == APPELLANT_EVIDENCE && caseData.getAppeal().getRep() != null
            && caseData.getAppeal().getRep().getHasRepresentative().toLowerCase().equals("yes"))) {
            bulkPrintService.sendToBulkPrint(pdfs609_98, caseData);
        }
    }

    private void send609_98_Dwp(SscsCaseData caseData, List<Pdf> pdfs) {
        byte[] bulkPrintList609_98Dwp = buildPdfsFor609_98(caseData, DWP_LETTER);
        bulkPrintService.sendToBulkPrint(buildPdfs(bulkPrintList609_98Dwp, pdfs), caseData);
    }

    private FurtherEvidenceLetterType findLetterType(DocumentType documentType) {
        return documentType == APPELLANT_EVIDENCE ? APPELLANT_LETTER : REPRESENTATIVE_LETTER;
    }

    private FurtherEvidenceLetterType switchLetterType(DocumentType documentType) {
        return documentType == APPELLANT_EVIDENCE ? REPRESENTATIVE_LETTER : APPELLANT_LETTER;
    }

    private List<Pdf> buildPdfs(byte[] coverLetterContent, List<Pdf> pdfsToBulkPrint) {
        coverLetterService.appendCoverLetter(coverLetterContent, pdfsToBulkPrint);
        return pdfsToBulkPrint;
    }

    private byte[] buildPdfsFor609_97(SscsCaseData caseData, FurtherEvidenceLetterType letterType) {
        return coverLetterService.generateCoverLetter(caseData, letterType, "TB-SCS-GNO-ENG-00068.doc", "609-97-template (original sender)");
    }

    private byte[] buildPdfsFor609_98(SscsCaseData caseData, FurtherEvidenceLetterType letterType) {
        return coverLetterService.generateCoverLetter(caseData, letterType, "TB-SCS-GNO-ENG-BLAAAA.doc", "609-98-template (other parties)");
    }

    public boolean canHandleAnyDocument(List<SscsDocument> sscsDocumentList) {
        return null != sscsDocumentList && sscsDocumentList.stream()
            .anyMatch(sscsDocument -> canHandleDocument(sscsDocument));
    }

    private boolean canHandleDocument(SscsDocument sscsDocument) {
        return sscsDocument != null && sscsDocument.getValue() != null
            && "No".equals(sscsDocument.getValue().getEvidenceIssued());
    }
}
