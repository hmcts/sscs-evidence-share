package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderUtility.defaultToEmptyStringIfNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;
import uk.gov.hmcts.reform.sscs.docmosis.service.PdfGenerationService;
import uk.gov.hmcts.reform.sscs.service.placeholders.CommonPlaceholderService;
import uk.gov.hmcts.reform.sscs.service.placeholders.RpcPlaceholderService;

@Service
public class IssueFurtherEvidenceHandler implements CallbackHandler<SscsCaseData> {

    @Value("${document.pdf.hmctsImgKey}")
    private String hmctsImgKey;
    @Value("${document.pdf.hmctsImgVal}")
    private String hmctsImgVal;
    @Qualifier("docmosisPdfGenerationService")
    @Autowired
    private PdfGenerationService pdfGenerationService;
    @Autowired
    private RpcPlaceholderService rpcPlaceholderService;
    @Autowired
    private CommonPlaceholderService commonPlaceholderService;

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

    private byte[] generate609_97_OriginalSenderCoverLetter(Callback<SscsCaseData> callback) {
        return pdfGenerationService.generatePdf(DocumentHolder.builder()
            .template(new Template("TB-SCS-GNO-ENG-00068.doc",
                "609-97-template (original sender)"))
            .placeholders(populatePlaceHolders(callback.getCaseDetails().getCaseData()))
            .pdfArchiveMode(true)
            .build());
    }

    //todo: extract and delegate to new OriginalSender609_97PlaceholderService
    private Map<String, Object> populatePlaceHolders(SscsCaseData caseData) {
        Map<String, Object> placeholders = new ConcurrentHashMap<>();
        commonPlaceholderService.populatePlaceholders(caseData, placeholders);
        rpcPlaceholderService.populatePlaceHolders(placeholders, caseData);

        Appeal appeal = caseData.getAppeal();
        Address address = appeal.getAppellant().getAddress();
        placeholders.put("original_sender_address_line1", defaultToEmptyStringIfNull(address.getLine1()));
        placeholders.put("original_sender_address_line2", defaultToEmptyStringIfNull(address.getLine2()));
        placeholders.put("original_sender_address_line3", defaultToEmptyStringIfNull(address.getCounty()));
        placeholders.put("original_sender_address_line4", defaultToEmptyStringIfNull(address.getPostcode()));

        return placeholders;
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
