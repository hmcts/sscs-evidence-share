package uk.gov.hmcts.reform.sscs.bundling;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@Service
public class SscsBundlePopulator {

    public Bundle populateNewBundle(SscsCaseData caseData) {

        List<SscsDocument> sscsDocuments = sortBundledDocuments(caseData.getSscsDocument());

        List<BundleDocument> bundledDocs = new ArrayList<>();
        for (int i = 0; i < sscsDocuments.size(); i++) {
            SscsDocument sscsDocument = sscsDocuments.get(i);
            BundleDocument doc = BundleDocument.builder().value(
                BundleDocumentDetails.builder()
                    .name(sscsDocument.getValue().getDocumentFileName())
                    .sortIndex(i)
                    .sourceDocument(new DocumentLink(
                        sscsDocument.getValue().getDocumentLink().getDocumentUrl(),
                        sscsDocument.getValue().getDocumentLink().getDocumentBinaryUrl(),
                        sscsDocument.getValue().getDocumentLink().getDocumentFilename())
                    ).build())
                .build();

            bundledDocs.add(doc);
        }

        return Bundle.builder().value(BundleDetails.builder()
            .title("SSCS DWP Bundle")
            .documents(bundledDocs)
            .eligibleForStitching("yes")
            .hasTableOfContents("Yes")
            .build())
        .build();
    }

    private List<SscsDocument> sortBundledDocuments(List<SscsDocument> sscsDocuments) {
        List<SscsDocument> sortedDocuments = new ArrayList<>();

        for (SscsDocument doc : sscsDocuments) {
            if (null != doc.getValue().getDocumentType() && doc.getValue().getDocumentType().equals("DWP response")) {
                sortedDocuments.add(0, doc);
            } else if (null != doc.getValue() && null != doc.getValue().getBundleAddition()) {
                sortedDocuments.add(doc);
            }
        }
        return sortedDocuments;
    }

}
