package uk.gov.hmcts.reform.sscs.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import junitparams.JUnitParamsRunner;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;


@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:config/application_it.properties")
public class DocmosisTemplateConfigTest {

    // Below rules are needed to use the junitParamsRunner together with SpringRunner
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();
    //end of rules needed for junitParamsRunner

    @Autowired
    private DocmosisTemplateConfig docmosisTemplateConfig;

    @Test
    public void docmosisTemplate() {
        assertEquals("TB-SCS-GNO-ENG-00010.doc",
                docmosisTemplateConfig.getTemplate().get(LanguagePreference.ENGLISH).get(DocumentType.DL6).get("name"));
        assertEquals("TB-SCS-GNO-WEL-00469.docx",
                docmosisTemplateConfig.getTemplate().get(LanguagePreference.WELSH).get(DocumentType.D609_97).get(
                        "name"));

    }
}
