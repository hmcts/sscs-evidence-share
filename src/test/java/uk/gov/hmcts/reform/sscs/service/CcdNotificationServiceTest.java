package uk.gov.hmcts.reform.sscs.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;

class CcdNotificationServiceTest {

    @Mock
    CcdNotificationsPdfService ccdNotificationsPdfService;


    @BeforeEach
    void setup() {
        openMocks(this);
    }

    @Test
    void test1() {
        CcdNotificationService ccdNotificationService = new CcdNotificationService(ccdNotificationsPdfService);

        byte[] letter = new byte[1];

        var event = EventType.ISSUE_GENERIC_LETTER;

        Long caseId = 0L;

        ccdNotificationService.storeNotificationLetterIntoCcd(event, letter, caseId);

        verify(ccdNotificationsPdfService, times(1)).mergeLetterCorrespondenceIntoCcd(eq(letter), eq(caseId), any());
    }
}
