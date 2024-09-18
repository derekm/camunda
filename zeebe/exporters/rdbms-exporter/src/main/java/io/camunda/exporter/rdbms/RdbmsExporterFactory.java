/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.exporter.rdbms;

import io.camunda.db.rdbms.RdbmsService;
import io.camunda.zeebe.broker.exporter.repo.ExporterFactory;
import io.camunda.zeebe.broker.exporter.repo.ExporterInstantiationException;
import io.camunda.zeebe.exporter.api.Exporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RdbmsExporterFactory implements ExporterFactory {

  private static final Logger LOG = LoggerFactory.getLogger(RdbmsExporterFactory.class);

  private final RdbmsService rdbmsService;

  public RdbmsExporterFactory(final RdbmsService rdbmsService) {
    this.rdbmsService = rdbmsService;
  }

  @Override
  public String exporterId() {
    return "rdbms";
  }

  @Override
  public Exporter newInstance() throws ExporterInstantiationException {
    LOG.info("Creating new RDBMS exporter instance");
    return new RdbmsExporter(rdbmsService);
  }
}
