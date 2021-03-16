package uk.gov.hmcts.reform.sscs.service;

import static java.util.Collections.singletonList;
import static java.util.Optional.*;
import static org.springframework.http.MediaType.APPLICATION_PDF;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AbstractDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.domain.pdf.ByteArrayMultipartFile;
import uk.gov.hmcts.reform.sscs.exception.BulkPrintException;
import uk.gov.hmcts.reform.sscs.helper.PdfHelper;
import uk.gov.hmcts.reform.sscs.model.PdfDocument;

@Slf4j
@Service
public class SscsDocumentService {

    private EvidenceManagementService evidenceManagementService;
    private PdfHelper pdfHelper;

    @Autowired
    public SscsDocumentService(EvidenceManagementService evidenceManagementService, PdfHelper pdfHelper) {
        this.evidenceManagementService = evidenceManagementService;
        this.pdfHelper = pdfHelper;
    }

    private static final PDRectangle pdfPageSize = PDRectangle.A4;

    public List<PdfDocument> getPdfsForGivenDocTypeNotIssued(List<? extends AbstractDocument> sscsDocuments, DocumentType documentType, boolean isConfidentialCase) {
        Objects.requireNonNull(sscsDocuments);
        Objects.requireNonNull(documentType);

        return sscsDocuments.stream()
            .filter(doc -> documentType.getValue().equals(doc.getValue().getDocumentType())
                    && "No".equals(doc.getValue().getEvidenceIssued()))
            .map(doc -> PdfDocument.builder().pdf(toPdf(doc, isConfidentialCase)).document(doc).build())
            .collect(Collectors.toList());
    }

    private Pdf toPdf(AbstractDocument sscsDocument, boolean isConfidentialCase) {
        return new Pdf(getContentForGivenDoc(sscsDocument, isConfidentialCase), sscsDocument.getValue().getDocumentFileName());
    }

    private byte[] getContentForGivenDoc(AbstractDocument sscsDocument, boolean isConfidentialCase) {
        final DocumentLink documentLink = isConfidentialCase ? ofNullable(sscsDocument.getValue().getEditedDocumentLink())
            .orElse(sscsDocument.getValue().getDocumentLink()) : sscsDocument.getValue().getDocumentLink();
        return evidenceManagementService.download(URI.create(documentLink.getDocumentUrl()), "sscs");
    }

    public void filterByDocTypeAndApplyAction(List<SscsDocument> sscsDocument, DocumentType documentType,
                                              Consumer<SscsDocument> action) {
        Objects.requireNonNull(sscsDocument);
        Objects.requireNonNull(documentType);
        Objects.requireNonNull(action);
        sscsDocument.stream()
            .filter(doc -> documentType.getValue().equals(doc.getValue().getDocumentType()))
            .forEach(action);
    }

    public List<PdfDocument> sizeNormalisePdfs(List<PdfDocument> pdfDocuments) {

        List<PdfDocument> normalisedPdfs = new ArrayList<>();

        for (PdfDocument pdfDoc : pdfDocuments) {

            AbstractDocument updatedSscsDocument;
            Pdf updatedPdf;

            Optional<Pdf> resized = resizedPdf(pdfDoc.getPdf());

            if (resized.isPresent()) {
                updatedPdf = resized.get();
                updatedSscsDocument = saveAndUpdateDocument(updatedPdf, pdfDoc.getDocument());
            } else {
                updatedPdf = pdfDoc.getPdf();
                updatedSscsDocument = pdfDoc.getDocument();
            }
            normalisedPdfs.add(PdfDocument.builder().document(updatedSscsDocument).pdf(updatedPdf).build());
        }
        return normalisedPdfs;
    }

    public AbstractDocument saveAndUpdateDocument(Pdf pdf, AbstractDocument document) {

        String pdfFileName = document.getValue().getDocumentFileName() + ".pdf";

        ByteArrayMultipartFile file = ByteArrayMultipartFile
            .builder()
            .content(pdf.getContent())
            .name(pdfFileName)
            .contentType(APPLICATION_PDF)
            .build();

        log.info("About to upload resized document [" + pdfFileName + "]");

        saveFile(pdfFileName, file.getBytes());

        try {
            UploadResponse upload = evidenceManagementService.upload(singletonList(file), "sscs");
            String location = upload.getEmbedded().getDocuments().get(0).links.self.href;
            DocumentLink documentLink = DocumentLink.builder().documentUrl(location).build();
            document.getValue().setResizedDocumentLink(documentLink);

        } catch (Exception e) {
            log.error("Failed to store resized pdf document but carrying on [" + pdfFileName + "]", e);
        }
        return document;
    }

    public Optional<Pdf> resizedPdf(Pdf originalPdf) throws BulkPrintException {

        try {
            PDDocument document = PDDocument.load(originalPdf.getContent());
            final boolean isPdfAcceptedSize = pdfHelper.isDocumentWithinSize(document, pdfPageSize);
            if (isPdfAcceptedSize) {
                log.info("PDF is correct size");
                return Optional.empty();
            } else {
                saveFile("pre-" + originalPdf.getName() + ".pdf", originalPdf.getContent());

                final BigDecimal scalingFactor = pdfHelper.calculateScalingFactor(document, pdfPageSize);

                log.info("PDF is NOT correct size, scaling factor  = {}", scalingFactor.doubleValue());
                PDDocument resizedDoc = pdfHelper.scaleDownDocumentToPageSize(document, scalingFactor, pdfPageSize);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resizedDoc.save(baos);
                return Optional.of(new Pdf(baos.toByteArray(), originalPdf.getName()));
            }
        } catch (Exception e) {
            throw new BulkPrintException("Failed to check and resize PDF", e);
        }
    }

    private void saveFile(String pdfFileName, byte[] bytes) {
        URL url = this.getClass().getClassLoader().getResource("/");
        File fileOut = new File(url + pdfFileName);

        try (OutputStream os = new FileOutputStream(fileOut)) {
            os.write(bytes);
        } catch (Exception e) {
            log.error("Problem saving pdf to disk", e);
        }
    }
}
