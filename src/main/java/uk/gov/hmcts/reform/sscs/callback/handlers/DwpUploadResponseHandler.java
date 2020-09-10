package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Slf4j
@Service
public class DwpUploadResponseHandler implements CallbackHandler<SscsCaseData> {

    private CcdService ccdService;
    private IdamService idamService;

    @Autowired
    public DwpUploadResponseHandler(CcdService ccdService, IdamService idamService) {
        this.ccdService = ccdService;
        this.idamService = idamService;

    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.DWP_UPLOAD_RESPONSE
            && READY_TO_LIST.getId().equals(callback.getCaseDetails().getCaseData().getCreatedInGapsFrom());
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            log.info("Cannot handle this event");
            throw new IllegalStateException("Cannot handle callback");
        }

        BenefitType benefitType = callback.getCaseDetails().getCaseData().getAppeal().getBenefitType();
        log.info("BenefitType" + benefitType);

        if (benefitType.getCode().equals("UC")) {
            handleUc(callbackType, callback);
        } else {
            handleNonUc(callbackType, callback);
        }
    }

    private void handleNonUc(CallbackType callbackType, Callback<SscsCaseData> callback) {
        boolean notDwpFurtherInfo =
            StringUtils.equalsIgnoreCase(callback.getCaseDetails().getCaseData().getDwpFurtherInfo(), "no");

        if (notDwpFurtherInfo) {
            log.info("updating to ready to list");

            SscsCaseData caseData = callback.getCaseDetails().getCaseData();

            caseData.setDwpState(DwpState.RESPONSE_SUBMITTED_DWP.getId());

            ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                EventType.READY_TO_LIST.getCcdType(), "ready to list",
                "update to ready to list event as there is no further information to assist the tribunal and no dispute.", idamService.getIdamTokens());
        }
    }

    private void handleUc(CallbackType callbackType, Callback<SscsCaseData> callback) {
        boolean notDwpFurtherInfo =
            StringUtils.equalsIgnoreCase(callback.getCaseDetails().getCaseData().getDwpFurtherInfo(), "no");

        boolean notDisputedDecision = true;

        notDisputedDecision = callback.getCaseDetails().getCaseData().getElementsDisputedIsDecisionDisputedByOthers() == null
            || StringUtils.equalsIgnoreCase(callback.getCaseDetails().getCaseData().getElementsDisputedIsDecisionDisputedByOthers(), "no");

        if (notDwpFurtherInfo
            && notDisputedDecision) {
            log.info("updating to ready to list");

            SscsCaseData caseData = callback.getCaseDetails().getCaseData();

            caseData.setDwpState(DwpState.RESPONSE_SUBMITTED_DWP.getId());

            ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                EventType.READY_TO_LIST.getCcdType(), "ready to list",
                "update to ready to list event as there is no further information to assist the tribunal and no dispute.", idamService.getIdamTokens());
        } else {
            log.info("updating to response received");

            String description = null;
            if (!notDwpFurtherInfo && !notDisputedDecision) {
                description = "update to response received event as there is further information to "
                    + "assist the tribunal and there is a dispute.";
            } else if (!notDwpFurtherInfo) {
                description = "update to response received event as there is further information to "
                    + "assist the tribunal.";
            } else if (!notDisputedDecision) {
                description = "update to response received event as there is a dispute.";
            }

            SscsCaseData caseData = callback.getCaseDetails().getCaseData();

            caseData.setDwpState(DwpState.RESPONSE_SUBMITTED_DWP.getId());

            ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                EventType.DWP_RESPOND.getCcdType(), "Response received",
                description, idamService.getIdamTokens());
        }
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
