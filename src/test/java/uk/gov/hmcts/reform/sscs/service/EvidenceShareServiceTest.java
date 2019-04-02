package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.APPEAL_CREATED;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.EvidenceShareConfig;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocumentManagementService;
import uk.gov.hmcts.reform.sscs.factory.DocumentRequestFactory;

@RunWith(JUnitParamsRunner.class)
public class EvidenceShareServiceTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

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

    @Mock
    private EvidenceShareConfig evidenceShareConfig;

    private EvidenceShareService evidenceShareService;

    private LocalDateTime now = LocalDateTime.now();

    @Before
    public void setUp() {
        evidenceShareService = new EvidenceShareService(sscsCaseCallbackDeserializer, documentManagementService,
            documentRequestFactory, evidenceManagementService, bulkPrintService, evidenceShareConfig);
        when(evidenceShareConfig.getAllowedBenefitTypes()).thenReturn(Collections.singletonList("pip"));
        when(evidenceShareConfig.getSubmitTypes()).thenReturn(Collections.singletonList("paper"));
    }

    @Test
    public void givenAMessageWhichFindsToATemplate_thenConvertToSscsCaseDataAndAddPdfToCase() {

        String docUrl = "my/1/url.pdf";
        Pdf docPdf = new Pdf(docUrl.getBytes(), "evidence1.pdf");
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", "Paper", Arrays.asList(
            SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName(docPdf.getName())
                .documentLink(DocumentLink.builder().documentUrl(docUrl)
                    .documentFilename(docPdf.getName()).build())
                .build()).build(),
            SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("filtered out word.doc")
                .documentLink(DocumentLink.builder().documentUrl("/my/1/doc.url")
                    .documentFilename("filtered out word.doc").build())
                .build()).build(),
            SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("filtered out as there is no documentLink object.pfd")
                .build()).build()));

        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.EVIDENCE_RECEIVED);
        when(sscsCaseCallbackDeserializer.deserialize(eq(MY_JSON_DATA))).thenReturn(callback);
        when(evidenceManagementService.download(eq(URI.create(docUrl)), any())).thenReturn(docPdf.getContent());

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("Test", "Value");

        Template template = new Template("bla", "bla2");

        DocumentHolder holder = DocumentHolder.builder().placeholders(placeholders).template(template).build();

        when(documentRequestFactory.create(caseDetails.getCaseData(), now)).thenReturn(holder);
        when(documentManagementService.generateDocumentAndAddToCcd(holder, caseDetails.getCaseData())).thenReturn(DL6_PDF);

        Optional<UUID> expectedOptionalUuid = Optional.of(UUID.randomUUID());
        when(bulkPrintService.sendToBulkPrint(eq(Arrays.asList(DL6_PDF, docPdf)), any()))
            .thenReturn(expectedOptionalUuid);

        Optional<UUID> optionalUuid = evidenceShareService.processMessage(MY_JSON_DATA);

        assertEquals(expectedOptionalUuid, optionalUuid);
        verify(evidenceManagementService).download(eq(URI.create(docUrl)), any());
        verify(bulkPrintService).sendToBulkPrint(eq(Arrays.asList(DL6_PDF, docPdf)), any());
    }

    @Test
    public void givenAMessageWhichCannotFindATemplate_thenConvertToSscsCaseDataAndDoNotAddPdfToCase() {

        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", "Paper", null);
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.EVIDENCE_RECEIVED);
        when(sscsCaseCallbackDeserializer.deserialize(eq(MY_JSON_DATA))).thenReturn(callback);

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("Test", "Value");

        DocumentHolder holder = DocumentHolder.builder().placeholders(placeholders).template(null).build();

        when(documentRequestFactory.create(caseDetails.getCaseData(), now)).thenReturn(holder);
        verifyNoMoreInteractions(documentManagementService);

        Optional<UUID> optionalUuid = evidenceShareService.processMessage(MY_JSON_DATA);

        assertEquals(Optional.empty(), optionalUuid);

    }

    private CaseDetails<SscsCaseData> getCaseDetails(String benefitType, String receivedVia, List<SscsDocument> sscsDocuments) {
        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code(benefitType).build())
                .receivedVia(receivedVia)
                .build())
            .sscsDocument(sscsDocuments)
            .build();

        return new CaseDetails<>(
            123L,
            "jurisdiction",
            APPEAL_CREATED,
            caseData,
            now
        );

    }

    @Test
    @Parameters({"Online", "COR"})
    public void nonReceivedViaPaperCases_doesNotGetSentToBulkPrint(String reveivedVia) {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", reveivedVia, null);
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.EVIDENCE_RECEIVED);
        when(sscsCaseCallbackDeserializer.deserialize(eq(MY_JSON_DATA))).thenReturn(callback);


        Optional<UUID> optionalUuid = evidenceShareService.processMessage(MY_JSON_DATA);

        verifyNoMoreInteractions(documentManagementService);
        assertEquals(Optional.empty(), optionalUuid);
    }

    @Test
    @Parameters({"ESA", "UC"})
    public void nonPipBenefitTypes_doesNotGetSentToDwp(String benefitCode) {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(benefitCode, "Online", null);
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.EVIDENCE_RECEIVED);
        when(sscsCaseCallbackDeserializer.deserialize(eq(MY_JSON_DATA))).thenReturn(callback);

        verifyNoMoreInteractions(documentManagementService);

        Optional<UUID> optionalUuid = evidenceShareService.processMessage(MY_JSON_DATA);

        assertEquals(Optional.empty(), optionalUuid);
    }

}
