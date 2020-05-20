package uk.gov.hmcts.reform.sscs.service.placeholders;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.NAME;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderUtility.truncateAddressLine;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;

@Service
@Slf4j
public class FurtherEvidencePlaceholderService {

    private final PlaceholderService placeholderService;

    @Autowired
    public FurtherEvidencePlaceholderService(PlaceholderService placeholderService) {
        this.placeholderService = placeholderService;
    }

    public Map<String, Object> populatePlaceholders(SscsCaseData caseData, FurtherEvidenceLetterType letterType) {


        requireNonNull(caseData, "caseData must not be null");

        Map<String, Object> placeholders = new ConcurrentHashMap<>();

        Address address = getAddress(caseData, letterType);

        placeholderService.build(caseData, placeholders, address, null);

        String name = getName(caseData, letterType);

        if (name != null) {
            placeholders.put(NAME, truncateAddressLine(name));
        }

        return placeholders;
    }

    private String getName(SscsCaseData caseData, FurtherEvidenceLetterType letterType) {

        if (FurtherEvidenceLetterType.APPELLANT_LETTER.getValue().equals(letterType.getValue())) {
            return Optional.of(caseData.getAppeal())
                .map(Appeal::getAppellant)
                .filter(appellant -> "yes".equalsIgnoreCase(appellant.getIsAppointee()))
                .map(Appellant::getAppointee)
                .map(Appointee::getName)
                .map(Name::getFullNameNoTitle)
                .orElseGet(() -> Optional.of(caseData.getAppeal())
                    .map(Appeal::getAppellant)
                    .map(Appellant::getName)
                    .map(Name::getFullNameNoTitle)
                    .orElse("Sir/Madam"));

        } else if (FurtherEvidenceLetterType.REPRESENTATIVE_LETTER.getValue().equals(letterType.getValue())) {

            return Optional.of(caseData.getAppeal())
                .map(Appeal::getRep)
                .map(Representative::getName)
                .map(Name::getFullNameNoTitle)
                .orElseGet(() -> Optional.of(caseData.getAppeal())
                        .map(Appeal::getRep)
                        .map(Representative::getOrganisation)
                        .orElse("Sir/Madam"));
        } else {
            return null;
        }
    }

    private Address getAddress(SscsCaseData caseData, FurtherEvidenceLetterType letterType) {
        if (FurtherEvidenceLetterType.APPELLANT_LETTER.getValue().equals(letterType.getValue())) {
            return getAppellantAddress(caseData);
        }
        return getRepsAddress(caseData);
    }

    private Address getRepsAddress(SscsCaseData caseData) {
        return Optional.of(caseData.getAppeal())
            .map(Appeal::getRep)
            .map(Representative::getAddress)
            .orElseGet(this::getEmptyAddress);
    }

    private Address getAppellantAddress(SscsCaseData caseData) {
        return Optional.of(caseData.getAppeal())
            .map(Appeal::getAppellant)
            .filter(appellant -> "yes".equalsIgnoreCase(appellant.getIsAppointee()))
            .map(Appellant::getAppointee)
            .map(Appointee::getAddress)
            .orElseGet(() -> defaultAddress(caseData.getAppeal()));
    }

    private Address defaultAddress(Appeal appeal) {
        return Optional.of(appeal)
            .map(Appeal::getAppellant)
            .map(Appellant::getAddress)
            .orElseGet(this::getEmptyAddress);
    }

    private Address getEmptyAddress() {
        log.error("Sending out letter with empty address");

        return Address.builder()
            .line1(StringUtils.EMPTY)
            .line2(StringUtils.EMPTY)
            .county(StringUtils.EMPTY)
            .postcode(StringUtils.EMPTY)
            .build();
    }
}
