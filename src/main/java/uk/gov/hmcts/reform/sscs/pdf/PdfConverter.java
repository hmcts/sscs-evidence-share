package uk.gov.hmcts.reform.sscs.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.coobird.thumbnailator.Thumbnails;
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

    private byte[] readImageAsBytesWithCorrectRotation(String imagePath) throws IOException {

        // "Thumbnails" seems like real overkill, but honestly rotating the image correctly
        // is a right pain otherwise. Reading it in at scale 1.0 like this does the trick.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Thumbnails.of(imagePath).scale(1).toOutputStream(baos);
        baos.flush();
        byte[] imageInByte = baos.toByteArray();
        baos.close();

        return imageInByte;
    }

    public byte[] jpegToPdf(byte[] imageBytes) throws IOException {
        // create blank PDF
        try (PDDocument doc = new PDDocument()) {
            // a valid PDF document requires at least one page
            PDPage page = new PDPage(PDRectangle.A4);
            // To get MacOSX Preview to scale exactly 100% for A4 you need to use rounded dimensions:
            // PDRectangle adjustedA4 = new PDRectangle(595F, 842F);
            // PDPage page = new PDPage(adjustedA4);
            doc.addPage(page);

            // Load image:
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(doc, imageBytes, null);

            // need to find the scale, or the image will be too big or too small
            // Q: do we scale up, if the image is too small? Will result in pixellation..
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
