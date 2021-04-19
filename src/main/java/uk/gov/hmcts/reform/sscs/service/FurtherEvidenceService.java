package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.JOINT_PARTY_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REPRESENTATIVE_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.APPELLANT_LETTER;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.DWP_LETTER;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.JOINT_PARTY_LETTER;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.REPRESENTATIVE_LETTER;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.DocmosisTemplateConfig;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.model.PdfDocument;

@Service
@Slf4j
public class FurtherEvidenceService {

    private DocmosisTemplateConfig docmosisTemplateConfig;

    private CoverLetterService coverLetterService;

    private SscsDocumentService sscsDocumentService;

    private PrintService bulkPrintService;

    public FurtherEvidenceService(@Autowired CoverLetterService coverLetterService,
                                  @Autowired SscsDocumentService sscsDocumentService,
                                  @Autowired PrintService bulkPrintService,
                                  @Autowired DocmosisTemplateConfig docmosisTemplateConfig) {
        this.coverLetterService = coverLetterService;
        this.sscsDocumentService = sscsDocumentService;
        this.bulkPrintService = bulkPrintService;
        this.docmosisTemplateConfig =  docmosisTemplateConfig;
    }

    public void issue(List<? extends AbstractDocument> sscsDocuments, SscsCaseData caseData, DocumentType documentType,
                      List<FurtherEvidenceLetterType> allowedLetterTypes) {
        List<PdfDocument> pdfDocument = sscsDocumentService.getPdfsForGivenDocTypeNotIssued(sscsDocuments, documentType, isYes(caseData.getIsConfidentialCase()));

        List<PdfDocument> sizeNormalisedPdfDocuments = sscsDocumentService.sizeNormalisePdfs(pdfDocument);

        updateCaseDocuments(sizeNormalisedPdfDocuments.stream().map(pdfDoc -> pdfDoc.getDocument()).collect(Collectors.toList()), caseData, documentType);

        List<Pdf> pdfs = sizeNormalisedPdfDocuments.stream().map(pdfDoc -> pdfDoc.getPdf()).collect(Collectors.toList());

        if (pdfs != null && pdfs.size() > 0) {
            send609_97_OriginalSender(caseData, documentType, pdfs, allowedLetterTypes);
            send609_98_OtherParty(caseData, documentType, pdfs, allowedLetterTypes);
            log.info("Sending documents to bulk print for ccd Id: {} and document type: {}", caseData.getCcdCaseId(), documentType);
        }
    }

    public void updateCaseDocuments(List<? extends AbstractDocument> documents, SscsCaseData caseData, DocumentType documentType) {

        List<SscsDocument> sscsCaseDocuments = caseData.getSscsDocument();

        for (AbstractDocument<AbstractDocumentDetails> doc : documents) {

            if (doc.getValue() != null
                && documentType.getValue().equals(doc.getValue().getDocumentType())
                && doc.getValue().getResizedDocumentLink() != null) {

                if (doc.getValue().getClass().isAssignableFrom(SscsDocumentDetails.class)) {
                    sscsCaseDocuments
                        .stream()
                        .filter(d -> d.getValue().getDocumentLink().getDocumentBinaryUrl().equals(doc.getValue().getDocumentLink().getDocumentBinaryUrl()))
                        .map(d -> {
                            DocumentLink resizedLink = doc.getValue().getResizedDocumentLink();
                            d.getValue().setResizedDocumentLink(resizedLink);
                            log.info("Sending resized document to bulk print link: DocumentLink(documentUrl= {} , documentFilename= {} and caseId {} )",
                                resizedLink.getDocumentUrl(), resizedLink.getDocumentFilename(), caseData.getCcdCaseId());
                            return d;
                        }).findFirst();
                }
            }
        }
    }


    protected void send609_97_OriginalSender(SscsCaseData caseData, DocumentType documentType, List<Pdf> pdfs,
                                           List<FurtherEvidenceLetterType> allowedLetterTypes) {

        String docName = "609-97-template (original sender)";
        final FurtherEvidenceLetterType letterType = findLetterType(documentType);

        if (allowedLetterTypes.contains(letterType)) {
            byte[] bulkPrintList60997 = buildPdfsFor609_97(caseData, letterType, docName);
            bulkPrintService.sendToBulkPrint(buildPdfs(bulkPrintList60997, pdfs, docName), caseData, letterType,
                EventType.ISSUE_FURTHER_EVIDENCE);

        }
    }

    protected void send609_98_OtherParty(SscsCaseData caseData, DocumentType documentType, List<Pdf> pdfs,
                                       List<FurtherEvidenceLetterType> allowedLetterTypes) {

        List<FurtherEvidenceLetterType> otherPartiesList = buildOtherPartiesList(caseData, documentType);

        for (FurtherEvidenceLetterType letterType : otherPartiesList) {
            String docName = letterType == DWP_LETTER ? "609-98-template (DWP)" : "609-98-template (other parties)";

            if (allowedLetterTypes.contains(letterType)) {
                byte[] bulkPrintList60998 = buildPdfsFor609_98(caseData, letterType, docName);

                List<Pdf> pdfs60998 = buildPdfs(bulkPrintList60998, pdfs, docName);
                bulkPrintService.sendToBulkPrint(pdfs60998, caseData, letterType,
                    EventType.ISSUE_FURTHER_EVIDENCE);
            }
        }
    }

    private List<FurtherEvidenceLetterType> buildOtherPartiesList(SscsCaseData caseData, DocumentType documentType) {
        List<FurtherEvidenceLetterType> otherPartiesList = new ArrayList<>();

        if (documentType != APPELLANT_EVIDENCE) {
            otherPartiesList.add(APPELLANT_LETTER);
        }
        if (documentType != REPRESENTATIVE_EVIDENCE && checkRepExists(caseData)) {
            otherPartiesList.add(REPRESENTATIVE_LETTER);
        }
        if (documentType != JOINT_PARTY_EVIDENCE && checkJointPartyExists(caseData)) {
            otherPartiesList.add(JOINT_PARTY_LETTER);
        }

        return otherPartiesList;
    }

    private boolean checkRepExists(SscsCaseData caseData) {
        Representative rep = caseData.getAppeal().getRep();
        return null != rep && "yes".equalsIgnoreCase(rep.getHasRepresentative());
    }

    private boolean checkJointPartyExists(SscsCaseData caseData) {
        return caseData.isThereAJointParty();
    }

    private FurtherEvidenceLetterType findLetterType(DocumentType documentType) {
        if (documentType == APPELLANT_EVIDENCE) {
            return APPELLANT_LETTER;
        } else if (documentType == REPRESENTATIVE_EVIDENCE) {
            return REPRESENTATIVE_LETTER;
        } else if (documentType == JOINT_PARTY_EVIDENCE) {
            return JOINT_PARTY_LETTER;
        } else {
            return DWP_LETTER;
        }
    }

    private List<Pdf> buildPdfs(byte[] coverLetterContent, List<Pdf> pdfsToBulkPrint, String pdfName) {
        List<Pdf> pdfs = new ArrayList<>(pdfsToBulkPrint);
        coverLetterService.appendCoverLetter(coverLetterContent, pdfs, pdfName);
        return pdfs;
    }

    private byte[] buildPdfsFor609_97(SscsCaseData caseData, FurtherEvidenceLetterType letterType, String pdfName) {
        return coverLetterService.generateCoverLetter(caseData, letterType,
            getTemplateNameBasedOnLanguagePreference(caseData.getLanguagePreference(), "d609-97"), pdfName);
    }

    private byte[] buildPdfsFor609_98(SscsCaseData caseData, FurtherEvidenceLetterType letterType, String pdfName) {
        return coverLetterService.generateCoverLetter(caseData, letterType,
            getTemplateNameBasedOnLanguagePreference(caseData.getLanguagePreference(), "d609-98"), pdfName);
    }

    public boolean canHandleAnyDocument(List<SscsDocument> sscsDocumentList) {
        return null != sscsDocumentList && sscsDocumentList.stream()
            .anyMatch(this::canHandleDocument);
    }

    private boolean canHandleDocument(SscsDocument sscsDocument) {
        return sscsDocument != null && sscsDocument.getValue() != null
            && "No".equals(sscsDocument.getValue().getEvidenceIssued())
            && null != sscsDocument.getValue().getDocumentType();
    }

    private String getTemplateNameBasedOnLanguagePreference(LanguagePreference languagePreference, String documentType) {
        return docmosisTemplateConfig.getTemplate().get(languagePreference)
                .get(documentType).get("name");
    }
}
