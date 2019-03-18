package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.APPEAL_CREATED;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;
import uk.gov.hmcts.reform.sscs.docmosis.factory.DocumentRequestFactory;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocumentManagementService;
import uk.gov.hmcts.reform.sscs.domain.Pdf;

@RunWith(MockitoJUnitRunner.class)
public class EvidenceShareServiceTest {

    private static final String MY_JSON_DATA = "{myJson: true}";

    @Mock
    private SscsCaseCallbackDeserializer sscsCaseCallbackDeserializer;

    @Mock
    private DocumentManagementService documentManagementService;

    @Mock
    private DocumentRequestFactory documentRequestFactory;

    @Mock
    private BulkPrintService bulkPrintService;

    @InjectMocks
    private EvidenceShareService evidenceShareService;

    @Test
    public void givenAMessageWhichFindsToATemplate_thenConvertToSscsCaseDataAndAddPdfToCase() {

        long expectedId = 123L;
        SscsCaseData caseData = SscsCaseData.builder().build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(
            expectedId,
            "jurisdiction",
            APPEAL_CREATED,
            caseData,
            LocalDateTime.now()
        );
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.EVIDENCE_RECEIVED);
        when(sscsCaseCallbackDeserializer.deserialize(eq(MY_JSON_DATA))).thenReturn(callback);

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("Test", "Value");

        DocumentHolder holder = DocumentHolder.builder().placeholders(placeholders).template(Template.DL6).build();

        when(documentRequestFactory.create(caseData)).thenReturn(holder);
        when(documentManagementService.generateDocumentAndAddToCcd(holder, caseData)).thenReturn(new Pdf(null, "test.pdf"));
        when(bulkPrintService.sendToBulkPrint(any(), any())).thenReturn(Optional.of(UUID.randomUUID()));

        long id = evidenceShareService.processMessage(MY_JSON_DATA);

        assertEquals("the id should be " + expectedId, expectedId, id);

    }

    @Test
    public void givenAMessageWhichCannotFindATemplate_thenConvertToSscsCaseDataAndDoNotAddPdfToCase() {

        long expectedId = 123L;
        SscsCaseData caseData = SscsCaseData.builder().build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(
            expectedId,
            "jurisdiction",
            APPEAL_CREATED,
            caseData,
            LocalDateTime.now()
        );
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.EVIDENCE_RECEIVED);
        when(sscsCaseCallbackDeserializer.deserialize(eq(MY_JSON_DATA))).thenReturn(callback);

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("Test", "Value");

        DocumentHolder holder = DocumentHolder.builder().placeholders(placeholders).template(null).build();

        when(documentRequestFactory.create(caseData)).thenReturn(holder);
        verifyNoMoreInteractions(documentManagementService);

        long id = evidenceShareService.processMessage(MY_JSON_DATA);

        assertEquals("the id should be " + expectedId, expectedId, id);

    }

}
