package uk.gov.hmcts.reform.sscs.service;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;

public class BulkPrintServiceHelperTest {
    @Mock
    private CcdNotificationsPdfService ccdNotificationsPdfService;

    private SscsCaseData appellantWantsRa;
    private SscsCaseData repWantsRa;

    @Before
    public void setUp() {
        appellantWantsRa  = SscsCaseData.builder()
            .reasonableAdjustments(ReasonableAdjustments.builder()
                .appellant(ReasonableAdjustmentDetails.builder()
                    .wantsReasonableAdjustment(YesNo.YES).build()).build())
            .build();

        repWantsRa  = SscsCaseData.builder()
            .reasonableAdjustments(ReasonableAdjustments.builder()
                .representative(ReasonableAdjustmentDetails.builder()
                    .wantsReasonableAdjustment(YesNo.YES).build()).build())
            .build();
    }

    @Test
    public void testForAppellantWhoWantsAdjustmentWithAppellantLetter() {
        BulkPrintServiceHelper bulkPrintServiceHelper =
            new BulkPrintServiceHelper(ccdNotificationsPdfService);

        assertTrue(bulkPrintServiceHelper.sendForReasonableAdjustMent(appellantWantsRa,
            FurtherEvidenceLetterType.APPELLANT_LETTER, EventType.ISSUE_FURTHER_EVIDENCE));
    }

    @Test
    public void testForAppellantWhoWantsAdjustmentWithRepLetter() {
        BulkPrintServiceHelper bulkPrintServiceHelper =
            new BulkPrintServiceHelper(ccdNotificationsPdfService);

        assertFalse(bulkPrintServiceHelper.sendForReasonableAdjustMent(appellantWantsRa,
            FurtherEvidenceLetterType.REPRESENTATIVE_LETTER, EventType.ISSUE_FURTHER_EVIDENCE));
    }

    @Test
    public void testForRepWhoWantsAdjustmentWithAppellantLetter() {
        BulkPrintServiceHelper bulkPrintServiceHelper =
            new BulkPrintServiceHelper(ccdNotificationsPdfService);

        assertFalse(bulkPrintServiceHelper.sendForReasonableAdjustMent(appellantWantsRa,
            FurtherEvidenceLetterType.APPELLANT_LETTER, EventType.ISSUE_FURTHER_EVIDENCE));
    }

    @Test
    public void testForRepWhoWantsAdjustmentWithRepLetter() {
        BulkPrintServiceHelper bulkPrintServiceHelper =
            new BulkPrintServiceHelper(ccdNotificationsPdfService);

        assertTrue(bulkPrintServiceHelper.sendForReasonableAdjustMent(appellantWantsRa,
            FurtherEvidenceLetterType.REPRESENTATIVE_LETTER, EventType.ISSUE_FURTHER_EVIDENCE));
    }
}
