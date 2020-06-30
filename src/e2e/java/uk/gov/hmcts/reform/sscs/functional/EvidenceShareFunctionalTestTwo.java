package uk.gov.hmcts.reform.sscs.functional;

import static org.junit.Assert.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;

import io.github.artsok.RepeatedIfExceptionsTest;
import org.junit.jupiter.api.BeforeEach;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

public class EvidenceShareFunctionalTestTwo extends AbstractFunctionalTest {

    public EvidenceShareFunctionalTestTwo() {
        super();
    }

    @BeforeEach
    public void beforeEach() {
        ccdCaseId = null;
    }

    @RepeatedIfExceptionsTest(repeats = 3)
    public void processAnAppealWithNoValidMrnDate_shouldNoTBeSentToDwpAndShouldBeUpdatedToFlagError() throws Exception {
        Thread.sleep(5000);
        createCaseWithValidAppealState(VALID_APPEAL_CREATED);

        System.out.println("Test 3 Case Id" + ccdCaseId);

        String json = getJson(VALID_APPEAL_CREATED.getCcdType());
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", "");

        simulateCcdCallback(json);
        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);

        assertNull(caseDetails.getData().getSscsDocument());
        assertEquals("validAppeal", caseDetails.getState());
        assertEquals("failedSending", caseDetails.getData().getHmctsDwpState());
    }
}
