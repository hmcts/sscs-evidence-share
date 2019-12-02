package uk.gov.hmcts.reform.sscs.callback.handlers;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HandlerHelper {

    public static Callback<SscsCaseData> buildTestCallbackForGivenData(SscsCaseData sscsCaseData, State state, EventType eventType) {
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(1L, "SSCS", state, sscsCaseData,
            LocalDateTime.now());
        return new Callback<>(caseDetails, Optional.empty(), eventType);
    }
}
