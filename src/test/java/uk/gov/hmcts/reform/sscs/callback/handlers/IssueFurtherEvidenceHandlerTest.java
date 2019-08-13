package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.callback.handlers.HandlerHelper.buildTestCallbackForGivenData;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.INTERLOCUTORY_REVIEW_STATE;

import java.util.Collections;
import junitparams.JUnitParamsRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.exception.RequiredFieldMissingException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.FurtherEvidenceService;

@RunWith(JUnitParamsRunner.class)
public class IssueFurtherEvidenceHandlerTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private FurtherEvidenceService furtherEvidenceService;

    @Mock
    private IdamService idamService;
    @Mock
    private CcdService ccdService;

    @InjectMocks
    private IssueFurtherEvidenceHandler issueAppellantAppointeeFurtherEvidenceHandler;

    @Captor
    ArgumentCaptor<SscsCaseData> captor;

    @Test(expected = NullPointerException.class)
    public void givenCallbackIsNull_whenHandleIsCalled_shouldThrowException() {
        issueAppellantAppointeeFurtherEvidenceHandler.handle(CallbackType.SUBMITTED, null);
    }

    @Test(expected = RequiredFieldMissingException.class)
    public void givenCaseDataInCallbackIsNull_shouldThrowException() {
        issueAppellantAppointeeFurtherEvidenceHandler.handle(CallbackType.SUBMITTED,
            buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, ISSUE_FURTHER_EVIDENCE));
    }

    @Test(expected = NullPointerException.class)
    public void givenCallbackIsNull_whenCanHandleIsCalled_shouldThrowException() {
        issueAppellantAppointeeFurtherEvidenceHandler.canHandle(CallbackType.SUBMITTED, null);
    }

    @Test(expected = RequiredFieldMissingException.class)
    public void givenCaseDataInCallbackIsNull_whenCanHandleIsCalled_shouldThrowException() {
        issueAppellantAppointeeFurtherEvidenceHandler.canHandle(CallbackType.SUBMITTED,
            buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, ISSUE_FURTHER_EVIDENCE));
    }

    @Test(expected = IllegalStateException.class)
    public void givenHandleMethodIsCalled_shouldThrowExceptionIfCanNotBeHandled() {
        given(furtherEvidenceService.canHandleAnyDocument(any())).willReturn(false);

        issueAppellantAppointeeFurtherEvidenceHandler.handle(CallbackType.SUBMITTED,
            buildTestCallbackForGivenData(SscsCaseData.builder().build(), INTERLOCUTORY_REVIEW_STATE, ISSUE_FURTHER_EVIDENCE));
    }

    @Test
    public void givenIssueFurtherEvidenceCallback_shouldIssueEvidenceForAppellantAndRepAndDwp() {
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());

        given(furtherEvidenceService.canHandleAnyDocument(any())).willReturn(true);

        SscsDocument sscsDocumentNotIssued = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType(APPELLANT_EVIDENCE.getValue())
                .evidenceIssued("No")
                .build())
            .build();

        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId("1563382899630221")
            .sscsDocument(Collections.singletonList(sscsDocumentNotIssued))
            .appeal(Appeal.builder().build())
            .build();

        issueAppellantAppointeeFurtherEvidenceHandler.handle(CallbackType.SUBMITTED,
            buildTestCallbackForGivenData(caseData, INTERLOCUTORY_REVIEW_STATE, ISSUE_FURTHER_EVIDENCE));

        verify(furtherEvidenceService).issue(eq(caseData), eq(APPELLANT_EVIDENCE));
        verify(furtherEvidenceService).issue(eq(caseData), eq(REPRESENTATIVE_EVIDENCE));
        verify(furtherEvidenceService).issue(eq(caseData), eq(DWP_EVIDENCE));

        verify(ccdService).updateCase(captor.capture(), any(Long.class), eq(EventType.UPDATE_CASE_ONLY.getCcdType()),
            any(), any(), any(IdamTokens.class));

        assertEquals("Yes", captor.getValue().getSscsDocument().get(0).getValue().getEvidenceIssued());
    }
}
