package uk.gov.hmcts.reform.sscs.service.placeholders;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.getBenefitByCodeOrThrowException;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.ADDRESS_NAME;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.APPEAL_REF;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.APPELLANT_NAME;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.BENEFIT_NAME_ACRONYM_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.CASE_ID_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.GENERATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.HMCTS2;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.INFO_REQUEST_DETAIL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.IS_OTHER_PARTY;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.IS_REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.JOINT;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.LETTER_ADDRESS_LINE_1;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.LETTER_ADDRESS_LINE_2;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.LETTER_ADDRESS_LINE_3;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.LETTER_ADDRESS_LINE_4;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.LETTER_ADDRESS_POSTCODE;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.NAME;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_PHONE_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.REPRESENTATIVE_NAME;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.SSCS_URL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.SSCS_URL_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderService.lines;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderUtility.defaultToEmptyStringIfNull;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderUtility.truncateAddressLine;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;

@Service
@Slf4j
public class GenericLetterPlaceholderService {

    private final PlaceholderService placeholderService;

    private static final String HMCTS_IMG = "[userImage:hmcts.png]";

    @Autowired
    public GenericLetterPlaceholderService(PlaceholderService placeholderService) {
        this.placeholderService = placeholderService;
    }

    public Map<String, Object> populatePlaceholders(SscsCaseData caseData, FurtherEvidenceLetterType letterType, String otherPartyId) {
        var placeholders = new HashMap<String, Object>();

        Address address = PlaceholderUtility.getAddress(caseData, letterType, otherPartyId);
        String name = PlaceholderUtility.getName(caseData, letterType, otherPartyId);

        placeholders.putAll(getAddressPlaceHolders(address));

        if (name != null) {
            placeholders.put(ADDRESS_NAME, truncateAddressLine(name));
            placeholders.put(NAME, name);
        }

        String appellantName = caseData.getAppeal().getAppellant().getName().getFullNameNoTitle();
        placeholders.put(APPELLANT_NAME, appellantName);

        Benefit benefit = getBenefitByCodeOrThrowException(caseData.getAppeal().getBenefitType().getCode());
        String benefitAcronym = benefit.isHasAcronym() ? benefit.getShortName() : benefit.getDescription();
        placeholders.put(BENEFIT_NAME_ACRONYM_LITERAL, benefitAcronym);

        placeholders.put(SSCS_URL_LITERAL, SSCS_URL);
        placeholders.put(GENERATED_DATE_LITERAL, LocalDateTime.now().toLocalDate().toString());
        placeholders.put(IS_REPRESENTATIVE, "No");

        if (isRepresentativeLetter(letterType) || isOtherPartyLetter(letterType)) {
            String representativeName = PlaceholderUtility.getName(caseData, letterType, otherPartyId);
            placeholders.put(REPRESENTATIVE_NAME, representativeName);

            if (isOtherPartyLetter(letterType)) {
                placeholders.put(IS_OTHER_PARTY, "Yes");
            } else {
                placeholders.put(IS_REPRESENTATIVE, "Yes");
            }
        }

        if (placeholderService.hasRegionalProcessingCenter(caseData)) {
            RegionalProcessingCenter rpc = caseData.getRegionalProcessingCenter();
            placeholders.put(REGIONAL_OFFICE_PHONE_LITERAL, defaultToEmptyStringIfNull(rpc.getPhoneNumber()));
        }

        placeholders.put(JOINT, isJointPartyLetter(letterType) ? JOINT : "");
        placeholders.put(APPEAL_REF, getAppealReference(caseData));
        placeholders.put(INFO_REQUEST_DETAIL, caseData.getGenericLetterText());
        placeholders.put(HMCTS2, HMCTS_IMG);
        placeholders.put(CASE_ID_LITERAL, caseData.getCcdCaseId());

        placeholderService.buildExcelaAddress(caseData.getIsScottishCase(), placeholders);

        return placeholders;
    }

    private static boolean isJointPartyLetter(FurtherEvidenceLetterType letterType) {
        return FurtherEvidenceLetterType.JOINT_PARTY_LETTER.getValue().equals(letterType.getValue());
    }

    private static boolean isRepresentativeLetter(FurtherEvidenceLetterType letterType) {
        return FurtherEvidenceLetterType.REPRESENTATIVE_LETTER.getValue().equals(letterType.getValue());
    }

    private static boolean isOtherPartyLetter(FurtherEvidenceLetterType letterType) {
        return FurtherEvidenceLetterType.OTHER_PARTY_LETTER.getValue().equals(letterType.getValue()) || FurtherEvidenceLetterType.OTHER_PARTY_REP_LETTER.getValue().equals(letterType.getValue());
    }

    private String getAppealReference(SscsCaseData caseData) {
        final String caseReference = caseData.getCaseReference();
        return isBlank(caseReference) || (caseData.getCreatedInGapsFrom() != null && caseData.getCreatedInGapsFrom().equals("readyToList"))
            ? caseData.getCcdCaseId() : caseReference;
    }

    private Map<String, Object> getAddressPlaceHolders(Address address) {
        var addressPlaceHolders = new HashMap<String, Object>();
        String[] lines = lines(address);
        String[] addressConstants = {LETTER_ADDRESS_LINE_1, LETTER_ADDRESS_LINE_2, LETTER_ADDRESS_LINE_3,
            LETTER_ADDRESS_LINE_4, LETTER_ADDRESS_POSTCODE};

        for (int i = 0; i < lines.length; i++) {
            addressPlaceHolders.put(addressConstants[i], truncateAddressLine(defaultToEmptyStringIfNull(lines[i])));
        }

        return addressPlaceHolders;
    }
}
