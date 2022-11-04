package uk.gov.hmcts.reform.sscs.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
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
                .title("SSCS Evidence Share")
                .description("Microservice to handle sending evidence via an SFTP server to DWP from SSCS")
                .version("1.0.0")
                .license(new License()
                    .name("The MIT License (MIT)")
                    .url("https://github.com/hmcts/sscs-evidence-share/blob/master/LICENSE")))
                    .externalDocs(new ExternalDocumentation()
                        .description("Building and running the application")
                        .url("https://github.com/hmcts/sscs-evidence-share"));
    }

}
