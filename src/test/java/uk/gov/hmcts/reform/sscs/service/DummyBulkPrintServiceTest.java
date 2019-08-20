package uk.gov.hmcts.reform.sscs.service;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

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
public class DummyBulkPrintServiceTest {

    private static final List<Pdf> PDF_LIST = singletonList(new Pdf("myData".getBytes(), "file.pdf"));

    private static final SscsCaseData SSCS_CASE_DATA = SscsCaseData.builder().build();

    private DummyBulkPrintService dummyBulkPrintService;

    @Before
    public void setUp() {
        this.dummyBulkPrintService = new DummyBulkPrintService();
    }

    @Test
    public void willDummySendToBulkPrint() {
        Optional<UUID> letterIdOptional = dummyBulkPrintService.sendToBulkPrint(PDF_LIST, SSCS_CASE_DATA);
        assertEquals(Optional.of(UUID.fromString("abc123ca-c336-11e9-9cb5-123456789abc")), letterIdOptional);
    }
}
