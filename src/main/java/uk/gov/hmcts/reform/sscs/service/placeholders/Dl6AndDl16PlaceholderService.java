package uk.gov.hmcts.reform.sscs.service.placeholders;

import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.APPELLANT_FULL_NAME_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.BENEFIT_TYPE_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.CASE_CREATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.CASE_ID_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.GENERATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.NINO_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.SSCS_URL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.SSCS_URL_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderUtility.defaultToEmptyStringIfNull;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.config.PdfDocumentConfig;

@Service
@Slf4j
public class Dl6AndDl16PlaceholderService {

    @Autowired
    private DwpAddressPlaceholderService dwpAddressPlaceholderService;
    @Autowired
    private RpcPlaceholderService rpcPlaceholderService;
    @Autowired
    private PdfDocumentConfig pdfDocumentConfig;

    public Map<String, Object> generatePlaceholders(SscsCaseData caseData, LocalDateTime caseCreatedDate) {
        Map<String, Object> placeholders = new ConcurrentHashMap<>();

        Appeal appeal = caseData.getAppeal();
        placeholders.put(BENEFIT_TYPE_LITERAL, appeal.getBenefitType().getDescription().toUpperCase());
        placeholders.put(APPELLANT_FULL_NAME_LITERAL, appeal.getAppellant().getName().getAbbreviatedFullName());
        placeholders.put(CASE_ID_LITERAL, caseData.getCcdCaseId());
        placeholders.put(NINO_LITERAL, defaultToEmptyStringIfNull(appeal.getAppellant().getIdentity().getNino()));
        placeholders.put(SSCS_URL_LITERAL, SSCS_URL);
        placeholders.put(GENERATED_DATE_LITERAL, generateNowDate());
        placeholders.put(pdfDocumentConfig.getHmctsImgKey(), pdfDocumentConfig.getHmctsImgVal());

        placeholders.put(CASE_CREATED_DATE_LITERAL, caseCreatedDate.toLocalDate().toString());
        rpcPlaceholderService.setRegionalProcessingOfficeAddress(placeholders, caseData);
        dwpAddressPlaceholderService.verifyAndSetDwpAddress(placeholders, caseData);
        return placeholders;
    }

    private String generateNowDate() {
        return LocalDateTime.now().toLocalDate().toString();
    }

}
