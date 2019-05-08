package uk.gov.hmcts.reform.sscs.functional;

import static io.restassured.RestAssured.baseURI;
import static org.slf4j.LoggerFactory.getLogger;

import helper.EnvironmentProfileValueSource;
import junitparams.JUnitParamsRunner;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.ProfileValueSourceConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
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

    protected SscsCaseData createCase(String mrnDate) {
        SscsCaseData minimalCaseData = CaseDataUtils.buildMinimalCaseData();
        SscsCaseData caseData = minimalCaseData.toBuilder().appeal(minimalCaseData.getAppeal().toBuilder()
            .benefitType(BenefitType.builder().code("PIP").description("Personal Independence Payment").build())
            .mrnDetails(MrnDetails.builder().mrnDate(mrnDate).build())
            .receivedVia("Paper")
            .build()).build();
        SscsCaseDetails caseDetails = ccdService.createCase(caseData, "appealCreated",
            "Evidence share service appeal created", "Evidence share service appeal created in test", idamTokens);
        ccdCaseId = String.valueOf(caseDetails.getId());
        return caseData;
    }

    protected void sendToDwpEvent(SscsCaseData caseData) {
        ccdService.updateCase(caseData, Long.valueOf(ccdCaseId), "sendToDwp",
            "Test send to DWP", "Testing sending to DWP", idamTokens);
    }

    protected SscsCaseDetails findCaseById(String ccdCaseId) {
        return ccdService.getByCaseId(Long.valueOf(ccdCaseId), idamTokens);
    }
}
