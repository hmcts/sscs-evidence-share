package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AbstractDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.Correspondence;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.BulkPrintServiceHelper;
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


        final AbstractDocument selectedDocument = Stream.of(caseData.getSscsDocument(), caseData.getSscsWelshDocuments()).flatMap(x -> x == null ? null : x.stream()).filter(f -> f.getValue().getDocumentLink().getDocumentUrl().equals(caseData.getReissueFurtherEvidenceDocument().getValue().getCode())).findFirst()
            .orElseThrow(() ->
                new IllegalStateException(String.format("Cannot find the selected document to reissue with url %s for caseId %s.", caseData.getReissueFurtherEvidenceDocument().getValue().getCode(), caseData.getCcdCaseId()))
            );

        final DocumentType documentType = Arrays.stream(DocumentType.values()).filter(f -> f.getValue().equals(selectedDocument.getValue().getDocumentType())).findFirst().orElse(DocumentType.APPELLANT_EVIDENCE);

        List<FurtherEvidenceLetterType> allowedLetterTypes = getAllowedFurtherEvidenceLetterTypes(caseData);

        List<Correspondence> reasonableAdjustments = furtherEvidenceService.issue(Collections.singletonList(selectedDocument), caseData, documentType, allowedLetterTypes);

        if (CollectionUtils.isNotEmpty(allowedLetterTypes)) {
            setReasonableAdjustments(reasonableAdjustments, caseData);
            setEvidenceIssuedFlagToYes(selectedDocument);
            setReissueFlagsToNull(caseData);
            updateCase(caseData);
        }

    }

    private void setReasonableAdjustments(List<Correspondence> reasonableAdjustments, SscsCaseData caseData) {
        caseData = BulkPrintServiceHelper.addReasonableAdjustments(reasonableAdjustments, caseData);
    }

    private List<FurtherEvidenceLetterType> getAllowedFurtherEvidenceLetterTypes(SscsCaseData caseData) {
        final boolean resendToAppellant = caseData.isResendToAppellant();
        boolean resendToRepresentative = caseData.isResendToRepresentative();

        List<FurtherEvidenceLetterType> allowedLetterTypes = new ArrayList<>();
        if (resendToAppellant) {
            allowedLetterTypes.add(FurtherEvidenceLetterType.APPELLANT_LETTER);
        }
        if (resendToRepresentative) {
            allowedLetterTypes.add(FurtherEvidenceLetterType.REPRESENTATIVE_LETTER);
        }

        return allowedLetterTypes;
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

    private void setEvidenceIssuedFlagToYes(AbstractDocument doc) {
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
