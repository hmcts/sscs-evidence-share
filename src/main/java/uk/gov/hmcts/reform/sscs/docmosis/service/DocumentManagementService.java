package uk.gov.hmcts.reform.sscs.docmosis.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.domain.Pdf;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.CcdPdfService;

@Service
@Slf4j
public class DocumentManagementService {

    @Autowired
    private PdfGenerationService pdfGenerationService;

    @Autowired
    private CcdPdfService ccdPdfService;

    @Autowired
    private IdamService idamService;

    public Pdf generateDocumentAndAddToCcd(DocumentHolder holder, SscsCaseData caseData) {
        log.info("Generating template {} for case id {}", holder.getTemplate().getHmctsDocName(), caseData.getCcdCaseId());

        byte[] pdfBytes = pdfGenerationService.generatePdf(holder);
        String pdfName = getPdfName(holder.getTemplate().getHmctsDocName(), caseData.getCcdCaseId());

        log.info("Adding document template {} to ccd for id {}", holder.getTemplate().getHmctsDocName(), caseData.getCcdCaseId());
        ccdPdfService.mergeDocIntoCcd(pdfName, pdfBytes, Long.valueOf(caseData.getCcdCaseId()), caseData, idamService.getIdamTokens());

        return new Pdf(pdfBytes, pdfName);
    }

    private String getPdfName(String documentNamePrefix, String caseId) {
        return documentNamePrefix + "-" + caseId + ".pdf";
    }
}
