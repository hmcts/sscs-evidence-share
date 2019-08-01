package uk.gov.hmcts.reform.sscs.service.placeholders;

import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.config.PdfDocumentConfig;

@Service
public class CommonPlaceholderService {

    @Autowired
    private PdfDocumentConfig pdfDocumentConfig;

    public void populatePlaceholders(SscsCaseData caseData, Map<String, Object> placeholders) {
        Appeal appeal = caseData.getAppeal();
        placeholders.put(PlaceholderConstants.BENEFIT_TYPE_LITERAL, appeal.getBenefitType().getDescription().toUpperCase());
        placeholders.put(PlaceholderConstants.APPELLANT_FULL_NAME_LITERAL, appeal.getAppellant().getName().getAbbreviatedFullName());
        placeholders.put(PlaceholderConstants.CASE_ID_LITERAL, caseData.getCcdCaseId());
        placeholders.put(PlaceholderConstants.NINO_LITERAL, PlaceholderUtility.defaultToEmptyStringIfNull(appeal.getAppellant().getIdentity().getNino()));
        placeholders.put(PlaceholderConstants.SSCS_URL_LITERAL, PlaceholderConstants.SSCS_URL);
        placeholders.put(PlaceholderConstants.GENERATED_DATE_LITERAL, LocalDateTime.now().toLocalDate().toString());
        placeholders.put(pdfDocumentConfig.getHmctsImgKey(), pdfDocumentConfig.getHmctsImgVal());
    }
}
