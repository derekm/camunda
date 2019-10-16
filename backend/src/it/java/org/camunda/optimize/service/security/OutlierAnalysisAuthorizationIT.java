/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.security;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.StartEventBuilder;
import org.camunda.optimize.OptimizeRequestExecutor;
import org.camunda.optimize.dto.engine.ProcessDefinitionEngineDto;
import org.camunda.optimize.exception.OptimizeIntegrationTestException;
import org.camunda.optimize.test.engine.AuthorizationClient;
import org.camunda.optimize.test.it.extension.ElasticSearchIntegrationTestExtensionRule;
import org.camunda.optimize.test.it.extension.EmbeddedOptimizeExtensionRule;
import org.camunda.optimize.test.it.extension.EngineIntegrationExtensionRule;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.camunda.optimize.service.util.configuration.EngineConstantsUtil.RESOURCE_TYPE_PROCESS_DEFINITION;
import static org.camunda.optimize.service.util.configuration.EngineConstantsUtil.RESOURCE_TYPE_TENANT;
import static org.camunda.optimize.test.engine.AuthorizationClient.KERMIT_USER;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class OutlierAnalysisAuthorizationIT {
  private static final String PROCESS_DEFINITION_KEY = "outlierTest";
  private static final String ENDPOINT_FLOW_NODE_OUTLIERS = "flowNodeOutliers";
  private static final String ENDPOINT_DURATION_CHART = "durationChart";
  private static final String ENDPOINT_SIGNIFICANT_OUTLIER_VARIABLE_TERMS = "significantOutlierVariableTerms";
  private static final String ENDPOINT_SIGNIFICANT_OUTLIER_VARIABLE_TERMS_PROCESS_INSTANCE_IDS_EXPORT =
    "significantOutlierVariableTerms/processInstanceIdsExport";
  private static final String FLOW_NODE_ID_START = "start";

  @RegisterExtension
  @Order(1)
  public ElasticSearchIntegrationTestExtensionRule elasticSearchIntegrationTestExtensionRule =
    new ElasticSearchIntegrationTestExtensionRule();
  @RegisterExtension
  @Order(2)
  public EngineIntegrationExtensionRule engineIntegrationExtensionRule = new EngineIntegrationExtensionRule();
  @RegisterExtension
  @Order(3)
  public EmbeddedOptimizeExtensionRule embeddedOptimizeExtensionRule = new EmbeddedOptimizeExtensionRule();

  private final AuthorizationClient authorizationClient = new AuthorizationClient(engineIntegrationExtensionRule);

  private static Stream<String> endpoints() {
    return Stream.of(
      ENDPOINT_FLOW_NODE_OUTLIERS,
      ENDPOINT_DURATION_CHART,
      ENDPOINT_SIGNIFICANT_OUTLIER_VARIABLE_TERMS,
      ENDPOINT_SIGNIFICANT_OUTLIER_VARIABLE_TERMS_PROCESS_INSTANCE_IDS_EXPORT
    );
  }

  @ParameterizedTest
  @MethodSource("endpoints")
  public void outlierEndpoint_unauthenticated(String endpoint) {
    // given
    final String activityId = "chartTestActivity";
    ProcessDefinitionEngineDto processDefinition =
      engineIntegrationExtensionRule.deployProcessAndGetProcessDefinition(getBpmnModelInstance(activityId));

    startInstanceWithSampleVariables(processDefinition);
    startInstanceWithSampleVariables(processDefinition);

    embeddedOptimizeExtensionRule.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtensionRule.refreshAllOptimizeIndices();

    //when
    final Response response = executeRequest(
      processDefinition,
      embeddedOptimizeExtensionRule.getRequestExecutor().withoutAuthentication(),
      null,
      endpoint
    );

    //then
    assertThat(response.getStatus(), is(401));
  }

  @ParameterizedTest
  @MethodSource("endpoints")
  public void outlierEndpoint_authorized(String endpoint) {
    // given
    final String activityId = "chartTestActivity";
    ProcessDefinitionEngineDto processDefinition =
      engineIntegrationExtensionRule.deployProcessAndGetProcessDefinition(getBpmnModelInstance(activityId));

    startInstanceWithSampleVariables(processDefinition);
    startInstanceWithSampleVariables(processDefinition);

    embeddedOptimizeExtensionRule.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtensionRule.refreshAllOptimizeIndices();

    //when
    final Response response = executeRequest(
      processDefinition,
      embeddedOptimizeExtensionRule.getRequestExecutor(),
      null,
      endpoint
    );

    //then
    assertThat(response.getStatus(), is(200));
  }

  @ParameterizedTest
  @MethodSource("endpoints")
  public void outlierEndpoint_notAuthorizedToProcessDefinition(String endpoint) {
    // given
    final String activityId = "chartTestActivity";
    ProcessDefinitionEngineDto processDefinition =
      engineIntegrationExtensionRule.deployProcessAndGetProcessDefinition(getBpmnModelInstance(activityId));

    authorizationClient.addKermitUserAndGrantAccessToOptimize();

    startInstanceWithSampleVariables(processDefinition);
    startInstanceWithSampleVariables(processDefinition);

    embeddedOptimizeExtensionRule.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtensionRule.refreshAllOptimizeIndices();

    //when
    final Response response = executeRequest(
      processDefinition,
      embeddedOptimizeExtensionRule.getRequestExecutor().withUserAuthentication(KERMIT_USER, KERMIT_USER),
      null,
      endpoint
    );

    //then
    assertThat(response.getStatus(), is(403));
  }

  @ParameterizedTest
  @MethodSource("endpoints")
  public void outlierEndpoint_noneTenantAuthorized(String endpoint) {
    // given
    final String activityId = "chartTestActivity";
    ProcessDefinitionEngineDto processDefinition =
      engineIntegrationExtensionRule.deployProcessAndGetProcessDefinition(getBpmnModelInstance(activityId));

    startInstanceWithSampleVariables(processDefinition);
    startInstanceWithSampleVariables(processDefinition);

    embeddedOptimizeExtensionRule.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtensionRule.refreshAllOptimizeIndices();

    //when
    final Response response = executeRequest(
      processDefinition,
      embeddedOptimizeExtensionRule.getRequestExecutor(),
      Collections.singletonList(null),
      endpoint
    );

    //then
    assertThat(response.getStatus(), is(200));
  }

  @ParameterizedTest
  @MethodSource("endpoints")
  public void outlierEndpoint_authorizedTenant(String endpoint) {
    // given
    final String tenantId = "tenantId";
    final String activityId = "chartTestActivity";
    engineIntegrationExtensionRule.createTenant(tenantId);
    ProcessDefinitionEngineDto processDefinition =
      engineIntegrationExtensionRule.deployProcessAndGetProcessDefinition(getBpmnModelInstance(activityId), tenantId);

    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.addGlobalAuthorizationForResource(RESOURCE_TYPE_PROCESS_DEFINITION);
    authorizationClient.grantSingleResourceAuthorizationsForUser(KERMIT_USER, tenantId, RESOURCE_TYPE_TENANT);

    startInstanceWithSampleVariables(processDefinition);

    embeddedOptimizeExtensionRule.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtensionRule.refreshAllOptimizeIndices();

    //when
    final Response response = executeRequest(
      processDefinition,
      embeddedOptimizeExtensionRule.getRequestExecutor().withUserAuthentication(KERMIT_USER, KERMIT_USER),
      Collections.singletonList(tenantId),
      endpoint
    );

    //then
    assertThat(response.getStatus(), is(200));
  }

  @ParameterizedTest
  @MethodSource("endpoints")
  public void outlierEndpoint_unauthorizedTenant(String endpoint) {
    // given
    final String tenantId = "tenantId";
    final String activityId = "chartTestActivity";
    engineIntegrationExtensionRule.createTenant(tenantId);
    ProcessDefinitionEngineDto processDefinition =
      engineIntegrationExtensionRule.deployProcessAndGetProcessDefinition(getBpmnModelInstance(activityId), tenantId);

    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.addGlobalAuthorizationForResource(RESOURCE_TYPE_PROCESS_DEFINITION);

    startInstanceWithSampleVariables(processDefinition);

    embeddedOptimizeExtensionRule.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtensionRule.refreshAllOptimizeIndices();

    //when
    final Response response = executeRequest(
      processDefinition,
      embeddedOptimizeExtensionRule.getRequestExecutor().withUserAuthentication(KERMIT_USER, KERMIT_USER),
      Collections.singletonList(tenantId),
      endpoint
    );

    //then
    assertThat(response.getStatus(), is(403));
  }

  @ParameterizedTest
  @MethodSource("endpoints")
  public void outlierEndpoint_partiallyUnauthorizedTenants(String endpoint) {
    // given
    final String tenantId1 = "tenantId1";
    engineIntegrationExtensionRule.createTenant(tenantId1);
    final String tenantId2 = "tenantId2";
    engineIntegrationExtensionRule.createTenant(tenantId2);
    final String activityId = "chartTestActivity";
    ProcessDefinitionEngineDto processDefinition1 =
      engineIntegrationExtensionRule.deployProcessAndGetProcessDefinition(getBpmnModelInstance(activityId), tenantId1);
    ProcessDefinitionEngineDto processDefinition2 =
      engineIntegrationExtensionRule.deployProcessAndGetProcessDefinition(getBpmnModelInstance(activityId), tenantId2);
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.addGlobalAuthorizationForResource(RESOURCE_TYPE_PROCESS_DEFINITION);
    authorizationClient.grantSingleResourceAuthorizationsForUser(KERMIT_USER, tenantId1, RESOURCE_TYPE_TENANT);

    startInstanceWithSampleVariables(processDefinition1);
    startInstanceWithSampleVariables(processDefinition2);

    embeddedOptimizeExtensionRule.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtensionRule.refreshAllOptimizeIndices();

    //when
    final Response response = executeRequest(
      processDefinition1,
      embeddedOptimizeExtensionRule.getRequestExecutor().withUserAuthentication(KERMIT_USER, KERMIT_USER),
      ImmutableList.of(tenantId1, tenantId2),
      endpoint
    );

    //then
    assertThat(response.getStatus(), is(403));
  }

  private Response executeRequest(final ProcessDefinitionEngineDto processDefinition,
                                  final OptimizeRequestExecutor optimizeRequestExecutor,
                                  final List<String> tenants,
                                  final String endpoint) {
    switch (endpoint) {
      case ENDPOINT_FLOW_NODE_OUTLIERS:
        return optimizeRequestExecutor
          .buildFlowNodeOutliersRequest(
            processDefinition.getKey(),
            Collections.singletonList("1"),
            tenants
          )
          .execute();
      case ENDPOINT_DURATION_CHART:
        return optimizeRequestExecutor
          .buildFlowNodeDurationChartRequest(
            processDefinition.getKey(),
            Collections.singletonList("1"),
            tenants,
            FLOW_NODE_ID_START
          )
          .execute();
      case ENDPOINT_SIGNIFICANT_OUTLIER_VARIABLE_TERMS:
        return optimizeRequestExecutor
          .buildSignificantOutlierVariableTermsRequest(
            processDefinition.getKey(),
            Collections.singletonList("1"),
            tenants,
            FLOW_NODE_ID_START,
            null,
            // -1 ensures we get results as
            -1L
          )
          .execute();
      case ENDPOINT_SIGNIFICANT_OUTLIER_VARIABLE_TERMS_PROCESS_INSTANCE_IDS_EXPORT:
        return optimizeRequestExecutor
          .buildSignificantOutlierVariableTermsInstanceIdsRequest(
            processDefinition.getKey(),
            Collections.singletonList("1"),
            tenants,
            FLOW_NODE_ID_START,
            0L,
            100L,
            "fake",
            "fake"
          )
          .execute();
      default:
        throw new OptimizeIntegrationTestException("Unsupported endpoint: " + endpoint);
    }
  }

  private void startInstanceWithSampleVariables(final ProcessDefinitionEngineDto processDefinition) {
    engineIntegrationExtensionRule.startProcessInstance(processDefinition.getId(), ImmutableMap.of("var", "value"));
  }

  private BpmnModelInstance getBpmnModelInstance(String... activityId) {
    StartEventBuilder builder = Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
      .name("aProcessName")
      .startEvent(FLOW_NODE_ID_START);
    for (String activity : activityId) {
      builder.serviceTask(activity)
        .camundaExpression("${true}");
    }
    return builder.endEvent("end").done();
  }

}
