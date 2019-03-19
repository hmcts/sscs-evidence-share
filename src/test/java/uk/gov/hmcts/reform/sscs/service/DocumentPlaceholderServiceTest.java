package uk.gov.hmcts.reform.sscs.service;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.*;

import java.util.Map;
import org.joda.time.DateTimeUtils;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

public class DocumentPlaceholderServiceTest {

    private static final String RPC_ADDRESS1 = "HM Courts & Tribunals Service";
    private static final String RPC_ADDRESS2 = "Social Security & Child Support Appeals";
    private static final String RPC_ADDRESS3 = "Prudential Buildings";
    private static final String RPC_ADDRESS4 = "36 Dale Street";
    private static final String RPC_CITY = "LIVERPOOL";
    private static final String POSTCODE = "L2 5UZ";

    private DocumentPlaceholderService service;
    private SscsCaseData caseData;

    @Before
    public void setup() {
        service = new DocumentPlaceholderService();
        DateTimeUtils.setCurrentMillisFixed(1550000000000L);
    }

    @Test
    public void givenACaseData_thenGenerateThePlaceholderMappings() {
        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
            .name("Liverpool").address1(RPC_ADDRESS1).address2(RPC_ADDRESS2).address3(RPC_ADDRESS3).address4(RPC_ADDRESS4).city(RPC_CITY).postcode(POSTCODE).build();

        buildCaseData(rpc);

        Map<String, Object> result = service.generatePlaceholders(caseData);

        assertEquals(result.get(CASE_CREATED_DATE_LITERAL), "2018-10-23");
        assertEquals(result.get(REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL), RPC_ADDRESS1);
        assertEquals(result.get(REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL), RPC_ADDRESS2);
        assertEquals(result.get(REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL), RPC_ADDRESS3);
        assertEquals(result.get(REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL), RPC_ADDRESS4);
        assertEquals(result.get(REGIONAL_OFFICE_COUNTY_LITERAL), RPC_CITY);
        assertEquals(result.get(REGIONAL_OFFICE_POSTCODE_LITERAL), POSTCODE);
        assertEquals(result.get(DWP_ADDRESS_LINE1_LITERAL), "Dummy dwp address line1");
        assertEquals(result.get(DWP_ADDRESS_LINE2_LITERAL), "Dummy dwp address line2");
        assertEquals(result.get(DWP_ADDRESS_LINE3_LITERAL), "Dummy dwp address line3");
        assertEquals(result.get(DWP_ADDRESS_LINE4_LITERAL), "Dummy dwp address line4");
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

        Map<String, Object> result = service.generatePlaceholders(caseData);

        assertEquals(result.get(CASE_CREATED_DATE_LITERAL), "2018-10-23");
        assertNull(result.get(REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL));
        assertNull(result.get(REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL));
        assertNull(result.get(REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL));
        assertNull(result.get(REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL));
        assertNull(result.get(REGIONAL_OFFICE_COUNTY_LITERAL));
        assertNull(result.get(REGIONAL_OFFICE_POSTCODE_LITERAL));

    }

    private void buildCaseData(RegionalProcessingCenter rpc) {
        caseData = SscsCaseData.builder()
            .caseCreated("2018-10-23T15:04:48.187")
            .ccdCaseId("123456")
            .regionalProcessingCenter(rpc)
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("PIP").description("Personal Independence Payment").build())
                .appellant(Appellant.builder()
                    .name(Name.builder().title("Mr").firstName("Terry").lastName("Tibbs").build())
                    .identity(Identity.builder().nino("JT0123456B").build())
                    .build())
                .build())
            .build();
    }
}
