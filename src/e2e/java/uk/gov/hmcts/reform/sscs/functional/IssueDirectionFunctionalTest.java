package uk.gov.hmcts.reform.sscs.functional;

import static junit.framework.TestCase.assertNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DIRECTION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.INTERLOC_VALID_APPEAL;

import java.io.IOException;
import java.time.LocalDate;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

public class IssueDirectionFunctionalTest extends AbstractFunctionalTest {

    public IssueDirectionFunctionalTest() {
        super();
    }

    @Test
    public void processAnIssueDirectionEvent_shouldUpdateInterlocReviewState() throws IOException {

        createCaseWithValidAppealState(INTERLOC_VALID_APPEAL);

        String json = getJson(DIRECTION_ISSUED);
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().toString());

        simulateCcdCallback(json);

        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        SscsCaseData caseData = caseDetails.getData();

        assertNull(caseData.getDirectionType());
    }
}
