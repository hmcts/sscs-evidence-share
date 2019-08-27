package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.callback.handlers.HandlerHelper.buildTestCallbackForGivenData;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.REISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.INTERLOCUTORY_REVIEW_STATE;

import java.util.Collections;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
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
public class ReissueFurtherEvidenceHandlerTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private FurtherEvidenceService furtherEvidenceService;

    @Mock
    private IdamService idamService;
    @Mock
    private CcdService ccdService;

    @InjectMocks
    private ReissueFurtherEvidenceHandler handler;

    @Captor
    ArgumentCaptor<SscsCaseData> captor;

    @Test(expected = NullPointerException.class)
    public void givenCallbackIsNull_whenHandleIsCalled_shouldThrowException() {
        handler.handle(CallbackType.SUBMITTED, null);
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    public void givenCallbackIsNotSubmitted_willThrowAnException(CallbackType callbackType) {
        handler.handle(callbackType,
            buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, REISSUE_FURTHER_EVIDENCE));
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"ISSUE_FURTHER_EVIDENCE", "EVIDENCE_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenEventTypeIsNotReIssueFurtherEvidence_willThrowAnException(EventType eventType) {
        handler.handle(CallbackType.SUBMITTED,
            buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, eventType));
    }

    @Test(expected = RequiredFieldMissingException.class)
    public void givenCaseDataInCallbackIsNull_shouldThrowException() {
        handler.handle(CallbackType.SUBMITTED,
            buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, REISSUE_FURTHER_EVIDENCE));
    }

    @Test(expected = NullPointerException.class)
    public void givenCallbackIsNull_whenCanHandleIsCalled_shouldThrowException() {
        handler.canHandle(CallbackType.SUBMITTED, null);
    }

    @Test(expected = RequiredFieldMissingException.class)
    public void givenCaseDataInCallbackIsNull_whenCanHandleIsCalled_shouldThrowException() {
        handler.canHandle(CallbackType.SUBMITTED,
            buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, REISSUE_FURTHER_EVIDENCE));
    }

    @Test(expected = IllegalStateException.class)
    public void givenHandleMethodIsCalled_shouldThrowExceptionIfCanNotBeHandled() {
        given(furtherEvidenceService.canHandleAnyDocument(any())).willReturn(false);

        handler.handle(CallbackType.SUBMITTED,
            buildTestCallbackForGivenData(SscsCaseData.builder()
                .reissueFurtherEvidenceDocument(new DynamicList(new DynamicListItem("url", "label"), null))
                .build(), INTERLOCUTORY_REVIEW_STATE, REISSUE_FURTHER_EVIDENCE));
    }

    @Test
    @Parameters({"true, true, true",
        "false, false, false",
        "true, true, false",
        "true, false, false",
        "false, true, false",
        "false, false, true"})
    public void givenIssueFurtherEvidenceCallback_shouldReissueEvidenceForAppellantAndRepAndDwp(boolean resendToAppellant,
                                                                                                boolean resendToRepresentative,
                                                                                                boolean resendToDwp) {
        if (resendToAppellant || resendToDwp || resendToRepresentative) {
            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        }

        given(furtherEvidenceService.canHandleAnyDocument(any())).willReturn(true);

        SscsDocument sscsDocumentNotIssued = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentUrl("www.acme.co.uk").build())
                .documentType(APPELLANT_EVIDENCE.getValue())
                .evidenceIssued("No")
                .build())
            .build();

        DynamicListItem dynamicListItem = new DynamicListItem(
            sscsDocumentNotIssued.getValue().getDocumentLink().getDocumentUrl(), "a label");
        DynamicList dynamicList = new DynamicList(dynamicListItem, Collections.singletonList(dynamicListItem));
        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId("1563382899630221")
            .sscsDocument(Collections.singletonList(sscsDocumentNotIssued))
            .appeal(Appeal.builder().build())
            .reissueFurtherEvidenceDocument(dynamicList)
            .resendToAppellant(resendToAppellant ? "yes" : "no")
            .resendToRepresentative(resendToRepresentative ? "yes" : "no")
            .resendToDwp(resendToDwp ? "yes" : "no")
            .build();

        handler.handle(CallbackType.SUBMITTED,
            buildTestCallbackForGivenData(caseData, INTERLOCUTORY_REVIEW_STATE, REISSUE_FURTHER_EVIDENCE));

        verify(furtherEvidenceService).canHandleAnyDocument(eq(caseData.getSscsDocument()));
        if (resendToAppellant) {
            verify(furtherEvidenceService).issue(eq(Collections.singletonList(sscsDocumentNotIssued)), eq(caseData), eq(APPELLANT_EVIDENCE));
        }
        if (resendToRepresentative) {
            verify(furtherEvidenceService).issue(eq(Collections.singletonList(sscsDocumentNotIssued)), eq(caseData), eq(REPRESENTATIVE_EVIDENCE));
        }
        if (resendToDwp) {
            verify(furtherEvidenceService).issue(eq(Collections.singletonList(sscsDocumentNotIssued)), eq(caseData), eq(DWP_EVIDENCE));
        }
        verifyNoMoreInteractions(furtherEvidenceService);

        if (resendToAppellant || resendToDwp || resendToRepresentative) {
            verify(ccdService).updateCase(captor.capture(), any(Long.class), eq(EventType.UPDATE_CASE_ONLY.getCcdType()),
                any(), any(), any(IdamTokens.class));
            assertEquals("Yes", captor.getValue().getSscsDocument().get(0).getValue().getEvidenceIssued());
        } else {
            verifyZeroInteractions(ccdService);
        }

    }
}
