package uk.gov.hmcts.reform.sscs.service.placeholders;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderUtility.defaultToEmptyStringIfNull;

import java.util.Map;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.PlaceholderConstants;

@Service
public class RpcPlaceholderService {

    void setRegionalProcessingOfficeAddress(Map<String, Object> placeholders, SscsCaseData caseData) {
        if (hasRegionalProcessingCenter(caseData)) {
            RegionalProcessingCenter rpc = caseData.getRegionalProcessingCenter();
            placeholders.put(PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL,
                defaultToEmptyStringIfNull(rpc.getAddress1()));
            placeholders.put(PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL,
                defaultToEmptyStringIfNull(rpc.getAddress2()));
            placeholders.put(PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL,
                defaultToEmptyStringIfNull(rpc.getAddress3()));
            placeholders.put(PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL,
                defaultToEmptyStringIfNull(rpc.getAddress4()));
            placeholders.put(PlaceholderConstants.REGIONAL_OFFICE_COUNTY_LITERAL,
                defaultToEmptyStringIfNull(rpc.getCity()));
            placeholders.put(PlaceholderConstants.REGIONAL_OFFICE_PHONE_LITERAL,
                defaultToEmptyStringIfNull(rpc.getPhoneNumber()));
            placeholders.put(PlaceholderConstants.REGIONAL_OFFICE_FAX_LITERAL,
                defaultToEmptyStringIfNull(rpc.getFaxNumber()));
            placeholders.put(PlaceholderConstants.REGIONAL_OFFICE_POSTCODE_LITERAL,
                defaultToEmptyStringIfNull(rpc.getPostcode()));
        }
    }

    private boolean hasRegionalProcessingCenter(SscsCaseData ccdResponse) {
        return nonNull(ccdResponse.getRegionalProcessingCenter())
            && nonNull(ccdResponse.getRegionalProcessingCenter().getName());
    }
}
