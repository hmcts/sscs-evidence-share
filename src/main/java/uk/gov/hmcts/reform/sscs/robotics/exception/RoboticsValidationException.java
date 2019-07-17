package uk.gov.hmcts.reform.sscs.robotics.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY,
        reason = "Invalid robotics validation")
public class RoboticsValidationException extends RuntimeException {

    public RoboticsValidationException(Throwable cause) {
        super(cause);
    }

}
