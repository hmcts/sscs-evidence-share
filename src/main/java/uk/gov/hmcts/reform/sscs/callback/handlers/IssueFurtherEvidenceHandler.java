package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DWP_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REPRESENTATIVE_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.APPELLANT_LETTER;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.DWP_LETTER;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.REPRESENTATIVE_LETTER;

import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.exception.IssueFurtherEvidenceException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.FurtherEvidenceService;

@Service
@Slf4j
public class IssueFurtherEvidenceHandler implements CallbackHandler<SscsCaseData> {

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
        issueFurtherEvidence(caseData);
        postIssueFurtherEvidenceTasks(caseData);
    }

    private void issueFurtherEvidence(SscsCaseData caseData) {
        List<DocumentType> documentTypes = Arrays.asList(APPELLANT_EVIDENCE, REPRESENTATIVE_EVIDENCE, DWP_EVIDENCE);
        List<FurtherEvidenceLetterType> allowedLetterTypes = Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER,
            DWP_LETTER);
        documentTypes.forEach(documentType -> doIssuePerDocumentType(caseData, allowedLetterTypes, documentType));
    }

    private void doIssuePerDocumentType(SscsCaseData caseData, List<FurtherEvidenceLetterType> allowedLetterTypes,
                                        DocumentType documentType) {
        try {
            furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, documentType, allowedLetterTypes);
        } catch (Exception e) {
            handleIssueFurtherEvidenceException(caseData, documentType);
            String errorMsg = "Failed sending further evidence for case(%s) and documentType(%s)...";
            throw new IssueFurtherEvidenceException(String.format(errorMsg, caseData.getCcdCaseId(),
                documentType.getValue()), e);
        }
    }

    private void postIssueFurtherEvidenceTasks(SscsCaseData caseData) {
        setEvidenceIssuedFlagToYes(caseData.getSscsDocument());
        ccdService.updateCase(caseData, Long.valueOf(caseData.getCcdCaseId()), EventType.UPDATE_CASE_ONLY.getCcdType(),
            "Update case data only",
            "Update document evidence issued flags after issuing further evidence to DWP",
            idamService.getIdamTokens());
    }

    private void handleIssueFurtherEvidenceException(SscsCaseData caseData, DocumentType documentType) {
        caseData.setHmctsDwpState("failedSendingFurtherEvidence");
        ccdService.updateCase(caseData, Long.valueOf(caseData.getCcdCaseId()),
            EventType.SEND_FURTHER_EVIDENCE_ERROR.getCcdType(),
            "Failed to issue documentType",
            String.format("Failed to issue the %s documentType", documentType.getValue()),
            idamService.getIdamTokens());
    }

    private void setEvidenceIssuedFlagToYes(List<SscsDocument> sscsDocuments) {
        if (sscsDocuments != null) {
            for (SscsDocument doc : sscsDocuments) {
                if (doc.getValue().getEvidenceIssued() != null
                    && "No".equalsIgnoreCase(doc.getValue().getEvidenceIssued())) {
                    doc.getValue().setEvidenceIssued("Yes");
                }
            }
        }
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
