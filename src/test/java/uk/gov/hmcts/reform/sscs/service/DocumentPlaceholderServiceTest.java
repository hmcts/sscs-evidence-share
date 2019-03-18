package uk.gov.hmcts.reform.sscs.service;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.*;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class DocumentPlaceholderServiceTest {

    private static final String RPC_ADDRESS1 = "HM Courts & Tribunals Service";
    private static final String RPC_ADDRESS2 = "Social Security & Child Support Appeals";
    private static final String RPC_ADDRESS3 = "Prudential Buildings";
    private static final String RPC_ADDRESS4 = "36 Dale Street";
    private static final String RPC_CITY = "LIVERPOOL";
    private static final String POSTCODE = "L2 5UZ";

    private DocumentPlaceholderService service;

    @Before
    public void setup() {
        service = new DocumentPlaceholderService();
    }

    @Test
    public void givenACaseData_thenGenerateThePlaceholderMappings() {
        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
            .name("Liverpool").address1(RPC_ADDRESS1).address2(RPC_ADDRESS2).address3(RPC_ADDRESS3).address4(RPC_ADDRESS4).city(RPC_CITY).postcode(POSTCODE).build();

        SscsCaseData caseData = SscsCaseData.builder().caseCreated("01/01/2019").regionalProcessingCenter(rpc).build();

        Map<String, Object> result = service.generatePlaceholders(caseData);

        assertEquals(result.get(CASE_CREATED_DATE_LITERAL), "01/01/2019");
        assertEquals(result.get(REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL), RPC_ADDRESS1);
        assertEquals(result.get(REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL), RPC_ADDRESS2);
        assertEquals(result.get(REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL), RPC_ADDRESS3);
        assertEquals(result.get(REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL), RPC_ADDRESS4);
        assertEquals(result.get(REGIONAL_OFFICE_COUNTY_LITERAL), RPC_CITY);
        assertEquals(result.get(REGIONAL_OFFICE_POSTCODE_LITERAL), POSTCODE);
    }

    @Test
    public void givenACaseDataWithNoRpc_thenGenerateThePlaceholderMappingsWithoutRpc() {
        SscsCaseData caseData = SscsCaseData.builder().caseCreated("01/01/2019").build();

        Map<String, Object> result = service.generatePlaceholders(caseData);

        assertEquals(result.get(CASE_CREATED_DATE_LITERAL), "01/01/2019");
        assertNull(result.get(REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL));
        assertNull(result.get(REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL));
        assertNull(result.get(REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL));
        assertNull(result.get(REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL));
        assertNull(result.get(REGIONAL_OFFICE_COUNTY_LITERAL));
        assertNull(result.get(REGIONAL_OFFICE_POSTCODE_LITERAL));
    }
}
