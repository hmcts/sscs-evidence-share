package uk.gov.hmcts.reform.sscs.exception;

public class UnrecoverableError extends RuntimeException {

    private static final long serialVersionUID = -5246465513909414162L;

    public UnrecoverableError(String message, Throwable cause) {
        super(message, cause);
    }

}
