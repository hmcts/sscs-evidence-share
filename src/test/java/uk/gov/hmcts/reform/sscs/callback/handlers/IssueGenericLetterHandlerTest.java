package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.callback.handlers.HandlerHelper.buildTestCallbackForGivenData;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_GENERIC_LETTER;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.NON_COMPLIANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderHelper.buildJointParty;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderHelper.buildOtherParty;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherPartySelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.config.DocmosisTemplateConfig;
import uk.gov.hmcts.reform.sscs.service.CcdNotificationService;
import uk.gov.hmcts.reform.sscs.service.CoverLetterService;
import uk.gov.hmcts.reform.sscs.service.PrintService;
import uk.gov.hmcts.reform.sscs.service.placeholders.GenericLetterPlaceholderService;

@ExtendWith(MockitoExtension.class)
@Slf4j
class IssueGenericLetterHandlerTest {

    @Mock
    private GenericLetterPlaceholderService genericLetterPlaceholderService;

    @Mock
    private IssueGenericLetterHandler handler;

    @Mock
    private PrintService bulkPrintService;

    @Mock
    CoverLetterService coverLetterService;

    @Mock
    CcdNotificationService ccdNotificationService;

    private Map<LanguagePreference, Map<String, Map<String, String>>> template =  new HashMap<>();

    private byte[] letter = new byte[1];

    @BeforeEach
    public void setup() {
        openMocks(this);

        Map<String, String> nameMap;
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-LET-ENG-Issue-Generic-Letter.docx");
        nameMap.put("cover", "TB-SCS-LET-ENG-Cover-Sheet.docx");
        Map<String, Map<String, String>> englishDocs = new HashMap<>();
        englishDocs.put("generic-letter", nameMap);
        template.put(LanguagePreference.ENGLISH, englishDocs);

        DocmosisTemplateConfig docmosisTemplateConfig = new DocmosisTemplateConfig();
        docmosisTemplateConfig.setTemplate(template);

        handler = new IssueGenericLetterHandler(bulkPrintService, genericLetterPlaceholderService, coverLetterService,
            ccdNotificationService, docmosisTemplateConfig, true);
    }

    @Test
    void shouldReturnFalse_givenANonQualifyingCallbackType() {
        Callback<SscsCaseData> callback = buildTestCallbackForGivenData(SscsCaseData.builder().build(),
            READY_TO_LIST,
            NON_COMPLIANT);

        boolean result = handler.canHandle(ABOUT_TO_SUBMIT,  callback);

        Assertions.assertFalse(result);
    }

    @Test
    void shouldReturnFalse_givenANonQualifyingEvent() {
        Callback<SscsCaseData> callback = buildTestCallbackForGivenData(SscsCaseData.builder().build(),
            READY_TO_LIST,
            DECISION_ISSUED);

        boolean result = handler.canHandle(SUBMITTED, callback);

        Assertions.assertFalse(result);
    }

    @Test
    void shouldReturnFalse_givenFeatureFlagIsFalse() {
        Callback<SscsCaseData> callback = buildTestCallbackForGivenData(SscsCaseData.builder().build(),
            READY_TO_LIST,
            ISSUE_GENERIC_LETTER);
        handler = new IssueGenericLetterHandler(bulkPrintService, genericLetterPlaceholderService, coverLetterService,
            ccdNotificationService, null, false);

        boolean result = handler.canHandle(SUBMITTED, callback);

        Assertions.assertFalse(result);
    }

    @Test
    void shouldThrowException_givenCallbackIsNull() {
        assertThrows(NullPointerException.class, () ->
            handler.canHandle(SUBMITTED, null)
        );
    }

    @Test
    void shouldThrowExceptionInHandler_givenCallbackIsNull() {
        SscsCaseData caseData = buildCaseData();
        Callback<SscsCaseData> callback = buildTestCallbackForGivenData(caseData, READY_TO_LIST, ISSUE_ADJOURNMENT_NOTICE);

        assertThrows(IllegalStateException.class, () ->
            handler.handle(MID_EVENT, callback)
        );

        assertThrows(IllegalStateException.class, () ->
            handler.handle(SUBMITTED, callback)
        );
    }

    @Test
    void shouldSendLettersToAllPartiesWhenAllPartiesSelected() {
        SscsCaseData caseData = buildCaseData();
        caseData.setSendToAllParties(YesNo.YES);

        var jointParty = buildJointParty();
        caseData.setJointParty(jointParty);

        var otherParty = new CcdValue<>(buildOtherParty());

        var otherPartyWithRep = buildOtherParty();
        Representative representative = caseData.getAppeal().getRep();
        otherPartyWithRep.setRep(representative);

        caseData.setOtherParties(List.of(otherParty, new CcdValue<>(otherPartyWithRep)));
        caseData.setOtherPartySelection(buildOtherPartiesSelection(otherParty, representative));

        UUID uuid = UUID.randomUUID();

        when(bulkPrintService.sendToBulkPrint(any(), eq(caseData), any())).thenReturn(Optional.of(uuid));
        when(genericLetterPlaceholderService.populatePlaceholders(eq(caseData), any(), nullable(String.class))).thenReturn(Map.of());
        when(coverLetterService.generateCoverLetterRetry(any(), anyString(), anyString(), any(), anyInt())).thenReturn(letter);
        when(coverLetterService.generateCoverSheet(anyString(), eq("coversheet"), eq(Map.of()))).thenReturn(letter);

        Callback<SscsCaseData> callback = buildTestCallbackForGivenData(caseData, READY_TO_LIST, ISSUE_GENERIC_LETTER);

        handler.handle(SUBMITTED, callback);

        verify(ccdNotificationService, times(5))
            .storeNotificationLetterIntoCcd(eq(ISSUE_GENERIC_LETTER), notNull(), eq(callback.getCaseDetails().getId()));
        verify(bulkPrintService, times(5)).sendToBulkPrint(any(), eq(caseData), any());
    }

    @Test
    void shouldSendToSelectedParties() {
        SscsCaseData caseData = buildCaseData();
        caseData.setSendToApellant(YesNo.YES);
        caseData.setSendToJointParty(YesNo.YES);
        caseData.setSendToOtherParties(YesNo.YES);
        caseData.setSendToRepresentative(YesNo.YES);

        var jointParty = buildJointParty();
        caseData.setJointParty(jointParty);

        var otherParty = new CcdValue<>(buildOtherParty());

        var otherPartyWithRep = buildOtherParty();
        Representative representative = caseData.getAppeal().getRep();
        otherPartyWithRep.setRep(representative);

        caseData.setOtherParties(List.of(otherParty, new CcdValue<>(otherPartyWithRep)));
        caseData.setOtherPartySelection(buildOtherPartiesSelection(otherParty, representative));

        UUID uuid = UUID.randomUUID();

        when(bulkPrintService.sendToBulkPrint(any(), eq(caseData), any())).thenReturn(Optional.of(uuid));
        when(genericLetterPlaceholderService.populatePlaceholders(eq(caseData), any(), nullable(String.class))).thenReturn(Map.of());
        when(coverLetterService.generateCoverLetterRetry(any(), anyString(), anyString(), any(), anyInt())).thenReturn(letter);

        Callback<SscsCaseData> callback = buildTestCallbackForGivenData(caseData, READY_TO_LIST, ISSUE_GENERIC_LETTER);

        handler.handle(SUBMITTED, callback);

        verify(ccdNotificationService, times(5))
            .storeNotificationLetterIntoCcd(eq(ISSUE_GENERIC_LETTER), notNull(), eq(callback.getCaseDetails().getId()));
        verify(bulkPrintService, times(5)).sendToBulkPrint(any(), eq(caseData), any());
    }

    @Test
    void shouldNotSendLettersWhenNoPartiesSelected() {
        SscsCaseData caseData = buildCaseData();
        caseData.setSendToApellant(YesNo.NO);
        caseData.setSendToJointParty(YesNo.NO);
        caseData.setSendToOtherParties(YesNo.NO);
        caseData.setSendToRepresentative(YesNo.NO);

        var jointParty = buildJointParty();
        caseData.setJointParty(jointParty);

        var otherParty = new CcdValue<>(buildOtherParty());

        var otherPartyWithRep = buildOtherParty();
        Representative representative = caseData.getAppeal().getRep();
        otherPartyWithRep.setRep(representative);

        caseData.setOtherParties(List.of(otherParty, new CcdValue<>(otherPartyWithRep)));
        caseData.setOtherPartySelection(buildOtherPartiesSelection(otherParty, representative));

        Callback<SscsCaseData> callback = buildTestCallbackForGivenData(caseData, READY_TO_LIST, ISSUE_GENERIC_LETTER);

        handler.handle(SUBMITTED, callback);

        verifyNoInteractions(ccdNotificationService);
        verifyNoInteractions(bulkPrintService);
    }


    @Test
    void shouldLogErrorWhenIdIsEmpty() {
        SscsCaseData caseData = buildCaseData();
        caseData.setSendToAllParties(YesNo.YES);

        when(bulkPrintService.sendToBulkPrint(any(), eq(caseData), any())).thenReturn(Optional.empty());
        when(genericLetterPlaceholderService.populatePlaceholders(eq(caseData), any(), nullable(String.class))).thenReturn(Map.of());

        Callback<SscsCaseData> callback = buildTestCallbackForGivenData(caseData, READY_TO_LIST, ISSUE_GENERIC_LETTER);

        handler.handle(SUBMITTED, callback);

        verify(ccdNotificationService, times(0)).storeNotificationLetterIntoCcd(any(), any(), any());
        verify(bulkPrintService, times(2)).sendToBulkPrint(any(), eq(caseData), any());
    }

    @Test
    void shouldBundleCoverLetter() throws IOException {
        SscsCaseData caseData = buildCaseData();
        caseData.setSendToApellant(YesNo.YES);
        caseData.setSendToJointParty(YesNo.NO);
        caseData.setSendToOtherParties(YesNo.NO);
        caseData.setSendToRepresentative(YesNo.NO);

        UUID uuid = UUID.randomUUID();

        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("myPdf.pdf"));
        when(bulkPrintService.sendToBulkPrint(any(), eq(caseData), any())).thenReturn(Optional.of(uuid));
        when(genericLetterPlaceholderService.populatePlaceholders(eq(caseData), any(), nullable(String.class))).thenReturn(Map.of());
        when(coverLetterService.generateCoverLetterRetry(any(), anyString(), anyString(), any(), anyInt())).thenReturn(pdfBytes);
        when(coverLetterService.generateCoverSheet(anyString(), anyString(), any())).thenReturn(pdfBytes);

        Callback<SscsCaseData> callback = buildTestCallbackForGivenData(caseData, READY_TO_LIST, ISSUE_GENERIC_LETTER);

        handler.handle(SUBMITTED, callback);

        verify(ccdNotificationService, times(1))
            .storeNotificationLetterIntoCcd(eq(ISSUE_GENERIC_LETTER), notNull(), eq(callback.getCaseDetails().getId()));
        verify(bulkPrintService, times(1)).sendToBulkPrint(any(), eq(caseData), any());
    }

    private static List<CcdValue<OtherPartySelectionDetails>> buildOtherPartiesSelection(CcdValue<OtherParty> otherParty, Representative representative) {
        var item1 = new DynamicListItem(otherParty.getValue().getId(), "test");
        var item2 = new DynamicListItem(representative.getId(), "test");

        var list1 = new DynamicList(item1, List.of());
        CcdValue<OtherPartySelectionDetails> otherParties1 = new CcdValue<>(new OtherPartySelectionDetails(list1));

        var list2 = new DynamicList(item2, List.of());
        CcdValue<OtherPartySelectionDetails> otherParties2 = new CcdValue<>(new OtherPartySelectionDetails(list2));

        return List.of(otherParties1, otherParties2);
    }
}