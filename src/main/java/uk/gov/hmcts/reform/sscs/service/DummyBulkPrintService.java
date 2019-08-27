package uk.gov.hmcts.reform.sscs.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;

@Service("bulkPrintService")
@ConditionalOnProperty(prefix = "send-letter", name = "url", havingValue = "false")
public class DummyBulkPrintService implements PrintService {
    private static final Logger logger = LoggerFactory.getLogger(DummyBulkPrintService.class);

    public Optional<UUID> sendToBulkPrint(List<Pdf> pdfs, SscsCaseData sscsCaseData) {

        logger.info("No bulk print operation needs to be performed as 'Bulk print url' is switched off.");

        return Optional.of(UUID.fromString("abc123ca-c336-11e9-9cb5-123456789abc"));
    }
}
