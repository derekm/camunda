/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.writer;

import io.camunda.optimize.dto.optimize.index.TimestampBasedImportIndexDto;
import io.camunda.optimize.service.db.repository.ImportRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
@Slf4j
public class ImportIndexWriter {
  private final ImportRepository importRepository;

  public void importIndexes(List<TimestampBasedImportIndexDto> timestampBasedImportIndexDtos) {
    String importItemName = "import index information";
    log.debug("Writing [{}] {} to database.", timestampBasedImportIndexDtos.size(), importItemName);
    importRepository.importIndices(importItemName, timestampBasedImportIndexDtos);
  }
}