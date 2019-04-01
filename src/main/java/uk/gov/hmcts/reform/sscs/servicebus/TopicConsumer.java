package uk.gov.hmcts.reform.sscs.servicebus;

import java.util.Optional;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.service.EvidenceShareService;

@Slf4j
@Component
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
        Optional<UUID> optionalUuid = evidenceShareService.processMessage(message);
        log.info("Processed message for with returned value {}", optionalUuid);
    }
}
