package uk.gov.hmcts.reform.sscs.service.placeholders;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.APPELLANT_FULL_NAME_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.BENEFIT_TYPE_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.CASE_CREATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.CASE_ID_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.EXELA_ADDRESS_LINE1_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.EXELA_ADDRESS_LINE2_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.EXELA_ADDRESS_LINE3_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.EXELA_ADDRESS_POSTCODE_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.GENERATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.NINO_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.RECIPIENT_ADDRESS_LINE_1_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.RECIPIENT_ADDRESS_LINE_2_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.RECIPIENT_ADDRESS_LINE_3_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.RECIPIENT_ADDRESS_LINE_4_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.RECIPIENT_ADDRESS_LINE_5_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_COUNTY_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_FAX_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_PHONE_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_POSTCODE_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.SC_NUMBER_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.SSCS_URL_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.WELSH_CASE_CREATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.WELSH_GENERATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderUtility.defaultToEmptyStringIfNull;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderUtility.truncateAddressLine;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.ExelaAddressConfig;
import uk.gov.hmcts.reform.sscs.docmosis.config.PdfDocumentConfig;
import uk.gov.hmcts.reform.sscs.service.conversion.LocalDateToWelshStringConverter;

@Service
public class PlaceholderService {

    private final PdfDocumentConfig pdfDocumentConfig;
    private final ExelaAddressConfig exelaAddressConfig;

    @Autowired
    public PlaceholderService(PdfDocumentConfig pdfDocumentConfig, ExelaAddressConfig exelaAddressConfig) {
        this.pdfDocumentConfig = pdfDocumentConfig;
        this.exelaAddressConfig = exelaAddressConfig;
    }

    public void build(SscsCaseData caseData, Map<String, Object> placeholders, Address address, String caseCreatedDate) {
        Appeal appeal = caseData.getAppeal();
        String description = appeal.getBenefitType() != null ? appeal.getBenefitType().getDescription() : null;

        if (description == null && appeal.getBenefitType() != null && appeal.getBenefitType().getCode() != null) {
            description = Benefit.getBenefitByCode(appeal.getBenefitType().getCode()).getDescription();
        }
        if (description != null) {
            description = description.toUpperCase();
        } else {
            description = StringUtils.EMPTY;
        }

        placeholders.put(BENEFIT_TYPE_LITERAL, description);
        placeholders.put(APPELLANT_FULL_NAME_LITERAL, appeal.getAppellant().getName().getAbbreviatedFullName());
        placeholders.put(CASE_ID_LITERAL, caseData.getCcdCaseId());
        placeholders.put(NINO_LITERAL, defaultToEmptyStringIfNull(appeal.getAppellant().getIdentity().getNino()));
        placeholders.put(SSCS_URL_LITERAL, PlaceholderConstants.SSCS_URL);
        placeholders.put(GENERATED_DATE_LITERAL, LocalDateTime.now().toLocalDate().toString());
        placeholders.put(pdfDocumentConfig.getHmctsImgKey(), pdfDocumentConfig.getHmctsImgVal());
        if (caseData.isLanguagePreferenceWelsh()) {
            if (caseCreatedDate != null) {
                placeholders.put(WELSH_CASE_CREATED_DATE_LITERAL,
                    LocalDateToWelshStringConverter.convert(caseCreatedDate));
            }
            placeholders.put(WELSH_GENERATED_DATE_LITERAL,
                LocalDateToWelshStringConverter.convert(LocalDateTime.now().toLocalDate()));
            placeholders.put(pdfDocumentConfig.getHmctsWelshImgKey(), pdfDocumentConfig.getHmctsWelshImgVal());
        }
        placeholders.put(SC_NUMBER_LITERAL, defaultToEmptyStringIfNull(caseData.getCaseReference()));

        if (caseCreatedDate != null) {
            placeholders.put(CASE_CREATED_DATE_LITERAL, caseCreatedDate);
        }

        placeholders.put(EXELA_ADDRESS_LINE1_LITERAL, exelaAddressConfig.getAddressLine1());
        placeholders.put(EXELA_ADDRESS_LINE2_LITERAL, exelaAddressConfig.getAddressLine2());
        placeholders.put(EXELA_ADDRESS_LINE3_LITERAL, exelaAddressConfig.getAddressLine3());
        placeholders.put(EXELA_ADDRESS_POSTCODE_LITERAL, exelaAddressConfig.getAddressPostcode());

        populateRpcPlaceHolders(caseData, placeholders);
        buildRecipientAddressPlaceholders(address, placeholders);
    }

    private void populateRpcPlaceHolders(SscsCaseData caseData, Map<String, Object> placeholders) {
        if (hasRegionalProcessingCenter(caseData)) {
            RegionalProcessingCenter rpc = caseData.getRegionalProcessingCenter();
            placeholders.put(REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL, defaultToEmptyStringIfNull(rpc.getAddress1()));
            placeholders.put(REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL, defaultToEmptyStringIfNull(rpc.getAddress2()));
            placeholders.put(REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL, defaultToEmptyStringIfNull(rpc.getAddress3()));
            placeholders.put(REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL, defaultToEmptyStringIfNull(rpc.getAddress4()));
            placeholders.put(REGIONAL_OFFICE_COUNTY_LITERAL, defaultToEmptyStringIfNull(rpc.getCity()));
            placeholders.put(REGIONAL_OFFICE_PHONE_LITERAL, defaultToEmptyStringIfNull(rpc.getPhoneNumber()));
            placeholders.put(REGIONAL_OFFICE_FAX_LITERAL, defaultToEmptyStringIfNull(rpc.getFaxNumber()));
            placeholders.put(REGIONAL_OFFICE_POSTCODE_LITERAL, defaultToEmptyStringIfNull(rpc.getPostcode()));
        }
    }

    private boolean hasRegionalProcessingCenter(SscsCaseData ccdResponse) {
        return nonNull(ccdResponse.getRegionalProcessingCenter())
            && nonNull(ccdResponse.getRegionalProcessingCenter().getName());
    }

    private void buildRecipientAddressPlaceholders(Address address, Map<String, Object> placeholders) {
        String[] lines = lines(address);

        if (lines.length >= 1) {
            placeholders.put(RECIPIENT_ADDRESS_LINE_1_LITERAL, truncateAddressLine(defaultToEmptyStringIfNull(lines[0])));
        }
        if (lines.length >= 2) {
            placeholders.put(RECIPIENT_ADDRESS_LINE_2_LITERAL, truncateAddressLine(defaultToEmptyStringIfNull(lines[1])));
        }
        if (lines.length >= 3) {
            placeholders.put(RECIPIENT_ADDRESS_LINE_3_LITERAL, truncateAddressLine(defaultToEmptyStringIfNull(lines[2])));
        }
        if (lines.length >= 4) {
            placeholders.put(RECIPIENT_ADDRESS_LINE_4_LITERAL, truncateAddressLine(defaultToEmptyStringIfNull(lines[3])));
        }
        if (lines.length >= 5) {
            placeholders.put(RECIPIENT_ADDRESS_LINE_5_LITERAL, truncateAddressLine(defaultToEmptyStringIfNull(lines[4])));
        }
    }

    public static String[] lines(Address address) {
        return Stream.of(address.getLine1(), address.getLine2(), address.getTown(), address.getCounty(), address.getPostcode())
            .filter(x -> x != null)
            .toArray(String[]::new);
    }
}
