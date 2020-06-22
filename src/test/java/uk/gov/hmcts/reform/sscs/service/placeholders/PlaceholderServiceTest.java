package uk.gov.hmcts.reform.sscs.service.placeholders;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.APPELLANT_FULL_NAME_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.BENEFIT_TYPE_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.CASE_CREATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.CASE_ID_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.EXELA_ADDRESS_LINE1_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.EXELA_ADDRESS_LINE2_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.EXELA_ADDRESS_LINE3_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.GENERATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.NINO_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.RECIPIENT_ADDRESS_LINE_1_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.RECIPIENT_ADDRESS_LINE_2_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.RECIPIENT_ADDRESS_LINE_3_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.RECIPIENT_ADDRESS_LINE_4_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.RECIPIENT_ADDRESS_LINE_5_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_COUNTY_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_POSTCODE_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.SC_NUMBER_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.SSCS_URL_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.WELSH_CASE_CREATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderHelper.buildCaseData;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderHelper.buildCaseDataWithoutBenefitType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.joda.time.DateTimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.ExelaAddressConfig;
import uk.gov.hmcts.reform.sscs.docmosis.config.PdfDocumentConfig;
import uk.gov.hmcts.reform.sscs.service.conversion.LocalDateToWelshStringConverter;

@Service
public class PlaceholderServiceTest {

    PlaceholderService service;

    private SscsCaseData caseData;

    private String now;
    private String welshDate;

    @Mock
    private PdfDocumentConfig pdfDocumentConfig;

    @Mock
    private ExelaAddressConfig exelaAddressConfig;

    @Mock
    private LocalDateToWelshStringConverter localDateToWelshStringConverter;

    Map<String, Object> placeholders;

    @Before
    public void setup() {
        initMocks(this);
        DateTimeUtils.setCurrentMillisFixed(1550000000000L);

        now = (DateTimeFormatter.ISO_LOCAL_DATE).format(LocalDateTime.now());
        welshDate = "2001-12-02";
        caseData = buildCaseData();
        service = new PlaceholderService(pdfDocumentConfig, exelaAddressConfig,localDateToWelshStringConverter);
        placeholders = new HashMap<>();
        when(localDateToWelshStringConverter.convert(welshDate)).thenReturn("2 Rhagfyr 2012");
        when(localDateToWelshStringConverter.convert("2001-12-02")).thenReturn("2 Rhagfyr 2012");

        given(pdfDocumentConfig.getHmctsWelshImgKey()).willReturn("hmctsWelshImgKey");
        given(pdfDocumentConfig.getHmctsWelshImgVal()).willReturn("welshhmcts.png");
        given(pdfDocumentConfig.getHmctsImgKey()).willReturn("hmctsKey");
        given(exelaAddressConfig.getAddressLine1()).willReturn("Line 1");
        given(exelaAddressConfig.getAddressLine2()).willReturn("Line 2");
        given(exelaAddressConfig.getAddressLine3()).willReturn("Line 3");
        given(exelaAddressConfig.getAddressPostcode()).willReturn("Postcode");
    }

    @Test
    public void givenACase_thenPopulateThePlaceholders() {
        Address address = Address.builder()
            .line1("Unit 2")
            .line2("156 The Road")
            .town("Lechworth")
            .county("Bedford")
            .postcode("L2 5UZ").build();

        service.build(caseData, placeholders, address, now);

        assertEquals("HM Courts & Tribunals Service", placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL));
        assertEquals("Social Security & Child Support Appeals", placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL));
        assertEquals("Prudential Buildings", placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL));
        assertEquals("36 Dale Street", placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL));
        assertEquals("LIVERPOOL", placeholders.get(REGIONAL_OFFICE_COUNTY_LITERAL));
        assertEquals("L2 5UZ", placeholders.get(REGIONAL_OFFICE_POSTCODE_LITERAL));
        assertEquals(now, placeholders.get(GENERATED_DATE_LITERAL));
        assertEquals(now, placeholders.get(CASE_CREATED_DATE_LITERAL));
        assertEquals("Mr T Tibbs", placeholders.get(APPELLANT_FULL_NAME_LITERAL));
        assertEquals("PERSONAL INDEPENDENCE PAYMENT", placeholders.get(BENEFIT_TYPE_LITERAL));
        assertEquals("123456", placeholders.get(CASE_ID_LITERAL));
        assertEquals("JT0123456B", placeholders.get(NINO_LITERAL));
        assertEquals("https://www.gov.uk/appeal-benefit-decision", placeholders.get(SSCS_URL_LITERAL));
        assertEquals("Line 1", placeholders.get(EXELA_ADDRESS_LINE1_LITERAL));
        assertEquals("Line 2", placeholders.get(EXELA_ADDRESS_LINE2_LITERAL));
        assertEquals("Line 3", placeholders.get(EXELA_ADDRESS_LINE3_LITERAL));
        assertEquals("Unit 2", placeholders.get(RECIPIENT_ADDRESS_LINE_1_LITERAL));
        assertEquals("156 The Road", placeholders.get(RECIPIENT_ADDRESS_LINE_2_LITERAL));
        assertEquals("Lechworth", placeholders.get(RECIPIENT_ADDRESS_LINE_3_LITERAL));
        assertEquals("Bedford", placeholders.get(RECIPIENT_ADDRESS_LINE_4_LITERAL));
        assertEquals("L2 5UZ", placeholders.get(RECIPIENT_ADDRESS_LINE_5_LITERAL));
        assertEquals("SC123/12/1234", placeholders.get(SC_NUMBER_LITERAL));
    }

    @Test
    public void givenACase_thenPopulateThePlaceholdersWithBenefitTypeEmpty() {
        Address address = Address.builder()
                .line1("Unit 2")
                .line2("156 The Road")
                .town("Lechworth")
                .county("Bedford")
                .postcode("L2 5UZ").build();

        caseData.getAppeal().setBenefitType(BenefitType.builder().code("PIP").description(null).build());
        service.build(caseData, placeholders, address, now);

        assertEquals("PERSONAL INDEPENDENCE PAYMENT", placeholders.get(BENEFIT_TYPE_LITERAL));
    }

    @Test
    public void givenACase_thenPopulateThePlaceholdersWithBenefitTypeDescriptionEmpty() {
        Address address = Address.builder()
            .line1("Unit 2")
            .line2("156 The Road")
            .town("Lechworth")
            .county("Bedford")
            .postcode("L2 5UZ").build();

        caseData = buildCaseDataWithoutBenefitType();
        service.build(caseData, placeholders, address, now);

        assertEquals("HM Courts & Tribunals Service", placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL));
        assertEquals("Social Security & Child Support Appeals", placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL));
        assertEquals("Prudential Buildings", placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL));
        assertEquals("36 Dale Street", placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL));
        assertEquals("LIVERPOOL", placeholders.get(REGIONAL_OFFICE_COUNTY_LITERAL));
        assertEquals("L2 5UZ", placeholders.get(REGIONAL_OFFICE_POSTCODE_LITERAL));
        assertEquals(now, placeholders.get(GENERATED_DATE_LITERAL));
        assertEquals(now, placeholders.get(CASE_CREATED_DATE_LITERAL));
        assertEquals("Mr T Tibbs", placeholders.get(APPELLANT_FULL_NAME_LITERAL));
        assertEquals("", placeholders.get(BENEFIT_TYPE_LITERAL));
        assertEquals("123456", placeholders.get(CASE_ID_LITERAL));
        assertEquals("JT0123456B", placeholders.get(NINO_LITERAL));
        assertEquals("https://www.gov.uk/appeal-benefit-decision", placeholders.get(SSCS_URL_LITERAL));
        assertEquals("Line 1", placeholders.get(EXELA_ADDRESS_LINE1_LITERAL));
        assertEquals("Line 2", placeholders.get(EXELA_ADDRESS_LINE2_LITERAL));
        assertEquals("Line 3", placeholders.get(EXELA_ADDRESS_LINE3_LITERAL));
        assertEquals("Unit 2", placeholders.get(RECIPIENT_ADDRESS_LINE_1_LITERAL));
        assertEquals("156 The Road", placeholders.get(RECIPIENT_ADDRESS_LINE_2_LITERAL));
        assertEquals("Lechworth", placeholders.get(RECIPIENT_ADDRESS_LINE_3_LITERAL));
        assertEquals("Bedford", placeholders.get(RECIPIENT_ADDRESS_LINE_4_LITERAL));
        assertEquals("L2 5UZ", placeholders.get(RECIPIENT_ADDRESS_LINE_5_LITERAL));
        assertEquals("SC123/12/1234", placeholders.get(SC_NUMBER_LITERAL));
    }

    @Test
    public void givenARecipientAddressWith4Lines_thenPopulateThePlaceholders() {
        Address address = Address.builder()
            .line1("Unit 2")
            .town("Lechworth")
            .county("Bedford")
            .postcode("L2 5UZ").build();

        service.build(caseData, placeholders, address, now);

        assertEquals("Unit 2", placeholders.get(RECIPIENT_ADDRESS_LINE_1_LITERAL));
        assertEquals("Lechworth", placeholders.get(RECIPIENT_ADDRESS_LINE_2_LITERAL));
        assertEquals("Bedford", placeholders.get(RECIPIENT_ADDRESS_LINE_3_LITERAL));
        assertEquals("L2 5UZ", placeholders.get(RECIPIENT_ADDRESS_LINE_4_LITERAL));
    }

    @Test
    public void givenAnAppellantWithALongNameAndAddressExceeding45Characters_thenGenerateThePlaceholdersWithTruncatedName() {
        Address address = Address.builder()
                .line1("MyFirstVeryVeryLongAddressLineWithLotsOfCharacters")
                .line2("MySecondVeryVeryLongAddressLineWithLotsOfCharacters")
                .town("MyTownVeryVeryLongAddressLineWithLotsOfCharacters")
                .county("MyCountyVeryVeryLongAddressLineWithLotsOfCharacters")
                .postcode("L2 5UZ").build();

        service.build(caseData, placeholders, address, now);

        assertEquals("MyFirstVeryVeryLongAddressLineWithLotsOfChara", placeholders.get(RECIPIENT_ADDRESS_LINE_1_LITERAL));
        assertEquals("MySecondVeryVeryLongAddressLineWithLotsOfChar", placeholders.get(RECIPIENT_ADDRESS_LINE_2_LITERAL));
        assertEquals("MyTownVeryVeryLongAddressLineWithLotsOfCharac", placeholders.get(RECIPIENT_ADDRESS_LINE_3_LITERAL));
        assertEquals("MyCountyVeryVeryLongAddressLineWithLotsOfChar", placeholders.get(RECIPIENT_ADDRESS_LINE_4_LITERAL));
        assertEquals("L2 5UZ", placeholders.get(RECIPIENT_ADDRESS_LINE_5_LITERAL));

    }

    @Test
    public void givenARecipientAddressWith3Lines_thenPopulateThePlaceholders() {
        Address address = Address.builder()
            .line1("Unit 2")
            .county("Bedford")
            .postcode("L2 5UZ").build();

        service.build(caseData, placeholders, address, now);

        assertEquals("Unit 2", placeholders.get(RECIPIENT_ADDRESS_LINE_1_LITERAL));
        assertEquals("Bedford", placeholders.get(RECIPIENT_ADDRESS_LINE_2_LITERAL));
        assertEquals("L2 5UZ", placeholders.get(RECIPIENT_ADDRESS_LINE_3_LITERAL));
    }

    @Test
    public void givenALanguagePreferenceIsWelsh_ThenPickWelshLogo() {
        Address address = Address.builder()
            .line1("Unit 2")
            .line2("156 The Road")
            .town("Lechworth")
            .county("Bedford")
            .postcode("L2 5UZ").build();
        caseData.setLanguagePreferenceWelsh("Yes");

        service.build(caseData, placeholders, address, welshDate);

        assertEquals("HM Courts & Tribunals Service", placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL));
        assertEquals("welshhmcts.png", placeholders.get("hmctsWelshImgKey"));
        assertEquals("2 Rhagfyr 2012", placeholders.get(WELSH_CASE_CREATED_DATE_LITERAL));
    }
}
