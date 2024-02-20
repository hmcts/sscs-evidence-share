package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.DocmosisTemplateConfig;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.service.BulkPrintService;
import uk.gov.hmcts.reform.sscs.service.CoverLetterService;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.service.placeholders.SorPlaceholderService;

@Slf4j
@Service
public class SorWriteHandler implements CallbackHandler<SscsCaseData> {
    public static final String POST_HEARING_APP_SOR_WRITTEN = "postHearingAppSorWritten";
    private String docmosisTemplate;

    private String docmosisCoverSheetTemplate;

    private final DocmosisTemplateConfig docmosisTemplateConfig;

    private final SorPlaceholderService sorPlaceholderService;

    private final BulkPrintService bulkPrintService;

    private final CoverLetterService coverLetterService;

    private final PdfStoreService pdfStoreService;

    @Autowired
    public SorWriteHandler(DocmosisTemplateConfig docmosisTemplateConfig, SorPlaceholderService sorPlaceholderService,
                           BulkPrintService bulkPrintService, CoverLetterService coverLetterService, PdfStoreService pdfStoreService) {
        this.docmosisTemplateConfig = docmosisTemplateConfig;
        this.sorPlaceholderService = sorPlaceholderService;
        this.bulkPrintService = bulkPrintService;
        this.coverLetterService = coverLetterService;
        this.pdfStoreService = pdfStoreService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.POST_HEARING_APP_SOR_WRITTEN;
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            log.info("Cannot handle this event for case id: {}", callback.getCaseDetails().getId());
            throw new IllegalStateException("Cannot handle callback");
        }

        sendLetters(callback.getCaseDetails().getCaseData());
    }

    private void sendLetters(SscsCaseData caseData) {
        var template = docmosisTemplateConfig.getTemplate();
        LanguagePreference languagePreference = caseData.getLanguagePreference();
        docmosisTemplate = template.get(languagePreference).get(POST_HEARING_APP_SOR_WRITTEN).get("name");
        docmosisCoverSheetTemplate = template.get(languagePreference).get(POST_HEARING_APP_SOR_WRITTEN).get("cover");

        List<Entity> parties = getParties(caseData);

        parties.forEach(party -> {
            var letterType = getLetterType(party);
            var partyId = party instanceof OtherParty ? party.getId() : null;
            var placeholders = sorPlaceholderService.populatePlaceholders(caseData,
                letterType,
                party.getClass().getSimpleName(),
                partyId);

            String letterName = String.format(LETTER_NAME, placeholders.get(ADDRESS_NAME), LocalDateTime.now());

            var generatedPdf = coverLetterService.generateCoverLetterRetry(letterType,
                docmosisTemplate, letterName, placeholders, 1);

            var coverSheet = coverLetterService.generateCoverSheet(docmosisCoverSheetTemplate,
                "coversheet", placeholders);

            final byte[] caseDocument = downloadDocument(caseData);

            var bundledLetter = bulkPrintService.buildBundledLetter(caseDocument, generatedPdf);
            bundledLetter = bulkPrintService.buildBundledLetter(coverSheet, bundledLetter);

            Pdf pdf = new Pdf(bundledLetter, letterName);
            List<Pdf> letter = new ArrayList<>();
            letter.add(pdf);

            String recipient = party.getName().getFullNameNoTitle();
            log.info("Party {} {}", party, party.getName());
            log.info("Sending letter to {}", recipient);
            log.info("Appellant name {} entity type {} name {}",
                placeholders.get(APPELLANT_NAME),
                placeholders.get(ENTITY_TYPE),
                placeholders.get(NAME));
            bulkPrintService.sendToBulkPrint(Long.parseLong(caseData.getCcdCaseId()), caseData, letter,
                EventType.POST_HEARING_APP_SOR_WRITTEN, recipient);
        });
    }

    private FurtherEvidenceLetterType getLetterType(Entity party) {
        if (party instanceof Appellant) {
            return FurtherEvidenceLetterType.APPELLANT_LETTER;
        } else if (party instanceof Appointee) {
            return FurtherEvidenceLetterType.APPELLANT_LETTER;
        } else if (party instanceof Representative) {
            return FurtherEvidenceLetterType.REPRESENTATIVE_LETTER;
        } else if (party instanceof OtherParty) {
            return FurtherEvidenceLetterType.OTHER_PARTY_LETTER;
        }
        return FurtherEvidenceLetterType.JOINT_PARTY_LETTER;
    }

    private List<Entity> getParties(SscsCaseData caseData) {
        var parties = new ArrayList<Entity>();

        Appeal appeal = caseData.getAppeal();
        if (isYes(appeal.getAppellant().getIsAppointee())) {
            parties.add(appeal.getAppellant().getAppointee());
        } else {
            parties.add(appeal.getAppellant());
        }

        if (caseData.isThereAJointParty()) {
            parties.add(caseData.getJointParty());
        }

        if (!isNull(appeal.getRep()) && isYes(appeal.getRep().getHasRepresentative())) {
            parties.add(appeal.getRep());
        }

        if (!isNull(caseData.getOtherParties()) && !caseData.getOtherParties().isEmpty()) {
            caseData.getOtherParties()
                .stream()
                .filter(p -> !isNull(p.getValue()))
                .map(CcdValue::getValue)
                .forEach(party -> {
                    if (party.hasAppointee()) {
                        parties.add(party.getAppointee());
                    } else {
                        parties.add(party);
                    }

                    if (party.hasRepresentative()) {
                        parties.add(party.getRep());
                    }
                });
        }

        return parties;
    }

    private byte[] downloadDocument(SscsCaseData caseData) {
        byte[] caseDocument = null;
        SscsDocument document = caseData.getLatestDocumentForDocumentType(DocumentType.STATEMENT_OF_REASONS);

        String documentUrl = document.getValue()
            .getDocumentLink().getDocumentUrl();

        if (null != documentUrl) {
            caseDocument = pdfStoreService.download(documentUrl);
        }
        return caseDocument;
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
