package uk.gov.hmcts.reform.sscs.robotics.json;

import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.robotics.exception.RoboticsValidationException;

@Component
@Slf4j
public class RoboticsJsonValidator {

    private final String schemaResourceLocation;
    private Schema schema = null;

    @Autowired
    public RoboticsJsonValidator(@Value("${robotics.schema.resource.location}") String schemaResourceLocation) {
        this.schemaResourceLocation = schemaResourceLocation;
    }

    public void validate(JSONObject roboticsJson) {

        tryLoadSchema();

        try {
            schema.validate(roboticsJson);
        } catch (ValidationException validationException) {
            log.error("Robotics service failed to validate json", validationException);
            throw new RoboticsValidationException(validationException);
        }
    }

    private synchronized void tryLoadSchema() {

        if (schema != null) {
            return;
        }

        InputStream inputStream = getClass().getResourceAsStream(schemaResourceLocation);
        schema = SchemaLoader.load(new JSONObject(new JSONTokener(inputStream)));
    }
}
