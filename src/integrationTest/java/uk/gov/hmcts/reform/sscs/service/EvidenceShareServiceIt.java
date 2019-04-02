package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.io.FileUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.document.EvidenceDownloadClientApi;
import uk.gov.hmcts.reform.sscs.document.EvidenceMetadataDownloadClientApi;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest
public class EvidenceShareServiceIt {

    // Below rules are needed to use the junitParamsRunner together with SpringRunner
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();
    //end of rules needed for junitParamsRunner

    @MockBean
    @SuppressWarnings({"PMD.UnusedPrivateField"})
    private IdamService idamService;

    @MockBean
    @SuppressWarnings({"PMD.UnusedPrivateField"})
    private CcdClient ccdClient;

    @MockBean
    @SuppressWarnings({"PMD.UnusedPrivateField"})
    private EvidenceDownloadClientApi evidenceDownloadClientApi;

    @MockBean
    @SuppressWarnings({"PMD.UnusedPrivateField"})
    private EvidenceMetadataDownloadClientApi evidenceMetadataDownloadClientApi;

    @MockBean
    private EvidenceManagementService evidenceManagementService;

    @MockBean
    private CcdService ccdService;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private BulkPrintService bulkPrintService;

    @Autowired
    private EvidenceShareService evidenceShareService;

    private static final String FILE_CONTENT = "Welcome to PDF document service";

    @Test
    public void appealWithMrnDateWithin30Days_shouldGenerateDL6TemplateAgainstDocmosisAndUploadToEvidenceManagementServiceAndAddToCaseInCcd() throws IOException {
        assertNotNull("evidenceShareService must be autowired", evidenceShareService);
        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("appealReceivedCallbackWithMrn.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = updateMrnDate(json, LocalDate.now().toString());
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().toString());

        doReturn(new ResponseEntity<>(FILE_CONTENT.getBytes(), HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), any(), eq(byte[].class));

        UploadResponse uploadResponse = createUploadResponse();
        when(evidenceManagementService.upload(any(),  eq("sscs"))).thenReturn(uploadResponse);
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().build());
        Optional<UUID> expectedOptionalUuid = Optional.of(UUID.randomUUID());
        when(bulkPrintService.sendToBulkPrint(any(), any())).thenReturn(expectedOptionalUuid);

        Optional<UUID> optionalUuid = evidenceShareService.processMessage(json);

        assertEquals(expectedOptionalUuid, optionalUuid);

        verify(restTemplate).postForEntity(anyString(), any(), eq(byte[].class));
        verify(evidenceManagementService).upload(any(),  eq("sscs"));
        verify(ccdService).updateCase(any(), any(), any(), any(), eq("Uploaded DL6-12345656789.pdf into SSCS"), any());
        verify(bulkPrintService).sendToBulkPrint(any(), any());
    }

    @Test
    public void appealWithNoMrnDateOlderThan30Days_shouldNotGenerateTemplateOrAddToCcd() throws IOException {
        assertNotNull("evidenceShareService must be autowired", evidenceShareService);
        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("appealReceivedCallbackWithMrn.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().minusDays(31).toString());

        doReturn(new ResponseEntity<>(FILE_CONTENT.getBytes(), HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), any(), eq(byte[].class));

        UploadResponse uploadResponse = createUploadResponse();
        when(evidenceManagementService.upload(any(),  eq("sscs"))).thenReturn(uploadResponse);
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().build());
        Optional<UUID> expectedOptionalUuid = Optional.of(UUID.randomUUID());
        when(bulkPrintService.sendToBulkPrint(any(), any())).thenReturn(expectedOptionalUuid);

        Optional<UUID> optionalUuid = evidenceShareService.processMessage(json);

        assertEquals(expectedOptionalUuid, optionalUuid);

        verify(restTemplate).postForEntity(anyString(), any(), eq(byte[].class));
        verify(evidenceManagementService).upload(any(),  eq("sscs"));
        verify(ccdService).updateCase(any(), any(), any(), any(), eq("Uploaded DL16-12345656789.pdf into SSCS"), any());
        verify(bulkPrintService).sendToBulkPrint(any(), any());
    }

    @Test
    public void appealWithNoMrnDate_shouldNotGenerateTemplateOrAddToCcd() throws IOException {
        assertNotNull("evidenceShareService must be autowired", evidenceShareService);
        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("appealReceivedCallback.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        Optional<UUID> optionalUuid = evidenceShareService.processMessage(json);

        assertEquals(Optional.empty(), optionalUuid);

        verifyNoMoreInteractions(restTemplate);
        verifyNoMoreInteractions(evidenceManagementService);
        verifyNoMoreInteractions(ccdService);
    }

    @Test
    @Parameters({"ONLINE", "COR"})
    public void nonReceivedViaPaper_shouldNotBeProcessed(String receivedVia) throws IOException {
        assertNotNull("evidenceShareService must be autowired", evidenceShareService);
        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("appealReceivedCallback.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = json.replace("PAPER", receivedVia);
        Optional<UUID> optionalUuid = evidenceShareService.processMessage(json);

        assertEquals(Optional.empty(), optionalUuid);

        verifyNoMoreInteractions(restTemplate);
        verifyNoMoreInteractions(evidenceManagementService);
        verifyNoMoreInteractions(ccdService);
    }

    @Test
    @Parameters({"ESA", "UC"})
    public void nonPipBenefitTypes_shouldNotBeProcessed(String benefitCode) throws IOException {
        assertNotNull("evidenceShareService must be autowired", evidenceShareService);
        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("appealReceivedCallback.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = json.replace("PIP", benefitCode);
        Optional<UUID> optionalUuid = evidenceShareService.processMessage(json);

        assertEquals(Optional.empty(), optionalUuid);

        verifyNoMoreInteractions(restTemplate);
        verifyNoMoreInteractions(evidenceManagementService);
        verifyNoMoreInteractions(ccdService);
    }

    private String updateMrnDate(String json, String updatedDate) {
        json = json.replace("2019-01-01", updatedDate);

        return json;
    }

    private UploadResponse createUploadResponse() {
        UploadResponse response = mock(UploadResponse.class);
        UploadResponse.Embedded embedded = mock(UploadResponse.Embedded.class);
        when(response.getEmbedded()).thenReturn(embedded);
        Document document = createDocument();
        when(embedded.getDocuments()).thenReturn(Collections.singletonList(document));
        return response;
    }

    private Document createDocument() {
        Document document = new Document();
        Document.Links links = new Document.Links();
        Document.Link link = new Document.Link();
        link.href = "some link";
        links.self = link;
        document.links = links;
        return document;
    }
}
