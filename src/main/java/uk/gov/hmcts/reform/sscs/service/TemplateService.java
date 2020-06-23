package uk.gov.hmcts.reform.sscs.service;

import java.time.LocalDate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.DocmosisTemplateConfig;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;


@Service
@Slf4j
public class TemplateService {

    @Autowired
    private DocmosisTemplateConfig docmosisTemplateConfig;

    public Template findTemplate(SscsCaseData caseData) {

        if (caseData.getAppeal().getMrnDetails() != null && caseData.getAppeal().getMrnDetails().getMrnDate() != null) {
            LocalDate mrnDate = LocalDate.parse(caseData.getAppeal().getMrnDetails().getMrnDate());
            if (mrnDate.plusDays(30).isBefore(LocalDate.now())) {
                return getTemplateByLanguagePreference(caseData, DocumentType.DL16);
            } else {
                return getTemplateByLanguagePreference(caseData, DocumentType.DL6);
            }
        }
        return null;
    }

    private Template getTemplateByLanguagePreference(final SscsCaseData caseData, final DocumentType documentType) {
        if (caseData.isLanguagePreferenceWelsh()) {
            return new Template(docmosisTemplateConfig.getTemplate().get(LanguagePreference.WELSH)
                    .get(documentType).get("name"),documentType.getValue());
        }
        return new Template(docmosisTemplateConfig.getTemplate().get(LanguagePreference.ENGLISH)
                .get(documentType).get("name"),documentType.getValue());
    }
}
