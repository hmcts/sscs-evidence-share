package uk.gov.hmcts.reform.sscs.factory;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.docmosis.domain.Template.DL6;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.service.DocumentPlaceholderService;
import uk.gov.hmcts.reform.sscs.service.TemplateService;

public class DocumentRequestFactoryTest {

    @Mock
    private DocumentPlaceholderService documentPlaceholderService;

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private DocumentRequestFactory factory;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void givenACaseData_thenCreateTheDocumentHolderObject() {
        SscsCaseData caseData = SscsCaseData.builder().build();
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("Test", "Value");
        LocalDateTime now = LocalDateTime.now();

        given(templateService.findTemplate(caseData)).willReturn(DL6);
        given(documentPlaceholderService.generatePlaceholders(caseData, now)).willReturn(placeholders);

        DocumentHolder holder = factory.create(caseData, now);

        assertEquals(holder.getTemplate(), DL6);
        assertEquals(holder.getPlaceholders(), placeholders);
    }
}
