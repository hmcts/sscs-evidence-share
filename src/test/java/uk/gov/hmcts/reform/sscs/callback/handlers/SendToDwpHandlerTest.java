package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.callback.handlers.HandlerHelper.buildTestCallbackForGivenData;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@RunWith(JUnitParamsRunner.class)
public class SendToDwpHandlerTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private CcdService ccdCaseService;

    @Mock
    private IdamService idamService;

    private SendToDwpHandler handler;

    @Before
    public void setUp() {
        handler = new SendToDwpHandler(ccdCaseService, idamService, true);
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    public void givenCallbackIsNotSubmitted_willThrowAnException(CallbackType callbackType) {
        handler.handle(callbackType,
            buildTestCallbackForGivenData(null, State.VALID_APPEAL, VALID_APPEAL_CREATED));
    }

    @Test
    @Parameters({"VALID_APPEAL_CREATED", "SYA_APPEAL_CREATED"})
    public void givenAValidAppealCreatedEvent_thenReturnTrue(EventType eventType) {
        assertTrue(handler.canHandle(SUBMITTED, buildTestCallbackForGivenData(SscsCaseData.builder().build(), State.VALID_APPEAL, eventType)));
    }

    @Test
    public void givenANonAppealCreatedEvent_thenReturnFalse() {
        assertFalse(handler.canHandle(SUBMITTED, buildTestCallbackForGivenData(SscsCaseData.builder().build(), State.VALID_APPEAL, DECISION_ISSUED)));
    }

    @Test(expected = NullPointerException.class)
    public void givenCallbackIsNull_whenCanHandleIsCalled_shouldThrowException() {
        handler.canHandle(SUBMITTED, null);
    }

    @Test
    @Parameters({"VALID_APPEAL_CREATED", "SYA_APPEAL_CREATED"})
    public void givenAnAppealCreatedEvent_thenUpdateCaseWithSendCaseToDwp(EventType eventType) {
        handler.handle(SUBMITTED, buildTestCallbackForGivenData(SscsCaseData.builder().interlocReviewState("test").build(), State.VALID_APPEAL, eventType));

        verify(ccdCaseService).updateCase(any(), eq(1L), eq(SEND_TO_DWP.getCcdType()), eq("Send to DWP"), eq("Send to DWP event has been triggered from Evidence share service"), any());
    }

    @Test
    public void givenBulkScanMigratedFeatureFlagOff_thenDoNotSendCaseToDwp() {
        handler = new SendToDwpHandler(ccdCaseService, idamService, false);

        assertFalse(handler.canHandle(SUBMITTED, buildTestCallbackForGivenData(SscsCaseData.builder().interlocReviewState("test").build(), State.VALID_APPEAL, VALID_APPEAL_CREATED)));
    }
}
