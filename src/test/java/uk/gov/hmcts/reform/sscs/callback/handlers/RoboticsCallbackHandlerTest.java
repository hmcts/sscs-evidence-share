package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;

import java.time.LocalDateTime;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.RoboticsService;

@RunWith(JUnitParamsRunner.class)
public class RoboticsCallbackHandlerTest {

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private RoboticsService roboticsService;

    private RoboticsCallbackHandler handler;

    private LocalDateTime now = LocalDateTime.now();

    @Before
    public void setUp() {
        initMocks(this);
        when(callback.getEvent()).thenReturn(EventType.SEND_TO_DWP);

        handler = new RoboticsCallbackHandler(roboticsService);

    }

    @Test
    public void givenASendToDwpEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenAValidAppealEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.VALID_APPEAL);

        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenAInterlocValidAppealEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.INTERLOC_VALID_APPEAL);

        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenAResendCaseToGaps2Event_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.RESEND_CASE_TO_GAPS2);

        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenANonRoboticsEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenARoboticsRequestAndCreatedInGapsMatchesState_thenSendCaseToRobotics() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(READY_TO_LIST, "readyToList");
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.SEND_TO_DWP);

        handler.handle(SUBMITTED, callback);

        verify(roboticsService).sendCaseToRobotics(any());
    }

    @Test
    public void givenARoboticsRequestAndCreatedInGapsDoesNotMatchState_thenDoNotSendCaseToRobotics() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(READY_TO_LIST, "validAppeal");
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.SEND_TO_DWP);

        handler.handle(SUBMITTED, callback);

        verifyNoInteractions(roboticsService);
    }

    @Test
    public void givenARoboticsRequestAndEventIsReissuetoGaps2_thenSendCaseToRobotics() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(APPEAL_CREATED, "readyToList");
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.RESEND_CASE_TO_GAPS2);

        handler.handle(SUBMITTED, callback);

        verify(roboticsService).sendCaseToRobotics(any());
    }

    @Test
    public void givenARoboticsRequestAndCreatedInGapsFieldIsBlank_thenSendCaseToRobotics() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(APPEAL_CREATED, null);
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.SEND_TO_DWP);

        handler.handle(SUBMITTED, callback);

        verify(roboticsService).sendCaseToRobotics(any());
    }

    private CaseDetails<SscsCaseData> getCaseDetails(State state, String createdInGapsFrom) {
        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId("123")
            .appeal(Appeal.builder().build())
            .createdInGapsFrom(createdInGapsFrom)
            .build();

        return new CaseDetails<>(123L, "jurisdiction", state, caseData, now);
    }
}
