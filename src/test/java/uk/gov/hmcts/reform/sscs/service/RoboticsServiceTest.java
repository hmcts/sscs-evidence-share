package uk.gov.hmcts.reform.sscs.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.APPEAL_CREATED;
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
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.config.EvidenceShareConfig;
import uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment;
import uk.gov.hmcts.reform.sscs.domain.email.RoboticsEmailTemplate;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.model.dwp.Mapping;
import uk.gov.hmcts.reform.sscs.model.dwp.OfficeMapping;
import uk.gov.hmcts.reform.sscs.robotics.json.RoboticsJsonMapper;
import uk.gov.hmcts.reform.sscs.robotics.json.RoboticsJsonValidator;

@RunWith(JUnitParamsRunner.class)
public class RoboticsServiceTest {

    RoboticsService roboticsService;

    @Mock
    EvidenceManagementService evidenceManagementService;

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

    @Mock
    DwpAddressLookupService dwpAddressLookupService;

    @Mock
    CcdService ccdService;

    @Mock
    IdamService idamService;

    SscsCcdConvertService convertService;

    LocalDate localDate;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Captor
    private ArgumentCaptor<List<EmailAttachment>> captor;

    @Captor
    private ArgumentCaptor<SscsCaseData> caseDataCaptor;

    private SscsCaseData appeal;

    private CaseDetails<SscsCaseData> caseData;

    @Before
    public void setup() {
        initMocks(this);

        convertService = new SscsCcdConvertService();

        roboticsService = new RoboticsService(
            evidenceManagementService,
            emailService,
            roboticsJsonMapper,
            roboticsJsonValidator,
            roboticsEmailTemplate,
            evidenceShareConfig,
            dwpAddressLookupService,
            ccdService,
            idamService,
            1,
            1);

        localDate = LocalDate.now();

        JSONObject mappedJson = mock(JSONObject.class);
        given(roboticsJsonMapper.map(any())).willReturn(mappedJson);
        given(evidenceShareConfig.getSubmitTypes()).willReturn(Collections.singletonList("paper"));

        appeal = buildCaseData();
        caseData = new CaseDetails<>(1L, null, APPEAL_CREATED, appeal, null);
    }

    @Test
    @Parameters({"CARDIFF", "GLASGOW", "", "null"})
    public void generatingRoboticsSendsAnEmail(String rpcName) {

        Appeal appeal = Appeal.builder()
            .mrnDetails(MrnDetails.builder().mrnDate(localDate.format(formatter)).build())
            .benefitType(BenefitType.builder().code("PIP").build())
            .appellant(Appellant.builder().address(
                Address.builder().postcode("CM120HN").build())
            .build()).build();

        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(appeal).regionalProcessingCenter(RegionalProcessingCenter.builder().name(rpcName).build()).ccdCaseId("123").build();

        given(evidenceManagementService.download(any(), eq(null))).willReturn(null);

        given(emailService.generateUniqueEmailId(appeal.getAppellant())).willReturn("Bloggs_123");

        CaseDetails<SscsCaseData> caseData = new CaseDetails<>(1L, null, APPEAL_CREATED, sscsCaseData, null);
        roboticsService.sendCaseToRobotics(caseData);

        boolean isScottish = StringUtils.equalsAnyIgnoreCase(rpcName,"GLASGOW");
        verify(roboticsEmailTemplate).generateEmail(eq("Bloggs_123 for Robot [1]"), captor.capture(), eq(isScottish), eq(false));
        List<EmailAttachment> attachmentResult = captor.getValue();

        assertThat(attachmentResult.size(), is(1));
        assertThat(attachmentResult.get(0).getFilename(), is("Bloggs_123.txt"));

        verify(roboticsJsonMapper).map(any());
        verify(roboticsJsonValidator).validate(any());
        verify(emailService).sendEmail(eq(1L), any());
    }

    @Test
    @Parameters({"Paper", "Online"})
    public void givenACaseWithEvidenceToDownload_thenCreateRoboticsFileWithDownloadedEvidence(String receivedVia) {

        byte[] expectedBytes = {1, 2, 3};
        given(evidenceManagementService.download(URI.create("www.download.com"), null)).willReturn(expectedBytes);

        Appeal appeal = Appeal.builder().mrnDetails(MrnDetails.builder().mrnDate(localDate.format(formatter)).build())
            .benefitType(BenefitType.builder().code("PIP").build())
            .receivedVia(receivedVia)
            .appellant(Appellant.builder().address(
                Address.builder().postcode("CM120HN").build())
                .build()).build();

        given(emailService.generateUniqueEmailId(appeal.getAppellant())).willReturn("Bloggs_123");

        List<SscsDocument> documents = new ArrayList<>();
        documents.add(SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType("appellantEvidence")
                .documentFileName("test.jpg")
                .documentLink(DocumentLink.builder().documentUrl("www.download.com").documentFilename("fileName.pdf").build())
                .build())
            .build());

        CaseDetails<SscsCaseData> caseData = new CaseDetails<>(1L, null, APPEAL_CREATED, SscsCaseData.builder().appeal(appeal).sscsDocument(documents).ccdCaseId("123").build(), null);

        roboticsService.sendCaseToRobotics(caseData);

        verify(roboticsEmailTemplate).generateEmail(eq("Bloggs_123 for Robot [1]"), captor.capture(), eq(false), eq(false));
        List<EmailAttachment> attachmentResult = captor.getValue();

        assertThat(attachmentResult.get(0).getFilename(), is("Bloggs_123.txt"));

        if (receivedVia.equals("Paper")) {
            assertThat(attachmentResult.size(), is(1));
        } else {
            assertThat(attachmentResult.size(), is(2));
            assertThat(attachmentResult.get(1).getFilename(), is("fileName.pdf"));
        }

        verify(roboticsJsonMapper).map(any());
        verify(roboticsJsonValidator).validate(any());
        verify(emailService).sendEmail(eq(1L), any());
    }

    @Test
    public void givenAdditionalEvidenceHasEmptyFileName_doNotDownloadAdditionalEvidenceAndStillGenerateRoboticsAndSendEmail() {

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
                .documentFileName(null)
                .documentLink(DocumentLink.builder().documentUrl("www.download.com").build())
                .build())
            .build());

        CaseDetails<SscsCaseData> caseData = new CaseDetails<>(1L, null, APPEAL_CREATED, SscsCaseData.builder().appeal(appeal).sscsDocument(documents).ccdCaseId("123").build(), null);

        roboticsService.sendCaseToRobotics(caseData);

        verify(roboticsEmailTemplate).generateEmail(eq("Bloggs_123 for Robot [1]"), captor.capture(), eq(false), eq(false));
        List<EmailAttachment> attachmentResult = captor.getValue();

        assertThat(attachmentResult.get(0).getFilename(), is("Bloggs_123.txt"));
        assertThat(attachmentResult.size(), is(1));

        verify(roboticsJsonMapper).map(any());
        verify(roboticsJsonValidator).validate(any());
        verify(emailService).sendEmail(eq(1L), any());
    }

    @Test
    public void givenAdditionalEvidenceHasEmptyFileNameAndIsPipAeTrue_doNotDownloadAdditionalEvidenceAndStillGenerateRoboticsAndSendEmail() {

        byte[] expectedBytes = {1, 2, 3};
        given(evidenceManagementService.download(URI.create("www.download.com"), null)).willReturn(expectedBytes);

        Map<String, byte[]> expectedAdditionalEvidence = new HashMap<>();
        expectedAdditionalEvidence.put("test.jpg", expectedBytes);

        Appeal appeal = Appeal.builder().mrnDetails(MrnDetails.builder().mrnDate(localDate.format(formatter)).dwpIssuingOffice("DWP PIP (AE)").build())
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
                        .documentFileName(null)
                        .documentLink(DocumentLink.builder().documentUrl("www.download.com").build())
                        .build())
                .build());

        CaseDetails<SscsCaseData> caseData = new CaseDetails<>(1L, null, APPEAL_CREATED, SscsCaseData.builder().appeal(appeal).sscsDocument(documents).ccdCaseId("123").build(), null);

        roboticsService.sendCaseToRobotics(caseData);

        verify(roboticsEmailTemplate).generateEmail(eq("Bloggs_123 for Robot [1]"), captor.capture(), eq(false), eq(true));
        List<EmailAttachment> attachmentResult = captor.getValue();

        assertThat(attachmentResult.get(0).getFilename(), is("Bloggs_123.txt"));
        assertThat(attachmentResult.size(), is(1));

        verify(roboticsJsonMapper).map(any());
        verify(roboticsJsonValidator).validate(any());
        verify(emailService).sendEmail(eq(1L), any());
    }

    @Test
    @Parameters({"ESA, Balham DRT, Watford DRT", "PIP, DWP PIP (1), DWP PIP (2)", "PIP, PIP (AE), DWP PIP (AE)"})
    public void givenACaseAndDwpIssuingOfficeIsClosed_thenFindNewIssuingOfficeAndUpdateCaseInCcdAndRobotics(String benefitType, String existingOffice, String newOffice) {
        given(dwpAddressLookupService.getDwpMappingByOffice(benefitType, existingOffice)).willReturn(Optional.of(
            OfficeMapping.builder().code(existingOffice).mapping(Mapping.builder().ccd(newOffice).build()).build()));

        Appeal appeal = Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice(existingOffice).mrnDate(localDate.format(formatter)).build())
            .benefitType(BenefitType.builder().code(benefitType).build())
            .appellant(Appellant.builder().address(Address.builder().postcode("CM120HN").build()).build()).build();

        CaseDetails<SscsCaseData> caseData = new CaseDetails<>(1L, null, APPEAL_CREATED, SscsCaseData.builder().appeal(appeal).ccdCaseId("123").build(), null);

        roboticsService.sendCaseToRobotics(caseData);

        assertThat(caseData.getCaseData().getAppeal().getMrnDetails().getDwpIssuingOffice(), is(newOffice));
        verify(ccdService).updateCase(caseDataCaptor.capture(), any(), any(), any(), any(), any());

        assertThat(caseDataCaptor.getValue().getAppeal().getMrnDetails().getDwpIssuingOffice(), is(newOffice));
    }

    @Test
    @Parameters({"ESA, Balham DRT, Watford DRT", "PIP, DWP PIP (1), DWP PIP (2)", "PIP, PIP (AE), DWP PIP (AE)"})
    public void givenACaseAndDwpOriginatingOfficeIsClosed_thenFindNewOriginatingOfficeAndUpdateCaseInCcdAndRobotics(String benefitType, String existingOffice, String newOffice) {
        given(dwpAddressLookupService.getDwpMappingByOffice(benefitType, existingOffice)).willReturn(Optional.of(
            OfficeMapping.builder().code(existingOffice).mapping(Mapping.builder().ccd(newOffice).build()).build()));

        Appeal appeal = Appeal.builder().mrnDetails(MrnDetails.builder().mrnDate(localDate.format(formatter)).build())
            .benefitType(BenefitType.builder().code(benefitType).build())
            .appellant(Appellant.builder().address(Address.builder().postcode("CM120HN").build()).build()).build();

        DynamicListItem value = new DynamicListItem(existingOffice, existingOffice);
        CaseDetails<SscsCaseData> caseData = new CaseDetails<>(1L, null, APPEAL_CREATED, SscsCaseData.builder().dwpOriginatingOffice(new DynamicList(value, Collections.singletonList(value))).appeal(appeal).ccdCaseId("123").build(), null);

        roboticsService.sendCaseToRobotics(caseData);

        assertThat(caseData.getCaseData().getDwpOriginatingOffice().getValue().getCode(), is(newOffice));
        verify(ccdService).updateCase(caseDataCaptor.capture(), any(), any(), any(), any(), any());

        assertThat(caseDataCaptor.getValue().getDwpOriginatingOffice().getValue().getCode(), is(newOffice));
    }

    @Test
    @Parameters({"ESA, Balham DRT, Watford DRT", "PIP, DWP PIP (1), DWP PIP (2)", "PIP, PIP (AE), DWP PIP (AE)"})
    public void givenACaseAndDwpPresentingOfficeIsClosed_thenFindNewPresentingOfficeAndUpdateCaseInCcdAndRobotics(String benefitType, String existingOffice, String newOffice) {
        given(dwpAddressLookupService.getDwpMappingByOffice(benefitType, existingOffice)).willReturn(Optional.of(
            OfficeMapping.builder().code(existingOffice).mapping(Mapping.builder().ccd(newOffice).build()).build()));

        Appeal appeal = Appeal.builder().mrnDetails(MrnDetails.builder().mrnDate(localDate.format(formatter)).build())
            .benefitType(BenefitType.builder().code(benefitType).build())
            .appellant(Appellant.builder().address(Address.builder().postcode("CM120HN").build()).build()).build();

        DynamicListItem value = new DynamicListItem(existingOffice, existingOffice);
        CaseDetails<SscsCaseData> caseData = new CaseDetails<>(1L, null, APPEAL_CREATED, SscsCaseData.builder().dwpPresentingOffice(new DynamicList(value, Collections.singletonList(value))).appeal(appeal).ccdCaseId("123").build(), null);

        roboticsService.sendCaseToRobotics(caseData);

        assertThat(caseData.getCaseData().getDwpPresentingOffice().getValue().getCode(), is(newOffice));
        verify(ccdService).updateCase(caseDataCaptor.capture(), any(), any(), any(), any(), any());

        assertThat(caseDataCaptor.getValue().getDwpPresentingOffice().getValue().getCode(), is(newOffice));
    }

    @Test
    @Parameters({"ESA, Balham DRT, Watford DRT", "PIP, DWP PIP (1), DWP PIP (2)", "PIP, PIP (AE), DWP PIP (AE)"})
    public void givenACaseAndOfficeUsedInAllOfficeFieldsIsClosed_thenFindNewOfficeAndUpdateAllOfficesForCaseInCcdAndRobotics(String benefitType, String existingOffice, String newOffice) {
        given(dwpAddressLookupService.getDwpMappingByOffice(benefitType, existingOffice)).willReturn(Optional.of(
            OfficeMapping.builder().code(existingOffice).mapping(Mapping.builder().ccd(newOffice).build()).build()));

        Appeal appeal = Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice(existingOffice).mrnDate(localDate.format(formatter)).build())
            .benefitType(BenefitType.builder().code(benefitType).build())
            .appellant(Appellant.builder().address(Address.builder().postcode("CM120HN").build()).build()).build();

        DynamicListItem value = new DynamicListItem(existingOffice, existingOffice);
        CaseDetails<SscsCaseData> caseData = new CaseDetails<>(1L, null, APPEAL_CREATED,
            SscsCaseData.builder().dwpOriginatingOffice(new DynamicList(value, Collections.singletonList(value)))
                .dwpPresentingOffice(new DynamicList(value, Collections.singletonList(value))).appeal(appeal).ccdCaseId("123").build(), null);

        roboticsService.sendCaseToRobotics(caseData);

        assertThat(caseData.getCaseData().getAppeal().getMrnDetails().getDwpIssuingOffice(), is(newOffice));
        assertThat(caseData.getCaseData().getDwpOriginatingOffice().getValue().getCode(), is(newOffice));
        assertThat(caseData.getCaseData().getDwpPresentingOffice().getValue().getCode(), is(newOffice));
        verify(ccdService).updateCase(caseDataCaptor.capture(), any(), any(), any(), any(), any());

        assertThat(caseDataCaptor.getValue().getAppeal().getMrnDetails().getDwpIssuingOffice(), is(newOffice));
        assertThat(caseData.getCaseData().getDwpOriginatingOffice().getValue().getCode(), is(newOffice));
        assertThat(caseDataCaptor.getValue().getDwpPresentingOffice().getValue().getCode(), is(newOffice));
    }
}
