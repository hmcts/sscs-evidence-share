package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.callback.handlers.HandlerHelper.buildTestCallbackForGivenData;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_GENERIC_LETTER;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.NON_COMPLIANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderHelper.buildJointParty;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderHelper.buildOtherParty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherPartySelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.config.DocmosisTemplateConfig;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.CcdPdfService;
import uk.gov.hmcts.reform.sscs.service.CoverLetterService;
import uk.gov.hmcts.reform.sscs.service.PrintService;
import uk.gov.hmcts.reform.sscs.service.placeholders.GenericLetterPlaceholderService;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class IssueGenericLetterHandlerTest {

    @Mock
    private GenericLetterPlaceholderService genericLetterPlaceholderService;

    @Mock
    private IssueGenericLetterHandler handler;

    @Mock
    private IdamService idamService;

    @Mock
    private PrintService bulkPrintService;

    @Mock
    CoverLetterService coverLetterService;

    @Mock
    CcdPdfService ccdPdfService;

    private Map<LanguagePreference, Map<String, Map<String, String>>> template =  new HashMap<>();

    @BeforeEach
    public void setup() {
        openMocks(this);

        Map<String, String> nameMap;
        Map<String, Map<String, String>> englishDocs = new HashMap<>();
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-LET-ENG-Issue-Generic-Letter.docx");
        englishDocs.put("generic-letter", nameMap);
        template.put(LanguagePreference.ENGLISH, englishDocs);


        DocmosisTemplateConfig docmosisTemplateConfig = new DocmosisTemplateConfig();
        docmosisTemplateConfig.setTemplate(template);

        handler = new IssueGenericLetterHandler(bulkPrintService, genericLetterPlaceholderService, coverLetterService,
            ccdPdfService, idamService, docmosisTemplateConfig);
    }

    @Test
    public void shouldReturnFalse_givenANonQualifyingCallbackType() {
        Callback<SscsCaseData> callback = buildTestCallbackForGivenData(SscsCaseData.builder().build(),
            READY_TO_LIST,
            NON_COMPLIANT);

        log.info("Handler {}", handler);

        boolean result = handler.canHandle(ABOUT_TO_SUBMIT,  callback);

        Assertions.assertFalse(result);
    }

    @Test
    public void shouldReturnFalse_givenANonQualifyingEvent() {
        Assertions.assertFalse(handler.canHandle(SUBMITTED,
            buildTestCallbackForGivenData(SscsCaseData.builder()
                    .createdInGapsFrom(READY_TO_LIST.getId()).build(),
                READY_TO_LIST,
                DECISION_ISSUED)
        ));
    }

    @Test
    public void shouldThrowException_givenCallbackIsNull() {
        assertThrows(NullPointerException.class, () ->
            handler.canHandle(SUBMITTED, null)
        );
    }

    @Test
    public void test_1() {
        SscsCaseData caseData = buildCaseData();
        caseData.setSendToAllParties(YesNo.YES);

        var jointParty = buildJointParty();
        caseData.setJointParty(jointParty);

        var otherParty = new CcdValue<>(buildOtherParty());
        caseData.setOtherParties(List.of(otherParty));

        var item = new DynamicListItem(otherParty.getValue().getId(), "test");
        var list = new DynamicList(item, List.of());
        CcdValue<OtherPartySelectionDetails> otherParties = new CcdValue<>(new OtherPartySelectionDetails(list));
        caseData.setOtherPartySelection(List.of(otherParties));

        UUID uuid = UUID.randomUUID();

        when(bulkPrintService.sendToBulkPrint(any(), eq(caseData))).thenReturn(Optional.of(uuid));
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(genericLetterPlaceholderService.populatePlaceholders(eq(caseData), any(), nullable(String.class))).thenReturn(Map.of());

        Callback<SscsCaseData> callback = buildTestCallbackForGivenData(caseData, READY_TO_LIST, ISSUE_GENERIC_LETTER);

        handler.handle(SUBMITTED, callback);

        verify(bulkPrintService, times(4)).sendToBulkPrint(any(), eq(caseData));
    }
}
