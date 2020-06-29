package uk.gov.hmcts.reform.sscs.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;

import io.github.artsok.RepeatedIfExceptionsTest;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;

public class EvidenceShareFunctionalTest extends AbstractFunctionalTest {

    public EvidenceShareFunctionalTest() {
        super();
    }

    public void beforeEach() {
        ccdCaseId = null;
    }

    @RepeatedIfExceptionsTest(repeats = 3)
    public void processAnAppealWithValidMrn_shouldGenerateADl6AndAddToCcdAndUpdateState() throws Exception {
        Thread.sleep(5000);
        createCaseWithValidAppealState(VALID_APPEAL_CREATED);

        String json = getJson(VALID_APPEAL_CREATED.getCcdType());
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().toString());

        simulateCcdCallback(json);

        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        SscsCaseData caseData = caseDetails.getData();

        List<SscsDocument> docs = caseData.getSscsDocument();
        assertEquals(1, docs.size());
        assertEquals("dl6-" + ccdCaseId + ".pdf", docs.get(0).getValue().getDocumentFileName());
        assertEquals("withDwp", caseDetails.getState());
        assertEquals(LocalDate.now().toString(), caseData.getDateSentToDwp());
        assertEquals(LocalDate.now().toString(), caseData.getDateCaseSentToGaps());
    }

    @RepeatedIfExceptionsTest(repeats = 3)
    public void processAnAppealWithNoValidMrnDate_shouldNoTBeSentToDwpAndShouldBeUpdatedToFlagError() throws Exception {
        Thread.sleep(5000);
        createCaseWithValidAppealState(VALID_APPEAL_CREATED);

        String json = getJson(VALID_APPEAL_CREATED.getCcdType());
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", "");

        simulateCcdCallback(json);
        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);

        assertNull(caseDetails.getData().getSscsDocument());
        assertEquals("validAppeal", caseDetails.getState());
        assertEquals("failedSending", caseDetails.getData().getHmctsDwpState());
    }

    @Test
    public void processAnAppealWithLateMrn_shouldGenerateADl16AndAddToCcdAndUpdateState() throws Exception {
        Thread.sleep(5000);
        createCaseWithValidAppealState(VALID_APPEAL_CREATED);

        String json = getJson(VALID_APPEAL_CREATED.getCcdType());
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().minusDays(31).toString());

        simulateCcdCallback(json);
        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        SscsCaseData caseData = caseDetails.getData();

        List<SscsDocument> docs = caseData.getSscsDocument();

        assertNotNull("docs is not null", docs);
        assertEquals(1, docs.size());
        assertEquals("dl16-" + ccdCaseId + ".pdf", docs.get(0).getValue().getDocumentFileName());
        assertEquals("withDwp", caseDetails.getState());
        assertEquals(LocalDate.now().toString(), caseData.getDateSentToDwp());
    }
}
