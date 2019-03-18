package uk.gov.hmcts.reform.sscs.docmosis.service;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class DocumentPlaceholderServiceTest {

    private DocumentPlaceholderService service;

    @Before
    public void setup() {
        service = new DocumentPlaceholderService();
    }

    @Test
    public void givenACaseData_thenGenerateThePlaceholderMappings() {
        SscsCaseData caseData = SscsCaseData.builder().caseCreated("01/01/2019").build();

        Map<String, Object> result = service.generatePlaceholders(caseData);

        assertEquals(result.get("caseCreatedDate"), "01/01/2019");
    }
}
