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
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.FurtherEvidenceService;

@Service
public class ReissueFurtherEvidenceHandler implements CallbackHandler<SscsCaseData> {

    private final FurtherEvidenceService furtherEvidenceService;
    private final CcdService ccdService;
    private final IdamService idamService;

    @Autowired
    public ReissueFurtherEvidenceHandler(FurtherEvidenceService furtherEvidenceService,
                                         CcdService ccdService,
                                         IdamService idamService) {
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

        final AbstractDocument selectedDocument =
            getSelectedDocumentInUiFromCaseData(caseData, caseData.getReissueArtifactUi());

        final DocumentType documentType = Arrays.stream(DocumentType.values())
            .filter(f -> f.getValue().equals(selectedDocument.getValue().getDocumentType()))
            .findFirst()
            .orElse(DocumentType.APPELLANT_EVIDENCE);

        List<FurtherEvidenceLetterType> allowedLetterTypes = getAllowedFurtherEvidenceLetterTypes(caseData);
        furtherEvidenceService.issue(Collections.singletonList(selectedDocument), caseData, documentType, allowedLetterTypes, null);

        if (CollectionUtils.isNotEmpty(allowedLetterTypes)) {
            udateCaseForReasonableAdjustments(caseData, selectedDocument);
        }
    }

    private AbstractDocument<? extends AbstractDocumentDetails> getSelectedDocumentInUiFromCaseData(SscsCaseData caseData,
                                                                                                    ReissueArtifactUi reissueArtifactUi) {
        return Stream.of(caseData.getSscsDocument(), caseData.getSscsWelshDocuments())
            .flatMap(documents -> getStreamIfNonNull(documents))
            .filter(document -> isDocumentSelectedInUiEqualsToStreamDocument(reissueArtifactUi, document))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(getNoSelectedDocumentErrorMessage(caseData)));
    }

    private boolean isDocumentSelectedInUiEqualsToStreamDocument(ReissueArtifactUi reissueArtifactUi,
                                                                 AbstractDocument<? extends AbstractDocumentDetails> document) {
        return document.getValue().getDocumentLink().getDocumentUrl().equals(reissueArtifactUi.getReissueFurtherEvidenceDocument().getValue().getCode());
    }

    private Stream<? extends AbstractDocument<? extends AbstractDocumentDetails>> getStreamIfNonNull(List<? extends AbstractDocument<? extends AbstractDocumentDetails>> documents) {
        return documents == null ? null : documents.stream();
    }

    private String getNoSelectedDocumentErrorMessage(SscsCaseData caseData) {
        return String.format("Cannot find the selected document to reissue with url %s for caseId %s.",
            caseData.getReissueArtifactUi().getReissueFurtherEvidenceDocument().getValue().getCode(),
            caseData.getCcdCaseId());
    }

    private void udateCaseForReasonableAdjustments(SscsCaseData caseData, AbstractDocument selectedDocument) {
        if (caseData.getReasonableAdjustmentsLetters() != null) {
            final SscsCaseDetails sscsCaseDetails = ccdService.getByCaseId(Long.valueOf(caseData.getCcdCaseId()), idamService.getIdamTokens());
            caseData = sscsCaseDetails.getData();
        }
        setEvidenceIssuedFlagToYes(selectedDocument);
        setReissueFlagsToNull(caseData);
        updateCase(caseData, selectedDocument);
    }

    private List<FurtherEvidenceLetterType> getAllowedFurtherEvidenceLetterTypes(SscsCaseData caseData) {
        final boolean resendToAppellant = YesNo.isYes(caseData.getReissueArtifactUi().getResendToAppellant());
        boolean resendToRepresentative = YesNo.isYes(caseData.getReissueArtifactUi().getResendToRepresentative());

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
        ReissueArtifactUi reissueArtifactUi = sscsCaseData.getReissueArtifactUi();
        reissueArtifactUi.setReissueFurtherEvidenceDocument(null);
        reissueArtifactUi.setResendToAppellant(null);
        reissueArtifactUi.setResendToRepresentative(null);
        reissueArtifactUi.setResendToDwp(null);
        reissueArtifactUi.setOtherPartyOptions(null);
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        ReissueArtifactUi reissueArtifactUi = callback.getCaseDetails().getCaseData().getReissueArtifactUi();
        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.REISSUE_FURTHER_EVIDENCE
            && Objects.nonNull(reissueArtifactUi.getReissueFurtherEvidenceDocument())
            && Objects.nonNull(reissueArtifactUi.getReissueFurtherEvidenceDocument().getValue())
            && Objects.nonNull(reissueArtifactUi.getReissueFurtherEvidenceDocument().getValue().getCode())
            && furtherEvidenceService.canHandleAnyDocument(callback.getCaseDetails().getCaseData().getSscsDocument());
    }

    private void setEvidenceIssuedFlagToYes(AbstractDocument doc) {
        if (doc.getValue().getEvidenceIssued() != null && doc.getValue().getEvidenceIssued().equals("No")) {
            doc.getValue().setEvidenceIssued("Yes");
        }
    }

    private void updateCase(SscsCaseData caseData, AbstractDocument selectedDocument) {

        ccdService.updateCase(
            caseData,
            Long.valueOf(caseData.getCcdCaseId()),
            EventType.UPDATE_CASE_ONLY.getCcdType(),
            "Update case data only",
            determineDescription(selectedDocument),
            idamService.getIdamTokens());
    }

    public String determineDescription(AbstractDocument document) {
        final boolean hasResizedDocs = document.getValue().getResizedDocumentLink() != null;

        final String baseDescription = "Update document evidence reissued flags after re-issuing further evidence to DWP";

        return !hasResizedDocs ? baseDescription : baseDescription + " and attached resized document(s)";
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
