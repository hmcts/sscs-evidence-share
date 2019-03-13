package uk.gov.hmcts.reform.sscs.docmosis.service;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.sscs.docmosis.config.PdfDocumentConfig;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.PdfDocumentRequest;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;

@Service
@Slf4j
public class DocmosisPdfGenerationService implements PdfGenerationService {

    private static final String CASE_DETAILS = "caseDetails";
    private static final String CASE_DATA = "case_data";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PdfDocumentConfig pdfDocumentConfig;

    @Value("${service.pdf-service.uri}")
    private String pdfServiceEndpoint;

    @Value("${service.pdf-service.accessKey}")
    private String pdfServiceAccessKey;

    @Override
    public byte[] generateDocFrom(DocumentHolder documentHolder) {

        String templateName = documentHolder.getTemplate().getName();

        checkArgument(!isNullOrEmpty(templateName), "document generation template cannot be empty");
        checkNotNull(documentHolder.getPlaceholders(), "placeholders map cannot be null");

        log.info("Making request to pdf service to generate pdf document with template {} "
            + "and placeholders of size [{}]", templateName, documentHolder.getPlaceholders().size());

        try {
            ResponseEntity<byte[]> response =
                restTemplate.postForEntity(pdfServiceEndpoint, request(templateName, documentHolder.getPlaceholders()), byte[].class);
            return response.getBody();
        } catch (Exception e) {
            throw new PdfGenerationException("Failed to request PDF from REST endpoint " + e.getMessage(), e);
        }
    }

    private PdfDocumentRequest request(String templateName, Map<String, Object> placeholders) {
        return PdfDocumentRequest.builder()
                .accessKey(pdfServiceAccessKey)
                .templateName(templateName)
                .outputName("result.pdf")
                .data(caseData(placeholders)).build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> caseData(Map<String, Object> placeholders) {
        Map<String, Object> data = (Map<String, Object>) ((Map) placeholders.get(CASE_DETAILS)).get(CASE_DATA);
        data.put(pdfDocumentConfig.getDisplayTemplateKey(), pdfDocumentConfig.getDisplayTemplateVal());
        data.put(pdfDocumentConfig.getFamilyCourtImgKey(), pdfDocumentConfig.getFamilyCourtImgVal());
        data.put(pdfDocumentConfig.getHmctsImgKey(), pdfDocumentConfig.getHmctsImgVal());

        return data;
    }
}
