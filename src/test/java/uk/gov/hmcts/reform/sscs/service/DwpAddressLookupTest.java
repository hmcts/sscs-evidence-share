package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.*;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.domain.DwpAddress;
import uk.gov.hmcts.reform.sscs.exception.DwpAddressLookupException;

@RunWith(JUnitParamsRunner.class)
public class DwpAddressLookupTest {

    private static final String PIP = "PIP";
    private static final String ESA = "ESA";

    private final DwpAddressLookup dwpAddressLookup = new DwpAddressLookup();

    @Test
    @Parameters({"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"})
    public void pipAddressesExist(final String dwpIssuingOffice) {
        DwpAddress address = dwpAddressLookup.lookup(PIP, dwpIssuingOffice);
        assertNotNull(address);
    }

    @Test
    @Parameters({
        "PIP, 1", "pip, 1", "PiP, 1", "pIP, 1",
        "ESA, Balham DRT", "EsA, Balham DRT", "esa, Balham DRT"
    })
    public void benefitTypeIsCaseInsensitive(final String benefitType, String dwpIssuingOffice) {
        DwpAddress address = dwpAddressLookup.lookup(benefitType, dwpIssuingOffice);
        assertNotNull(address);
    }

    @Test
    public void handleCaseInsensitiveAddresses() {
        DwpAddress address = dwpAddressLookup.lookup("ESA", "BALHAM DRT");
        assertNotNull(address);
    }

    @Test
    @Parameters({
        "Balham DRT", "Birkenhead LM DRT", "Lowestoft DRT", "Wellingborough DRT", "Chesterfield DRT",
        "Coatbridge Benefit Centre", "Inverness DRT", "Milton Keynes DRT", "Springburn DRT", "Watford DRT",
        "Norwich DRT", "Sheffield DRT", "Worthing DRT"
    })
    public void esaAddressesExist(final String dwpIssuingOffice) {
        DwpAddress address = dwpAddressLookup.lookup(ESA, dwpIssuingOffice);
        assertNotNull(address);
    }

    @Test(expected = DwpAddressLookupException.class)
    @Parameters({"JOB", "UNK", "PLOP", "BIG", "FIG"})
    public void unknownBenefitTypeReturnsNone(final String benefitType) {
        dwpAddressLookup.lookup(benefitType, "1");
    }

    @Test(expected = DwpAddressLookupException.class)
    @Parameters({"11", "12", "13", "14", "JOB"})
    public void unknownPipDwpIssuingOfficeReturnsNone(final String dwpIssuingOffice) {
        dwpAddressLookup.lookup(PIP, dwpIssuingOffice);
    }

    @Test(expected = DwpAddressLookupException.class)
    @Parameters({"JOB", "UNK", "PLOP", "BIG", "11"})
    public void unknownEsaDwpIssuingOfficeReturnsNone(final String dwpIssuingOffice) {
        dwpAddressLookup.lookup(ESA, dwpIssuingOffice);
    }

    @Test
    public void excela_addressIsConfigured() {
        String[] lines = DwpAddressLookup.EXCELA_DWP_ADDRESS.lines();
        assertArrayEquals(new String[]{"PO BOX XXX", "Exela BSP Services", "Harlow", "CM19 5QS"}, lines);
    }


    @Test
    public void pip_1_isConfiguredCorrectly() {
        DwpAddress address = dwpAddressLookup.lookup(PIP, "1");
        assertNotNull(address);
        assertArrayEquals(new String[]{"Mail Handling Site A", "WOLVERHAMPTON", "WV98 1AA"}, address.lines());
    }

    @Test
    @Parameters({"PIP", "ESA", "JOB", "UNK", "PLOP", "BIG", "11"})
    public void willAlwaysReturnTestAddressForATestDwpIssuingOffice(final String benefitType) {
        DwpAddress address = dwpAddressLookup.lookup(benefitType, "test-hmcts-address");
        assertNotNull(address);
        assertEquals("E1 8FA", address.lines()[3]);
    }
}
