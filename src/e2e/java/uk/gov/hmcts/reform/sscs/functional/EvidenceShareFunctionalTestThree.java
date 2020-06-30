package uk.gov.hmcts.reform.sscs.functional;

import static org.junit.Assert.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;

import java.time.LocalDate;
import java.util.List;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;

public class EvidenceShareFunctionalTestThree extends AbstractFunctionalTest {

    public EvidenceShareFunctionalTestThree() {
        super();
    }

    @BeforeEach
    public void beforeEach() {
        ccdCaseId = null;
    }

    @Test
    public void processAnAppealWithLateMrn_shouldGenerateADl16AndAddToCcdAndUpdateState() throws Exception {
        Thread.sleep(5000);
        createCaseWithValidAppealState(VALID_APPEAL_CREATED);

        System.out.println("Test 3 Case Id" + ccdCaseId);

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
