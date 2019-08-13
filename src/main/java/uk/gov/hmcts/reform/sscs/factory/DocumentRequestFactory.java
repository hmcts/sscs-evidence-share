package uk.gov.hmcts.reform.sscs.factory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookup;
import uk.gov.hmcts.reform.sscs.service.TemplateService;
import uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderService;

@Component
@Slf4j
public class DocumentRequestFactory {

    @Autowired
    private PlaceholderService placeholderService;

    @Autowired
    private DwpAddressLookup dwpAddressLookup;

    @Autowired
    private TemplateService templateService;

    public <E extends SscsCaseData> DocumentHolder create(E caseData, LocalDateTime caseCreatedDate) {
        Template template = templateService.findTemplate(caseData);
        Map<String, Object> placeholders = new ConcurrentHashMap<>();

        placeholderService.build(caseData, placeholders, dwpAddressLookup.lookupDwpAddress(caseData), caseCreatedDate);

        return DocumentHolder.builder()
            .template(template)
            .placeholders(placeholders)
            .pdfArchiveMode(true)
            .build();
    }
}
