/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.importing.user_task;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.optimize.exception.OptimizeIntegrationTestException;
import org.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import org.camunda.optimize.service.util.OptimizeDateTimeFormatterFactory;
import org.camunda.optimize.service.util.configuration.ConfigurationServiceBuilder;
import org.camunda.optimize.service.util.mapper.ObjectMapperFactory;
import org.camunda.optimize.test.it.extension.ElasticSearchIntegrationTestExtensionRule;
import org.camunda.optimize.test.it.extension.EmbeddedOptimizeExtensionRule;
import org.camunda.optimize.test.it.extension.EngineDatabaseExtensionRule;
import org.camunda.optimize.test.it.extension.EngineIntegrationExtensionRule;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

public abstract class AbstractUserTaskImportIT {

  protected static final String START_EVENT = "startEvent";
  protected static final String END_EVENT = "endEvent";
  protected static final String USER_TASK_1 = "userTask1";
  protected static final String USER_TASK_2 = "userTask2";

  @RegisterExtension
  @Order(1)
  public ElasticSearchIntegrationTestExtensionRule elasticSearchIntegrationTestExtensionRule = new ElasticSearchIntegrationTestExtensionRule();
  @RegisterExtension
  @Order(2)
  public EngineIntegrationExtensionRule engineIntegrationExtensionRule = new EngineIntegrationExtensionRule();
  @RegisterExtension
  @Order(3)
  public EmbeddedOptimizeExtensionRule embeddedOptimizeExtensionRule = new EmbeddedOptimizeExtensionRule();
  @RegisterExtension
  @Order(4)
  public EngineDatabaseExtensionRule engineDatabaseExtensionRule = new EngineDatabaseExtensionRule(engineIntegrationExtensionRule.getEngineName());

  protected ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() {
    if (objectMapper == null) {
      objectMapper = new ObjectMapperFactory(
        new OptimizeDateTimeFormatterFactory().getObject(),
        ConfigurationServiceBuilder.createDefaultConfiguration()
      ).createOptimizeMapper();
    }
  }

  protected void changeUserTaskIdleDuration(final ProcessInstanceEngineDto processInstanceDto,
                                            final long idleDuration) {
    engineIntegrationExtensionRule.getHistoricTaskInstances(processInstanceDto.getId())
      .forEach(historicUserTaskInstanceDto -> {
        try {
          engineDatabaseExtensionRule.changeUserTaskAssigneeOperationTimestamp(
            historicUserTaskInstanceDto.getId(),
            historicUserTaskInstanceDto.getStartTime().plus(idleDuration, ChronoUnit.MILLIS)
          );
        } catch (SQLException e) {
          throw new OptimizeIntegrationTestException(e);
        }
      });
  }

  protected void changeUserTaskWorkDuration(final ProcessInstanceEngineDto processInstanceDto,
                                            final long workDuration) {
    engineIntegrationExtensionRule.getHistoricTaskInstances(processInstanceDto.getId())
      .forEach(historicUserTaskInstanceDto -> {
        if (historicUserTaskInstanceDto.getEndTime() != null) {
          try {
            engineDatabaseExtensionRule.changeUserTaskAssigneeOperationTimestamp(
              historicUserTaskInstanceDto.getId(),
              historicUserTaskInstanceDto.getEndTime().minus(workDuration, ChronoUnit.MILLIS)
            );
          } catch (SQLException e) {
            throw new OptimizeIntegrationTestException(e);
          }
        }
      });
  }

  protected ProcessInstanceEngineDto deployAndStartOneUserTaskProcess() {
    BpmnModelInstance processModel = Bpmn.createExecutableProcess("aProcess")
      .startEvent(START_EVENT)
      .userTask(USER_TASK_1)
      .endEvent(END_EVENT)
      .done();
    return engineIntegrationExtensionRule.deployAndStartProcess(processModel);
  }

  protected ProcessInstanceEngineDto deployAndStartTwoUserTasksProcess() {
    BpmnModelInstance processModel = Bpmn.createExecutableProcess("aProcess")
      .startEvent(START_EVENT)
      .userTask(USER_TASK_1)
      .userTask(USER_TASK_2)
      .endEvent(END_EVENT)
      .done();
    return engineIntegrationExtensionRule.deployAndStartProcess(processModel);
  }

  protected SearchResponse getSearchResponseForAllDocumentsOfType(String elasticsearchType) throws IOException {
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
      .query(matchAllQuery())
      .size(100);

    SearchRequest searchRequest = new SearchRequest()
      .indices(elasticsearchType)
      .types(elasticsearchType)
      .source(searchSourceBuilder);

    return elasticSearchIntegrationTestExtensionRule.getOptimizeElasticClient().search(searchRequest, RequestOptions.DEFAULT);
  }


}
