package uk.gov.hmcts.reform.sscs.servicebus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.exception.*;
import uk.gov.hmcts.reform.sscs.service.EvidenceShareService;

public class TopicConsumerTest {

    private static final String MESSAGE = "message";
    private static final Exception EXCEPTION = new RuntimeException("blah");
    private static final int RETRY_THREE_TIMES = 3;

    private final EvidenceShareService evidenceShareService = mock(EvidenceShareService.class);

    private final TopicConsumer topicConsumer = new TopicConsumer(evidenceShareService, RETRY_THREE_TIMES);

    @Test
    public void bulkPrintExceptionWillBeCaught() {
        BulkPrintException exception = new BulkPrintException(MESSAGE, EXCEPTION);
        when(evidenceShareService.processMessage(any())).thenThrow(exception);
        topicConsumer.onMessage(MESSAGE);
        verify(evidenceShareService, atLeastOnce()).processMessage(any());
    }

    @Test
    public void pdfStoreExceptionWillBeCaught() {
        PdfStoreException exception = new PdfStoreException(MESSAGE, EXCEPTION);
        when(evidenceShareService.processMessage(any())).thenThrow(exception);
        topicConsumer.onMessage(MESSAGE);
        verify(evidenceShareService, atLeastOnce()).processMessage(any());
    }

    @Test
    public void dwpAddressLookupExceptionWillBeCaught() {
        DwpAddressLookupException exception = new DwpAddressLookupException(MESSAGE);
        when(evidenceShareService.processMessage(any())).thenThrow(exception);
        topicConsumer.onMessage(MESSAGE);
        verify(evidenceShareService, atLeastOnce()).processMessage(any());
    }

    @Test
    public void noMrnDetailsExceptionWillBeCaught() {
        NoMrnDetailsException exception = new NoMrnDetailsException(SscsCaseData.builder().ccdCaseId("123").build());
        when(evidenceShareService.processMessage(any())).thenThrow(exception);
        topicConsumer.onMessage(MESSAGE);
        verify(evidenceShareService, atLeastOnce()).processMessage(any());
    }

    @Test
    public void nullPointerExceptionWillBeCaught() {
        NullPointerException exception = new NullPointerException();
        when(evidenceShareService.processMessage(any())).thenThrow(exception);
        topicConsumer.onMessage(MESSAGE);
        verify(evidenceShareService, atLeast(RETRY_THREE_TIMES)).processMessage(any());
    }

    @Test
    public void clientAuthorisationExceptionWillBeCaught() {
        ClientAuthorisationException exception = new ClientAuthorisationException(EXCEPTION);
        when(evidenceShareService.processMessage(any())).thenThrow(exception);
        topicConsumer.onMessage(MESSAGE);
        verify(evidenceShareService, atLeast(RETRY_THREE_TIMES)).processMessage(any());
    }

}
