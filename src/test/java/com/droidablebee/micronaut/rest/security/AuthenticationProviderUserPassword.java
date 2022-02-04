package com.droidablebee.micronaut.rest.security;

import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationProvider;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.List;
import java.util.Map;

import static com.droidablebee.micronaut.rest.endpoint.PersonEndpoint.PERSON_READ_PERMISSION;
import static com.droidablebee.micronaut.rest.endpoint.PersonEndpoint.PERSON_WRITE_PERMISSION;

@Singleton
public class AuthenticationProviderUserPassword implements AuthenticationProvider {

    public static final User USER_WITHOUT_ROLES = new User("user-without-roles", "password1", List.of());
    public static final User USER_WITH_READ_ROLE = new User("user-with-read-role", "password2", List.of(PERSON_READ_PERMISSION));
    public static final User USER_WITH_WRITE_ROLE = new User("user-with-write-role", "password3", List.of(PERSON_WRITE_PERMISSION));

    static Map<String, User> users = Map.of(
            USER_WITHOUT_ROLES.getUsername(), USER_WITHOUT_ROLES,
            USER_WITH_READ_ROLE.getUsername(), USER_WITH_READ_ROLE,
            USER_WITH_WRITE_ROLE.getUsername(), USER_WITH_WRITE_ROLE
    );

    @Override
    public Publisher<AuthenticationResponse> authenticate(HttpRequest<?> httpRequest, AuthenticationRequest<?, ?> authenticationRequest) {
        return Flux.create(emitter -> {

            User user = users.get(authenticationRequest.getIdentity());

            if (user != null && authenticationRequest.getSecret().equals(user.getPassword())) {
                emitter.next(AuthenticationResponse.success((String) authenticationRequest.getIdentity(), user.getRoles()));
                emitter.complete();
            } else {
                emitter.error(AuthenticationResponse.exception());
            }
        }, FluxSink.OverflowStrategy.ERROR);
    }

    static public class User {
        private String username;
        private String password;
        private List<String> roles;

        public User(String username, String password, List<String> roles) {
            this.username = username;
            this.password = password;
            this.roles = roles;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }
}