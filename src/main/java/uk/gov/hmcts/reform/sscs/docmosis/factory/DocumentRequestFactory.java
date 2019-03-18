package uk.gov.hmcts.reform.sscs.docmosis.factory;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocumentPlaceholderService;
import uk.gov.hmcts.reform.sscs.docmosis.service.TemplateService;

@Component
@Slf4j
public class DocumentRequestFactory {

    @Autowired
    private DocumentPlaceholderService documentPlaceholderService;

    @Autowired
    private TemplateService templateService;

    public <E extends SscsCaseData> DocumentHolder create(E caseData) {
        Template template = templateService.findTemplate(caseData);
        Map<String, Object> placeholders = documentPlaceholderService.generatePlaceholders(caseData);

        return DocumentHolder.builder()
            .template(template)
            .placeholders(placeholders)
            .build();
    }
}
