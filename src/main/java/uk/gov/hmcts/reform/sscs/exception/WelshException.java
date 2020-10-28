package uk.gov.hmcts.reform.sscs.exception;

public class WelshException extends RuntimeException {
    public WelshException(Exception ex) {
        super(ex);
    }
}