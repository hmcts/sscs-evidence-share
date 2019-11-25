package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Service
public class DwpUploadResponseHandler implements CallbackHandler<SscsCaseData> {

    private CcdService ccdService;
    private IdamService idamService;

    @Autowired
    public DwpUploadResponseHandler(CcdService ccdService, IdamService idamService) {
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.DWP_UPLOAD_RESPONSE
            && READY_TO_LIST.getId().equals(callback.getCaseDetails().getCaseData().getCreatedInGapsFrom());
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        if (StringUtils.equalsIgnoreCase(callback.getCaseDetails().getCaseData().getDwpFurtherInfo(), "no")) {
            ccdService.updateCase(callback.getCaseDetails().getCaseData(), callback.getCaseDetails().getId(),
                EventType.READY_TO_LIST.getCcdType(), "ready to list",
                "update to ready to list event as there is no further information to assist the tribunal.", idamService.getIdamTokens());
        }
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
