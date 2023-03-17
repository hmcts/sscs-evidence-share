package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.ADDRESS_NAME;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.config.DocmosisTemplateConfig;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.service.CcdNotificationService;
import uk.gov.hmcts.reform.sscs.service.CoverLetterService;
import uk.gov.hmcts.reform.sscs.service.FeatureToggleService;
import uk.gov.hmcts.reform.sscs.service.PrintService;
import uk.gov.hmcts.reform.sscs.service.placeholders.GenericLetterPlaceholderService;

@Slf4j
@Service
public class IssueGenericLetterHandler implements CallbackHandler<SscsCaseData> {

    private final PrintService bulkPrintService;

    private final CcdNotificationService ccdNotificationService;

    private final GenericLetterPlaceholderService genericLetterPlaceholderService;

    private final CoverLetterService coverLetterService;

    private final DocmosisTemplateConfig docmosisTemplateConfig;

    private final FeatureToggleService featureToggleService;

    private String docmosisTemplate;

    private static String LETTER_NAME = "Generic Letter to %s %s.pdf";


    @Autowired
    public IssueGenericLetterHandler(PrintService bulkPrintService,
                                     GenericLetterPlaceholderService genericLetterPlaceholderService,
                                     CoverLetterService coverLetterService,
                                     CcdNotificationService ccdNotificationService,
                                     DocmosisTemplateConfig docmosisTemplateConfig,
                                     FeatureToggleService featureToggleService) {
        this.bulkPrintService = bulkPrintService;
        this.genericLetterPlaceholderService = genericLetterPlaceholderService;
        this.coverLetterService = coverLetterService;
        this.ccdNotificationService = ccdNotificationService;
        this.docmosisTemplateConfig = docmosisTemplateConfig;
        this.featureToggleService = featureToggleService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return featureToggleService.isIssueGenericLetterEnabled() && callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.ISSUE_GENERIC_LETTER;
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            log.info("Cannot handle this event for case id: {}", callback.getCaseDetails().getId());
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        long id = callback.getCaseDetails().getId();

        docmosisTemplate = getDocmosisTemplate(caseData);

        process(id, caseData);
    }

    private void sendLetter(long caseId, SscsCaseData caseData, List<Pdf> pdfs) {
        Optional<UUID> id = bulkPrintService.sendToBulkPrint(pdfs, caseData);
        //Optional.of(UUID.randomUUID());

        Pdf letter = pdfs.get(0);

        if (id.isPresent()) {
            ccdNotificationService.storeNotificationLetterIntoCcd(EventType.ISSUE_GENERIC_LETTER, letter.getContent(),
                caseId);
            log.info("Generic letters were send for case {}, send-letter-service id {}", caseId, id.get());
        } else {
            log.error("Failed to send to bulk print for case {}. No print id returned", caseId);
        }
    }

    private String getDocmosisTemplate(SscsCaseData caseData) {
        return docmosisTemplateConfig.getTemplate().get(caseData.getLanguagePreference())
            .get("generic-letter").get("name");
    }

    private void process(long caseId, SscsCaseData caseData) {
        List<Pdf> documents = new ArrayList<>();

        if (YesNo.isYes(caseData.getAddDocuments())) {
            documents.addAll(coverLetterService.getSelectedDocuments(caseData));
        }

        if (YesNo.isYes(caseData.getSendToAllParties())) {
            sendToAllParties(caseId, caseData, documents);
            return;
        }

        if (YesNo.isYes(caseData.getSendToApellant())) {
            sendToAppellant(caseId, caseData, documents);
        }

        if (YesNo.isYes(caseData.getSendToRepresentative())) {
            sendToRepresentative(caseId, caseData, documents);
        }

        if (YesNo.isYes(caseData.getSendToJointParty())) {
            sendToJointParty(caseId, caseData, documents);
        }

        if (YesNo.isYes(caseData.getSendToOtherParties())) {
            sendToOtherParties(caseId, caseData, documents);
        }

        // TODO reasonable adjustments?
        // blank page??
    }

    private void sendToOtherParties(long caseId, SscsCaseData caseData, List<Pdf> documents) {
        var selectedOtherParties = caseData.getOtherPartySelection();
        var otherParties = caseData.getOtherParties();

        for (var party : selectedOtherParties) {
            String entityId = party.getValue().getOtherPartiesList().getValue().getCode();
            OtherParty otherParty = getOtherPartyByEntityId(entityId, otherParties);

            if (otherParty != null) {
                var letterType = getLetterType(otherParty, entityId);
                var placeholders = genericLetterPlaceholderService.populatePlaceholders(caseData,
                    letterType, entityId);

                String letterName = String.format(LETTER_NAME, placeholders.get(ADDRESS_NAME), LocalDateTime.now());

                var generatedPdf = coverLetterService.generateCoverLetterRetry(letterType,
                    docmosisTemplate, letterName, placeholders, 1);

                var pdf = new Pdf(generatedPdf, letterName);
                List<Pdf> letter = new ArrayList<>();
                letter.add(pdf);
                letter.addAll(documents);
                sendLetter(caseId, caseData, letter);
            }
        }
    }

    private FurtherEvidenceLetterType getLetterType(OtherParty otherParty, String entityId) {
        if (otherParty.hasRepresentative() && entityId.equals(otherParty.getRep().getId())) {
            return FurtherEvidenceLetterType.OTHER_PARTY_REP_LETTER;
        }

        return FurtherEvidenceLetterType.OTHER_PARTY_LETTER;
    }

    private void sendToJointParty(long caseId, SscsCaseData caseData, List<Pdf> documents) {
        var placeholders = genericLetterPlaceholderService.populatePlaceholders(caseData,
            FurtherEvidenceLetterType.JOINT_PARTY_LETTER,
            null);

        String letterName = getLetterName(placeholders);

        var generatedPdf = coverLetterService.generateCoverLetterRetry(FurtherEvidenceLetterType.JOINT_PARTY_LETTER,
            docmosisTemplate, letterName, placeholders, 1);

        Pdf pdf = new Pdf(generatedPdf, letterName);
        List<Pdf> letter = new ArrayList<>();
        letter.add(pdf);
        letter.addAll(documents);
        sendLetter(caseId, caseData, letter);
    }

    private void sendToRepresentative(long caseId, SscsCaseData caseData, List<Pdf> documents) {
        var placeholders = genericLetterPlaceholderService.populatePlaceholders(caseData,
            FurtherEvidenceLetterType.REPRESENTATIVE_LETTER,
            null);

        String letterName = getLetterName(placeholders);

        var generatedPdf = coverLetterService.generateCoverLetterRetry(FurtherEvidenceLetterType.REPRESENTATIVE_LETTER,
            docmosisTemplate, letterName, placeholders, 1);

        Pdf pdf = new Pdf(generatedPdf, letterName);
        List<Pdf> letter = new ArrayList<>();
        letter.add(pdf);
        letter.addAll(documents);
        sendLetter(caseId, caseData, letter);
    }

    private void sendToAppellant(long caseId, SscsCaseData caseData, List<Pdf> documents) {
        var placeholders = genericLetterPlaceholderService.populatePlaceholders(caseData,
            FurtherEvidenceLetterType.APPELLANT_LETTER,
            null);

        String letterName = getLetterName(placeholders);

        var generatedPdf = coverLetterService.generateCoverLetterRetry(FurtherEvidenceLetterType.APPELLANT_LETTER,
            docmosisTemplate, letterName, placeholders, 1);

        Pdf pdf = new Pdf(generatedPdf, letterName);
        List<Pdf> letter = new ArrayList<>();
        letter.add(pdf);
        letter.addAll(documents);
        sendLetter(caseId, caseData, letter);
    }

    private static String getLetterName(Map<String, Object> placeholders) {
        return String.format(LETTER_NAME, placeholders.get(ADDRESS_NAME), LocalDateTime.now());
    }

    private void sendToAllParties(long caseId, SscsCaseData caseData, List<Pdf> documents) {
        sendToAppellant(caseId, caseData, documents);

        if (caseData.isThereARepresentative()) {
            sendToRepresentative(caseId, caseData, documents);
        }

        if (caseData.isThereAJointParty()) {
            sendToJointParty(caseId, caseData, documents);
        }

        if (isNotEmpty(caseData.getOtherParties())) {
            sendToOtherParties(caseId, caseData, documents);
        }
    }

    private OtherParty getOtherPartyByEntityId(String entityId, List<CcdValue<OtherParty>> otherParties) {
        return otherParties.stream()
            .map(CcdValue::getValue)
            .filter(o -> filterByEntityID(entityId, o)).findFirst()
            .orElse(null);
    }

    private static boolean filterByEntityID(String entityId, OtherParty o) {
        return entityId.contains(o.getId())
            || (o.hasRepresentative() && entityId.contains(o.getRep().getId()))
            || (o.hasAppointee() && entityId.contains(o.getAppointee().getId()));
    }
}
