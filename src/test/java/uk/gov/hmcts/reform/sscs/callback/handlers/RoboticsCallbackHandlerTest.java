package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;
import uk.gov.hmcts.reform.sscs.service.RoboticsService;

@RunWith(JUnitParamsRunner.class)
public class RoboticsCallbackHandlerTest {

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private RoboticsService roboticsService;

    private DwpAddressLookupService dwpAddressLookupService;

    private RoboticsCallbackHandler handler;

    private LocalDateTime now = LocalDateTime.now();

    private List<String> offices;

    @Before
    public void setUp() {
        initMocks(this);
        when(callback.getEvent()).thenReturn(EventType.SEND_TO_DWP);

        offices = new ArrayList<>();
        offices.add("1");
        dwpAddressLookupService = new DwpAddressLookupService();
        handler = new RoboticsCallbackHandler(roboticsService, dwpAddressLookupService, true, offices);

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
    public void givenARoboticsRequestAndReadyToListFeatureFalse_thenSendCaseToRobotics() {
        handler = new RoboticsCallbackHandler(roboticsService, dwpAddressLookupService, false, offices);

        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(APPEAL_CREATED, "Pip", "1");
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.SEND_TO_DWP);

        handler.handle(SUBMITTED, callback);

        verify(roboticsService).sendCaseToRobotics(any());
    }

    @Test
    public void givenARoboticsRequestAndReadyToListFeatureTrueAndCaseIsAppealCreatedAndPipAndOfficeSelectedForReadyToList_thenDoNotSendCaseToRobotics() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(APPEAL_CREATED, "Pip", "1");
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.SEND_TO_DWP);

        handler.handle(SUBMITTED, callback);

        verifyNoMoreInteractions(roboticsService);
    }

    @Test
    public void givenARoboticsRequestFromSyaAndReadyToListFeatureTrueAndCaseIsAppealCreatedAndPipAndOfficeSelectedForReadyToList_thenDoNotSendCaseToRobotics() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(APPEAL_CREATED, "Pip", "DWP PIP Office(1)");
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.SEND_TO_DWP);

        handler.handle(SUBMITTED, callback);

        verifyNoMoreInteractions(roboticsService);
    }

    @Test
    public void givenARoboticsRequestFromSyaAndReadyToListFeatureTrueAndCaseIsAppealCreatedAndPipAndOfficeSelectedForReadyToList_thenDoNotSendCaseToRobotics2() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(INTERLOCUTORY_REVIEW_STATE, "Pip", "DWP PIP Office(2)");
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.SEND_TO_DWP);

        handler.handle(SUBMITTED, callback);

        verify(roboticsService).sendCaseToRobotics(any());
    }

    @Test
    public void givenARoboticsRequestAndReadyToListFeatureTrueAndCaseIsAppealCreatedAndPipAndOfficeSelectedDoesNotExist_thenDoNotSendCaseToRobotics() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(APPEAL_CREATED, "Pip", "Dummy office");
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.SEND_TO_DWP);

        handler.handle(SUBMITTED, callback);

        verifyNoMoreInteractions(roboticsService);
    }

    @Test
    public void givenARoboticsRequestAndReadyToListFeatureTrueAndCaseIsAppealCreatedAndPipAndOfficeNotSelectedForReadyToList_thenSendCaseToRobotics() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(APPEAL_CREATED, "Pip", "2");
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.SEND_TO_DWP);

        handler.handle(SUBMITTED, callback);

        verify(roboticsService).sendCaseToRobotics(any());
    }

    @Test
    public void givenARoboticsRequestAndReadyToListFeatureTrueAndCaseIsAppealCreatedAndEsa_thenSendCaseToRobotics() {
        handler = new RoboticsCallbackHandler(roboticsService, dwpAddressLookupService, true, offices);

        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(APPEAL_CREATED, "Esa", "1");
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.SEND_TO_DWP);

        handler.handle(SUBMITTED, callback);

        verify(roboticsService).sendCaseToRobotics(any());
    }

    @Test
    public void givenARoboticsRequestAndReadyToListFeatureTrueAndCaseIsReadyToListAndPipAndOfficeSelectedForReadyToList_thenSendCaseToRobotics() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(READY_TO_LIST, "Pip", "1");
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.SEND_TO_DWP);

        handler.handle(SUBMITTED, callback);

        verify(roboticsService).sendCaseToRobotics(any());
    }

    @Test
    public void givenARoboticsRequestAndReadyToListFeatureTrueAndEsaCase_theSendCaseToRobotics() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(APPEAL_CREATED, "Esa", "Watford DRT");
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.SEND_TO_DWP);

        handler.handle(SUBMITTED, callback);

        verify(roboticsService).sendCaseToRobotics(any());
    }

    @Test
    public void givenARoboticsRequestAndReadyToListFeatureTrueAndEventIsReissuetoGaps2_thenSendCaseToRobotics() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(APPEAL_CREATED, "Pip", "1");
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.RESEND_CASE_TO_GAPS2);

        handler.handle(SUBMITTED, callback);

        verify(roboticsService).sendCaseToRobotics(any());
    }

    private CaseDetails<SscsCaseData> getCaseDetails(State state, String benefitTypeCode, String office) {
        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId("123")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefitTypeCode).build())
            .mrnDetails(MrnDetails.builder().dwpIssuingOffice(office).build()).build())
            .createdInGapsFrom(benefitTypeCode.equalsIgnoreCase("pip") ? State.READY_TO_LIST.getId() : State.VALID_APPEAL.getId())
            .build();

        return new CaseDetails<>(123L, "jurisdiction", state, caseData, now);
    }
}
