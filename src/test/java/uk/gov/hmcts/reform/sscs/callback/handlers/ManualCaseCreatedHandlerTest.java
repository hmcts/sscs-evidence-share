package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.callback.handlers.HandlerHelper.buildCallbackWithSupplementaryData;
import static uk.gov.hmcts.reform.sscs.callback.handlers.HandlerHelper.buildTestCallbackForGivenData;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;

import java.util.HashMap;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;


@RunWith(JUnitParamsRunner.class)
public class ManualCaseCreatedHandlerTest {
    @Mock
    private CcdService ccdCaseService;

    @Mock
    private IdamService idamService;

    private ManualCaseCreatedHandler handler;

    private String setKey = "$set";

    @Before
    public void setUp() {
        openMocks(this);
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        handler = new ManualCaseCreatedHandler(ccdCaseService,
            idamService);
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    public void givenCallbackIsNotSubmitted_willThrowAnException(CallbackType callbackType) {
        handler.handle(callbackType,
            buildTestCallbackForGivenData(null, READY_TO_LIST, VALID_APPEAL_CREATED));
    }

    @Test
    @Parameters({"VALID_APPEAL_CREATED", "INCOMPLETE_APPLICATION_RECEIVED", "NON_COMPLIANT"})
    public void givenAQualifyingEvent_thenReturnTrue(EventType eventType) {
        assertTrue(handler.canHandle(SUBMITTED, buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(READY_TO_LIST.getId()).build(), READY_TO_LIST, eventType)));
    }

    @Test
    public void givenANonQualifyingEvent_thenReturnFalse() {
        assertFalse(handler.canHandle(SUBMITTED, buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(READY_TO_LIST.getId()).build(), READY_TO_LIST, DECISION_ISSUED)));
    }

    @Test(expected = NullPointerException.class)
    public void givenCallbackIsNull_whenCanHandleIsCalled_shouldThrowException() {
        handler.canHandle(SUBMITTED, null);
    }

    @Test
    public void givenNullSupplementaryData_addServiceId() {
        handler.handle(SUBMITTED, buildCallbackWithSupplementaryData(SscsCaseData.builder().createdInGapsFrom(READY_TO_LIST.getId()).build(), READY_TO_LIST, VALID_APPEAL_CREATED, null));

        verify(ccdCaseService).setSupplementaryData(any(), any(), eq(getWrappedData(getSupplementaryData())));
    }

    @Test
    public void givenOtherSupplementaryData_addServiceId() {
        Map<String, Map<String, Object>> suppMap = getSupplementaryData();
        suppMap.remove(setKey);
        Map<String, Object> otherMap = new HashMap();
        otherMap.put("other", "thing");
        suppMap.put("$something", otherMap);
        handler.handle(SUBMITTED, buildCallbackWithSupplementaryData(SscsCaseData.builder().createdInGapsFrom(READY_TO_LIST.getId()).build(), READY_TO_LIST, VALID_APPEAL_CREATED, suppMap));

        Map<String, Map<String, Object>> finalMap = getSupplementaryData();
        Map<String, Object> otherMapFinal = new HashMap();
        otherMapFinal.put("other", "thing");
        finalMap.put("$something", otherMapFinal);

        verify(ccdCaseService).setSupplementaryData(any(), any(), eq(getWrappedData(finalMap)));
    }

    @Test
    public void givenOtherSetData_addServiceIdRetainOtherData() {
        Map<String, Map<String, Object>> suppMap = getSupplementaryData();
        suppMap.remove(setKey);
        Map<String, Object> otherMap = new HashMap();
        otherMap.put("other", "thing");
        suppMap.put(setKey, otherMap);
        handler.handle(SUBMITTED, buildCallbackWithSupplementaryData(SscsCaseData.builder().createdInGapsFrom(READY_TO_LIST.getId()).build(), READY_TO_LIST, VALID_APPEAL_CREATED, suppMap));

        Map<String, Map<String, Object>> finalMap = getSupplementaryData();
        Map<String, Object> otherMapFinal = new HashMap();
        otherMapFinal.put("other", "thing");
        otherMapFinal.put("HMCTSServiceId", "BBA3");
        finalMap.put(setKey, otherMapFinal);

        verify(ccdCaseService).setSupplementaryData(any(), any(), eq(getWrappedData(finalMap)));
    }

    @Test
    public void givenExistingSupplementaryData_dontChange() {
        handler.handle(SUBMITTED, buildCallbackWithSupplementaryData(SscsCaseData.builder().createdInGapsFrom(READY_TO_LIST.getId()).build(), READY_TO_LIST, VALID_APPEAL_CREATED, getSupplementaryData()));

        verify(ccdCaseService, never()).setSupplementaryData(any(), any(), eq(getWrappedData(getSupplementaryData())));
    }

    public Map<String, Map<String, Object>> getSupplementaryData() {
        Map<String, Object> hmctsServiceIdMap = new HashMap<>();
        hmctsServiceIdMap.put("HMCTSServiceId", "BBA3");
        Map<String, Map<String, Object>> supplementaryDataRequestMap = new HashMap<>();
        supplementaryDataRequestMap.put(setKey, hmctsServiceIdMap);
        return supplementaryDataRequestMap;
    }

    public Map<String, Map<String, Map<String, Object>>> getWrappedData(Map<String, Map<String, Object>> supplementaryData) {
        Map<String, Map<String, Map<String, Object>>> supplementaryDataUpdates = new HashMap<>();
        supplementaryDataUpdates.put("supplementary_data_updates", supplementaryData);
        return supplementaryDataUpdates;
    }

}
