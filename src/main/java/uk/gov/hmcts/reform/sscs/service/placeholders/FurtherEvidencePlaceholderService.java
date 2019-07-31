package uk.gov.hmcts.reform.sscs.service.placeholders;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.PARTY_ADDRESS_LINE_1;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.PARTY_ADDRESS_LINE_2;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.PARTY_ADDRESS_LINE_3;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.PARTY_ADDRESS_LINE_4;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderUtility.defaultToEmptyStringIfNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.DwpAddress;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookup;

@Service
@Slf4j
public class FurtherEvidencePlaceholderService {

    private final RpcPlaceholderService rpcPlaceholderService;
    private final CommonPlaceholderService commonPlaceholderService;
    private final DwpAddressLookup dwpAddressLookup;

    @Autowired
    public FurtherEvidencePlaceholderService(RpcPlaceholderService rpcPlaceholderService,
                                             CommonPlaceholderService commonPlaceholderService,
                                             DwpAddressLookup dwpAddressLookup) {
        this.rpcPlaceholderService = rpcPlaceholderService;
        this.commonPlaceholderService = commonPlaceholderService;
        this.dwpAddressLookup = dwpAddressLookup;
    }

    public Map<String, Object> populatePlaceHolders(SscsCaseData caseData, FurtherEvidenceLetterType letterType) {
        requireNonNull(caseData, "caseData must not be null");
        Map<String, Object> placeholders = new ConcurrentHashMap<>();
        commonPlaceholderService.populatePlaceholders(caseData, placeholders);
        rpcPlaceholderService.populatePlaceHolders(placeholders, caseData);

        Address address = getAddress(caseData, letterType);
        //TODO: Use 5 lines
        placeholders.put(PARTY_ADDRESS_LINE_1, defaultToEmptyStringIfNull(address.getLine1()));
        placeholders.put(PARTY_ADDRESS_LINE_2, defaultToEmptyStringIfNull(address.getLine2()));
        placeholders.put(PARTY_ADDRESS_LINE_3, defaultToEmptyStringIfNull(address.getCounty()));
        placeholders.put(PARTY_ADDRESS_LINE_4, defaultToEmptyStringIfNull(address.getPostcode()));

        return placeholders;
    }

    private Address getAddress(SscsCaseData caseData, FurtherEvidenceLetterType letterType) {
        if (FurtherEvidenceLetterType.DWP_LETTER.getValue().equals(letterType.getValue())) {
            return getDwpAddress(caseData);
        } else if (FurtherEvidenceLetterType.APPELLANT_LETTER.getValue().equals(letterType.getValue())) {
            return getAppellantAddress(caseData);
        }
        return getRepsAddress(caseData);
    }

    private Address getDwpAddress(SscsCaseData caseData) {
        return buildDwpAddress(dwpAddressLookup.lookup(caseData.getAppeal().getBenefitType().getCode(),
            caseData.getAppeal().getMrnDetails().getDwpIssuingOffice()));
    }

    private Address getRepsAddress(SscsCaseData caseData) {
        return Optional.of(caseData.getAppeal())
            .map(Appeal::getRep)
            .map(Representative::getAddress)
            .orElse(getEmptyAddress());
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
            .orElse(getEmptyAddress());
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

    private Address buildDwpAddress(DwpAddress dwpAddress) {
        return Address.builder()
            .line1(dwpAddress.getLine1().orElse(null))
            .town(dwpAddress.getLine2().orElse(null))
            .county(dwpAddress.getLine3().orElse(null))
            .postcode(dwpAddress.getPostCode().orElse(null))
            .build();
    }
}
