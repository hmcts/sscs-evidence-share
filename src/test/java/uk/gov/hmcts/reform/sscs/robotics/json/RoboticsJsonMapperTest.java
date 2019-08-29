package uk.gov.hmcts.reform.sscs.robotics.json;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.robotics.domain.RoboticsWrapper;

@RunWith(JUnitParamsRunner.class)
public class RoboticsJsonMapperTest {
    private RoboticsJsonMapper roboticsJsonMapper = new RoboticsJsonMapper();
    private RoboticsWrapper appeal;
    private RoboticsJsonValidator roboticsJsonValidator = new RoboticsJsonValidator(
        "/schema/sscs-robotics.json");
    private JSONObject roboticsJson;

    @Before
    public void setup() {
        appeal = RoboticsWrapper
            .builder()
            .sscsCaseData(buildCaseData())
            .ccdCaseId(123L).venueName("Bromley").evidencePresent("Yes")
            .build();
    }

    @Test
    public void mapsAppealToRoboticsJson() {
        roboticsJsonMapper = new RoboticsJsonMapper();
        roboticsJson = roboticsJsonMapper.map(appeal);

        roboticsJsonValidator.validate(roboticsJson);

        assertEquals(
            "If this fails, add an assertion below, do not just increment the number :)", 17,
            roboticsJson.length()
        );

        assertEquals("002DD", roboticsJson.get("caseCode"));
        assertEquals(123L, roboticsJson.get("caseId"));
        assertEquals("AB 22 55 66 B", roboticsJson.get("appellantNino"));
        assertEquals("Bromley", roboticsJson.get("appellantPostCode"));
        assertEquals(LocalDate.now().toString(), roboticsJson.get("appealDate"));
        assertEquals("2018-06-29", roboticsJson.get("mrnDate"));
        assertEquals("Lost my paperwork", roboticsJson.get("mrnReasonForBeingLate"));
        assertEquals("1", roboticsJson.get("pipNumber"));
        assertEquals("Oral", roboticsJson.get("hearingType"));
        assertEquals("Mr User Test", roboticsJson.get("hearingRequestParty"));
        assertEquals("Yes", roboticsJson.get("evidencePresent"));
        assertEquals("Online", roboticsJson.get("receivedVia"));
        assertTrue(roboticsJson.has("rpcEmail"));
        assertEquals("Cardiff_SYA_Respon@justice.gov.uk", roboticsJson.get("rpcEmail"));

        assertEquals(
            "If this fails, add an assertion below, do not just increment the number :)", 11,
            roboticsJson.getJSONObject("appointee").length()
        );

        assertEquals("Mrs", roboticsJson.getJSONObject("appointee").get("title"));
        assertEquals("April", roboticsJson.getJSONObject("appointee").get("firstName"));
        assertEquals("Appointer", roboticsJson.getJSONObject("appointee").get("lastName"));
        assertEquals("No", roboticsJson.getJSONObject("appointee").get("sameAddressAsAppellant"));
        assertEquals("Apton", roboticsJson.getJSONObject("appointee").get("townOrCity"));
        assertEquals("07700 900555", roboticsJson.getJSONObject("appointee").get("phoneNumber"));
        assertEquals("Appshire", roboticsJson.getJSONObject("appointee").get("county"));
        assertEquals("42 Appointed Mews", roboticsJson.getJSONObject("appointee").get("addressLine1"));
        assertEquals("Apford", roboticsJson.getJSONObject("appointee").get("addressLine2"));
        assertEquals("AP12 4PA", roboticsJson.getJSONObject("appointee").get("postCode"));
        assertEquals("appointee@hmcts.net", roboticsJson.getJSONObject("appointee").get("email"));

        assertEquals(
            "If this fails, add an assertion below, do not just increment the number :)", 10,
            roboticsJson.getJSONObject("appellant").length()
        );

        assertEquals("Mr", roboticsJson.getJSONObject("appellant").get("title"));
        assertEquals("User", roboticsJson.getJSONObject("appellant").get("firstName"));
        assertEquals("Test", roboticsJson.getJSONObject("appellant").get("lastName"));
        assertEquals("123 Hairy Lane", roboticsJson.getJSONObject("appellant").get("addressLine1"));
        assertEquals("Off Hairy Park", roboticsJson.getJSONObject("appellant").get("addressLine2"));
        assertEquals("Hairyfield", roboticsJson.getJSONObject("appellant").get("townOrCity"));
        assertEquals("Kent", roboticsJson.getJSONObject("appellant").get("county"));
        assertEquals("TN32 6PL", roboticsJson.getJSONObject("appellant").get("postCode"));
        assertEquals("01234567890", roboticsJson.getJSONObject("appellant").get("phoneNumber"));
        assertEquals("mail@email.com", roboticsJson.getJSONObject("appellant").get("email"));

        assertEquals(
            "If this fails, add an assertion below, do not just increment the number :)", 11,
            roboticsJson.getJSONObject("appointee").length()
        );

        assertEquals("Mrs", roboticsJson.getJSONObject("appointee").get("title"));
        assertEquals("April", roboticsJson.getJSONObject("appointee").get("firstName"));
        assertEquals("Appointer", roboticsJson.getJSONObject("appointee").get("lastName"));
        assertEquals("No", roboticsJson.getJSONObject("appointee").get("sameAddressAsAppellant"));
        assertEquals("42 Appointed Mews", roboticsJson.getJSONObject("appointee").get("addressLine1"));
        assertEquals("Apford", roboticsJson.getJSONObject("appointee").get("addressLine2"));
        assertEquals("Apton", roboticsJson.getJSONObject("appointee").get("townOrCity"));
        assertEquals("Appshire", roboticsJson.getJSONObject("appointee").get("county"));
        assertEquals("AP12 4PA", roboticsJson.getJSONObject("appointee").get("postCode"));
        assertEquals("07700 900555", roboticsJson.getJSONObject("appointee").get("phoneNumber"));
        assertEquals("appointee@hmcts.net", roboticsJson.getJSONObject("appointee").get("email"));

        assertEquals(
            "If this fails, add an assertion, do not just increment the number :)", 11,
            roboticsJson.getJSONObject("representative").length()
        );

        assertEquals("Mrs", roboticsJson.getJSONObject("representative").get("title"));
        assertEquals("Wendy", roboticsJson.getJSONObject("representative").get("firstName"));
        assertEquals("Giles", roboticsJson.getJSONObject("representative").get("lastName"));
        assertEquals("HP Ltd", roboticsJson.getJSONObject("representative").get("organisation"));
        assertEquals("123 Hairy Lane", roboticsJson.getJSONObject("representative").get("addressLine1"));
        assertEquals("Off Hairy Park", roboticsJson.getJSONObject("representative").get("addressLine2"));
        assertEquals("Hairyfield", roboticsJson.getJSONObject("representative").get("townOrCity"));
        assertEquals("Kent", roboticsJson.getJSONObject("representative").get("county"));
        assertEquals("TN32 6PL", roboticsJson.getJSONObject("representative").get("postCode"));
        assertEquals("01234567890", roboticsJson.getJSONObject("representative").get("phoneNumber"));
        assertEquals("mail@email.com", roboticsJson.getJSONObject("representative").get("email"));

        assertEquals(
            "If this fails, add an assertion below, do not just increment the number :)", 6,
            roboticsJson.getJSONObject("hearingArrangements").length()
        );

        assertEquals("Spanish", roboticsJson.getJSONObject("hearingArrangements").get("languageInterpreter"));
        assertEquals("A sign language", roboticsJson.getJSONObject("hearingArrangements").get("signLanguageInterpreter"));
        assertEquals("Yes", roboticsJson.getJSONObject("hearingArrangements").get("hearingLoop"));
        assertEquals("No", roboticsJson.getJSONObject("hearingArrangements").get("accessibleHearingRoom"));
        assertEquals("Yes, this...", roboticsJson.getJSONObject("hearingArrangements").get("other"));
        assertEquals(3, roboticsJson.getJSONObject("hearingArrangements").getJSONArray("datesCantAttend").length());
        assertEquals("2018-06-30", roboticsJson.getJSONObject("hearingArrangements").getJSONArray("datesCantAttend").get(0));
        assertEquals("2018-07-30", roboticsJson.getJSONObject("hearingArrangements").getJSONArray("datesCantAttend").get(1));
        assertEquals("2018-08-30", roboticsJson.getJSONObject("hearingArrangements").getJSONArray("datesCantAttend").get(2));
    }

    @Test
    public void mapRepTitleToDefaultValuesWhenSetToNull() {
        appeal.getSscsCaseData().getAppeal().getRep().getName().setTitle(null);

        roboticsJson = roboticsJsonMapper.map(appeal);

        assertEquals("s/m", roboticsJson.getJSONObject("representative").get("title"));
    }

    @Test
    @Parameters({"PIP, 002DD", "ESA, 051DD", "null, 002DD", ", 002DD"})
    public void givenBenefitType_shouldMapCaseCodeAccordingly(String benefitCode, String expectedCaseCode) {
        appeal.getSscsCaseData().getAppeal().getBenefitType().setCode(benefitCode);

        roboticsJson = roboticsJsonMapper.map(appeal);

        assertThat(roboticsJson.get("caseCode"), is(expectedCaseCode));
    }

    @Test
    @Parameters({"051DD, 051DD", "null, 002DD", ", 002DD"})
    public void givenCaseCodeOnCase_shouldSetRetrieveCaseCodeAccordingly(@Nullable String caseCode, String expectedCaseCode) {
        appeal.getSscsCaseData().setCaseCode(caseCode);

        roboticsJson = roboticsJsonMapper.map(appeal);

        assertThat(roboticsJson.get("caseCode"), is(expectedCaseCode));
    }

    @Test
    public void mapRepFirstNameToDefaultValuesWhenSetToNull() {
        appeal.getSscsCaseData().getAppeal().getRep().getName().setFirstName(null);

        roboticsJson = roboticsJsonMapper.map(appeal);

        assertEquals(".", roboticsJson.getJSONObject("representative").get("firstName"));
    }

    @Test
    public void mapRepLastNameToDefaultValuesWhenSetToNull() {
        appeal.getSscsCaseData().getAppeal().getRep().getName().setLastName(null);

        roboticsJson = roboticsJsonMapper.map(appeal);

        assertEquals(".", roboticsJson.getJSONObject("representative").get("lastName"));
    }

    @Test
    public void givenLanguageInterpreterIsTrue_thenSetToLanguageInterpreterType() {
        appeal.getSscsCaseData().getAppeal().getHearingOptions().setLanguages("My Language");
        appeal.getSscsCaseData().getAppeal().getHearingOptions().setLanguageInterpreter("Yes");

        roboticsJson = roboticsJsonMapper.map(appeal);

        assertEquals("My Language", roboticsJson.getJSONObject("hearingArrangements").get("languageInterpreter"));
    }

    @Test
    public void givenHearingArrangementIsNull_thenSetToExcludeDatesHearingLoopAndAccHearingRoom() {
        DateRange dateRange1 = DateRange.builder()
            .start("2018-06-30")
            .end("2018-06-30")
            .build();
        List<ExcludeDate> excludeDates = new ArrayList<>();
        excludeDates.add(ExcludeDate.builder()
            .value(dateRange1)
            .build());
        appeal.getSscsCaseData().getAppeal().getHearingOptions().setArrangements(null);
        appeal.getSscsCaseData().getAppeal().getHearingOptions().setExcludeDates(excludeDates);

        roboticsJson = roboticsJsonMapper.map(appeal);

        assertEquals("No", roboticsJson.getJSONObject("hearingArrangements").get("hearingLoop"));
        assertEquals("No", roboticsJson.getJSONObject("hearingArrangements").get("accessibleHearingRoom"));
        assertEquals("2018-06-30", roboticsJson.getJSONObject("hearingArrangements").getJSONArray("datesCantAttend").get(0));
    }

    @Test
    public void givenHearingArrangementIsNull_whenEmptyExcludedDate() {
        DateRange dateRange1 = DateRange.builder()
            .start("2018-06-30")
            .end("2018-06-30")
            .build();
        DateRange dateRange2 = DateRange.builder()
            .start("")
            .end("")
            .build();
        List<ExcludeDate> excludeDates = new ArrayList<>();
        excludeDates.add(ExcludeDate.builder()
            .value(dateRange1)
            .build());
        excludeDates.add(ExcludeDate.builder()
            .value(dateRange2)
            .build());
        appeal.getSscsCaseData().getAppeal().getHearingOptions().setArrangements(null);
        appeal.getSscsCaseData().getAppeal().getHearingOptions().setExcludeDates(excludeDates);

        roboticsJson = roboticsJsonMapper.map(appeal);

        assertEquals("No", roboticsJson.getJSONObject("hearingArrangements").get("hearingLoop"));
        assertEquals("No", roboticsJson.getJSONObject("hearingArrangements").get("accessibleHearingRoom"));
        assertEquals(1, roboticsJson.getJSONObject("hearingArrangements").getJSONArray("datesCantAttend").length());
        assertEquals("2018-06-30", roboticsJson.getJSONObject("hearingArrangements").getJSONArray("datesCantAttend").get(0));
    }

    @Test
    public void givenLanguageInterpreterIsFalse_thenDoNotSetLanguageInterpreter() {
        appeal.getSscsCaseData().getAppeal().getHearingOptions().setLanguages("My Language");
        appeal.getSscsCaseData().getAppeal().getHearingOptions().setLanguageInterpreter("No");

        roboticsJson = roboticsJsonMapper.map(appeal);

        assertFalse(roboticsJson.getJSONObject("hearingArrangements").has("languageInterpreter"));
    }

    @Test
    public void givenLanguageInterpreterIsTrueAndInterpreterLanguageTypeIsNull_thenDoNotSetLanguageInterpreter() {
        appeal.getSscsCaseData().getAppeal().getHearingOptions().setLanguages(null);
        appeal.getSscsCaseData().getAppeal().getHearingOptions().setLanguageInterpreter("Yes");

        roboticsJson = roboticsJsonMapper.map(appeal);

        assertFalse(roboticsJson.getJSONObject("hearingArrangements").has("languageInterpreter"));
    }

    @Test
    public void givenSignLanguageInterpreterIsTrue_thenSetToSignLanguageInterpreterType() {
        appeal.getSscsCaseData().getAppeal().getHearingOptions().setSignLanguageType("My Language");
        List<String> arrangements = new ArrayList<>();
        arrangements.add("signLanguageInterpreter");
        appeal.getSscsCaseData().getAppeal().getHearingOptions().setArrangements(arrangements);

        roboticsJson = roboticsJsonMapper.map(appeal);

        assertEquals("My Language", roboticsJson.getJSONObject("hearingArrangements").get("signLanguageInterpreter"));
    }

    @Test
    public void givenSignLanguageInterpreterIsFalse_thenDoNotSetSignLanguageInterpreter() {
        appeal.getSscsCaseData().getAppeal().getHearingOptions().setSignLanguageType("My Language");
        List<String> arrangements = new ArrayList<>();
        appeal.getSscsCaseData().getAppeal().getHearingOptions().setArrangements(arrangements);

        roboticsJson = roboticsJsonMapper.map(appeal);

        assertFalse(roboticsJson.getJSONObject("hearingArrangements").has("signLanguageInterpreter"));
    }

    @Test
    public void givenSignLanguageInterpreterIsTrueAndSignInterpreterLanguageTypeIsNull_thenDoNotSetSignLanguageInterpreter() {
        appeal.getSscsCaseData().getAppeal().getHearingOptions().setSignLanguageType(null);
        List<String> arrangements = new ArrayList<>();
        arrangements.add("signLanguageInterpreter");
        appeal.getSscsCaseData().getAppeal().getHearingOptions().setArrangements(arrangements);

        roboticsJson = roboticsJsonMapper.map(appeal);

        assertFalse(roboticsJson.getJSONObject("hearingArrangements").has("signLanguageInterpreter"));
    }

    @Test
    public void givenCcdCaseIdIsNull_thenDoNotSetCcdCaseId() {
        appeal.setCcdCaseId(null);

        roboticsJson = roboticsJsonMapper.map(appeal);

        roboticsJsonValidator.validate(roboticsJson);

        assertFalse(roboticsJson.has("caseId"));
    }

    @Test
    public void givenAMissingRepresentative_thenProcessRobotics() {
        appeal.getSscsCaseData().getAppeal().getRep().setAddress(null);
        appeal.getSscsCaseData().getAppeal().getRep().setName(null);
        appeal.getSscsCaseData().getAppeal().getRep().setOrganisation(null);
        appeal.getSscsCaseData().getAppeal().getRep().setHasRepresentative("No");

        roboticsJson = roboticsJsonMapper.map(appeal);

        assertFalse(roboticsJson.has("representative"));
    }

    @Test
    public void givenAnAppointee_thenProcessRobotics() {
        Name appointeeName = Name.builder().title("Mrs").firstName("Ap").lastName("Pointee").build();

        Appointee appointee = Appointee.builder()
            .name(appointeeName)
            .address(appeal.getSscsCaseData().getAppeal().getAppellant().getAddress())
            .contact(appeal.getSscsCaseData().getAppeal().getAppellant().getContact())
            .build();

        appeal.getSscsCaseData().getAppeal().getAppellant().setIsAddressSameAsAppointee("Yes");

        appeal.getSscsCaseData().getAppeal().getAppellant().setAppointee(appointee);

        roboticsJson = roboticsJsonMapper.map(appeal);

        assertTrue(roboticsJson.has("appointee"));
        assertEquals("Yes", roboticsJson.getJSONObject("appointee").getString("sameAddressAsAppellant"));
    }

    @Test
    public void givenAnAppointeeWithEmptyDetails_thenProcessRobotics() {
        Name appointeeName = Name.builder().title(null).firstName(null).lastName(null).build();

        Appointee appointee = Appointee.builder()
            .name(appointeeName)
            .address(Address.builder().line1(null).line2(null).town(null).county(null).postcode(null).build())
            .contact(Contact.builder().email(null).phone(null).mobile(null).build())
            .identity(Identity.builder().dob(null).nino(null).build())
            .build();

        appeal.getSscsCaseData().getAppeal().getAppellant().setAppointee(appointee);

        roboticsJson = roboticsJsonMapper.map(appeal);

        assertFalse(roboticsJson.has("appointee"));
    }


    @Test
    public void givenNoAppointee_thenProcessRobotics() {
        appeal.getSscsCaseData().getAppeal().getAppellant().setAppointee(null);

        roboticsJson = roboticsJsonMapper.map(appeal);

        assertFalse(roboticsJson.has("appointee"));
    }

    @Test
    public void givenCaseCreatedDate_thenAppealDateShouldGetUpdated() {
        String caseCreatedDate = "2019-08-01";

        appeal.getSscsCaseData().setCaseCreated(caseCreatedDate);

        roboticsJson = roboticsJsonMapper.map(appeal);

        assertTrue(roboticsJson.has("appealDate"));

        assertEquals(caseCreatedDate, roboticsJson.get("appealDate"));
    }

    @Test
    public void givenCaseCreatedDateIsNull_thenAppealDateShouldBeCurrentDate() {
        appeal.getSscsCaseData().setCaseCreated(null);

        roboticsJson = roboticsJsonMapper.map(appeal);

        assertTrue(roboticsJson.has("appealDate"));

        assertEquals(LocalDate.now().toString(), roboticsJson.get("appealDate"));
    }

}
