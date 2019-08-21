package uk.gov.hmcts.reform.sscs.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.config.EvidenceShareConfig;
import uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment;
import uk.gov.hmcts.reform.sscs.domain.email.RoboticsEmailTemplate;
import uk.gov.hmcts.reform.sscs.model.AirlookupBenefitToVenue;
import uk.gov.hmcts.reform.sscs.robotics.domain.RoboticsWrapper;
import uk.gov.hmcts.reform.sscs.robotics.json.RoboticsJsonMapper;
import uk.gov.hmcts.reform.sscs.robotics.json.RoboticsJsonValidator;

@RunWith(JUnitParamsRunner.class)
public class RoboticsServiceTest {

    private static final boolean NOT_SCOTTISH = false;

    RoboticsService roboticsService;

    @Mock
    EvidenceManagementService evidenceManagementService;

    @Mock
    RegionalProcessingCenterService regionalProcessingCenterService;

    @Mock
    AirLookupService airLookupService;

    @Mock
    EmailService emailService;

    @Mock
    RoboticsJsonMapper roboticsJsonMapper;

    @Mock
    RoboticsJsonValidator roboticsJsonValidator;

    @Mock
    RoboticsEmailTemplate roboticsEmailTemplate;

    @Mock
    EvidenceShareConfig evidenceShareConfig;

    SscsCcdConvertService convertService;

    LocalDate localDate;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Captor
    private ArgumentCaptor<List<EmailAttachment>> captor;

    @Captor
    private ArgumentCaptor<List<EmailAttachment>> attachmentCaptor;

    @Before
    public void setup() {
        initMocks(this);

        convertService = new SscsCcdConvertService();

        roboticsService = new RoboticsService(regionalProcessingCenterService,
            evidenceManagementService,
            airLookupService,
            emailService,
            roboticsJsonMapper,
            roboticsJsonValidator,
            roboticsEmailTemplate,
            evidenceShareConfig);

        localDate = LocalDate.now();

        given(airLookupService.lookupAirVenueNameByPostCode("CM12")).willReturn(AirlookupBenefitToVenue.builder().pipVenue("Bristol").build());

        JSONObject mappedJson = mock(JSONObject.class);
        given(roboticsJsonMapper.map(any())).willReturn(mappedJson);
        given(evidenceShareConfig.getSubmitTypes()).willReturn(Collections.singletonList("paper"));
    }

    @Test
    public void givenACaseWithCaseCreatedEvent_thenCreateRoboticsFile() {

        Appeal appeal = Appeal.builder().mrnDetails(MrnDetails.builder().mrnDate(localDate.format(formatter)).build())
            .benefitType(BenefitType.builder().code("PIP").build())
            .appellant(Appellant.builder().address(
                Address.builder().postcode("CM120HN").build())
            .build()).build();

        SscsCaseData caseData = SscsCaseData.builder().appeal(appeal).ccdCaseId("123").build();

        given(evidenceManagementService.download(any(), eq(null))).willReturn(null);

        given(regionalProcessingCenterService.getFirstHalfOfPostcode("CM120HN")).willReturn("CM12");

        roboticsService.sendCaseToRobotics(caseData);

        verify(emailService).sendEmail(any());
    }

    @Test
    public void givenAnOnlineCaseWithCaseCreatedEventAndEvidenceToDownload_thenCreateRoboticsFileWithDownloadedEvidence() {

        given(regionalProcessingCenterService.getFirstHalfOfPostcode("CM120HN")).willReturn("CM12");

        byte[] expectedBytes = {1, 2, 3};
        given(evidenceManagementService.download(URI.create("www.download.com"), null)).willReturn(expectedBytes);

        Map<String, byte[]> expectedAdditionalEvidence = new HashMap<>();
        expectedAdditionalEvidence.put("test.jpg", expectedBytes);

        Appeal appeal = Appeal.builder().mrnDetails(MrnDetails.builder().mrnDate(localDate.format(formatter)).build())
            .benefitType(BenefitType.builder().code("PIP").build())
            .receivedVia("Online")
            .appellant(Appellant.builder().address(
                Address.builder().postcode("CM120HN").build())
                .build()).build();

        given(emailService.generateUniqueEmailId(appeal.getAppellant())).willReturn("Bloggs_123");

        List<SscsDocument> documents = new ArrayList<>();
        documents.add(SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType("appellantEvidence")
                .documentFileName("test.jpg")
                .documentLink(DocumentLink.builder().documentUrl("www.download.com").build())
                .build())
            .build());

        SscsCaseData caseData = SscsCaseData.builder().appeal(appeal).sscsDocument(documents).ccdCaseId("123").build();

        roboticsService.sendCaseToRobotics(caseData);

        verify(emailService).sendEmail(any());
        verify(roboticsEmailTemplate).generateEmail(eq("Bloggs_123"), attachmentCaptor.capture(), eq(false));

        assertEquals("Bloggs_123.txt", attachmentCaptor.getValue().get(0).getFilename());
        assertEquals("test.jpg", attachmentCaptor.getValue().get(1).getFilename());
    }

    @Test
    public void givenAPaperCaseWithCaseCreatedEventAndEvidenceToDownload_thenCreateRoboticsFileWithNoDownloadedEvidence() {

        given(regionalProcessingCenterService.getFirstHalfOfPostcode("CM120HN")).willReturn("CM12");

        byte[] expectedBytes = {1, 2, 3};
        given(evidenceManagementService.download(URI.create("www.download.com"), null)).willReturn(expectedBytes);

        Map<String, byte[]> expectedAdditionalEvidence = new HashMap<>();
        expectedAdditionalEvidence.put("test.jpg", expectedBytes);

        Appeal appeal = Appeal.builder().mrnDetails(MrnDetails.builder().mrnDate(localDate.format(formatter)).build())
            .benefitType(BenefitType.builder().code("PIP").build())
            .receivedVia("Paper")
            .appellant(Appellant.builder().address(
                Address.builder().postcode("CM120HN").build())
                .build()).build();

        given(emailService.generateUniqueEmailId(appeal.getAppellant())).willReturn("Bloggs_123");

        List<SscsDocument> documents = new ArrayList<>();
        documents.add(SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType("appellantEvidence")
                .documentFileName("test.jpg")
                .documentLink(DocumentLink.builder().documentUrl("www.download.com").build())
                .build())
            .build());

        SscsCaseData caseData = SscsCaseData.builder().appeal(appeal).sscsDocument(documents).ccdCaseId("123").build();

        roboticsService.sendCaseToRobotics(caseData);

        verify(emailService).sendEmail(any());
        verify(roboticsEmailTemplate).generateEmail(eq("Bloggs_123"), attachmentCaptor.capture(), eq(false));

        assertEquals("Bloggs_123.txt", attachmentCaptor.getValue().get(0).getFilename());
        assertEquals(1, attachmentCaptor.getValue().size());
    }

    @Test
    public void createValidRoboticsAndReturnAsJsonObject() {

        RoboticsWrapper appeal =
            RoboticsWrapper
                .builder()
                .sscsCaseData(buildCaseData())
                .ccdCaseId(123L).venueName("Bromley")
                .build();

        JSONObject mappedJson = mock(JSONObject.class);

        given(roboticsJsonMapper.map(appeal)).willReturn(mappedJson);

        JSONObject actualRoboticsJson = roboticsService.createRobotics(appeal);

        then(roboticsJsonMapper).should(times(1)).map(appeal);
        then(roboticsJsonValidator).should(times(1)).validate(mappedJson);

        assertEquals(mappedJson, actualRoboticsJson);
    }

    @Test
    @Parameters({"CARDIFF", "GLASGOW", "", "null"})
    public void generatingRoboticsSendsAnEmail(String rpcName) {

        SscsCaseData appeal = buildCaseData().toBuilder().regionalProcessingCenter(
            buildCaseData().getRegionalProcessingCenter().toBuilder().name(rpcName.equals("null") ? null : rpcName).build()
        ).build();

        JSONObject mappedJson = mock(JSONObject.class);

        given(roboticsJsonMapper.map(any())).willReturn(mappedJson);

        given(airLookupService.lookupAirVenueNameByPostCode("AB12 XYZ")).willReturn(AirlookupBenefitToVenue.builder().pipVenue("Bristol").build());

        given(emailService.generateUniqueEmailId(appeal.getAppeal().getAppellant())).willReturn("Bloggs_123");

        byte[] pdf = {};

        roboticsService.processRobotics(appeal, 123L, "AB12 XYZ", pdf, Collections.emptyMap());

        boolean isScottish = StringUtils.equalsAnyIgnoreCase(rpcName,"GLASGOW");
        verify(roboticsEmailTemplate).generateEmail(eq("Bloggs_123"), captor.capture(), eq(isScottish));
        List<EmailAttachment> attachmentResult = captor.getValue();

        assertThat(attachmentResult.size(), is(2));
        assertThat(attachmentResult.get(0).getFilename(), is("Bloggs_123.txt"));
        assertThat(attachmentResult.get(1).getFilename(), is("Bloggs_123.pdf"));

        verify(roboticsJsonMapper).map(any());
        verify(roboticsJsonValidator).validate(mappedJson);
        verify(emailService).sendEmail(any());
    }

    @Test
    public void generatingRoboticsWithEmptyPdfSendsAnEmail() {

        SscsCaseData appeal = buildCaseData();

        JSONObject mappedJson = mock(JSONObject.class);

        given(roboticsJsonMapper.map(any())).willReturn(mappedJson);

        given(airLookupService.lookupAirVenueNameByPostCode("AB12 XYZ")).willReturn(AirlookupBenefitToVenue.builder().pipVenue("Bristol").build());

        given(emailService.generateUniqueEmailId(appeal.getAppeal().getAppellant())).willReturn("Bloggs_123");

        roboticsService.processRobotics(appeal, 123L, "AB12 XYZ", null, Collections.emptyMap());

        verify(roboticsEmailTemplate).generateEmail(eq("Bloggs_123"), captor.capture(), eq(NOT_SCOTTISH));
        List<EmailAttachment> attachmentResult = captor.getValue();

        assertThat(attachmentResult.size(), is(1));
        assertThat(attachmentResult.get(0).getFilename(), is("Bloggs_123.txt"));
        verify(roboticsJsonMapper).map(any());
        verify(roboticsJsonValidator).validate(mappedJson);
        verify(emailService).sendEmail(any());
    }

    @Test
    public void generatingRoboticsSendsAnEmailWithAdditionalEvidence() {

        SscsCaseData appeal = buildCaseData();

        JSONObject mappedJson = mock(JSONObject.class);

        given(roboticsJsonMapper.map(any())).willReturn(mappedJson);

        given(airLookupService.lookupAirVenueNameByPostCode("AB12 XYZ")).willReturn(AirlookupBenefitToVenue.builder().pipVenue("Bristol").build());

        given(emailService.generateUniqueEmailId(appeal.getAppeal().getAppellant())).willReturn("Bloggs_123");

        byte[] pdf = {};
        byte[] someFile = {};

        roboticsService.processRobotics(appeal, 123L, "AB12 XYZ", pdf, Collections.singletonMap("Some Evidence.doc", someFile));

        verify(roboticsEmailTemplate).generateEmail(eq("Bloggs_123"), captor.capture(), eq(NOT_SCOTTISH));
        List<EmailAttachment> attachmentResult = captor.getValue();

        assertThat(attachmentResult.size(), is(3));
        assertThat(attachmentResult.get(0).getFilename(), is("Bloggs_123.txt"));
        assertThat(attachmentResult.get(1).getFilename(), is("Bloggs_123.pdf"));
        assertThat(attachmentResult.get(2).getFilename(), is("Some Evidence.doc"));

        verify(roboticsJsonMapper).map(any());
        verify(roboticsJsonValidator).validate(mappedJson);
        verify(emailService).sendEmail(any());
    }

    @Test
    public void givenAdditionalEvidenceHasEmptyFileName_doNotDownloadAdditionalEvidenceAndStillGenerateRoboticsAndSendEmail() {

        SscsCaseData appeal = buildCaseData();

        JSONObject mappedJson = mock(JSONObject.class);

        given(roboticsJsonMapper.map(any())).willReturn(mappedJson);

        given(airLookupService.lookupAirVenueNameByPostCode("AB12 XYZ")).willReturn(AirlookupBenefitToVenue.builder().pipVenue("Bristol").build());

        given(emailService.generateUniqueEmailId(appeal.getAppeal().getAppellant())).willReturn("Bloggs_123");

        byte[] pdf = {};
        byte[] someFile = {};

        roboticsService.processRobotics(appeal, 123L, "AB12 XYZ", pdf, Collections.singletonMap(null, someFile));

        verify(roboticsEmailTemplate).generateEmail(eq("Bloggs_123"), captor.capture(), eq(NOT_SCOTTISH));
        List<EmailAttachment> attachmentResult = captor.getValue();

        assertThat(attachmentResult.size(), is(2));
        assertThat(attachmentResult.get(0).getFilename(), is("Bloggs_123.txt"));
        assertThat(attachmentResult.get(1).getFilename(), is("Bloggs_123.pdf"));

        verify(roboticsJsonMapper).map(any());
        verify(roboticsJsonValidator).validate(mappedJson);
        verify(emailService).sendEmail(any());
    }

    @Test
    public void givenAdditionalEvidenceFileIsEmpty_doNotDownloadAdditionalEvidenceAndStillGenerateRoboticsAndSendEmail() {

        SscsCaseData appeal = buildCaseData();

        JSONObject mappedJson = mock(JSONObject.class);

        given(roboticsJsonMapper.map(any())).willReturn(mappedJson);

        given(airLookupService.lookupAirVenueNameByPostCode("AB12 XYZ")).willReturn(AirlookupBenefitToVenue.builder().pipVenue("Bristol").build());

        given(emailService.generateUniqueEmailId(appeal.getAppeal().getAppellant())).willReturn("Bloggs_123");

        byte[] pdf = {};

        roboticsService.processRobotics(appeal, 123L, "AB12 XYZ", pdf, Collections.singletonMap("Some Evidence.doc", null));

        verify(roboticsEmailTemplate).generateEmail(eq("Bloggs_123"), captor.capture(), eq(NOT_SCOTTISH));
        List<EmailAttachment> attachmentResult = captor.getValue();

        assertThat(attachmentResult.size(), is(2));
        assertThat(attachmentResult.get(0).getFilename(), is("Bloggs_123.txt"));
        assertThat(attachmentResult.get(1).getFilename(), is("Bloggs_123.pdf"));

        verify(roboticsJsonMapper).map(any());
        verify(roboticsJsonValidator).validate(mappedJson);
        verify(emailService).sendEmail(any());
    }

    @Test
    public void generatingRoboticsReturnsTheJson() {

        SscsCaseData appeal = buildCaseData();

        JSONObject mappedJson = mock(JSONObject.class);

        given(roboticsJsonMapper.map(any())).willReturn(mappedJson);

        given(airLookupService.lookupAirVenueNameByPostCode("AB12 XYZ")).willReturn(AirlookupBenefitToVenue.builder().pipVenue("Bristol").build());

        given(emailService.generateUniqueEmailId(appeal.getAppeal().getAppellant())).willReturn("Bloggs_123");

        JSONObject roboticsJson = roboticsService.processRobotics(appeal, 123L, "AB12 XYZ", null, Collections.emptyMap());

        assertThat(roboticsJson, is(mappedJson));
    }
}
