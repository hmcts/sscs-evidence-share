package uk.gov.hmcts.reform.sscs.service;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.sscs.SscsEvidenceShareApplication;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocmosisPdfGenerationService;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SscsEvidenceShareApplication.class)
public class DocmosisPdfGenerationIt {

    public static final String FILE_CONTENT = "Welcome to PDF document service";
    public static final Map<String, Object> PLACEHOLDERS = caseDataMap();

    public static final String PDF_SERVICE_URI = "https://docmosis-development.platform.hmcts.net/rs/render";

    private DocmosisPdfGenerationService pdfGenerationService;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    private static Map<String, Object> caseDataMap() {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("ccdId", "123456");

        return dataMap;
    }

    @Before
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        pdfGenerationService = new DocmosisPdfGenerationService(PDF_SERVICE_URI, "bla2", restTemplate);
    }

    @Test
    public void generatePdfDocument() {
        mockServer.expect(requestTo(PDF_SERVICE_URI))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(FILE_CONTENT, MediaType.APPLICATION_JSON));

        byte[] result = pdfGenerationService.generatePdf(DocumentHolder.builder().template(new Template("dl6-template.doc", "dl6")).placeholders(PLACEHOLDERS).build());
        assertThat(result, is(notNullValue()));
        assertThat(result, is(equalTo(FILE_CONTENT.getBytes())));
    }

    @Test
    public void generatePdfDocument400() {
        mockServer.expect(requestTo(PDF_SERVICE_URI))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withBadRequest());

        try {
            pdfGenerationService.generatePdf(DocumentHolder.builder().template(new Template("dl6-template.doc", "dl6")).placeholders(PLACEHOLDERS).build());
            fail("should have thrown bad-request exception");
        } catch (PdfGenerationException e) {
            HttpStatus httpStatus = ((HttpClientErrorException) e.getCause()).getStatusCode();
            assertThat(httpStatus, is(HttpStatus.BAD_REQUEST));
        }
    }
}
