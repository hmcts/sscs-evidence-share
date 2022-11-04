package uk.gov.hmcts.reform.sscs.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {
    @Bean
    public OpenAPI api() {
        return new OpenAPI()
            .info(new Info()
                .title("sscs-evidence-share")
                .description("SSCS Evidence Share")
                .version("1.0.0")
                .contact(new Contact()
                    .url("https://www.gov.uk/government/publications/first-tier-tribunal-social-security-and-child-support-hearing-lists/"))
                .license(new License()
                    .name("The MIT License (MIT)")
                    .url("https://github.com/hmcts/sscs-evidence-share/blob/master/LICENSE")));
    }

}
