package uk.gov.hmcts.reform.sscs.service.placeholders;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.*;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderHelper.buildCaseData;

import java.util.Map;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.domain.DwpAddress;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookup;

@RunWith(JUnitParamsRunner.class)
public class FurtherEvidencePlaceholderServiceTest {

    private static final String RPC_ADDRESS1 = "HM Courts & Tribunals Service";
    private static final String POSTCODE = "L2 5UZ";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private RpcPlaceholderService rpcPlaceholderService;
    @Mock
    private CommonPlaceholderService commonPlaceholderService;
    @Mock
    private DwpAddressLookup dwpAddressLookup;

    @InjectMocks
    private FurtherEvidencePlaceholderService furtherEvidencePlaceholderService;

    SscsCaseData sscsCaseDataWithAppointee;

    SscsCaseData sscsCaseDataWithRep;

    SscsCaseData caseDataWithNullAppellantAddress;

    private DwpAddress dwpAddress = new DwpAddress("HM Courts & Tribunals Service Dwp", "Prudential Buildings Dwp",  "L2 5UZ");

    @Before
    public void setup() {
        sscsCaseDataWithAppointee = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("PIP").build())
                .mrnDetails(MrnDetails.builder().dwpIssuingOffice("1").build())
                .appellant(Appellant.builder()
                    .appointee(Appointee.builder()
                        .name(Name.builder().title("Mr").firstName("Terry").lastName("Appointee").build())
                        .identity(Identity.builder().nino("JT0123456B").build())
                        .address(Address.builder()
                            .line1("HM Courts & Tribunals Service Appointee")
                            .town("Social Security & Child Support Appeals Appointee")
                            .county("Prudential Buildings Appointee")
                            .postcode("L2 5UZ")
                            .build())
                        .build())
                    .isAppointee("Yes")
                    .build())
                .build())
            .build();

        sscsCaseDataWithRep = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("PIP").build())
                .mrnDetails(MrnDetails.builder().dwpIssuingOffice("1").build())
                .rep(Representative.builder()
                    .name(Name.builder().title("Mr").firstName("Terry").lastName("Rep").build())
                    .address(Address.builder()
                        .line1("HM Courts & Tribunals Service Reps")
                        .town("Social Security & Child Support Appeals Reps")
                        .county("Prudential Buildings Reps")
                        .postcode("L2 5UZ")
                        .build())
                    .build())
                .build())
            .build();
    }

    @Test
    public void givenAnAppellant_thenGenerateThePlaceholders() {
        Map<String, Object> actual = furtherEvidencePlaceholderService.populatePlaceHolders(buildCaseData(), APPELLANT_LETTER);

        assertEquals("HM Courts & Tribunals Service", actual.get("party_address_line1"));
        assertEquals("Down the road", actual.get("party_address_line2"));
        assertEquals("Social Security & Child Support Appeals", actual.get("party_address_line3"));
        assertEquals("Prudential Buildings", actual.get("party_address_line4"));
        assertEquals("L2 5UZ", actual.get("party_address_line5"));
        assertEquals("Terry Tibbs", actual.get("name"));
    }

    @Test
    public void givenAnAppointee_thenGenerateThePlaceholders() {
        Map<String, Object> actual = furtherEvidencePlaceholderService.populatePlaceHolders(sscsCaseDataWithAppointee, APPELLANT_LETTER);

        assertEquals("HM Courts & Tribunals Service Appointee", actual.get("party_address_line1"));
        assertEquals("Social Security & Child Support Appeals Appointee", actual.get("party_address_line2"));
        assertEquals("Prudential Buildings Appointee", actual.get("party_address_line3"));
        assertEquals("L2 5UZ", actual.get("party_address_line4"));
        assertEquals("Terry Appointee", actual.get("name"));
    }

    @Test
    public void givenARep_thenGenerateThePlaceholders() {
        Map<String, Object> actual = furtherEvidencePlaceholderService.populatePlaceHolders(sscsCaseDataWithRep, REPRESENTATIVE_LETTER);

        assertEquals("HM Courts & Tribunals Service Reps", actual.get("party_address_line1"));
        assertEquals("Social Security & Child Support Appeals Reps", actual.get("party_address_line2"));
        assertEquals("Prudential Buildings Reps", actual.get("party_address_line3"));
        assertEquals("L2 5UZ", actual.get("party_address_line4"));
        assertEquals("Terry Rep", actual.get("name"));
    }

    @Test
    @Parameters(method = "generateSscsCaseDataEdgeCaseScenariosForAppellantAndAppointees")
    public void handleEdgeCaseScenarios(SscsCaseData caseData, FurtherEvidenceLetterType letterType, String expected) {
        Map<String, Object> actual = furtherEvidencePlaceholderService
            .populatePlaceHolders(caseData, letterType);
        assertEquals(expected, actual.toString());
    }

    @Test
    public void populateDwpAddressPlaceHolders() {

        when(dwpAddressLookup.lookup("PIP", "1")).thenReturn(dwpAddress);

        String expectedDwp = "{party_address_line1=HM Courts & Tribunals Service Dwp, "
            + "party_address_line3=L2 5UZ, "
            + "party_address_line2=Prudential Buildings Dwp"
            + "}";

        Map<String, Object> actual = furtherEvidencePlaceholderService
            .populatePlaceHolders(sscsCaseDataWithRep, DWP_LETTER);
        assertEquals(expectedDwp, actual.toString());
    }


    private Object[] generateSscsCaseDataEdgeCaseScenariosForAppellantAndAppointees() {
        setup();

        SscsCaseData caseDataWithNullAppellantAddress = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .appellant(Appellant.builder()
                    .build())
                .build())
            .build();

        SscsCaseData caseDataWithNullAppellant = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .appellant(null)
                .build())
            .build();

        SscsCaseData caseDataWithNullAppointeeAddress = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .appellant(Appellant.builder()
                    .isAppointee("yes")
                    .build())
                .build())
            .build();

        String expectedDefaultEmptyAddress = "{party_address_line1=, party_address_line3=, "
            + "party_address_line2=, party_address_line4=}";

        return new Object[]{
            //edge scenarios
            new Object[]{caseDataWithNullAppellantAddress, APPELLANT_LETTER, expectedDefaultEmptyAddress},
            new Object[]{caseDataWithNullAppellant, APPELLANT_LETTER, expectedDefaultEmptyAddress},
            new Object[]{caseDataWithNullAppointeeAddress, APPELLANT_LETTER, expectedDefaultEmptyAddress}
        };
    }
}
