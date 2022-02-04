package com.droidablebee.micronaut.rest.endpoint;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.security.authentication.UsernamePasswordCredentials;
import io.micronaut.security.token.jwt.render.BearerAccessRefreshToken;
import jakarta.inject.Inject;

import static io.micronaut.http.HttpRequest.POST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

abstract class BaseEndpointTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Value("${micronaut.application.name}")
    String applicationName;

    String username = "sherlock";
    String password = "password";

    /**
     * Returns a BearerAccessRefreshToken.
     */
    protected BearerAccessRefreshToken loginAndAssert(UsernamePasswordCredentials credentials) {

        HttpResponse<BearerAccessRefreshToken> response = login(credentials);

        assertEquals(HttpStatus.OK, response.getStatus());

        BearerAccessRefreshToken bearerAccessRefreshToken = response.body();
        assertNotNull(bearerAccessRefreshToken);
        assertEquals(credentials.getUsername(), bearerAccessRefreshToken.getUsername());
        assertNotNull(bearerAccessRefreshToken.getAccessToken());

        return bearerAccessRefreshToken;
    }

    protected HttpResponse<BearerAccessRefreshToken> login(UsernamePasswordCredentials credentials) {

        HttpResponse<BearerAccessRefreshToken> response = client.toBlocking().exchange(
                POST("/login", credentials),
                BearerAccessRefreshToken.class
        );

        return response;
    }

}
