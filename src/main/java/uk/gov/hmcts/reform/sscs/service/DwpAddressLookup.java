package uk.gov.hmcts.reform.sscs.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.domain.BenefitLookup;
import uk.gov.hmcts.reform.sscs.domain.DwpAddress;

@Service
@Slf4j
public class DwpAddressLookup {

    private static final String PIP = "PIP";
    private static final String ESA = "ESA";
    private static final String EXCELA = "excela";
    private static final String ADDRESS = "address";

    private static JSONObject configObject;

    static {
        JSONParser parser = new JSONParser();
        try {

            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            File file = resolver.getResource("classpath:dwpAddresses.json").getFile();

            configObject = (JSONObject) parser.parse(FileUtils.readFileToString(file, "UTF-8"));

        } catch (Exception exception) {
            log.error("Cannot parse dwp addresses. " + exception.getMessage(), exception);
            throw new RuntimeException("cannot parse dwp addresses", exception);
        }
    }

    private static final BenefitLookup<Long> PIP_LOOKUP = new BenefitLookup<>(getJsonArray(PIP));
    private static final BenefitLookup<String> ESA_LOOKUP = new BenefitLookup<>(getJsonArray(ESA));
    private static final JSONObject EXCELA_CONFIG = (JSONObject) configObject.get(EXCELA);
    public static final DwpAddress EXCELA_DWP_ADDRESS = BenefitLookup.getAddress((JSONObject) EXCELA_CONFIG.get(ADDRESS));

    public Optional<DwpAddress> lookup(String benefitType, String dwpIssuingOffice) {
        log.info("looking up address for benefitType {} and dwpIssuingOffice {}", benefitType, dwpIssuingOffice);
        Optional<DwpAddress> dwpAddressOptional =
            getDwpAddress(StringUtils.stripToNull(benefitType), StringUtils.stripToNull(dwpIssuingOffice));
        if (!dwpAddressOptional.isPresent()) {
            log.warn("could not find dwp address for benefitType {} and dwpIssuingOffice {}",
                benefitType, dwpIssuingOffice);
        }
        return dwpAddressOptional;
    }

    private Optional<DwpAddress> getDwpAddress(String benefitType, String dwpIssuingOffice) {
        if (StringUtils.equalsIgnoreCase(PIP, benefitType) && NumberUtils.isCreatable(dwpIssuingOffice)) {
            return Optional.ofNullable(PIP_LOOKUP.get(NumberUtils.toLong(dwpIssuingOffice, 0)));
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
