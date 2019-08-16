package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;

@RunWith(JUnitParamsRunner.class)
public class FurtherEvidenceServiceTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private CoverLetterService coverLetterService;
    @Spy
    private SscsDocumentService sscsDocumentService;
    @Mock
    private BulkPrintService bulkPrintService;

    private FurtherEvidenceService furtherEvidenceService;

    private SscsCaseData caseData;
    private final List<Pdf> pdfList = Collections.singletonList(new Pdf(new byte[]{}, "some doc name"));

    String furtherEvidenceOriginalSenderTemplateName = "TB-SCS-GNO-ENG-00068.doc";
    String furtherEvidenceOriginalSenderDocName = "609-97-template (original sender)";
    String furtherEvidenceOtherPartiesTemplateName = "TB-SCS-GNO-ENG-00069.doc";
    String furtherEvidenceOtherPartiesDocName = "609-98-template (other parties)";
    String furtherEvidenceOtherPartiesDwpDocName = "609-98-template (DWP)";

    @Before
    public void setup() {
        furtherEvidenceService = new FurtherEvidenceService(furtherEvidenceOriginalSenderTemplateName, furtherEvidenceOtherPartiesTemplateName,
            coverLetterService, sscsDocumentService, bulkPrintService);
    }

    @Test
    public void givenAppellantIssueFurtherEvidenceCallbackWithAppellantAndNoRep_shouldGenerateCoverLetterOriginalSenderAndOtherPartyDwpAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock();

        furtherEvidenceService.issue(caseData, APPELLANT_EVIDENCE);

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDwpDocName));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData));
    }

    @Test
    public void givenAppellantIssueFurtherEvidenceCallbackWithAppellantAndRep_shouldGenerateCoverLetterOriginalSenderAndOtherPartyRepAndDwpAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock();
        withRep();

        furtherEvidenceService.issue(caseData, APPELLANT_EVIDENCE);

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(REPRESENTATIVE_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDwpDocName));
        then(coverLetterService).should(times(3)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(3)).sendToBulkPrint(eq(pdfList), eq(caseData));
    }

    @Test
    public void givenRepIssueFurtherEvidenceCallbackWithAppellantRep_shouldGenerateCoverLetterOriginalSenderAndOtherPartyAppellantAndDwpAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock();
        withRep();

        furtherEvidenceService.issue(caseData, REPRESENTATIVE_EVIDENCE);

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(REPRESENTATIVE_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDwpDocName));
        then(coverLetterService).should(times(3)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(3)).sendToBulkPrint(eq(pdfList), eq(caseData));
    }

    @Test
    public void givenDwpIssueFurtherEvidenceCallbackWithAppellant_shouldGenerateCoverLetterOriginalSenderAndOtherPartyAppellantAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock();

        furtherEvidenceService.issue(caseData, DWP_EVIDENCE);

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData));
    }

    @Test
    public void givenDwpIssueFurtherEvidenceCallbackWithAppellantAndRep_shouldGenerateCoverLetterOriginalSenderAndOtherPartyAppellantAndRepAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock();
        withRep();

        furtherEvidenceService.issue(caseData, DWP_EVIDENCE);

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(REPRESENTATIVE_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(3)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(3)).sendToBulkPrint(eq(pdfList), eq(caseData));
    }

    private void createTestDataAndConfigureSscsDocumentServiceMock() {
        SscsDocument sscsDocument1WithAppellantEvidenceAndNoIssued = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType(APPELLANT_EVIDENCE.getValue())
                .evidenceIssued("No")
                .build())
            .build();

        caseData = SscsCaseData.builder()
            .ccdCaseId("1563382899630221")
            .sscsDocument(Collections.singletonList(sscsDocument1WithAppellantEvidenceAndNoIssued))
            .appeal(Appeal.builder().build())
            .build();

        doReturn(pdfList).when(sscsDocumentService).getPdfsForGivenDocTypeNotIssued(
            eq(Collections.singletonList(sscsDocument1WithAppellantEvidenceAndNoIssued)), any());
    }

    private void withRep() {
        caseData.getAppeal().setRep(Representative.builder().hasRepresentative("Yes").build());
    }

    @Test
    @Parameters(method = "generateDifferentTestScenarios")
    public void givenDocList_shouldBeHandledUnderCertainConditions(List<SscsDocument> documentList,
                                                                   boolean expected) {

        boolean actual = furtherEvidenceService.canHandleAnyDocument(documentList);

        assertEquals(expected, actual);
    }

    private Object[] generateDifferentTestScenarios() {

        SscsDocument sscsDocument1WithAppellantEvidenceAndNoIssued = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType(APPELLANT_EVIDENCE.getValue())
                .evidenceIssued("No")
                .build())
            .build();

        SscsDocument sscsDocument2WithAppellantEvidenceAndNoIssued = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType(APPELLANT_EVIDENCE.getValue())
                .evidenceIssued("No")
                .build())
            .build();

        SscsDocument sscsDocument3WithAppellantEvidenceAndYesIssued = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType(APPELLANT_EVIDENCE.getValue())
                .evidenceIssued("Yes")
                .build())
            .build();

        SscsDocument sscsDocument4WithRepEvidenceAndNoIssued = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType(REPRESENTATIVE_EVIDENCE.getValue())
                .evidenceIssued("No")
                .build())
            .build();

        return new Object[]{
            //happy path sceanrios
            new Object[]{Collections.singletonList(sscsDocument1WithAppellantEvidenceAndNoIssued), true},
            new Object[]{Collections.singletonList(sscsDocument3WithAppellantEvidenceAndYesIssued), false},
            new Object[]{Collections.singletonList(sscsDocument4WithRepEvidenceAndNoIssued), true},

            new Object[]{Arrays.asList(sscsDocument1WithAppellantEvidenceAndNoIssued,
                sscsDocument2WithAppellantEvidenceAndNoIssued), true},
            new Object[]{Arrays.asList(sscsDocument3WithAppellantEvidenceAndYesIssued,
                sscsDocument1WithAppellantEvidenceAndNoIssued), true},

            //edge scenarios
            new Object[]{null, false},
            new Object[]{Collections.singletonList(SscsDocument.builder().build()), false},
            new Object[]{Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder().build())
                .build()), false},
            new Object[]{Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                    .evidenceIssued("No")
                    .build())
                .build()), false},
            new Object[]{Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                    .evidenceIssued("No")
                    .documentType(null)
                    .build())
                .build()), false},
            new Object[]{Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                    .documentType(APPELLANT_EVIDENCE.getValue())
                    .build())
                .build()), false},
            new Object[]{Arrays.asList(null, sscsDocument1WithAppellantEvidenceAndNoIssued), true}
        };
    }

}
