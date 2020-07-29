package uk.gov.hmcts.reform.sscs.service;

import java.time.LocalDate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
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
                return new Template(docmosisTemplateConfig.getTemplate().get(caseData.getLanguagePreference())
                        .get(DocumentType.DL16.getValue()).get("name"), DocumentType.DL16.getValue());
            } else {
                return new Template(docmosisTemplateConfig.getTemplate().get(caseData.getLanguagePreference())
                        .get(DocumentType.DL6.getValue()).get("name"), DocumentType.DL6.getValue());
            }
        }
        return null;
    }
}
