package uk.gov.hmcts.reform.sscs.functional;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;

import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@Slf4j
public class IssueFurtherEvidenceHandlerFunctionalTest extends AbstractFunctionalTest {

    @Before
    public void slowDownTests() throws InterruptedException {
        // we have a problem with functional tests failing randomly. It was noticed that there are frequent connection failures,
        //  so maybe the tests run before the corresponding services are spun up. This method introduces a delay before tests
        //  to try and give the environment a bit more time to cope.

        Thread.sleep(5000);
    }

    @Test
    @Ignore("test failing due to 409 conflict error - documents generated during test are already duplicate that needs to be amended before sent to bulk print.")
    public void givenIssueFurtherEventIsTriggered_shouldBulkPrintEvidenceAndCoverLetterAndSetEvidenceIssuedToYes()
        throws IOException {
        // TODO: SSCS-11780
        String issueFurtherEvidenceCallback = createTestData(ISSUE_FURTHER_EVIDENCE.getCcdType());
        simulateCcdCallback(issueFurtherEvidenceCallback);
        verifyEvidenceIssued();
    }

    @Test
    public void givenIssueFurtherEvidenceFails_shouldHandleException() throws IOException {
        // we are able to cause the issue further evidence to fail by setting to null the Appellant.Name in the callback.json
        String issueFurtherEvidenceCallback = createTestData(ISSUE_FURTHER_EVIDENCE.getCcdType() + "Faulty");
        log.info("FAILING TEST STARTED");
        simulateCcdCallback(issueFurtherEvidenceCallback);
        log.info("FAILING TEST MIDDLE");
        verifyEvidenceIsNotIssued();
        log.info("FAILING TEST ENDED");
    }

    @Test
    @Ignore("test failing due to 409 conflict error - documents generated during test are already duplicate that needs to be amended before sent to bulk print.")
    public void givenIssueFurtherEventIsTriggeredWithReasonableAdjustment_shouldNotBulkPrintEvidenceAndCoverLetterAndSetEvidenceIssuedToYes()
        throws IOException {
        // TODO: SSCS-11780
        SscsCaseDetails caseDetails = createDigitalCaseWithEvent(VALID_APPEAL_CREATED);
        String issueFurtherEvidenceCallback = uploadCaseDocuments(ISSUE_FURTHER_EVIDENCE.getCcdType() + "ReasonableAdjustment", caseDetails);

        simulateCcdCallback(issueFurtherEvidenceCallback);
        verifyEvidenceIssuedAndReasonableAdjustmentRaised(caseDetails);
    }

    @Test
    @Ignore("test failing due to 409 conflict error - documents generated during test are already duplicate that needs to be amended before sent to bulk print.")
    public void givenIssueFurtherEventIsTriggeredWithExistingReasonableAdjustment_shouldNotBulkPrintEvidenceAndCoverLetterAndSetEvidenceIssuedToYes()
        throws IOException {
        // TODO: SSCS-11780
        SscsCaseDetails caseDetails = createDigitalCaseWithEvent(VALID_APPEAL_CREATED);
        String issueFurtherEvidenceCallback = uploadCaseDocuments(ISSUE_FURTHER_EVIDENCE.getCcdType() + "ExistingReasonableAdjustment", caseDetails);

        simulateCcdCallback(issueFurtherEvidenceCallback);
        verifyEvidenceIssuedAndExistingReasonableAdjustmentRaised(caseDetails);
    }

    private void verifyEvidenceIsNotIssued() {
        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        SscsCaseData caseData = caseDetails.getData();
        log.info("Case ID = " + caseDetails.getId());
        log.info("caseData.getHmctsDwpState() = " + caseData.getHmctsDwpState());
        assertEquals("failedSendingFurtherEvidence", caseData.getHmctsDwpState());
        List<SscsDocument> docs = caseData.getSscsDocument();
        Assertions.assertThat(docs)
            .extracting(SscsDocument::getValue)
            .extracting(SscsDocumentDetails::getEvidenceIssued)
            .filteredOn("No"::equalsIgnoreCase)
            .hasSize(3);
    }

    private void verifyEvidenceIssued() {
        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        SscsCaseData caseData = caseDetails.getData();
        List<SscsDocument> docs = caseData.getSscsDocument();

        Assertions.assertThat(docs)
            .extracting(SscsDocument::getValue)
            .extracting(SscsDocumentDetails::getEvidenceIssued)
            .filteredOn("Yes"::equalsIgnoreCase)
            .hasSize(3);
    }

    private void verifyEvidenceIssuedAndReasonableAdjustmentRaised(SscsCaseDetails caseDetailsIn) {
        SscsCaseDetails caseDetails = findCaseById(String.valueOf(caseDetailsIn.getId()));
        SscsCaseData caseData = caseDetails.getData();
        List<SscsDocument> docs = caseData.getSscsDocument();

        Assertions.assertThat(docs)
            .extracting(SscsDocument::getValue)
            .extracting(SscsDocumentDetails::getEvidenceIssued)
            .filteredOn("Yes"::equalsIgnoreCase)
            .hasSize(3);

        assertEquals(YesNo.YES, caseData.getReasonableAdjustmentsOutstanding());
        assertEquals(2, caseData.getReasonableAdjustmentsLetters().getAppellant().size());
    }

    private void verifyEvidenceIssuedAndExistingReasonableAdjustmentRaised(SscsCaseDetails caseDetailsIn) {
        SscsCaseDetails caseDetails = findCaseById(String.valueOf(caseDetailsIn.getId()));
        SscsCaseData caseData = caseDetails.getData();
        List<SscsDocument> docs = caseData.getSscsDocument();

        Assertions.assertThat(docs)
            .extracting(SscsDocument::getValue)
            .extracting(SscsDocumentDetails::getEvidenceIssued)
            .filteredOn("Yes"::equalsIgnoreCase)
            .hasSize(3);

        assertEquals(YesNo.YES, caseData.getReasonableAdjustmentsOutstanding());
        assertEquals(2, caseData.getReasonableAdjustmentsLetters().getAppellant().size());
    }


}
