package uk.gov.hmcts.reform.sscs.functional;

import org.assertj.core.api.Assertions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;

import java.util.List;

public abstract class FurtherEvidenceHandlerAbstractFunctionalTest extends AbstractFunctionalTest {
    private void verifyEvidenceIssued(SscsCaseDetails caseDetails) {
        SscsCaseData caseData = caseDetails.getData();
        List<SscsDocument> docs = caseData.getSscsDocument();
        log.info("verifyEvidenceIssued ccdCaseId " + ccdCaseId);

        Assertions.assertThat(docs)
            .extracting(SscsDocument::getValue)
            .extracting(SscsDocumentDetails::getEvidenceIssued)
            .hasSize(3).containsOnly("Yes");
    }
}
