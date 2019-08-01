package uk.gov.hmcts.reform.sscs.factory;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;
import uk.gov.hmcts.reform.sscs.service.TemplateService;
import uk.gov.hmcts.reform.sscs.service.placeholders.Dl6AndDl16PlaceholderService;

public class DocumentRequestFactoryTest {

    @Mock
    private Dl6AndDl16PlaceholderService dl6AndDl16PlaceholderService;

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
        Template template = new Template("bla", "bla2");

        given(templateService.findTemplate(caseData)).willReturn(template);
        given(dl6AndDl16PlaceholderService.populatePlaceholders(caseData, now)).willReturn(placeholders);

        DocumentHolder holder = factory.create(caseData, now);

        assertEquals(holder.getTemplate(), template);
        assertEquals(holder.getPlaceholders(), placeholders);
        assertEquals(holder.isPdfArchiveMode(), true);
    }
}
