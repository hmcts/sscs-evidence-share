package uk.gov.hmcts.reform.sscs.service;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.APPELLANT_LETTER;

import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.reform.sendletter.api.LetterWithPdfsRequest;
import uk.gov.hmcts.reform.sendletter.api.SendLetterApi;
import uk.gov.hmcts.reform.sendletter.api.SendLetterResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.exception.BulkPrintException;
import uk.gov.hmcts.reform.sscs.exception.NonPdfBulkPrintException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@RunWith(MockitoJUnitRunner.class)
public class BulkPrintServiceTest {

    private static final List<Pdf> PDF_LIST = singletonList(new Pdf("myData".getBytes(), "file.pdf"));
    private static final UUID LETTER_ID = UUID.randomUUID();

    private static final SscsCaseData SSCS_CASE_DATA = SscsCaseData.builder()
        .ccdCaseId("234")
        .appeal(Appeal.builder().appellant(
            Appellant.builder()
                .name(Name.builder().firstName("Appellant").lastName("LastName").build())
                .address(Address.builder().line1("line1").build())
                .build())
            .build())
        .build();
    private static final String AUTH_TOKEN = "Auth_Token";

    private BulkPrintService bulkPrintService;
    @Mock
    private SendLetterApi sendLetterApi;
    @Mock
    private IdamService idamService;
    @Mock
    private BulkPrintServiceHelper bulkPrintServiceHelper;

    @Captor
    ArgumentCaptor<LetterWithPdfsRequest> captor;

    @Before
    public void setUp() {
        this.bulkPrintService = new BulkPrintService(sendLetterApi, idamService, bulkPrintServiceHelper, true, 1, false);
        when(idamService.generateServiceAuthorization()).thenReturn(AUTH_TOKEN);
    }

    @Test
    public void willSendToBulkPrint() {
        when(sendLetterApi.sendLetter(eq(AUTH_TOKEN), captor.capture()))
            .thenReturn(new SendLetterResponse(LETTER_ID));
        List<Correspondence> reasonableAdjustments = bulkPrintService.sendToBulkPrint(PDF_LIST, SSCS_CASE_DATA, APPELLANT_LETTER, EventType.VALID_APPEAL_CREATED);
        assertEquals("Should be no reasonable adjustment", 0, reasonableAdjustments.size());
    }

    @Test
    public void willSendToBulkPrintWithAdditionalData() {
        when(sendLetterApi.sendLetter(eq(AUTH_TOKEN), any(LetterWithPdfsRequest.class)))
            .thenReturn(new SendLetterResponse(LETTER_ID));
        List<Correspondence> reasonableAdjustments = bulkPrintService.sendToBulkPrint(PDF_LIST, SSCS_CASE_DATA, APPELLANT_LETTER, EventType.VALID_APPEAL_CREATED);
        assertEquals("Should be no reasonable adjustment", 0, reasonableAdjustments.size());
    }

    @Test(expected = BulkPrintException.class)
    public void willThrowAnyExceptionsToBulkPrint() {
        when(sendLetterApi.sendLetter(eq(AUTH_TOKEN), any(LetterWithPdfsRequest.class)))
            .thenThrow(new RuntimeException("error"));
        bulkPrintService.sendToBulkPrint(PDF_LIST, SSCS_CASE_DATA, APPELLANT_LETTER, EventType.VALID_APPEAL_CREATED);
    }

    @Test(expected = NonPdfBulkPrintException.class)
    public void shouldThrowANonPdfBulkPrintExceptionOnHttpClientErrorExceptionFromBulkPrint() {
        when(sendLetterApi.sendLetter(eq(AUTH_TOKEN), any(LetterWithPdfsRequest.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.valueOf(400)));
        bulkPrintService.sendToBulkPrint(PDF_LIST, SSCS_CASE_DATA, APPELLANT_LETTER, EventType.VALID_APPEAL_CREATED);
    }

    @Test
    public void sendLetterNotEnabledWillNotSendToBulkPrint() {
        BulkPrintService notEnabledBulkPrint = new BulkPrintService(sendLetterApi, idamService, bulkPrintServiceHelper, false, 1, false);
        notEnabledBulkPrint.sendToBulkPrint(PDF_LIST, SSCS_CASE_DATA, APPELLANT_LETTER, EventType.VALID_APPEAL_CREATED);
        verifyNoInteractions(idamService);
        verifyNoInteractions(sendLetterApi);
    }

    @Test
    public void willSendToBulkPrintWithReasonableAdjustment() {
        this.bulkPrintService = new BulkPrintService(sendLetterApi, idamService, bulkPrintServiceHelper, true, 1, true);

        SSCS_CASE_DATA.setReasonableAdjustments(ReasonableAdjustments.builder()
            .appellant(ReasonableAdjustmentDetails.builder()
                .wantsReasonableAdjustment(YesNo.YES).reasonableAdjustmentRequirements("Big text")
                .build()).build());

        when(bulkPrintServiceHelper.sendForReasonableAdjustMent(SSCS_CASE_DATA, APPELLANT_LETTER, ISSUE_FURTHER_EVIDENCE)).thenReturn(true);
        List<Correspondence> reasonableAdjustment = bulkPrintService.sendToBulkPrint(PDF_LIST, SSCS_CASE_DATA, APPELLANT_LETTER, ISSUE_FURTHER_EVIDENCE);
        assertEquals("Should be no reasonable adjustment", 0, reasonableAdjustment.size());

        verify(bulkPrintServiceHelper).saveAsReasonableAdjustment(any(), any(), any(), any());
    }
}
