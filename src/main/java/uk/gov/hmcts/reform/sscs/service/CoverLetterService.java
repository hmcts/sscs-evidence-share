package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;
import uk.gov.hmcts.reform.sscs.docmosis.service.PdfGenerationService;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.exception.UnableToContactThirdPartyException;
import uk.gov.hmcts.reform.sscs.service.placeholders.FurtherEvidencePlaceholderService;

@Service
@Slf4j
public class CoverLetterService {


    private FurtherEvidencePlaceholderService furtherEvidencePlaceholderService;

    private PdfGenerationService pdfGenerationService;


    private int maxRetryAttempts;

    @Autowired
    public CoverLetterService(FurtherEvidencePlaceholderService furtherEvidencePlaceholderService,
                              @Qualifier("docmosisPdfGenerationService") PdfGenerationService pdfGenerationService,
                              @Value("${send-letter.maxRetryAttempts:3}") int maxRetryAttempts) {
        this.furtherEvidencePlaceholderService = furtherEvidencePlaceholderService;
        this.pdfGenerationService = pdfGenerationService;
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public void appendCoverLetter(byte[] coverLetterContent, List<Pdf> pdfsToBulkPrint, String pdfName) {
        requireNonNull(coverLetterContent, "coverLetter must not be null");
        requireNonNull(pdfsToBulkPrint, "pdfsToBulkPrint must not be null");
        Pdf pdfCoverLetter = new Pdf(coverLetterContent, pdfName);

        pdfsToBulkPrint.add(0, pdfCoverLetter);
    }

    /**
     * Intended use of this method is only for local development or testing. This method produces a
     * cover letter pdf file and stores it in the application root path.
     *
     * @param coverLetterContent this is the content in bytes of the cover letter from docmosis
     */
    private void printCoverLetterToPdfLocallyForDebuggingPurpose(byte[] coverLetterContent,
                                                                 FurtherEvidenceLetterType letterType,
                                                                 String hmctsDocName) {
        if (log.isDebugEnabled()) {
            try {
                FileUtils.writeByteArrayToFile(new File(hmctsDocName + letterType.getValue()
                    + "CoverLetter" + ".pdf"), coverLetterContent);
            } catch (Exception e) {
                log.info("CoverLetter fails to be created", e);
            }
        }
    }

    public byte[] generateCoverLetter(SscsCaseData caseData, FurtherEvidenceLetterType letterType, String templateName,
                                      String hmctsDocName) {

        requireNonNull(caseData, "caseData must not be null");

        Map<String, Object> placeholders = furtherEvidencePlaceholderService.populatePlaceholders(caseData, letterType);
        return generateCoverLetterRetry(letterType, templateName, hmctsDocName, placeholders, 1);
    }

    public byte[] generateCoverLetterRetry(FurtherEvidenceLetterType letterType, String templateName,
                                      String hmctsDocName, Map<String, Object> placeholders, int retries) {
        try {

            byte[] coverLetterContent = pdfGenerationService.generatePdf(DocumentHolder.builder()
                .template(new Template(templateName, hmctsDocName))
                .placeholders(placeholders)
                .pdfArchiveMode(true)
                .build());

            printCoverLetterToPdfLocallyForDebuggingPurpose(coverLetterContent, letterType, hmctsDocName);

            return coverLetterContent;

        } catch (Exception e) {
            if (retries < maxRetryAttempts) {
                log.info("Retrying Cover Letter Service retry " + retries + " due to " + e.getMessage());
                return generateCoverLetterRetry(letterType, templateName, hmctsDocName, placeholders, retries + 1);
            } else {
                throw new UnableToContactThirdPartyException("docmosis", e);
            }
        }
    }

}
