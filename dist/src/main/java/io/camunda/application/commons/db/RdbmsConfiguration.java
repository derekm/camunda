/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.application.commons.db;

import io.camunda.db.rdbms.RdbmsDBConfiguration;
import io.camunda.db.rdbms.RdbmsService;
import io.camunda.exporter.rdbms.RdbmsExporterFactory;
import io.camunda.zeebe.broker.exporter.repo.ExporterFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(
    prefix = "camunda.database",
    name = "type",
    havingValue = "rdbms",
    matchIfMissing = false)
@Import(RdbmsDBConfiguration.class)
public class RdbmsConfiguration {

  @Bean
  public ExporterFactory rdbmsExporterFactory(final RdbmsService rdbmsService) {
    return new RdbmsExporterFactory(rdbmsService);
  }
}
