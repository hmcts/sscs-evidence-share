package uk.gov.hmcts.reform.sscs.bundling;

import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.STITCH_BUNDLE;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Bundle;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Service
public class SscsBundlingAndStitchingService implements CcdCaseUpdater {

    private final SscsBundlePopulator sscsBundlePopulator;

    private final UpdateCcdCaseService updateCcdCaseService;

    private final IdamService idamService;

    @Autowired
    public SscsBundlingAndStitchingService(SscsBundlePopulator sscsBundlePopulator,
                                           UpdateCcdCaseService updateCcdCaseService,
                                           IdamService idamService) {
        this.sscsBundlePopulator = sscsBundlePopulator;
        this.updateCcdCaseService = updateCcdCaseService;
        this.idamService = idamService;
    }

    @Override
    public List<Bundle> bundleAndStitch(SscsCaseData sscsCaseData) {

        if (sscsCaseData != null && sscsCaseData.getSscsDocument() != null) {

            Bundle newBundle = sscsBundlePopulator.populateNewBundle(sscsCaseData);

            addBundleToCase(sscsCaseData, newBundle);

            SscsCaseDetails caseDetails = updateCcdCaseService.updateCase(
                sscsCaseData, Long.valueOf(sscsCaseData.getCcdCaseId()), STITCH_BUNDLE.getCcdType(), "Stitch bundle", "Stitching the bundle before sending", idamService.getIdamTokens());

            return caseDetails.getData().getCaseBundles();
        }

        return null;
    }

    private void addBundleToCase(SscsCaseData sscsCaseData, Bundle newBundle) {
        if (sscsCaseData.getCaseBundles() != null) {
            sscsCaseData.getCaseBundles().add(newBundle);
        } else {
            List<Bundle> bundles = new ArrayList<>();
            bundles.add(newBundle);
            sscsCaseData.setCaseBundles(bundles);
        }
    }
}
