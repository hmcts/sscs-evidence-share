package uk.gov.hmcts.reform.sscs.service.placeholders;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.*;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderUtility.defaultToEmptyStringIfNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
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

        buildAddressPlaceholders(address, placeholders);

        return placeholders;
    }

    private void buildAddressPlaceholders(Address address, Map<String, Object> placeholders) {
        String[] lines = lines(address);

        if (lines.length >= 1) {
            placeholders.put(PARTY_ADDRESS_LINE_1, defaultToEmptyStringIfNull(lines[0]));
        }
        if (lines.length >= 2) {
            placeholders.put(PARTY_ADDRESS_LINE_2, defaultToEmptyStringIfNull(lines[1]));
        }
        if (lines.length >= 3) {
            placeholders.put(PARTY_ADDRESS_LINE_3, defaultToEmptyStringIfNull(lines[2]));
        }
        if (lines.length >= 4) {
            placeholders.put(PARTY_ADDRESS_LINE_4, defaultToEmptyStringIfNull(lines[3]));
        }
        if (lines.length >= 5) {
            placeholders.put(PARTY_ADDRESS_LINE_5, defaultToEmptyStringIfNull(lines[4]));
        }
    }

    public static String[] lines(Address address) {
        return Stream.of(address.getLine1(), address.getLine2(), address.getTown(), address.getCounty(), address.getPostcode())
            .filter(x -> x != null)
            .toArray(String[]::new);
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
            .orElseGet(() -> getEmptyAddress());
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
            .orElseGet(() -> getEmptyAddress());
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
