package uk.gov.hmcts.reform.sscs.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;

public class RoboticsHandlerTest {

    RoboticsHandler roboticsHandler;

    @Mock
    EvidenceManagementService evidenceManagementService;

    @Mock
    RoboticsService roboticsService;

    @Mock
    RegionalProcessingCenterService regionalProcessingCenterService;

    SscsCcdConvertService convertService;

    LocalDate localDate;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Before
    public void setup() {
        initMocks(this);

        convertService = new SscsCcdConvertService();

        roboticsHandler = new RoboticsHandler(roboticsService, regionalProcessingCenterService, evidenceManagementService);

        localDate = LocalDate.now();
    }

    @Test
    public void givenACaseWithCaseCreatedEvent_thenCreateRoboticsFile() {

        Appeal appeal = Appeal.builder().mrnDetails(MrnDetails.builder().mrnDate(localDate.format(formatter)).build())
            .appellant(Appellant.builder().address(
                Address.builder().postcode("CM120HN").build())
            .build()).build();

        SscsCaseData caseData = SscsCaseData.builder().appeal(appeal).ccdCaseId("123").build();

        given(evidenceManagementService.download(any(), eq(null))).willReturn(null);

        given(regionalProcessingCenterService.getFirstHalfOfPostcode("CM120HN")).willReturn("CM12");

        when(roboticsService
            .sendCaseToRobotics(caseData, 123L, "CM12", null, Collections.emptyMap()))
            .thenReturn(null);

        roboticsHandler.sendCaseToRobotics(caseData);

        verify(roboticsService).sendCaseToRobotics(caseData, 123L, "CM12", null, Collections.emptyMap());
    }

    @Test
    public void givenACaseWithCaseCreatedEventAndEvidenceToDownload_thenCreateRoboticsFileWithDownloadedEvidence() {

        Appeal appeal = Appeal.builder().mrnDetails(MrnDetails.builder().mrnDate(localDate.format(formatter)).build())
            .appellant(Appellant.builder().address(
                Address.builder().postcode("CM120HN").build())
                .build()).build();

        List<SscsDocument> documents = new ArrayList<>();
        documents.add(SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType("appellantEvidence")
                .documentFileName("test.jpg")
                .documentLink(DocumentLink.builder().documentUrl("www.download.com").build())
                .build())
            .build());

        SscsCaseData caseData = SscsCaseData.builder().appeal(appeal).sscsDocument(documents).ccdCaseId("123").build();

        given(regionalProcessingCenterService.getFirstHalfOfPostcode("CM120HN")).willReturn("CM12");

        byte[] expectedBytes = {1, 2, 3};
        given(evidenceManagementService.download(URI.create("www.download.com"), null)).willReturn(expectedBytes);

        Map<String, byte[]> expectedAdditionalEvidence = new HashMap<>();
        expectedAdditionalEvidence.put("test.jpg", expectedBytes);
        when(roboticsService
            .sendCaseToRobotics(caseData, 123L, "CM12", null, expectedAdditionalEvidence))
            .thenReturn(null);

        roboticsHandler.sendCaseToRobotics(caseData);

        verify(roboticsService).sendCaseToRobotics(caseData, 123L, "CM12", null, expectedAdditionalEvidence);
    }
}
