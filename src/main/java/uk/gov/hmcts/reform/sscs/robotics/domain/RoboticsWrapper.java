package uk.gov.hmcts.reform.sscs.robotics.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

@Data
@Builder
public class RoboticsWrapper {

    private SscsCaseData sscsCaseData;

    private Long ccdCaseId;

    private String evidencePresent;

    private State state;
}
