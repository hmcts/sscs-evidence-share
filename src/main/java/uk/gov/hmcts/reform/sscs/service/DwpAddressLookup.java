package uk.gov.hmcts.reform.sscs.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.domain.DwpAddress;

@Service
@Slf4j
public class DwpAddressLookup {

    private static final String PIP = "PIP";
    private static final String ESA = "ESA";

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

    @SuppressWarnings({"PMD.UnusedPrivateField"})
    private static final DwpAddress EXCELA_DWP_ADDRESS =
        new DwpAddress("PO BOX XXX", "Exela BSP Services", "Harlow", "CM19 5QS");

    private static final Map<Integer, DwpAddress> PIP_LOOKUP = new ConcurrentHashMap<Integer, DwpAddress>() {
        {
            this.put(1, new DwpAddress("Mail Handling Site A", "WOLVERHAMPTON", "WV98 1AA"));
            this.put(2, new DwpAddress("Mail Handling Site A", "WOLVERHAMPTON", "WV98 1AB"));
            this.put(3, new DwpAddress("Mail Handling Site A", "WOLVERHAMPTON", "WV98 1AD"));
            this.put(4, new DwpAddress("Post Handling Site B", "WOLVERHAMPTON", "WV99 1AA"));
            this.put(5, new DwpAddress("Post Handling Site B", "WOLVERHAMPTON", "WV99 1AB"));
            this.put(6, new DwpAddress("Post Handling Site B", "WOLVERHAMPTON", "WV99 1AD"));
            this.put(7, new DwpAddress("Post Handling Site B", "WOLVERHAMPTON", "WV99 1AE"));
            this.put(8, new DwpAddress("Post Handling Site B", "WOLVERHAMPTON", "WV99 1AF"));
            this.put(9, new DwpAddress("Post Handling Site B", "WOLVERHAMPTON", "WV99 1AG"));
            this.put(10, new DwpAddress("Warbreck House", "Blackpool", "FY2 0UZ"));
        }
    };

    private static final Map<String, DwpAddress> ESA_LOOKUP = new ConcurrentHashMap<String, DwpAddress>() {
        {
            this.put("Balham DRT", new DwpAddress("Mail Handling Site A", "WOLVERHAMPTON", "WV98 1HJ"));
            this.put("Birkenhead LM DRT", new DwpAddress("Birkenhead Benefit Centre", "Post Handling Site B","WOLVERHAMPTON", "WV99 1FZ"));
            this.put("Lowestoft DRT", new DwpAddress("Post Handling Site B", "WOLVERHAMPTON", "WV99 1NH"));
            this.put("Wellingborough DRT", new DwpAddress("Post Handling Site B", "WOLVERHAMPTON", "WV99 1DZ"));
            this.put("Chesterfield DRT", new DwpAddress("Chesterfield Benefit Centre", "Mail Handling Site A","WOLVERHAMPTON", "WV98 1GG"));
            this.put("Coatbridge Benefit Centre", new DwpAddress("Coatbridge Benefit Centre", "Mail Handling Site A", "WOLVERHAMPTON", "WV98 1BX"));
            this.put("Inverness DRT", new DwpAddress("Inverness Benefit Centre",  "Mail Handling Site A", "Wolverhampton", "WV98 1PL"));
            this.put("Milton Keynes DRT", new DwpAddress("Milton Keynes Benefit Centre", "Post Handling Site B", "WOLVERHAMPTON", "WV99 1RA"));
            this.put("Springburn DRT", new DwpAddress("", "", ""));
            this.put("Watford DRT", new DwpAddress("Watford Service Centre", "Post Handling Site B", "WOLVERHAMPTON", "WV99 1RH"));
            this.put("Norwich DRT", new DwpAddress("Norwich Benefit Centre", "Post Handling Site B","WOLVERHAMPTON", "WV99 1NN"));
            this.put("Sheffield DRT", new DwpAddress("Sheffield Benefit Centre", "Mail Handling Site A", "WOLVERHAMPTON", "WV98 1FZ"));

        }
    };

}
