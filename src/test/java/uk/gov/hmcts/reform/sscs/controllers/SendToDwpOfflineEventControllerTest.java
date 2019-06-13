package uk.gov.hmcts.reform.sscs.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@WebMvcTest(SendToDwpOfflineEventController.class)
@OverrideAutoConfiguration(enabled=true)
public class SendToDwpOfflineEventControllerTest {
    @Autowired
    private MockMvc mvc;

    @Test
    public void givenSendToDwpOfflineEventIsTriggered_shouldRemoveExceptionFlagAndReturnCase() throws Exception {
        mvc.perform(post("/sendToDwpOffline")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}
