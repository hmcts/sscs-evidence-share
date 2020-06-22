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

    private final String dl6WelshTemplateName;

    private final String dl16WelshTemplateName;

    public TemplateService(@Value("${docmosis.template.dl6.name}") String dl6TemplateName,
                           @Value("${docmosis.template.dl16.name}") String dl16TemplateName,
                           @Value("${docmosis.template.dl6-welsh.name}") String dl6WelshTemplateName,
                           @Value("${docmosis.template.dl16-welsh.name}") String dl16WelshTemplateName) {
        this.dl6TemplateName = dl6TemplateName;
        this.dl16TemplateName = dl16TemplateName;
        this.dl6WelshTemplateName = dl6WelshTemplateName;
        this.dl16WelshTemplateName =  dl16WelshTemplateName;
    }

    public Template findTemplate(SscsCaseData caseData) {

        if (caseData.getAppeal().getMrnDetails() != null && caseData.getAppeal().getMrnDetails().getMrnDate() != null) {
            LocalDate mrnDate = LocalDate.parse(caseData.getAppeal().getMrnDetails().getMrnDate());
            if (mrnDate.plusDays(30).isBefore(LocalDate.now())) {
                return getTemplateByLanguagePreference(caseData, dl16TemplateName, dl16WelshTemplateName,"dl16",
                    "dl16-welsh");
            } else {
                return getTemplateByLanguagePreference(caseData, dl6TemplateName, dl6WelshTemplateName,"dl6",
                    "dl6-welsh");
            }
        }
        return null;
    }

    private Template getTemplateByLanguagePreference(SscsCaseData caseData, String englishTemplate,
                                                     String welshTemplate,
                                                     String hmctsEnDocName, String hmctsCyDocName) {
        if (caseData.isLanguagePreferenceWelsh()) {
            return new Template(welshTemplate,hmctsCyDocName);
        }
        return new Template(englishTemplate,hmctsEnDocName);
    }
}
