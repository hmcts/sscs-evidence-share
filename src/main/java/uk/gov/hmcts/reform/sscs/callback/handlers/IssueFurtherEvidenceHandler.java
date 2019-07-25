package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.APPELLANT_FULL_NAME_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.BENEFIT_TYPE_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.CASE_ID_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.GENERATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.NINO_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.SSCS_URL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.SSCS_URL_LITERAL;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
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
            .placeholders(populatePlaceHolders(callback))
            .pdfArchiveMode(true)
            .build());
    }

    private Map<String, Object> populatePlaceHolders(Callback<SscsCaseData> callback) {
        Map<String, Object> placeholders = new ConcurrentHashMap<>();
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        Appeal appeal = caseData.getAppeal();

        placeholders.put(BENEFIT_TYPE_LITERAL, appeal.getBenefitType().getDescription().toUpperCase());
        placeholders.put(APPELLANT_FULL_NAME_LITERAL, appeal.getAppellant().getName().getAbbreviatedFullName());
        placeholders.put(CASE_ID_LITERAL, caseData.getCcdCaseId());
        placeholders.put(NINO_LITERAL, defaultToEmptyStringIfNull(appeal.getAppellant().getIdentity().getNino()));
        placeholders.put(SSCS_URL_LITERAL, SSCS_URL);
        placeholders.put(GENERATED_DATE_LITERAL, LocalDateTime.now().toLocalDate().toString());
        placeholders.put(hmctsImgKey, hmctsImgVal);

        Address address = appeal.getAppellant().getAddress();
        placeholders.put("original_sender_address_line1", defaultToEmptyStringIfNull(address.getLine1()));
        placeholders.put("original_sender_address_line2", defaultToEmptyStringIfNull(address.getLine2()));
        placeholders.put("original_sender_address_line3", defaultToEmptyStringIfNull(address.getCounty()));
        placeholders.put("original_sender_address_line4", defaultToEmptyStringIfNull(address.getPostcode()));

        rpcPlaceholderService.setRegionalProcessingOfficeAddress(placeholders, caseData);
        return placeholders;
    }

    private Object defaultToEmptyStringIfNull(Object value) {
        return (value == null) ? StringUtils.EMPTY : value;
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
