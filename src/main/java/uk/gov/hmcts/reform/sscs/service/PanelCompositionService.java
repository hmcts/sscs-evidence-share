package uk.gov.hmcts.reform.sscs.service;


import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
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

    public SscsCaseDetails processCaseState(Callback<SscsCaseData> callback, SscsCaseData caseData, EventType eventType) {
        if (caseData.getIsFqpmRequired() == null
            || hasDueDateSetAndOtherPartyWithoutHearingOption(caseData)) {
            return ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                EventType.NOT_LISTABLE.getCcdType(), "Not listable",
                "Update to Not Listable as the case is either awaiting hearing enquiry form or for FQPM to be set", idamService.getIdamTokens());
        } else {
            if (eventType.equals(EventType.UPDATE_OTHER_PARTY_DATA)) {
                caseData.setDwpDueDate(null);
            }
            return ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                EventType.READY_TO_LIST.getCcdType(), "Ready to list",
                "Update to ready to list event as there is no further information to assist the tribunal and no dispute.", idamService.getIdamTokens());
        }
    }

    private boolean hasDueDateSetAndOtherPartyWithoutHearingOption(SscsCaseData sscsCaseData) {
        return StringUtils.isNotBlank(sscsCaseData.getDwpDueDate())
            && !everyOtherPartyHasAtLeastOneHearingOption(sscsCaseData);
    }

    private boolean everyOtherPartyHasAtLeastOneHearingOption(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getOtherParties() != null) {
            return sscsCaseData.getOtherParties().stream().noneMatch(otherParty -> otherParty.getValue().getHearingOptions() == null);
        } else {
            return false;
        }
    }
}
