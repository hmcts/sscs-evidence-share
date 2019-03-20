package uk.gov.hmcts.reform.sscs.service;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.*;

import java.util.Map;
import java.util.Optional;

import org.joda.time.DateTimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.domain.DwpAddress;

@RunWith(MockitoJUnitRunner.class)
public class DocumentPlaceholderServiceTest {

    private static final String RPC_ADDRESS1 = "HM Courts & Tribunals Service";
    private static final String RPC_ADDRESS2 = "Social Security & Child Support Appeals";
    private static final String RPC_ADDRESS3 = "Prudential Buildings";
    private static final String RPC_ADDRESS4 = "36 Dale Street";
    private static final String RPC_CITY = "LIVERPOOL";
    private static final String POSTCODE = "L2 5UZ";

    private DocumentPlaceholderService service;
    private SscsCaseData caseData;
    @Mock
    private DwpAddressLookup dwpAddressLookup;

    @Before
    public void setup() {
        service = new DocumentPlaceholderService(dwpAddressLookup);
        DateTimeUtils.setCurrentMillisFixed(1550000000000L);
    }

    @Test
    public void givenACaseData_thenGenerateThePlaceholderMappings() {

        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
            .name("Liverpool").address1(RPC_ADDRESS1).address2(RPC_ADDRESS2).address3(RPC_ADDRESS3)
            .address4(RPC_ADDRESS4).city(RPC_CITY).postcode(POSTCODE).build();

        buildCaseData(rpc);
        DwpAddress dwpAddress = new DwpAddress(RPC_ADDRESS1, RPC_ADDRESS2, RPC_CITY, POSTCODE);
        when(dwpAddressLookup.lookup("PIP", "1")).thenReturn(Optional.of(dwpAddress));

        Map<String, Object> result = service.generatePlaceholders(caseData);

        assertEquals(result.get(CASE_CREATED_DATE_LITERAL), "2018-10-23");
        assertEquals(result.get(REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL), RPC_ADDRESS1);
        assertEquals(result.get(REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL), RPC_ADDRESS2);
        assertEquals(result.get(REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL), RPC_ADDRESS3);
        assertEquals(result.get(REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL), RPC_ADDRESS4);
        assertEquals(result.get(REGIONAL_OFFICE_COUNTY_LITERAL), RPC_CITY);
        assertEquals(result.get(REGIONAL_OFFICE_POSTCODE_LITERAL), POSTCODE);
        assertEquals(dwpAddress.lines()[0], result.get(DWP_ADDRESS_LINE1_LITERAL));
        assertEquals(dwpAddress.lines()[1], result.get(DWP_ADDRESS_LINE2_LITERAL));
        assertEquals(dwpAddress.lines()[2], result.get(DWP_ADDRESS_LINE3_LITERAL));
        assertEquals(dwpAddress.lines()[3], result.get(DWP_ADDRESS_LINE4_LITERAL));
        assertEquals(result.get(GENERATED_DATE_LITERAL), "2019-02-12");
        assertEquals(result.get(APPELLANT_FULL_NAME_LITERAL), "Mr T Tibbs");
        assertEquals(result.get(BENEFIT_TYPE_LITERAL), "PERSONAL INDEPENDENCE PAYMENT");
        assertEquals(result.get(CASE_ID_LITERAL), "123456");
        assertEquals(result.get(NINO_LITERAL), "JT0123456B");
        assertEquals(result.get(SSCS_URL_LITERAL), "http://www.tribunals.gov.uk/");
    }

    @Test
    public void givenACaseDataWithNoRpc_thenGenerateThePlaceholderMappingsWithoutRpc() {
        buildCaseData(null);
        when(dwpAddressLookup.lookup("PIP", "1"))
            .thenReturn(Optional.of(new DwpAddress(RPC_ADDRESS1, RPC_ADDRESS2, RPC_CITY, POSTCODE)));

        Map<String, Object> result = service.generatePlaceholders(caseData);

        assertEquals(result.get(CASE_CREATED_DATE_LITERAL), "2018-10-23");
        assertNull(result.get(REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL));
        assertNull(result.get(REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL));
        assertNull(result.get(REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL));
        assertNull(result.get(REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL));
        assertNull(result.get(REGIONAL_OFFICE_COUNTY_LITERAL));
        assertNull(result.get(REGIONAL_OFFICE_POSTCODE_LITERAL));
    }

    @Test
    public void anAddressWithTwoLinesAndPostCodeWillNotHaveRow4() {
        buildCaseData(null);
        DwpAddress dwpAddress = new DwpAddress(RPC_ADDRESS1, RPC_ADDRESS2, POSTCODE);
        when(dwpAddressLookup.lookup("PIP", "1")).thenReturn(Optional.of(dwpAddress));
        Map<String, Object> result = service.generatePlaceholders(caseData);
        assertEquals(dwpAddress.lines()[0], result.get(DWP_ADDRESS_LINE1_LITERAL));
        assertEquals(dwpAddress.lines()[1], result.get(DWP_ADDRESS_LINE2_LITERAL));
        assertEquals(dwpAddress.lines()[2], result.get(DWP_ADDRESS_LINE3_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE4_LITERAL));
    }

    @Test
    public void anAddressWithOneLineAndPostCodeWillNotHaveRow3AndRow4() {
        buildCaseData(null);
        DwpAddress dwpAddress = new DwpAddress(RPC_ADDRESS1, "", POSTCODE);
        when(dwpAddressLookup.lookup("PIP", "1")).thenReturn(Optional.of(dwpAddress));
        Map<String, Object> result = service.generatePlaceholders(caseData);
        assertEquals(dwpAddress.lines()[0], result.get(DWP_ADDRESS_LINE1_LITERAL));
        assertEquals(dwpAddress.lines()[1], result.get(DWP_ADDRESS_LINE2_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE3_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE4_LITERAL));
    }

    @Test
    public void addressWithOneLineWillNotHaveRow2AndRow3AndRow4() {
        buildCaseData(null);
        DwpAddress dwpAddress = new DwpAddress("", "", POSTCODE);
        when(dwpAddressLookup.lookup("PIP", "1")).thenReturn(Optional.of(dwpAddress));
        Map<String, Object> result = service.generatePlaceholders(caseData);
        assertEquals(dwpAddress.lines()[0], result.get(DWP_ADDRESS_LINE1_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE2_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE3_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE4_LITERAL));
    }

    @Test
    public void addressWithNoLinesWillNotHaveADwpAddress() {
        buildCaseData(null);
        DwpAddress dwpAddress = new DwpAddress("", "", "");
        when(dwpAddressLookup.lookup("PIP", "1")).thenReturn(Optional.of(dwpAddress));
        Map<String, Object> result = service.generatePlaceholders(caseData);
        assertNull(result.get(DWP_ADDRESS_LINE1_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE2_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE3_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE4_LITERAL));
    }

    @Test
    public void asAppealWithNoMrnDetailsWillNotHaveADwpAddress() {
        buildCaseData(null);
        caseData = caseData.toBuilder().appeal(caseData.getAppeal().toBuilder().mrnDetails(null).build()).build();
        Map<String, Object> result = service.generatePlaceholders(caseData);
        verifyZeroInteractions(dwpAddressLookup);
        assertNull(result.get(DWP_ADDRESS_LINE1_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE2_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE3_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE4_LITERAL));
    }

    @Test
    public void anAppealWithNoDwpIssuingOfficeWillNotHaveADwpAddress() {
        buildCaseData(null);
        caseData = caseData.toBuilder().appeal(caseData.getAppeal().toBuilder().mrnDetails(
            MrnDetails.builder().mrnLateReason("soz").build()).build()).build();
        Map<String, Object> result = service.generatePlaceholders(caseData);
        verifyZeroInteractions(dwpAddressLookup);
        assertNull(result.get(DWP_ADDRESS_LINE1_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE2_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE3_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE4_LITERAL));
    }

    private void buildCaseData(RegionalProcessingCenter rpc) {
        caseData = SscsCaseData.builder()
            .caseCreated("2018-10-23T15:04:48.187")
            .ccdCaseId("123456")
            .regionalProcessingCenter(rpc)
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder().dwpIssuingOffice("1").build())
                .benefitType(BenefitType.builder().code("PIP").description("Personal Independence Payment").build())
                .appellant(Appellant.builder()
                    .name(Name.builder().title("Mr").firstName("Terry").lastName("Tibbs").build())
                    .identity(Identity.builder().nino("JT0123456B").build())
                    .build())
                .build())
            .build();
    }
}
