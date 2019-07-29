package uk.gov.hmcts.reform.sscs.service.placeholders;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.ORIGINAL_SENDER_ADDRESS_LINE_1;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.ORIGINAL_SENDER_ADDRESS_LINE_2;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.ORIGINAL_SENDER_ADDRESS_LINE_3;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.ORIGINAL_SENDER_ADDRESS_LINE_4;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderUtility.defaultToEmptyStringIfNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Service
public class OriginalSender60997PlaceholderService {

    private final RpcPlaceholderService rpcPlaceholderService;
    private final CommonPlaceholderService commonPlaceholderService;

    @Autowired
    public OriginalSender60997PlaceholderService(RpcPlaceholderService rpcPlaceholderService,
                                                 CommonPlaceholderService commonPlaceholderService) {
        this.rpcPlaceholderService = rpcPlaceholderService;
        this.commonPlaceholderService = commonPlaceholderService;
    }

    public Map<String, Object> populatePlaceHolders(SscsCaseData caseData, DocumentType documentType) {
        requireNonNull(caseData, "caseData must not be null");
        Map<String, Object> placeholders = new ConcurrentHashMap<>();
        commonPlaceholderService.populatePlaceholders(caseData, placeholders);
        rpcPlaceholderService.populatePlaceHolders(placeholders, caseData);

        Address address = getAddress(caseData);
        placeholders.put(ORIGINAL_SENDER_ADDRESS_LINE_1, defaultToEmptyStringIfNull(address.getLine1()));
        placeholders.put(ORIGINAL_SENDER_ADDRESS_LINE_2, defaultToEmptyStringIfNull(address.getLine2()));
        placeholders.put(ORIGINAL_SENDER_ADDRESS_LINE_3, defaultToEmptyStringIfNull(address.getCounty()));
        placeholders.put(ORIGINAL_SENDER_ADDRESS_LINE_4, defaultToEmptyStringIfNull(address.getPostcode()));

        return placeholders;
    }

    private Address getAddress(SscsCaseData caseData) {
        return Optional.of(caseData.getAppeal())
            .map(Appeal::getAppellant)
            .filter(appellant -> "yes".equalsIgnoreCase(appellant.getIsAppointee()))
            .map(Appellant::getAppointee)
            .map(Appointee::getAddress)
            .orElseGet(() -> defaultAddress(caseData.getAppeal()));
    }

    private Address defaultAddress(Appeal appeal) {
        Address emptyAddress = Address.builder()
            .line1(StringUtils.EMPTY)
            .line2(StringUtils.EMPTY)
            .county(StringUtils.EMPTY)
            .postcode(StringUtils.EMPTY)
            .build();

        return Optional.of(appeal)
            .map(Appeal::getAppellant)
            .map(Appellant::getAddress)
            .orElse(emptyAddress);
    }
}
