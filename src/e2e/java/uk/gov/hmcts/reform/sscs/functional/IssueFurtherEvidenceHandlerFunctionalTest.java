package uk.gov.hmcts.reform.sscs.functional;

import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FURTHER_EVIDENCE;

import java.io.IOException;
import org.junit.Test;

public class IssueFurtherEvidenceHandlerFunctionalTest extends AbstractFunctionalTest {
    @Test
    public void givenIssueFurtherEventIsTriggered_shouldBeHandled() throws IOException {
        //todo: create the test data needed for this test
        //1. create simple case in CCD
        //2. upload some docs evidence to EM
        //3. update the case created above with the document links -> use the updateCaseNoNoCallbacks for this
        // Now we have a case in CCD with documentLinks that exist in EM and that we can use to issue further evidence

        String json = getJson(ISSUE_FURTHER_EVIDENCE);
        simulateCcdCallback(json);

        //todo: check that the appellant evidence documents for this test case has the evidenceIssued to Yes
    }
}
