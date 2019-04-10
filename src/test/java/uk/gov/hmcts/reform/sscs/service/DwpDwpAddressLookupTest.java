package uk.gov.hmcts.reform.sscs.service;

import java.util.Optional;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.domain.DwpAddress;

import static org.junit.Assert.*;

@RunWith(JUnitParamsRunner.class)
public class DwpDwpAddressLookupTest {

    private static final String PIP = "PIP";
    private static final String ESA = "ESA";

    private final DwpAddressLookup dwpAddressLookup = new DwpAddressLookup(false);

    @Test
    @Parameters({"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"})
    public void pipAddressesExist(final String dwpIssuingOffice) {
        Optional<DwpAddress> optionalAddress = dwpAddressLookup.lookup(PIP, dwpIssuingOffice);
        assertTrue(optionalAddress.isPresent());
    }

    @Test
    @Parameters({
        "PIP, 1", "pip, 1", "PiP, 1", "pIP, 1",
        "ESA, Balham DRT", "EsA, Balham DRT", "esa, Balham DRT"
    })
    public void benefitTypeIsCaseInsensitive(final String benefitType, String dwpIssuingOffice) {
        Optional<DwpAddress> optionalAddress = dwpAddressLookup.lookup(benefitType, dwpIssuingOffice);
        assertTrue(optionalAddress.isPresent());
    }

    @Test
    @Parameters({
        "Balham DRT", "Birkenhead LM DRT", "Lowestoft DRT", "Wellingborough DRT", "Chesterfield DRT",
        "Coatbridge Benefit Centre", "Inverness DRT", "Milton Keynes DRT", "Springburn DRT", "Watford DRT",
        "Norwich DRT", "Sheffield DRT"
    })
    public void esaAddressesExist(final String dwpIssuingOffice) {
        Optional<DwpAddress> optionalAddress = dwpAddressLookup.lookup(ESA, dwpIssuingOffice);
        assertTrue(optionalAddress.isPresent());
    }

    @Test
    @Parameters({"JOB", "UNK", "PLOP", "BIG", "FIG"})
    public void unknownBenefitTypeReturnsNone(final String benefitType) {
        Optional<DwpAddress> optionalAddress = dwpAddressLookup.lookup(benefitType, "1");
        assertFalse(optionalAddress.isPresent());
    }

    @Test
    @Parameters({"11", "12", "13", "14", "JOB"})
    public void unknownPipDwpIssuingOfficeReturnsNone(final String dwpIssuingOffice) {
        Optional<DwpAddress> optionalAddress = dwpAddressLookup.lookup(PIP, dwpIssuingOffice);
        assertFalse(optionalAddress.isPresent());
    }

    @Test
    @Parameters({"JOB", "UNK", "PLOP", "BIG", "11"})
    public void unknownEsaDwpIssuingOfficeReturnsNone(final String dwpIssuingOffice) {
        Optional<DwpAddress> optionalAddress = dwpAddressLookup.lookup(ESA, dwpIssuingOffice);
        assertFalse(optionalAddress.isPresent());
    }

    @Test
    public void excela_addressIsConfigured() {
        String[] lines = DwpAddressLookup.EXCELA_DWP_ADDRESS.lines();
        assertArrayEquals(new String[]{"PO BOX XXX", "Exela BSP Services", "Harlow", "CM19 5QS"}, lines);
    }


    @Test
    public void pip_1_isConfiguredCorrectly() {
        Optional<DwpAddress> optionalAddress = dwpAddressLookup.lookup(PIP, "1");
        assertTrue(optionalAddress.isPresent());
        DwpAddress address = optionalAddress.get();
        assertArrayEquals(new String[]{"Mail Handling Site A", "WOLVERHAMPTON", "WV98 1AA"}, address.lines());
    }

    @Test
    @Parameters({
        "PIP, 1", "pip, 1", "PiP, 1", "pIP, 1",
        "ESA, Balham DRT", "EsA, Balham DRT", "esa, Balham DRT",
        "JOB, Job", "UNK, unk"
    })
    public void willAlwaysReturnTestAddressIfConfigured(final String benefitType, String dwpIssuingOffice) {
        DwpAddressLookup dwpAddressLookup = new DwpAddressLookup(true);
        Optional<DwpAddress> optionalAddress = dwpAddressLookup.lookup(benefitType, "1");
        assertTrue(optionalAddress.isPresent());
        DwpAddress address = optionalAddress.get();
        assertEquals("E1 8FA", address.lines()[3]);
    }
}
