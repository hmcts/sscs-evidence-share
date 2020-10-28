package uk.gov.hmcts.reform.sscs.functional;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;

import java.io.IOException;
import java.time.LocalDate;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;

public class CaseUpdatedHandlerFunctionalTest extends AbstractFunctionalTest {
    @Autowired
    private EvidenceManagementService evidenceManagementService;

    public CaseUpdatedHandlerFunctionalTest() {
        super();
    }

    // Need tribunals running to pass this functional test
    @Test
    public void updateCaseDataAddsJointParty() throws IOException {

        createCaseWithValidAppealState(VALID_APPEAL_CREATED, "UC", "Universal Credit", State.READY_TO_LIST.getId());

        String json = getJson(CASE_UPDATED.getCcdType());
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().toString());

        simulateCcdCallback(json);

        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        SscsCaseData caseData = caseDetails.getData();

        assertEquals("Yes", caseDetails.getData().getJointParty());
    }
}
