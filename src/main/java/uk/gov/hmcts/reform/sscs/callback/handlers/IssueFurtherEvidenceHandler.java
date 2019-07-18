package uk.gov.hmcts.reform.sscs.callback.handlers;

import java.util.List;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;

public class IssueFurtherEvidenceHandler implements CallbackHandler<SscsCaseData> {
    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback, DispatchPriority priority) {

        return validateCallback(callback.getCaseDetails().getCaseData().getSscsDocument());
    }

    private boolean validateCallback(List<SscsDocument> sscsDocument) {
        return sscsDocument.get(0).getValue().getEvidenceIssued().equals("No")
            && sscsDocument.get(0).getValue().getDocumentType().equals(DocumentType.APPELLANT_EVIDENCE.getValue());
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback, DispatchPriority priority) {

    }
}
