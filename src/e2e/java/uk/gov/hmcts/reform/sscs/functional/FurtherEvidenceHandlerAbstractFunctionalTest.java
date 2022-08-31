package uk.gov.hmcts.reform.sscs.functional;

import org.assertj.core.api.Assertions;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

import java.util.List;

import static org.junit.Assert.assertEquals;

public abstract class FurtherEvidenceHandlerAbstractFunctionalTest extends AbstractFunctionalTest {
    protected void verifyEvidenceIssued(SscsCaseDetails caseDetails) {
        SscsCaseData caseData = caseDetails.getData();
        List<SscsDocument> docs = caseData.getSscsDocument();
        log.info("verifyEvidenceIssued ccdCaseId " + ccdCaseId);

        Assertions.assertThat(docs)
            .extracting(SscsDocument::getValue)
            .extracting(SscsDocumentDetails::getEvidenceIssued)
            .hasSize(3).containsOnly("Yes");
    }

    protected void verifyEvidenceIsNotIssued(SscsCaseDetails caseDetails) {
        SscsCaseData caseData = caseDetails.getData();
        assertEquals("failedSendingFurtherEvidence", caseData.getHmctsDwpState());
        List<SscsDocument> docs = caseData.getSscsDocument();
        Assertions.assertThat(docs)
            .extracting(SscsDocument::getValue)
            .extracting(SscsDocumentDetails::getEvidenceIssued)
            .hasSize(3).containsOnly("No");
    }

    protected void verifyReasonableAdjustmentRaised(SscsCaseDetails caseDetails) {
        SscsCaseData caseData = caseDetails.getData();
        List<SscsDocument> docs = caseData.getSscsDocument();

        assertEquals(YesNo.YES, caseData.getReasonableAdjustmentsOutstanding());
    }

    protected void verifyReasonableAdjustmentAppellantLettersAmountIsCorrect(SscsCaseDetails caseDetails) {
        SscsCaseData caseData = caseDetails.getData();
        List<SscsDocument> docs = caseData.getSscsDocument();

        assertEquals(2, caseData.getReasonableAdjustmentsLetters().getAppellant().size());
    }
}
