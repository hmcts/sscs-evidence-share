package uk.gov.hmcts.reform.sscs.service.placeholders;

import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.ORIGINAL_SENDER_ADDRESS_LINE_1;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.ORIGINAL_SENDER_ADDRESS_LINE_2;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.ORIGINAL_SENDER_ADDRESS_LINE_3;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.ORIGINAL_SENDER_ADDRESS_LINE_4;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderUtility.defaultToEmptyStringIfNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Service
public class OriginalSender60997PlaceholderService {

    @Autowired
    private RpcPlaceholderService rpcPlaceholderService;
    @Autowired
    private CommonPlaceholderService commonPlaceholderService;

    //todo: add junit test
    public Map<String, Object> populatePlaceHolders(SscsCaseData caseData) {
        Map<String, Object> placeholders = new ConcurrentHashMap<>();
        commonPlaceholderService.populatePlaceholders(caseData, placeholders);
        rpcPlaceholderService.populatePlaceHolders(placeholders, caseData);

        Address address = caseData.getAppeal().getAppellant().getAddress();
        placeholders.put(ORIGINAL_SENDER_ADDRESS_LINE_1, defaultToEmptyStringIfNull(address.getLine1()));
        placeholders.put(ORIGINAL_SENDER_ADDRESS_LINE_2, defaultToEmptyStringIfNull(address.getLine2()));
        placeholders.put(ORIGINAL_SENDER_ADDRESS_LINE_3, defaultToEmptyStringIfNull(address.getCounty()));
        placeholders.put(ORIGINAL_SENDER_ADDRESS_LINE_4, defaultToEmptyStringIfNull(address.getPostcode()));

        return placeholders;
    }
}
