package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.when;

import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;

@RunWith(MockitoJUnitRunner.class)
public class BulkPrintServiceHelperTest {
    @Mock
    private CcdNotificationsPdfService ccdNotificationsPdfService;

    BulkPrintServiceHelper bulkPrintServiceHelper;

    private SscsCaseData appellantWantsRa;
    private SscsCaseData repWantsRa;
    List<Pdf> pdfs;
    @Before
    public void setUp() {
        bulkPrintServiceHelper =
            new BulkPrintServiceHelper(ccdNotificationsPdfService);

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

        pdfs = new ArrayList<>();
        Pdf pdf = new Pdf(new byte[]{0,0,0,0}, "document");
        pdfs.add(pdf);
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

    @Test
    public void testSaveAsReasonableAdjustment() {
        bulkPrintServiceHelper.saveAsReasonableAdjustment(SscsCaseData.builder()
            .ccdCaseId("111111111111111")
                .appeal(Appeal.builder()
                    .appellant(Appellant.builder()
                        .name(new Name("Mr", "Jimmy", "Greg")).build()).build()).build(),
            pdfs, FurtherEvidenceLetterType.APPELLANT_LETTER, EventType.ISSUE_FURTHER_EVIDENCE);
    }
}
