package uk.gov.hmcts.reform.sscs.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;

@Service("bulkPrintService")
@Slf4j
@ConditionalOnProperty(prefix = "send-letter", name = "url", havingValue = "false")
@RequiredArgsConstructor
public class MockBulkPrintService implements PrintService {
    public static final String MOCK_UUID = "abc123ca-c336-11e9-9cb5-123456789abc";

    private final BulkPrintServiceHelper bulkPrintServiceHelper;

    public Optional<UUID> sendToBulkPrint(List<Pdf> pdfs, SscsCaseData sscsCaseData) {
        log.info("No bulk print operation needs to be performed as 'Bulk print url' is switched off.");
        return Optional.of(UUID.fromString(MOCK_UUID));
    }


    public Optional<UUID> sendToBulkPrint(List<Pdf> pdfs, SscsCaseData sscsCaseData,
                                          FurtherEvidenceLetterType letterType, EventType event) {
        if (bulkPrintServiceHelper.sendForReasonableAdjustment(sscsCaseData, letterType)) {
            log.info("Sending to bulk print service reasonable adjustments enabled {}", sscsCaseData.getCcdCaseId());
            bulkPrintServiceHelper.saveAsReasonableAdjustment(sscsCaseData, pdfs, letterType, event);
        } else {
            log.info("No bulk print operation needs to be performed as 'Bulk print url' is switched off.");
        }

        return Optional.empty();
    }
}
