package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.APPELLANT_LETTER;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.DWP_LETTER;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.REPRESENTATIVE_LETTER;

import java.util.Arrays;
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
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.FurtherEvidenceService;

@Service
public class IssueFurtherEvidenceHandler implements CallbackHandler<SscsCaseData> {

    private static final List<FurtherEvidenceLetterType> ALLOWED_LETTER_TYPES = Arrays.asList(APPELLANT_LETTER,
        REPRESENTATIVE_LETTER, DWP_LETTER);
    private FurtherEvidenceService furtherEvidenceService;
    private CcdService ccdService;
    private IdamService idamService;

    @Autowired
    public IssueFurtherEvidenceHandler(FurtherEvidenceService furtherEvidenceService, CcdService ccdService, IdamService idamService) {
        this.furtherEvidenceService = furtherEvidenceService;
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.ISSUE_FURTHER_EVIDENCE
            && furtherEvidenceService.canHandleAnyDocument(callback.getCaseDetails().getCaseData().getSscsDocument());
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, APPELLANT_EVIDENCE, ALLOWED_LETTER_TYPES);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData,  REPRESENTATIVE_EVIDENCE, ALLOWED_LETTER_TYPES);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData,  DWP_EVIDENCE, ALLOWED_LETTER_TYPES);
        setEvidenceIssuedFlagToYes(caseData.getSscsDocument());
        updateCase(caseData);
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

    protected void updateCase(SscsCaseData caseData) {
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
