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

import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

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
            .generate609_97_OriginalSenderCoverLetter(eq(caseData));
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

}
