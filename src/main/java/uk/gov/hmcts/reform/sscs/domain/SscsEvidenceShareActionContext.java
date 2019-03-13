package uk.gov.hmcts.reform.sscs.domain;

import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

public class SscsEvidenceShareActionContext {

    private final Pdf pdf;
    private final SscsCaseDetails caseDetails;

    public SscsEvidenceShareActionContext(Pdf pdf, SscsCaseDetails caseDetails) {
        this.pdf = pdf;
        this.caseDetails = caseDetails;
    }

    public Pdf getPdf() {
        return pdf;
    }

    public SscsCaseDetails getCaseDetails() {
        return caseDetails;
    }
}
