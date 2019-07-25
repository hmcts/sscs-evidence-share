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
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.service.BulkPrintService;
import uk.gov.hmcts.reform.sscs.service.CoverLetterService;
import uk.gov.hmcts.reform.sscs.service.SscsDocumentToPdfService;

@Service
public class IssueFurtherEvidenceHandler implements CallbackHandler<SscsCaseData> {

    @Autowired
    private CoverLetterService coverLetterService;
    @Autowired
    private SscsDocumentToPdfService sscsDocumentToPdfService;
    @Autowired
    private BulkPrintService bulkPrintService;

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
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        bulkPrintService.sendToBulkPrint(buildPdfsToBulkPrint(caseData), caseData);
        //And the Evidence Issued Flag on the Document is set to "Yes"
    }

    private List<Pdf> buildPdfsToBulkPrint(SscsCaseData caseData) {
        List<Pdf> pdfsToBulkPrint = sscsDocumentToPdfService.getPdfsForGivenDocType(
            caseData.getSscsDocument(), APPELLANT_EVIDENCE);
        coverLetterService.appendCoverLetter(caseData, pdfsToBulkPrint);
        return pdfsToBulkPrint;
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
