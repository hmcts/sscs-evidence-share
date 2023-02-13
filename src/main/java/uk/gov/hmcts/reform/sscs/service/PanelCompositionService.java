package uk.gov.hmcts.reform.sscs.service;


import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;


@Service
public class PanelCompositionService {

    private final CcdService ccdService;
    private final IdamService idamService;

    @Autowired
    public PanelCompositionService(CcdService ccdService, IdamService idamService) {
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    public void processCaseState(Callback<SscsCaseData> callback, SscsCaseData caseData, EventType eventType) {
        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();

        if (stateNotDormant(caseDetails.getState())) {
            if (caseData.getIsFqpmRequired() == null
                || hasDueDateSetAndOtherPartyWithoutHearingOption(caseData)) {
                ccdService.updateCase(caseData, caseDetails.getId(),
                    EventType.NOT_LISTABLE.getCcdType(),
                    "Not listable",
                    "Update to Not Listable as the case is either awaiting hearing enquiry form or for FQPM to be set",
                    idamService.getIdamTokens());
            } else {
                if (eventType.equals(EventType.UPDATE_OTHER_PARTY_DATA)) {
                    caseData.setDirectionDueDate(null);
                }
                ccdService.updateCase(caseData, caseDetails.getId(),
                    EventType.READY_TO_LIST.getCcdType(),
                    "Ready to list",
                    "Update to ready to list event as there is no further information to assist the tribunal and no dispute.",
                    idamService.getIdamTokens());
            }
        }
    }

    private static boolean stateNotDormant(State caseState) {
        return !State.DORMANT_APPEAL_STATE.equals(caseState);
    }

    private boolean hasDueDateSetAndOtherPartyWithoutHearingOption(SscsCaseData sscsCaseData) {
        return StringUtils.isNotBlank(sscsCaseData.getDirectionDueDate())
            && !everyOtherPartyHasAtLeastOneHearingOption(sscsCaseData);
    }

    private boolean everyOtherPartyHasAtLeastOneHearingOption(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getOtherParties() != null && !sscsCaseData.getOtherParties().isEmpty()) {
            return sscsCaseData.getOtherParties().stream().noneMatch(this::hasNoHearingOption);
        } else {
            return false;
        }
    }

    private boolean hasNoHearingOption(CcdValue<OtherParty> otherPartyCcdValue) {
        HearingOptions hearingOptions = otherPartyCcdValue.getValue().getHearingOptions();
        return hearingOptions == null
            || (StringUtils.isBlank(hearingOptions.getWantsToAttend())
            && StringUtils.isBlank(hearingOptions.getWantsSupport())
            && StringUtils.isBlank(hearingOptions.getLanguageInterpreter())
            && StringUtils.isBlank(hearingOptions.getLanguages())
            && StringUtils.isBlank(hearingOptions.getSignLanguageType())
            && (hearingOptions.getArrangements() == null || hearingOptions.getArrangements().isEmpty())
            && StringUtils.isBlank(hearingOptions.getScheduleHearing())
            && (hearingOptions.getExcludeDates() == null || hearingOptions.getExcludeDates().isEmpty())
            && StringUtils.isBlank(hearingOptions.getAgreeLessNotice())
            && StringUtils.isBlank(hearingOptions.getOther()));
    }
}
