package uk.gov.hmcts.reform.sscs.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Correspondence;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;

@Service
@Slf4j
public class CcdNotificationService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d MMM y HH:mm");
    private static final ZoneId ZONE_ID_LONDON = ZoneId.of("Europe/London");

    private static final String SENDER_TYPE = "Bulk Print";

    private final CcdNotificationsPdfService ccdNotificationsPdfService;

    public CcdNotificationService(CcdNotificationsPdfService ccdNotificationsPdfService) {
        this.ccdNotificationsPdfService = ccdNotificationsPdfService;
    }

    public void storeNotificationLetterIntoCcd(EventType eventType, byte[] pdfLetter,
                                               Long ccdCaseId) {
        var correspondence = buildCorrespondence(eventType);
        ccdNotificationsPdfService.mergeLetterCorrespondenceIntoCcd(pdfLetter, ccdCaseId, correspondence, SENDER_TYPE);
    }

    private Correspondence buildCorrespondence(EventType eventType) {
        var correspondenceDetails = CorrespondenceDetails.builder()
            .correspondenceType(CorrespondenceType.Letter)
            .eventType(eventType.getCcdType())
            .sentOn(LocalDateTime.now(ZONE_ID_LONDON).format(DATE_TIME_FORMATTER))
            .build();

        return Correspondence.builder()
            .value(correspondenceDetails)
            .build();
    }
}
