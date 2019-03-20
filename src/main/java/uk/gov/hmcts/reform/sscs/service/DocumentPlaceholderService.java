package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.DwpAddress;

@Service
@Slf4j
public class DocumentPlaceholderService {

    private final DwpAddressLookup dwpAddressLookup;

    @Autowired
    public DocumentPlaceholderService(DwpAddressLookup dwpAddressLookup) {
        this.dwpAddressLookup = dwpAddressLookup;
    }

    public Map<String, Object> generatePlaceholders(SscsCaseData caseData) {
        Map<String, Object> placeholders = new HashMap<>();

        Appeal appeal = caseData.getAppeal();
        placeholders.put(CASE_CREATED_DATE_LITERAL, formatDate(caseData.getCaseCreated()));
        placeholders.put(BENEFIT_TYPE_LITERAL, appeal.getBenefitType().getDescription().toUpperCase());
        placeholders.put(APPELLANT_FULL_NAME_LITERAL, appeal.getAppellant().getName().getAbbreviatedFullName());
        placeholders.put(CASE_ID_LITERAL, caseData.getCcdCaseId());
        placeholders.put(NINO_LITERAL, appeal.getAppellant().getIdentity().getNino());
        placeholders.put(SSCS_URL_LITERAL, SSCS_URL);
        placeholders.put(GENERATED_DATE_LITERAL, generateNowDate());

        setRegionalProcessingOfficeAddress(placeholders, caseData);
        setDwpAddress(placeholders, caseData);

        return placeholders;
    }

    private String formatDate(String date) {
        LocalDateTime dateTime = LocalDateTime.parse(date);
        return dateTime.toLocalDate().toString();
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
            placeholders.put(REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL, rpc.getAddress1());
            placeholders.put(REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL, rpc.getAddress2());
            placeholders.put(REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL, rpc.getAddress3());
            placeholders.put(REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL, rpc.getAddress4());
            placeholders.put(REGIONAL_OFFICE_COUNTY_LITERAL, rpc.getCity());
            placeholders.put(REGIONAL_OFFICE_PHONE_LITERAL, rpc.getPhoneNumber());
            placeholders.put(REGIONAL_OFFICE_FAX_LITERAL, rpc.getFaxNumber());
            placeholders.put(REGIONAL_OFFICE_POSTCODE_LITERAL, rpc.getPostcode());
        }
    }

    private void setDwpAddress(Map<String, Object> placeholders, SscsCaseData caseData) {
        if (nonNull(caseData.getAppeal())
            && nonNull(caseData.getAppeal().getMrnDetails())
            && nonNull(caseData.getAppeal().getMrnDetails().getDwpIssuingOffice())) {

            final Optional<DwpAddress> optionalAddress = dwpAddressLookup.lookup(
                caseData.getAppeal().getBenefitType().getCode(),
                caseData.getAppeal().getMrnDetails().getDwpIssuingOffice());

            if (optionalAddress.isPresent()) {
                String[] lines = optionalAddress.get().lines();
                if (lines.length >= 1) {
                    placeholders.put(DWP_ADDRESS_LINE1_LITERAL, lines[0]);
                }
                if (lines.length >= 2) {
                    placeholders.put(DWP_ADDRESS_LINE2_LITERAL, lines[1]);
                }
                if (lines.length >= 3) {
                    placeholders.put(DWP_ADDRESS_LINE3_LITERAL, lines[2]);
                }
                if (lines.length >= 4) {
                    placeholders.put(DWP_ADDRESS_LINE4_LITERAL, lines[3]);
                }
            }
        }
    }

    private static boolean hasRegionalProcessingCenter(SscsCaseData ccdResponse) {
        return null != ccdResponse.getRegionalProcessingCenter()
            && null != ccdResponse.getRegionalProcessingCenter().getName();
    }
}
