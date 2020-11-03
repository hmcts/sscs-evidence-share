package uk.gov.hmcts.reform.sscs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EmailSenderProvider {

    private final FeatureToggleService featureToggleService;

    @Autowired
    @Qualifier("sendGridMailSender")
    private final JavaMailSender sendGridMailSender;

    @Autowired
    @Qualifier("mtaMailSender")
    private final JavaMailSender mtaMailSender;


    public JavaMailSender getMailSender() {
        boolean sendGridEnabled = featureToggleService.isSendGridEnabled();
        if (sendGridEnabled) {
            log.info("Sending email via sendgrid");
            return sendGridMailSender;
        }
        log.info("Sending email via mta");
        return mtaMailSender;
    }

}
