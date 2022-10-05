package uk.gov.hmcts.reform.sscs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.APPELLANT_LETTER;
import static uk.gov.hmcts.reform.sscs.service.MockBulkPrintService.MOCK_UUID;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;


@RunWith(MockitoJUnitRunner.class)
public class MockBulkPrintServiceTest {
    public static final String CASE_ID = "12345678";
    @InjectMocks
    private MockBulkPrintService mockBulkPrintService;

    @Mock
    private BulkPrintServiceHelper bulkPrintServiceHelper;
    private SscsCaseData caseData;
    private List<Pdf> pdfs;

    @Before
    public void setUp() {
        caseData = SscsCaseData.builder()
            .ccdCaseId(CASE_ID)
            .build();

        pdfs = List.of(new Pdf("myData".getBytes(), "file.pdf"));
    }

    @Test
    public void sendToMockBulkPrint() {
        Optional<UUID> result = mockBulkPrintService.sendToBulkPrint(pdfs, caseData);
        assertThat(result)
            .isPresent()
            .hasValue(UUID.fromString(MOCK_UUID));
    }

    @Test
    public void sendToMockBulkPrintReasonableAdjustment() {
        when(bulkPrintServiceHelper.sendForReasonableAdjustment(caseData,APPELLANT_LETTER)).thenReturn(true);

        Optional<UUID> result = mockBulkPrintService.sendToBulkPrint(pdfs, caseData, APPELLANT_LETTER,
            VALID_APPEAL_CREATED);

        assertThat(result).isNotPresent();
    }

    @Test
    public void sendToMockBulkPrintNoReasonableAdjustment() {
        when(bulkPrintServiceHelper.sendForReasonableAdjustment(caseData,APPELLANT_LETTER)).thenReturn(false);

        Optional<UUID> result = mockBulkPrintService.sendToBulkPrint(pdfs, caseData, APPELLANT_LETTER,
            VALID_APPEAL_CREATED);

        assertThat(result).isNotPresent();
    }
}
