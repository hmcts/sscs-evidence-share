package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;
import uk.gov.hmcts.reform.sscs.docmosis.service.PdfGenerationService;
import uk.gov.hmcts.reform.sscs.service.placeholders.OriginalSender60997PlaceholderService;

@Service
public class IssueFurtherEvidenceHandler implements CallbackHandler<SscsCaseData> {

    @Qualifier("docmosisPdfGenerationService")
    @Autowired
    private PdfGenerationService pdfGenerationService;
    @Autowired
    private OriginalSender60997PlaceholderService originalSender60997PlaceholderService;

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
            && DocumentType.APPELLANT_EVIDENCE.getValue().equals(sscsDocument.getValue().getDocumentType());
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        byte[] coverLetter = generate609_97_OriginalSenderCoverLetter(callback);
        // bulk print cover letter and pdf doc
        //And the Evidence Issued Flag on the Document is set to "Yes"
    }

    //todo: add unit test
    private byte[] generate609_97_OriginalSenderCoverLetter(Callback<SscsCaseData> callback) {
        return pdfGenerationService.generatePdf(DocumentHolder.builder()
            .template(new Template("TB-SCS-GNO-ENG-00068.doc",
                "609-97-template (original sender)"))
            .placeholders(originalSender60997PlaceholderService
                .populatePlaceHolders(callback.getCaseDetails().getCaseData()))
            .pdfArchiveMode(true)
            .build());
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
