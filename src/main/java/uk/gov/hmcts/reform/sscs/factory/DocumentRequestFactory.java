package uk.gov.hmcts.reform.sscs.factory;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;
import uk.gov.hmcts.reform.sscs.service.TemplateService;
import uk.gov.hmcts.reform.sscs.service.placeholders.Dl6AndDl16PlaceholderService;

@Component
@Slf4j
public class DocumentRequestFactory {

    @Autowired
    private Dl6AndDl16PlaceholderService dl6AndDl16PlaceholderService;

    @Autowired
    private TemplateService templateService;

    public <E extends SscsCaseData> DocumentHolder create(E caseData, LocalDateTime caseCreatedDate) {
        Template template = templateService.findTemplate(caseData);
        Map<String, Object> placeholders = dl6AndDl16PlaceholderService.populatePlaceholders(caseData, caseCreatedDate);

        return DocumentHolder.builder()
            .template(template)
            .placeholders(placeholders)
            .pdfArchiveMode(true)
            .build();
    }
}
