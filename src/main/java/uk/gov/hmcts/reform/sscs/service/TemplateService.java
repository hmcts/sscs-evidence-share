package uk.gov.hmcts.reform.sscs.service;

import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;

@Service
@Slf4j
public class TemplateService {

    private final String dl6TemplateName;

    private final String dl16TemplateName;

    public TemplateService(@Value("${docmosis.template.dl6.name}") String dl6TemplateName,
                           @Value("${docmosis.template.dl16.name}") String dl16TemplateName) {
        this.dl6TemplateName = dl6TemplateName;
        this.dl16TemplateName = dl16TemplateName;
    }

    public Template findTemplate(SscsCaseData caseData) {

        if (caseData.getAppeal().getMrnDetails() != null && caseData.getAppeal().getMrnDetails().getMrnDate() != null) {
            LocalDate mrnDate = LocalDate.parse(caseData.getAppeal().getMrnDetails().getMrnDate());
            if (mrnDate.plusDays(30).isBefore(LocalDate.now())) {
                return new Template(dl16TemplateName, "dl16");
            } else {
                return new Template(dl6TemplateName, "dl6");
            }
        }
        return null;
    }
}
