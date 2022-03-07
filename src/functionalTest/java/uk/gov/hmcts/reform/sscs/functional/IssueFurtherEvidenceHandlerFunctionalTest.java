package uk.gov.hmcts.reform.sscs.functional;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

public class IssueFurtherEvidenceHandlerFunctionalTest extends AbstractFunctionalTest {

    @Before
    public void slowDownTests() throws InterruptedException {
        // we have a problem with functional tests failing randomly. It was noticed that there are frequent connection failures,
        //  so maybe the tests run before the corresponding services are spun up. This method introduces a delay before tests
        //  to try and give the environment a bit more time to cope.

        Thread.sleep(5000);
    }

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
        SscsCaseDetails caseDetails = createDigitalCaseWithEvent(VALID_APPEAL_CREATED);
        String issueFurtherEvidenceCallback = uploadCaseDocuments(ISSUE_FURTHER_EVIDENCE.getCcdType() + "ReasonableAdjustment", caseDetails);

        simulateCcdCallback(issueFurtherEvidenceCallback);
        verifyEvidenceIssuedAndReasonableAdjustmentRaised(caseDetails);
    }

    @Test
    public void givenIssueFurtherEventIsTriggeredWithExistingReasonableAdjustment_shouldNotBulkPrintEvidenceAndCoverLetterAndSetEvidenceIssuedToYes()
        throws IOException {
        SscsCaseDetails caseDetails = createDigitalCaseWithEvent(VALID_APPEAL_CREATED);
        String issueFurtherEvidenceCallback = uploadCaseDocuments(ISSUE_FURTHER_EVIDENCE.getCcdType() + "ExistingReasonableAdjustment", caseDetails);

        simulateCcdCallback(issueFurtherEvidenceCallback);
        verifyEvidenceIssuedAndExistingReasonableAdjustmentRaised(caseDetails);
    }

    private void verifyEvidenceIsNotIssued() {
        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        SscsCaseData caseData = caseDetails.getData();

        assertThat(caseData.getHmctsDwpState()).isEqualTo("failedSendingFurtherEvidence");

        assertThat(caseData.getSscsDocument())
            .extracting(SscsDocument::getValue)
            .extracting(SscsDocumentDetails::getEvidenceIssued)
            .filteredOn(YesNo::isNo)
            .hasSize(3);
    }

    private void verifyEvidenceIssued() {
        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        SscsCaseData caseData = caseDetails.getData();

        assertThat(caseData.getSscsDocument())
            .extracting(SscsDocument::getValue)
            .extracting(SscsDocumentDetails::getEvidenceIssued)
            .filteredOn(YesNo::isYes)
            .hasSize(3);
    }

    private void verifyEvidenceIssuedAndReasonableAdjustmentRaised(SscsCaseDetails caseDetailsIn) {
        SscsCaseDetails caseDetails = findCaseById(String.valueOf(caseDetailsIn.getId()));
        SscsCaseData caseData = caseDetails.getData();

        assertThat(caseData.getSscsDocument())
            .extracting(SscsDocument::getValue)
            .extracting(SscsDocumentDetails::getEvidenceIssued)
            .filteredOn(YesNo::isYes)
            .hasSize(3);

        assertThat(caseData.getReasonableAdjustmentsOutstanding()).isEqualTo(YES);
        assertThat(caseData.getReasonableAdjustmentsLetters().getAppellant()).hasSize(2);
    }

    private void verifyEvidenceIssuedAndExistingReasonableAdjustmentRaised(SscsCaseDetails caseDetailsIn) {
        SscsCaseDetails caseDetails = findCaseById(String.valueOf(caseDetailsIn.getId()));
        SscsCaseData caseData = caseDetails.getData();

        assertThat(caseData.getSscsDocument())
            .extracting(SscsDocument::getValue)
            .extracting(SscsDocumentDetails::getEvidenceIssued)
            .filteredOn(YesNo::isYes)
            .hasSize(3);

        assertThat(caseData.getReasonableAdjustmentsOutstanding()).isEqualTo(YES);
        assertThat(caseData.getReasonableAdjustmentsLetters().getAppellant()).hasSize(2);
    }


}
