/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.qa.util.cluster;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.util.Either;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

public class TestRestV2ApiClient implements AutoCloseable, TestClient {

  private static final ObjectMapper OBJECT_MAPPER = (new ObjectMapper())
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private final URI endpoint;
  private final HttpClient httpClient;

  public TestRestV2ApiClient(final URI endpoint) {
    this.endpoint = endpoint;

    httpClient = HttpClient.newHttpClient();
  }

  private Either<Exception, HttpRequest> createProcessInstanceRequest(final long key) {
    final HttpRequest request;
    try {
      request =
          HttpRequest.newBuilder()
              .uri(new URI(String.format("%sv2/process-instances/search", endpoint)))
              .header("content-type", "application/json")
              .POST(
                  HttpRequest.BodyPublishers.ofString(
                      String.format(
                          "{\"filter\":{\"key\":%d},\"sort\":[{\"field\":\"endDate\",\"order\":\"ASC\"}],\"size\":20}",
                          key)))
              .build();
    } catch (final URISyntaxException e) {
      return Either.left(e);
    }
    return Either.right(request);
  }

  private Either<Exception, HttpResponse<String>> sendProcessInstanceQuery(
      final HttpRequest request) {
    try {
      return Either.right(httpClient.send(request, BodyHandlers.ofString()));
    } catch (final IOException | InterruptedException e) {
      return Either.left(e);
    }
  }

  private Either<Exception, ProcessInstanceResult> mapProcessInstanceResult(
      final HttpResponse<String> response) {
    if (response.statusCode() != 200) {
      return Either.left(
          new IllegalStateException(
              String.format(
                  "Expected status code 200, but got %d. [Response body: %s, Endpoint: %s].",
                  response.statusCode(), response.body(), response.uri())));
    }

    try {
      return Either.right(OBJECT_MAPPER.readValue(response.body(), ProcessInstanceResult.class));
    } catch (final JsonProcessingException e) {
      return Either.left(e);
    }
  }

  @Override
  public Either<Exception, ProcessInstanceResult> getProcessInstanceWith(final long key) {
    return createProcessInstanceRequest(key)
        .flatMap(this::sendProcessInstanceQuery)
        .flatMap(this::mapProcessInstanceResult);
  }

  @Override
  public void close() throws Exception {
    httpClient.close();
  }

}
