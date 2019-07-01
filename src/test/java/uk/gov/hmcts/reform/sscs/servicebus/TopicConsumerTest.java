package uk.gov.hmcts.reform.sscs.servicebus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.exception.BulkPrintException;
import uk.gov.hmcts.reform.sscs.exception.ClientAuthorisationException;
import uk.gov.hmcts.reform.sscs.exception.DwpAddressLookupException;
import uk.gov.hmcts.reform.sscs.exception.NoMrnDetailsException;
import uk.gov.hmcts.reform.sscs.exception.PdfStoreException;
import uk.gov.hmcts.reform.sscs.service.EvidenceShareService;

public class TopicConsumerTest {

    private static final String MESSAGE = "message";
    private static final Exception EXCEPTION = new RuntimeException("blah");
    private static final int RETRY_THREE_TIMES = 3;

    private final EvidenceShareService evidenceShareService = mock(EvidenceShareService.class);

    private final TopicConsumer topicConsumer = new TopicConsumer(evidenceShareService, RETRY_THREE_TIMES);
    private Exception exception;

    @Test
    public void bulkPrintExceptionWillBeCaught() {
        exception = new BulkPrintException(MESSAGE, EXCEPTION);
        doThrow(exception).when(evidenceShareService).processMessage(any());
        topicConsumer.onMessage(MESSAGE);
        verify(evidenceShareService, atLeastOnce()).processMessage(any());
    }

    @Test
    public void pdfStoreExceptionWillBeCaught() {
        exception = new PdfStoreException(MESSAGE, EXCEPTION);
        doThrow(exception).when(evidenceShareService).processMessage(any());
        topicConsumer.onMessage(MESSAGE);
        verify(evidenceShareService, atLeastOnce()).processMessage(any());
    }

    @Test
    public void dwpAddressLookupExceptionWillBeCaught() {
        exception = new DwpAddressLookupException(MESSAGE);
        doThrow(exception).when(evidenceShareService).processMessage(any());
        topicConsumer.onMessage(MESSAGE);
        verify(evidenceShareService, atLeastOnce()).processMessage(any());
    }

    @Test
    public void noMrnDetailsExceptionWillBeCaught() {
        exception = new NoMrnDetailsException(SscsCaseData.builder().ccdCaseId("123").build());
        doThrow(exception).when(evidenceShareService).processMessage(any());
        topicConsumer.onMessage(MESSAGE);
        verify(evidenceShareService, atLeastOnce()).processMessage(any());
    }

    @Test
    public void nullPointerExceptionWillBeCaught() {
        exception = new NullPointerException();
        doThrow(exception).when(evidenceShareService).processMessage(any());
        topicConsumer.onMessage(MESSAGE);
        verify(evidenceShareService, atLeast(RETRY_THREE_TIMES)).processMessage(any());
    }

    @Test
    public void clientAuthorisationExceptionWillBeCaught() {
        exception = new ClientAuthorisationException(EXCEPTION);
        doThrow(exception).when(evidenceShareService).processMessage(any());
        topicConsumer.onMessage(MESSAGE);
        verify(evidenceShareService, atLeast(RETRY_THREE_TIMES)).processMessage(any());
    }

}
