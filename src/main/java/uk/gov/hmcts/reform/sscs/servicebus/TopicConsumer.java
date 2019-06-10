package uk.gov.hmcts.reform.sscs.servicebus;

import static java.lang.String.format;

import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.exception.BulkPrintException;
import uk.gov.hmcts.reform.sscs.exception.DwpAddressLookupException;
import uk.gov.hmcts.reform.sscs.exception.NoMrnDetailsException;
import uk.gov.hmcts.reform.sscs.exception.PdfStoreException;
import uk.gov.hmcts.reform.sscs.service.EvidenceShareService;

@Slf4j
@Component
@Lazy(false)
@ConditionalOnProperty(value = "topic.consumer.enable", matchIfMissing = true)
public class TopicConsumer {

    private final EvidenceShareService evidenceShareService;
    private final Integer maxRetryAttempts;

    public TopicConsumer(final EvidenceShareService evidenceShareService,
                         @Value("${send-letter.maxRetryAttempts}") Integer maxRetryAttempts) {
        this.evidenceShareService = evidenceShareService;
        this.maxRetryAttempts = maxRetryAttempts;
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
            processMessage(message);
        } catch (Exception e) {
            if (retry > maxRetryAttempts) {
                // retried and now unrecoverable. Catch to remove it from the queue.
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
            Optional<UUID> optionalUuid = evidenceShareService.processMessage(message);
            log.info("Processed message for with returned value {}", optionalUuid);
        } catch (PdfStoreException | BulkPrintException | DwpAddressLookupException | NoMrnDetailsException exception) {
            // unrecoverable. Catch to remove it from the queue.
            log.error(format("Caught unrecoverable error: %s", exception.getMessage()), exception);
        }
    }
}
