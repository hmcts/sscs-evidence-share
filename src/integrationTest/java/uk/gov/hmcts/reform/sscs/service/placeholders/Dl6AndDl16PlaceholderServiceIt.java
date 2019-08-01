package uk.gov.hmcts.reform.sscs.service.placeholders;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.APPELLANT_FULL_NAME_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.BENEFIT_TYPE_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.CASE_CREATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.CASE_ID_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.DWP_ADDRESS_LINE1_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.DWP_ADDRESS_LINE2_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.DWP_ADDRESS_LINE3_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.DWP_ADDRESS_LINE4_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.GENERATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.NINO_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_COUNTY_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_POSTCODE_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.SSCS_URL_LITERAL;

import java.time.LocalDateTime;
import java.util.Map;
import org.joda.time.DateTimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.DwpAddress;
import uk.gov.hmcts.reform.sscs.exception.NoMrnDetailsException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class Dl6AndDl16PlaceholderServiceIt {

    private static final String RPC_ADDRESS1 = "HM Courts & Tribunals Service";
    private static final String RPC_ADDRESS2 = "Social Security & Child Support Appeals";
    private static final String RPC_ADDRESS3 = "Prudential Buildings";
    private static final String RPC_ADDRESS4 = "36 Dale Street";
    private static final String RPC_CITY = "LIVERPOOL";
    private static final String POSTCODE = "L2 5UZ";

    private SscsCaseData caseData;
    private LocalDateTime now;

    @Autowired
    private Dl6AndDl16PlaceholderService dl6AndDl16PlaceholderService;

    @Before
    public void setup() {
        DateTimeUtils.setCurrentMillisFixed(1550000000000L);
        now = LocalDateTime.now();
    }

    @Test
    public void givenACaseData_thenGenerateThePlaceholderMappings() {

        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
            .name("Liverpool").address1(RPC_ADDRESS1).address2(RPC_ADDRESS2).address3(RPC_ADDRESS3)
            .address4(RPC_ADDRESS4).city(RPC_CITY).postcode(POSTCODE).build();

        caseData = buildCaseData(rpc);
        Map<String, Object> result = dl6AndDl16PlaceholderService.populatePlaceholders(caseData, now);

        assertEquals(now.toLocalDate().toString(), result.get(CASE_CREATED_DATE_LITERAL));
        assertEquals(RPC_ADDRESS1, result.get(REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL));
        assertEquals(RPC_ADDRESS2, result.get(REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL));
        assertEquals(RPC_ADDRESS3, result.get(REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL));
        assertEquals(RPC_ADDRESS4, result.get(REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL));
        assertEquals(RPC_CITY, result.get(REGIONAL_OFFICE_COUNTY_LITERAL));
        assertEquals(POSTCODE, result.get(REGIONAL_OFFICE_POSTCODE_LITERAL));
        assertEquals("Mail Handling Site A", result.get(DWP_ADDRESS_LINE1_LITERAL));
        assertEquals("WOLVERHAMPTON", result.get(DWP_ADDRESS_LINE2_LITERAL));
        assertEquals(now.toLocalDate().toString(), result.get(GENERATED_DATE_LITERAL));
        assertEquals("Mr T Tibbs", result.get(APPELLANT_FULL_NAME_LITERAL));
        assertEquals("PERSONAL INDEPENDENCE PAYMENT", result.get(BENEFIT_TYPE_LITERAL));
        assertEquals("123456", result.get(CASE_ID_LITERAL));
        assertEquals("JT0123456B", result.get(NINO_LITERAL));
        assertEquals("http://www.tribunals.gov.uk/", result.get(SSCS_URL_LITERAL));
    }

    @Test
    public void givenACaseDataWithNoRpc_thenGenerateThePlaceholderMappingsWithoutRpc() {
        caseData = buildCaseData(null);
        Map<String, Object> result = dl6AndDl16PlaceholderService.populatePlaceholders(caseData, now);

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
        caseData = buildCaseData(null);
        DwpAddress dwpAddress = new DwpAddress("Mail Handling Site A", "WOLVERHAMPTON", "WV98 1AA");
        Map<String, Object> result = dl6AndDl16PlaceholderService.populatePlaceholders(caseData, now);
        assertEquals(dwpAddress.lines()[0], result.get(DWP_ADDRESS_LINE1_LITERAL));
        assertEquals(dwpAddress.lines()[1], result.get(DWP_ADDRESS_LINE2_LITERAL));
        assertEquals(dwpAddress.lines()[2], result.get(DWP_ADDRESS_LINE3_LITERAL));
        assertNull(result.get(DWP_ADDRESS_LINE4_LITERAL));
    }

    @Test(expected = NoMrnDetailsException.class)
    public void asAppealWithNoMrnDetailsWillNotHaveADwpAddress() {
        caseData = buildCaseData(null);
        caseData = caseData.toBuilder().appeal(caseData.getAppeal().toBuilder().mrnDetails(null).build()).build();
        dl6AndDl16PlaceholderService.populatePlaceholders(caseData, now);
    }

    @Test(expected = NoMrnDetailsException.class)
    public void anAppealWithNoDwpIssuingOfficeWillNotHaveADwpAddress() {
        caseData = buildCaseData(null);
        caseData = caseData.toBuilder().appeal(caseData.getAppeal().toBuilder().mrnDetails(
            MrnDetails.builder().mrnLateReason("soz").build()).build()).build();
        dl6AndDl16PlaceholderService.populatePlaceholders(caseData, now);
    }

    private SscsCaseData buildCaseData(RegionalProcessingCenter rpc) {
        return SscsCaseData.builder()
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
