package uk.gov.hmcts.reform.sscs.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

class PdfConverterTest {

    @Test
    void testJpegToPdfConversion() throws Exception {
        PdfConverter conv = new PdfConverter();
        byte[] imageBytes = IOUtils.resourceToByteArray("/pdf/dl6.jpg");
        byte[] pdf = conv.jpegToPdf(imageBytes);
        try (PDDocument doc = PDDocument.load(pdf)) {
            assertEquals(1, doc.getPages().getCount());
        }
    }


}
