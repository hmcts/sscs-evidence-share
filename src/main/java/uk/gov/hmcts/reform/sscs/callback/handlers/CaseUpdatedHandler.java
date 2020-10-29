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
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Slf4j
@Service
public class CaseUpdatedHandler  implements CallbackHandler<SscsCaseData> {
    private CcdService ccdService;
    private IdamService idamService;

    @Autowired
    public CaseUpdatedHandler(CcdService ccdService, IdamService idamService) {
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.CASE_UPDATED
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

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        BenefitType benefitType = caseData.getAppeal().getBenefitType();
        log.info("BenefitType" + benefitType);

        if (StringUtils.equalsIgnoreCase(benefitType.getCode(), "uc") && isANewJointParty(callback, caseData)) {
            ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                EventType.JOINT_PARTY_ADDED.getCcdType(), "Joint party added",
                "", idamService.getIdamTokens());
            log.info("jointPartyAdded event updated");
        }
    }



    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }

}
