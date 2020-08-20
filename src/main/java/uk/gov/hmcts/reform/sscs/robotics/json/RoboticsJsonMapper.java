package uk.gov.hmcts.reform.sscs.robotics.json;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.json.simple.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.AirlookupBenefitToVenue;
import uk.gov.hmcts.reform.sscs.model.dwp.OfficeMapping;
import uk.gov.hmcts.reform.sscs.robotics.domain.RoboticsWrapper;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

@Component
@Slf4j
public class RoboticsJsonMapper {

    private static final String YES = "Yes";
    private static final String ESA_CASE_CODE = "051DD";
    private static final String PIP_CASE_CODE = "002DD";

    private final DwpAddressLookupService dwpAddressLookupService;
    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final AirLookupService airLookupService;
    private final boolean ucEnabled;

    @Autowired
    public RoboticsJsonMapper(DwpAddressLookupService dwpAddressLookupService,
                              RegionalProcessingCenterService regionalProcessingCenterService,
                              AirLookupService airLookupService,
                              @Value("${universal-credit.enabled}") boolean ucEnabled) {
        this.dwpAddressLookupService = dwpAddressLookupService;
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.airLookupService = airLookupService;
        this.ucEnabled = ucEnabled;
    }

    public JSONObject map(RoboticsWrapper roboticsWrapper) {

        SscsCaseData sscsCaseData = roboticsWrapper.getSscsCaseData();

        JSONObject obj = buildAppealDetails(new JSONObject(), sscsCaseData);

        obj.put("caseId", roboticsWrapper.getCcdCaseId());
        obj.put("evidencePresent", roboticsWrapper.getEvidencePresent());
        obj.put("caseCode", getCaseCode(sscsCaseData));

        if (!isAppointeeDetailsEmpty(sscsCaseData.getAppeal().getAppellant().getAppointee()) && "yes".equalsIgnoreCase(sscsCaseData.getAppeal().getAppellant().getIsAppointee())) {
            boolean sameAddressAsAppointee = "Yes".equalsIgnoreCase(sscsCaseData.getAppeal().getAppellant().getIsAddressSameAsAppointee());
            obj.put("appointee", buildAppointeeDetails(sscsCaseData.getAppeal().getAppellant().getAppointee(), sameAddressAsAppointee));
        }

        obj.put("appellant", buildAppellantDetails(sscsCaseData.getAppeal().getAppellant()));

        if (sscsCaseData.getAppeal().getRep() != null
            && sscsCaseData.getAppeal().getRep().getHasRepresentative() != null
            && sscsCaseData.getAppeal().getRep().getHasRepresentative().equals("Yes")) {
            obj.put("representative", buildRepresentativeDetails(sscsCaseData.getAppeal().getRep()));
        }

        if (sscsCaseData.getAppeal().getHearingOptions() != null) {
            JSONObject hearingArrangements = buildHearingOptions(sscsCaseData.getAppeal().getHearingOptions());
            if (hearingArrangements.length() > 0) {
                obj.put("hearingArrangements", hearingArrangements);
            }
        }

        addRpcEmail(sscsCaseData.getRegionalProcessingCenter(), obj);

        String isReadyToList = "No";
        if (roboticsWrapper.getState().equals(State.READY_TO_LIST)) {
            isReadyToList = "Yes";
        }
        String isDigital = "No";
        if (StringUtils.equals(roboticsWrapper.getSscsCaseData().getCreatedInGapsFrom(), ("readyToList"))) {
            isDigital = "Yes";
        }
        obj.put("isReadyToList", isReadyToList);
        obj.put("isDigital", isDigital);

        obj.put("dwpResponseDate", sscsCaseData.getDwpResponseDate());

        Optional<OfficeMapping> officeMapping = buildOffice(obj, sscsCaseData.getAppeal());

        String dwpIssuingOffice = "";
        String dwpPresentingOffice = "";

        if (!officeMapping.isPresent()) {
            log.error("could not find dwp officeAddress for benefitType {} and dwpIssuingOffice {} so could not set dwp offices in robotics",
                sscsCaseData.getAppeal().getBenefitType().getCode(), sscsCaseData.getAppeal().getMrnDetails().getDwpIssuingOffice());
        }

        if (sscsCaseData.getDwpOriginatingOffice() != null && sscsCaseData.getDwpOriginatingOffice().getValue().getLabel() != null) {
            dwpIssuingOffice = sscsCaseData.getDwpOriginatingOffice().getValue().getLabel();
        } else if (officeMapping.isPresent()) {
            dwpIssuingOffice = officeMapping.get().getMapping().getGaps();
        }

        if (sscsCaseData.getDwpPresentingOffice() != null && sscsCaseData.getDwpPresentingOffice().getValue().getLabel() != null) {
            dwpPresentingOffice = sscsCaseData.getDwpPresentingOffice().getValue().getLabel();
        } else if (officeMapping.isPresent()) {
            dwpPresentingOffice = officeMapping.get().getMapping().getGaps();
        }

        String dwpIsOfficerAttending = sscsCaseData.getDwpIsOfficerAttending() != null ? sscsCaseData.getDwpIsOfficerAttending() : "No";
        String dwpUcb = sscsCaseData.getDwpUcb() != null ? sscsCaseData.getDwpUcb() : "No";

        obj.put("dwpIssuingOffice", dwpIssuingOffice);
        obj.put("dwpPresentingOffice", dwpPresentingOffice);
        obj.put("dwpIsOfficerAttending", dwpIsOfficerAttending);
        obj.put("dwpUcb", dwpUcb);


        if (ucEnabled) {

            JSONObject elementsDisputed = buildElementsDisputedLists(obj, sscsCaseData);
            if (elementsDisputed.length() > 0) {
                obj.put("elementsDisputed", elementsDisputed);
            }

            if (null != sscsCaseData.getJointParty() && "Yes".equalsIgnoreCase(sscsCaseData.getJointParty())) {
                obj.put("jointParty", buildJointPartyDetails(sscsCaseData.getJointPartyName(), sscsCaseData.getJointPartyAddress(), sscsCaseData.isJointPartyAddressSameAsAppeallant(),
                    sscsCaseData.getJointPartyIdentity().getDob(), sscsCaseData.getJointPartyIdentity().getNino()));
            }
            if (sscsCaseData.getElementsDisputedIsDecisionDisputedByOthers() != null) {
                obj.put("ucDecisionDisputedByOthers", sscsCaseData.getElementsDisputedIsDecisionDisputedByOthers());
            }
            if (sscsCaseData.getElementsDisputedLinkedAppealRef() != null) {
                obj.put("linkedAppealRef", sscsCaseData.getElementsDisputedLinkedAppealRef());
            }
        }

        return obj;
    }

    private void addRpcEmail(RegionalProcessingCenter rpc, JSONObject obj) {
        if (rpc != null && rpc.getEmail() != null) {
            obj.put("rpcEmail", rpc.getEmail());
        }
    }

    private JSONObject buildAppealDetails(JSONObject obj, SscsCaseData sscsCaseData) {
        Appeal appeal = sscsCaseData.getAppeal();
        obj.put("appellantNino", appeal.getAppellant().getIdentity().getNino());

        obj.put("appellantPostCode", findVenueName(sscsCaseData));

        if (sscsCaseData.getCaseCreated() != null) {
            obj.put("appealDate", sscsCaseData.getCaseCreated());
        } else {
            obj.put("appealDate", LocalDate.now().toString());
        }

        obj.put("receivedVia", appeal.getReceivedVia());

        if (appeal.getMrnDetails() != null) {
            if (appeal.getMrnDetails().getMrnDate() != null) {
                obj.put("mrnDate", appeal.getMrnDetails().getMrnDate());
            }
            if (appeal.getMrnDetails().getMrnLateReason() != null) {
                obj.put("mrnReasonForBeingLate", appeal.getMrnDetails().getMrnLateReason());
            }
        }

        Optional<OfficeMapping> officeMapping = buildOffice(obj, appeal);

        if (officeMapping.isPresent()) {
            obj.put("pipNumber", officeMapping.get().getMapping().getGaps());
        } else {
            log.error("could not find dwp officeAddress for benefitType {} and dwpIssuingOffice {}",
                appeal.getBenefitType().getCode(), appeal.getMrnDetails().getDwpIssuingOffice());
        }

        obj.put("hearingType", convertBooleanToPaperOral(appeal.getHearingOptions().isWantsToAttendHearing()));

        if (Boolean.TRUE.equals(appeal.getHearingOptions().isWantsToAttendHearing())) {
            obj.put("hearingRequestParty", appeal.getAppellant().getName().getFullName());
        }

        return obj;
    }

    private String findVenueName(SscsCaseData sscsCaseData) {
        String postcodeToUse = "yes".equalsIgnoreCase(sscsCaseData.getAppeal().getAppellant().getIsAppointee())
            ? sscsCaseData.getAppeal().getAppellant().getAppointee().getAddress().getPostcode()
            : sscsCaseData.getAppeal().getAppellant().getAddress().getPostcode();

        String firstHalfOfPostcode = regionalProcessingCenterService.getFirstHalfOfPostcode(postcodeToUse);

        AirlookupBenefitToVenue venue = airLookupService.lookupAirVenueNameByPostCode(firstHalfOfPostcode);
        return sscsCaseData.getAppeal().getBenefitType().getCode().equalsIgnoreCase("pip") ? venue.getPipVenue() : venue.getEsaOrUcVenue();
    }

    private Optional<OfficeMapping> buildOffice(JSONObject obj, Appeal appeal) {
        return dwpAddressLookupService.getDwpMappingByOffice(appeal.getBenefitType().getCode(),
            appeal.getMrnDetails().getDwpIssuingOffice());
    }

    private static String getCaseCode(SscsCaseData sscsCaseData) {

        if (StringUtils.isNotEmpty(sscsCaseData.getCaseCode())) {
            return sscsCaseData.getCaseCode();
            // Leave this in for now, whilst we have legacy cases where the case code is not set.
            // This will be an issue for cases where the caseworker tries to regenerate the robotics json. Can remove after a few weeks I suspect.
        } else if (StringUtils.equalsIgnoreCase("esa", sscsCaseData.getAppeal().getBenefitType().getCode())) {
            return ESA_CASE_CODE;
        } else {
            return PIP_CASE_CODE;
        }
    }

    private static JSONObject buildAppellantDetails(Appellant appellant) {
        JSONObject json = new JSONObject();

        buildName(json, appellant.getName().getTitle(), appellant.getName().getFirstName(), appellant.getName().getLastName());

        return buildContactDetails(json, appellant.getAddress(), appellant.getContact());
    }

    private static JSONObject buildAppointeeDetails(Appointee appointee, boolean sameAddressAsAppellant) {
        JSONObject json = new JSONObject();

        buildName(json, appointee.getName().getTitle(), appointee.getName().getFirstName(), appointee.getName().getLastName());

        json.put("sameAddressAsAppellant", sameAddressAsAppellant ? "Yes" : "No");

        return buildContactDetails(json, appointee.getAddress(), appointee.getContact());
    }

    private static JSONObject buildJointPartyDetails(JointPartyName jointPartyName, Address jointPartyAddress, boolean sameAddressAsAppellant,
                                                     String dob, String nino) {
        JSONObject json = new JSONObject();

        buildName(json, jointPartyName.getTitle(), jointPartyName.getFirstName(), jointPartyName.getLastName());

        json.put("sameAddressAsAppellant", sameAddressAsAppellant ? "Yes" : "No");
        json.put("dob", dob);
        json.put("nino", nino);

        return buildContactDetails(json, jointPartyAddress, null);
    }

    private static JSONObject buildRepresentativeDetails(Representative rep) {
        JSONObject json = new JSONObject();

        String title = rep.getName().getTitle() != null ? rep.getName().getTitle() : "s/m";
        String firstName = rep.getName().getFirstName() != null ? rep.getName().getFirstName() : ".";
        String lastName = rep.getName().getLastName() != null ? rep.getName().getLastName() : ".";

        buildName(json, title, firstName, lastName);

        if (rep.getOrganisation() != null) {
            json.put("organisation", rep.getOrganisation());
        }

        return buildContactDetails(json, rep.getAddress(), rep.getContact());
    }

    private static void buildName(JSONObject json, String title, String firstName, String lastName) {
        json.put("title", title);
        json.put("firstName", firstName);
        json.put("lastName", lastName);
    }

    @SuppressWarnings("unchecked")
    private static JSONObject buildHearingOptions(HearingOptions hearingOptions) {
        JSONObject hearingArrangements = new JSONObject();

        if (hearingOptions.getArrangements() != null) {

            if (hearingOptions.getLanguageInterpreter() != null && hearingOptions.getLanguageInterpreter().equals(YES) && hearingOptions.getLanguages() != null) {
                hearingArrangements.put("languageInterpreter", hearingOptions.getLanguages());
            }

            if (Boolean.TRUE.equals(hearingOptions.wantsSignLanguageInterpreter()) && hearingOptions.getSignLanguageType() != null) {
                hearingArrangements.put("signLanguageInterpreter", hearingOptions.getSignLanguageType());
            }

            hearingArrangements.put("hearingLoop", convertBooleanToYesNo(hearingOptions.wantsHearingLoop()));
            hearingArrangements.put("accessibleHearingRoom", convertBooleanToYesNo(hearingOptions.wantsAccessibleHearingRoom()));
        } else if (hearingOptions.getOther() != null || hearingOptions.getExcludeDates() != null) {

            hearingArrangements.put("hearingLoop", convertBooleanToYesNo(false));
            hearingArrangements.put("accessibleHearingRoom", convertBooleanToYesNo(false));
        }

        if (hearingOptions.getOther() != null) {
            hearingArrangements.put("other", hearingOptions.getOther());
        }

        if (hearingOptions.getExcludeDates() != null
            && hearingOptions.getExcludeDates().size() > 0) {
            JSONArray datesCantAttendArray = new JSONArray();
            for (ExcludeDate a : hearingOptions.getExcludeDates()) {
                if (!isBlank(a.getValue().getStart())) {
                    // Assume start and end date are always the same
                    datesCantAttendArray.add(getLocalDate(a.getValue().getStart()));
                }
            }

            hearingArrangements.put("datesCantAttend", datesCantAttendArray);
        }

        return hearingArrangements;
    }

    private static JSONObject buildContactDetails(JSONObject json, Address address, Contact contact) {
        json.put("addressLine1", address.getLine1());

        if (address.getLine2() != null) {
            json.put("addressLine2", address.getLine2());
        }

        json.put("townOrCity", address.getTown());
        json.put("county", address.getCounty());
        json.put("postCode", address.getPostcode());

        if (contact != null) {
            json.put("phoneNumber", contact.getMobile());
            json.put("email", contact.getEmail());
        }

        return json;
    }

    private static JSONObject buildElementsDisputedLists(JSONObject json, SscsCaseData sscsCaseData) {

        JSONObject elementsDisputed = new JSONObject();

        if (sscsCaseData.getElementsDisputedGeneral() != null && sscsCaseData.getElementsDisputedGeneral().size() > 0) {
            elementsDisputed.put("general", buildElementIssueArray(sscsCaseData.getElementsDisputedGeneral()));
        }

        if (sscsCaseData.getElementsDisputedSanctions() != null && sscsCaseData.getElementsDisputedSanctions().size() > 0) {
            elementsDisputed.put("sanctions", buildElementIssueArray(sscsCaseData.getElementsDisputedSanctions()));
        }

        if (sscsCaseData.getElementsDisputedOverpayment() != null && sscsCaseData.getElementsDisputedOverpayment().size() > 0) {
            elementsDisputed.put("overpayment", buildElementIssueArray(sscsCaseData.getElementsDisputedOverpayment()));
        }

        if (sscsCaseData.getElementsDisputedHousing() != null && sscsCaseData.getElementsDisputedHousing().size() > 0) {
            elementsDisputed.put("housing", buildElementIssueArray(sscsCaseData.getElementsDisputedHousing()));
        }

        if (sscsCaseData.getElementsDisputedChildCare() != null && sscsCaseData.getElementsDisputedChildCare().size() > 0) {
            elementsDisputed.put("childCare", buildElementIssueArray(sscsCaseData.getElementsDisputedChildCare()));
        }

        if (sscsCaseData.getElementsDisputedCare() != null && sscsCaseData.getElementsDisputedCare().size() > 0) {
            elementsDisputed.put("care", buildElementIssueArray(sscsCaseData.getElementsDisputedCare()));
        }

        if (sscsCaseData.getElementsDisputedChildElement() != null && sscsCaseData.getElementsDisputedChildElement().size() > 0) {
            elementsDisputed.put("childElement", buildElementIssueArray(sscsCaseData.getElementsDisputedChildElement()));
        }

        if (sscsCaseData.getElementsDisputedChildDisabled() != null && sscsCaseData.getElementsDisputedChildDisabled().size() > 0) {
            elementsDisputed.put("childDisabled", buildElementIssueArray(sscsCaseData.getElementsDisputedChildDisabled()));
        }
        return elementsDisputed;
    }

    @SuppressWarnings("unchecked")
    private static JSONArray buildElementIssueArray(List<ElementDisputed> elementList) {
        JSONArray elementsDisputedArray = new JSONArray();

        elementList.forEach(e -> elementsDisputedArray.add(e.getValue().getIssueCode()));

        return elementsDisputedArray;
    }

    private boolean isAppointeeDetailsEmpty(Appointee appointee) {
        return appointee == null
            || (isAddressEmpty(appointee.getAddress())
            && isContactEmpty(appointee.getContact())
            && isIdentityEmpty(appointee.getIdentity())
            && isNameEmpty(appointee.getName()));
    }

    private boolean isAddressEmpty(Address address) {
        return address == null
            || (address.getLine1() == null
            && address.getLine2() == null
            && address.getTown() == null
            && address.getCounty() == null
            && address.getPostcode() == null);
    }

    private boolean isContactEmpty(Contact contact) {
        return contact == null
            || (contact.getEmail() == null
            && contact.getPhone() == null
            && contact.getMobile() == null);
    }

    private boolean isIdentityEmpty(Identity identity) {
        return identity == null
            || (identity.getDob() == null
            && identity.getNino() == null);
    }

    private boolean isNameEmpty(Name name) {
        return name == null
            || (name.getFirstName() == null
            && name.getLastName() == null
            && name.getTitle() == null);
    }

    private static String convertBooleanToYesNo(Boolean value) {
        return Boolean.TRUE.equals(value) ? "Yes" : "No";
    }

    private static String convertBooleanToPaperOral(Boolean value) {
        return Boolean.TRUE.equals(value) ? "Oral" : "Paper";
    }

    private static String getLocalDate(String dateStr) {
        LocalDate localDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return localDate.toString();
    }
}
