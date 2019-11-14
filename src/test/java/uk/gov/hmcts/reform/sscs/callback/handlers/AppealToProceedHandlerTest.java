package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.callback.handlers.HandlerHelper.buildTestCallbackForGivenData;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_TO_PROCEED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.INTERLOCUTORY_REVIEW_STATE;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@RunWith(JUnitParamsRunner.class)
public class AppealToProceedHandlerTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private CcdService ccdCaseService;

    @Mock
    private IdamService idamService;

    private AppealToProceedHandler handler;

    @Captor
    ArgumentCaptor<SscsCaseData> captor;

    @Before
    public void setUp() {
        handler = new AppealToProceedHandler(ccdCaseService, idamService);
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    public void givenCallbackIsNotSubmitted_willThrowAnException(CallbackType callbackType) {
        handler.handle(callbackType,
            buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, APPEAL_TO_PROCEED));
    }

    @Test
    public void givenAValidAppealToProceedEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(SUBMITTED, buildTestCallbackForGivenData(SscsCaseData.builder().build(), INTERLOCUTORY_REVIEW_STATE, APPEAL_TO_PROCEED)));
    }

    @Test
    public void givenANonAppealToProceedEvent_thenReturnFalse() {
        assertFalse(handler.canHandle(SUBMITTED, buildTestCallbackForGivenData(SscsCaseData.builder().build(), INTERLOCUTORY_REVIEW_STATE, DECISION_ISSUED)));
    }

    @Test(expected = NullPointerException.class)
    public void givenCallbackIsNull_whenCanHandleIsCalled_shouldThrowException() {
        handler.canHandle(SUBMITTED, null);
    }

    @Test
    public void givenAnAppealToProceedEvent_thenUpdateCase() {
        handler.handle(SUBMITTED, buildTestCallbackForGivenData(SscsCaseData.builder().interlocReviewState("test").build(), INTERLOCUTORY_REVIEW_STATE, APPEAL_TO_PROCEED));

        verify(ccdCaseService).updateCase(captor.capture(), eq(1L), eq(EventType.CASE_UPDATED.getCcdType()), eq("Appeal updated"), eq("Appeal proceed event triggered - clearing interlocutory review state"), any());

        assertEquals("none", captor.getValue().getInterlocReviewState());
    }
}
