package com.droidablebee.micronaut.rest.endpoint;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ReadContext;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Providing error type is critical for `micronaut.http.client.exception-on-error-status: false` to work.
 * See https://github.com/micronaut-projects/micronaut-core/blob/7ca6534245959979139fbf7440b57c8d14b7669c/http-client/src/main/java/io/micronaut/http/client/netty/DefaultHttpClient.java#L2409
 */
@MicronautTest
class ManagementEndpointTest {

    @Inject
    @Client("/")
    HttpClient client;

    /**
     * https://docs.micronaut.io/latest/guide/#infoEndpoint
     */
    @Test
    void getInfo() {

        HttpResponse<String> response = client.toBlocking().exchange(
                HttpRequest.GET("/management/info"),
                Argument.of(String.class),
                Argument.of(String.class)
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertTrue(response.getContentType().isPresent());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getContentType().get());

        ReadContext ctx = JsonPath.parse(response.body());

        assertThat(ctx.read("$.build"), isA(Object.class));
        assertThat(ctx.read("$.build.version"), isA(String.class));
        assertThat(ctx.read("$.build.name"), is("micronaut-rest-example"));
        assertThat(ctx.read("$.build.group"), is("com.droidablebee"));
        assertThat(ctx.read("$.build.time"), isA(String.class));
    }

    /**
     * https://docs.micronaut.io/latest/guide/#healthEndpoint
     */
    @Test
    void getHealth() {

        HttpResponse<String> response = client.toBlocking().exchange(
                HttpRequest.GET("/management/health"),
                Argument.of(String.class),
                Argument.of(String.class)
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertTrue(response.getContentType().isPresent());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getContentType().get());

        ReadContext ctx = JsonPath.parse(response.body(),
                Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS));

        assertThat(ctx.read("$.status"), is("UP"));
        assertThat(ctx.read("$.details"), nullValue());
    }

//    @Test
//     void getHealthAuthorized() throws Exception {
//
//        mockMvc.perform(get("/actuator/health").with(jwt()))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(JSON_MEDIA_TYPE))
//                .andExpect(jsonPath("$.status", is("UP")))
//                .andExpect(jsonPath("$.components").doesNotExist())
//        ;
//    }

//    @Test
//     void getHealthAuthorizedWithConfiguredRole() throws Exception {
//
//        mockMvc.perform(get("/actuator/health").with(jwtWithScope("health-details")))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(JSON_MEDIA_TYPE))
//                .andExpect(jsonPath("$.status", is("UP")))
//                .andExpect(jsonPath("$.components", isA(Object.class)))
//                .andExpect(jsonPath("$.components.diskSpace.status", is("UP")))
//                .andExpect(jsonPath("$.components.diskSpace.details", isA(Object.class)))
//                .andExpect(jsonPath("$.components.db.status", is("UP")))
//                .andExpect(jsonPath("$.components.db.details", isA(Object.class)))
//                .andExpect(jsonPath("$.components.ping.status", is("UP")))
//        ;
//    }

    /**
     * https://docs.micronaut.io/latest/guide/#environmentEndpoint
     */
    @Test
    void getEnv() {

        HttpResponse<String> response = client.toBlocking().exchange(
                HttpRequest.GET("/management/env"),
                Argument.of(String.class),
                Argument.of(String.class)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatus());
    }

//    @Test
//     void getEnvAuthorized() throws Exception {
//
//        mockMvc.perform(get("/actuator/env").with(jwt()))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(JSON_MEDIA_TYPE))
//                .andExpect(jsonPath("$.activeProfiles", isA(JSONArray.class)))
//                .andExpect(jsonPath("$.propertySources", isA(JSONArray.class)))
//        ;
//    }

//    @Test
//     void getCustom() throws Exception {
//
//        mockMvc.perform(get("/actuator/" + CustomActuatorEndpoint.CUSTOM))
//                .andDo(print())
//                .andExpect(status().isUnauthorized())
//        ;
//    }

//    @Test
//     void getCustomAuthorized() throws Exception {
//
//        mockMvc.perform(get("/actuator/" + CustomActuatorEndpoint.CUSTOM).with(jwt()))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(JSON_MEDIA_TYPE))
//                .andExpect(content().string("{}"))
//        ;
//    }

    @Test
    void getLoggers() {

        HttpResponse<String> response = client.toBlocking().exchange(
                HttpRequest.GET("/management/loggers"),
                Argument.of(String.class),
                Argument.of(String.class)
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertTrue(response.getContentType().isPresent());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getContentType().get());

        ReadContext ctx = JsonPath.parse(response.body());

        assertThat(ctx.read("$.levels"), isA(JSONArray.class));
        assertThat(ctx.read("$.levels.size()"), is(8));
        assertThat(ctx.read("$.loggers.size()"), greaterThan(100));
        assertThat(ctx.read("$.loggers.ROOT.configuredLevel"), is("INFO"));
        assertThat(ctx.read("$.loggers.ROOT.effectiveLevel"), is("INFO"));
        assertThat(ctx.read("$.loggers.com.configuredLevel"), is("NOT_SPECIFIED"));
        assertThat(ctx.read("$.loggers.com.effectiveLevel"), is("INFO"));
    }

    @ParameterizedTest
    @CsvSource({
            "com.droidablebee, DEBUG, DEBUG"
    })
    void getSpecificLogger(String logger, String configured, String effective) {

        HttpResponse<String> response = client.toBlocking().exchange(
                HttpRequest.GET("/management/loggers/" + logger),
                Argument.of(String.class),
                Argument.of(String.class)
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertTrue(response.getContentType().isPresent());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getContentType().get());

        ReadContext ctx = JsonPath.parse(response.body());

        assertThat(ctx.read("$.size()"), is(2));
        assertThat(ctx.read("$.configuredLevel"), is(configured));
        assertThat(ctx.read("$.effectiveLevel"), is(effective));
    }

    @Test
    void setSpecificLoggerLevel() {

        String payload = "{ \"configuredLevel\": \"ERROR\" }";

        HttpResponse<String> response = client.toBlocking().exchange(
                HttpRequest.POST("/management/loggers/com.droidablebee.test", payload),
                Argument.of(String.class),
                Argument.of(String.class)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatus());
    }

    @Test()
    @Disabled("add authorization")
    void setSpecificLoggerLevelAuthorized() {

        String logger = "com.droidablebee.test";

        getSpecificLogger(logger, "NOT_SPECIFIED", "DEBUG");

        String payload = "{ \"configuredLevel\": \"ERROR\" }";

        HttpResponse<String> response = client.toBlocking().exchange(
                HttpRequest.POST("/management/loggers/" + logger, payload),
                Argument.of(String.class),
                Argument.of(String.class)
        );

        assertEquals(HttpStatus.OK, response.getStatus());

        getSpecificLogger(logger, "ERROR", "ERROR");
    }

}
