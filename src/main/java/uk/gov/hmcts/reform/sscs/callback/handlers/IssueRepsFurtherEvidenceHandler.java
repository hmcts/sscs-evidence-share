package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REPRESENTATIVE_EVIDENCE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.FurtherEvidenceService;

@Service
public class IssueRepsFurtherEvidenceHandler implements CallbackHandler<SscsCaseData> {

    @Autowired
    private FurtherEvidenceService furtherEvidenceService;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        return furtherEvidenceService.canHandleAnyDocument(callback.getEvent(),
            callback.getCaseDetails().getCaseData().getSscsDocument(),
            REPRESENTATIVE_EVIDENCE);
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        furtherEvidenceService.issue(callback.getCaseDetails().getCaseData(), REPRESENTATIVE_EVIDENCE);
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
