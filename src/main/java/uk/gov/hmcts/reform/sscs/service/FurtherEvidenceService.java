package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REPRESENTATIVE_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.*;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;

@Service
public class FurtherEvidenceService {

    private CoverLetterService coverLetterService;

    private SscsDocumentService sscsDocumentService;

    private BulkPrintService bulkPrintService;

    private final String furtherEvidenceOriginalSenderTemplateName;

    private final String furtherEvidenceOtherPartiesTemplateName;

    public FurtherEvidenceService(@Value("${docmosis.template.609-97.name}") String furtherEvidenceOriginalSenderTemplateName,
                                  @Value("${docmosis.template.609-98.name}") String furtherEvidenceOtherPartiesTemplateName,
                                  @Autowired CoverLetterService coverLetterService,
                                  @Autowired SscsDocumentService sscsDocumentService,
                                  @Autowired BulkPrintService bulkPrintService) {
        this.furtherEvidenceOriginalSenderTemplateName = furtherEvidenceOriginalSenderTemplateName;
        this.furtherEvidenceOtherPartiesTemplateName = furtherEvidenceOtherPartiesTemplateName;
        this.coverLetterService = coverLetterService;
        this.sscsDocumentService = sscsDocumentService;
        this.bulkPrintService = bulkPrintService;

    }

    public void issue(SscsCaseData caseData, DocumentType documentType) {
        List<Pdf> pdfs = sscsDocumentService.getPdfsForGivenDocType(caseData.getSscsDocument(), documentType);

        send609_97_OriginalSender(caseData, documentType, pdfs);
        send609_98_OtherParty(caseData, documentType, pdfs);
        send609_98_Dwp(caseData, pdfs);
    }

    private void send609_97_OriginalSender(SscsCaseData caseData, DocumentType documentType, List<Pdf> pdfs) {
        byte[] bulkPrintList60997 = buildPdfsFor609_97(caseData, findLetterType(documentType));
        bulkPrintService.sendToBulkPrint(buildPdfs(bulkPrintList60997, pdfs), caseData);
    }

    private void send609_98_OtherParty(SscsCaseData caseData, DocumentType documentType, List<Pdf> pdfs) {
        if (documentType == REPRESENTATIVE_EVIDENCE
            || (documentType == APPELLANT_EVIDENCE && caseData.getAppeal().getRep() != null
            && caseData.getAppeal().getRep().getHasRepresentative().toLowerCase().equals("yes"))) {

            byte[] bulkPrintList60998 = buildPdfsFor609_98(caseData, switchLetterType(documentType));

            List<Pdf> pdfs60998 = buildPdfs(bulkPrintList60998, pdfs);
            bulkPrintService.sendToBulkPrint(pdfs60998, caseData);
        }
    }

    private void send609_98_Dwp(SscsCaseData caseData, List<Pdf> pdfs) {
        byte[] bulkPrintList60998Dwp = buildPdfsFor609_98(caseData, DWP_LETTER);
        bulkPrintService.sendToBulkPrint(buildPdfs(bulkPrintList60998Dwp, pdfs), caseData);
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
        return coverLetterService.generateCoverLetter(caseData, letterType, furtherEvidenceOriginalSenderTemplateName, "609-97-template (original sender)");
    }

    private byte[] buildPdfsFor609_98(SscsCaseData caseData, FurtherEvidenceLetterType letterType) {
        return coverLetterService.generateCoverLetter(caseData, letterType, furtherEvidenceOtherPartiesTemplateName, "609-98-template (other parties)");
    }

    public boolean canHandleAnyDocument(List<SscsDocument> sscsDocumentList) {
        return null != sscsDocumentList && sscsDocumentList.stream()
            .anyMatch(sscsDocument -> canHandleDocument(sscsDocument));
    }

    private boolean canHandleDocument(SscsDocument sscsDocument) {
        return sscsDocument != null && sscsDocument.getValue() != null
            && "No".equals(sscsDocument.getValue().getEvidenceIssued())
            && null != sscsDocument.getValue().getDocumentType();
    }
}
