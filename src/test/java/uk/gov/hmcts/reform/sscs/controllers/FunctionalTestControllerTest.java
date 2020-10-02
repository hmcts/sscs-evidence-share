package uk.gov.hmcts.reform.sscs.controllers;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.servicebus.TopicConsumer;

public class FunctionalTestControllerTest {

    @Mock
    private AuthorisationService authorisationService;

    @Mock
    private TopicConsumer consumer;

    @InjectMocks
    private FunctionalTestController functionalTestController;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void shouldCreateTemplateAndAddToCcdForMessage() {
        functionalTestController.send("", "message");
        verify(consumer).onMessage("message", "1");
    }
}
