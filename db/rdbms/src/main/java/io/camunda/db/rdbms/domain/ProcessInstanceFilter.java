/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.db.rdbms.domain;

import java.util.Collection;

public record ProcessInstanceFilter(
    String bpmnProcessId, VariableFilter variable, Integer limit, Integer offset) {

  public record VariableFilter(String name, Collection<String> values) {}
}