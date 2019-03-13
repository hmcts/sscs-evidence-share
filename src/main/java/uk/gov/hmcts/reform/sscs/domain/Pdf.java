package uk.gov.hmcts.reform.sscs.domain;

public class Pdf {
    private final byte[] content;
    private final String name;

    public Pdf(byte[] content, String name) {
        this.content = content;
        this.name = name;
    }

    public byte[] getContent() {
        return content;
    }

    public String getName() {
        return name;
    }
}
