package com.droidablebee.micronaut.rest.endpoint;

import com.droidablebee.micronaut.rest.security.AuthenticationProviderUserPassword.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.security.authentication.UsernamePasswordCredentials;
import io.micronaut.security.token.jwt.render.BearerAccessRefreshToken;
import jakarta.inject.Inject;

import java.io.IOException;

import static io.micronaut.http.HttpRequest.POST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

abstract class BaseEndpointTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    ObjectMapper objectMapper;

    @Value("${micronaut.application.name}")
    String applicationName;

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

    protected UsernamePasswordCredentials createCredentials(User user) {

        return new UsernamePasswordCredentials(user.getUsername(), user.getPassword());
    }

    /**
     * Returns json representation of the object.
     * @param o instance
     * @return json
     * @throws IOException
     */
    protected String json(Object o) throws IOException {

        return objectMapper.writeValueAsString(o);
    }
}
