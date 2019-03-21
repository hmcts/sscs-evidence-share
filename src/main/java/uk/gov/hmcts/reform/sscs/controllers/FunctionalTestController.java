package uk.gov.hmcts.reform.sscs.controllers;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.servicebus.TopicConsumer;

@RestController
public class FunctionalTestController {

    private final AuthorisationService authorisationService;
    private final TopicConsumer consumer;

    @Autowired
    public FunctionalTestController(AuthorisationService authorisationService, TopicConsumer consumer) {
        this.authorisationService = authorisationService;
        this.consumer = consumer;
    }

    //FIXME: This is a test endpoint until we figure out a way of using topics and queues for functional tests

    @PostMapping(value = "/send", produces = APPLICATION_JSON_VALUE)
    public void send(@RequestHeader(AuthorisationService.SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
                        @RequestBody String message) {
        authorisationService.authorise(serviceAuthHeader);
        consumer.onMessage(message);
    }
}
