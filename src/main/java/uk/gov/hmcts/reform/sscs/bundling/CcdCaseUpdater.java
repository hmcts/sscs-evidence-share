package uk.gov.hmcts.reform.sscs.bundling;

import java.util.List;
import uk.gov.hmcts.reform.sscs.ccd.domain.Bundle;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public interface CcdCaseUpdater {

    List<Bundle> bundleAndStitch(SscsCaseData sscsCaseData);

}
