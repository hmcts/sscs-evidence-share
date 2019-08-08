package uk.gov.hmcts.reform.sscs.domain;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

public class DwpAddress {
    private final Optional<String> line1;
    private final Optional<String> line2;
    private final Optional<String> line3;
    private final Optional<String> postCode;

    public DwpAddress(String line1, String line2, String line3, String postCode) {
        this.line1 = Optional.ofNullable(StringUtils.stripToNull(line1));
        this.line2 = Optional.ofNullable(StringUtils.stripToNull(line2));
        this.line3 = Optional.ofNullable(StringUtils.stripToNull(line3));
        this.postCode = Optional.ofNullable(StringUtils.stripToNull(postCode));
    }

    public DwpAddress(String line1, String line2, String postCode) {
        this(line1, line2, null, postCode);
    }

    public Optional<String> getLine1() {
        return line1;
    }

    public Optional<String> getLine2() {
        return line2;
    }

    public Optional<String> getLine3() {
        return line3;
    }

    public Optional<String> getPostCode() {
        return postCode;
    }

    public String[] lines() {
        return Stream.of(line1, line2, line3, postCode).filter(Optional::isPresent).map(Optional::get).toArray(String[]::new);
    }
}
