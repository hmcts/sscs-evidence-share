package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

class IssueFurtherEvidenceHandlerTest {
    @Test
    void givenIssueFurtherEvidenceCallback_shouldBeHandledUnderCertainConditions() {
        IssueFurtherEvidenceHandler issueFurtherEvidenceHandler = new IssueFurtherEvidenceHandler();

        Callback<SscsCaseData> callbackWithSscsCaseDataWithNoAppointeeAndDocTypeWithAppellantEvidenceAndNoIssued =
            buildTestCallback();

        boolean actual = issueFurtherEvidenceHandler.canHandle(CallbackType.SUBMITTED,
            callbackWithSscsCaseDataWithNoAppointeeAndDocTypeWithAppellantEvidenceAndNoIssued, DispatchPriority.EARLIEST);

        assertTrue(actual);
    }

    private Callback<SscsCaseData> buildTestCallback() {
        SscsCaseData sscsCaseDataWithNoAppointeeAndDocTypeWithAppellantEvidenceAndNoIssued = SscsCaseData.builder()
            .sscsDocument(Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                    .documentType(DocumentType.APPELLANT_EVIDENCE.getValue())
                    .evidenceIssued("No")
                    .build())
                .build()))
            .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(1L, "SSCS",
            State.INTERLOCUTORY_REVIEW_STATE, sscsCaseDataWithNoAppointeeAndDocTypeWithAppellantEvidenceAndNoIssued,
            LocalDateTime.now());
        return new Callback<>(caseDetails, Optional.empty(), EventType.ISSUE_FURTHER_EVIDENCE);
    }
}
