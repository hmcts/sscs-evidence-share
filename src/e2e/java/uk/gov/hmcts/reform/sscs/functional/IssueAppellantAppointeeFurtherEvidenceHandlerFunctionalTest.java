package uk.gov.hmcts.reform.sscs.functional;

import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertNull;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FURTHER_EVIDENCE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.domain.pdf.ByteArrayMultipartFile;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;

public class IssueAppellantAppointeeFurtherEvidenceHandlerFunctionalTest extends AbstractFunctionalTest {
    private static final String EVIDENCE_DOCUMENT_PDF = "evidence-document.pdf";
    @Autowired
    private EvidenceManagementService evidenceManagementService;

    @Test
    public void givenIssueFurtherEventIsTriggered_shouldBulkPrintEvidenceAndCoverLetterAndSetEvidenceIssuedToYes()
        throws IOException {
        String issueFurtherEvidenceCallback = createTestData();
        simulateCcdCallback(issueFurtherEvidenceCallback);
        verifyEvidenceIssued();
    }

    private void verifyEvidenceIssued() {
        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        SscsCaseData caseData = caseDetails.getData();
        List<SscsDocument> docs = caseData.getSscsDocument();

        assertNull(docs.get(0).getValue().getEvidenceIssued());
        assertThat(docs.get(1).getValue().getEvidenceIssued(), is("Yes"));
        assertThat(docs.get(2).getValue().getEvidenceIssued(), is("Yes"));
        assertThat(docs.get(3).getValue().getEvidenceIssued(), is("Yes"));
    }

    private String createTestData() throws IOException {
        String docUrl = uploadDocToDocMgmtStore();
        createCaseWithValidAppealState();
        String json = getJson(ISSUE_FURTHER_EVIDENCE);
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("EVIDENCE_DOCUMENT_URL_PLACEHOLDER", docUrl);
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
