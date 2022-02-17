package uk.gov.hmcts.reform.sscs.functional;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FURTHER_EVIDENCE;

import java.io.IOException;
import java.util.List;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

public class IssueFurtherEvidenceHandlerFunctionalTest extends AbstractFunctionalTest {

    @Test
    public void givenIssueFurtherEventIsTriggered_shouldBulkPrintEvidenceAndCoverLetterAndSetEvidenceIssuedToYes()
        throws IOException {
        String issueFurtherEvidenceCallback = createTestData(ISSUE_FURTHER_EVIDENCE.getCcdType());
        simulateCcdCallback(issueFurtherEvidenceCallback);
        verifyEvidenceIssued();
    }

    @Test
    public void givenIssueFurtherEvidenceFails_shouldHandleException() throws IOException {
        // we are able to cause the issue further evidence to fail by setting to null the Appellant.Name in the callback.json
        String issueFurtherEvidenceCallback = createTestData(ISSUE_FURTHER_EVIDENCE.getCcdType() + "Faulty");
        simulateCcdCallback(issueFurtherEvidenceCallback);
        verifyEvidenceIsNotIssued();
    }

    @Test
    public void givenIssueFurtherEventIsTriggeredWithReasonableAdjustment_shouldNotBulkPrintEvidenceAndCoverLetterAndSetEvidenceIssuedToYes()
        throws IOException {
        String issueFurtherEvidenceCallback = createTestData(ISSUE_FURTHER_EVIDENCE.getCcdType() + "ReasonableAdjustment");
        simulateCcdCallback(issueFurtherEvidenceCallback);
        verifyEvidenceIssuedAndReasonableAdjustmentRaised();
    }

    @Test
    public void givenIssueFurtherEventIsTriggeredWithExistingReasonableAdjustment_shouldNotBulkPrintEvidenceAndCoverLetterAndSetEvidenceIssuedToYes()
        throws IOException {
        String issueFurtherEvidenceCallback = createTestData(ISSUE_FURTHER_EVIDENCE.getCcdType() + "ExistingReasonableAdjustment");
        System.out.println("[givenIssueFurtherEventIsTriggeredWithExistingReasonableAdjustment_shouldNotBulkPrintEvidenceAndCoverLetterAndSetEvidenceIssuedToYes] issueFurtherEvidenceCallback :  " + issueFurtherEvidenceCallback);
        simulateCcdCallback(issueFurtherEvidenceCallback);

        verifyEvidenceIssuedAndExistingReasonableAdjustmentRaised();
    }

    private void verifyEvidenceIsNotIssued() {
        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        SscsCaseData caseData = caseDetails.getData();
        assertEquals("failedSendingFurtherEvidence", caseData.getHmctsDwpState());
        List<SscsDocument> docs = caseData.getSscsDocument();
        assertNull(docs.get(0).getValue().getEvidenceIssued());
        assertEquals("No", docs.get(1).getValue().getEvidenceIssued());
        assertEquals("No", docs.get(2).getValue().getEvidenceIssued());
        assertEquals("No", docs.get(3).getValue().getEvidenceIssued());
    }

    private void verifyEvidenceIssued() {
        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        SscsCaseData caseData = caseDetails.getData();
        List<SscsDocument> docs = caseData.getSscsDocument();

        assertNull(docs.get(0).getValue().getEvidenceIssued());
        assertEquals("Yes", docs.get(1).getValue().getEvidenceIssued());
        assertEquals("Yes", docs.get(2).getValue().getEvidenceIssued());
        assertEquals("Yes", docs.get(3).getValue().getEvidenceIssued());
    }

    private void verifyEvidenceIssuedAndReasonableAdjustmentRaised() {
        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        SscsCaseData caseData = caseDetails.getData();
        List<SscsDocument> docs = caseData.getSscsDocument();

        assertNull(docs.get(0).getValue().getEvidenceIssued());
        assertEquals("Yes", docs.get(1).getValue().getEvidenceIssued());
        assertEquals("Yes", docs.get(2).getValue().getEvidenceIssued());
        assertEquals("Yes", docs.get(3).getValue().getEvidenceIssued());
        assertEquals(YesNo.YES, caseData.getReasonableAdjustmentsOutstanding());
        assertEquals(2, caseData.getReasonableAdjustmentsLetters().getAppellant().size());
    }

    private void verifyEvidenceIssuedAndExistingReasonableAdjustmentRaised() {
        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        SscsCaseData caseData = caseDetails.getData();
        List<SscsDocument> docs = caseData.getSscsDocument();

        System.out.println("[verifyEvidenceIssuedAndExistingReasonableAdjustmentRaised] docs.size() :  " + docs.size());
        System.out.println("[verifyEvidenceIssuedAndExistingReasonableAdjustmentRaised] docs.get(0) :  " + docs.get(0).getValue().toString());
        System.out.println("[verifyEvidenceIssuedAndExistingReasonableAdjustmentRaised] docs.get(1) :  " + docs.get(1).getValue().toString());
        System.out.println("[verifyEvidenceIssuedAndExistingReasonableAdjustmentRaised] docs.get(2) :  " + docs.get(2).getValue().toString());
        System.out.println("[verifyEvidenceIssuedAndExistingReasonableAdjustmentRaised] docs.get(3) :  " + docs.get(3).getValue().toString());
        System.out.println("[verifyEvidenceIssuedAndExistingReasonableAdjustmentRaised] docs.get(0).getEvidenceIssued( :  " + docs.get(0).getValue().getEvidenceIssued());
        System.out.println("[verifyEvidenceIssuedAndExistingReasonableAdjustmentRaised] docs.get(1).getEvidenceIssued( :  " + docs.get(1).getValue().getEvidenceIssued());
        System.out.println("[verifyEvidenceIssuedAndExistingReasonableAdjustmentRaised] docs.get(2).getEvidenceIssued( :  " + docs.get(2).getValue().getEvidenceIssued());
        System.out.println("[verifyEvidenceIssuedAndExistingReasonableAdjustmentRaised] docs.get(3).getEvidenceIssued( :  " + docs.get(3).getValue().getEvidenceIssued());


        assertNull(docs.get(0).getValue().getEvidenceIssued());
        assertEquals("Yes", docs.get(1).getValue().getEvidenceIssued());
        assertEquals("Yes", docs.get(2).getValue().getEvidenceIssued());
        assertEquals("Yes", docs.get(3).getValue().getEvidenceIssued());

        assertEquals(YesNo.YES, caseData.getReasonableAdjustmentsOutstanding());
        assertEquals(2, caseData.getReasonableAdjustmentsLetters().getAppellant().size());
    }


}
