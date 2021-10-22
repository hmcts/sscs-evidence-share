package uk.gov.hmcts.reform.sscs.functional;

import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.REISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.domain.pdf.ByteArrayMultipartFile;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;

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
        List<SscsDocument> docs = caseData.getSscsDocument();
        log.info("verifyEvidenceIssued ccdCaseId " + ccdCaseId);

        assertNull(docs.get(0).getValue().getEvidenceIssued());
        assertEquals("Yes",docs.get(1).getValue().getEvidenceIssued());
        assertEquals("Yes",docs.get(2).getValue().getEvidenceIssued());
        assertEquals("Yes",docs.get(3).getValue().getEvidenceIssued());
    }
}
