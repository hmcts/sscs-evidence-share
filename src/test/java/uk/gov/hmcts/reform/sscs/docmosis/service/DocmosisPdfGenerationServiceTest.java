package uk.gov.hmcts.reform.sscs.docmosis.service;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.sscs.docmosis.config.PdfDocumentConfig;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;

public class DocmosisPdfGenerationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PdfDocumentConfig pdfDocumentConfig;

    @InjectMocks
    private DocmosisPdfGenerationService pdfGenerationService;

    public static final Map<String, Object> PLACEHOLDERS = caseDataMap();

    public static final String FILE_CONTENT = "Welcome to PDF document service";

    @Before
    public void setup() {
        initMocks(this);

        ReflectionTestUtils.setField(pdfGenerationService, "pdfServiceEndpoint", "bla");
        ReflectionTestUtils.setField(pdfGenerationService, "pdfServiceAccessKey", "bla2");
    }

    private static Map<String, Object> caseDataMap() {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("PBANumber", "PBA123456");

        return dataMap;
    }

    @Test
    public void givenADocumentHolder_thenGenerateAPdf() {
        doReturn(createResponseEntity()).when(restTemplate).postForEntity(anyString(), any(), eq(byte[].class));

        byte[] result = pdfGenerationService.generatePdf(DocumentHolder.builder().template(Template.DL6).placeholders(PLACEHOLDERS).build());
        assertThat(result, is(notNullValue()));
        assertThat(result, is(equalTo(FILE_CONTENT.getBytes())));
    }

    private ResponseEntity<byte[]> createResponseEntity() {
        return new ResponseEntity<>(FILE_CONTENT.getBytes(), HttpStatus.OK);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyTemplateName_thenThrowIllegalArgumentException() {
        pdfGenerationService.generatePdf(DocumentHolder.builder().template(null).placeholders(PLACEHOLDERS).build());
    }

    @Test(expected = NullPointerException.class)
    public void emptyPlaceholders_thenThrowIllegalArgumentException() {
        pdfGenerationService.generatePdf(DocumentHolder.builder().template(Template.DL6).placeholders(null).build());
    }
}
