package uk.gov.hmcts.reform.sscs.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertNotNull;

import junitparams.JUnitParamsRunner;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest
public class EvidenceShareConfigTest {
    // Below rules are needed to use the junitParamsRunner together with SpringRunner
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();
    //end of rules needed for junitParamsRunner

    @Autowired
    private EvidenceShareConfig evidenceShareConfig;

    @Test
    public void submitTypeContainsPaperAndAllowedBenefitTypesContainsPip() {
        assertNotNull("evidenceShareConfig must be autowired", evidenceShareConfig);
        assertThat(evidenceShareConfig.getSubmitTypes(), contains("PAPER"));
        assertThat(evidenceShareConfig.getAllowedBenefitTypes(), contains("PIP"));
    }

}
