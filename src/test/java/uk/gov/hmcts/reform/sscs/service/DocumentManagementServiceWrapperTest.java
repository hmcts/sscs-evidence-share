package uk.gov.hmcts.reform.sscs.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocumentManagementService;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.reform.sscs.exception.PdfStoreException;


public class DocumentManagementServiceWrapperTest {

    private final DocumentManagementService documentManagementService = mock(DocumentManagementService.class);
    private final Pdf pdf = mock(Pdf.class);

    private final DocumentManagementServiceWrapper service =
        new DocumentManagementServiceWrapper(documentManagementService, 3);
    private final DocumentHolder holder = DocumentHolder.builder().build();
    private final SscsCaseData caseData = SscsCaseData.builder().build();

    @Test
    public void successfulCallToDocumentManagementService_willBeCalledOnce() {
        when(documentManagementService.generateDocumentAndAddToCcd(any(), any())).thenReturn(pdf);

        service.generateDocumentAndAddToCcd(holder, caseData);

        verify(documentManagementService).generateDocumentAndAddToCcd(eq(holder), eq(caseData));
        verifyNoMoreInteractions(documentManagementService);
    }

    @Test(expected = PdfStoreException.class)
    public void ifAPdfGenerationExceptionIsThrownServiceCallWillNotBeRetried() {
        when(documentManagementService.generateDocumentAndAddToCcd(any(), any()))
            .thenThrow(new PdfGenerationException("a message", new RuntimeException("blah")));
        try {
            service.generateDocumentAndAddToCcd(holder, caseData);
        } catch (PdfStoreException e) {
            verify(documentManagementService).generateDocumentAndAddToCcd(eq(holder), eq(caseData));
            verifyNoMoreInteractions(documentManagementService);
            throw e;
        }
    }

    @Test
    public void anExceptionWillBeCaughtAndRetriedUntilSuccessful() {
        when(documentManagementService.generateDocumentAndAddToCcd(any(), any()))
            .thenThrow(new RuntimeException("blah"))
            .thenThrow(new RuntimeException("blah"))
            .thenReturn(pdf);

        service.generateDocumentAndAddToCcd(holder, caseData);

        verify(documentManagementService, times(3)).generateDocumentAndAddToCcd(eq(holder), eq(caseData));
        verifyNoMoreInteractions(documentManagementService);
    }

    @Test(expected = PdfStoreException.class)
    public void anExceptionWillBeCaughtAndRetriedUntilItFails() {
        when(documentManagementService.generateDocumentAndAddToCcd(any(), any()))
            .thenThrow(new RuntimeException("blah"))
            .thenThrow(new RuntimeException("blah"))
            .thenThrow(new RuntimeException("blah"));

        service.generateDocumentAndAddToCcd(holder, caseData);
    }
}
