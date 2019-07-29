package uk.gov.hmcts.reform.sscs.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * Copied from https://github.com/keefmarshall/pdfpoc
 * Take another document format e.g. JPEG and convert to PDF
 */
public class PdfConverter {

    private static final int MARGIN = 50; // TODO allow this to be configurable

    public byte[] jpegToPdf(byte[] imageBytes) throws IOException {
        // create blank PDF
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            // Load image:
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(doc, imageBytes, null);

            int imageHeight = pdImage.getHeight();
            int imageWidth = pdImage.getWidth();

            float hscale = (page.getCropBox().getWidth() - (MARGIN * 2)) / imageWidth;
            float vscale = (page.getCropBox().getHeight() - (MARGIN * 2)) / imageHeight;
            float scale = Math.min(hscale, vscale);

            // Place image near the top of the page:
            float ypos = page.getCropBox().getHeight() - (imageHeight * scale) - MARGIN;

            // Add image to document
            try (PDPageContentStream contentStream =
                     new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                contentStream.drawImage(pdImage, MARGIN, ypos, imageWidth * scale, imageHeight * scale);
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                doc.save(baos);
                return baos.toByteArray();
            }
        }
    }
}
