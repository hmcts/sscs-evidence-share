package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment.*;

import java.net.URI;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.config.EvidenceShareConfig;
import uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment;
import uk.gov.hmcts.reform.sscs.domain.email.RoboticsEmailTemplate;
import uk.gov.hmcts.reform.sscs.model.AirlookupBenefitToVenue;
import uk.gov.hmcts.reform.sscs.robotics.domain.RoboticsWrapper;
import uk.gov.hmcts.reform.sscs.robotics.json.RoboticsJsonMapper;
import uk.gov.hmcts.reform.sscs.robotics.json.RoboticsJsonValidator;

@Component
@Slf4j
public class RoboticsService {

    private final RegionalProcessingCenterService regionalProcessingCenterService;

    private final EvidenceManagementService evidenceManagementService;

    private static final String GLASGOW = "GLASGOW";
    private final AirLookupService airLookupService;
    private final EmailService emailService;
    private final RoboticsJsonMapper roboticsJsonMapper;
    private final RoboticsJsonValidator roboticsJsonValidator;
    private final RoboticsEmailTemplate roboticsEmailTemplate;
    private final EvidenceShareConfig evidenceShareConfig;

    @Autowired
    public RoboticsService(
        RegionalProcessingCenterService regionalProcessingCenterService,
        EvidenceManagementService evidenceManagementService,
        AirLookupService airLookupService,
        EmailService emailService,
        RoboticsJsonMapper roboticsJsonMapper,
        RoboticsJsonValidator roboticsJsonValidator,
        RoboticsEmailTemplate roboticsEmailTemplate,
        EvidenceShareConfig evidenceShareConfig
    ) {
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.evidenceManagementService = evidenceManagementService;
        this.airLookupService = airLookupService;
        this.emailService = emailService;
        this.roboticsJsonMapper = roboticsJsonMapper;
        this.roboticsJsonValidator = roboticsJsonValidator;
        this.roboticsEmailTemplate = roboticsEmailTemplate;
        this.evidenceShareConfig = evidenceShareConfig;
    }

    public void sendCaseToRobotics(SscsCaseData caseData) {

        String firstHalfOfPostcode = regionalProcessingCenterService.getFirstHalfOfPostcode(caseData.getAppeal().getAppellant().getAddress().getPostcode());

        byte[] sscs1Form = downloadSscs1(caseData, Long.valueOf(caseData.getCcdCaseId()));
        Map<String, byte[]> additionalEvidence = downloadEvidence(caseData, Long.valueOf(caseData.getCcdCaseId()));

        processRobotics(caseData, Long.valueOf(caseData.getCcdCaseId()), firstHalfOfPostcode, sscs1Form, additionalEvidence);
    }

    private byte[] downloadSscs1(SscsCaseData sscsCaseData, Long caseId) {
        if (hasEvidence(sscsCaseData)) {
            for (SscsDocument doc : sscsCaseData.getSscsDocument()) {
                if (doc.getValue().getDocumentType() != null && doc.getValue().getDocumentType().equalsIgnoreCase("sscs1")) {
                    return downloadBinary(doc, caseId);
                }
            }
        }
        return null;
    }

    private Map<String, byte[]> downloadEvidence(SscsCaseData sscsCaseData, Long caseId) {
        if (hasEvidence(sscsCaseData) && !isEvidenceSentForBulkPrint(sscsCaseData)) {
            Map<String, byte[]> map = new LinkedHashMap<>();
            for (SscsDocument doc : sscsCaseData.getSscsDocument()) {
                if (doc.getValue().getDocumentType() == null || doc.getValue().getDocumentType().equalsIgnoreCase("appellantEvidence")) {
                    map.put(doc.getValue().getDocumentFileName(), downloadBinary(doc, caseId));
                }
            }
            return map;
        } else {
            return Collections.emptyMap();
        }
    }

    private boolean isEvidenceSentForBulkPrint(SscsCaseData caseData) {
        return nonNull(caseData)
            && nonNull(caseData.getAppeal())
            && nonNull(caseData.getAppeal().getReceivedVia())
            && evidenceShareConfig.getSubmitTypes().stream()
            .anyMatch(caseData.getAppeal().getReceivedVia()::equalsIgnoreCase);
    }

    private byte[] downloadBinary(SscsDocument doc, Long caseId) {
        log.info("About to download binary to attach to robotics for caseId {}", caseId);
        if (doc.getValue().getDocumentLink() != null) {
            return evidenceManagementService.download(URI.create(doc.getValue().getDocumentLink().getDocumentUrl()), null);
        } else {
            return new byte[0];
        }
    }

    private boolean hasEvidence(SscsCaseData sscsCaseData) {
        return CollectionUtils.isNotEmpty(sscsCaseData.getSscsDocument());
    }

    public JSONObject processRobotics(SscsCaseData caseData, Long caseId, String postcode, byte[] pdf, Map<String, byte[]> additionalEvidence) {
        AirlookupBenefitToVenue venue = airLookupService.lookupAirVenueNameByPostCode(postcode);

        String venueName = caseData.getAppeal().getBenefitType().getCode().equalsIgnoreCase("esa") ? venue.getEsaVenue() : venue.getPipVenue();

        JSONObject roboticsJson = createRobotics(RoboticsWrapper.builder().sscsCaseData(caseData)
            .ccdCaseId(caseId).venueName(venueName).evidencePresent(caseData.getEvidencePresent()).build());

        log.info("Case {} Robotics JSON successfully created for benefit type {}", caseId,
            caseData.getAppeal().getBenefitType().getCode());

        boolean isScottish = Optional.ofNullable(caseData.getRegionalProcessingCenter()).map(f -> equalsIgnoreCase(f.getName(), GLASGOW)).orElse(false);
        sendJsonByEmail(caseData.getAppeal().getAppellant(), roboticsJson, pdf, additionalEvidence, isScottish);
        log.info("Case {} Robotics JSON email sent successfully for benefit type {} isScottish {}", caseId,
            caseData.getAppeal().getBenefitType().getCode(), isScottish);

        return roboticsJson;
    }

    public JSONObject createRobotics(RoboticsWrapper appeal) {

        JSONObject roboticsAppeal = roboticsJsonMapper.map(appeal);

        roboticsJsonValidator.validate(roboticsAppeal);

        return roboticsAppeal;
    }

    private void sendJsonByEmail(Appellant appellant, JSONObject json, byte[] pdf, Map<String, byte[]> additionalEvidence, boolean isScottish) {
        log.info("Generating unique email id");
        String appellantUniqueId = emailService.generateUniqueEmailId(appellant);
        log.info("Add default attachments");
        List<EmailAttachment> attachments = addDefaultAttachment(json, pdf, appellantUniqueId);
        log.info("Add additional evidence");
        addAdditionalEvidenceAttachments(additionalEvidence, attachments);
        log.info("Send email");
        emailService.sendEmail(
            roboticsEmailTemplate.generateEmail(
                appellantUniqueId,
                attachments,
                isScottish
            )
        );
    }

    private void addAdditionalEvidenceAttachments(Map<String, byte[]> additionalEvidence, List<EmailAttachment> attachments) {
        for (String filename : additionalEvidence.keySet()) {
            if (filename != null) {
                byte[] content = additionalEvidence.get(filename);
                if (content != null) {
                    attachments.add(file(content, filename));
                }
            }
        }
    }

    private List<EmailAttachment> addDefaultAttachment(JSONObject json, byte[] pdf, String appellantUniqueId) {
        List<EmailAttachment> emailAttachments = new ArrayList<>();

        emailAttachments.add(json(json.toString().getBytes(), appellantUniqueId + ".txt"));

        if (pdf != null) {
            emailAttachments.add(pdf(pdf, appellantUniqueId + ".pdf"));
        }

        return emailAttachments;
    }
}
