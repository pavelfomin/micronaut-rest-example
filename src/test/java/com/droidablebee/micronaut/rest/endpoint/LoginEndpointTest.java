
package com.droidablebee.micronaut.rest.endpoint;

import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.security.authentication.UsernamePasswordCredentials;
import io.micronaut.security.token.jwt.render.BearerAccessRefreshToken;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.text.ParseException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class LoginEndpointTest extends BaseEndpointTest {

    @Test
    void login() throws ParseException {

        BearerAccessRefreshToken bearerAccessRefreshToken = loginAndAssert(new UsernamePasswordCredentials(username, password));
        assertTrue(JWTParser.parse(bearerAccessRefreshToken.getAccessToken()) instanceof SignedJWT);
    }

    @ParameterizedTest
    @MethodSource("credentials")
    @ArgumentsSource(Credentials.class)
    void loginWithInvalidCredentials(String username, String password, HttpStatus status) {

        //error class does not match so exception is thrown
        HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> login(new UsernamePasswordCredentials(username, password))
        );

        assertEquals(status, exception.getStatus());
    }

    /**
     * Simple but requires method name hardcoded in the `@MethodSource` annotation.
     */
    static Stream<Arguments> credentials() {

        return Stream.of(
                Arguments.of(null, null, HttpStatus.BAD_REQUEST),
                Arguments.of(null, "invalid", HttpStatus.BAD_REQUEST),
                Arguments.of("invalid", null, HttpStatus.BAD_REQUEST),
                Arguments.of("invalid", "invalid", HttpStatus.UNAUTHORIZED),
                Arguments.of("invalid", "password", HttpStatus.UNAUTHORIZED),
                Arguments.of("username", "invalid", HttpStatus.UNAUTHORIZED)
        );
    }

    /**
     * A bit more verbose but it's referenced by class name in `@ArgumentsSource` annotation.
     */
    static class Credentials implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

            return credentials();
        }
    }

}
