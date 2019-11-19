package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.callback.handlers.HandlerHelper.buildTestCallbackForGivenData;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@RunWith(JUnitParamsRunner.class)
public class IssueDirectionHandlerTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private CcdService ccdCaseService;

    @Mock
    private IdamService idamService;

    private IssueDirectionHandler handler;

    @Captor
    ArgumentCaptor<SscsCaseData> captor;

    @Before
    public void setUp() {
        handler = new IssueDirectionHandler(ccdCaseService, idamService);
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    public void givenCallbackIsNotSubmitted_willThrowAnException(CallbackType callbackType) {
        handler.handle(callbackType,
            buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, DIRECTION_ISSUED));
    }

    @Test
    public void givenAValidDirectionIssuedEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(SUBMITTED, buildTestCallbackForGivenData(SscsCaseData.builder().directionType(DirectionType.APPEAL_TO_PROCEED).build(), INTERLOCUTORY_REVIEW_STATE, DIRECTION_ISSUED)));
    }

    @Test
    public void givenANonDirectionIssuedEvent_thenReturnFalse() {
        assertFalse(handler.canHandle(SUBMITTED, buildTestCallbackForGivenData(SscsCaseData.builder().directionType(DirectionType.APPEAL_TO_PROCEED).build(), INTERLOCUTORY_REVIEW_STATE, APPEAL_RECEIVED)));
    }

    @Test(expected = NullPointerException.class)
    public void givenCallbackIsNull_whenCanHandleIsCalled_shouldThrowException() {
        handler.canHandle(SUBMITTED, null);
    }

    @Test
    public void givenAnIssueDirectionEvent_thenTriggerAppealToProceedEvent() {
        handler.handle(SUBMITTED, buildTestCallbackForGivenData(SscsCaseData.builder().directionType(DirectionType.APPEAL_TO_PROCEED).build(), INTERLOCUTORY_REVIEW_STATE, DIRECTION_ISSUED));

        verify(ccdCaseService).updateCase(captor.capture(), eq(1L), eq(EventType.APPEAL_TO_PROCEED.getCcdType()), eq("Appeal to proceed"), eq("Appeal proceed event triggered"), any());

        assertNull((captor.getValue().getDirectionType()));
    }
}
