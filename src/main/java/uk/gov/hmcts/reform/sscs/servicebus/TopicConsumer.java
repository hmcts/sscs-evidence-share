package uk.gov.hmcts.reform.sscs.servicebus;

import java.util.Optional;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.exception.BulkPrintException;
import uk.gov.hmcts.reform.sscs.exception.DwpAddressLookupException;
import uk.gov.hmcts.reform.sscs.exception.PdfStoreException;
import uk.gov.hmcts.reform.sscs.service.EvidenceShareService;

@Slf4j
@Component
@Lazy(false)
public class TopicConsumer {

    private final EvidenceShareService evidenceShareService;

    public TopicConsumer(final EvidenceShareService evidenceShareService) {
        this.evidenceShareService = evidenceShareService;
    }

    @JmsListener(
        destination = "${amqp.topic}",
        containerFactory = "topicJmsListenerContainerFactory",
        subscription = "${amqp.subscription}"
    )
    public void onMessage(String message) {
        try {
            Optional<UUID> optionalUuid = evidenceShareService.processMessage(message);
            log.info("Processed message for with returned value {}", optionalUuid);
        } catch (PdfStoreException | BulkPrintException | DwpAddressLookupException exception) {
            // unrecoverable. Catch to remove it from the queue.
            log.error(exception.getMessage(), exception);
        }

    }
}
