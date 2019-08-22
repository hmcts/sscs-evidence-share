package uk.gov.hmcts.reform.sscs.health;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class DocmosisHealthIndicator implements HealthIndicator {

    private static final Logger LOG = LoggerFactory.getLogger(DocmosisHealthIndicator.class);

    private final String docmosisStatusUri;
    private final RestTemplate restTemplate;

    public DocmosisHealthIndicator(
        @Value("${service.pdf-service.health.uri}") String docmosisStatusUri,
        RestTemplate restTemplate
    ) {
        this.docmosisStatusUri = docmosisStatusUri;
        this.restTemplate = restTemplate;
    }

    public Health health() {

        try {
            return Optional.ofNullable(restTemplate
                    .exchange(
                        docmosisStatusUri,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<Map<String, Object>>() {
                        }
                    ).getBody())
                .filter(response -> response.containsKey("ready"))
                .filter(response -> "true".equalsIgnoreCase((String) response.get("ready")))
                .map(response -> new Health.Builder().up())
                .orElse(new Health.Builder().down())
                .build();
        } catch (RestClientException e) {

            LOG.error("Error performing Docmosis healthcheck", e);
            return new Health.Builder().down(e).build();
        }
    }
}
