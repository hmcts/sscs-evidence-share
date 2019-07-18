package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REPRESENTATIVE_EVIDENCE;

import java.time.LocalDateTime;
import java.util.Arrays;
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

        // Different combinations of sscsDocument objects
        SscsDocument sscsDocument1WithAppellantEvidenceAndNoIssued = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType(APPELLANT_EVIDENCE.getValue())
                .evidenceIssued("No")
                .build())
            .build();

        SscsDocument sscsDocument2WithAppellantEvidenceAndNoIssued = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType(APPELLANT_EVIDENCE.getValue())
                .evidenceIssued("No")
                .build())
            .build();

        SscsDocument sscsDocument3WithAppellantEvidenceAndYesIssued = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType(APPELLANT_EVIDENCE.getValue())
                .evidenceIssued("Yes")
                .build())
            .build();

        SscsDocumentDetails sscsDocument4withRepEvidenceAndNoIssued = SscsDocumentDetails.builder()
            .documentType(REPRESENTATIVE_EVIDENCE.getValue())
            .evidenceIssued("No")
            .build();

        //Happy path scenarios

        SscsCaseData sscsCaseDataWithNoAppointeeAndDocTypeWithAppellantEvidenceAndNoIssued = SscsCaseData.builder()
            .sscsDocument(Collections.singletonList(sscsDocument1WithAppellantEvidenceAndNoIssued))
            .build();

        SscsCaseData sscsCaseDataWithNoAppointeeAndDocTypeWithAppellantEvidenceAndYesIssued = SscsCaseData.builder()
            .sscsDocument(Collections.singletonList(sscsDocument3WithAppellantEvidenceAndYesIssued))
            .build();


        SscsCaseData sscsCaseDataWithNoAppointeeAndDocTypeWithRepsEvidenceAndNoIssued = SscsCaseData.builder()
            .sscsDocument(Collections.singletonList(SscsDocument.builder()
                .value(sscsDocument4withRepEvidenceAndNoIssued)
                .build()))
            .build();

        //happy path scenarios with multiple documents

        SscsCaseData sscsCaseDataWithMultipleDocumentsWithAppellantEvidenceAndNoIssued = SscsCaseData.builder()
            .sscsDocument(Arrays.asList(sscsDocument1WithAppellantEvidenceAndNoIssued,
                sscsDocument2WithAppellantEvidenceAndNoIssued))
            .build();

        SscsCaseData sscsCaseDataWithMultipleDocumentsAndOneOfThemNotMeetingCriteria = SscsCaseData.builder()
            .sscsDocument(Arrays.asList(sscsDocument3WithAppellantEvidenceAndYesIssued,
                sscsDocument1WithAppellantEvidenceAndNoIssued))
            .build();

        //edge case scenarios

        SscsCaseData sscsCaseDataWithNoDocuments = SscsCaseData.builder().build();
        SscsCaseData sscsCaseDataWithEmptyDocuments = SscsCaseData.builder()
            .sscsDocument(Collections.singletonList(SscsDocument.builder().build()))
            .build();
        SscsCaseData sscsCaseDataWithEmptySscsDocumentDetails = SscsCaseData.builder()
            .sscsDocument(Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder().build())
                .build()))
            .build();
        SscsCaseData sscsCaseDataWithEmptyDocumentType = SscsCaseData.builder()
            .sscsDocument(Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                    .evidenceIssued("No")
                    .build())
                .build()))
            .build();
        SscsCaseData sscsCaseDataWithEmptyEvidenceIssued = SscsCaseData.builder()
            .sscsDocument(Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                    .documentType(APPELLANT_EVIDENCE.getValue())
                    .build())
                .build()))
            .build();
        SscsCaseData sscsCaseDataWithNullDocumentType = SscsCaseData.builder()
            .sscsDocument(Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                    .evidenceIssued("No")
                    .documentType(null)
                    .build())
                .build()))
            .build();

        SscsCaseData sscsCaseDataWithMultipleDocumentsAndOneOfThemNull = SscsCaseData.builder()
            .sscsDocument(Arrays.asList(null, sscsDocument1WithAppellantEvidenceAndNoIssued))
            .build();


        return new Object[]{
            new Object[]{sscsCaseDataWithNoAppointeeAndDocTypeWithAppellantEvidenceAndNoIssued, true},
            new Object[]{sscsCaseDataWithNoAppointeeAndDocTypeWithAppellantEvidenceAndYesIssued, false},
            new Object[]{sscsCaseDataWithNoAppointeeAndDocTypeWithRepsEvidenceAndNoIssued, false},
            new Object[]{sscsCaseDataWithNoDocuments, false},
            new Object[]{sscsCaseDataWithMultipleDocumentsWithAppellantEvidenceAndNoIssued, true},
            new Object[]{sscsCaseDataWithMultipleDocumentsAndOneOfThemNotMeetingCriteria, true},
            new Object[]{sscsCaseDataWithEmptyDocuments, false},
            new Object[]{sscsCaseDataWithEmptySscsDocumentDetails, false},
            new Object[]{sscsCaseDataWithEmptyDocumentType, false},
            new Object[]{sscsCaseDataWithNullDocumentType, false},
            new Object[]{sscsCaseDataWithEmptyEvidenceIssued, false},
            new Object[]{sscsCaseDataWithMultipleDocumentsAndOneOfThemNull, true}

        };
    }

    private Callback<SscsCaseData> buildTestCallbackForGivenData(SscsCaseData sscsCaseData) {
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(1L, "SSCS",
            State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData,
            LocalDateTime.now());
        return new Callback<>(caseDetails, Optional.empty(), EventType.ISSUE_FURTHER_EVIDENCE);
    }
}
