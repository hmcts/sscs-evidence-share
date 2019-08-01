package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.config.EvidenceShareConfig;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;
import uk.gov.hmcts.reform.sscs.factory.DocumentRequestFactory;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.BulkPrintService;
import uk.gov.hmcts.reform.sscs.service.DocumentManagementServiceWrapper;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;

@RunWith(JUnitParamsRunner.class)
public class SendToBulkPrintHandlerTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private DocumentManagementServiceWrapper documentManagementServiceWrapper;

    @Mock
    private DocumentRequestFactory documentRequestFactory;

    @Mock
    private BulkPrintService bulkPrintService;

    @Mock
    private EvidenceManagementService evidenceManagementService;

    @Mock
    private EvidenceShareConfig evidenceShareConfig;

    @Mock
    private CcdService ccdCaseService;

    @Mock
    private IdamService idamService;

    @Mock
    private Callback<SscsCaseData> callback;

    private SendToBulkPrintHandler handler;

    private LocalDateTime now = LocalDateTime.now();

    @Captor
    private ArgumentCaptor<SscsCaseData> caseDataCaptor;

    String docUrl = "my/1/url.pdf";
    Pdf docPdf = new Pdf(docUrl.getBytes(), "evidence1.pdf");
    Pdf docPdf2 = new Pdf(docUrl.getBytes(), "evidence2.pdf");

    @Before
    public void setUp() {
        when(callback.getEvent()).thenReturn(EventType.SEND_TO_DWP);
        handler = new SendToBulkPrintHandler(documentManagementServiceWrapper,
            documentRequestFactory, evidenceManagementService, bulkPrintService, evidenceShareConfig,
            ccdCaseService, idamService);
        when(evidenceShareConfig.getSubmitTypes()).thenReturn(Collections.singletonList("paper"));

        ReflectionTestUtils.setField(handler, "bulkPrintFeature", true);
    }

    @Test
    public void givenASendToDwpEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenANonBulkPrintEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenAMessageWhichFindsATemplate_thenConvertToSscsCaseDataAndAddPdfToCaseAndSendToBulkPrint() {

        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", "Paper", Arrays.asList(
            SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName(docPdf.getName())
                .documentType("sscs1")
                .documentLink(DocumentLink.builder().documentUrl(docUrl)
                    .documentFilename(docPdf.getName()).build())
                .build()).build(),
            SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName(docPdf2.getName())
                .documentType("appellantEvidence")
                .documentLink(DocumentLink.builder().documentUrl(docUrl)
                    .documentFilename(docPdf2.getName()).build())
                .evidenceIssued("No")
                .build()).build(),
            SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("filtered out word.doc")
                .documentType("appellantEvidence")
                .documentLink(DocumentLink.builder().documentUrl("/my/1/doc.url")
                    .documentFilename("filtered out word.doc").build())
                .build()).build(),
            SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("filtered out as there is no documentLink object.pfd")
                .build()).build()), APPEAL_CREATED);

        when(evidenceManagementService.download(eq(URI.create(docUrl)), any())).thenReturn(docPdf.getContent());

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("Test", "Value");

        Template template = new Template("bla", "bla2");

        DocumentHolder holder = DocumentHolder.builder().placeholders(placeholders).template(template).build();

        when(documentRequestFactory.create(caseDetails.getCaseData(), now)).thenReturn(holder);

        Optional<UUID> expectedOptionalUuid = Optional.of(UUID.fromString("0f14d0ab-9605-4a62-a9e4-5ed26688389b"));

        when(bulkPrintService.sendToBulkPrint(eq(Arrays.asList(docPdf, docPdf2)), any()))
            .thenReturn(expectedOptionalUuid);

        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.EVIDENCE_RECEIVED);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(evidenceManagementService, times(2)).download(eq(URI.create(docUrl)), any());
        verify(bulkPrintService).sendToBulkPrint(eq(Arrays.asList(docPdf, docPdf2)), any());

        String documentList = "Case has been sent to the DWP via Bulk Print with bulk print id: 0f14d0ab-9605-4a62-a9e4-5ed26688389b and with documents: evidence1.pdf, evidence2.pdf";
        verify(ccdCaseService).updateCase(caseDataCaptor.capture(), eq(123L), eq(EventType.SENT_TO_DWP.getCcdType()), eq("Sent to DWP"), eq(documentList), any());

        List<SscsDocument> docs = caseDataCaptor.getValue().getSscsDocument();
        assertNull(docs.get(0).getValue().getEvidenceIssued());
        assertEquals("Yes", docs.get(1).getValue().getEvidenceIssued());
    }

    @Test
    public void givenAnErrorWhenSendToBulkPrint_shouldUpdateCaseInCcdToFlagError() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", "Paper", Arrays.asList(
            SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName(docPdf.getName())
                .documentType("sscs1")
                .evidenceIssued("No")
                .documentLink(DocumentLink.builder().documentUrl(docUrl)
                    .documentFilename(docPdf.getName()).build())
                .build()).build()), APPEAL_CREATED);

        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.EVIDENCE_RECEIVED);

        ArgumentCaptor<SscsCaseData> caseDataCaptor = ArgumentCaptor.forClass(SscsCaseData.class);

        handler.handle(CallbackType.SUBMITTED, callback);
        then(ccdCaseService)
            .should(times(1))
            .updateCase(caseDataCaptor.capture(), eq(123L), eq("sendToDwpError"), any(), any(), any());

        assertEquals("failedSending", caseDataCaptor.getValue().getHmctsDwpState());

        List<SscsDocument> docs = caseDataCaptor.getValue().getSscsDocument();
        assertEquals("No", docs.get(0).getValue().getEvidenceIssued());
    }

    @Test
    public void givenNoTemplates_shouldThrowAnExceptionAndFlagError() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", "Paper",
            null, APPEAL_CREATED);
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.EVIDENCE_RECEIVED);

        when(documentRequestFactory.create(caseDetails.getCaseData(), now))
            .thenReturn(DocumentHolder.builder()
                .template(null)
                .build());

        handler.handle(CallbackType.SUBMITTED, callback);

        then(ccdCaseService)
            .should(times(1))
            .updateCase(caseDataCaptor.capture(), eq(123L), eq("sendToDwpError"), any(), any(), any());
        assertEquals("failedSending", caseDataCaptor.getValue().getHmctsDwpState());
    }

    @Test
    public void givenNoBulkPrintIdReturned_shouldThrowAnExceptionAndFlagError() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", "Paper", Arrays.asList(
            SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName(docPdf.getName())
                .documentType("sscs1")
                .documentLink(DocumentLink.builder().documentUrl(docUrl)
                    .documentFilename(docPdf.getName()).build())
                .build()).build()), APPEAL_CREATED);

        when(evidenceManagementService.download(eq(URI.create(docUrl)), any())).thenReturn(docPdf.getContent());

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("Test", "Value");

        Template template = new Template("bla", "bla2");

        DocumentHolder holder = DocumentHolder.builder().placeholders(placeholders).template(template).build();

        when(documentRequestFactory.create(caseDetails.getCaseData(), now)).thenReturn(holder);

        when(bulkPrintService.sendToBulkPrint(eq(Arrays.asList(docPdf, docPdf2)), any()))
            .thenReturn(Optional.empty());

        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.EVIDENCE_RECEIVED);

        handler.handle(CallbackType.SUBMITTED, callback);

        then(ccdCaseService)
            .should(times(1))
            .updateCase(caseDataCaptor.capture(), eq(123L), eq("sendToDwpError"), any(), any(), any());
        assertEquals("failedSending", caseDataCaptor.getValue().getHmctsDwpState());
    }

    @Test
    public void givenAMessageWhichCannotFindATemplate_thenConvertToSscsCaseDataAndDoNotAddPdfToCase() {

        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", "Paper", null, APPEAL_CREATED);
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.EVIDENCE_RECEIVED);

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("Test", "Value");

        DocumentHolder holder = DocumentHolder.builder().placeholders(placeholders).template(null).build();

        when(documentRequestFactory.create(caseDetails.getCaseData(), now)).thenReturn(holder);

        handler.handle(CallbackType.SUBMITTED, callback);

        verifyNoMoreInteractions(documentManagementServiceWrapper);
    }

    @Test
    @Parameters({"Online", "COR"})
    public void nonReceivedViaPaperCases_doesNotGetSentToBulkPrint(String receivedVia) {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", receivedVia, null, APPEAL_CREATED);
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.EVIDENCE_RECEIVED);

        handler.handle(CallbackType.SUBMITTED, callback);

        verifyNoMoreInteractions(documentManagementServiceWrapper);
    }


    @Test
    public void givenBulkPrintFeatureFlagIsOff_doNotProcess() {
        ReflectionTestUtils.setField(handler, "bulkPrintFeature", false);

        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", "Paper", null, VALID_APPEAL);
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.VALID_APPEAL_CREATED);

        handler.handle(CallbackType.SUBMITTED, callback);

        verifyNoMoreInteractions(bulkPrintService);
        verify(ccdCaseService).updateCase(any(), eq(123L), eq(EventType.SENT_TO_DWP.getCcdType()), eq("Sent to DWP"), eq("Case state is now sent to DWP"), eq(null));
    }

    private CaseDetails<SscsCaseData> getCaseDetails(String benefitType, String receivedVia, List<SscsDocument> sscsDocuments, State state) {
        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId("123")
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code(benefitType).build())
                .receivedVia(receivedVia)
                .build())
            .sscsDocument(sscsDocuments)
            .build();

        return new CaseDetails<>(
            123L,
            "jurisdiction",
            state,
            caseData,
            now
        );
    }
}
