/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.qa.util.cluster;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.camunda.operate.webapp.api.v1.entities.ProcessInstance;
import io.camunda.zeebe.util.Either;
import java.util.List;

public interface TestClient {

  Either<Exception, ProcessInstanceResult> getProcessInstanceWith(final long key);

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ProcessInstanceResult(
      @JsonProperty("items") List<ProcessInstance> processInstances,
      @JsonProperty("total") long total, // V1 Property
      @JsonProperty("page") Page page) // V2 Property
  {}

  // Only in V2
  record Page(@JsonProperty("totalItems") long totalItems) {}
}
