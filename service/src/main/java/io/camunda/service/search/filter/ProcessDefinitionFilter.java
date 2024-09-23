/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.service.search.filter;

import io.camunda.util.ObjectBuilder;

public record ProcessDefinitionFilter() implements FilterBase {

  public static final class Builder implements ObjectBuilder<ProcessDefinitionFilter> {

    @Override
    public ProcessDefinitionFilter build() {
      return new ProcessDefinitionFilter();
    }
  }
}