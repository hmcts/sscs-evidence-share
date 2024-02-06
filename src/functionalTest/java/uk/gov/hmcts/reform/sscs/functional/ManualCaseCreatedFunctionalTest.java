package uk.gov.hmcts.reform.sscs.functional;

import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;

import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

public class ManualCaseCreatedFunctionalTest extends AbstractFunctionalTest {
    @Test
    public void processANonDigitalAppealWithValidMrn_shouldGenerateADl6AndAddToCcdAndUpdateState() throws Exception {

        SscsCaseDetails caseDetails = createNonDigitalCaseWithEvent(VALID_APPEAL_CREATED);

        Thread.sleep(4000);

        updateCaseEvent(SEND_TO_DWP_OFFLINE, caseDetails);
    }
}
