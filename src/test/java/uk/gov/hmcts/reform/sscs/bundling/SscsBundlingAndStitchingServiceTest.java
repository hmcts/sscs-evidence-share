package uk.gov.hmcts.reform.sscs.bundling;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

public class SscsBundlingAndStitchingServiceTest {

    @Mock
    private SscsBundlePopulator sscsBundlePopulator;

    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    @Mock
    private IdamService idamService;

    private SscsBundlingAndStitchingService sscsBundlingAndStitchingService;

    @Before
    public void setup() {
        initMocks(this);
        sscsBundlingAndStitchingService = new SscsBundlingAndStitchingService(sscsBundlePopulator, updateCcdCaseService, idamService);
    }

    @Test
    public void givenAnSscsCase_thenCreateBundleAndStitchToCase() {
        List<SscsDocument> docs = new ArrayList<>();

        SscsDocument sscsDocument1 = SscsDocument.builder().value(
            SscsDocumentDetails.builder().documentFileName("TestFileName1")
                .documentLink(
                    DocumentLink.builder().documentUrl("test1.com").documentBinaryUrl("test1.com/binary").documentFilename("MyFile1.jpg").build()).build())
            .build();

        docs.add(sscsDocument1);

        SscsCaseData caseData = SscsCaseData.builder().ccdCaseId("1").sscsDocument(docs).build();

        Bundle bundle = Bundle.builder().build();
        when(sscsBundlePopulator.populateNewBundle(caseData)).thenReturn(bundle);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        SscsCaseData result = sscsBundlingAndStitchingService.bundleAndStitch(caseData);

        verify(updateCcdCaseService, times(2)).updateCase(eq(caseData), eq(1L), any(), any(), any(), eq(idamTokens));

        assertEquals(result.getCaseBundles().get(0), bundle);
    }

    @Test
    public void givenAnSscsCaseWithExistingBundle_thenCreateAnotherBundleAndStitchToCase() {
        List<SscsDocument> docs = new ArrayList<>();

        SscsDocument sscsDocument1 = SscsDocument.builder().value(
            SscsDocumentDetails.builder().documentFileName("TestFileName1")
                .documentLink(
                    DocumentLink.builder().documentUrl("test1.com").documentBinaryUrl("test1.com/binary").documentFilename("MyFile1.jpg").build()).build())
            .build();

        docs.add(sscsDocument1);

        Bundle existingBundle = Bundle.builder().value(BundleDetails.builder().title("New bundle").build()).build();

        List<Bundle> bundles = new ArrayList<>();
        bundles.add(existingBundle);

        SscsCaseData caseData = SscsCaseData.builder().ccdCaseId("1").caseBundles(bundles).sscsDocument(docs).build();

        Bundle bundle = Bundle.builder().value(BundleDetails.builder().title("New bundle").build()).build();
        when(sscsBundlePopulator.populateNewBundle(caseData)).thenReturn(bundle);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        SscsCaseData result = sscsBundlingAndStitchingService.bundleAndStitch(caseData);

        verify(updateCcdCaseService, times(2)).updateCase(eq(caseData), eq(1L), any(), any(), any(), eq(idamTokens));

        assertEquals(result.getCaseBundles().get(0), existingBundle);
        assertEquals(result.getCaseBundles().get(1), bundle);
    }

    @Test
    public void givenACaseWithNoDocuments_thenReturnOriginalCaseData() {
        SscsCaseData caseData = SscsCaseData.builder().build();

        assertEquals(caseData, sscsBundlingAndStitchingService.bundleAndStitch(caseData));
    }
}
