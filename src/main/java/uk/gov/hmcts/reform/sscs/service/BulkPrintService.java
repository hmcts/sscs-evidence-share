package uk.gov.hmcts.reform.sscs.service;

import static java.lang.String.format;
import static java.util.Base64.getEncoder;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.reform.sendletter.api.LetterWithPdfsRequest;
import uk.gov.hmcts.reform.sendletter.api.SendLetterApi;
import uk.gov.hmcts.reform.sendletter.api.SendLetterResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.exception.BulkPrintException;
import uk.gov.hmcts.reform.sscs.exception.NonPdfBulkPrintException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "send-letter", name = "url")
public class BulkPrintService implements PrintService {

    private static final String XEROX_TYPE_PARAMETER = "SSCS001";
    private static final String CASE_IDENTIFIER = "caseIdentifier";
    private static final String LETTER_TYPE_KEY = "letterType";
    private static final String APPELLANT_NAME = "appellantName";

    private final SendLetterApi sendLetterApi;
    private final IdamService idamService;
    private final boolean sendLetterEnabled;
    private final Integer maxRetryAttempts;
    private final BulkPrintServiceHelper bulkPrintServiceHelper;

    @Autowired
    public BulkPrintService(SendLetterApi sendLetterApi,
                            IdamService idamService,
                            BulkPrintServiceHelper bulkPrintServiceHelper,
                            @Value("${send-letter.enabled}") boolean sendLetterEnabled,
                            @Value("${send-letter.maxRetryAttempts}")Integer maxRetryAttempts) {
        this.idamService = idamService;
        this.bulkPrintServiceHelper = bulkPrintServiceHelper;
        this.sendLetterApi = sendLetterApi;
        this.sendLetterEnabled = sendLetterEnabled;
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public Optional<UUID> sendToBulkPrint(List<Pdf> pdfs, final SscsCaseData sscsCaseData, FurtherEvidenceLetterType letterType, EventType event) {
        if (bulkPrintServiceHelper.sendForReasonableAdjustment(sscsCaseData, letterType)) {
            log.info("Sending to bulk print service {} reasonable adjustments", sscsCaseData.getCcdCaseId());
            bulkPrintServiceHelper.saveAsReasonableAdjustment(sscsCaseData, pdfs, letterType, event);
        } else {
            return sendToBulkPrint(pdfs, sscsCaseData, event);
        }

        return Optional.empty();
    }

    public Optional<UUID> sendToBulkPrint(List<Pdf> pdfs, final SscsCaseData sscsCaseData, EventType eventType)
        throws BulkPrintException {
        if (sendLetterEnabled) {
            List<String> encodedData = new ArrayList<>();
            for (Pdf pdf : pdfs) {
                encodedData.add(getEncoder().encodeToString(pdf.getContent()));
            }
            final String authToken = idamService.generateServiceAuthorization();
            return sendLetterWithRetry(authToken, sscsCaseData, encodedData, 1, eventType);
        }
        return Optional.empty();
    }

    private Optional<UUID> sendLetterWithRetry(String authToken, SscsCaseData sscsCaseData, List<String> encodedData,
                                               Integer reTryNumber, EventType eventType) {
        try {
            return sendLetter(authToken, sscsCaseData, encodedData, eventType);
        } catch (HttpClientErrorException e) {
            log.info(format("Failed to send to bulk print for case %s with error %s. Non-pdf's/broken pdf's seen in list of documents, please correct.",
                sscsCaseData.getCcdCaseId(), e.getMessage()));
            throw new NonPdfBulkPrintException(e);

        } catch (Exception e) {

            if (reTryNumber > maxRetryAttempts) {
                String message = format("Failed to send to bulk print for case %s with error %s.",
                    sscsCaseData.getCcdCaseId(), e.getMessage());
                throw new BulkPrintException(message, e);
            }
            log.info(String.format("Caught recoverable error %s, retrying %s out of %s",
                e.getMessage(), reTryNumber, maxRetryAttempts));
            return sendLetterWithRetry(authToken, sscsCaseData, encodedData, reTryNumber + 1, eventType);
        }
    }

    private Optional<UUID> sendLetter(String authToken, SscsCaseData sscsCaseData, List<String> encodedData, EventType eventType) {
        SendLetterResponse sendLetterResponse = sendLetterApi.sendLetter(
            authToken,
            new LetterWithPdfsRequest(
                encodedData,
                XEROX_TYPE_PARAMETER,
                getAdditionalData(sscsCaseData, eventType)
            )
        );
        log.info("Letter service produced the following letter Id {} for case {}",
            sendLetterResponse.letterId, sscsCaseData.getCcdCaseId());

        return Optional.of(sendLetterResponse.letterId);
    }

    private static Map<String, Object> getAdditionalData(final SscsCaseData sscsCaseData, EventType eventType) {
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put(LETTER_TYPE_KEY, "sscs-data-pack");
        additionalData.put(CASE_IDENTIFIER, sscsCaseData.getCcdCaseId());
        additionalData.put(APPELLANT_NAME, sscsCaseData.getAppeal().getAppellant().getName().getFullNameNoTitle());
        additionalData.put("recipients", getPartiesOnTheCase(sscsCaseData));
        return additionalData;
    }

    private static List<String> getPartiesOnTheCase(SscsCaseData caseData) {
        Appeal appeal = caseData.getAppeal();
        Appellant appellant = appeal.getAppellant();

        // possible parties: Appellant, Appointee, Joint Party, Representative, Other Party, Other Party Rep and Other Party Appointee

        List<String> parties = new ArrayList<>();

        parties.add(appellant.getName().getFullNameNoTitle());
        if (nonNull(appellant.getAppointee()) && isYes(appellant.getIsAppointee())) {
            parties.add(appellant.getAppointee().getName().getFullNameNoTitle());
        }
        if (caseData.isThereAJointParty()) {
            parties.add(caseData.getJointParty().getName().getFullNameNoTitle());
        }
        if (caseData.isThereARepresentative()) {
            parties.add(appeal.getRep().getName().getFullNameNoTitle());
        }


        List<CcdValue<OtherParty>> otherParties = caseData.getOtherParties();
        if (nonNull(otherParties)) {
            for (CcdValue<OtherParty> ccdOtherParty : otherParties) {
                OtherParty otherParty = ccdOtherParty.getValue();
                parties.add(otherParty.getName().getFullNameNoTitle());

                if (otherParty.hasRepresentative()) {
                    parties.add(otherParty.getRep().getName().getFullNameNoTitle());
                }
                if (otherParty.hasAppointee()) {
                    parties.add(otherParty.getAppointee().getName().getFullNameNoTitle());
                }
            }
        }

        return parties;
    }
}
