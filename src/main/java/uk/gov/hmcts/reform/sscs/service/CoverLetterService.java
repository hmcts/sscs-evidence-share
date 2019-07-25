package uk.gov.hmcts.reform.sscs.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;
import uk.gov.hmcts.reform.sscs.docmosis.service.PdfGenerationService;
import uk.gov.hmcts.reform.sscs.service.placeholders.OriginalSender60997PlaceholderService;

@Service
public class CoverLetterService {

    @Autowired
    private OriginalSender60997PlaceholderService originalSender60997PlaceholderService;
    @Autowired
    @Qualifier("docmosisPdfGenerationService")
    private PdfGenerationService pdfGenerationService;

    //todo: unit test
    public void appendCoverLetter(SscsCaseData caseData, List<Pdf> pdfsToBulkPrint) {
        byte[] coverLetterContent = generate609_97_OriginalSenderCoverLetter(caseData);
        Pdf pdfCoverLetter = new Pdf(coverLetterContent, "609_97_OriginalSenderCoverLetter");
        pdfsToBulkPrint.add(0, pdfCoverLetter);
    }

    private byte[] generate609_97_OriginalSenderCoverLetter(SscsCaseData caseData) {
        return pdfGenerationService.generatePdf(DocumentHolder.builder()
            .template(new Template("TB-SCS-GNO-ENG-00068.doc",
                "609-97-template (original sender)"))
            .placeholders(originalSender60997PlaceholderService.populatePlaceHolders(caseData))
            .pdfArchiveMode(true)
            .build());
    }
}
