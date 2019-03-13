package uk.gov.hmcts.reform.sscs.exception;

public class BulkPrintException extends RuntimeException {
    public BulkPrintException(String message, Throwable e) {
        super(message, e);
    }
}
