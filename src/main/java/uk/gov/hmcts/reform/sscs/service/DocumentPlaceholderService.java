package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.config.PdfDocumentConfig;
import uk.gov.hmcts.reform.sscs.domain.DwpAddress;
import uk.gov.hmcts.reform.sscs.exception.DwpAddressLookupException;

@Service
@Slf4j
public class DocumentPlaceholderService {

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

        setRegionalProcessingOfficeAddress(placeholders, caseData);
        setDwpAddress(placeholders, caseData);

        return placeholders;
    }

    private String generateNowDate() {
        return LocalDateTime.now().toLocalDate().toString();
    }

    private void setRegionalProcessingOfficeAddress(Map<String, Object> placeholders, SscsCaseData caseData) {
        RegionalProcessingCenter rpc = null;

        if (hasRegionalProcessingCenter(caseData)) {
            rpc = caseData.getRegionalProcessingCenter();
        }
        //FIXME: somehow add to some exception queue when null - this should be covered by another ticket
        if (rpc != null) {
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

    private void setDwpAddress(Map<String, Object> placeholders, SscsCaseData caseData) {
        if (nonNull(caseData.getAppeal())
            && nonNull(caseData.getAppeal().getMrnDetails())
            && nonNull(caseData.getAppeal().getMrnDetails().getDwpIssuingOffice())) {

            final DwpAddress dwpAddress = dwpAddressLookup.lookup(
                caseData.getAppeal().getBenefitType().getCode(),
                caseData.getAppeal().getMrnDetails().getDwpIssuingOffice());

            setDwpAddress(placeholders, dwpAddress);
        } else {
            throw new DwpAddressLookupException("There is no Appeal Mrn details.");
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

    private static boolean hasRegionalProcessingCenter(SscsCaseData ccdResponse) {
        return nonNull(ccdResponse.getRegionalProcessingCenter())
            && nonNull(ccdResponse.getRegionalProcessingCenter().getName());
    }

    private Object defaultToEmptyStringIfNull(Object value) {
        return (value == null) ? StringUtils.EMPTY : value;
    }

}
