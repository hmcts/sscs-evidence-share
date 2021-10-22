package uk.gov.hmcts.reform.sscs.functional;

import static io.restassured.RestAssured.baseURI;
import static java.util.Collections.singletonList;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.REISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;

import com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.RandomStringUtils;
import helper.EnvironmentProfileValueSource;
import io.restassured.RestAssured;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import junitparams.JUnitParamsRunner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.domain.pdf.ByteArrayMultipartFile;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@ProfileValueSourceConfiguration(EnvironmentProfileValueSource.class)
public abstract class AbstractFunctionalTest {

    private static final Logger log = getLogger(AuthorisationService.class);
    private static final String EVIDENCE_DOCUMENT_PDF = "evidence-document.pdf";
    private static final String EXISTING_DOCUMENT_PDF = "existing-document.pdf";

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
    private CcdService ccdService;

    @Autowired
    private EvidenceManagementService evidenceManagementService;

    String ccdCaseId;

    private final String tcaInstance = System.getenv("TEST_URL");
    private final String localInstance = "http://localhost:8091";

    void createNonDigitalCaseWithEvent(EventType eventType) {
        createCaseWithState(eventType, "PIP", "Personal Independence Payment", State.VALID_APPEAL.getId());
    }

    void createDigitalCaseWithEvent(EventType eventType) {
        createCaseWithState(eventType, "PIP", "Personal Independence Payment", State.READY_TO_LIST.getId());
    }


    SscsCaseDetails createCaseWithState(EventType eventType, String benefitType, String benefitDescription, String createdInGapsFrom) {
        idamTokens = idamService.getIdamTokens();

        SscsCaseData minimalCaseData = CaseDataUtils.buildMinimalCaseData();

        SscsCaseData caseData = minimalCaseData.toBuilder()
            .createdInGapsFrom(createdInGapsFrom)
            .appeal(minimalCaseData.getAppeal().toBuilder()
                .benefitType(BenefitType.builder()
                    .code(benefitType)
                    .description(benefitDescription)
                    .build())
                .receivedVia("Paper")
                .build())
            .build();


        SscsCaseDetails caseDetails = ccdService.createCase(caseData, eventType.getCcdType(),
            "Evidence share service created case",
            "Evidence share service case created for functional test", idamTokens);
        ccdCaseId = String.valueOf(caseDetails.getId());
        return caseDetails;
    }

    void updateCaseEvent(EventType eventType, SscsCaseDetails caseDetails) {
        idamTokens = idamService.getIdamTokens();

        ccdService.updateCase(caseDetails.getData(), caseDetails.getId(),
            eventType.getCcdType(), "Evidence share update case test",
            "Evidence share service pushed case update for functional test", idamService.getIdamTokens());
    }


    SscsCaseDetails findCaseById(String ccdCaseId) {
        return ccdService.getByCaseId(Long.valueOf(ccdCaseId), idamTokens);
    }

    String getJson(String fileName) throws IOException {
        String resource = fileName + "Callback.json";
        String file = Objects.requireNonNull(getClass().getClassLoader().getResource(resource)).getFile();
        return FileUtils.readFileToString(new File(file), StandardCharsets.UTF_8.name());
    }

    public static String getRandomNino() {
        return RandomStringUtils.random(9, true, true).toUpperCase();
    }

    public void simulateCcdCallback(String json) {

        baseURI = StringUtils.isNotBlank(tcaInstance) ? tcaInstance : localInstance;

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

    protected String createTestData(String fileName) throws IOException {
        String docUrl = uploadDocToDocMgmtStore(EVIDENCE_DOCUMENT_PDF);
        String existingDocUrl = uploadDocToDocMgmtStore(EXISTING_DOCUMENT_PDF);
        createDigitalCaseWithEvent(VALID_APPEAL_CREATED);
        String json = getJson(fileName);
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("EVIDENCE_DOCUMENT_URL_PLACEHOLDER", docUrl);
        json = json.replace("EXISTING_DOCUMENT_URL_PLACEHOLDER", existingDocUrl);
        json = json.replace("CREATED_IN_GAPS_FROM", State.READY_TO_LIST.getId());
        json = json.replaceAll("NINO_TO_BE_REPLACED", getRandomNino());
        json = json.replace("EVIDENCE_DOCUMENT_BINARY_URL_PLACEHOLDER", docUrl + "/binary");
        return json.replace("EXISTING_DOCUMENT_BINARY_URL_PLACEHOLDER", existingDocUrl + "/binary");
    }

    private String uploadDocToDocMgmtStore(String name) throws IOException {
        Path evidencePath = new File(Objects.requireNonNull(
            getClass().getClassLoader().getResource(name)).getFile()).toPath();

        ByteArrayMultipartFile file = ByteArrayMultipartFile.builder()
            .content(Files.readAllBytes(evidencePath))
            .name(name)
            .contentType(APPLICATION_PDF)
            .build();

        UploadResponse upload = evidenceManagementService.upload(singletonList(file), "sscs");

        return upload.getEmbedded().getDocuments().get(0).links.self.href;
    }
}
