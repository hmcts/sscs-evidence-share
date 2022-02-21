package uk.gov.hmcts.reform.sscs.callback.handlers;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

public final class HandlerHelper {

    private HandlerHelper() {
    }

    public static Callback<SscsCaseData> buildTestCallbackForGivenData(SscsCaseData sscsCaseData, State state, EventType eventType) {
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(1L, "SSCS", state, sscsCaseData,
            LocalDateTime.now(), "Benefit");
        return new Callback<>(caseDetails, Optional.empty(), eventType, false);
    }

    public static Callback<SscsCaseData> buildTestCallbackForGivenData(SscsCaseData sscsCaseData, SscsCaseData oldSscsCaseData, State state, EventType eventType) {
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(1L, "SSCS", state, sscsCaseData,
            LocalDateTime.now(), "Benefit");
        CaseDetails<SscsCaseData> oldCaseDetails = new CaseDetails<>(1L, "SSCS", state, oldSscsCaseData,
            LocalDateTime.now(), "Benefit");

        return new Callback<>(caseDetails, Optional.of(oldCaseDetails), eventType, false);
    }

    public static Callback<SscsCaseData> buildCallbackWithSupplementaryData(SscsCaseData sscsCaseData, State state, EventType eventType, Map<String, Map<String, Object>> supplementaryData) {
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(1L, "SSCS", state, sscsCaseData,
            LocalDateTime.now(), "Benefit");
        caseDetails.setSupplementaryData(supplementaryData);

        return new Callback<>(caseDetails, Optional.empty(), eventType, false);
    }
}
