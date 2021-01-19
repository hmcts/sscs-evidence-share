package uk.gov.hmcts.reform.sscs.functional;

import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.domain.pdf.ByteArrayMultipartFile;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;

public class IssueFurtherEvidenceHandlerFunctionalTest extends AbstractFunctionalTest {
    private static final String EVIDENCE_DOCUMENT_PDF = "evidence-document.pdf";
    @Autowired
    private EvidenceManagementService evidenceManagementService;

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
        assertEquals(2, caseData.getReasonableAdjustmentsLetters().size());
    }

    private String createTestData(String fileName) throws IOException {
        String docUrl = uploadDocToDocMgmtStore();
        createDigitalCaseWithEvent(VALID_APPEAL_CREATED);
        String json = getJson(fileName);
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("EVIDENCE_DOCUMENT_URL_PLACEHOLDER", docUrl);
        json = json.replace("CREATED_IN_GAPS_FROM", State.READY_TO_LIST.getId());
        return json.replace("EVIDENCE_DOCUMENT_BINARY_URL_PLACEHOLDER", docUrl + "/binary");
    }

    private String uploadDocToDocMgmtStore() throws IOException {
        Path evidencePath = new File(Objects.requireNonNull(
            getClass().getClassLoader().getResource(EVIDENCE_DOCUMENT_PDF)).getFile()).toPath();

        ByteArrayMultipartFile file = ByteArrayMultipartFile.builder()
            .content(Files.readAllBytes(evidencePath))
            .name(EVIDENCE_DOCUMENT_PDF)
            .contentType(APPLICATION_PDF)
            .build();

        UploadResponse upload = evidenceManagementService.upload(singletonList(file), "sscs");

        return upload.getEmbedded().getDocuments().get(0).links.self.href;
    }

}
