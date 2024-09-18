/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.exporter.rdbms;

import io.camunda.db.rdbms.domain.ProcessDefinitionModel;
import io.camunda.db.rdbms.service.ProcessRdbmsService;
import io.camunda.operate.entities.ProcessEntity;
import io.camunda.operate.zeebeimport.util.XMLUtil;
import io.camunda.zeebe.protocol.Protocol;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.ProcessIntent;
import io.camunda.zeebe.protocol.record.value.deployment.Process;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessExportHandler implements RdbmsExportHandler<Process> {

  private static final Logger LOG = LoggerFactory.getLogger(ProcessExportHandler.class);

  private final ProcessRdbmsService processRdbmsService;

  public ProcessExportHandler(final ProcessRdbmsService processRdbmsService) {
    this.processRdbmsService = processRdbmsService;
  }

  @Override
  public boolean canExport(final Record<Process> record) {
    // We get this Record on each partition, but just the one where the command was executed should
    // export it!
    final int originalPartitionId = Protocol.decodePartitionId(record.getKey());
    return record.getIntent() == ProcessIntent.CREATED
        && originalPartitionId == record.getPartitionId();
  }

  @Override
  public void export(final Record<Process> record) {
    final Process value = record.getValue();
    processRdbmsService.save(map(value));
  }

  private ProcessDefinitionModel map(final Process value) {
    String processName = null;
    try {
      final var xml =
          new XMLUtil().extractDiagramData(value.getResource(), value.getBpmnProcessId());
      processName = xml.map(ProcessEntity::getName).orElse(null);
    } catch (final Exception e) {
      // skip
      LOG.debug("Unable to parse XML diagram", e);
    }

    return new ProcessDefinitionModel(
        value.getProcessDefinitionKey(),
        value.getBpmnProcessId(),
        processName,
        value.getTenantId(),
        value.getVersionTag(),
        value.getVersion());
  }
}