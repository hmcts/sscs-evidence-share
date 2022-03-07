package uk.gov.hmcts.reform.sscs.functional;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.REISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;

@Slf4j
public class ReissueAppellantAppointeeFurtherEvidenceHandlerFunctionalTest extends AbstractFunctionalTest {

    @Test
    public void givenReIssueFurtherEventIsTriggered_shouldBulkPrintEvidenceAndCoverLetterAndSetEvidenceIssuedToYes()
        throws IOException {
        String reissueFurtherEvidenceCallback = createTestData(REISSUE_FURTHER_EVIDENCE.getCcdType());
        simulateCcdCallback(reissueFurtherEvidenceCallback);
        verifyEvidenceIssued();
    }

    private void verifyEvidenceIssued() {
        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        SscsCaseData caseData = caseDetails.getData();
        log.info("verifyEvidenceIssued ccdCaseId " + ccdCaseId);

        assertThat(caseData.getSscsDocument())
            .hasSize(4)
            .extracting(SscsDocument::getValue)
            .extracting(SscsDocumentDetails::getEvidenceIssued)
            .containsExactlyInAnyOrder(YES, YES, YES, null);
    }
}
