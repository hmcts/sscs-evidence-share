package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static uk.gov.hmcts.reform.sscs.docmosis.domain.Template.DL6;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.Test;
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
    public void givenACaseDataWithMrnWithin13Months_thenGenerateThePlaceholderMappings() {
        LocalDate date = LocalDate.now();
        String dateAsString = date.format(formatter);

        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder().mrnDate(dateAsString).build())
            .build()).build();

        Template result = service.findTemplate(caseData);

        assertEquals(result, DL6);
    }

    @Test
    public void givenACaseDataWithMrnGreaterThan13Months_thenGenerateThePlaceholderMappings() {
        LocalDate date = LocalDate.now().minusMonths(13).minusDays(1);
        String dateAsString = date.format(formatter);

        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder().mrnDate(dateAsString).build())
                .build())
            .build();

        Template result = service.findTemplate(caseData);

        assertNull(result);
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
