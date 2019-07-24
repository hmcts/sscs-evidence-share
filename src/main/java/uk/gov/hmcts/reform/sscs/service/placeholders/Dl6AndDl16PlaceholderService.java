package uk.gov.hmcts.reform.sscs.service.placeholders;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.APPELLANT_FULL_NAME_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.BENEFIT_TYPE_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.CASE_CREATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.CASE_ID_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.DWP_ADDRESS_LINE1_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.DWP_ADDRESS_LINE2_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.DWP_ADDRESS_LINE3_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.DWP_ADDRESS_LINE4_LITERAL;
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
import uk.gov.hmcts.reform.sscs.domain.DwpAddress;
import uk.gov.hmcts.reform.sscs.exception.NoMrnDetailsException;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookup;

@Service
@Slf4j
public class Dl6AndDl16PlaceholderService {

    @Autowired
    private RpcPlaceholderService rpcPlaceholderService;

    @Autowired
    private DwpAddressLookup dwpAddressLookup;

    @Autowired
    private PdfDocumentConfig pdfDocumentConfig;

    public Map<String, Object> generatePlaceholders(SscsCaseData caseData, LocalDateTime caseCreatedDate) {
        Map<String, Object> placeholders = new ConcurrentHashMap<>();

        Appeal appeal = caseData.getAppeal();
        placeholders.put(CASE_CREATED_DATE_LITERAL, caseCreatedDate.toLocalDate().toString());
        placeholders.put(BENEFIT_TYPE_LITERAL, appeal.getBenefitType().getDescription().toUpperCase());
        placeholders.put(APPELLANT_FULL_NAME_LITERAL, appeal.getAppellant().getName().getAbbreviatedFullName());
        placeholders.put(CASE_ID_LITERAL, caseData.getCcdCaseId());
        placeholders.put(NINO_LITERAL, defaultToEmptyStringIfNull(appeal.getAppellant().getIdentity().getNino()));
        placeholders.put(SSCS_URL_LITERAL, SSCS_URL);
        placeholders.put(GENERATED_DATE_LITERAL, generateNowDate());
        placeholders.put(pdfDocumentConfig.getHmctsImgKey(), pdfDocumentConfig.getHmctsImgVal());

        rpcPlaceholderService.setRegionalProcessingOfficeAddress(placeholders, caseData);
        verifyAndSetDwpAddress(placeholders, caseData);

        return placeholders;
    }

    private String generateNowDate() {
        return LocalDateTime.now().toLocalDate().toString();
    }


    private void verifyAndSetDwpAddress(Map<String, Object> placeholders, SscsCaseData caseData) {
        if (nonNull(caseData.getAppeal()) && nonNull(caseData.getAppeal().getMrnDetails())
            && nonNull(caseData.getAppeal().getMrnDetails().getDwpIssuingOffice())) {

            final DwpAddress dwpAddress = dwpAddressLookup.lookup(caseData.getAppeal().getBenefitType().getCode(),
                caseData.getAppeal().getMrnDetails().getDwpIssuingOffice());

            setDwpAddress(placeholders, dwpAddress);
        } else {
            throw new NoMrnDetailsException(caseData);
        }
    }

    private void setDwpAddress(Map<String, Object> placeholders, DwpAddress dwpAddress) {
        String[] lines = dwpAddress.lines();
        if (lines.length >= 1) {
            placeholders.put(DWP_ADDRESS_LINE1_LITERAL, defaultToEmptyStringIfNull(lines[0]));
        }
        if (lines.length >= 2) {
            placeholders.put(DWP_ADDRESS_LINE2_LITERAL, defaultToEmptyStringIfNull(lines[1]));
        }
        if (lines.length >= 3) {
            placeholders.put(DWP_ADDRESS_LINE3_LITERAL, defaultToEmptyStringIfNull(lines[2]));
        }
        if (lines.length >= 4) {
            placeholders.put(DWP_ADDRESS_LINE4_LITERAL, defaultToEmptyStringIfNull(lines[3]));
        }
    }

}
