package uk.gov.hmcts.reform.sscs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocumentManagementService;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.reform.sscs.exception.PdfStoreException;

@Slf4j
@Service
public class DocumentManagementServiceWrapper {

    private final DocumentManagementService documentManagementService;
    private Integer maxRetryAttempts;

    @Autowired
    public DocumentManagementServiceWrapper(DocumentManagementService documentManagementService,
                                            @Value("${send-letter.maxRetryAttempts}") Integer maxRetryAttempts) {
        this.documentManagementService = documentManagementService;
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public void generateDocumentAndAddToCcd(DocumentHolder holder, SscsCaseData caseData) {
        generateDocumentAndAddToCcdWithRetry(holder, caseData, 1);
    }

    private void generateDocumentAndAddToCcdWithRetry(DocumentHolder holder, SscsCaseData caseData,
                                                      Integer reTryNumber) {
        try {
            documentManagementService.generateDocumentAndAddToCcd(holder, caseData);
        } catch (PdfGenerationException e) {
            throw new PdfStoreException(e.getMessage(), e);
        } catch (Exception e) {
            if (reTryNumber > maxRetryAttempts) {
                throw new PdfStoreException(e.getMessage(), e);
            }
            generateDocumentAndAddToCcdWithRetry(holder, caseData, reTryNumber + 1);
        }
    }

}
