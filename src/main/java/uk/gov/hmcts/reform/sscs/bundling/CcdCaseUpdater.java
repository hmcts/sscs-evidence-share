package uk.gov.hmcts.reform.sscs.bundling;

import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public interface CcdCaseUpdater {

    SscsCaseData bundleAndStitch(SscsCaseData sscsCaseData);

}
