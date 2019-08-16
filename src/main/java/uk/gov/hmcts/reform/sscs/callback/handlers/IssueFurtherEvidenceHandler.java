package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.FurtherEvidenceService;

@Service
public class IssueFurtherEvidenceHandler implements CallbackHandler<SscsCaseData> {

    @Autowired
    private FurtherEvidenceService furtherEvidenceService;

    @Autowired
    private CcdService ccdService;

    @Autowired
    private IdamService idamService;


    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        return furtherEvidenceService.canHandleAnyDocument(callback.getCaseDetails().getCaseData().getSscsDocument());
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        furtherEvidenceService.issue(callback.getCaseDetails().getCaseData(), APPELLANT_EVIDENCE);
        furtherEvidenceService.issue(callback.getCaseDetails().getCaseData(), REPRESENTATIVE_EVIDENCE);
        furtherEvidenceService.issue(callback.getCaseDetails().getCaseData(), DWP_EVIDENCE);

        setEvidenceIssuedFlagToYes(callback.getCaseDetails().getCaseData().getSscsDocument());
        updateCase(callback.getCaseDetails().getCaseData());
    }

    private void setEvidenceIssuedFlagToYes(List<SscsDocument> sscsDocuments) {

        if (sscsDocuments != null) {
            for (SscsDocument doc : sscsDocuments) {
                if (doc.getValue().getEvidenceIssued() != null && doc.getValue().getEvidenceIssued().equals("No")) {
                    doc.getValue().setEvidenceIssued("Yes");
                }
            }
        }
    }

    private void updateCase(SscsCaseData caseData) {
        ccdService.updateCase(
            caseData,
            Long.valueOf(caseData.getCcdCaseId()),
            EventType.UPDATE_CASE_ONLY.getCcdType(),
            "Update case data only",
            "Update document evidence issued flags after issuing further evidence to DWP",
            idamService.getIdamTokens());
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
