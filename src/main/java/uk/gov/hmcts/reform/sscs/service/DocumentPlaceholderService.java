package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.*;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Service
@Slf4j
public class DocumentPlaceholderService {

    public Map<String, Object> generatePlaceholders(SscsCaseData caseData) {
        Map<String, Object> placeholders = new HashMap<>();

        placeholders.put(CASE_CREATED_DATE_LITERAL, caseData.getCaseCreated());

        setRegionalProcessingOfficeAddress(placeholders, caseData);
        return placeholders;
    }

    private Map<String, Object> setRegionalProcessingOfficeAddress(Map<String, Object> placeholders, SscsCaseData caseData) {
        RegionalProcessingCenter rpc;

        if (hasRegionalProcessingCenter(caseData)) {
            rpc = caseData.getRegionalProcessingCenter();
        } else {
            //FIXME: somehow add to some exception queue - this should be covered by another ticket
            return null;
        }
        if (rpc != null) {
            placeholders.put(REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL, rpc.getAddress1());
            placeholders.put(REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL, rpc.getAddress2());
            placeholders.put(REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL, rpc.getAddress3());
            placeholders.put(REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL, rpc.getAddress4());
            placeholders.put(REGIONAL_OFFICE_COUNTY_LITERAL, rpc.getCity());
            placeholders.put(REGIONAL_OFFICE_POSTCODE_LITERAL, rpc.getPostcode());
        }

        return placeholders;
    }

    private static boolean hasRegionalProcessingCenter(SscsCaseData ccdResponse) {
        return null != ccdResponse.getRegionalProcessingCenter()
            && null != ccdResponse.getRegionalProcessingCenter().getName();
    }
}
