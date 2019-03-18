package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.APPEAL_CREATED;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocumentManagementService;
import uk.gov.hmcts.reform.sscs.domain.Pdf;
import uk.gov.hmcts.reform.sscs.factory.DocumentRequestFactory;

@RunWith(MockitoJUnitRunner.class)
public class EvidenceShareServiceTest {

    private static final String MY_JSON_DATA = "{myJson: true}";
    private static final Pdf DL6_PDF = new Pdf("null".getBytes(), "dl6.pdf");

    @Mock
    private SscsCaseCallbackDeserializer sscsCaseCallbackDeserializer;

    @Mock
    private DocumentManagementService documentManagementService;

    @Mock
    private DocumentRequestFactory documentRequestFactory;

    @Mock
    private BulkPrintService bulkPrintService;

    @Mock
    private EvidenceManagementService evidenceManagementService;

    @InjectMocks
    private EvidenceShareService evidenceShareService;

    @Test
    public void givenAMessageWhichFindsToATemplate_thenConvertToSscsCaseDataAndAddPdfToCase() {

        long expectedId = 123L;
        String docUrl = "my/1/url";
        Pdf docPdf = new Pdf(docUrl.getBytes(), "evidence1.pdf");
        SscsCaseData caseData = SscsCaseData.builder().sscsDocument(
            Collections.singletonList(
                SscsDocument.builder().value(SscsDocumentDetails.builder()
                    .documentFileName(docPdf.getName())
                    .documentLink(DocumentLink.builder().documentUrl(docUrl)
                        .documentFilename(docPdf.getName()).build())
                    .build()).build()
            )
        ).build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(
            expectedId,
            "jurisdiction",
            APPEAL_CREATED,
            caseData,
            LocalDateTime.now()
        );
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.EVIDENCE_RECEIVED);
        when(sscsCaseCallbackDeserializer.deserialize(eq(MY_JSON_DATA))).thenReturn(callback);

        when(evidenceManagementService.download(eq(URI.create(docUrl)), any())).thenReturn(docPdf.getContent());

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("Test", "Value");

        DocumentHolder holder = DocumentHolder.builder().placeholders(placeholders).template(Template.DL6).build();

        when(documentRequestFactory.create(caseData)).thenReturn(holder);
        when(documentManagementService.generateDocumentAndAddToCcd(holder, caseData)).thenReturn(DL6_PDF);

        when(bulkPrintService.sendToBulkPrint(eq(Arrays.asList(DL6_PDF, docPdf)), any()))
            .thenReturn(Optional.of(UUID.randomUUID()));

        long id = evidenceShareService.processMessage(MY_JSON_DATA);

        assertEquals("the id should be " + expectedId, expectedId, id);
        verify(evidenceManagementService).download(eq(URI.create(docUrl)), any());
        verify(bulkPrintService).sendToBulkPrint(eq(Arrays.asList(DL6_PDF, docPdf)), any());
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
