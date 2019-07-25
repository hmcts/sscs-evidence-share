package uk.gov.hmcts.reform.sscs.service.placeholders;

import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderUtility.defaultToEmptyStringIfNull;

import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.DwpAddress;
import uk.gov.hmcts.reform.sscs.exception.NoMrnDetailsException;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookup;

@Service
public class DwpAddressPlaceholderService {

    @Autowired
    private DwpAddressLookup dwpAddressLookup;

    void populatePlaceholders(Map<String, Object> placeholders, SscsCaseData caseData) {
        if (Objects.nonNull(caseData.getAppeal()) && Objects.nonNull(caseData.getAppeal().getMrnDetails())
            && Objects.nonNull(caseData.getAppeal().getMrnDetails().getDwpIssuingOffice())) {

            final DwpAddress dwpAddress = dwpAddressLookup.lookup(caseData.getAppeal().getBenefitType().getCode(),
                caseData.getAppeal().getMrnDetails().getDwpIssuingOffice());

            setDwpAddress(placeholders, dwpAddress);
        } else {
            throw new NoMrnDetailsException(caseData);
        }
    }

    void setDwpAddress(Map<String, Object> placeholders, DwpAddress dwpAddress) {
        String[] lines = dwpAddress.lines();
        if (lines.length >= 1) {
            placeholders.put(PlaceholderConstants.DWP_ADDRESS_LINE1_LITERAL, defaultToEmptyStringIfNull(lines[0]));
        }
        if (lines.length >= 2) {
            placeholders.put(PlaceholderConstants.DWP_ADDRESS_LINE2_LITERAL, defaultToEmptyStringIfNull(lines[1]));
        }
        if (lines.length >= 3) {
            placeholders.put(PlaceholderConstants.DWP_ADDRESS_LINE3_LITERAL, defaultToEmptyStringIfNull(lines[2]));
        }
        if (lines.length >= 4) {
            placeholders.put(PlaceholderConstants.DWP_ADDRESS_LINE4_LITERAL, defaultToEmptyStringIfNull(lines[3]));
        }
    }
}
