package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;

public class TemplateServiceTest {

    private TemplateService service;

    private DateTimeFormatter formatter;

    @Before
    public void setup() {
        service = new TemplateService();
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }

    @Test
    public void givenACaseDataWithMrnWithin30Days_thenGenerateThePlaceholderMappingsForDl6() {
        LocalDate date = LocalDate.now();
        String dateAsString = date.format(formatter);
        ReflectionTestUtils.setField(service, "dl6TemplateName", "dl6-template.doc");

        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder().mrnDate(dateAsString).build())
            .build()).build();

        Template result = service.findTemplate(caseData);

        assertEquals("DL6", result.getHmctsDocName());
    }

    @Test
    public void givenACaseDataWithMrnEqualTo30Days_thenGenerateThePlaceholderMappings() {
        LocalDate date = LocalDate.now().minusDays(30);
        String dateAsString = date.format(formatter);
        ReflectionTestUtils.setField(service, "dl6TemplateName", "dl6-template.doc");

        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder().mrnDate(dateAsString).build())
                .build())
            .build();

        Template result = service.findTemplate(caseData);

        assertEquals("DL6", result.getHmctsDocName());
    }

    @Test
    public void givenACaseDataWithMrnGreaterThan30Days_thenGenerateThePlaceholderMappingsForDl16() {
        LocalDate date = LocalDate.now().minusDays(31);
        String dateAsString = date.format(formatter);
        ReflectionTestUtils.setField(service, "dl16TemplateName", "dl16-template.doc");

        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder().mrnDate(dateAsString).build())
                .build())
            .build();

        Template result = service.findTemplate(caseData);

        assertEquals("DL16", result.getHmctsDocName());
    }

    @Test
    public void givenACaseDataWithMrnMissing_thenGenerateThePlaceholderMappings() {
        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .mrnDetails(null)
                .build())
            .build();

        Template result = service.findTemplate(caseData);

        assertNull(result);
    }
}
