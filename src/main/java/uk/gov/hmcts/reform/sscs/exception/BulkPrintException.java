package uk.gov.hmcts.reform.sscs.exception;

public class BulkPrintException extends RuntimeException {
    public static final long serialVersionUID = -7703842220641781482L;

    public BulkPrintException(String message, Throwable exception) {
        super(message, exception);
    }
}
