package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.callback.handlers.RoboticsCallbackHandler;
import uk.gov.hmcts.reform.sscs.callback.handlers.SendToBulkPrintHandler;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.document.EvidenceDownloadClientApi;
import uk.gov.hmcts.reform.sscs.document.EvidenceMetadataDownloadClientApi;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.servicebus.TopicConsumer;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:config/application_it.properties")
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
    private UpdateCcdCaseService updateCcdCaseService;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private BulkPrintService bulkPrintService;

    @MockBean
    private RoboticsService roboticsService;

    @Autowired
    private SendToBulkPrintHandler bulkPrintHandler;

    @Autowired
    private RoboticsCallbackHandler roboticsCallbackHandler;

    @Autowired
    private TopicConsumer topicConsumer;

    @Captor
    ArgumentCaptor<ArrayList<Pdf>> documentCaptor;

    private static final String FILE_CONTENT = "Welcome to PDF document service";

    protected Session session = Session.getInstance(new Properties());

    protected MimeMessage message;

    @MockBean
    protected JavaMailSender mailSender;

    Optional<UUID> expectedOptionalUuid = Optional.of(UUID.fromString("0f14d0ab-9605-4a62-a9e4-5ed26688389b"));

    @Before
    public void setup() {
        message = new MimeMessage(session);
        when(mailSender.createMimeMessage()).thenReturn(message);
    }

    @Test
    public void appealWithMrnDateWithin30Days_shouldGenerateDL6TemplateAndAndAddToCaseInCcdAndSendToRoboticsAndBulkPrintInCorrectOrder() throws IOException {
        assertNotNull("SendToBulkPrintHandler must be autowired", bulkPrintHandler);
        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("validAppealCreatedCallbackWithMrn.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = updateMrnDate(json, LocalDate.now().toString());
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().toString());

        doReturn(new ResponseEntity<>(FILE_CONTENT.getBytes(), HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), any(), eq(byte[].class));

        UploadResponse uploadResponse = createUploadResponse();
        when(evidenceManagementService.upload(any(), eq("sscs"))).thenReturn(uploadResponse);
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().build());
        when(ccdService.updateCase(any(), any(), eq("uploadDocument"), any(), eq("Uploaded dl6-12345656789.pdf into SSCS"), any())).thenReturn(SscsCaseDetails.builder().build());

        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any())).thenReturn(expectedOptionalUuid);

        String documentList = "Case has been sent to the DWP via Bulk Print with bulk print id: 0f14d0ab-9605-4a62-a9e4-5ed26688389b and with documents: dl6-12345656789.pdf, sscs1.pdf, filename1.pdf";
        when(ccdService.updateCase(any(), any(), eq(EventType.SENT_TO_DWP.getCcdType()), any(), eq(documentList), any())).thenReturn(SscsCaseDetails.builder().build());
        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        topicConsumer.onMessage(json);

        assertEquals(3, documentCaptor.getValue().size());
        assertEquals("dl6-12345656789.pdf", documentCaptor.getValue().get(0).getName());
        assertEquals("sscs1.pdf", documentCaptor.getValue().get(1).getName());
        assertEquals("filename1.pdf", documentCaptor.getValue().get(2).getName());

        verify(restTemplate).postForEntity(anyString(), any(), eq(byte[].class));
        verify(evidenceManagementService).upload(any(), eq("sscs"));
        verify(ccdService).updateCase(any(), any(), any(), any(), eq("Uploaded dl6-12345656789.pdf into SSCS"), any());
        verify(bulkPrintService).sendToBulkPrint(any(), any());
        verify(roboticsService).sendCaseToRobotics(any());

        verify(ccdService).updateCase(any(), any(), eq(EventType.SENT_TO_DWP.getCcdType()), any(), eq(documentList), any());
    }

    @Test
    public void appealWithMrnDateOlderThan30Days_shouldGenerateDL16TemplateAndAndAddToCaseInCcdAndSendToRoboticsAndBulkPrint() throws IOException {
        assertNotNull("SendToBulkPrintHandler must be autowired", bulkPrintHandler);
        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("validAppealCreatedCallbackWithMrn.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().minusDays(31).toString());

        doReturn(new ResponseEntity<>(FILE_CONTENT.getBytes(), HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), any(), eq(byte[].class));

        UploadResponse uploadResponse = createUploadResponse();
        when(evidenceManagementService.upload(any(), eq("sscs"))).thenReturn(uploadResponse);
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().build());
        when(ccdService.updateCase(any(), any(), any(), any(), eq("Uploaded dl16-12345656789.pdf into SSCS"), any())).thenReturn(SscsCaseDetails.builder().build());

        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any())).thenReturn(expectedOptionalUuid);

        String documentList = "Case has been sent to the DWP via Bulk Print with bulk print id: 0f14d0ab-9605-4a62-a9e4-5ed26688389b and with documents: dl16-12345656789.pdf, sscs1.pdf, filename1.pdf";
        when(ccdService.updateCase(any(), any(), eq(EventType.SENT_TO_DWP.getCcdType()), any(), eq(documentList), any())).thenReturn(SscsCaseDetails.builder().build());
        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        topicConsumer.onMessage(json);

        assertEquals(3, documentCaptor.getValue().size());
        assertEquals("dl16-12345656789.pdf", documentCaptor.getValue().get(0).getName());
        assertEquals("sscs1.pdf", documentCaptor.getValue().get(1).getName());
        assertEquals("filename1.pdf", documentCaptor.getValue().get(2).getName());

        verify(restTemplate).postForEntity(anyString(), any(), eq(byte[].class));
        verify(evidenceManagementService).upload(any(), eq("sscs"));
        verify(ccdService).updateCase(any(), any(), any(), any(), eq("Uploaded dl16-12345656789.pdf into SSCS"), any());
        verify(bulkPrintService).sendToBulkPrint(any(), any());
        verify(roboticsService).sendCaseToRobotics(any());

        verify(ccdService).updateCase(any(), any(), eq(EventType.SENT_TO_DWP.getCcdType()), any(), eq(documentList), any());
    }

    @Test
    public void appealWithNoMrnDate_shouldNotGenerateTemplateOrAddToCcdAndShouldUpdateCaseWithSecondaryState()
        throws IOException {
        assertNotNull("SendToBulkPrintHandler must be autowired", bulkPrintHandler);
        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("validAppealCreatedCallback.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = json.replace("CREATED_IN_GAPS_FROM", "validAppeal");

        ArgumentCaptor<SscsCaseData> caseDataCaptor = ArgumentCaptor.forClass(SscsCaseData.class);

        topicConsumer.onMessage(json);

        then(ccdService)
            .should(times(1))
            .updateCase(caseDataCaptor.capture(), any(), eq("sendToDwpError"), any(), any(), any());
        assertNull(caseDataCaptor.getValue().getAppeal().getMrnDetails().getMrnDate());
        assertEquals("failedSending", caseDataCaptor.getValue().getHmctsDwpState());

        verifyNoMoreInteractions(restTemplate);
        verifyNoMoreInteractions(evidenceManagementService);
        verify(roboticsService).sendCaseToRobotics(any());
    }

    @Test
    @Parameters({"ONLINE", "COR"})
    public void nonReceivedViaPaper_shouldNotBeBulkPrintedAndStateShouldBeUpdated(String receivedVia) throws IOException {
        assertNotNull("SendToBulkPrintHandler must be autowired", bulkPrintHandler);
        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("validAppealCreatedCallback.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = json.replace("PAPER", receivedVia);
        json = json.replace("CREATED_IN_GAPS_FROM", "validAppeal");

        topicConsumer.onMessage(json);

        verifyNoMoreInteractions(restTemplate);
        verifyNoMoreInteractions(evidenceManagementService);
        verify(ccdService).updateCase(any(), any(), eq(EventType.SENT_TO_DWP.getCcdType()), any(), eq("Case state is now sent to DWP"), any());
        verify(roboticsService).sendCaseToRobotics(any());
    }

    @Test
    public void givenADigitalCase_shouldNotBeBulkPrintedAndStateShouldBeUpdatedAndNotSentToRobotics() throws IOException {
        assertNotNull("SendToBulkPrintHandler must be autowired", bulkPrintHandler);
        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("validAppealCreatedCallback.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = json.replace("CREATED_IN_GAPS_FROM", "readyToList");

        topicConsumer.onMessage(json);

        verifyNoMoreInteractions(restTemplate);
        verifyNoMoreInteractions(evidenceManagementService);
        verify(ccdService).updateCase(any(), any(), eq(EventType.SENT_TO_DWP.getCcdType()), any(), eq("Case state is now sent to DWP"), any());
        verifyNoMoreInteractions(roboticsService);
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
        link.href = "http://link.com";
        links.self = link;
        document.links = links;
        return document;
    }
}
