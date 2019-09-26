package uk.gov.hmcts.reform.sscs.callback.handlers;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.callback.handlers.HandlerHelper.buildTestCallbackForGivenData;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DWP_UPLOAD_RESPONSE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.INTERLOCUTORY_REVIEW_STATE;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.exception.RequiredFieldMissingException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;


@RunWith(JUnitParamsRunner.class)
public class DwpUploadResponseHandlerTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private IdamService idamService;
    @Mock
    private CcdService ccdService;

    private DwpUploadResponseHandler handler;

    @Before
    public void setup() {
        handler = new DwpUploadResponseHandler(true, ccdService, idamService);
    }

    @Test(expected = NullPointerException.class)
    public void givenCallbackIsNull_whenHandleIsCalled_shouldThrowException() {
        handler.handle(CallbackType.SUBMITTED, null);
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    public void givenCallbackIsNotSubmitted_willThrowAnException(CallbackType callbackType) {
        handler.handle(callbackType,
            buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE));
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"REISSUE_FURTHER_EVIDENCE", "EVIDENCE_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenEventTypeIsNotIssueFurtherEvidence_willThrowAnException(EventType eventType) {
        handler.handle(CallbackType.SUBMITTED,
            buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, eventType));
    }

    @Test(expected = RequiredFieldMissingException.class)
    public void givenCaseDataInCallbackIsNull_shouldThrowException() {
        handler.handle(CallbackType.SUBMITTED,
            buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE));
    }

    @Test(expected = NullPointerException.class)
    public void givenCallbackIsNull_whenCanHandleIsCalled_shouldThrowException() {
        handler.canHandle(CallbackType.SUBMITTED, null);
    }

    @Test
    public void givenCallbackIsOkay_thenCanHandleIsTrue() {
        assertTrue(handler.canHandle(CallbackType.SUBMITTED, buildTestCallbackForGivenData(SscsCaseData.builder().build(), INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE)));
    }

    @Test
    public void givenCallbackIsOkay_butFlagIsNotSet_thenCanHandleIsFalse() {
        handler = new DwpUploadResponseHandler(false, ccdService, idamService);
        assertFalse(handler.canHandle(CallbackType.SUBMITTED, buildTestCallbackForGivenData(SscsCaseData.builder().build(), INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE)));
    }

    @Test
    public void givenADwpUploadResponseEventWithDwpFurtherInfoIsNo_runReadyToListEvent() {
        final Callback<SscsCaseData> callback = buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").dwpFurtherInfo("No").build(), INTERLOCUTORY_REVIEW_STATE, DWP_UPLOAD_RESPONSE);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L).data(callback.getCaseDetails().getCaseData()).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(idamService).getIdamTokens();
        verify(ccdService).updateCase(eq(callback.getCaseDetails().getCaseData()), eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())), eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any());
    }

}
