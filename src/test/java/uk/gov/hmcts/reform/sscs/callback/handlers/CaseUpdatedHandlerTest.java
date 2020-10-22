package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.callback.handlers.HandlerHelper.buildTestCallbackForGivenData;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CASE_UPDATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.INTERLOCUTORY_REVIEW_STATE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(JUnitParamsRunner.class)
public class CaseUpdatedHandlerTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private IdamService idamService;
    @Mock
    private CcdService ccdService;

    private CaseUpdatedHandler handler;

    @Before
    public void setUp() {
        handler = new CaseUpdatedHandler(ccdService, idamService);
    }

    @Test(expected = NullPointerException.class)
    public void givenCallbackIsNull_whenHandleIsCalled_shouldThrowException() {
        handler.handle(CallbackType.SUBMITTED, null);
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    public void givenCallbackIsNotSubmitted_willThrowAnException(CallbackType callbackType) {
        handler.handle(callbackType,
            buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, CASE_UPDATED));
    }

    @Test(expected = IllegalStateException.class)
    public void givenCallbackHasNoBenefitType_willThrowAnException() {
        final Callback<SscsCaseData> callback = buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).jointParty("Yes")
                .appeal(Appeal.builder()
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, CASE_UPDATED);
        handler.handle(CallbackType.SUBMITTED, callback);
    }

    @Test(expected = IllegalStateException.class)
    public void givenCallbackHasNullAppeal_willThrowAnException() {
        final Callback<SscsCaseData> callback = buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).jointParty("Yes")
                .build(), INTERLOCUTORY_REVIEW_STATE, CASE_UPDATED);
        handler.handle(CallbackType.SUBMITTED, callback);
    }

    @Test
    public void givenCallbackWithMatchingParams_returnsTrue() {
        final Callback<SscsCaseData> callback = buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).jointParty("Yes")
                .appeal(Appeal.builder()
                    .benefitType(BenefitType.builder().code("UC").build())
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, CASE_UPDATED);
        assertTrue(handler.canHandle(CallbackType.SUBMITTED, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    public void givenAValidAppealReceivedEventForNonDigitalCase_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(VALID_APPEAL.getId()).build(), INTERLOCUTORY_REVIEW_STATE, CASE_UPDATED)));
    }

    @Test
    public void givenACaseUpdatedWithJointParty_runJointPartyAddedEvent() {
        final Callback<SscsCaseData> callback = buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).jointParty("Yes")
                .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("UC").build())
                .build()).build(), INTERLOCUTORY_REVIEW_STATE, CASE_UPDATED);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L)
            .data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(ccdService).updateCase(eq(callback.getCaseDetails().getCaseData()),
            eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())), eq(EventType.JOINT_PARTY_ADDED.getCcdType()), anyString(), anyString(), any());
    }

    @Test
    public void givenACaseUpdatedWithJointPartyNo_dontRunJointPartyAddedEvent() {
        final Callback<SscsCaseData> callback = buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId()).jointParty("No")
                .appeal(Appeal.builder()
                    .benefitType(BenefitType.builder().code("UC").build())
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, CASE_UPDATED);

        handler.handle(CallbackType.SUBMITTED, callback);

        verifyNoInteractions(idamService);
        verifyNoInteractions(ccdService);
    }

    @Test
    public void givenACaseUpdatedWithJointPartyNull_dontRunJointPartyAddedEvent() {
        final Callback<SscsCaseData> callback = buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").createdInGapsFrom(State.READY_TO_LIST.getId())
                .appeal(Appeal.builder()
                    .benefitType(BenefitType.builder().code("UC").build())
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, CASE_UPDATED);

        handler.handle(CallbackType.SUBMITTED, callback);

        verifyNoInteractions(idamService);
        verifyNoInteractions(ccdService);
    }
}
