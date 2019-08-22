package uk.gov.hmcts.reform.sscs.service;

import static java.lang.String.format;
import static java.util.Base64.getEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.api.LetterWithPdfsRequest;
import uk.gov.hmcts.reform.sendletter.api.SendLetterApi;
import uk.gov.hmcts.reform.sendletter.api.SendLetterResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.exception.BulkPrintException;
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

    @Autowired
    public BulkPrintService(SendLetterApi sendLetterApi,
                            IdamService idamService,
                            @Value("${send-letter.enabled}") boolean sendLetterEnabled,
                            @Value("${send-letter.maxRetryAttempts}")Integer maxRetryAttempts) {
        this.idamService = idamService;
        this.sendLetterApi = sendLetterApi;
        this.sendLetterEnabled = sendLetterEnabled;
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public Optional<UUID> sendToBulkPrint(List<Pdf> pdfs, final SscsCaseData sscsCaseData)
        throws BulkPrintException {
        if (sendLetterEnabled) {
            List<String> encodedData = new ArrayList<>();
            for (Pdf pdf : pdfs) {
                encodedData.add(getEncoder().encodeToString(pdf.getContent()));
            }
            final String authToken = idamService.generateServiceAuthorization();
            return sendLetterWithRetry(authToken, sscsCaseData, encodedData, 1);
        }
        return Optional.empty();
    }

    private Optional<UUID> sendLetterWithRetry(String authToken, SscsCaseData sscsCaseData, List<String> encodedData,
                                               Integer reTryNumber) {
        try {
            return sendLetter(authToken, sscsCaseData, encodedData);
        } catch (Exception e) {
            if (reTryNumber > maxRetryAttempts) {
                String message = format("Failed to send to bulk print for case %s with error %s.",
                    sscsCaseData.getCcdCaseId(), e.getMessage());
                throw new BulkPrintException(message, e);
            }
            log.info(String.format("Caught recoverable error %s, retrying %s out of %s",
                e.getMessage(), reTryNumber, maxRetryAttempts));
            return sendLetterWithRetry(authToken, sscsCaseData, encodedData, reTryNumber + 1);
        }
    }

    private Optional<UUID> sendLetter(String authToken, SscsCaseData sscsCaseData, List<String> encodedData) {
        SendLetterResponse sendLetterResponse = sendLetterApi.sendLetter(
            authToken,
            new LetterWithPdfsRequest(
                encodedData,
                XEROX_TYPE_PARAMETER,
                getAdditionalData(sscsCaseData)
            )
        );
        log.info("Letter service produced the following letter Id {} for case {}",
            sendLetterResponse.letterId, sscsCaseData.getCcdCaseId());

        return Optional.of(sendLetterResponse.letterId);
    }

    private static Map<String, Object> getAdditionalData(final SscsCaseData sscsCaseData) {
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put(LETTER_TYPE_KEY, "sscs-data-pack");
        additionalData.put(CASE_IDENTIFIER, sscsCaseData.getCcdCaseId());
        additionalData.put(APPELLANT_NAME, sscsCaseData.getAppeal().getAppellant().getName().getFullNameNoTitle());
        return additionalData;
    }
}
