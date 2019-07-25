package uk.gov.hmcts.reform.sscs.service.placeholders;

import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.CASE_CREATED_DATE_LITERAL;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Service
@Slf4j
public class Dl6AndDl16PlaceholderService {

    @Autowired
    private CommonPlaceholderService commonPlaceholderService;
    @Autowired
    private DwpAddressPlaceholderService dwpAddressPlaceholderService;
    @Autowired
    private RpcPlaceholderService rpcPlaceholderService;

    public Map<String, Object> populatePlaceholders(SscsCaseData caseData, LocalDateTime caseCreatedDate) {
        Map<String, Object> placeholders = new ConcurrentHashMap<>();
        commonPlaceholderService.populatePlaceholders(caseData, placeholders);
        placeholders.put(CASE_CREATED_DATE_LITERAL, caseCreatedDate.toLocalDate().toString());
        rpcPlaceholderService.populatePlaceHolders(placeholders, caseData);
        dwpAddressPlaceholderService.populatePlaceholders(placeholders, caseData);
        return placeholders;
    }

}
