package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment.file;
import static uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment.pdf;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocmosisPdfGenerationService;
import uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment;
import uk.gov.hmcts.reform.sscs.domain.email.RequestTranslationTemplate;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;

@Component
@Slf4j
public class RequestTranslationService {

    private final EvidenceManagementService evidenceManagementService;
    private final EmailService emailService;
    private final RequestTranslationTemplate requestTranslationTemplate;
    private DocmosisPdfGenerationService pdfGenerationService;
    private final IdamService idamService;
    private String loggedInUserEmail;

    @Autowired
    public RequestTranslationService(
        EvidenceManagementService evidenceManagementService,
        EmailService emailService,
        RequestTranslationTemplate requestTranslationTemplate,
        DocmosisPdfGenerationService pdfGenerationService,
        IdamService idamService) {
        this.evidenceManagementService = evidenceManagementService;
        this.emailService = emailService;
        this.requestTranslationTemplate = requestTranslationTemplate;
        this.pdfGenerationService = pdfGenerationService;
        this.idamService = idamService;
    }

    public void sendCaseToWlu(CaseDetails<SscsCaseData> caseDetails) {
        log.info("Case sent to wlu for case id {} ", caseDetails.getId());
        SscsCaseData caseData = caseDetails.getCaseData();
        Map<String, Object> placeholderMap = caseDataMap(caseDetails);

        log.info("Generate tranlsation request form from wlu for casedetails id {} ", caseDetails.getId());
        byte[] wluRequestForm = pdfGenerationService.generatePdf(DocumentHolder.builder()
                .template(new Template("TB-SCS-EML-ENG-00530.docx",
                        "WLU Request Form")).placeholders(placeholderMap).build());

        log.info("Downloading additional evidence for wlu for case id {} ", caseDetails.getId());
        Map<SscsDocument, byte[]> additionalEvidence = downloadEvidence(caseData, Long.valueOf(caseData.getCcdCaseId()));

        sendEmailToWlu(caseDetails.getId(), caseData, wluRequestForm, additionalEvidence);

        log.info("Case {} successfully sent for benefit type {} to wlu", caseDetails.getId(),
            caseData.getAppeal().getBenefitType().getCode());
    }

    private Map<String, Object> caseDataMap(CaseDetails<SscsCaseData> caseDetails) {
        UserDetails userDetails = idamService.getUserDetails(idamService.getIdamOauth2Token());
        Map<String, Object> dataMap = new HashMap<>();
        if (userDetails != null) {
            dataMap.put("name", String.join(" ",userDetails.getForename(), userDetails.getSurname()));
            loggedInUserEmail = userDetails.getEmail();
            dataMap.put("email", userDetails.getEmail());
            dataMap.put("telephone", userDetails.getEmail());
        }
        dataMap.put("ccdId", caseDetails.getId());
        dataMap.put("department", "SSCS Requestor");
        dataMap.put("workdescription", "Translation required");
        dataMap.put("translation", caseDetails.getCaseData().getLanguagePreference().getCode().toUpperCase());
        return dataMap;
    }

    private Map<SscsDocument, byte[]> downloadEvidence(SscsCaseData sscsCaseData, Long caseId) {
        if (hasEvidence(sscsCaseData)) {
            Map<SscsDocument, byte[]> map = new LinkedHashMap<>();
            for (SscsDocument doc : sscsCaseData.getSscsDocument()) {
                if (doc.getValue().getDocumentTranslationStatus() != null
                        && doc.getValue().getDocumentTranslationStatus().equals(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED)) {
                    if (doc.getValue().getDocumentType() != null
                            && (doc.getValue().getDocumentType().equalsIgnoreCase("appellantEvidence")
                            || doc.getValue().getDocumentType().equalsIgnoreCase("Decision Notice")
                            || doc.getValue().getDocumentType().equalsIgnoreCase("Direction Notice")
                            || doc.getValue().getDocumentType().equalsIgnoreCase("sscs1"))) {
                        doc.getValue().setDocumentTranslationStatus(SscsDocumentTranslationStatus
                                .TRANSLATION_REQUESTED);
                        map.put(doc, downloadBinary(doc, caseId));

                    }
                }
            }
            return map;
        } else {
            return Collections.emptyMap();
        }
    }

    private byte[] downloadBinary(SscsDocument doc, Long caseId) {
        log.info("About to download binary to attach to wlu for caseId {}", caseId);
        if (doc.getValue().getDocumentLink() != null) {
            return evidenceManagementService.download(URI.create(doc.getValue().getDocumentLink().getDocumentUrl()), null);
        } else {
            return new byte[0];
        }
    }

    private boolean hasEvidence(SscsCaseData sscsCaseData) {
        return CollectionUtils.isNotEmpty(sscsCaseData.getSscsDocument());
    }

    private void sendEmailToWlu(long caseId, SscsCaseData caseData, byte[] requestFormPdf,
                            Map<SscsDocument, byte[]> additionalEvidence) {

        log.info("Add request and sscs1 default attachments for case id {}", caseId);
        List<EmailAttachment> attachments = addDefaultAttachment(requestFormPdf, caseId);
        addAdditionalEvidenceAttachments(additionalEvidence, attachments);

        emailService.sendEmail(caseId, requestTranslationTemplate.generateEmail(attachments, loggedInUserEmail));

        log.info("Case {} wlu email sent successfully. for benefit type {}  ",
            caseId, caseData.getAppeal().getBenefitType().getCode());
    }

    private void addAdditionalEvidenceAttachments(Map<SscsDocument, byte[]> additionalEvidence, List<EmailAttachment> attachments) {
        for (SscsDocument sscsDocument : additionalEvidence.keySet()) {
            if (sscsDocument != null) {
                if (sscsDocument.getValue().getDocumentLink().getDocumentFilename() != null) {
                    byte[] content = additionalEvidence.get(sscsDocument);
                    if (content != null) {
                        attachments.add(file(content, sscsDocument.getValue().getDocumentLink().getDocumentFilename()));
                    }
                }
            }
        }
    }

    private List<EmailAttachment> addDefaultAttachment(byte[] requestForm, long caseId) {
        List<EmailAttachment> emailAttachments = new ArrayList<>();
        if (requestForm != null) {
            emailAttachments.add(pdf(requestForm, "RequestTranslationForm-" + caseId + ".pdf"));
        }
        return emailAttachments;
    }
}
