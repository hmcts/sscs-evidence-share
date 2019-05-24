package uk.gov.hmcts.reform.sscs.service;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.*;

import java.time.LocalDateTime;
import java.util.Map;

import org.joda.time.DateTimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docmosis.config.PdfDocumentConfig;
import uk.gov.hmcts.reform.sscs.domain.DwpAddress;
import uk.gov.hmcts.reform.sscs.exception.NoMrnDetailsException;

@RunWith(MockitoJUnitRunner.class)
public class DocumentPlaceholderServiceTest {

    private static final String RPC_ADDRESS1 = "HM Courts & Tribunals Service";
    private static final String RPC_ADDRESS2 = "Social Security & Child Support Appeals";
    private static final String RPC_ADDRESS3 = "Prudential Buildings";
    private static final String RPC_ADDRESS4 = "36 Dale Street";
    private static final String RPC_CITY = "LIVERPOOL";
    private static final String POSTCODE = "L2 5UZ";

    private SscsCaseData caseData;
    private LocalDateTime now;

    @Mock
    private DwpAddressLookup dwpAddressLookup;

    @Mock
    private PdfDocumentConfig pdfDocumentConfig;

    @InjectMocks
    private DocumentPlaceholderService service;

    @Before
    public void setup() {
        DateTimeUtils.setCurrentMillisFixed(1550000000000L);
        when(pdfDocumentConfig.getHmctsImgKey()).thenReturn("HmctsImgKey");
        when(pdfDocumentConfig.getHmctsImgVal()).thenReturn("HmctsImgVal");
        now = LocalDateTime.now();
    }

    @Test
    public void givenACaseData_thenGenerateThePlaceholderMappings() {

        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
            .name("Liverpool").address1(RPC_ADDRESS1).address2(RPC_ADDRESS2).address3(RPC_ADDRESS3)
            .address4(RPC_ADDRESS4).city(RPC_CITY).postcode(POSTCODE).build();

        buildCaseData(rpc);
        DwpAddress dwpAddress = new DwpAddress(RPC_ADDRESS1, RPC_ADDRESS2, RPC_CITY, POSTCODE);
        when(dwpAddressLookup.lookup("PIP", "1")).thenReturn(dwpAddress);

        Map<String, Object> result = service.generatePlaceholders(caseData, now);

        assertEquals(now.toLocalDate().toString(), result.get(CASE_CREATED_DATE_LITERAL));
        assertEquals(RPC_ADDRESS1, result.get(REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL));
        assertEquals(RPC_ADDRESS2, result.get(REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL));
        assertEquals(RPC_ADDRESS3, result.get(REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL));
        assertEquals(RPC_ADDRESS4, result.get(REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL));
        assertEquals(RPC_CITY, result.get(REGIONAL_OFFICE_COUNTY_LITERAL));
        assertEquals(POSTCODE, result.get(REGIONAL_OFFICE_POSTCODE_LITERAL));
        assertEquals(dwpAddress.lines()[0], result.get(DWP_ADDRESS_LINE1_LITERAL));
        assertEquals(dwpAddress.lines()[1], result.get(DWP_ADDRESS_LINE2_LITERAL));
        assertEquals(dwpAddress.lines()[2], result.get(DWP_ADDRESS_LINE3_LITERAL));
        assertEquals(dwpAddress.lines()[3], result.get(DWP_ADDRESS_LINE4_LITERAL));
        assertEquals(now.toLocalDate().toString(), result.get(GENERATED_DATE_LITERAL));
        assertEquals("Mr T Tibbs", result.get(APPELLANT_FULL_NAME_LITERAL));
        assertEquals("PERSONAL INDEPENDENCE PAYMENT", result.get(BENEFIT_TYPE_LITERAL));
        assertEquals("123456", result.get(CASE_ID_LITERAL));
        assertEquals("JT0123456B", result.get(NINO_LITERAL));
        assertEquals("http://www.tribunals.gov.uk/", result.get(SSCS_URL_LITERAL));
    }

    @Test
    public void givenACaseDataWithNoRpc_thenGenerateThePlaceholderMappingsWithoutRpc() {
        buildCaseData(null);
        when(dwpAddressLookup.lookup("PIP", "1"))
            .thenReturn(new DwpAddress(RPC_ADDRESS1, RPC_ADDRESS2, RPC_CITY, POSTCODE));

        Map<String, Object> result = service.generatePlaceholders(caseData, now);

        assertEquals(now.toLocalDate().toString(), result.get(CASE_CREATED_DATE_LITERAL));
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
        when(dwpAddressLookup.lookup("PIP", "1")).thenReturn(dwpAddress);
        Map<String, Object> result = service.generatePlaceholders(caseData, now);
        assertEquals(dwpAddress.lines()[0], result.get(DWP_ADDRESS_LINE1_LITERAL));
        assertEquals(dwpAddress.lines()[1], result.get(DWP_ADDRESS_LINE2_LITERAL));
        assertEquals(dwpAddress.lines()[2], result.get(DWP_ADDRESS_LINE3_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE4_LITERAL));
    }

    @Test
    public void anAddressWithOneLineAndPostCodeWillNotHaveRow3AndRow4() {
        buildCaseData(null);
        DwpAddress dwpAddress = new DwpAddress(RPC_ADDRESS1, "", POSTCODE);
        when(dwpAddressLookup.lookup("PIP", "1")).thenReturn(dwpAddress);
        Map<String, Object> result = service.generatePlaceholders(caseData, now);
        assertEquals(dwpAddress.lines()[0], result.get(DWP_ADDRESS_LINE1_LITERAL));
        assertEquals(dwpAddress.lines()[1], result.get(DWP_ADDRESS_LINE2_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE3_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE4_LITERAL));
    }

    @Test
    public void addressWithOneLineWillNotHaveRow2AndRow3AndRow4() {
        buildCaseData(null);
        DwpAddress dwpAddress = new DwpAddress("", "", POSTCODE);
        when(dwpAddressLookup.lookup("PIP", "1")).thenReturn(dwpAddress);
        Map<String, Object> result = service.generatePlaceholders(caseData, now);
        assertEquals(dwpAddress.lines()[0], result.get(DWP_ADDRESS_LINE1_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE2_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE3_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE4_LITERAL));
    }

    @Test
    public void addressWithNoLinesWillNotHaveADwpAddress() {
        buildCaseData(null);
        DwpAddress dwpAddress = new DwpAddress("", "", "");
        when(dwpAddressLookup.lookup("PIP", "1")).thenReturn(dwpAddress);
        Map<String, Object> result = service.generatePlaceholders(caseData, now);
        assertNull(result.get(DWP_ADDRESS_LINE1_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE2_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE3_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE4_LITERAL));
    }

    @Test(expected = NoMrnDetailsException.class)
    public void asAppealWithNoMrnDetailsWillNotHaveADwpAddress() {
        buildCaseData(null);
        caseData = caseData.toBuilder().appeal(caseData.getAppeal().toBuilder().mrnDetails(null).build()).build();
        service.generatePlaceholders(caseData, now);
    }

    @Test(expected = NoMrnDetailsException.class)
    public void anAppealWithNoDwpIssuingOfficeWillNotHaveADwpAddress() {
        buildCaseData(null);
        caseData = caseData.toBuilder().appeal(caseData.getAppeal().toBuilder().mrnDetails(
            MrnDetails.builder().mrnLateReason("soz").build()).build()).build();
        service.generatePlaceholders(caseData, now);
    }

    private void buildCaseData(RegionalProcessingCenter rpc) {
        caseData = SscsCaseData.builder()
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
