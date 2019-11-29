package uk.gov.hmcts.reform.sscs.service;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;

@RunWith(MockitoJUnitRunner.class)
public class MockBulkPrintServiceTest {

    private MockBulkPrintService mockBulkPrintService;

    @Before
    public void setUp() {
        this.mockBulkPrintService = new MockBulkPrintService();
    }

    @Test
    public void sendToMockBulkPrint() {
        Optional<UUID> letterIdOptional = mockBulkPrintService.sendToBulkPrint(
            singletonList(new Pdf("myData".getBytes(), "file.pdf")),
            SscsCaseData.builder().build());
        assertEquals(Optional.of(UUID.fromString("abc123ca-c336-11e9-9cb5-123456789abc")), letterIdOptional);
    }
}
