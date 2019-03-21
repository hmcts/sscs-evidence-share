package uk.gov.hmcts.reform.sscs.docmosis.domain;

public enum Template {

    DL6("TB-SCS-GNO-ENG-00010 v0.2.doc", "DL6");

    private String templateName;
    private String hmctsDocName;

    Template(String templateName, String hmctsDocName) {
        this.templateName = templateName;
        this.hmctsDocName = hmctsDocName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getHmctsDocName() {
        return hmctsDocName;
    }
}
