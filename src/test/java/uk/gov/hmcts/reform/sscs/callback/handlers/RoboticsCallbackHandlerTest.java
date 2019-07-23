package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.APPEAL_CREATED;

import java.time.LocalDateTime;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
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
    public void givenACreateAppealPdfEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.CREATE_APPEAL_PDF);

        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenANonRoboticsEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenARoboticsRequest_thenSendCaseToRobotics() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(APPEAL_CREATED);
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.EVIDENCE_RECEIVED);

        handler.handle(SUBMITTED, callback);

        verify(roboticsService).sendCaseToRobotics(any());
    }

    private CaseDetails<SscsCaseData> getCaseDetails(State state) {
        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId("123")
            .appeal(Appeal.builder().build())
            .build();

        return new CaseDetails<>(123L, "jurisdiction", state, caseData, now);
    }
}
