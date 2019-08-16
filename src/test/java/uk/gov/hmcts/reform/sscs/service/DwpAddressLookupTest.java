package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.*;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderHelper.buildCaseData;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.DwpAddress;
import uk.gov.hmcts.reform.sscs.exception.DwpAddressLookupException;
import uk.gov.hmcts.reform.sscs.exception.NoMrnDetailsException;

@RunWith(JUnitParamsRunner.class)
public class DwpAddressLookupTest {

    private static final String PIP = "PIP";
    private static final String ESA = "ESA";

    private SscsCaseData caseData;

    private final DwpAddressLookup dwpAddressLookup = new DwpAddressLookup();

    @Before
    public void setup() {
        caseData = buildCaseData();
    }

    @Test
    public void dwpLookupAddressShouldBeHandled() {
        Address address = dwpAddressLookup.lookupDwpAddress(caseData);

        assertEquals("Mail Handling Site A", address.getLine1());
        assertEquals("WOLVERHAMPTON", address.getTown());
        assertEquals("WV98 1AA", address.getPostcode());
    }

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
    public void dwpOfficeStripsText() {
        DwpAddress address = dwpAddressLookup.lookup("PIP", "DWP Issuing Office(10)");
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

    @Test(expected = NoMrnDetailsException.class)
    public void asAppealWithNoMrnDetailsWillNotHaveADwpAddress() {
        caseData = buildCaseData();
        caseData.setRegionalProcessingCenter(null);

        caseData = caseData.toBuilder().appeal(caseData.getAppeal().toBuilder().mrnDetails(null).build()).build();
        dwpAddressLookup.lookupDwpAddress(caseData);
    }

    @Test(expected = NoMrnDetailsException.class)
    public void anAppealWithNoDwpIssuingOfficeWillNotHaveADwpAddress() {
        caseData = buildCaseData();
        caseData.setRegionalProcessingCenter(null);

        caseData = caseData.toBuilder().appeal(caseData.getAppeal().toBuilder().mrnDetails(
            MrnDetails.builder().mrnLateReason("soz").build()).build()).build();
        dwpAddressLookup.lookupDwpAddress(caseData);
    }
}
