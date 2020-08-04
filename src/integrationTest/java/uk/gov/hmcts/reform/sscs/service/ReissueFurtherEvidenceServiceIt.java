package uk.gov.hmcts.reform.sscs.service;

import static com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import junitparams.JUnitParamsRunner;
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
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.sscs.callback.handlers.ReissueFurtherEvidenceHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.docmosis.domain.PdfDocumentRequest;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.servicebus.TopicConsumer;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:config/application_it.properties")
public class ReissueFurtherEvidenceServiceIt {

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
    private EvidenceManagementService evidenceManagementService;

    @MockBean
    @SuppressWarnings({"PMD.UnusedPrivateField"})
    private CcdService ccdService;

    @MockBean
    @SuppressWarnings({"PMD.UnusedPrivateField"})
    private UpdateCcdCaseService updateCcdCaseService;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private BulkPrintService bulkPrintService;

    @Autowired
    private ReissueFurtherEvidenceHandler handler;

    @Autowired
    private TopicConsumer topicConsumer;

    @Captor
    ArgumentCaptor<ArrayList<Pdf>> documentCaptor;

    @Captor
    ArgumentCaptor<PdfDocumentRequest> pdfDocumentRequest;

    private ObjectMapper mapper() {
        Jackson2ObjectMapperBuilder objectMapperBuilder =
            new Jackson2ObjectMapperBuilder()
                .featuresToEnable(READ_ENUMS_USING_TO_STRING)
                .featuresToEnable(READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .featuresToEnable(WRITE_ENUMS_USING_TO_STRING)
                .serializationInclusion(JsonInclude.Include.NON_ABSENT);

        ObjectMapper mapper = objectMapperBuilder.createXmlMapper(false).build();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    public SscsCaseCallbackDeserializer sscsDeserializer() {
        return new SscsCaseCallbackDeserializer(mapper());
    }

    private static final String FILE_CONTENT = "Welcome to PDF document service";

    private Session session = Session.getInstance(new Properties());

    private Optional<UUID> expectedOptionalUuid = Optional.of(UUID.fromString("0f14d0ab-9605-4a62-a9e4-5ed26688389b"));

    @Before
    public void setup() {
        MimeMessage message = new MimeMessage(session);
        assertNotNull("ReissueFurtherEvidenceHandler must be autowired", handler);

    }

    @Test
    public void appealWithAppellantAndFurtherEvidenceFromAppellant_shouldSend609_97ToAppellantAndNotSend609_98() throws IOException {

        doReturn(new ResponseEntity<>(FILE_CONTENT.getBytes(), HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), pdfDocumentRequest.capture(), eq(byte[].class));

        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any())).thenReturn(expectedOptionalUuid);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("issueFurtherEvidenceCallback.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        topicConsumer.onMessage(json);

        verify(bulkPrintService).sendToBulkPrint(any(), any());

        assertEquals(1, documentCaptor.getAllValues().size());
        assertEquals(2, documentCaptor.getAllValues().get(0).size());

        assertEquals("Sarah Smith", pdfDocumentRequest.getAllValues().get(0).getData().get("name"));
        assertEquals("609-97-template (original sender)", documentCaptor.getAllValues().get(0).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(0).get(1).getName());
    }

    @Test
    public void doesItNpe() throws IOException {

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("issueFurtherEvidenceCallback.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());


        SscsCaseCallbackDeserializer deserializer =  sscsDeserializer();

        Callback<SscsCaseData> callback = deserializer.deserialize(json);

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();

        SscsCaseData caseData  = caseDetails.getCaseData();

        Appeal appeal = caseData.getAppeal();

        String name = appeal.getAppellant().getName().getAbbreviatedFullName();

        System.out.println("Name is " + name);

        assertTrue(true);
    }

    @Test
    public void appealWithAppellantAndRepFurtherEvidenceFromAppellant_shouldSend609_97ToAppellantAnd609_98ToRep() throws IOException {

        doReturn(new ResponseEntity<>(FILE_CONTENT.getBytes(), HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), pdfDocumentRequest.capture(), eq(byte[].class));

        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any())).thenReturn(expectedOptionalUuid);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("issueFurtherEvidenceCallbackWithRep.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        topicConsumer.onMessage(json);

        verify(bulkPrintService, times(2)).sendToBulkPrint(any(), any());

        assertEquals(2, documentCaptor.getAllValues().size());
        assertEquals(2, documentCaptor.getAllValues().get(0).size());
        assertEquals(2, documentCaptor.getAllValues().get(1).size());

        assertEquals("Sarah Smith", pdfDocumentRequest.getAllValues().get(0).getData().get("name"));
        assertEquals("609-97-template (original sender)", documentCaptor.getAllValues().get(0).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(0).get(1).getName());

        assertEquals("Peter Hyland", pdfDocumentRequest.getAllValues().get(1).getData().get("name"));
        assertEquals("609-98-template (other parties)", documentCaptor.getAllValues().get(1).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(1).get(1).getName());
    }

    @Test
    public void appealWithAppellantAndRepFurtherEvidenceFromRep_shouldSend609_97ToRepAnd609_98ToAppellant() throws IOException {

        doReturn(new ResponseEntity<>(FILE_CONTENT.getBytes(), HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), pdfDocumentRequest.capture(), eq(byte[].class));

        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any())).thenReturn(expectedOptionalUuid);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("issueFurtherEvidenceCallbackWithRepEvidence.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        topicConsumer.onMessage(json);

        verify(bulkPrintService, times(2)).sendToBulkPrint(any(), any());

        assertEquals(2, documentCaptor.getAllValues().size());
        assertEquals(2, documentCaptor.getAllValues().get(0).size());
        assertEquals(2, documentCaptor.getAllValues().get(1).size());

        assertEquals("Peter Hyland", pdfDocumentRequest.getAllValues().get(0).getData().get("name"));
        assertEquals("609-97-template (original sender)", documentCaptor.getAllValues().get(0).get(0).getName());
        assertEquals("rep-document", documentCaptor.getAllValues().get(0).get(1).getName());

        assertEquals("Sarah Smith", pdfDocumentRequest.getAllValues().get(1).getData().get("name"));
        assertEquals("609-98-template (other parties)", documentCaptor.getAllValues().get(1).get(0).getName());
        assertEquals("rep-document", documentCaptor.getAllValues().get(1).get(1).getName());
    }

    @Test
    public void appealWithAppellantFurtherEvidenceAndRepFurtherEvidence_shouldSend609_97ToRepAndAppellantAnd609_98ToAppellantAndRep() throws IOException {

        doReturn(new ResponseEntity<>(FILE_CONTENT.getBytes(), HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), pdfDocumentRequest.capture(), eq(byte[].class));

        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any())).thenReturn(expectedOptionalUuid);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("issueFurtherEvidenceCallbackWithAppellantEvidenceAndRepEvidence.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        topicConsumer.onMessage(json);

        verify(bulkPrintService, times(4)).sendToBulkPrint(any(), any());

        assertEquals(4, documentCaptor.getAllValues().size());
        assertEquals(2, documentCaptor.getAllValues().get(0).size());
        assertEquals(2, documentCaptor.getAllValues().get(1).size());
        assertEquals(2, documentCaptor.getAllValues().get(2).size());
        assertEquals(2, documentCaptor.getAllValues().get(3).size());

        assertEquals("Sarah Smith", pdfDocumentRequest.getAllValues().get(0).getData().get("name"));
        assertEquals("609-97-template (original sender)", documentCaptor.getAllValues().get(0).get(0).getName());
        assertEquals("appellant-document", documentCaptor.getAllValues().get(0).get(1).getName());

        assertEquals("Peter Hyland", pdfDocumentRequest.getAllValues().get(1).getData().get("name"));
        assertEquals("609-98-template (other parties)", documentCaptor.getAllValues().get(1).get(0).getName());
        assertEquals("appellant-document", documentCaptor.getAllValues().get(1).get(1).getName());

        assertEquals("Peter Hyland", pdfDocumentRequest.getAllValues().get(2).getData().get("name"));
        assertEquals("609-97-template (original sender)", documentCaptor.getAllValues().get(2).get(0).getName());
        assertEquals("rep-document", documentCaptor.getAllValues().get(2).get(1).getName());

        assertEquals("Sarah Smith", pdfDocumentRequest.getAllValues().get(3).getData().get("name"));
        assertEquals("609-98-template (other parties)", documentCaptor.getAllValues().get(3).get(0).getName());
        assertEquals("rep-document", documentCaptor.getAllValues().get(3).get(1).getName());
    }

    @Test
    public void appealWithFurtherEvidenceFromDwp_shouldNotSend609_97And609_98ToAppellant() throws IOException {

        doReturn(new ResponseEntity<>(FILE_CONTENT.getBytes(), HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), pdfDocumentRequest.capture(), eq(byte[].class));

        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any())).thenReturn(expectedOptionalUuid);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("issueFurtherEvidenceCallbackWithDwpEvidence.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        topicConsumer.onMessage(json);

        verify(bulkPrintService).sendToBulkPrint(any(), any());

        assertEquals(1, documentCaptor.getAllValues().size());
        assertEquals(2, documentCaptor.getAllValues().get(0).size());

        assertEquals("Sarah Smith", pdfDocumentRequest.getAllValues().get(0).getData().get("name"));
        assertEquals("609-98-template (other parties)", documentCaptor.getAllValues().get(0).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(0).get(1).getName());
    }

    @Test
    public void appealWithRepAndFurtherEvidenceFromDwp_shouldNotSend609_97AndSend609_98ToRepAndAppellant() throws IOException {

        doReturn(new ResponseEntity<>(FILE_CONTENT.getBytes(), HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), pdfDocumentRequest.capture(), eq(byte[].class));

        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any())).thenReturn(expectedOptionalUuid);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("issueFurtherEvidenceCallbackWithRepAndEvidenceFromDwp.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        topicConsumer.onMessage(json);

        verify(bulkPrintService, times(2)).sendToBulkPrint(any(), any());

        assertEquals(2, documentCaptor.getAllValues().size());
        assertEquals(2, documentCaptor.getAllValues().get(0).size());
        assertEquals(2, documentCaptor.getAllValues().get(1).size());

        assertEquals("Sarah Smith", pdfDocumentRequest.getAllValues().get(0).getData().get("name"));
        assertEquals("609-98-template (other parties)", documentCaptor.getAllValues().get(0).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(0).get(1).getName());

        assertEquals("Peter Hyland", pdfDocumentRequest.getAllValues().get(1).getData().get("name"));
        assertEquals("609-98-template (other parties)", documentCaptor.getAllValues().get(1).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(1).get(1).getName());
    }
}
