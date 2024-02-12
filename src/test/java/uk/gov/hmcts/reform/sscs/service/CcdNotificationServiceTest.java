package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.Correspondence;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;

@ExtendWith(MockitoExtension.class)
class CcdNotificationServiceTest {

    @Mock
    CcdNotificationsPdfService ccdNotificationsPdfService;

    @Test
    void verifyCcdNotificationsPdfServiceCall() {
        CcdNotificationService ccdNotificationService = new CcdNotificationService(ccdNotificationsPdfService);
        byte[] letter = new byte[1];
        var event = EventType.ISSUE_GENERIC_LETTER;
        var recipient = "Test";
        Long caseId = 0L;
        String senderType = "Bulk Print";

        ccdNotificationService.storeNotificationLetterIntoCcd(event, letter, caseId, recipient);
        verify(ccdNotificationsPdfService, times(1))
            .mergeLetterCorrespondenceIntoCcd(eq(letter), eq(caseId), any(), eq(senderType));
    }

    @Test
    void verifyToFieldOnCorrespondenceForSorLetter() {
        CcdNotificationService ccdNotificationService = new CcdNotificationService(ccdNotificationsPdfService);
        EventType eventType = EventType.SOR_WRITE;
        String recipient = "Test name";

        Correspondence correspondence = ccdNotificationService.buildCorrespondence(eventType, recipient);

        assertEquals(recipient, correspondence.getValue().getTo());
        assertEquals(eventType.getCcdType(), correspondence.getValue().getEventType());
    }
}
