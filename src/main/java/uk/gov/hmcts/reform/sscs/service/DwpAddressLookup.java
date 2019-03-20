package uk.gov.hmcts.reform.sscs.service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.domain.DwpAddress;

@Service
@Slf4j
public class DwpAddressLookup {

    private static final String PIP = "PIP";
    private static final String ESA = "ESA";
    private static final String EXCELA = "excela";
    private static final String ADDRESS = "address";
    private static final String LINE_1 = "line1";
    private static final String LINE_2 = "line2";
    private static final String LINE_3 = "line3";
    private static final String POST_CODE = "postCode";
    private static final String CODE = "code";

    private static JSONObject configObject;

    static {
        JSONParser parser = new JSONParser();
        try {
            configObject = (JSONObject) parser.parse(
                Files.newBufferedReader(Paths.get(
                    Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("dwpAddresses.json")).toURI())
                )
            );
        } catch (Exception exception) {
            log.error("Cannot parse dwp addresses.", exception);
            throw new RuntimeException("cannot parse dwp addresses", exception);
        }
    }

    private static final JSONObject EXCELA_CONFIG = (JSONObject) configObject.get(EXCELA);

    public static final DwpAddress EXCELA_DWP_ADDRESS =  new DwpAddress(
        (String) ((JSONObject)EXCELA_CONFIG.get(ADDRESS)).get(LINE_1),
        (String) ((JSONObject)EXCELA_CONFIG.get(ADDRESS)).get(LINE_2),
        (String) ((JSONObject)EXCELA_CONFIG.get(ADDRESS)).get(LINE_3),
        (String) ((JSONObject)EXCELA_CONFIG.get(ADDRESS)).get(POST_CODE));

    public Optional<DwpAddress> lookup(String benefitType, String dwpIssuingOffice) {
        Optional<DwpAddress> toReturn = Optional.empty();
        log.info("looking up address for benefitType {} and dwpIssuingOffice {}", benefitType, dwpIssuingOffice);
        if (StringUtils.equalsIgnoreCase(PIP, benefitType) && NumberUtils.isCreatable(dwpIssuingOffice)) {
            toReturn = Optional.ofNullable(PIP_LOOKUP.get(NumberUtils.toInt(dwpIssuingOffice, 0)));
        } else if (StringUtils.equalsIgnoreCase(ESA, benefitType)) {
            toReturn = Optional.ofNullable(ESA_LOOKUP.get(StringUtils.stripToEmpty(dwpIssuingOffice)));
        }
        if (!toReturn.isPresent()) {
            log.warn("could not find dwp address for benefitType {} and dwpIssuingOffice {}",
                benefitType, dwpIssuingOffice);
        }
        return toReturn;
    }

    private static final Map<Integer, DwpAddress> PIP_LOOKUP = new ConcurrentHashMap<Integer, DwpAddress>() {
        {
            @SuppressWarnings("unchecked")
            ArrayList<JSONObject>  jsonArray = (ArrayList<JSONObject>) configObject.get(StringUtils.lowerCase(PIP));
            jsonArray.forEach(jsonObj -> {
                Integer code = ((Long) jsonObj.get(CODE)).intValue();
                String line1 = (String) jsonObj.get(LINE_1);
                String line2 = (String) jsonObj.get(LINE_2);
                String line3 = (String) jsonObj.get(LINE_3);
                String postCode = (String) jsonObj.get(POST_CODE);
                this.put(code, new DwpAddress(line1, line2, line3, postCode));
            });
        }
    };

    private static final Map<String, DwpAddress> ESA_LOOKUP = new ConcurrentHashMap<String, DwpAddress>() {
        {
            @SuppressWarnings("unchecked")
            ArrayList<JSONObject>  jsonArray = (ArrayList<JSONObject>) configObject.get(StringUtils.lowerCase(ESA));
            jsonArray.forEach(jsonObj -> {
                String code = (String) jsonObj.get(CODE);
                String line1 = (String) jsonObj.get(LINE_1);
                String line2 = (String) jsonObj.get(LINE_2);
                String line3 = (String) jsonObj.get(LINE_3);
                String postCode = (String) jsonObj.get(POST_CODE);
                this.put(code, new DwpAddress(line1, line2, line3, postCode));
            });
        }
    };

}
