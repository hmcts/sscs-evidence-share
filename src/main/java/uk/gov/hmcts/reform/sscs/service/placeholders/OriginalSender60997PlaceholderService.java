package uk.gov.hmcts.reform.sscs.service.placeholders;

import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderUtility.defaultToEmptyStringIfNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
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

        Appeal appeal = caseData.getAppeal();
        Address address = appeal.getAppellant().getAddress();
        //todo: add literal string to the constant class
        placeholders.put("original_sender_address_line1", defaultToEmptyStringIfNull(address.getLine1()));
        placeholders.put("original_sender_address_line2", defaultToEmptyStringIfNull(address.getLine2()));
        placeholders.put("original_sender_address_line3", defaultToEmptyStringIfNull(address.getCounty()));
        placeholders.put("original_sender_address_line4", defaultToEmptyStringIfNull(address.getPostcode()));

        return placeholders;
    }
}
