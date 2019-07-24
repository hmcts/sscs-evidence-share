package uk.gov.hmcts.reform.sscs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocmosisPdfGenerationService;
import uk.gov.hmcts.reform.sscs.docmosis.service.PdfGenerationService;

@Configuration
public class PdfGenerationServiceConfig {
    @Bean
    public PdfGenerationService docmosisPdfGenerationService() {
        return new DocmosisPdfGenerationService();
    }
}
