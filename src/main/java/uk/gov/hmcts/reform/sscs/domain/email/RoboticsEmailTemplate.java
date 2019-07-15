package uk.gov.hmcts.reform.sscs.domain.email;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.domain.email.Email;
import uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment;

@Component
public class RoboticsEmailTemplate {
    private final String from;
    private final String to;
    private final String message;
    private final String scottishTo;

    public RoboticsEmailTemplate(@Value("${robotics.email.from}") String from,
                                 @Value("${robotics.email.to}") String to,
                                 @Value("${robotics.email.scottishTo}") String scottishTo,
                                 @Value("${robotics.email.message}") String message) {
        this.from = from;
        this.to = to;
        this.scottishTo = scottishTo;
        this.message = message;
    }

    public Email generateEmail(String subject, List<EmailAttachment> attachments, boolean isScottish) {
        return new Email(
                from,
                isScottish ? scottishTo : to,
                subject,
                message,
                attachments
        );
    }
}
