package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
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
@Slf4j
public class CoverLetterService {

    @Autowired
    private OriginalSender60997PlaceholderService originalSender60997PlaceholderService;
    @Autowired
    @Qualifier("docmosisPdfGenerationService")
    private PdfGenerationService pdfGenerationService;

    public void appendCoverLetter(SscsCaseData caseData, List<Pdf> pdfsToBulkPrint) {
        requireNonNull(caseData, "caseData must not be null");
        requireNonNull(pdfsToBulkPrint, "pdfsToBulkPrint must not be null");
        byte[] coverLetterContent = generate609_97_OriginalSenderCoverLetter(caseData);
        printCoverLetterToPdfLocallyForDebuggingPurpose(coverLetterContent);
        Pdf pdfCoverLetter = new Pdf(coverLetterContent, "609_97_OriginalSenderCoverLetter");
        pdfsToBulkPrint.add(0, pdfCoverLetter);
    }

    /**
     * Intended use of this method is only for local development or testing.
     * This method produces a cover letter pdf and we can review and confirm is correct
     * @param coverLetterContent
     */
    private void printCoverLetterToPdfLocallyForDebuggingPurpose(byte[] coverLetterContent) {
        if (log.isDebugEnabled()) {
            try {
                FileUtils.writeByteArrayToFile(new File("coverLetter.pdf"), coverLetterContent);
            } catch (Exception e) {
                log.info("CoverLetter fails to be created", e);
            }
        }
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
