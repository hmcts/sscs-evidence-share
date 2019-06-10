package uk.gov.hmcts.reform.sscs.functional;

import static io.restassured.RestAssured.baseURI;
import static org.slf4j.LoggerFactory.getLogger;

import helper.EnvironmentProfileValueSource;
import io.restassured.RestAssured;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import junitparams.JUnitParamsRunner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.ProfileValueSourceConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@ProfileValueSourceConfiguration(EnvironmentProfileValueSource.class)
public abstract class AbstractFunctionalTest {

    private static final Logger log = getLogger(AuthorisationService.class);

    // Below rules are needed to use the junitParamsRunner together with SpringRunner
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();
    //end of rules needed for junitParamsRunner

    @Autowired
    private IdamService idamService;

    private IdamTokens idamTokens;

    @Autowired
    protected CcdService ccdService;

    protected String ccdCaseId;

    private final String tcaInstance = System.getenv("TEST_URL");
    private final String localInstance = "http://localhost:8091";

    @Before
    public void setup() {
        baseURI = StringUtils.isNotBlank(tcaInstance) ? tcaInstance : localInstance;
        idamTokens = idamService.getIdamTokens();
    }

    protected void createCaseInSendingToDwpState() {
        SscsCaseData minimalCaseData = CaseDataUtils.buildMinimalCaseData();
        SscsCaseData caseData = minimalCaseData.toBuilder().appeal(minimalCaseData.getAppeal().toBuilder()
            .benefitType(BenefitType.builder().code("PIP").description("Personal Independence Payment").build())

            .receivedVia("Paper")
            .build()).build();
        SscsCaseDetails caseDetails = ccdService.createCase(caseData, "validAppealCreated",
            "Evidence share service send to DWP test", "Evidence share service send to DWP case created", idamTokens);
        ccdCaseId = String.valueOf(caseDetails.getId());
    }


    protected SscsCaseDetails findCaseById(String ccdCaseId) {
        return ccdService.getByCaseId(Long.valueOf(ccdCaseId), idamTokens);
    }

    protected String getJson(EventType eventType) throws IOException {
        String resource = eventType.getCcdType()  + "Callback.json";
        String file = getClass().getClassLoader().getResource(resource).getFile();
        return FileUtils.readFileToString(new File(file), StandardCharsets.UTF_8.name());
    }

    public void simulateCcdCallback(String json) throws IOException {
        final String callbackUrl = baseURI + "/send";

        RestAssured.useRelaxedHTTPSValidation();
        RestAssured
                .given()
                .header("ServiceAuthorization", "" + idamTokens.getServiceAuthorization())
                .contentType("application/json")
                .body(json)
                .when()
                .post(callbackUrl)
                .then()
                .statusCode(HttpStatus.OK.value());
    }
}
