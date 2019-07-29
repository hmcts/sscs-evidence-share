package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REPRESENTATIVE_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SEND_TO_DWP;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

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
    private IdamService idamService;
    @Mock
    private CcdService ccdService;

    @InjectMocks
    private FurtherEvidenceService furtherEvidenceService;

    private SscsCaseData caseData;
    private final List<Pdf> pdfList = Collections.singletonList(new Pdf(new byte[]{}, "some doc name"));

    @Test
    public void givenIssueFurtherEvidenceCallback_shouldGenerateCoverLetterAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock();

        furtherEvidenceService.issue(caseData, APPELLANT_EVIDENCE);

        then(coverLetterService).should(times(1))
            .generate609_97_OriginalSenderCoverLetter(eq(caseData), eq(APPELLANT_EVIDENCE));
        then(coverLetterService).should(times(1)).appendCoverLetter(any(), anyList());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData));
    }

    @Test
    public void givenIssueFurtherEvidenceCallback_shouldUpdateEvidenceIssuedPropToYesInCcd() {
        createTestDataAndConfigureSscsDocumentServiceMock();

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());

        furtherEvidenceService.issue(caseData, APPELLANT_EVIDENCE);

        assertEquals("Yes", caseData.getSscsDocument().get(0).getValue().getEvidenceIssued());
        verify(sscsDocumentService, times(1)).filterByDocTypeAndApplyAction(anyList(),
            eq(APPELLANT_EVIDENCE), any());

        verify(ccdService, times(1)).updateCase(
            eq(caseData),
            any(Long.class),
            eq(EventType.UPDATE_CASE_ONLY.getCcdType()),
            any(),
            any(),
            any(IdamTokens.class));
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
            .build();

        doReturn(pdfList).when(sscsDocumentService).getPdfsForGivenDocType(
            eq(Collections.singletonList(sscsDocument1WithAppellantEvidenceAndNoIssued)), eq(APPELLANT_EVIDENCE));
    }

    @Test
    @Parameters(method = "generateDifferentTestScenarios")
    public void givenDocList_shouldBeHandledUnderCertainConditions(List<SscsDocument> documentList,
                                                                   DocumentType documentType,
                                                                   EventType eventType, boolean expected) {

        boolean actual = furtherEvidenceService.canHandleAnyDocument(eventType, documentList, documentType);

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
            new Object[]{Collections.singletonList(sscsDocument1WithAppellantEvidenceAndNoIssued), APPELLANT_EVIDENCE,
                ISSUE_FURTHER_EVIDENCE, true},
            new Object[]{Collections.singletonList(sscsDocument3WithAppellantEvidenceAndYesIssued), APPELLANT_EVIDENCE,
                ISSUE_FURTHER_EVIDENCE, false},
            new Object[]{Collections.singletonList(sscsDocument4WithRepEvidenceAndNoIssued), REPRESENTATIVE_EVIDENCE,
                ISSUE_FURTHER_EVIDENCE, true},

            new Object[]{Arrays.asList(sscsDocument1WithAppellantEvidenceAndNoIssued,
                sscsDocument2WithAppellantEvidenceAndNoIssued), APPELLANT_EVIDENCE, ISSUE_FURTHER_EVIDENCE, true},
            new Object[]{Arrays.asList(sscsDocument1WithAppellantEvidenceAndNoIssued,
                sscsDocument2WithAppellantEvidenceAndNoIssued), REPRESENTATIVE_EVIDENCE, ISSUE_FURTHER_EVIDENCE, false},
            new Object[]{Arrays.asList(sscsDocument3WithAppellantEvidenceAndYesIssued,
                sscsDocument1WithAppellantEvidenceAndNoIssued), APPELLANT_EVIDENCE, ISSUE_FURTHER_EVIDENCE, true},

            //edge scenarios

            new Object[]{Collections.singletonList(sscsDocument1WithAppellantEvidenceAndNoIssued),
                APPELLANT_EVIDENCE, SEND_TO_DWP, false},
            new Object[]{null, APPELLANT_EVIDENCE, ISSUE_FURTHER_EVIDENCE, false},
            new Object[]{Collections.singletonList(SscsDocument.builder().build()), APPELLANT_EVIDENCE,
                ISSUE_FURTHER_EVIDENCE, false},
            new Object[]{Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder().build())
                .build()), APPELLANT_EVIDENCE, ISSUE_FURTHER_EVIDENCE, false},
            new Object[]{Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                    .evidenceIssued("No")
                    .build())
                .build()), APPELLANT_EVIDENCE, ISSUE_FURTHER_EVIDENCE, false},
            new Object[]{Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                    .evidenceIssued("No")
                    .documentType(null)
                    .build())
                .build()), APPELLANT_EVIDENCE, ISSUE_FURTHER_EVIDENCE, false},
            new Object[]{Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                    .documentType(APPELLANT_EVIDENCE.getValue())
                    .build())
                .build()), APPELLANT_EVIDENCE, ISSUE_FURTHER_EVIDENCE, false},
            new Object[]{Arrays.asList(null, sscsDocument1WithAppellantEvidenceAndNoIssued), APPELLANT_EVIDENCE,
                ISSUE_FURTHER_EVIDENCE, true}
        };
    }

}
