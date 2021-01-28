package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.callback.handlers.HandlerUtils.isANewJointParty;
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
    public DwpUploadResponseHandler(CcdService ccdService,
                                    IdamService idamService) {
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

        if (callback.getCaseDetails().getCaseData().getAppeal() == null
            || callback.getCaseDetails().getCaseData().getAppeal().getBenefitType() == null) {
            log.info("Cannot handle this event as no data");
            throw new IllegalStateException("Cannot handle callback");
        }

        BenefitType benefitType = callback.getCaseDetails().getCaseData().getAppeal().getBenefitType();
        log.info("BenefitType" + benefitType);

        if (StringUtils.equalsIgnoreCase(benefitType.getCode(), "uc")) {
            handleUc(callback);
        } else {
            handleNonUc(callback);
        }
    }

    private void handleNonUc(Callback<SscsCaseData> callback) {
        if ("Yes".equalsIgnoreCase(callback.getCaseDetails().getCaseData().getUrgentCase())) {
            log.info("urgent case updating to responseReceived");
            SscsCaseData caseData = setDwpState(callback);
            ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                EventType.DWP_RESPOND.getCcdType(), "Response received",
                "urgent hearing set to response received event", idamService.getIdamTokens());
        } else if (StringUtils.equalsIgnoreCase(callback.getCaseDetails().getCaseData().getDwpFurtherInfo(), "no")) {
            log.info("updating to ready to list");

            SscsCaseData caseData = setDwpState(callback);

            ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                EventType.READY_TO_LIST.getCcdType(), "ready to list",
                "update to ready to list event as there is no further information to assist the tribunal and no dispute.", idamService.getIdamTokens());
        }
    }

    private SscsCaseData setDwpState(Callback<SscsCaseData> callback) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        caseData.setDwpState(DwpState.RESPONSE_SUBMITTED_DWP.getId());
        return caseData;
    }

    private void handleUc(Callback<SscsCaseData> callback) {
        boolean dwpFurtherInfo =
            StringUtils.equalsIgnoreCase(callback.getCaseDetails().getCaseData().getDwpFurtherInfo(), "yes");

        boolean disputedDecision = false;
        if (callback.getCaseDetails().getCaseData().getElementsDisputedIsDecisionDisputedByOthers() != null) {
            disputedDecision = StringUtils.equalsIgnoreCase(callback.getCaseDetails().getCaseData().getElementsDisputedIsDecisionDisputedByOthers(), "yes");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        if ("Yes".equalsIgnoreCase(callback.getCaseDetails().getCaseData().getUrgentCase())) {
            log.info("urgent case updating to responseReceived");
            caseData = setDwpState(callback);
            ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                EventType.DWP_RESPOND.getCcdType(), "Response received",
                "urgent hearing set to response received event", idamService.getIdamTokens());

        } else if (!dwpFurtherInfo
            && !disputedDecision) {
            log.info("updating to ready to list");

            caseData = setDwpState(callback);

            ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                EventType.READY_TO_LIST.getCcdType(), "ready to list",
                "update to ready to list event as there is no further information to assist the tribunal and no dispute.", idamService.getIdamTokens());
        } else {
            log.info("updating to response received");

            String description = null;
            if (dwpFurtherInfo && disputedDecision) {
                description = "update to response received event as there is further information to "
                    + "assist the tribunal and there is a dispute.";
            } else if (dwpFurtherInfo) {
                description = "update to response received event as there is further information to "
                    + "assist the tribunal.";
            } else {
                description = "update to response received event as there is a dispute.";
            }

            caseData.setDwpState(DwpState.RESPONSE_SUBMITTED_DWP.getId());

            ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                EventType.DWP_RESPOND.getCcdType(), "Response received",
                description, idamService.getIdamTokens());
        }

        if (isANewJointParty(callback, caseData)) {
            ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                EventType.JOINT_PARTY_ADDED.getCcdType(), "Joint party added",
                "A joint party was added to the appeal", idamService.getIdamTokens());
            log.info("jointPartyAdded event updated");
        }
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
