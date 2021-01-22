package uk.gov.hmcts.reform.sscs.service;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.APPELLANT_LETTER;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.Correspondence;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;


@RunWith(MockitoJUnitRunner.class)
public class MockBulkPrintServiceTest {

    private MockBulkPrintService mockBulkPrintService;

    private CcdNotificationsPdfService ccdNotificationsPdfService;

    private BulkPrintServiceHelper bulkPrintServiceHelper;

    @Before
    public void setUp() {
        ccdNotificationsPdfService = new CcdNotificationsPdfService();
        bulkPrintServiceHelper = new BulkPrintServiceHelper(ccdNotificationsPdfService);
        this.mockBulkPrintService = new MockBulkPrintService(ccdNotificationsPdfService, bulkPrintServiceHelper,false);

    }

    @Test
    public void sendToMockBulkPrint() {
        List<Correspondence> reasonableAdjustments = mockBulkPrintService.sendToBulkPrint(
            singletonList(new Pdf("myData".getBytes(), "file.pdf")),
            SscsCaseData.builder().ccdCaseId("12345678").build(), APPELLANT_LETTER, EventType.VALID_APPEAL_CREATED);
        assertEquals(0, reasonableAdjustments.size());
    }
}
