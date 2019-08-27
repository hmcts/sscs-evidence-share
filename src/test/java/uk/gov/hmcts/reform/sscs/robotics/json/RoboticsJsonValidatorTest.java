package uk.gov.hmcts.reform.sscs.robotics.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.robotics.exception.RoboticsValidationException;

@RunWith(JUnitParamsRunner.class)
public class RoboticsJsonValidatorTest {

    private JSONObject jsonData = new JSONObject(
            new JSONTokener(getClass().getResourceAsStream("/schema/valid_robotics_agreed.json")));

    private RoboticsJsonValidator roboticsJsonValidator = new RoboticsJsonValidator(
            "/schema/sscs-robotics.json");

    @Test
    public void givenValidInputAgreedWithAutomationTeam_thenValidateAgainstSchema() throws ValidationException {
        roboticsJsonValidator.validate(jsonData);
    }

    @Test
    @Parameters({"appellant", "appointee"})
    public void givenRoboticJsonWithDobForAppellantAndAppointee_shouldValidateSuccessfully(String person) throws IOException {
        jsonData = updateEmbeddedProperty(jsonData.toString(), "2018-08-12", person, "dob");
        roboticsJsonValidator.validate(jsonData);
    }

    @Test
    public void givenRoboticJsonWithNoDobForAppellantOrAppointee_shouldValidateSuccessfully() {
        jsonData.getJSONObject("appellant").remove("dob");
        jsonData.getJSONObject("appointee").remove("dob");
        roboticsJsonValidator.validate(jsonData);
    }

    @Test
    @Parameters({"Yes", "No"})
    public void givenJsonWithTheSameAddressAsAppellantProperty_shouldValidateSuccessfully(String value) {
        jsonData.getJSONObject("appointee").put("sameAddressAsAppellant", value);
        roboticsJsonValidator.validate(jsonData);
    }

    @Test
    public void givenJsonWithNoTheSameAddressAsAppellantProp_shouldValidateSuccessfully() {
        jsonData.getJSONObject("appointee").remove("sameAddressAsAppellant");
        roboticsJsonValidator.validate(jsonData);
    }

    @Test(expected = RoboticsValidationException.class)
    public void givenInvalidInputForCaseCode_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData = updateEmbeddedProperty(jsonData.toString(), "002CC", "caseCode");
        roboticsJsonValidator.validate(jsonData);
    }

    @Test(expected = RoboticsValidationException.class)
    public void givenInvalidInputForPostCode_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData = updateEmbeddedProperty(jsonData.toString(), "B231ABXXX", "appellant", "postCode");
        roboticsJsonValidator.validate(jsonData);
    }

    @Test(expected = RoboticsValidationException.class)
    public void givenInvalidInputForCaseCreatedDate_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData = updateEmbeddedProperty(jsonData.toString(), "2018/06/01", "caseCreatedDate");
        roboticsJsonValidator.validate(jsonData);
    }

    @Test(expected = RoboticsValidationException.class)
    public void givenInvalidInputForMrnDate_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData = updateEmbeddedProperty(jsonData.toString(), "2018/06/02", "mrnDate");
        roboticsJsonValidator.validate(jsonData);
    }

    @Test(expected = RoboticsValidationException.class)
    public void givenInvalidInputForAppealDate_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData = updateEmbeddedProperty(jsonData.toString(), "2018/06/03", "appealDate");
        roboticsJsonValidator.validate(jsonData);
    }

    @Test(expected = RoboticsValidationException.class)
    public void givenInvalidInputForHearingType_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData = updateEmbeddedProperty(jsonData.toString(), "Computer", "hearingType");
        roboticsJsonValidator.validate(jsonData);
    }

    @Test(expected = RoboticsValidationException.class)
    public void givenOralInputForLanguageInterpreterWithNoHearingRequestParty_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData = updateEmbeddedProperty(jsonData.toString(), "Oral", "hearingType");
        jsonData = removeProperty(jsonData.toString(), "hearingRequestParty");
        roboticsJsonValidator.validate(jsonData);
    }

    @Test
    public void givenPaperInputForLanguageInterpreterWithNoHearingRequestParty_doesNotThrowExceptionWhenValidatingAgainstSchema() throws IOException {
        jsonData = updateEmbeddedProperty(jsonData.toString(), "Paper", "hearingType");
        jsonData = removeProperty(jsonData.toString(), "hearingRequestParty");
        roboticsJsonValidator.validate(jsonData);
    }

    @Test(expected = RoboticsValidationException.class)
    public void givenInvalidInputForWantsToAttendHearing_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData = updateEmbeddedProperty(jsonData.toString(), "Bla", "wantsToAttendHearing");
        roboticsJsonValidator.validate(jsonData);
    }

    @Test(expected = RoboticsValidationException.class)
    public void givenInvalidInputForHearingLoop_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData = updateEmbeddedProperty(jsonData.toString(), "Bla", "hearingArrangements", "hearingLoop");
        roboticsJsonValidator.validate(jsonData);
    }

    @Test(expected = RoboticsValidationException.class)
    public void givenInvalidInputForAccessibleHearingRoom_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData = updateEmbeddedProperty(jsonData.toString(), "Bla", "hearingArrangements", "accessibleHearingRoom");
        roboticsJsonValidator.validate(jsonData);
    }

    @Test(expected = RoboticsValidationException.class)
    public void givenInvalidInputForDisabilityAccess_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData = updateEmbeddedProperty(jsonData.toString(), "Bla", "hearingArrangements", "disabilityAccess");
        roboticsJsonValidator.validate(jsonData);
    }

    @Test(expected = RoboticsValidationException.class)
    public void givenInvalidInputForHearingDatesCantAttend_throwExceptionWhenValidatingAgainstSchema() throws ValidationException {
        jsonData = new JSONObject(jsonData.toString().replace("2018-08-12", "2018/08/12"));
        roboticsJsonValidator.validate(jsonData);
    }

    private static JSONObject updateEmbeddedProperty(String json, String value, String... keys) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        Map<Object, String> map = objectMapper.readValue(json, Map.class);

        Map t = map;
        for (int i = 0; i < keys.length - 1; i++) {
            t = (Map) t.get(keys[i]);
        }

        t.put(keys[keys.length - 1], value);

        JSONObject jsonObject = new JSONObject(objectMapper.writeValueAsString(map));

        return jsonObject;
    }

    private static JSONObject removeProperty(String json, String key) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        Map map = objectMapper.readValue(json, Map.class);

        map.remove(key);

        String jsonString = objectMapper.writeValueAsString(map);
        JSONObject jsonObject = new JSONObject(jsonString);

        return jsonObject;
    }

}
