/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.filter;

import com.fasterxml.jackson.core.type.TypeReference;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.optimize.dto.optimize.query.report.single.filter.data.date.RelativeDateFilterUnit;
import org.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
import org.camunda.optimize.dto.optimize.query.report.single.process.result.raw.RawDataProcessInstanceDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.result.raw.RawDataProcessReportResultDto;
import org.camunda.optimize.dto.optimize.rest.report.AuthorizedProcessReportEvaluationResultDto;
import org.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import org.camunda.optimize.test.it.extension.ElasticSearchIntegrationTestExtensionRule;
import org.camunda.optimize.test.it.extension.EmbeddedOptimizeExtensionRule;
import org.camunda.optimize.test.it.extension.EngineDatabaseExtensionRule;
import org.camunda.optimize.test.it.extension.EngineIntegrationExtensionRule;
import org.camunda.optimize.test.util.ProcessReportDataBuilder;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.camunda.optimize.test.util.ProcessReportDataType.RAW_DATA;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.IsNull.notNullValue;

public abstract class AbstractRollingDateFilterIT {

  @RegisterExtension
  @Order(1)
  public ElasticSearchIntegrationTestExtensionRule elasticSearchIntegrationTestExtensionRule = new ElasticSearchIntegrationTestExtensionRule();
  @RegisterExtension
  @Order(2)
  public EngineIntegrationExtensionRule engineIntegrationExtensionRule = new EngineIntegrationExtensionRule();
  @RegisterExtension
  @Order(3)
  public EngineDatabaseExtensionRule engineDatabaseExtensionRule = new EngineDatabaseExtensionRule(engineIntegrationExtensionRule.getEngineName());
  @RegisterExtension
  @Order(4)
  public EmbeddedOptimizeExtensionRule embeddedOptimizeExtensionRule = new EmbeddedOptimizeExtensionRule();

  protected ProcessInstanceEngineDto deployAndStartSimpleProcess() {
    return deployAndStartSimpleProcessWithVariables(new HashMap<>());
  }

  private ProcessInstanceEngineDto deployAndStartSimpleProcessWithVariables(Map<String, Object> variables) {
    // @formatter:off
    BpmnModelInstance processModel = Bpmn.createExecutableProcess("aProcess")
      .name("aProcessName")
      .startEvent()
       .serviceTask()
       .camundaExpression("${true}")
       .userTask()
      .endEvent()
      .done();
    // @formatter:on
    return engineIntegrationExtensionRule.deployAndStartProcessWithVariables(processModel, variables);
  }

  protected void assertResults(
    ProcessInstanceEngineDto processInstance,
    AuthorizedProcessReportEvaluationResultDto<RawDataProcessReportResultDto> evaluationResult,
    int expectedPiCount) {

    final ProcessReportDataDto resultDataDto = evaluationResult.getReportDefinition().getData();
    assertThat(resultDataDto.getDefinitionVersions(), contains(processInstance.getProcessDefinitionVersion()));
    assertThat(resultDataDto.getProcessDefinitionKey(), is(processInstance.getProcessDefinitionKey()));
    assertThat(resultDataDto.getView(), is(notNullValue()));
    final List<RawDataProcessInstanceDto> resultData = evaluationResult.getResult().getData();
    assertThat(resultData, is(notNullValue()));
    assertThat("rolling date result size", resultData.size(), is(expectedPiCount));

    if (expectedPiCount > 0) {
      RawDataProcessInstanceDto rawDataProcessInstanceDto = resultData.get(0);
      assertThat(rawDataProcessInstanceDto.getProcessInstanceId(), is(processInstance.getId()));
    }
  }

  protected AuthorizedProcessReportEvaluationResultDto<RawDataProcessReportResultDto> createAndEvaluateReportWithRollingStartDateFilter(
    String processDefinitionKey,
    String processDefinitionVersion,
    RelativeDateFilterUnit unit,
    boolean newToken
  ) {
    ProcessReportDataDto reportData = ProcessReportDataBuilder
      .createReportData()
      .setProcessDefinitionKey(processDefinitionKey)
      .setProcessDefinitionVersion(processDefinitionVersion)
      .setReportDataType(RAW_DATA)
      .build();
    List<ProcessFilterDto> rollingDateFilter = ProcessFilterBuilder
      .filter()
      .relativeStartDate()
      .start(1L, unit)
      .add()
      .buildList();

    reportData.setFilter(rollingDateFilter);
    return evaluateReport(reportData, newToken);
  }

  protected AuthorizedProcessReportEvaluationResultDto<RawDataProcessReportResultDto> createAndEvaluateReportWithRollingEndDateFilter(
    String processDefinitionKey,
    String processDefinitionVersion,
    RelativeDateFilterUnit unit,
    boolean newToken
  ) {
    ProcessReportDataDto reportData = ProcessReportDataBuilder
      .createReportData()
      .setProcessDefinitionKey(processDefinitionKey)
      .setProcessDefinitionVersion(processDefinitionVersion)
      .setReportDataType(RAW_DATA)
      .build();
    List<ProcessFilterDto> rollingDateFilter = ProcessFilterBuilder
      .filter()
      .relativeEndDate()
      .start(1L, unit)
      .add()
      .buildList();

    reportData.setFilter(rollingDateFilter);
    return evaluateReport(reportData, newToken);
  }

  protected AuthorizedProcessReportEvaluationResultDto<RawDataProcessReportResultDto> evaluateReport(ProcessReportDataDto reportData, boolean newToken) {
    if (newToken) {
      return evaluateReportWithNewToken(reportData);
    } else {
      return evaluateReport(reportData);
    }
  }

  protected AuthorizedProcessReportEvaluationResultDto<RawDataProcessReportResultDto> evaluateReport(ProcessReportDataDto reportData) {
    return embeddedOptimizeExtensionRule
      .getRequestExecutor()
      .buildEvaluateSingleUnsavedReportRequest(reportData)
      // @formatter:off
      .execute(new TypeReference<AuthorizedProcessReportEvaluationResultDto<RawDataProcessReportResultDto>>() {});
      // @formatter:on
  }

  private AuthorizedProcessReportEvaluationResultDto<RawDataProcessReportResultDto> evaluateReportWithNewToken(ProcessReportDataDto reportData) {
    return embeddedOptimizeExtensionRule
      .getRequestExecutor()
      .withGivenAuthToken(embeddedOptimizeExtensionRule.getNewAuthenticationToken())
      .buildEvaluateSingleUnsavedReportRequest(reportData)
      // @formatter:off
      .execute(new TypeReference<AuthorizedProcessReportEvaluationResultDto<RawDataProcessReportResultDto>>() {});
      // @formatter:on
  }

  protected Response evaluateReportAndReturnResponse(ProcessReportDataDto reportData) {
    return embeddedOptimizeExtensionRule
      .getRequestExecutor()
      .buildEvaluateSingleUnsavedReportRequest(reportData)
      .execute();
  }

}
