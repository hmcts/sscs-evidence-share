package uk.gov.hmcts.reform.sscs.exception;

public class NoDl6DocumentException extends RuntimeException {
    public static final long serialVersionUID = 1L;

    public NoDl6DocumentException(String message, Throwable exception) {
        super(message, exception);
    }

    public NoDl6DocumentException(String message) {
        super(message);
    }
}
