package uk.gov.hmcts.reform.sscs.functional;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FURTHER_EVIDENCE;

import java.io.IOException;
import java.util.List;
import org.assertj.core.api.Assertions;
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
        simulateCcdCallback(issueFurtherEvidenceCallback);
        verifyEvidenceIssuedAndExistingReasonableAdjustmentRaised();
    }

    private void verifyEvidenceIsNotIssued() {
        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        SscsCaseData caseData = caseDetails.getData();
        assertEquals("failedSendingFurtherEvidence", caseData.getHmctsDwpState());
        List<SscsDocument> docs = caseData.getSscsDocument();
        Assertions.assertThat(docs)
            .extracting(SscsDocument::getValue)
            .extracting(SscsDocumentDetails::getEvidenceIssued)
            .contains("No", "No", "No");
    }

    private void verifyEvidenceIssued() {
        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        SscsCaseData caseData = caseDetails.getData();
        List<SscsDocument> docs = caseData.getSscsDocument();

        Assertions.assertThat(docs).hasSize(3);

        Assertions.assertThat(docs)
            .extracting(SscsDocument::getValue)
            .extracting(SscsDocumentDetails::getEvidenceIssued)
            .containsOnly("Yes");
    }

    private void verifyEvidenceIssuedAndReasonableAdjustmentRaised() {
        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        SscsCaseData caseData = caseDetails.getData();
        List<SscsDocument> docs = caseData.getSscsDocument();

        Assertions.assertThat(docs).hasSize(3);

        Assertions.assertThat(docs)
            .extracting(SscsDocument::getValue)
            .extracting(SscsDocumentDetails::getEvidenceIssued)
            .containsOnly("Yes");

        assertEquals(YesNo.YES, caseData.getReasonableAdjustmentsOutstanding());
        assertEquals(2, caseData.getReasonableAdjustmentsLetters().getAppellant().size());
    }

    private void verifyEvidenceIssuedAndExistingReasonableAdjustmentRaised() {
        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        SscsCaseData caseData = caseDetails.getData();
        List<SscsDocument> docs = caseData.getSscsDocument();

        Assertions.assertThat(docs).hasSize(3);

        Assertions.assertThat(docs)
            .extracting(SscsDocument::getValue)
            .extracting(SscsDocumentDetails::getEvidenceIssued)
            .containsOnly("Yes");

        assertEquals(YesNo.YES, caseData.getReasonableAdjustmentsOutstanding());
        assertEquals(2, caseData.getReasonableAdjustmentsLetters().getAppellant().size());
    }


}
