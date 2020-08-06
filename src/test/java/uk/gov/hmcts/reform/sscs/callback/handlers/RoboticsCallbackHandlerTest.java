package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.RoboticsService;

@RunWith(JUnitParamsRunner.class)
public class RoboticsCallbackHandlerTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private RoboticsService roboticsService;

    @Mock
    private CcdService ccdService;

    @Mock
    private IdamService idamService;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Mock
    private CaseDetails caseDetails;

    @Mock
    private SscsCaseData caseData;

    private RoboticsCallbackHandler handler;

    private final LocalDateTime now = LocalDateTime.now();

    @Before
    public void setUp() {
        when(callback.getEvent()).thenReturn(EventType.VALID_APPEAL_CREATED);

        handler = new RoboticsCallbackHandler(roboticsService, ccdService, idamService, regionalProcessingCenterService);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(caseData.isTranslationWorkOutstanding()).thenReturn(Boolean.FALSE);
    }

    @Test
    @Parameters({"VALID_APPEAL", "INTERLOC_VALID_APPEAL", "READY_TO_LIST", "VALID_APPEAL_CREATED", "RESEND_CASE_TO_GAPS2", "APPEAL_TO_PROCEED"})
    public void givenAValidRoboticsEvent_thenReturnTrue(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);

        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenANonRoboticsEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    @Parameters({"VALID_APPEAL", "INTERLOC_VALID_APPEAL", "VALID_APPEAL_CREATED", "APPEAL_TO_PROCEED"})
    public void givenARoboticsRequestAndCreatedInGapsMatchesState_thenSendCaseToRoboticsAndSetSentToGapsDateAndDoNotTriggerUpdateCaseEvent(EventType eventType) {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(READY_TO_LIST, READY_TO_LIST.getId());
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), eventType, false);

        handler.handle(SUBMITTED, callback);

        verify(roboticsService).sendCaseToRobotics(any());

        assertEquals(LocalDate.now().toString(), callback.getCaseDetails().getCaseData().getDateCaseSentToGaps());
        verifyNoInteractions(ccdService);
    }

    @Test
    @Parameters({"READY_TO_LIST", "RESEND_CASE_TO_GAPS2"})
    public void givenARoboticsRequestAndCreatedInGapsMatchesState_thenSendCaseToRoboticsAndSetSentToGapsDateAndTriggerUpdateCaseEvent(EventType eventType) {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(READY_TO_LIST, READY_TO_LIST.getId());
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), eventType, false);

        handler.handle(SUBMITTED, callback);

        verify(roboticsService).sendCaseToRobotics(any());

        assertEquals(LocalDate.now().toString(), callback.getCaseDetails().getCaseData().getDateCaseSentToGaps());
        verify(ccdService).updateCase(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void givenARoboticsRequestAndCreatedInGapsDoesNotMatchState_thenDoNotSendCaseToRobotics() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(READY_TO_LIST, VALID_APPEAL.getId());
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.VALID_APPEAL_CREATED, false);

        handler.handle(SUBMITTED, callback);

        verifyNoInteractions(roboticsService);
    }

    @Test
    public void givenARoboticsRequestAndEventIsReissuetoGaps2_thenSendCaseToRobotics() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(APPEAL_CREATED, READY_TO_LIST.getId());
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.RESEND_CASE_TO_GAPS2, false);

        handler.handle(SUBMITTED, callback);

        verify(roboticsService).sendCaseToRobotics(any());
    }

    @Test
    public void givenARoboticsRequestAndCreatedInGapsFieldIsBlank_thenSendCaseToRobotics() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(APPEAL_CREATED, null);
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.VALID_APPEAL_CREATED, false);

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

    @Test
    public void givenSendToDWPWithDocTranslated_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.SEND_TO_DWP);

        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenDocTranslationOutstanding_thenReturnFalse() {
        when(caseData.isTranslationWorkOutstanding()).thenReturn(Boolean.TRUE);
        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenResendCaseToGaps2WithDocTranslationOutstanding_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.RESEND_CASE_TO_GAPS2);
        when(caseData.isTranslationWorkOutstanding()).thenReturn(Boolean.TRUE);
        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

}
