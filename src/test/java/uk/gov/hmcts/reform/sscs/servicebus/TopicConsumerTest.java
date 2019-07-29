package uk.gov.hmcts.reform.sscs.servicebus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.callback.CallbackDispatcher;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.exception.*;

public class TopicConsumerTest {

    private static final String MESSAGE = "message";
    private static final Exception EXCEPTION = new RuntimeException("blah");
    private static final int RETRY_THREE_TIMES = 3;

    @Mock
    private CallbackDispatcher<SscsCaseData> dispatcher;

    @Mock
    private SscsCaseCallbackDeserializer deserializer;

    private TopicConsumer topicConsumer;
    private Exception exception;

    @Before
    public void setup() {
        initMocks(this);
        topicConsumer = new TopicConsumer(RETRY_THREE_TIMES, dispatcher, deserializer);
    }

    @Test
    public void bulkPrintExceptionWillBeCaught() {
        exception = new BulkPrintException(MESSAGE, EXCEPTION);
        doThrow(exception).when(dispatcher).handle(any(), any());
        topicConsumer.onMessage(MESSAGE);
        verify(dispatcher, atLeastOnce()).handle(any(), any());
    }

    @Test
    public void pdfStoreExceptionWillBeCaught() {
        exception = new PdfStoreException(MESSAGE, EXCEPTION);
        doThrow(exception).when(dispatcher).handle(any(), any());
        topicConsumer.onMessage(MESSAGE);
        verify(dispatcher, atLeastOnce()).handle(any(), any());
    }

    @Test
    public void dwpAddressLookupExceptionWillBeCaught() {
        exception = new DwpAddressLookupException(MESSAGE);
        doThrow(exception).when(dispatcher).handle(any(), any());
        topicConsumer.onMessage(MESSAGE);
        verify(dispatcher, atLeastOnce()).handle(any(), any());
    }

    @Test
    public void noMrnDetailsExceptionWillBeCaught() {
        exception = new NoMrnDetailsException(SscsCaseData.builder().ccdCaseId("123").build());
        doThrow(exception).when(dispatcher).handle(any(), any());
        topicConsumer.onMessage(MESSAGE);
        verify(dispatcher, atLeastOnce()).handle(any(), any());
    }

    @Test
    public void nullPointerExceptionWillBeCaught() {
        exception = new NullPointerException();
        doThrow(exception).when(dispatcher).handle(any(), any());
        topicConsumer.onMessage(MESSAGE);
        verify(dispatcher, atLeast(RETRY_THREE_TIMES)).handle(any(), any());
    }

    @Test
    public void clientAuthorisationExceptionWillBeCaught() {
        exception = new ClientAuthorisationException(EXCEPTION);
        doThrow(exception).when(dispatcher).handle(any(), any());
        topicConsumer.onMessage(MESSAGE);
        verify(dispatcher, atLeast(RETRY_THREE_TIMES)).handle(any(), any());
    }

    @Test
    public void handleValidRequest() {
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(
            123L,
            "jurisdiction",
            null,
            SscsCaseData.builder().build(),
            null
        );
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.EVIDENCE_RECEIVED);
        when(deserializer.deserialize(any())).thenReturn(callback);
        topicConsumer.onMessage(MESSAGE);
        verify(dispatcher).handle(any(), any());
    }

}
