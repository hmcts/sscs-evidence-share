package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;

@RunWith(JUnitParamsRunner.class)
public class SscsDocumentServiceTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private EvidenceManagementService evidenceManagementService;

    @InjectMocks
    private SscsDocumentService sscsDocumentService;

    @Test
    public void filterByDocTypeAndApplyActionHappyPath() {
        List<SscsDocument> sscsDocumentList = createTestData(false);
        Consumer<SscsDocument> action = doc -> doc.getValue().setEvidenceIssued("Yes");

        sscsDocumentService.filterByDocTypeAndApplyAction(sscsDocumentList, APPELLANT_EVIDENCE, action);

        sscsDocumentList.stream()
            .filter(doc -> APPELLANT_EVIDENCE.getValue().equals(doc.getValue().getDocumentType()))
            .forEach(doc -> assertEquals("Yes", doc.getValue().getEvidenceIssued()));
    }

    @Test
    @Parameters({
        "APPELLANT_EVIDENCE,appellantEvidenceDoc, false",
        "REPRESENTATIVE_EVIDENCE,repsEvidenceDoc, false",
        "OTHER_DOCUMENT,otherEvidenceDoc, false",
        "APPELLANT_EVIDENCE,appellantEvidenceDoc, true",
        "REPRESENTATIVE_EVIDENCE,repsEvidenceDoc, true",
        "OTHER_DOCUMENT,otherEvidenceDoc, true"
    })
    public void getPdfsForGivenDocType(DocumentType documentType, String expectedDocName, boolean editedDocument) {

        String expectedDocumentUrl = editedDocument ? "http://editedDocumentUrl" : "http://documentUrl";
        given(evidenceManagementService.download(eq(URI.create(expectedDocumentUrl)), eq("sscs")))
            .willReturn(new byte[]{'a'});

        List<Pdf> actualPdfs = sscsDocumentService.getPdfsForGivenDocTypeNotIssued(createTestData(editedDocument), documentType);

        assertEquals(1, actualPdfs.size());
        assertEquals(new Pdf(new byte[]{'a'}, expectedDocName), actualPdfs.get(0));
    }

    private List<SscsDocument> createTestData(boolean withEditedDocument) {
        DocumentLink documentLink = DocumentLink.builder()
            .documentUrl("http://documentUrl")
            .build();
        DocumentLink editedDocumentLink = DocumentLink.builder()
            .documentUrl("http://editedDocumentUrl")
            .build();
        SscsDocument sscsDocumentAppellantType = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentFileName("appellantEvidenceDoc")
                .documentType(APPELLANT_EVIDENCE.getValue())
                .documentLink(documentLink)
                .editedDocumentLink(withEditedDocument ? editedDocumentLink : null)
                .evidenceIssued("No")
                .build())
            .build();
        SscsDocument sscsDocumentAppellantTypeIssued = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentFileName("appellantEvidenceDoc")
                .documentType(APPELLANT_EVIDENCE.getValue())
                .documentLink(documentLink)
                .editedDocumentLink(withEditedDocument ? editedDocumentLink : null)
                .evidenceIssued("Yes")
                .build())
            .build();
        SscsDocument sscsDocumentRepsType = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentFileName("repsEvidenceDoc")
                .documentType(REPRESENTATIVE_EVIDENCE.getValue())
                .documentLink(documentLink)
                .editedDocumentLink(withEditedDocument ? editedDocumentLink : null)
                .evidenceIssued("No")
                .build())
            .build();
        SscsDocument sscsDocumentOtherType = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentFileName("otherEvidenceDoc")
                .documentType(OTHER_DOCUMENT.getValue())
                .documentLink(documentLink)
                .editedDocumentLink(withEditedDocument ? editedDocumentLink : null)
                .evidenceIssued("No")
                .build())
            .build();
        return Arrays.asList(sscsDocumentAppellantType, sscsDocumentAppellantTypeIssued, sscsDocumentRepsType, sscsDocumentOtherType);
    }
}
