package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

@RunWith(JUnitParamsRunner.class)
public class IssueFurtherEvidenceHandlerTest {

    @Test
    @Parameters(method = "generateDifferentTestScenarios")
    public void givenIssueFurtherEvidenceCallback_shouldBeHandledUnderCertainConditions(SscsCaseData sscsCaseData,
                                                                                        boolean expected) {
        IssueFurtherEvidenceHandler issueFurtherEvidenceHandler = new IssueFurtherEvidenceHandler();

        boolean actual = issueFurtherEvidenceHandler.canHandle(CallbackType.SUBMITTED,
            buildTestCallbackForGivenData(sscsCaseData), DispatchPriority.EARLIEST);

        assertEquals(expected, actual);
    }

    private Object[] generateDifferentTestScenarios() {
        SscsCaseData sscsCaseDataWithNoAppointeeAndDocTypeWithAppellantEvidenceAndNoIssued = SscsCaseData.builder()
            .sscsDocument(Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                    .documentType(APPELLANT_EVIDENCE.getValue())
                    .evidenceIssued("No")
                    .build())
                .build()))
            .build();

        SscsCaseData sscsCaseDataWithNoAppointeeAndDocTypeWithAppellantEvidenceAndYesIssued = SscsCaseData.builder()
            .sscsDocument(Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                    .documentType(APPELLANT_EVIDENCE.getValue())
                    .evidenceIssued("Yes")
                    .build())
                .build()))
            .build();

        return new Object[]{
            new Object[]{sscsCaseDataWithNoAppointeeAndDocTypeWithAppellantEvidenceAndNoIssued, true},
            new Object[]{sscsCaseDataWithNoAppointeeAndDocTypeWithAppellantEvidenceAndYesIssued, false}
        };
    }

    private Callback<SscsCaseData> buildTestCallbackForGivenData(SscsCaseData sscsCaseData) {
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(1L, "SSCS",
            State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData,
            LocalDateTime.now());
        return new Callback<>(caseDetails, Optional.empty(), EventType.ISSUE_FURTHER_EVIDENCE);
    }
}
