package uk.gov.hmcts.reform.sscs.docmosis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "document.pdf")
public class PdfDocumentConfig {

    private String hmctsImgKey;
    private String hmctsImgVal;
}
