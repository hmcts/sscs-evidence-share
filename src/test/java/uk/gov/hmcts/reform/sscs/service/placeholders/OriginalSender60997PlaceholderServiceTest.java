package uk.gov.hmcts.reform.sscs.service.placeholders;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REPRESENTATIVE_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderHelper.buildCaseData;

import java.util.Map;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class OriginalSender60997PlaceholderServiceTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private RpcPlaceholderService rpcPlaceholderService;
    @Mock
    private CommonPlaceholderService commonPlaceholderService;

    @InjectMocks
    private OriginalSender60997PlaceholderService originalSender60997PlaceholderService;

    @Test
    @Parameters(method = "generateSscsCaseDataScenariosForAppellantAndAppointes")
    public void populatePlaceHolders(SscsCaseData caseData, DocumentType documentType, String expected) {
        Map<String, Object> actual = originalSender60997PlaceholderService
            .populatePlaceHolders(caseData, documentType);
        assertEquals(expected, actual.toString());
    }

    private Object[] generateSscsCaseDataScenariosForAppellantAndAppointes() {
        SscsCaseData sscsCaseDataWithAppointee = SscsCaseData.builder()
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

        SscsCaseData sscsCaseDataWithRep = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("PIP").build())
                .mrnDetails(MrnDetails.builder().dwpIssuingOffice("1").build())
                .rep(Representative.builder()
                    .address(Address.builder()
                        .line1("HM Courts & Tribunals Service Reps")
                        .town("Social Security & Child Support Appeals Reps")
                        .county("Prudential Buildings Reps")
                        .postcode("L2 5UZ")
                        .build())
                    .build())
                .build())
            .build();

        String expectedAppellant = "{party_address_line1=HM Courts & Tribunals Service, "
            + "party_address_line3=Social Security & Child Support Appeals, "
            + "party_address_line2=Down the road, "
            + "party_address_line5=L2 5UZ, "
            + "party_address_line4=Prudential Buildings}";

        String expectedAppointee = "{party_address_line1=HM Courts & Tribunals Service Appointee, "
            + "party_address_line3=Prudential Buildings Appointee, "
            + "party_address_line2=Social Security & Child Support Appeals Appointee, "
            + "party_address_line4=L2 5UZ}";

        String expectedRep = "{party_address_line1=HM Courts & Tribunals Service Reps, "
            + "party_address_line3=Prudential Buildings Reps, "
            + "party_address_line2=Social Security & Child Support Appeals Reps, "
            + "party_address_line4=L2 5UZ}";

        //edge cases

        SscsCaseData caseDataWithNullAppellantAddress = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .appellant(Appellant.builder()
                    .build())
                .build())
            .build();

        SscsCaseData caseDataWithNullAppellant = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .appellant(Appellant.builder()
                    .build())
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
            new Object[]{sscsCaseDataWithAppointee, APPELLANT_EVIDENCE, expectedAppointee},
            new Object[]{buildCaseData(), APPELLANT_EVIDENCE, expectedAppellant},
            new Object[]{sscsCaseDataWithRep, REPRESENTATIVE_EVIDENCE, expectedRep},
            //edge scenarios
            new Object[]{caseDataWithNullAppellantAddress, APPELLANT_EVIDENCE, expectedDefaultEmptyAddress},
            new Object[]{caseDataWithNullAppellant, APPELLANT_EVIDENCE, expectedDefaultEmptyAddress},
            new Object[]{caseDataWithNullAppointeeAddress, APPELLANT_EVIDENCE, expectedDefaultEmptyAddress}
        };
    }
}
