package uk.gov.hmcts.reform.sscs.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.APPEAL_CREATED;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.robotics.domain.RoboticsWrapper;
import uk.gov.hmcts.reform.sscs.robotics.json.RoboticsJsonMapper;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RoboticsServiceIt {

    @Autowired
    private RoboticsService roboticsService;

    @MockBean
    private IdamService idamService;

    @MockBean
    private CcdClient ccdClient;

    @MockBean
    private CcdService ccdService;

    @Autowired
    private RoboticsJsonMapper mapper;

    private SscsCaseData caseData;

    private RoboticsWrapper roboticsWrapper;

    private CaseDetails<SscsCaseData> caseDetails;

    @MockBean
    private EmailService emailService;

    @Captor
    private ArgumentCaptor<SscsCaseData> caseDataCaptor;

    @Before
    public void setup() {
        caseData = SscsCaseData.builder()
            .ccdCaseId("123456")
            .regionalProcessingCenter(null)
            .evidencePresent("Yes")
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder().dwpIssuingOffice("DWP PIP (1)").build())
                .benefitType(BenefitType.builder().code("PIP").description("Personal Independence Payment").build())
                .receivedVia("paper")
                .appellant(Appellant.builder()
                    .name(Name.builder().title("Mr").firstName("Terry").lastName("Tibbs").build())
                    .address(Address.builder().line1("99 My Road").town("Grantham").county("Surrey").postcode("CV10 6PO").build())
                    .identity(Identity.builder().nino("JT0123456B").build())
                    .contact(Contact.builder().mobile(null).email(null).build())
                    .build())
                .hearingOptions(HearingOptions.builder().wantsToAttend("Yes").other("My hearing").build())
                .build())
            .build();

        caseDetails = new CaseDetails<>(1234L, "sscs", APPEAL_CREATED, caseData, null);
    }

    @Test
    public void givenSscsCaseDataWithRep_makeValidRoboticsJsonThatValidatesAgainstSchema() {

        caseData.getAppeal().setRep(Representative.builder()
            .hasRepresentative("Yes").name(Name.builder().title("Mrs").firstName("Wendy").lastName("Barker").build())
            .address(Address.builder().line1("99 My Road").town("Grantham").county("Surrey").postcode("RH5 6PO").build())
            .contact(Contact.builder().mobile(null).email(null).build())
            .build());

        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertTrue(result.has("representative"));
        assertTrue(result.has("hearingArrangements"));

        verifyNoMoreInteractions(ccdService);
    }

    @Test
    public void givenSscsCaseDataWithoutRepresentative_makeValidRoboticsJsonThatValidatesAgainstSchema() {
        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertFalse(result.has("representative"));
        assertTrue(result.has("hearingArrangements"));
        assertTrue(result.has("isReadyToList"));

        verifyNoMoreInteractions(ccdService);
    }

    @Test
    public void givenSscsCaseDataWithUcBenefitType_makeValidRoboticsJsonThatValidatesAgainstSchema() {
        caseDetails.getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("UC").build());
        caseDetails.getCaseData().getAppeal().setMrnDetails(MrnDetails.builder().dwpIssuingOffice("Universal Credit").build());

        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertFalse(result.has("representative"));
        assertTrue(result.has("hearingArrangements"));
        assertTrue(result.has("isReadyToList"));
        assertEquals("Coventry (CMCB)", result.get("appellantPostCode"));

        verifyNoMoreInteractions(ccdService);
    }

    @Test
    public void givenSscsCaseDataWithEsaBenefitType_makeValidRoboticsJsonThatValidatesAgainstSchema() {
        caseDetails.getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("ESA").build());
        caseDetails.getCaseData().getAppeal().setMrnDetails(MrnDetails.builder().dwpIssuingOffice("Birkenhead LM DRT").build());

        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertFalse(result.has("representative"));
        assertTrue(result.has("hearingArrangements"));
        assertTrue(result.has("isReadyToList"));
        assertEquals("Coventry (CMCB)", result.get("appellantPostCode"));

        verifyNoMoreInteractions(ccdService);
    }

    @Test
    public void givenSscsCaseDataWithPipBenefitType_makeValidRoboticsJsonThatValidatesAgainstSchema() {
        caseDetails.getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("Pip").build());

        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertFalse(result.has("representative"));
        assertTrue(result.has("hearingArrangements"));
        assertTrue(result.has("isReadyToList"));
        assertEquals("Nuneaton", result.get("appellantPostCode"));

        verifyNoMoreInteractions(ccdService);
    }

    @Test
    public void givenSscsCaseDataWithAppointeeAndPipBenefitType_makeValidRoboticsJsonThatValidatesAgainstSchemaWithAppointeePostcode() {
        caseDetails.getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("Pip").build());

        caseDetails.getCaseData().getAppeal().setAppellant(Appellant.builder()
            .name(Name.builder().title("Mr").firstName("Terry").lastName("Tibbs").build())
            .address(Address.builder().line1("99 My Road").town("Grantham").county("Surrey").postcode("CV10 6PO").build())
            .identity(Identity.builder().nino("JT0123456B").build())
            .contact(Contact.builder().mobile(null).email(null).build())
            .isAppointee("Yes").appointee(
                Appointee.builder().name(Name.builder().title("Mr").firstName("Terry").lastName("Tibbs").build())
                    .identity(Identity.builder().nino("JT0123456B").build())
                    .address(Address.builder().line1("99 My Road").town("Chelmsford").county("Essex").postcode("CM12 0NS").build())
                    .contact(Contact.builder().mobile(null).email(null).build())
                    .build()).build());

        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertFalse(result.has("representative"));
        assertTrue(result.has("hearingArrangements"));
        assertTrue(result.has("isReadyToList"));
        assertEquals("Basildon Combined Court", result.get("appellantPostCode"));

        verifyNoMoreInteractions(ccdService);
    }

    @Test
    public void givenSscsCaseDataWithoutHearingArrangements_makeValidRoboticsJsonThatValidatesAgainstSchema() {
        caseData.getAppeal().setHearingOptions(HearingOptions.builder().wantsToAttend("Yes").build());

        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertFalse(result.has("representative"));
        assertFalse(result.has("hearingArrangements"));

        verifyNoMoreInteractions(ccdService);
    }

    @Test
    public void givenDwpOfficeIsClosed_thenSaveNewOfficeToCaseWhenRoboticsIsProcessed() {
        caseDetails.getCaseData().getAppeal().getMrnDetails().setDwpIssuingOffice("1");
        roboticsService.sendCaseToRobotics(caseDetails);

        verify(ccdService).updateCase(caseDataCaptor.capture(), any(), any(), any(), any(), any());

        assertEquals("DWP PIP (1)", caseDataCaptor.getValue().getAppeal().getMrnDetails().getDwpIssuingOffice());
    }
}
