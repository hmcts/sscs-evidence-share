package uk.gov.hmcts.reform.sscs.functional;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;

public class EvidenceShareFunctionalTest extends AbstractFunctionalTest {

    public EvidenceShareFunctionalTest() {
        super();
    }

    @Test
    public void processAnAppealWithValidMrn_shouldGenerateADl6AndAddToCcdAndUpdateState() throws InterruptedException {

        SscsCaseData sscsCaseData = createCase(LocalDate.now().toString());
        sendToDwpEvent(sscsCaseData);

        waitUntil(stateUpdated(), 60L);

        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        SscsCaseData caseData = caseDetails.getData();

        List<SscsDocument> docs = caseData.getSscsDocument();
        assertEquals(1, docs.size());
        assertEquals("dl6-" + ccdCaseId + ".pdf", docs.get(0).getValue().getDocumentFileName());
        assertEquals("withDwp", caseDetails.getState());
        assertEquals(LocalDate.now().toString(), caseData.getDateSentToDwp());
    }

    @Test
    public void processAnAppealWithLateMrn_shouldGenerateADl16AndAddToCcdAndUpdateState() throws InterruptedException {

        SscsCaseData sscsCaseData = createCase(LocalDate.now().minusDays(31).toString());
        sendToDwpEvent(sscsCaseData);

        waitUntil(stateUpdated(), 60L);

        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        SscsCaseData caseData = caseDetails.getData();

        List<SscsDocument> docs = caseData.getSscsDocument();
        assertEquals(1, docs.size());
        assertEquals("dl16-" + ccdCaseId + ".pdf", docs.get(0).getValue().getDocumentFileName());
        assertEquals("withDwp", caseDetails.getState());
        assertEquals(LocalDate.now().toString(), caseData.getDateSentToDwp());
    }

    private Supplier<Boolean> stateUpdated() {
        return () -> {
            SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
            return "withDwp".equals(caseDetails.getState());
        };
    }

    private static void waitUntil(Supplier<Boolean> condition, long timeoutInSeconds) throws InterruptedException {
        long timeout = timeoutInSeconds * 1000L * 1000000L;
        long startTime = System.nanoTime();
        while (true) {
            if (condition.get()) {
                System.out.println("Evidence share event completed after [" + ((System.nanoTime() - startTime) / (1000L * 1000000L)) + "] seconds");
                break;
            } else if (System.nanoTime() - startTime >= timeout) {
                throw new RuntimeException("Evidence share event has not been completed in [" + timeoutInSeconds + "] seconds.");
            }
            Thread.sleep(5000L);
        }
    }
}
