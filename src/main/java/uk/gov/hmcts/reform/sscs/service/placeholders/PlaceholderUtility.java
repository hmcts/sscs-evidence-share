package uk.gov.hmcts.reform.sscs.service.placeholders;

import org.apache.commons.lang3.StringUtils;

final class PlaceholderUtility {
    private PlaceholderUtility() {
    }

    static String defaultToEmptyStringIfNull(String value) {
        return (value == null) ? StringUtils.EMPTY : value;
    }

    static String truncateAddressLine(String addressLine) {
        return addressLine != null && addressLine.length() > 45  ? addressLine.substring(0, 45) : addressLine;
    }
}
