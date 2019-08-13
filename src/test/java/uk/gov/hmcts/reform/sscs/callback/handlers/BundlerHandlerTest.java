package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.callback.handlers.HandlerHelper.buildTestCallbackForGivenData;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CREATE_BUNDLE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;

import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.bundling.SscsBundlingAndStitchingService;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class BundlerHandlerTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private SscsBundlingAndStitchingService sscsBundlingAndStitchingService;

    private BundlerHandler handler;

    @Before
    public void setUp() {
        handler = new BundlerHandler(sscsBundlingAndStitchingService, true);
    }

    @Test
    public void givenAStitchBundleEvent_thenReturnTrue() {
        Callback<SscsCaseData> callback = buildTestCallbackForGivenData(null, VALID_APPEAL, CREATE_BUNDLE);

        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenANonBulkPrintEvent_thenReturnFalse() {
        Callback<SscsCaseData> callback = buildTestCallbackForGivenData(null, VALID_APPEAL, APPEAL_RECEIVED);

        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenFeatureFlagIsOff_thenReturnFalse() {
        Callback<SscsCaseData> callback = buildTestCallbackForGivenData(null, VALID_APPEAL, CREATE_BUNDLE);

        handler = new BundlerHandler(sscsBundlingAndStitchingService, false);

        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenAValidCallback_thenSendToBundleAndStitchService() {
        Callback<SscsCaseData> callback = buildTestCallbackForGivenData(SscsCaseData.builder().build(), VALID_APPEAL, CREATE_BUNDLE);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(sscsBundlingAndStitchingService).bundleAndStitch(callback.getCaseDetails().getCaseData());
    }
}
