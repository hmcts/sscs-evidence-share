package uk.gov.hmcts.reform.sscs.servicebus;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.callback.CallbackDispatcher;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.exception.BulkPrintException;
import uk.gov.hmcts.reform.sscs.exception.DwpAddressLookupException;
import uk.gov.hmcts.reform.sscs.exception.NoMrnDetailsException;
import uk.gov.hmcts.reform.sscs.exception.PdfStoreException;

@Slf4j
@Component
@Lazy(false)
public class TopicConsumer {

    private final Integer maxRetryAttempts;
    private final CallbackDispatcher<SscsCaseData> dispatcher;
    private final SscsCaseCallbackDeserializer sscsDeserializer;

    public TopicConsumer(@Value("${send-letter.maxRetryAttempts}") Integer maxRetryAttempts,
                         CallbackDispatcher dispatcher,
                         SscsCaseCallbackDeserializer sscsDeserializer) {
        this.maxRetryAttempts = maxRetryAttempts;
        this.dispatcher = dispatcher;
        this.sscsDeserializer = sscsDeserializer;
    }

    @JmsListener(
        destination = "${amqp.topic}",
        containerFactory = "topicJmsListenerContainerFactory",
        subscription = "${amqp.subscription}"
    )
    public void onMessage(String message) {
        processMessageWithRetry(message, 1);
    }

    private void processMessageWithRetry(String message, int retry) {
        try {
            log.info("Message received from the service bus by evidence share service");
            processMessage(message);
        } catch (Exception e) {
            if (retry > maxRetryAttempts) {
                log.error(format("Caught unknown unrecoverable error %s", e.getMessage()), e);
            } else {
                log.info(String.format("Caught recoverable error %s, retrying %s out of %s",
                    e.getMessage(), retry, maxRetryAttempts));
                processMessageWithRetry(message, retry + 1);
            }
        }
    }

    private void processMessage(String message) {
        try {
            Callback<SscsCaseData> callback = sscsDeserializer.deserialize(message);
            dispatcher.handle(SUBMITTED, callback);
            log.info("Sscs Case CCD callback `{}` handled for Case ID `{}`", callback.getEvent(), callback.getCaseDetails().getId());
        } catch (PdfStoreException | BulkPrintException | DwpAddressLookupException | NoMrnDetailsException exception) {
            // unrecoverable. Catch to remove it from the queue.
            log.error(format("Caught unrecoverable error: %s", exception.getMessage()), exception);
        }
    }
}
