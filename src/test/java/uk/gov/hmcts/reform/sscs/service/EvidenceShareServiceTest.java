package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
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
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
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

@RunWith(JUnitParamsRunner.class)
public class EvidenceShareServiceTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private static final String MY_JSON_DATA = "{myJson: true}";

    @Mock
    private SscsCaseCallbackDeserializer sscsCaseCallbackDeserializer;

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
    private RoboticsHandler roboticsHandler;

    private EvidenceShareService evidenceShareService;

    private LocalDateTime now = LocalDateTime.now();

    @Before
    public void setUp() {
        evidenceShareService = new EvidenceShareService(sscsCaseCallbackDeserializer, documentManagementServiceWrapper,
            documentRequestFactory, evidenceManagementService, bulkPrintService, evidenceShareConfig,
            ccdCaseService, idamService, roboticsHandler);
        when(evidenceShareConfig.getSubmitTypes()).thenReturn(Collections.singletonList("paper"));

        ReflectionTestUtils.setField(evidenceShareService, "sendToDwpFeature", true);
    }

    @Test
    public void givenAMessageWhichFindsATemplate_thenConvertToSscsCaseDataAndAddPdfToCaseAndSendToBulkPrint() {

        String docUrl = "my/1/url.pdf";
        Pdf docPdf = new Pdf(docUrl.getBytes(), "evidence1.pdf");
        Pdf docPdf2 = new Pdf(docUrl.getBytes(), "evidence2.pdf");
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

        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.EVIDENCE_RECEIVED);
        when(sscsCaseCallbackDeserializer.deserialize(eq(MY_JSON_DATA))).thenReturn(callback);
        when(evidenceManagementService.download(eq(URI.create(docUrl)), any())).thenReturn(docPdf.getContent());

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("Test", "Value");

        Template template = new Template("bla", "bla2");

        DocumentHolder holder = DocumentHolder.builder().placeholders(placeholders).template(template).build();

        when(documentRequestFactory.create(caseDetails.getCaseData(), now)).thenReturn(holder);

        Optional<UUID> expectedOptionalUuid = Optional.of(UUID.randomUUID());

        when(bulkPrintService.sendToBulkPrint(eq(Arrays.asList(docPdf, docPdf2)), any()))
            .thenReturn(expectedOptionalUuid);

        Optional<UUID> optionalUuid = evidenceShareService.processMessage(MY_JSON_DATA);

        assertEquals(expectedOptionalUuid, optionalUuid);

        verify(roboticsHandler).sendCaseToRobotics(any());
        verify(evidenceManagementService, times(2)).download(eq(URI.create(docUrl)), any());
        verify(bulkPrintService).sendToBulkPrint(eq(Arrays.asList(docPdf, docPdf2)), any());

        String documentList = "Case has been sent to the DWP via Bulk Print with documents: evidence1.pdf, evidence2.pdf";
        verify(ccdCaseService).updateCase(any(), eq(123L), eq(EventType.SENT_TO_DWP.getCcdType()), eq("Sent to DWP"), eq(documentList), any());
    }

    @Test
    public void givenAnErrorWhenSendToBulkPrint_shouldUpdateCaseInCcdToFlagError() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", "Paper",
            null, APPEAL_CREATED);
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.EVIDENCE_RECEIVED);
        when(sscsCaseCallbackDeserializer.deserialize(eq(MY_JSON_DATA))).thenReturn(callback);

        ArgumentCaptor<SscsCaseData> caseDataCaptor = ArgumentCaptor.forClass(SscsCaseData.class);

        Optional<UUID> result = evidenceShareService.processMessage(MY_JSON_DATA);

        assertFalse(result.isPresent());
        then(ccdCaseService)
            .should(times(1))
            .updateCase(caseDataCaptor.capture(), eq(123L), eq("sendToDwp"), any(), any(), any());
        assertEquals("failedSending", caseDataCaptor.getValue().getHmctsDwpState());
    }

    @Test
    public void givenNoTemplates_shouldThrowAnExceptionAndFlagError() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", "Paper",
            null, APPEAL_CREATED);
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.EVIDENCE_RECEIVED);
        when(sscsCaseCallbackDeserializer.deserialize(eq(MY_JSON_DATA))).thenReturn(callback);

        ArgumentCaptor<SscsCaseData> caseDataCaptor = ArgumentCaptor.forClass(SscsCaseData.class);
        when(documentRequestFactory.create(caseDetails.getCaseData(), now))
            .thenReturn(DocumentHolder.builder()
                .template(null)
                .build());

        Optional<UUID> result = evidenceShareService.processMessage(MY_JSON_DATA);

        assertFalse(result.isPresent());
        then(ccdCaseService)
            .should(times(1))
            .updateCase(caseDataCaptor.capture(), eq(123L), eq("sendToDwp"), any(), any(), any());
        assertEquals("failedSending", caseDataCaptor.getValue().getHmctsDwpState());


    }

    @Test
    public void givenAMessageWhichCannotFindATemplate_thenConvertToSscsCaseDataAndDoNotAddPdfToCase() {

        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", "Paper", null, APPEAL_CREATED);
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.EVIDENCE_RECEIVED);
        when(sscsCaseCallbackDeserializer.deserialize(eq(MY_JSON_DATA))).thenReturn(callback);

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("Test", "Value");

        DocumentHolder holder = DocumentHolder.builder().placeholders(placeholders).template(null).build();

        when(documentRequestFactory.create(caseDetails.getCaseData(), now)).thenReturn(holder);

        Optional<UUID> optionalUuid = evidenceShareService.processMessage(MY_JSON_DATA);

        verifyNoMoreInteractions(documentManagementServiceWrapper);
        verify(roboticsHandler).sendCaseToRobotics(any());

        assertEquals(Optional.empty(), optionalUuid);
    }

    @Test
    @Parameters({"Online", "COR"})
    public void nonReceivedViaPaperCases_doesNotGetSentToBulkPrint(String receivedVia) {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", receivedVia, null, APPEAL_CREATED);
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.EVIDENCE_RECEIVED);
        when(sscsCaseCallbackDeserializer.deserialize(eq(MY_JSON_DATA))).thenReturn(callback);

        Optional<UUID> optionalUuid = evidenceShareService.processMessage(MY_JSON_DATA);

        verify(roboticsHandler).sendCaseToRobotics(any());
        verifyNoMoreInteractions(documentManagementServiceWrapper);
        assertEquals(Optional.empty(), optionalUuid);
    }

    @Test
    public void givenSendToDwpFeatureFlagIsOffAndEventNotValidAppealCreated_doNotProcess() {
        ReflectionTestUtils.setField(evidenceShareService, "sendToDwpFeature", false);

        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", "Paper", null, APPEAL_CREATED);
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.SYA_APPEAL_CREATED);
        when(sscsCaseCallbackDeserializer.deserialize(eq(MY_JSON_DATA))).thenReturn(callback);

        Optional<UUID> optionalUuid = evidenceShareService.processMessage(MY_JSON_DATA);

        verifyNoMoreInteractions(roboticsHandler);
        verifyNoMoreInteractions(documentManagementServiceWrapper);
        assertEquals(Optional.empty(), optionalUuid);
    }

    @Test
    public void givenSendToDwpFeatureFlagIsOffAndEventIsValidAppealCreated_doNotProcess() {
        ReflectionTestUtils.setField(evidenceShareService, "sendToDwpFeature", false);

        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", "Paper", null, VALID_APPEAL);
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.VALID_APPEAL_CREATED);
        when(sscsCaseCallbackDeserializer.deserialize(eq(MY_JSON_DATA))).thenReturn(callback);

        Optional<UUID> optionalUuid = evidenceShareService.processMessage(MY_JSON_DATA);

        assertEquals(Optional.empty(), optionalUuid);
        verifyNoMoreInteractions(roboticsHandler);
        verifyNoMoreInteractions(documentManagementServiceWrapper);
        verify(ccdCaseService).updateCase(any(), eq(123L), eq(EventType.MOVE_TO_APPEAL_CREATED.getCcdType()), eq("Case created"), eq("Sending back to appealCreated state"), any());
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
