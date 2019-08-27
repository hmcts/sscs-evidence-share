package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DWP_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REPRESENTATIVE_EVIDENCE;

import java.util.Collections;
import java.util.Objects;

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
public class ReissueFurtherEvidenceHandler implements CallbackHandler<SscsCaseData> {

    private FurtherEvidenceService furtherEvidenceService;
    private CcdService ccdService;
    private IdamService idamService;

    @Autowired
    public ReissueFurtherEvidenceHandler(FurtherEvidenceService furtherEvidenceService, CcdService ccdService, IdamService idamService) {
        this.furtherEvidenceService = furtherEvidenceService;
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        final boolean resendToAppellant = caseData.isResendToAppellant();
        boolean resendToRepresentative = caseData.isResendToRepresentative();
        final boolean resendToDwp = caseData.isResendToDwp();

        final SscsDocument selectedDocument = caseData.getSscsDocument().stream().filter(f -> f.getValue().getDocumentLink().getDocumentUrl().equals(caseData.getReissueFurtherEvidenceDocument().getValue().getCode())).findFirst()
            .orElseThrow(() ->
                new IllegalStateException(String.format("Cannot find the selected document to reissue with url %s for caseId %s.", caseData.getReissueFurtherEvidenceDocument().getValue().getCode(), caseData.getCcdCaseId()))
            );

        if (resendToAppellant) {
            furtherEvidenceService.issue(Collections.singletonList(selectedDocument), caseData, APPELLANT_EVIDENCE);
        }
        if (resendToRepresentative) {
            furtherEvidenceService.issue(Collections.singletonList(selectedDocument), caseData, REPRESENTATIVE_EVIDENCE);
        }
        if (resendToDwp) {
            furtherEvidenceService.issue(Collections.singletonList(selectedDocument), caseData, DWP_EVIDENCE);
        }

        if (resendToAppellant || resendToDwp || resendToRepresentative) {
            setEvidenceIssuedFlagToYes(selectedDocument);
            setReissueFlagsToNull(caseData);
            updateCase(caseData);
        }
    }

    private void setReissueFlagsToNull(SscsCaseData sscsCaseData) {
        sscsCaseData.setReissueFurtherEvidenceDocument(null);
        sscsCaseData.setResendToAppellant(null);
        sscsCaseData.setResendToRepresentative(null);
        sscsCaseData.setResendToDwp(null);
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.REISSUE_FURTHER_EVIDENCE
            && Objects.nonNull(callback.getCaseDetails().getCaseData().getReissueFurtherEvidenceDocument())
            && Objects.nonNull(callback.getCaseDetails().getCaseData().getReissueFurtherEvidenceDocument().getValue())
            && Objects.nonNull(callback.getCaseDetails().getCaseData().getReissueFurtherEvidenceDocument().getValue().getCode())
            && furtherEvidenceService.canHandleAnyDocument(callback.getCaseDetails().getCaseData().getSscsDocument());
    }

    private void setEvidenceIssuedFlagToYes(SscsDocument doc) {
        if (doc.getValue().getEvidenceIssued() != null && doc.getValue().getEvidenceIssued().equals("No")) {
            doc.getValue().setEvidenceIssued("Yes");
        }
    }

    private void updateCase(SscsCaseData caseData) {
        ccdService.updateCase(
            caseData,
            Long.valueOf(caseData.getCcdCaseId()),
            EventType.UPDATE_CASE_ONLY.getCcdType(),
            "Update case data only",
            "Update document evidence reissued flags after re-issuing further evidence to DWP",
            idamService.getIdamTokens());
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
