package uk.gov.hmcts.reform.sscs.controllers;

import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SendToDwpOfflineEventController {

    @PostMapping(value = "/sendToDwpOffline", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public void sendToDwpOffline() {

    }
}

