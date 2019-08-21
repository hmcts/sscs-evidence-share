package uk.gov.hmcts.reform.sscs.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.robotics.domain.RoboticsWrapper;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RoboticsServiceIt {

    @Autowired
    private RoboticsService roboticsService;

    private SscsCaseData caseData;

    private RoboticsWrapper appeal;

    @Before
    public void setup() {
        caseData = SscsCaseData.builder()
            .ccdCaseId("123456")
            .regionalProcessingCenter(null)
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder().dwpIssuingOffice("1").build())
                .benefitType(BenefitType.builder().code("PIP").description("Personal Independence Payment").build())
                .appellant(Appellant.builder()
                    .name(Name.builder().title("Mr").firstName("Terry").lastName("Tibbs").build())
                    .address(Address.builder().line1("99 My Road").town("Grantham").county("Surrey").postcode("RH5 6PO").build())
                    .identity(Identity.builder().nino("JT0123456B").build())
                    .contact(Contact.builder().mobile(null).email(null).build())
                    .build())
                .hearingOptions(HearingOptions.builder().wantsToAttend("Yes").other("My hearing").build())
                .build())
            .build();

        appeal =
            RoboticsWrapper
                .builder()
                .sscsCaseData(caseData)
                .ccdCaseId(1234L)
                .evidencePresent("Yes")
                .build();
    }

    @Test
    public void givenSyaData_makeValidRoboticsJsonThatValidatesAgainstSchema() {

        caseData.getAppeal().setRep(Representative.builder()
            .hasRepresentative("Yes").name(Name.builder().title("Mrs").firstName("Wendy").lastName("Barker").build())
            .address(Address.builder().line1("99 My Road").town("Grantham").county("Surrey").postcode("RH5 6PO").build())
            .contact(Contact.builder().mobile(null).email(null).build())
            .build());

        JSONObject result = roboticsService.createRobotics(appeal);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertTrue(result.has("representative"));
        assertTrue(result.has("hearingArrangements"));
    }

    @Test
    public void givenSyaDataWithoutRepresentative_makeValidRoboticsJsonThatValidatesAgainstSchema() {

        JSONObject result = roboticsService.createRobotics(appeal);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertFalse(result.has("representative"));
        assertTrue(result.has("hearingArrangements"));
    }

    @Test
    public void givenSyaDataWithoutHearingArrangements_makeValidRoboticsJsonThatValidatesAgainstSchema() {
        caseData.getAppeal().setHearingOptions(HearingOptions.builder().wantsToAttend("Yes").build());

        JSONObject result = roboticsService.createRobotics(appeal);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertFalse(result.has("representative"));
        assertFalse(result.has("hearingArrangements"));
    }

}
