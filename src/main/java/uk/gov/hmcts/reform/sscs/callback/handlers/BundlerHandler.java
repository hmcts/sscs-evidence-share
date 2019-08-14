package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.bundling.SscsBundlingAndStitchingService;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Slf4j
@Service
public class BundlerHandler implements CallbackHandler<SscsCaseData> {

    private final SscsBundlingAndStitchingService sscsBundlingAndStitchingService;
    private final boolean bundleStitchingFeature;

    @Autowired
    public BundlerHandler(SscsBundlingAndStitchingService sscsBundlingAndStitchingService,
                          @Value("${feature.bundle-stitching.enabled}") boolean bundleStitchingFeature) {
        this.sscsBundlingAndStitchingService = sscsBundlingAndStitchingService;
        this.bundleStitchingFeature = bundleStitchingFeature;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.CREATE_BUNDLE
            && bundleStitchingFeature;
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        sscsBundlingAndStitchingService.bundleAndStitch(caseData);
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.EARLY;
    }
}
