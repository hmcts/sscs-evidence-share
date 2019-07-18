package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.assertTrue;

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
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

@RunWith(JUnitParamsRunner.class)
public class IssueFurtherEvidenceHandlerTest {

    @Test
    @Parameters({"No,APPELLANT_EVIDENCE"})
    public void givenIssueFurtherEvidenceCallback_shouldBeHandledUnderCertainConditions(String evidenceIssued,
                                                                                        DocumentType documentType) {
        IssueFurtherEvidenceHandler issueFurtherEvidenceHandler = new IssueFurtherEvidenceHandler();

        Callback<SscsCaseData> callbackWithSscsCaseDataWithNoAppointeeAndDocTypeWithAppellantEvidenceAndNoIssued =
            buildTestCallbackForGivenParams(evidenceIssued, documentType.getValue());

        boolean actual = issueFurtherEvidenceHandler.canHandle(CallbackType.SUBMITTED,
            callbackWithSscsCaseDataWithNoAppointeeAndDocTypeWithAppellantEvidenceAndNoIssued, DispatchPriority.EARLIEST);

        assertTrue(actual);
    }

    private Callback<SscsCaseData> buildTestCallbackForGivenParams(String evidenceIssued, String documentType) {
        SscsCaseData sscsCaseDataWithNoAppointeeAndDocTypeWithAppellantEvidenceAndNoIssued = SscsCaseData.builder()
            .sscsDocument(Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                    .documentType(documentType)
                    .evidenceIssued(evidenceIssued)
                    .build())
                .build()))
            .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(1L, "SSCS",
            State.INTERLOCUTORY_REVIEW_STATE, sscsCaseDataWithNoAppointeeAndDocTypeWithAppellantEvidenceAndNoIssued,
            LocalDateTime.now());
        return new Callback<>(caseDetails, Optional.empty(), EventType.ISSUE_FURTHER_EVIDENCE);
    }
}
