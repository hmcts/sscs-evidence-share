package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Service
public class FurtherEvidenceService {

    @Autowired
    private CoverLetterService coverLetterService;
    @Autowired
    private SscsDocumentService sscsDocumentService;
    @Autowired
    private BulkPrintService bulkPrintService;
    @Autowired
    private CcdService ccdService;
    @Autowired
    private IdamService idamService;


    public void issue(SscsCaseData caseData, DocumentType documentType) {
        bulkPrintService.sendToBulkPrint(buildPdfsToBulkPrint(caseData), caseData);
        setEvidenceIssuedFlagToYes(caseData, documentType);
    }

    private void setEvidenceIssuedFlagToYes(SscsCaseData caseData, DocumentType documentType) {
        sscsDocumentService.filterByDocTypeAndApplyAction(caseData.getSscsDocument(), documentType,
            doc -> doc.getValue().setEvidenceIssued("Yes"));
        ccdService.updateCase(
            caseData,
            Long.valueOf(caseData.getCcdCaseId()),
            EventType.UPDATE_CASE_ONLY.getCcdType(),
            "Update case data only",
            "Simply update case data with no callbacks afterwards",
            idamService.getIdamTokens());
    }

    private List<Pdf> buildPdfsToBulkPrint(SscsCaseData caseData) {
        List<Pdf> pdfsToBulkPrint = sscsDocumentService.getPdfsForGivenDocType(
            caseData.getSscsDocument(), APPELLANT_EVIDENCE);
        byte[] coverLetterContent = coverLetterService.generate609_97_OriginalSenderCoverLetter(caseData);
        coverLetterService.appendCoverLetter(coverLetterContent, pdfsToBulkPrint);
        return pdfsToBulkPrint;
    }
}
