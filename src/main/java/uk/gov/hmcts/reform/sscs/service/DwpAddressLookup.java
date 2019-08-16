package uk.gov.hmcts.reform.sscs.service;

import java.nio.charset.Charset;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.BenefitLookup;
import uk.gov.hmcts.reform.sscs.domain.DwpAddress;
import uk.gov.hmcts.reform.sscs.exception.DwpAddressLookupException;
import uk.gov.hmcts.reform.sscs.exception.NoMrnDetailsException;

@Service
@Slf4j
public class DwpAddressLookup {

    private static final String PIP = "PIP";
    private static final String ESA = "ESA";
    private static final String ADDRESS = "address";
    private static final String TEST_HMCTS_ADDRESS = "test-hmcts-address";

    private static JSONObject configObject;

    static {
        JSONParser parser = new JSONParser();
        try {
            configObject = (JSONObject) parser.parse(IOUtils.resourceToString("dwpAddresses.json",
                Charset.forName("UTF-8"), Thread.currentThread().getContextClassLoader()));

        } catch (Exception exception) {
            log.error("Cannot parse dwp addresses. " + exception.getMessage(), exception);
            throw new RuntimeException("cannot parse dwp addresses", exception);
        }
    }

    private static final BenefitLookup PIP_LOOKUP = new BenefitLookup(getJsonArray(PIP));
    private static final BenefitLookup ESA_LOOKUP = new BenefitLookup(getJsonArray(ESA));
    private static final JSONObject TEST_ADDRESS_CONFIG = (JSONObject) configObject.get(TEST_HMCTS_ADDRESS);
    private static final DwpAddress TEST_ADDRESS = BenefitLookup.getAddress((JSONObject) TEST_ADDRESS_CONFIG.get(ADDRESS));


    public Address lookupDwpAddress(SscsCaseData caseData) {
        if (Objects.nonNull(caseData.getAppeal()) && Objects.nonNull(caseData.getAppeal().getMrnDetails())
            && Objects.nonNull(caseData.getAppeal().getMrnDetails().getDwpIssuingOffice())) {

            final DwpAddress dwpAddress = lookup(caseData.getAppeal().getBenefitType().getCode(),
                caseData.getAppeal().getMrnDetails().getDwpIssuingOffice());

            return buildAddress(dwpAddress);
        } else {
            throw new NoMrnDetailsException(caseData);
        }
    }

    DwpAddress lookup(String benefitType, String dwpIssuingOffice) {
        log.info("looking up address for benefitType {} and dwpIssuingOffice {}", benefitType, dwpIssuingOffice);
        Optional<DwpAddress> dwpAddressOptional =
            getDwpAddress(StringUtils.stripToNull(benefitType), StringUtils.stripToNull(dwpIssuingOffice));
        if (!dwpAddressOptional.isPresent()) {
            throw new DwpAddressLookupException(String.format("could not find dwp address for benefitType %s and dwpIssuingOffice %s",
                benefitType, dwpIssuingOffice));

        }
        return dwpAddressOptional.get();
    }

    private Address buildAddress(DwpAddress dwpAddress) {
        return Address.builder()
            .line1(dwpAddress.getLine1().orElse(null))
            .town(dwpAddress.getLine2().orElse(null))
            .county(dwpAddress.getLine3().orElse(null))
            .postcode(dwpAddress.getPostCode().orElse(null))
            .build();
    }

    private Optional<DwpAddress> getDwpAddress(String benefitType, String dwpIssuingOffice) {
        if (StringUtils.equalsIgnoreCase(dwpIssuingOffice, TEST_HMCTS_ADDRESS)) {
            return Optional.of(TEST_ADDRESS);
        }
        if (StringUtils.equalsIgnoreCase(PIP, benefitType)) {
            String dwpIssuingOfficeStripped = dwpIssuingOffice.replaceAll("\\D+","");
            return Optional.ofNullable(PIP_LOOKUP.get(StringUtils.stripToEmpty(dwpIssuingOfficeStripped)));
        } else if (StringUtils.equalsIgnoreCase(ESA, benefitType)) {
            return Optional.ofNullable(ESA_LOOKUP.get(StringUtils.stripToEmpty(dwpIssuingOffice)));
        }
        return Optional.empty();
    }

    private static List<JSONObject> getJsonArray(String benefitType) {
        @SuppressWarnings("unchecked")
        ArrayList<JSONObject>  jsonArray = (ArrayList<JSONObject>) configObject.get(StringUtils.lowerCase(benefitType));
        return jsonArray;
    }

}
