package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;

public class BulkPrintServiceHelperTest {
    @Mock
    private CcdNotificationsPdfService ccdNotificationsPdfService;

    BulkPrintServiceHelper bulkPrintServiceHelper;

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

        bulkPrintServiceHelper =
            new BulkPrintServiceHelper(ccdNotificationsPdfService);
    }

    @Test
    public void testForAppellantWhoWantsAdjustmentWithAppellantLetter() {
        assertTrue(bulkPrintServiceHelper.sendForReasonableAdjustMent(appellantWantsRa,
            FurtherEvidenceLetterType.APPELLANT_LETTER, EventType.ISSUE_FURTHER_EVIDENCE));
    }

    @Test
    public void testForAppellantWhoWantsAdjustmentWithRepLetter() {
        assertFalse(bulkPrintServiceHelper.sendForReasonableAdjustMent(appellantWantsRa,
            FurtherEvidenceLetterType.REPRESENTATIVE_LETTER, EventType.ISSUE_FURTHER_EVIDENCE));
    }

    @Test
    public void testForRepWhoWantsAdjustmentWithAppellantLetter() {
        assertFalse(bulkPrintServiceHelper.sendForReasonableAdjustMent(repWantsRa,
            FurtherEvidenceLetterType.APPELLANT_LETTER, EventType.ISSUE_FURTHER_EVIDENCE));
    }

    @Test
    public void testForRepWhoWantsAdjustmentWithRepLetter() {
        assertTrue(bulkPrintServiceHelper.sendForReasonableAdjustMent(repWantsRa,
            FurtherEvidenceLetterType.REPRESENTATIVE_LETTER, EventType.ISSUE_FURTHER_EVIDENCE));
    }
}
