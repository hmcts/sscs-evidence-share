package uk.gov.hmcts.reform.sscs.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {
    /*

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
            .useDefaultResponseMessages(false)
            .select()
            .apis(RequestHandlerSelectors.basePackage(SscsEvidenceShareApplication.class.getPackage().getName() + ".controllers"))
            .paths(PathSelectors.any())
            .build();
    }
    */

    // This is boilerplate and requires implementing real values of the above commented code
    @Bean
    public OpenAPI api() {
        return new OpenAPI()
            .info(new Info().title("Blah")
            .description("blah")
            .version("1.2.3")
            .license(new License().name("Apache 2.0").url("http://springdoc.org")))
            .externalDocs(new ExternalDocumentation()
                .description("Deocumnetanetion")
                .url("www.web.io"));
    }

}
