package uk.gov.hmcts.reform.sscs;

import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_CASE_ONLY;

import org.junit.Test;
import uk.gov.hmcts.reform.sscs.functional.AbstractFunctionalTest;

public class SmokeTest extends AbstractFunctionalTest {

    public SmokeTest() {
        super();
    }

    @Test
    public void checkSendEndpointReturns200() throws Exception {

        String json = getJson(UPDATE_CASE_ONLY.getCcdType());

        simulateCcdCallback(json);
    }
}
