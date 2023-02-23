package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;
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
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.config.DocmosisTemplateConfig;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.CcdPdfService;
import uk.gov.hmcts.reform.sscs.service.CoverLetterService;
import uk.gov.hmcts.reform.sscs.service.PrintService;
import uk.gov.hmcts.reform.sscs.service.placeholders.GenericLetterPlaceholderService;

@Slf4j
@Service
public class IssueGenericLetterHandler implements CallbackHandler<SscsCaseData> {

    private final PrintService bulkPrintService;

    private final CcdPdfService ccdPdfService;

    private final GenericLetterPlaceholderService genericLetterPlaceholderService;

    private final CoverLetterService coverLetterService;

    private final DocmosisTemplateConfig docmosisTemplateConfig;

    private final IdamService idamService;

    private String docmosisTemplate;

    private static String LETTER_NAME = "Generic Letter to %s %s.pdf";


    @Autowired
    public IssueGenericLetterHandler(PrintService bulkPrintService,
                                     GenericLetterPlaceholderService genericLetterPlaceholderService,
                                     CoverLetterService coverLetterService,
                                     CcdPdfService ccdPdfService,
                                     IdamService idamService,
                                     DocmosisTemplateConfig docmosisTemplateConfig) {
        this.bulkPrintService = bulkPrintService;
        this.genericLetterPlaceholderService = genericLetterPlaceholderService;
        this.coverLetterService = coverLetterService;
        this.ccdPdfService = ccdPdfService;
        this.idamService = idamService;
        this.docmosisTemplateConfig = docmosisTemplateConfig;
    }


    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED) && callback.getEvent() == EventType.ISSUE_GENERIC_LETTER;
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

        docmosisTemplate = getDocmosisTemplate(caseData);

        process(caseData);
    }

    private void sendLetter(SscsCaseData caseData, List<Pdf> pdfs) {
        Optional<UUID> id = bulkPrintService.sendToBulkPrint(pdfs, caseData);//Optional.of(UUID.randomUUID());

        String caseId = caseData.getCcdCaseId();

        Pdf letter = pdfs.get(0);

        if (id.isPresent()) {
            ccdPdfService.mergeDocIntoCcd(letter.getName(), letter.getContent(), Long.valueOf(caseData.getCcdCaseId()),
                caseData, idamService.getIdamTokens(), DocumentType.OTHER_DOCUMENT.getValue());
            log.info("Generic letters were send for case {}", caseId);
        } else {
            log.error("Failed to send to bulk print for case {}. No print id returned", caseId);
        }
    }

    private String getDocmosisTemplate(SscsCaseData caseData) {
        return docmosisTemplateConfig.getTemplate().get(caseData.getLanguagePreference())
            .get("generic-letter").get("name");
    }

    private void process(SscsCaseData caseData) {
        List<Pdf> documents = new ArrayList<>();

        if (YesNo.isYes(caseData.getAddDocuments())) {
            documents.addAll(coverLetterService.getSelectedDocuments(caseData));
        }

        if (YesNo.isYes(caseData.getSendToAllParties())) {
            sendToAllParties(caseData, documents);
            return;
        }

        if (YesNo.isYes(caseData.getSendToApellant())) {
            sendToApellant(caseData, documents);
        }

        if (YesNo.isYes(caseData.getSendToRepresentative())) {
            sendToRepresentative(caseData, documents);
        }

        if (YesNo.isYes(caseData.getSendToJointParty())) {
            sendToJointParty(caseData, documents);
        }

        if (YesNo.isYes(caseData.getSendToOtherParties())) {
            sendToOtherParties(caseData, documents);
        }

        // TODO reasonable adjustments?
        // bland page??
    }

    private void sendToOtherParties(SscsCaseData caseData, List<Pdf> documents) {
        var selectedOtherParties = caseData.getOtherPartySelection();
        var otherParties = caseData.getOtherParties();

        for (var party : selectedOtherParties) {
            String entityId = party.getValue().getOtherPartiesList().getValue().getCode();
            OtherParty otherParty = getOtherPartyByEntityId(entityId, otherParties);

            if (otherParty != null) {
                var letterType = getLetterType(otherParty, entityId);
                var placeholders = genericLetterPlaceholderService.populatePlaceholders(caseData,
                    letterType, entityId);

                String letterName = String.format(LETTER_NAME, LocalDateTime.now());

                var generatedPdf = coverLetterService.generateCoverLetterRetry(letterType,
                    docmosisTemplate, letterName, placeholders, 1);

                var pdf = new Pdf(generatedPdf, letterName);
                List<Pdf> letter = new ArrayList<>();
                letter.add(pdf);
                letter.addAll(documents);
                sendLetter(caseData, letter);
            }
        }
    }

    private FurtherEvidenceLetterType getLetterType(OtherParty otherParty, String entityId) {
        if (otherParty.hasRepresentative() && entityId.equals(otherParty.getRep().getId())) {
            return FurtherEvidenceLetterType.OTHER_PARTY_REP_LETTER;
        }

        return FurtherEvidenceLetterType.OTHER_PARTY_LETTER;
    }

    private void sendToJointParty(SscsCaseData caseData, List<Pdf> documents) {
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
        sendLetter(caseData, letter);
    }

    private void sendToRepresentative(SscsCaseData caseData, List<Pdf> documents) {
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
        sendLetter(caseData, letter);
    }

    private void sendToApellant(SscsCaseData caseData, List<Pdf> documents) {
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
        sendLetter(caseData, letter);
    }

    private static String getLetterName(Map<String, Object> placeholders) {
        return String.format(LETTER_NAME, placeholders.get(ADDRESS_NAME), LocalDateTime.now());
    }

    private void sendToAllParties(SscsCaseData caseData, List<Pdf> documents) {
        sendToApellant(caseData, documents);

        if (YesNo.isYes(caseData.getHasRepresentative())) {
            sendToRepresentative(caseData, documents);
        }

        if (YesNo.isYes(caseData.getHasJointParty())) {
            sendToJointParty(caseData, documents);
        }

        if (YesNo.isYes(caseData.getHasOtherParties())) {
            sendToOtherParties(caseData, documents);
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
