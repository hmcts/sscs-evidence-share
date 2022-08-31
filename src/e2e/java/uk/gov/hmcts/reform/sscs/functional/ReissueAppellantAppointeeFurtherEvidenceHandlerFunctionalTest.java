package uk.gov.hmcts.reform.sscs.functional;

import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.REISSUE_FURTHER_EVIDENCE;

import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;

@Slf4j
public class ReissueAppellantAppointeeFurtherEvidenceHandlerFunctionalTest extends FurtherEvidenceHandlerAbstractFunctionalTest {

    @Test
    public void givenReIssueFurtherEventIsTriggered_shouldBulkPrintEvidenceAndCoverLetterAndSetEvidenceIssuedToYes()
        throws IOException {
        String reissueFurtherEvidenceCallback = createTestData(REISSUE_FURTHER_EVIDENCE.getCcdType());
        simulateCcdCallback(reissueFurtherEvidenceCallback);
        verifyEvidenceIssued(findCaseById(ccdCaseId));
    }

}
