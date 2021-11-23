package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.callback.handlers.HandlerHelper.buildTestCallbackForGivenData;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CONFIRM_PANEL_COMPOSITION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.INTERLOCUTORY_REVIEW_STATE;

import java.time.LocalDate;
import java.util.Arrays;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.PanelCompositionService;


@RunWith(JUnitParamsRunner.class)
public class ConfirmPanelCompositionHandlerTest {

    ConfirmPanelCompositionHandler handler;

    private SscsCaseData sscsCaseData;

    @Mock
    private CcdService ccdService;
    @Mock
    private IdamService idamService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Before
    public void setUp() {
        openMocks(this);

        handler = new ConfirmPanelCompositionHandler(new PanelCompositionService(ccdService, idamService));

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(callback.getEvent()).thenReturn(EventType.CONFIRM_PANEL_COMPOSITION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAValidSubmittedEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(SUBMITTED, buildTestCallbackForGivenData(
            SscsCaseData.builder()
                .ccdCaseId("1")
                .isFqpmRequired(YesNo.YES)
                .dwpDueDate(LocalDate.now().toString())
                .otherParties(Arrays.asList(buildOtherPartyWithHearing("2"), buildOtherPartyWithHearing("1")))
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build())
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, CONFIRM_PANEL_COMPOSITION)));
    }

    @Test
    @Parameters({"YES", "NO"})
    public void givenFqpmSetAndDueDateSetAndAllOtherPartyHearingOptionsSet_thenCaseStateIsReadyToList(String isFqpmRequired) {

        final Callback<SscsCaseData> callback = buildTestCallbackForGivenData(
            SscsCaseData.builder()
                .ccdCaseId("1")
                .isFqpmRequired(isFqpmRequired.equalsIgnoreCase("yes") ? YesNo.YES : YesNo.NO)
                .dwpDueDate(LocalDate.now().toString())
                .otherParties(Arrays.asList(buildOtherPartyWithHearing("2"), buildOtherPartyWithHearing("1")))
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build())
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, CONFIRM_PANEL_COMPOSITION);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(ccdService).updateCase(eq(callback.getCaseDetails().getCaseData()),
            eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())), eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any());
    }

    @Test
    @Parameters({"YES", "NO"})
    public void givenFqpmSetAndNoDueDateSetAndAllOtherPartyHearingOptionsSet_thenCaseStateIsReadyToList(String isFqpmRequired) {

        final Callback<SscsCaseData> callback = buildTestCallbackForGivenData(
            SscsCaseData.builder()
                .ccdCaseId("1")
                .isFqpmRequired(isFqpmRequired.equalsIgnoreCase("yes") ? YesNo.YES : YesNo.NO)
                .dwpDueDate(null)
                .otherParties(Arrays.asList(buildOtherPartyWithHearing("2"), buildOtherPartyWithHearing("1")))
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build())
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, CONFIRM_PANEL_COMPOSITION);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(ccdService).updateCase(eq(callback.getCaseDetails().getCaseData()),
            eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())), eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any());
    }

    @Test
    @Parameters({"YES", "NO"})
    public void givenFqpmSetAndNoDueDateSetAndNotAllOtherPartyHearingOptionsSet_thenCaseStateIsReadyToList(String isFqpmRequired) {

        final Callback<SscsCaseData> callback = buildTestCallbackForGivenData(
            SscsCaseData.builder()
                .ccdCaseId("1")
                .isFqpmRequired(isFqpmRequired.equalsIgnoreCase("yes") ? YesNo.YES : YesNo.NO)
                .dwpDueDate(null)
                .otherParties(Arrays.asList(buildOtherPartyWithHearing("2"), buildOtherParty("1", null)))
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build())
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, CONFIRM_PANEL_COMPOSITION);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(ccdService).updateCase(eq(callback.getCaseDetails().getCaseData()),
            eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())), eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any());
    }

    @Test
    @Parameters({"YES", "NO"})
    public void givenFqpmSetAndDueDateSetAndNotAllOtherPartyHearingOptionsSet_thenCaseStateIsNotListable(String isFqpmRequired) {

        final Callback<SscsCaseData> callback = buildTestCallbackForGivenData(
            SscsCaseData.builder()
                .ccdCaseId("1")
                .isFqpmRequired(isFqpmRequired.equalsIgnoreCase("yes") ? YesNo.YES : YesNo.NO)
                .dwpDueDate(LocalDate.now().toString())
                .otherParties(Arrays.asList(buildOtherPartyWithHearing("2"), buildOtherParty("1", null)))
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build())
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, CONFIRM_PANEL_COMPOSITION);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(ccdService).updateCase(eq(callback.getCaseDetails().getCaseData()),
            eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())), eq(EventType.NOT_LISTABLE.getCcdType()), anyString(), anyString(), any());
    }

    private CcdValue<OtherParty> buildOtherPartyWithHearing(String id) {
        return buildOtherParty(id, HearingOptions.builder().excludeDates(Arrays.asList(ExcludeDate.builder().build())).build());
    }

    private CcdValue<OtherParty> buildOtherParty(String id, HearingOptions hearingOptions) {
        return CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .id(id)
                .unacceptableCustomerBehaviour(YesNo.YES)
                .hearingOptions(hearingOptions)
                .build()).build();
    }
}
