package uk.gov.hmcts.reform.sscs.docmosis.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.domain.Pdf;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.SscsPdfService;

@Service
@Slf4j
public class DocumentManagementService {

    @Autowired
    private PdfGenerationService pdfGenerationService;

    @Autowired
    private SscsPdfService sscsPdfService;

    @Autowired
    private IdamService idamService;

    public Pdf generateDocumentAndAddToCcd(DocumentHolder holder, SscsCaseData caseData) {
        log.info("Generating template {} for case id {}", holder.getTemplate().getName(), caseData.getCcdCaseId());

        byte[] pdfBytes = pdfGenerationService.generateDocFrom(holder);
        String pdfName = getPdfName(holder.getTemplate().getName(), caseData.getCcdCaseId());

        log.info("Adding document template {} to ccd for id {}", holder.getTemplate().getName(), caseData.getCcdCaseId());
        sscsPdfService.mergeDocIntoCcd(pdfName, pdfBytes, Long.getLong(caseData.getCcdCaseId()), caseData, idamService.getIdamTokens());

        return new Pdf(pdfBytes, pdfName);
    }

    private String getPdfName(String documentNamePrefix, String caseId) {
        return documentNamePrefix + caseId + ".pdf";
    }
}
