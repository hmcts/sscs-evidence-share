package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.APPELLANT_FULL_NAME_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.BENEFIT_TYPE_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.CASE_ID_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.GENERATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.NINO_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.REGIONAL_OFFICE_COUNTY_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.REGIONAL_OFFICE_FAX_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.REGIONAL_OFFICE_PHONE_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.REGIONAL_OFFICE_POSTCODE_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.SSCS_URL;
import static uk.gov.hmcts.reform.sscs.config.PlaceholderConstants.SSCS_URL_LITERAL;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.io.FileUtils;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;
import uk.gov.hmcts.reform.sscs.docmosis.service.PdfGenerationService;

@Service
public class IssueFurtherEvidenceHandler implements CallbackHandler<SscsCaseData> {

    @Value("${document.pdf.hmctsImgKey}")
    private String hmctsImgKey;

    @Value("${document.pdf.hmctsImgVal}")
    private String hmctsImgVal;

    @Qualifier("docmosisPdfGenerationService")
    @Autowired
    private PdfGenerationService pdfGenerationService;

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
        //generate the 609-97 (original sender) cover letter using docmosis

        byte[] coverLetter = generate609_97_OriginalSenderCoverLetter(callback);


        try {
            FileUtils.writeByteArrayToFile(new File("coverLetter.pdf"), coverLetter);
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }

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

        setRegionalProcessingOfficeAddress(placeholders, caseData);
        return placeholders;
    }

    private boolean hasRegionalProcessingCenter(SscsCaseData ccdResponse) {
        return nonNull(ccdResponse.getRegionalProcessingCenter())
            && nonNull(ccdResponse.getRegionalProcessingCenter().getName());
    }

    private Object defaultToEmptyStringIfNull(Object value) {
        return (value == null) ? StringUtils.EMPTY : value;
    }

    private void setRegionalProcessingOfficeAddress(Map<String, Object> placeholders, SscsCaseData caseData) {
        if (hasRegionalProcessingCenter(caseData)) {
            RegionalProcessingCenter rpc = caseData.getRegionalProcessingCenter();
            placeholders.put(REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL, defaultToEmptyStringIfNull(rpc.getAddress1()));
            placeholders.put(REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL, defaultToEmptyStringIfNull(rpc.getAddress2()));
            placeholders.put(REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL, defaultToEmptyStringIfNull(rpc.getAddress3()));
            placeholders.put(REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL, defaultToEmptyStringIfNull(rpc.getAddress4()));
            placeholders.put(REGIONAL_OFFICE_COUNTY_LITERAL, defaultToEmptyStringIfNull(rpc.getCity()));
            placeholders.put(REGIONAL_OFFICE_PHONE_LITERAL, defaultToEmptyStringIfNull(rpc.getPhoneNumber()));
            placeholders.put(REGIONAL_OFFICE_FAX_LITERAL, defaultToEmptyStringIfNull(rpc.getFaxNumber()));
            placeholders.put(REGIONAL_OFFICE_POSTCODE_LITERAL, defaultToEmptyStringIfNull(rpc.getPostcode()));
        }
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
