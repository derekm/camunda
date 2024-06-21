/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest.security.cloud;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import lombok.SneakyThrows;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType;

public class AuthorizationRequestCookieValueMapper {

  private final ObjectMapper objectMapper;

  public AuthorizationRequestCookieValueMapper() {
    objectMapper = new ObjectMapper();
    objectMapper.addMixIn(
        OAuth2AuthorizationRequest.class, OAuth2AuthorizationRequestMixin.class);
    objectMapper.addMixIn(
        OAuth2AuthorizationResponseType.class, OAuth2AuthorizationResponseTypeMixin.class);
    objectMapper.addMixIn(AuthorizationGrantType.class, AuthorizationGrantTypeMixin.class);
  }

  @SneakyThrows
  public String serialize(final OAuth2AuthorizationRequest authorizationRequest) {
    return Base64.getUrlEncoder()
        .encodeToString(objectMapper.writeValueAsString(authorizationRequest).getBytes(UTF_8));
  }

  @SneakyThrows
  public OAuth2AuthorizationRequest deserialize(final String value) {
    return objectMapper.readValue(
        Base64.getUrlDecoder().decode(value), OAuth2AuthorizationRequest.class);
  }

  private abstract static class AuthorizationGrantTypeMixin {

    @JsonCreator
    public AuthorizationGrantTypeMixin(@JsonProperty("value") final String value) {
    }
  }

  private abstract static class OAuth2AuthorizationRequestMixin {

    @JsonProperty("grantType")
    AuthorizationGrantType authorizationGrantType;
  }

  private abstract static class OAuth2AuthorizationResponseTypeMixin {

    @JsonCreator
    public OAuth2AuthorizationResponseTypeMixin(@JsonProperty("value") final String value) {
    }
  }
}