package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DWP_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REPRESENTATIVE_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.APPELLANT_LETTER;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.DWP_LETTER;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.REPRESENTATIVE_LETTER;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.DocmosisTemplateConfig;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;

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
    @Mock
    private DocmosisTemplateConfig docmosisTemplateConfig;

    private FurtherEvidenceService furtherEvidenceService;

    private SscsCaseData caseData;
    private final List<Pdf> pdfList = Collections.singletonList(new Pdf(new byte[]{}, "some doc name"));

    private String furtherEvidenceOriginalSenderTemplateName = "TB-SCS-GNO-ENG-00068.doc";
    private String furtherEvidenceOriginalSenderWelshTemplateName = "TB-SCS-GNO-WEL-00469.docx";
    private String furtherEvidenceOriginalSenderDocName = "609-97-template (original sender)";
    private String furtherEvidenceOtherPartiesTemplateName = "TB-SCS-GNO-ENG-00069.doc";
    private String furtherEvidenceOtherPartiesWelshTemplateName = "TB-SCS-GNO-WEL-00470.docx";
    private String furtherEvidenceOtherPartiesDocName = "609-98-template (other parties)";
    private String furtherEvidenceOtherPartiesDwpDocName = "609-98-template (DWP)";
    Map<LanguagePreference, Map<String, Map<String, String>>> template =  new HashMap<>();

    @Before
    public void setup() {
        Map<String, String> nameMap;
        Map<String, Map<String, String>> englishDocs = new HashMap<>();
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-ENG-00010.doc");
        englishDocs.put(DocumentType.DL6.getValue(), nameMap);
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-ENG-00011.doc");
        englishDocs.put(DocumentType.DL16.getValue(), nameMap);
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-ENG-00068.doc");
        englishDocs.put("d609-97", nameMap);
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-ENG-00069.doc");
        englishDocs.put("d609-98", nameMap);

        Map<String, Map<String, String>> welshDocs = new HashMap<>();
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-ENG-00010.doc");
        welshDocs.put(DocumentType.DL6.getValue(), nameMap);
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-ENG-00011.doc");
        welshDocs.put(DocumentType.DL16.getValue(), nameMap);
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-WEL-00469.docx");
        welshDocs.put("d609-97", nameMap);
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-WEL-00470.docx");
        welshDocs.put("d609-98", nameMap);

        template.put(LanguagePreference.ENGLISH, englishDocs);
        template.put(LanguagePreference.WELSH, welshDocs);

        furtherEvidenceService = new FurtherEvidenceService(coverLetterService, sscsDocumentService, bulkPrintService,
                docmosisTemplateConfig);
    }

    @Test
    public void givenAppellantIssueFurtherEvidenceCallbackWithAppellantAndNoRep_shouldGenerateCoverLetterOriginalSenderAndOtherPartyDwpAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No");
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(),caseData, APPELLANT_EVIDENCE,
            Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, DWP_LETTER));

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDwpDocName));
        then(coverLetterService).should(times(1)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any());
    }

    @Test
    public void givenAppellantIssueFurtherEvidenceCallbackWithAppellantAndNoRep_whenLanguageIsWelsh_shouldGenerateWelshCoverLetterOriginalSenderAndOtherPartyDwpAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("Yes");
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(),caseData, APPELLANT_EVIDENCE,
            Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, DWP_LETTER));

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOriginalSenderWelshTemplateName),
                eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOtherPartiesWelshTemplateName),
                eq(furtherEvidenceOtherPartiesDwpDocName));
        then(coverLetterService).should(times(1)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any());
    }

    @Test
    public void givenAppellantIssueFurtherEvidenceCallbackWithAppellantAndRep_shouldGenerateCoverLetterOriginalSenderAndOtherPartyRepAndDwpAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No");
        withRep();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(),caseData, APPELLANT_EVIDENCE,
            Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, DWP_LETTER));

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(REPRESENTATIVE_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDwpDocName));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any());
    }

    @Test
    public void givenAppellantIssueFurtherEvidenceCallbackWithAppellantAndRep_whenLanguageIsWelsh_shouldGenerateWelshCoverLetterOriginalSenderAndOtherPartyRepAndDwpAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("Yes");
        withRep();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(),caseData, APPELLANT_EVIDENCE,
            Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, DWP_LETTER));

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOriginalSenderWelshTemplateName),
                eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(REPRESENTATIVE_LETTER),
                eq(furtherEvidenceOtherPartiesWelshTemplateName), eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOtherPartiesWelshTemplateName),
                eq(furtherEvidenceOtherPartiesDwpDocName));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any());
    }

    @Test
    public void givenRepIssueFurtherEvidenceCallbackWithAppellantRep_shouldGenerateCoverLetterOriginalSenderAndOtherPartyAppellantAndDwpAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No");
        withRep();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(),caseData, REPRESENTATIVE_EVIDENCE,
            Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, DWP_LETTER));

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(REPRESENTATIVE_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDwpDocName));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any());
    }

    @Test
    public void givenRepIssueFurtherEvidenceCallbackWithAppellantRep_whenLanguageIsWelsh_shouldGenerateWelshCoverLetterOriginalSenderAndOtherPartyAppellantAndDwpAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("Yes");
        withRep();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(),caseData, REPRESENTATIVE_EVIDENCE,
            Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, DWP_LETTER));

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(REPRESENTATIVE_LETTER),
                eq(furtherEvidenceOriginalSenderWelshTemplateName), eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesWelshTemplateName),
                eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOtherPartiesWelshTemplateName),
                eq(furtherEvidenceOtherPartiesDwpDocName));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any());
    }

    @Test
    public void givenDwpIssueFurtherEvidenceCallbackWithAppellant_shouldGenerateCoverLetterOriginalSenderAndOtherPartyAppellantAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No");
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(),caseData, DWP_EVIDENCE,
            Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER));

        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(1)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any());
    }

    @Test
    public void givenDwpIssueFurtherEvidenceCallbackWithAppellant_whenLanguageIsWelsh_shouldGenerateWelshCoverLetterOriginalSenderAndOtherPartyAppellantAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("Yes");
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(),caseData, DWP_EVIDENCE,
            Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER));

        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOriginalSenderWelshTemplateName),
                eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesWelshTemplateName),
                eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(1)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any());
    }

    @Test
    public void givenDwpIssueFurtherEvidenceCallbackWithAppellantAndRep_shouldGenerateCoverLetterOriginalSenderAndOtherPartyAppellantAndRepAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No");
        withRep();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(),caseData, DWP_EVIDENCE,
            Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER));

        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(REPRESENTATIVE_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any());
    }

    @Test
    public void givenDwpIssueFurtherEvidenceCallbackWithAppellantAndRep_whenLanguageIsWelsh_shouldGenerateWelshCoverLetterOriginalSenderAndOtherPartyAppellantAndRepAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("Yes");
        withRep();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(),caseData, DWP_EVIDENCE,
            Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER));

        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOriginalSenderWelshTemplateName),
                eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesWelshTemplateName),
                eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(REPRESENTATIVE_LETTER),
                eq(furtherEvidenceOtherPartiesWelshTemplateName), eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any());
    }

    @Test
    @Parameters({"APPELLANT_LETTER", "REPRESENTATIVE_LETTER", "DWP_LETTER" })
    public void givenIssueForParty_shouldGenerateCoverLetterForSelectedParty(FurtherEvidenceLetterType furtherEvidenceLetterType) {
        createTestDataAndConfigureSscsDocumentServiceMock("No");
        withRep();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, DWP_EVIDENCE, Collections.singletonList(furtherEvidenceLetterType));

        String templateName = furtherEvidenceOtherPartiesTemplateName;
        String docName = furtherEvidenceOtherPartiesDocName;
        if (furtherEvidenceLetterType.equals(DWP_LETTER)) {
            templateName = furtherEvidenceOriginalSenderTemplateName;
            docName = furtherEvidenceOriginalSenderDocName;
        }
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(furtherEvidenceLetterType), eq(templateName), eq(docName));
        then(coverLetterService).should(times(1)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any());
        then(coverLetterService).shouldHaveNoMoreInteractions();
        then(bulkPrintService).shouldHaveNoMoreInteractions();
    }

    @Test
    @Parameters({"APPELLANT_LETTER", "REPRESENTATIVE_LETTER", "DWP_LETTER" })
    public void givenIssueForParty_whenLanguageIsWelsh_shouldGenerateWelshCoverLetterForSelectedParty(FurtherEvidenceLetterType furtherEvidenceLetterType) {
        createTestDataAndConfigureSscsDocumentServiceMock("Yes");
        withRep();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, DWP_EVIDENCE, Collections.singletonList(furtherEvidenceLetterType));

        String templateName = furtherEvidenceOtherPartiesWelshTemplateName;
        String docName = furtherEvidenceOtherPartiesDocName;
        if (furtherEvidenceLetterType.equals(DWP_LETTER)) {
            templateName = furtherEvidenceOriginalSenderWelshTemplateName;
            docName = furtherEvidenceOriginalSenderDocName;
        }
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(furtherEvidenceLetterType), eq(templateName), eq(docName));
        then(coverLetterService).should(times(1)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any());
        then(coverLetterService).shouldHaveNoMoreInteractions();
        then(bulkPrintService).shouldHaveNoMoreInteractions();
    }

    private void createTestDataAndConfigureSscsDocumentServiceMock(String languagePrefernceFlag) {
        SscsDocument sscsDocument1WithAppellantEvidenceAndNoIssued = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType(APPELLANT_EVIDENCE.getValue())
                .evidenceIssued("No")
                .build())
            .build();

        caseData = SscsCaseData.builder()
            .ccdCaseId("1563382899630221")
            .languagePreferenceWelsh(languagePrefernceFlag)
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

    @SuppressWarnings("unused")
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
