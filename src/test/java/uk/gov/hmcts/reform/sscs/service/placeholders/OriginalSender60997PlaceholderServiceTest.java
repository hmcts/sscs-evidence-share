package uk.gov.hmcts.reform.sscs.service.placeholders;

import static org.junit.Assert.assertEquals;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

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
    public void populatePlaceHolders(SscsCaseData caseData, String expected) {
        Map<String, Object> actual = originalSender60997PlaceholderService.populatePlaceHolders(caseData);
        assertEquals(expected, actual.toString());
    }

    private Object[] generateSscsCaseDataScenariosForAppellantAndAppointes() {
        SscsCaseData sscsCaseDataWithAppointee = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .appellant(Appellant.builder()
                    .appointee(Appointee.builder()
                        .name(Name.builder().title("Mr").firstName("Terry").lastName("Appointee").build())
                        .identity(Identity.builder().nino("JT0123456B").build())
                        .address(Address.builder()
                            .line1("HM Courts & Tribunals Service Appointee")
                            .line2("Social Security & Child Support Appeals Appointee")
                            .county("Prudential Buildings Appointee")
                            .postcode("L2 5UZ")
                            .build())
                        .build())
                    .isAppointee("Yes")
                    .build())
                .build())
            .build();

        String expectedAppellant = "{original_sender_address_line3=Prudential Buildings, "
            + "original_sender_address_line4=L2 5UZ, "
            + "original_sender_address_line1=HM Courts & Tribunals Service, "
            + "original_sender_address_line2=Social Security & Child Support Appeals}";

        String expectedAppointee = "{original_sender_address_line3=Prudential Buildings Appointee, "
            + "original_sender_address_line4=L2 5UZ, "
            + "original_sender_address_line1=HM Courts & Tribunals Service Appointee, "
            + "original_sender_address_line2=Social Security & Child Support Appeals Appointee}";

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

        String expectedDefaultEmptyAddress = "{original_sender_address_line3=, original_sender_address_line4=, "
            + "original_sender_address_line1=, original_sender_address_line2=}";

        return new Object[]{
            new Object[]{sscsCaseDataWithAppointee, expectedAppointee},
            new Object[]{buildCaseData(), expectedAppellant},
            new Object[]{caseDataWithNullAppellantAddress, expectedDefaultEmptyAddress},
            new Object[]{caseDataWithNullAppellant, expectedDefaultEmptyAddress},
            new Object[]{caseDataWithNullAppointeeAddress, expectedDefaultEmptyAddress}
        };
    }
}
