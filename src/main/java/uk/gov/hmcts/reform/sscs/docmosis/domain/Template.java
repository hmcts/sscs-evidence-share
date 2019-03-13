package uk.gov.hmcts.reform.sscs.docmosis.domain;

public enum Template {

    DL6("DL6");

    private String name;

    Template(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
