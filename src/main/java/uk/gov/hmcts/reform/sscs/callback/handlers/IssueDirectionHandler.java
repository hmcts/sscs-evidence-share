package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_TO_PROCEED;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Slf4j
@Service
public class IssueDirectionHandler implements CallbackHandler<SscsCaseData> {

    private final DispatchPriority dispatchPriority;

    private final CcdService ccdService;

    private final IdamService idamService;

    @Autowired
    public IssueDirectionHandler(CcdService ccdService,
                                 IdamService idamService) {
        this.dispatchPriority = DispatchPriority.EARLY;
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.DIRECTION_ISSUED
            && DirectionType.APPEAL_TO_PROCEED.equals(callback.getCaseDetails().getCaseData().getDirectionType());
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        caseData.setDirectionType(null);

        log.info("About to update case with appealToProceed event for id {}", callback.getCaseDetails().getId());
        ccdService.updateCase(callback.getCaseDetails().getCaseData(), callback.getCaseDetails().getId(), APPEAL_TO_PROCEED.getCcdType(), "Appeal to proceed", "Appeal proceed event triggered",  idamService.getIdamTokens());
    }

    @Override
    public DispatchPriority getPriority() {
        return this.dispatchPriority;
    }
}
