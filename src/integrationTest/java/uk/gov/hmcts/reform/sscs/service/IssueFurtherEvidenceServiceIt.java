package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.sscs.callback.handlers.IssueFurtherEvidenceHandler;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.docmosis.domain.PdfDocumentRequest;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.servicebus.TopicConsumer;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest
public class IssueFurtherEvidenceServiceIt {

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
    private EvidenceManagementService evidenceManagementService;

    @MockBean
    private CcdService ccdService;

    @MockBean
    private UpdateCcdCaseService updateCcdCaseService;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private BulkPrintService bulkPrintService;

    @Autowired
    private IssueFurtherEvidenceHandler handler;

    @Autowired
    private TopicConsumer topicConsumer;

    @Captor
    ArgumentCaptor<ArrayList<Pdf>> documentCaptor;

    @Captor
    ArgumentCaptor<PdfDocumentRequest> pdfDocumentRequest;

    private static final String FILE_CONTENT = "Welcome to PDF document service";

    protected Session session = Session.getInstance(new Properties());

    protected MimeMessage message;

    Optional<UUID> expectedOptionalUuid = Optional.of(UUID.fromString("0f14d0ab-9605-4a62-a9e4-5ed26688389b"));

    @Before
    public void setup() {
        message = new MimeMessage(session);
    }

    @Test
    public void appealWithAppellantAndFurtherEvidenceFromAppellant_shouldSend609_97ToAppellantAnd609_98ToDwp() throws IOException {
        assertNotNull("IssueFurtherEvidenceHandler must be autowired", handler);

        doReturn(new ResponseEntity<>(FILE_CONTENT.getBytes(), HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), pdfDocumentRequest.capture(), eq(byte[].class));

        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any())).thenReturn(expectedOptionalUuid);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("issueFurtherEvidenceCallback.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        topicConsumer.onMessage(json);

        verify(bulkPrintService, times(2)).sendToBulkPrint(any(), any());

        assertEquals(2, documentCaptor.getAllValues().get(0).size());
        assertEquals(2, documentCaptor.getAllValues().get(1).size());

        assertEquals("Sarah Smith", pdfDocumentRequest.getAllValues().get(0).getData().get("name"));
        assertEquals("609-97-template (original sender)", documentCaptor.getAllValues().get(0).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(0).get(1).getName());

        assertNull(pdfDocumentRequest.getAllValues().get(1).getData().get("name"));
        assertEquals("609-98-template (DWP)", documentCaptor.getAllValues().get(1).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(1).get(1).getName());
    }

    @Test
    public void appealWithAppellantAndRepFurtherEvidenceFromAppellant_shouldSend609_97ToAppellantAnd609_98ToRepAndDwp() throws IOException {
        assertNotNull("IssueFurtherEvidenceHandler must be autowired", handler);

        doReturn(new ResponseEntity<>(FILE_CONTENT.getBytes(), HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), pdfDocumentRequest.capture(), eq(byte[].class));

        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any())).thenReturn(expectedOptionalUuid);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("issueFurtherEvidenceCallbackWithRep.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        topicConsumer.onMessage(json);

        verify(bulkPrintService, times(3)).sendToBulkPrint(any(), any());

        assertEquals(2, documentCaptor.getAllValues().get(0).size());
        assertEquals(2, documentCaptor.getAllValues().get(1).size());
        assertEquals(2, documentCaptor.getAllValues().get(2).size());

        assertEquals("Sarah Smith", pdfDocumentRequest.getAllValues().get(0).getData().get("name"));
        assertEquals("609-97-template (original sender)", documentCaptor.getAllValues().get(0).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(0).get(1).getName());

        assertEquals("Peter Hyland", pdfDocumentRequest.getAllValues().get(1).getData().get("name"));
        assertEquals("609-98-template (other parties)", documentCaptor.getAllValues().get(1).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(1).get(1).getName());

        assertNull(pdfDocumentRequest.getAllValues().get(2).getData().get("name"));
        assertEquals("609-98-template (DWP)", documentCaptor.getAllValues().get(2).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(2).get(1).getName());
    }

    @Test
    public void appealWithAppellantAndRepFurtherEvidenceFromRep_shouldSend609_97ToRepAnd609_98ToAppellantAndDwp() throws IOException {
        assertNotNull("IssueFurtherEvidenceHandler must be autowired", handler);

        doReturn(new ResponseEntity<>(FILE_CONTENT.getBytes(), HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), pdfDocumentRequest.capture(), eq(byte[].class));

        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any())).thenReturn(expectedOptionalUuid);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("issueFurtherEvidenceCallbackWithRepEvidence.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        topicConsumer.onMessage(json);

        verify(bulkPrintService, times(3)).sendToBulkPrint(any(), any());

        assertEquals(2, documentCaptor.getAllValues().get(0).size());
        assertEquals(2, documentCaptor.getAllValues().get(1).size());
        assertEquals(2, documentCaptor.getAllValues().get(2).size());

        assertEquals("Peter Hyland", pdfDocumentRequest.getAllValues().get(0).getData().get("name"));
        assertEquals("609-97-template (original sender)", documentCaptor.getAllValues().get(0).get(0).getName());
        assertEquals("rep-document", documentCaptor.getAllValues().get(0).get(1).getName());

        assertEquals("Sarah Smith", pdfDocumentRequest.getAllValues().get(1).getData().get("name"));
        assertEquals("609-98-template (other parties)", documentCaptor.getAllValues().get(1).get(0).getName());
        assertEquals("rep-document", documentCaptor.getAllValues().get(1).get(1).getName());

        assertNull(pdfDocumentRequest.getAllValues().get(2).getData().get("name"));
        assertEquals("609-98-template (DWP)", documentCaptor.getAllValues().get(2).get(0).getName());
        assertEquals("rep-document", documentCaptor.getAllValues().get(2).get(1).getName());
    }

    @Test
    public void appealWithAppellantFurtherEvidenceAndRepFurtherEvidence_shouldSend609_97ToRepAndAppellantAnd609_98ToAppellantAndRepAndDwp() throws IOException {
        assertNotNull("IssueFurtherEvidenceHandler must be autowired", handler);

        doReturn(new ResponseEntity<>(FILE_CONTENT.getBytes(), HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), pdfDocumentRequest.capture(), eq(byte[].class));

        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any())).thenReturn(expectedOptionalUuid);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("issueFurtherEvidenceCallbackWithAppellantEvidenceAndRepEvidence.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        topicConsumer.onMessage(json);

        verify(bulkPrintService, times(6)).sendToBulkPrint(any(), any());

        assertEquals(2, documentCaptor.getAllValues().get(0).size());
        assertEquals(2, documentCaptor.getAllValues().get(1).size());
        assertEquals(2, documentCaptor.getAllValues().get(2).size());
        assertEquals(2, documentCaptor.getAllValues().get(3).size());
        assertEquals(2, documentCaptor.getAllValues().get(4).size());
        assertEquals(2, documentCaptor.getAllValues().get(5).size());

        assertEquals("Sarah Smith", pdfDocumentRequest.getAllValues().get(0).getData().get("name"));
        assertEquals("609-97-template (original sender)", documentCaptor.getAllValues().get(0).get(0).getName());
        assertEquals("appellant-document", documentCaptor.getAllValues().get(0).get(1).getName());

        assertEquals("Peter Hyland", pdfDocumentRequest.getAllValues().get(1).getData().get("name"));
        assertEquals("609-98-template (other parties)", documentCaptor.getAllValues().get(1).get(0).getName());
        assertEquals("appellant-document", documentCaptor.getAllValues().get(1).get(1).getName());

        assertNull(pdfDocumentRequest.getAllValues().get(2).getData().get("name"));
        assertEquals("609-98-template (DWP)", documentCaptor.getAllValues().get(2).get(0).getName());
        assertEquals("appellant-document", documentCaptor.getAllValues().get(2).get(1).getName());

        assertEquals("Peter Hyland", pdfDocumentRequest.getAllValues().get(3).getData().get("name"));
        assertEquals("609-97-template (original sender)", documentCaptor.getAllValues().get(3).get(0).getName());
        assertEquals("rep-document", documentCaptor.getAllValues().get(3).get(1).getName());

        assertEquals("Sarah Smith", pdfDocumentRequest.getAllValues().get(4).getData().get("name"));
        assertEquals("609-98-template (other parties)", documentCaptor.getAllValues().get(4).get(0).getName());
        assertEquals("rep-document", documentCaptor.getAllValues().get(4).get(1).getName());

        assertNull(pdfDocumentRequest.getAllValues().get(5).getData().get("name"));
        assertEquals("609-98-template (DWP)", documentCaptor.getAllValues().get(5).get(0).getName());
        assertEquals("rep-document", documentCaptor.getAllValues().get(5).get(1).getName());
    }

    @Test
    public void appealWithFurtherEvidenceFromDwp_shouldSend609_97ToDwpAnd609_98ToAppellant() throws IOException {
        assertNotNull("IssueFurtherEvidenceHandler must be autowired", handler);

        doReturn(new ResponseEntity<>(FILE_CONTENT.getBytes(), HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), pdfDocumentRequest.capture(), eq(byte[].class));

        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any())).thenReturn(expectedOptionalUuid);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("issueFurtherEvidenceCallbackWithDwpEvidence.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        topicConsumer.onMessage(json);

        verify(bulkPrintService, times(2)).sendToBulkPrint(any(), any());

        assertEquals(2, documentCaptor.getAllValues().get(0).size());
        assertEquals(2, documentCaptor.getAllValues().get(1).size());

        assertNull(pdfDocumentRequest.getAllValues().get(0).getData().get("name"));
        assertEquals("609-97-template (original sender)", documentCaptor.getAllValues().get(0).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(0).get(1).getName());

        assertEquals("Sarah Smith", pdfDocumentRequest.getAllValues().get(1).getData().get("name"));
        assertEquals("609-98-template (other parties)", documentCaptor.getAllValues().get(1).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(1).get(1).getName());
    }

    @Test
    public void appealWithRepAndFurtherEvidenceFromDwp_shouldSend609_97ToDwpAnd609_98ToRepAndAppellant() throws IOException {
        assertNotNull("IssueFurtherEvidenceHandler must be autowired", handler);

        doReturn(new ResponseEntity<>(FILE_CONTENT.getBytes(), HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), pdfDocumentRequest.capture(), eq(byte[].class));

        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any())).thenReturn(expectedOptionalUuid);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("issueFurtherEvidenceCallbackWithRepAndEvidenceFromDwp.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        topicConsumer.onMessage(json);

        verify(bulkPrintService, times(3)).sendToBulkPrint(any(), any());

        assertEquals(2, documentCaptor.getAllValues().get(0).size());
        assertEquals(2, documentCaptor.getAllValues().get(1).size());
        assertEquals(2, documentCaptor.getAllValues().get(2).size());

        assertNull(pdfDocumentRequest.getAllValues().get(0).getData().get("name"));
        assertEquals("609-97-template (original sender)", documentCaptor.getAllValues().get(0).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(0).get(1).getName());

        assertEquals("Sarah Smith", pdfDocumentRequest.getAllValues().get(1).getData().get("name"));
        assertEquals("609-98-template (other parties)", documentCaptor.getAllValues().get(1).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(1).get(1).getName());

        assertEquals("Peter Hyland", pdfDocumentRequest.getAllValues().get(2).getData().get("name"));
        assertEquals("609-98-template (other parties)", documentCaptor.getAllValues().get(2).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(2).get(1).getName());
    }
}
