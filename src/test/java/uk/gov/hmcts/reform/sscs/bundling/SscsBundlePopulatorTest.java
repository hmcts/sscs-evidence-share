package uk.gov.hmcts.reform.sscs.bundling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

public class SscsBundlePopulatorTest {

    SscsBundlePopulator sscsBundlePopulator;

    @Before
    public void setup() {
        sscsBundlePopulator = new SscsBundlePopulator();
    }

    @Test
    public void givenACaseWithDocuments_thenCreateABundle() {
        List<SscsDocument> docs = new ArrayList<>();

        SscsDocument sscsDocument = SscsDocument.builder().value(
            SscsDocumentDetails.builder().documentFileName("TestFileName")
                .documentLink(
                DocumentLink.builder().documentUrl("test.com").documentBinaryUrl("test.com/binary").documentFilename("MyFile.jpg").build()).build())
            .build();

        docs.add(sscsDocument);

        SscsCaseData caseData = SscsCaseData.builder().sscsDocument(docs).build();

        Bundle result = sscsBundlePopulator.populateNewBundle(caseData);

        assertEquals("TestFileName", result.getValue().getDocuments().get(0).getValue().getName());
        assertEquals(0, result.getValue().getDocuments().get(0).getValue().getSortIndex());
        assertEquals("test.com", result.getValue().getDocuments().get(0).getValue().getSourceDocument().getDocumentUrl());
        assertEquals("test.com/binary", result.getValue().getDocuments().get(0).getValue().getSourceDocument().getDocumentBinaryUrl());
        assertEquals("MyFile.jpg", result.getValue().getDocuments().get(0).getValue().getSourceDocument().getDocumentFilename());
        assertEquals("yes", result.getValue().getEligibleForStitching());
        assertEquals("SSCS DWP Bundle", result.getValue().getTitle());
    }

    @Test
    public void givenACaseWithMultipleDocuments_thenCreateABundleWithDl6First() {
        List<SscsDocument> docs = new ArrayList<>();

        SscsDocument sscsDocument1 = SscsDocument.builder().value(
            SscsDocumentDetails.builder().documentFileName("TestFileName1").documentType("appellantEvidence")
                .documentLink(
                    DocumentLink.builder().documentUrl("test1.com").documentBinaryUrl("test1.com/binary").documentFilename("MyFile1.jpg").build()).build())
            .build();

        SscsDocument dl6Document2 = SscsDocument.builder().value(
            SscsDocumentDetails.builder().documentFileName("dl6.pdf").documentType("dl6")
                .documentLink(
                    DocumentLink.builder().documentUrl("test2.com").documentBinaryUrl("test2.com/binary").documentFilename("MyFile2.jpg").build()).build())
            .build();

        SscsDocument sscsDocument3 = SscsDocument.builder().value(
            SscsDocumentDetails.builder().documentFileName("TestFileName3").documentType("appellantEvidence")
                .documentLink(
                    DocumentLink.builder().documentUrl("test3.com").documentBinaryUrl("test3.com/binary").documentFilename("MyFile3.jpg").build()).build())
            .build();

        docs.add(sscsDocument1);
        docs.add(dl6Document2);
        docs.add(sscsDocument3);

        SscsCaseData caseData = SscsCaseData.builder().sscsDocument(docs).build();

        Bundle result = sscsBundlePopulator.populateNewBundle(caseData);

        assertEquals(0, result.getValue().getDocuments().get(0).getValue().getSortIndex());
        assertEquals("test2.com", result.getValue().getDocuments().get(0).getValue().getSourceDocument().getDocumentUrl());
        assertEquals("test2.com/binary", result.getValue().getDocuments().get(0).getValue().getSourceDocument().getDocumentBinaryUrl());
        assertEquals("MyFile2.jpg", result.getValue().getDocuments().get(0).getValue().getSourceDocument().getDocumentFilename());
        assertEquals("yes", result.getValue().getEligibleForStitching());
        assertEquals("dl6.pdf", result.getValue().getDocuments().get(0).getValue().getName());

        assertEquals("TestFileName1", result.getValue().getDocuments().get(1).getValue().getName());
        assertEquals(1, result.getValue().getDocuments().get(1).getValue().getSortIndex());
        assertEquals("test1.com", result.getValue().getDocuments().get(1).getValue().getSourceDocument().getDocumentUrl());
        assertEquals("test1.com/binary", result.getValue().getDocuments().get(1).getValue().getSourceDocument().getDocumentBinaryUrl());
        assertEquals("MyFile1.jpg", result.getValue().getDocuments().get(1).getValue().getSourceDocument().getDocumentFilename());

        assertEquals("TestFileName3", result.getValue().getDocuments().get(2).getValue().getName());
        assertEquals(2, result.getValue().getDocuments().get(2).getValue().getSortIndex());
        assertEquals("test3.com", result.getValue().getDocuments().get(2).getValue().getSourceDocument().getDocumentUrl());
        assertEquals("test3.com/binary", result.getValue().getDocuments().get(2).getValue().getSourceDocument().getDocumentBinaryUrl());
        assertEquals("MyFile3.jpg", result.getValue().getDocuments().get(2).getValue().getSourceDocument().getDocumentFilename());
        assertEquals("yes", result.getValue().getEligibleForStitching());

        assertEquals("SSCS DWP Bundle", result.getValue().getTitle());
    }

    @Test
    public void givenACaseWithMultipleDocuments_thenCreateABundleWithDl16First() {
        List<SscsDocument> docs = new ArrayList<>();

        SscsDocument sscsDocument1 = SscsDocument.builder().value(
            SscsDocumentDetails.builder().documentFileName("TestFileName1").documentType("appellantEvidence")
                .documentLink(
                    DocumentLink.builder().documentUrl("test1.com").documentBinaryUrl("test1.com/binary").documentFilename("MyFile1.jpg").build()).build())
            .build();

        SscsDocument dl16Document2 = SscsDocument.builder().value(
            SscsDocumentDetails.builder().documentFileName("dl16.pdf").documentType("dl16")
                .documentLink(
                    DocumentLink.builder().documentUrl("test2.com").documentBinaryUrl("test2.com/binary").documentFilename("MyFile2.jpg").build()).build())
            .build();

        SscsDocument sscsDocument3 = SscsDocument.builder().value(
            SscsDocumentDetails.builder().documentFileName("TestFileName3").documentType("appellantEvidence")
                .documentLink(
                    DocumentLink.builder().documentUrl("test3.com").documentBinaryUrl("test3.com/binary").documentFilename("MyFile3.jpg").build()).build())
            .build();

        docs.add(sscsDocument1);
        docs.add(dl16Document2);
        docs.add(sscsDocument3);

        SscsCaseData caseData = SscsCaseData.builder().sscsDocument(docs).build();

        Bundle result = sscsBundlePopulator.populateNewBundle(caseData);

        assertEquals(0, result.getValue().getDocuments().get(0).getValue().getSortIndex());
        assertEquals("test2.com", result.getValue().getDocuments().get(0).getValue().getSourceDocument().getDocumentUrl());
        assertEquals("test2.com/binary", result.getValue().getDocuments().get(0).getValue().getSourceDocument().getDocumentBinaryUrl());
        assertEquals("MyFile2.jpg", result.getValue().getDocuments().get(0).getValue().getSourceDocument().getDocumentFilename());
        assertEquals("yes", result.getValue().getEligibleForStitching());
        assertEquals("dl16.pdf", result.getValue().getDocuments().get(0).getValue().getName());

        assertEquals("TestFileName1", result.getValue().getDocuments().get(1).getValue().getName());
        assertEquals(1, result.getValue().getDocuments().get(1).getValue().getSortIndex());
        assertEquals("test1.com", result.getValue().getDocuments().get(1).getValue().getSourceDocument().getDocumentUrl());
        assertEquals("test1.com/binary", result.getValue().getDocuments().get(1).getValue().getSourceDocument().getDocumentBinaryUrl());
        assertEquals("MyFile1.jpg", result.getValue().getDocuments().get(1).getValue().getSourceDocument().getDocumentFilename());

        assertEquals("TestFileName3", result.getValue().getDocuments().get(2).getValue().getName());
        assertEquals(2, result.getValue().getDocuments().get(2).getValue().getSortIndex());
        assertEquals("test3.com", result.getValue().getDocuments().get(2).getValue().getSourceDocument().getDocumentUrl());
        assertEquals("test3.com/binary", result.getValue().getDocuments().get(2).getValue().getSourceDocument().getDocumentBinaryUrl());
        assertEquals("MyFile3.jpg", result.getValue().getDocuments().get(2).getValue().getSourceDocument().getDocumentFilename());
        assertEquals("yes", result.getValue().getEligibleForStitching());

        assertEquals("SSCS DWP Bundle", result.getValue().getTitle());
    }
}
