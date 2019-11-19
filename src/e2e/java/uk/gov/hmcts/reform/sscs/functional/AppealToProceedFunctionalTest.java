package uk.gov.hmcts.reform.sscs.functional;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_TO_PROCEED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.INTERLOC_VALID_APPEAL;

import java.io.IOException;
import java.time.LocalDate;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

public class AppealToProceedFunctionalTest extends AbstractFunctionalTest {

    public AppealToProceedFunctionalTest() {
        super();
    }

    @Test
    public void processAnAppealToProceedEvent_shouldUpdateInterlocReviewState() throws IOException {

        createCaseWithValidAppealState(INTERLOC_VALID_APPEAL);

        String json = getJson(APPEAL_TO_PROCEED);
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().toString());

        simulateCcdCallback(json);

        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        SscsCaseData caseData = caseDetails.getData();

        assertEquals("none", caseData.getInterlocReviewState());
        assertEquals("validAppeal", caseDetails.getState());
    }
}
