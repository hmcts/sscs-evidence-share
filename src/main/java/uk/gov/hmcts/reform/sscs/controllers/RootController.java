package uk.gov.hmcts.reform.sscs.controllers;

import static org.springframework.http.ResponseEntity.ok;

import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Default endpoints per application.
 */
@RestController
public class RootController {

    /**
     * Root GET endpoint.
     *
     * <p>Azure application service has a hidden feature of making requests to root endpoint when
     * "Always On" is turned on.
     * This is the endpoint to deal with that and therefore silence the unnecessary 404s as a response code.
     *
     * @return Welcome message from the service.
     */
    @GetMapping("/")
    public ResponseEntity<String> welcome(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("myCookie", "myCookieValue")
            .sameSite("Strict")
            .path("/")
            .secure(true)
            .httpOnly(true)
            .build();

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ok("Welcome to sscs-evidence-share");
    }
}
