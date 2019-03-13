package uk.gov.hmcts.reform.sscs.docmosis.service;

import static uk.gov.hmcts.reform.sscs.docmosis.domain.Template.DL6;

import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;

@Service
@Slf4j
public class TemplateService {

    public Template findTemplate(SscsCaseData caseData) {

        if (caseData.getAppeal().getMrnDetails() != null && caseData.getAppeal().getMrnDetails().getMrnDate() != null) {
            LocalDate mrnDate = LocalDate.parse(caseData.getAppeal().getMrnDetails().getMrnDate());
            if (mrnDate.plusMonths(13L).isBefore(LocalDate.now())) {
                return DL6;
            }
        }
        return null;
    }
}
