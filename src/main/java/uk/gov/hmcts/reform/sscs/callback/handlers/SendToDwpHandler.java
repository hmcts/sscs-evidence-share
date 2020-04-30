package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SEND_TO_DWP;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Slf4j
@Service
public class SendToDwpHandler implements CallbackHandler<SscsCaseData> {

    private final DispatchPriority dispatchPriority;

    private final CcdService ccdService;

    private final IdamService idamService;

    private final boolean bulkScanMigrated;

    @Autowired
    public SendToDwpHandler(CcdService ccdService,
                            IdamService idamService,
                            @Value("${feature.bulk-scan.migrated}") boolean bulkScanMigrated) {
        this.dispatchPriority = DispatchPriority.LATE;
        this.ccdService = ccdService;
        this.idamService = idamService;
        this.bulkScanMigrated = bulkScanMigrated;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return bulkScanMigrated
            && callbackType.equals(CallbackType.SUBMITTED)
            && (callback.getEvent() == EventType.VALID_APPEAL_CREATED
            || callback.getEvent() == EventType.SYA_APPEAL_CREATED);
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        log.info("About to update case with sendToDwp event for id {}", callback.getCaseDetails().getId());

        IdamTokens idamTokens = idamService.getIdamTokens();

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();

        ccdService.updateCase(caseDetails.getCaseData(), caseDetails.getId(), SEND_TO_DWP.getCcdType(), "Send to DWP", "Send to DWP event has been triggered from Evidence share service", idamTokens);

        log.info("Case updated with sendToDwp event for id {}", caseDetails.getId());
    }

    @Override
    public DispatchPriority getPriority() {
        return this.dispatchPriority;
    }
}
