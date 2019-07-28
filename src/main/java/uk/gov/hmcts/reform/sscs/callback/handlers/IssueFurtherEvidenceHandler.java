package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.BulkPrintService;
import uk.gov.hmcts.reform.sscs.service.CoverLetterService;
import uk.gov.hmcts.reform.sscs.service.SscsDocumentService;

@Service
public class IssueFurtherEvidenceHandler implements CallbackHandler<SscsCaseData> {

    @Autowired
    private CoverLetterService coverLetterService;
    @Autowired
    private SscsDocumentService sscsDocumentService;
    @Autowired
    private BulkPrintService bulkPrintService;
    @Autowired
    private CcdService ccdService;
    @Autowired
    private IdamService idamService;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        return canHandleAnyDocument(callback.getCaseDetails().getCaseData().getSscsDocument());
    }

    private boolean canHandleAnyDocument(List<SscsDocument> sscsDocument) {
        return null != sscsDocument && sscsDocument.stream().anyMatch(this::canHandleDocument);
    }

    private boolean canHandleDocument(SscsDocument sscsDocument) {
        return sscsDocument != null && sscsDocument.getValue() != null
            && "No".equals(sscsDocument.getValue().getEvidenceIssued())
            && APPELLANT_EVIDENCE.getValue().equals(sscsDocument.getValue().getDocumentType());
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        bulkPrintService.sendToBulkPrint(buildPdfsToBulkPrint(caseData), caseData);
        setEvidenceIssuedFlagToYes(caseData);
    }

    private void setEvidenceIssuedFlagToYes(SscsCaseData caseData) {
        sscsDocumentService.filterByDocTypeAndApplyAction(caseData.getSscsDocument(), APPELLANT_EVIDENCE,
            doc -> doc.getValue().setEvidenceIssued("Yes"));
        ccdService.updateCase(
            caseData,
            Long.valueOf(caseData.getCcdCaseId()),
            "updateCaseOnly",
            "Update case data only",
            "Simply update case data with no callbacks afterwards",
            idamService.getIdamTokens());
    }

    private List<Pdf> buildPdfsToBulkPrint(SscsCaseData caseData) {
        List<Pdf> pdfsToBulkPrint = sscsDocumentService.getPdfsForGivenDocType(
            caseData.getSscsDocument(), APPELLANT_EVIDENCE);
        coverLetterService.appendCoverLetter(caseData, pdfsToBulkPrint);
        return pdfsToBulkPrint;
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
