package uk.gov.hmcts.reform.sscs.callback.handlers;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class HandlerUtils {
    private HandlerUtils() {
        // empty
    }

    protected static boolean isANewJointParty(Callback<SscsCaseData> callback, SscsCaseData caseData) {
        boolean wasNotAlreadyJointParty = false;
        CaseDetails oldCaseDetails = callback.getCaseDetailsBefore().orElse(null);
        if (oldCaseDetails != null) {
            SscsCaseData oldCaseData = (SscsCaseData) oldCaseDetails.getCaseData();
            if (oldCaseData.getJointParty() == null || StringUtils.equalsIgnoreCase(oldCaseData.getJointParty(),"No")) {
                wasNotAlreadyJointParty = true;
            }
        } else {
            wasNotAlreadyJointParty = true;
        }
        return wasNotAlreadyJointParty && caseData.getJointParty() != null && StringUtils.equalsIgnoreCase("Yes", caseData.getJointParty());
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}
