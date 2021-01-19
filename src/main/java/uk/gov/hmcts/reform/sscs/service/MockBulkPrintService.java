package uk.gov.hmcts.reform.sscs.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;


@Service("bulkPrintService")
@ConditionalOnProperty(prefix = "send-letter", name = "url", havingValue = "false")
public class MockBulkPrintService implements PrintService {
    private static final Logger logger = LoggerFactory.getLogger(MockBulkPrintService.class);

    private final CcdNotificationsPdfService ccdNotificationsPdfService;

    private final BulkPrintServiceHelper bulkPrintServiceHelper;

    private final boolean reasonableAdjustmentsEnabled;

    public MockBulkPrintService(CcdNotificationsPdfService ccdNotificationsPdfService,
                                BulkPrintServiceHelper bulkPrintServiceHelper,
                                @Value("${feature.reasonable-adjustments.enabled}") boolean reasonableAdjustmentsEnabled) {
        this.ccdNotificationsPdfService = ccdNotificationsPdfService;
        this.bulkPrintServiceHelper = bulkPrintServiceHelper;
        this.reasonableAdjustmentsEnabled = reasonableAdjustmentsEnabled;
    }

    public Optional<UUID> sendToBulkPrint(List<Pdf> pdfs, SscsCaseData sscsCaseData) {
        return sendToBulkPrint(pdfs, sscsCaseData, null, null);
    }

    public Optional<UUID> sendToBulkPrint(List<Pdf> pdfs, SscsCaseData sscsCaseData, FurtherEvidenceLetterType letterType, EventType event) {
        if (reasonableAdjustmentsEnabled) {
            if (bulkPrintServiceHelper.sendForReasonableAdjustMent(sscsCaseData, letterType, event)) {
                bulkPrintServiceHelper.saveAsReasonableAdjustment(sscsCaseData, pdfs, letterType, event);
            }
        } else {
            logger.info("No bulk print operation needs to be performed as 'Bulk print url' is switched off.");
        }
        return Optional.of(UUID.fromString("abc123ca-c336-11e9-9cb5-123456789abc"));
    }
}
