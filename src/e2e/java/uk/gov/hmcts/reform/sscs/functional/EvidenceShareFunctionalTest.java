package uk.gov.hmcts.reform.sscs.functional;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SYA_APPEAL_CREATED;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;

public class EvidenceShareFunctionalTest extends AbstractFunctionalTest {

    public EvidenceShareFunctionalTest() {
        super();
    }

    @Test
    public void processAnAppealWithValidMrn_shouldGenerateADl6AndAddToCcd() throws IOException {

        createCase();
        String json = getJson(SYA_APPEAL_CREATED);
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().toString());

        simulateCcdCallback(json);

        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);

        List<SscsDocument> docs = caseDetails.getData().getSscsDocument();
        assertEquals(1, docs.size());
        assertEquals("DL6-" + ccdCaseId + ".pdf", docs.get(0).getValue().getDocumentFileName());
    }

    @Test
    public void processAnAppealWithValidMrn_shouldGenerateADl16AndAddToCcd() throws IOException {

        createCase();
        String json = getJson(SYA_APPEAL_CREATED);
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().minusDays(31).toString());

        simulateCcdCallback(json);

        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);

        List<SscsDocument> docs = caseDetails.getData().getSscsDocument();
        assertEquals(1, docs.size());
        assertEquals("DL16-" + ccdCaseId + ".pdf", docs.get(0).getValue().getDocumentFileName());
    }
}
