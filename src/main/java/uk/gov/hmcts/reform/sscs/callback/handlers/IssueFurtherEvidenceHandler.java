package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.callback.handlers.HandlerUtils.distinctByKey;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.*;

import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.exception.IssueFurtherEvidenceException;
import uk.gov.hmcts.reform.sscs.exception.PostIssueFurtherEvidenceTasksException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.FurtherEvidenceService;

@Service
@Slf4j
public class IssueFurtherEvidenceHandler implements CallbackHandler<SscsCaseData> {

    private final FurtherEvidenceService furtherEvidenceService;
    private final CcdService ccdService;
    private final IdamService idamService;
    private final UpdateCcdCaseService updateCcdCaseService;
    private static final String UPDATE_CASE_ONLY_SUMMARY = "Update case data";
    @Value("${feature.issue-further-evidence-handler-v2:false}")
    boolean issueFurtherEvidenceHandlerV2;

    @Autowired
    public IssueFurtherEvidenceHandler(FurtherEvidenceService furtherEvidenceService, CcdService ccdService,
                                       IdamService idamService, UpdateCcdCaseService updateCcdCaseService) {
        this.furtherEvidenceService = furtherEvidenceService;
        this.ccdService = ccdService;
        this.idamService = idamService;
        this.updateCcdCaseService = updateCcdCaseService;
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
        String caseId = callback.getCaseDetails().getCaseData().getCcdCaseId();


        if (issueFurtherEvidenceHandlerV2) {
            Long caseIdLong = Long.parseLong(caseId);
            log.info("STARTING Issue Further Evidence Handler V2 for caseId {}", caseIdLong);
            updateCcdCaseService.updateCaseV2(caseIdLong, EventType.UPDATE_CASE_ONLY.getCcdType(),
                idamService.getIdamTokens(), caseData -> {
                    log.info("HmctsDwpState is {} at pre mutator", caseData.getHmctsDwpState());
                    if (caseData.getAppeal() != null) {
                        if (caseData.getAppeal().getAppellant() != null) {
                            if (caseData.getAppeal().getAppellant().getName() != null) {
                                log.info("Appelant.Name is {}", caseData.getAppeal().getAppellant().getName());
                            } else {
                                log.info("Appelant.Name is null");
                            }
                        } else {
                            log.info("Appelant is null");
                        }
                    } else {
                        log.info("Appeal is null");
                    }

                    issueFurtherEvidence(caseData);
                    String description = postIssueFurtherEvidenceTasks(caseData);
                    log.info("HmctsDwpState is {} at post mutator", caseData.getHmctsDwpState());
                    return new UpdateResult(UPDATE_CASE_ONLY_SUMMARY, description);
                });
            log.info("FINISHED Issue Further Evidence Handler V2 for caseId {}", caseIdLong);
        } else {
            log.info("Handling with Issue Further Evidence Handler for caseId {}", caseId);

            SscsCaseData caseData = callback.getCaseDetails().getCaseData();
            issueFurtherEvidence(caseData);
            postIssueFurtherEvidenceTasks(caseData);
        }
    }

    private void issueFurtherEvidence(SscsCaseData caseData) {
        log.info("STARTING issueFurtherEvidence");
        List<DocumentType> documentTypes = Arrays.asList(APPELLANT_EVIDENCE, REPRESENTATIVE_EVIDENCE, DWP_EVIDENCE, JOINT_PARTY_EVIDENCE, HMCTS_EVIDENCE);
        List<FurtherEvidenceLetterType> allowedLetterTypes = Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, JOINT_PARTY_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER);

        documentTypes.forEach(documentType -> issueEvidencePerDocumentType(caseData, allowedLetterTypes, documentType, null));
        issueFurtherEvidenceForEachOtherPartyThatIsOriginalSender(caseData, allowedLetterTypes);
    }

    private void issueFurtherEvidenceForEachOtherPartyThatIsOriginalSender(SscsCaseData caseData, List<FurtherEvidenceLetterType> allowedLetterTypes) {
        List<SscsDocument> groupedOtherPartyDocuments = findUniqueOtherPartyDocumentsByOtherPartyId(caseData.getSscsDocument());

        if (groupedOtherPartyDocuments != null && !groupedOtherPartyDocuments.isEmpty()) {
            groupedOtherPartyDocuments.forEach(doc -> issueEvidencePerDocumentType(caseData, allowedLetterTypes, DocumentType.fromValue(doc.getValue().getDocumentType()), doc.getValue().getOriginalSenderOtherPartyId()));
        }
    }

    private List<SscsDocument> findUniqueOtherPartyDocumentsByOtherPartyId(List<SscsDocument> sscsDocuments) {
        // We need to find all the unissued other party documents for a given other party by Id, so we can iterate through and issue evidence for each other party that has unissued evidence.
        // To prevent multiple documents going to the same other party, we only need to find one distinct document as the document type and original sender id is all we care about here and they will always be the same if there were multiple documents for the same other party.
        // Further down the line, in the FurtherEvidenceService, we work out what documents to actually issue out.
        return sscsDocuments.stream()
            .filter(doc -> OTHER_PARTY_EVIDENCE.getValue().equals(doc.getValue().getDocumentType()) || OTHER_PARTY_REPRESENTATIVE_EVIDENCE.getValue().equals(doc.getValue().getDocumentType()))
            .filter(d -> "No".equals(d.getValue().getEvidenceIssued()))
            .filter(distinctByKey(p -> p.getValue().getOriginalSenderOtherPartyId()))
            .collect(Collectors.toList());
    }

    private void issueEvidencePerDocumentType(SscsCaseData caseData, List<FurtherEvidenceLetterType> allowedLetterTypes,
                                                 DocumentType documentType, String otherPartyOriginalSenderId) {
        try {
            log.info("Issuing for {} for caseId {}", documentType.getValue(), Long.parseLong(caseData.getCcdCaseId()));
            furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, documentType, allowedLetterTypes, otherPartyOriginalSenderId);
        } catch (Exception e) {
            log.info("Failed sending further evidence for caseId {}", Long.parseLong(caseData.getCcdCaseId()));
            handleIssueFurtherEvidenceException(caseData);
            String errorMsg = "Failed sending further evidence for case(%s)...";
            throw new IssueFurtherEvidenceException(String.format(errorMsg, caseData.getCcdCaseId()), e);
        }
        log.info("Issued for caseId {}", Long.parseLong(caseData.getCcdCaseId()));
    }

    private String postIssueFurtherEvidenceTasks(SscsCaseData caseData) {
        log.debug("Post Issue Tasks for caseId {}", caseData.getCcdCaseId());
        try {
            if (caseData.getReasonableAdjustmentsLetters() != null) {
                final SscsCaseDetails sscsCaseDetails = ccdService.getByCaseId(Long.valueOf(caseData.getCcdCaseId()), idamService.getIdamTokens());
                caseData = sscsCaseDetails.getData();
            }

            final String description = determineDescription(caseData.getSscsDocument());

            setEvidenceIssuedFlagToYes(caseData.getSscsDocument());

            if (issueFurtherEvidenceHandlerV2) {
                return description;
            } else {
                ccdService.updateCase(caseData, Long.valueOf(caseData.getCcdCaseId()),
                    EventType.UPDATE_CASE_ONLY.getCcdType(),
                    UPDATE_CASE_ONLY_SUMMARY,
                    description,
                    idamService.getIdamTokens());
            }
        } catch (Exception e) {
            String errorMsg = "Failed to update document evidence issued flags after issuing further evidence "
                + "for case(%s)";
            throw new PostIssueFurtherEvidenceTasksException(String.format(errorMsg, caseData.getCcdCaseId()), e);
        }
        return null;
    }

    public String determineDescription(List<SscsDocument> documents) {
        final boolean hasResizedDocs = documents.stream().anyMatch(document ->
            document.getValue().getResizedDocumentLink() != null && document.getValue().getEvidenceIssued().equals(NO.getValue())
        );

        final String baseDescription = "Update issued evidence document flags after issuing further evidence";

        return !hasResizedDocs ? baseDescription : baseDescription + " and attached resized document(s)";
    }

    private void handleIssueFurtherEvidenceException(SscsCaseData caseData) {
        log.info("Handling IssueFurtherEvidenceException for caseId {}", Long.parseLong(caseData.getCcdCaseId()));
        caseData.setHmctsDwpState("failedSendingFurtherEvidence");
        ccdService.updateCase(caseData, Long.valueOf(caseData.getCcdCaseId()),
            EventType.SEND_FURTHER_EVIDENCE_ERROR.getCcdType(), "Failed to issue further evidence",
            "Review document tab to see document(s) that haven't been issued, then use the"
                + " \"Reissue further evidence\" within next step and select affected document(s) to re-send",
            idamService.getIdamTokens());
    }

    private void setEvidenceIssuedFlagToYes(List<SscsDocument> sscsDocuments) {
        if (sscsDocuments != null) {
            for (SscsDocument doc : sscsDocuments) {
                if (doc.getValue() != null && doc.getValue().getEvidenceIssued() != null
                    && "No".equalsIgnoreCase(doc.getValue().getEvidenceIssued())) {
                    doc.getValue().setEvidenceIssued(YES.getValue());
                }
            }
        }
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
