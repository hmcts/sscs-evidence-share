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
                    .url("http://springdoc.org")))
                    .externalDocs(new ExternalDocumentation()
                        .description("Copyright (c) 2018 HMCTS (HM Courts & Tribunals Service)\n"
                            + "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated "
                            + "documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation "
                            + "the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to "
                            + "permit persons to whom the Software is furnished to do so, subject to the following conditions:\n"
                            + "The above copyright notice and this permission notice shall be included in all copies or substantial portions of "
                            + "the Software.\n"
                            + "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED "
                            + "TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE "
                            + "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, "
                            + "TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE "
                            + "SOFTWARE.\n")
                        .url("https://github.com/hmcts/sscs-evidence-share"));
    }

}
