package uk.gov.hmcts.reform.sscs.service.placeholders;

import org.apache.commons.lang3.StringUtils;

final class PlaceholderUtility {
    private PlaceholderUtility() {
    }

    static Object defaultToEmptyStringIfNull(Object value) {
        return (value == null) ? StringUtils.EMPTY : value;
    }
}
