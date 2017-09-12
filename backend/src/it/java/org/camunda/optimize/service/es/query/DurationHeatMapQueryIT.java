package org.camunda.optimize.service.es.query;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.optimize.dto.engine.HistoricProcessInstanceDto;
import org.camunda.optimize.dto.optimize.query.HeatMapQueryDto;
import org.camunda.optimize.dto.optimize.query.HeatMapResponseDto;
import org.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import org.camunda.optimize.test.it.rule.ElasticSearchIntegrationTestRule;
import org.camunda.optimize.test.it.rule.EmbeddedOptimizeRule;
import org.camunda.optimize.test.it.rule.EngineIntegrationRule;
import org.camunda.optimize.test.util.DataUtilHelper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.camunda.optimize.service.es.filter.FilterOperatorConstants.GREATER_THAN;
import static org.camunda.optimize.service.es.filter.FilterOperatorConstants.GREATER_THAN_EQUALS;
import static org.camunda.optimize.service.es.filter.FilterOperatorConstants.LESS_THAN;
import static org.camunda.optimize.service.es.filter.FilterOperatorConstants.LESS_THAN_EQUALS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Tests the duration heat map query.
 *
 * NOTE: In order to run this class, you need the
 * HistoricActivityInstanceAdaptionPlugin within the engine. Otherwise,
 * all tests will fail.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/rest/restTestApplicationContext.xml"})
public class DurationHeatMapQueryIT {

  private Logger logger = LoggerFactory.getLogger(DurationHeatMapQueryIT.class);
  private static final String SERVICE_TASK_ID = "aSimpleServiceTask";
  private static final String SERVICE_TASK_ID_2 = "aSimpleServiceTask2";

  public EngineIntegrationRule engineRule = new EngineIntegrationRule();
  public ElasticSearchIntegrationTestRule elasticSearchRule = new ElasticSearchIntegrationTestRule();
  public EmbeddedOptimizeRule embeddedOptimizeRule = new EmbeddedOptimizeRule();

  @Rule
  public RuleChain chain = RuleChain
      .outerRule(elasticSearchRule).around(engineRule).around(embeddedOptimizeRule);

  @Test
  public void getHeatMapWithImport() throws Exception {

    // given
    String processDefinitionId = deploySimpleServiceTaskProcessDefinition();
    Map<String, Object> variables = new HashMap<>();
    variables.put("activityDuration", 20L);
    engineRule.startProcessInstance(processDefinitionId, variables);
    engineRule.waitForAllProcessesToFinish();
    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    HeatMapResponseDto testDefinition = getHeatMapResponseDto(processDefinitionId);

    // then
    assertResults(testDefinition, 3, SERVICE_TASK_ID, 20L, 1L);
  }

  @Test
  public void getHeatMap() throws Exception {
    // given
    String processDefinitionId = deploySimpleServiceTaskProcessDefinition();
    Map<String, Object> variables = new HashMap<>();
    variables.put("activityDuration", 10L);
    engineRule.startProcessInstance(processDefinitionId, variables);
    variables.put("activityDuration", 30L);
    engineRule.startProcessInstance(processDefinitionId, variables);
    engineRule.waitForAllProcessesToFinish();
    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    HeatMapResponseDto testDefinition = getHeatMapResponseDto(processDefinitionId);

    // then
    assertResults(testDefinition, 3, SERVICE_TASK_ID, 20L, 2L);
  }

  @Test
  public void getHeatMapForMultipleActivities() throws Exception {
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("aProcess" + System.currentTimeMillis())
      .startEvent()
      .serviceTask(SERVICE_TASK_ID)
        .camundaExpression("${true}")
      .serviceTask(SERVICE_TASK_ID_2)
        .camundaExpression("${true}")
      .endEvent()
      .done();
    String processDefinitionId = engineRule.deployProcessAndGetId(modelInstance);

    Map<String, Object> variables = new HashMap<>();
    variables.put("activityDuration", 10L);
    variables.put("activityDuration2", 20L);
    engineRule.startProcessInstance(processDefinitionId, variables);
    engineRule.startProcessInstance(processDefinitionId, variables);
    engineRule.waitForAllProcessesToFinish();
    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    HeatMapResponseDto testDefinition = getHeatMapResponseDto(processDefinitionId);

    // then
    assertResults(testDefinition, 4, SERVICE_TASK_ID, 10L, 2L);
    assertResults(testDefinition, 4, SERVICE_TASK_ID_2, 20L, 2L);
  }

  @Test
  public void getHeatMapForMultipleProcessInstances() throws Exception {
    // given
    String processDefinitionId = deploySimpleServiceTaskProcessDefinition();
    Map<String, Object> variables = new HashMap<>();
    variables.put("activityDuration", 40L);
    engineRule.startProcessInstance(processDefinitionId, variables);
    engineRule.startProcessInstance(processDefinitionId, variables);
    variables.put("activityDuration", 20L);
    engineRule.startProcessInstance(processDefinitionId, variables);
    engineRule.startProcessInstance(processDefinitionId, variables);
    engineRule.waitForAllProcessesToFinish();
    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    HeatMapResponseDto testDefinition = getHeatMapResponseDto(processDefinitionId);

    // then
    assertResults(testDefinition, 3, SERVICE_TASK_ID, 30L, 4L);
  }

  @Test
  public void getHeatMapForMultipleProcessDefinitions() throws Exception {
    // given
    String processDefinitionId = deploySimpleServiceTaskProcessDefinition();
    String processDefinitionId2 = deploySimpleServiceTaskProcessDefinition();
    Map<String, Object> variables = new HashMap<>();
    variables.put("activityDuration", 40L);
    engineRule.startProcessInstance(processDefinitionId, variables);
    engineRule.startProcessInstance(processDefinitionId, variables);
    variables.put("activityDuration", 20L);
    engineRule.startProcessInstance(processDefinitionId2, variables);
    engineRule.startProcessInstance(processDefinitionId2, variables);
    engineRule.waitForAllProcessesToFinish();
    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    HeatMapResponseDto testDefinition1 = getHeatMapResponseDto(processDefinitionId);
    HeatMapResponseDto testDefinition2 = getHeatMapResponseDto(processDefinitionId2);

    // then
    assertResults(testDefinition1, 3, SERVICE_TASK_ID, 40L, 2L);
    assertResults(testDefinition2, 3, SERVICE_TASK_ID, 20L, 2L);
  }

  @Test
  public void getHeatMapCanHandleIrrationalAverageNumber() throws Exception {
    // given
    String processDefinitionId = deploySimpleServiceTaskProcessDefinition();
    Map<String, Object> variables = new HashMap<>();
    variables.put("activityDuration", 100L);
    engineRule.startProcessInstance(processDefinitionId, variables);
    variables.put("activityDuration", 300L);
    engineRule.startProcessInstance(processDefinitionId, variables);
    variables.put("activityDuration", 600L);
    engineRule.startProcessInstance(processDefinitionId, variables);
    engineRule.waitForAllProcessesToFinish();
    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    HeatMapResponseDto testDefinition = getHeatMapResponseDto(processDefinitionId);

    // then
    assertResults(testDefinition, 3, SERVICE_TASK_ID, 333L, 3L);
}

  @Test
  public void noEventMatchesReturnEmptyResult() {
    // when
    HeatMapResponseDto testDefinition = getHeatMapResponseDto("nonExistingProcessDefinitionId");

    // then
    assertThat(testDefinition.getPiCount(), is(0L));
    assertThat(testDefinition.getFlowNodes().size(), is(0));
  }

  @Test
  public void getHeatMapWithMIBody() throws Exception {
    // given
    BpmnModelInstance subProcess = Bpmn.createExecutableProcess("subProcess")
        .startEvent()
          .serviceTask(SERVICE_TASK_ID)
            .camundaExpression("${true}")
        .endEvent()
        .done();

    BpmnModelInstance miProcess = Bpmn.createExecutableProcess("miProcess")
        .name("MultiInstance")
          .startEvent("miStart")
          .callActivity("callActivity")
            .calledElement("subProcess")
            .camundaIn("activityDuration", "activityDuration")
            .multiInstance()
              .cardinality("2")
            .multiInstanceDone()
          .endEvent("miEnd")
        .done();
    String subProcessDefinitionId = engineRule.deployProcessAndGetId(subProcess);
    String processDefinitionId = engineRule.deployProcessAndGetId(miProcess);
    Map<String, Object> variables = new HashMap<>();
    variables.put("activityDuration", 10L);
    engineRule.startProcessInstance(processDefinitionId, variables);
    engineRule.waitForAllProcessesToFinish();
    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    HeatMapResponseDto subProcessResultMap = getHeatMapResponseDto(subProcessDefinitionId);

    // then
    assertResults(subProcessResultMap, 3, SERVICE_TASK_ID, 10L, 2L);
  }

  @Test
  public void getHeatMapWithMoreThenTenEvents() throws Exception {
    // given
    String processDefinitionId = deploySimpleServiceTaskProcessDefinition();
    Map<String, Object> variables = new HashMap<>();
    variables.put("activityDuration", 10L);

    for (int i = 0; i < 11; i++) {
      engineRule.startProcessInstance(processDefinitionId, variables);
    }
    engineRule.waitForAllProcessesToFinish();
    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    HeatMapResponseDto testDefinition = getHeatMapResponseDto(processDefinitionId);

    // then
    assertResults(testDefinition, 3, SERVICE_TASK_ID, 10L, 11L);
  }

  @Test
  public void testGetHeatMapWithLtStartDateCriteria() throws Exception {
    //given
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess(10L);
    Date past = engineRule.getHistoricProcessInstance(processInstanceDto.getId()).getStartTime();
    String processDefinitionId = processInstanceDto.getDefinitionId();
    engineRule.waitForAllProcessesToFinish();
    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    String operator = GREATER_THAN;
    String type = "start_date";

    //when
    HeatMapQueryDto dto = getHeatMapQueryDtoInFuture(past, processDefinitionId, operator, type);
    HeatMapResponseDto resultMap = getHeatMapResponseDto(dto);

    //then
    assertResults(resultMap, 0,0L);

    //when
    dto = createHeatMapQueryWithDateFilter(processDefinitionId, operator, type, past);
    resultMap = getHeatMapResponseDto(dto);
    //then
    assertResults(resultMap, 0,0L);

    //when
    dto = getHeatMapQueryDtoInPast(past, processDefinitionId, operator, type);
    resultMap = getHeatMapResponseDto(dto);
    //then
    assertResults(resultMap, 3, SERVICE_TASK_ID, 10L, 1L);
  }

  @Test
  public void testGetHeatMapWithLteStartDateCriteria() throws Exception {
    //given
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess(10L);
    Date past = engineRule.getHistoricProcessInstance(processInstanceDto.getId()).getStartTime();
    String processDefinitionId = processInstanceDto.getDefinitionId();
    engineRule.waitForAllProcessesToFinish();
    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    String operator = GREATER_THAN_EQUALS;
    String type = "start_date";

    //when
    HeatMapQueryDto dto = getHeatMapQueryDtoInFuture(past, processDefinitionId, operator, type);
    HeatMapResponseDto resultMap = getHeatMapResponseDto(dto);

    //then
    assertResults(resultMap, 0, 0L);

    //when
    dto = createHeatMapQueryWithDateFilter(processDefinitionId, operator, type, past);
    resultMap = getHeatMapResponseDto(dto);
    //then
    assertResults(resultMap, 3, 1L);

    //when
    dto = getHeatMapQueryDtoInPast(past, processDefinitionId, operator, type);
    resultMap = getHeatMapResponseDto(dto);
    //then
    assertResults(resultMap, 3, SERVICE_TASK_ID, 10L, 1L);
  }

  @Test
  public void testGetHeatMapWithLteEndDateCriteria() throws Exception {
    //given
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess(10L);
    Date past = engineRule.getHistoricProcessInstance(processInstanceDto.getId()).getEndTime();
    String processDefinitionId = processInstanceDto.getDefinitionId();
    engineRule.waitForAllProcessesToFinish();
    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    String operator = GREATER_THAN_EQUALS;
    String type = "end_date";

    //when
    HeatMapQueryDto dto = getHeatMapQueryDtoInFuture(past, processDefinitionId, operator, type);
    HeatMapResponseDto resultMap = getHeatMapResponseDto(dto);

    //then
    assertResults(resultMap, 0, 0L);

    //when
    dto = createHeatMapQueryWithDateFilter(processDefinitionId, operator, type, past);
    resultMap = getHeatMapResponseDto(dto);
    //then
    assertResults(resultMap, 3, SERVICE_TASK_ID, 10L, 1L);

    //when
    dto = getHeatMapQueryDtoInPast(past, processDefinitionId, operator, type);
    resultMap = getHeatMapResponseDto(dto);
    //then
    assertResults(resultMap, 3, SERVICE_TASK_ID, 10L, 1L);
  }

  @Test
  public void testGetHeatMapWithGtStartDateCriteria() throws Exception {
    //given
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess(10L);
    Date past = engineRule.getHistoricProcessInstance(processInstanceDto.getId()).getStartTime();
    String processDefinitionId = processInstanceDto.getDefinitionId();
    engineRule.waitForAllProcessesToFinish();
    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    String operator = LESS_THAN;
    String type = "start_date";

    //when
    HeatMapQueryDto dto = getHeatMapQueryDtoInFuture(past, processDefinitionId, operator, type);
    HeatMapResponseDto resultMap = getHeatMapResponseDto(dto);

    //then
    assertResults(resultMap, 3, SERVICE_TASK_ID, 10L, 1L);

    //when
    dto = createHeatMapQueryWithDateFilter(processDefinitionId, operator, type, past);
    resultMap = getHeatMapResponseDto(dto);
    //then
    assertResults(resultMap, 0, 0L);

    //when
    dto = getHeatMapQueryDtoInPast(past, processDefinitionId, operator, type);
    resultMap = getHeatMapResponseDto(dto);
    //then
    assertResults(resultMap, 0, 0L);
  }

  @Test
  public void testGetHeatMapWithMixedDateCriteria() throws Exception {
    //given
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess(10L);
    HistoricProcessInstanceDto historicProcessInstance =
      engineRule.getHistoricProcessInstance(processInstanceDto.getId());
    Date past = historicProcessInstance.getStartTime();
    String processDefinitionId = processInstanceDto.getDefinitionId();
    engineRule.waitForAllProcessesToFinish();
    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    String operator = GREATER_THAN;
    String type = "start_date";

    HeatMapQueryDto dto = createHeatMapQueryWithDateFilter(processDefinitionId, operator, type, new Date(past.getTime() - 1000));
    operator = LESS_THAN;
    type = "end_date";
    past = historicProcessInstance.getEndTime();
    DataUtilHelper.addDateFilter(operator, type, new Date(past.getTime() + 2000), dto);

    //when
    HeatMapResponseDto resultMap = getHeatMapResponseDto(dto);

    //then
    assertResults(resultMap, 3, 1L);

    //given
    operator = GREATER_THAN;
    type = "start_date";
    past = historicProcessInstance.getStartTime();
    dto = getHeatMapQueryDtoInPast(past, processDefinitionId, operator, type);
    operator = LESS_THAN;
    type = "end_date";
    past = historicProcessInstance.getEndTime();
    DataUtilHelper.addDateFilter(operator, type, new Date(past.getTime()), dto);

    //when
    resultMap = getHeatMapResponseDto(dto);

    //then
    assertResults(resultMap, 0, 0L);
  }

  @Test
  public void testGetHeatMapWithGtEndDateCriteria() throws Exception {
    //given
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess(20L);
    Date past = engineRule.getHistoricProcessInstance(processInstanceDto.getId()).getEndTime();
    String processDefinitionId = processInstanceDto.getDefinitionId();
    engineRule.waitForAllProcessesToFinish();
    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    String operator = LESS_THAN;
    String type = "end_date";

    //when
    HeatMapQueryDto dto = getHeatMapQueryDtoInFuture(past, processDefinitionId, operator, type);
    HeatMapResponseDto resultMap = getHeatMapResponseDto(dto);

    //then
    assertResults(resultMap, 3, SERVICE_TASK_ID, 20L, 1L);

    //when
    dto = createHeatMapQueryWithDateFilter(processDefinitionId, operator, type, past);
    resultMap = getHeatMapResponseDto(dto);
    //then
    assertResults(resultMap, 0, 0L);

    //when
    dto = getHeatMapQueryDtoInPast(past, processDefinitionId, operator, type);
    resultMap = getHeatMapResponseDto(dto);
    //then
    assertResults(resultMap, 0, 0L);
  }

  @Test
  public void testGetHeatMapWithGteStartDateCriteria() throws Exception {
    //given
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess(10L);
    Date past = engineRule.getHistoricProcessInstance(processInstanceDto.getId()).getStartTime();
    String processDefinitionId = processInstanceDto.getDefinitionId();
    engineRule.waitForAllProcessesToFinish();
    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    String operator = LESS_THAN_EQUALS;
    String type = "start_date";

    //when
    HeatMapQueryDto dto = getHeatMapQueryDtoInFuture(past, processDefinitionId, operator, type);
    HeatMapResponseDto resultMap = getHeatMapResponseDto(dto);

    //then
    assertResults(resultMap, 3, SERVICE_TASK_ID, 10L, 1L);

    //when
    dto = createHeatMapQueryWithDateFilter(processDefinitionId, operator, type, past);
    resultMap = getHeatMapResponseDto(dto);
    //then
    assertResults(resultMap, 3, SERVICE_TASK_ID, 10L, 1L);

    //when
    dto = getHeatMapQueryDtoInPast(past, processDefinitionId, operator, type);
    resultMap = getHeatMapResponseDto(dto);
    //then
    assertResults(resultMap, 0, 0L);
  }

  private HeatMapQueryDto getHeatMapQueryDtoInPast(Date past, String processDefinitionId, String operator, String type) {
    HeatMapQueryDto dto;
    dto = createHeatMapQueryWithDateFilter(
        processDefinitionId,
        operator,
        type,
        new Date(past.getTime() - 5000)
    );
    return dto;
  }

  private HeatMapQueryDto getHeatMapQueryDtoInFuture(Date past, String processDefinitionId, String operator, String type) {
    return createHeatMapQueryWithDateFilter(
        processDefinitionId,
        operator,
        type,
        new Date(past.getTime() + 5000)
    );
  }

  private ProcessInstanceEngineDto deployAndStartSimpleServiceTaskProcess(long timeToWait) {
    BpmnModelInstance processModel = Bpmn.createExecutableProcess("aProcess" + System.currentTimeMillis())
      .name("aProcessName")
      .startEvent()
      .serviceTask(SERVICE_TASK_ID)
        .camundaExpression("${true}")
      .endEvent()
      .done();
    Map<String, Object> variables = new HashMap<>();
    variables.put("activityDuration", timeToWait);
    ProcessInstanceEngineDto processInstanceEngineDto = engineRule.deployAndStartProcessWithVariables(processModel, variables);
    return processInstanceEngineDto;
  }

  private String deploySimpleServiceTaskProcessDefinition() throws IOException {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("aProcess" + System.currentTimeMillis())
      .startEvent()
      .serviceTask(SERVICE_TASK_ID)
        .camundaExpression("${true}")
      .endEvent()
      .done();
    String processDefinitionId = engineRule.deployProcessAndGetId(modelInstance);
    return processDefinitionId;
  }

  private void assertResults(HeatMapResponseDto resultMap, int activityCount, long piCount) {
    assertThat(resultMap.getFlowNodes().size(), is(activityCount));
    assertThat(resultMap.getPiCount(), is(piCount));
  }

  public void assertResults(HeatMapResponseDto resultMap, int activityCount, String activity, Long averageDuration, Long piCount) {
    this.assertResults(resultMap, activityCount, piCount);
    assertThat(resultMap.getFlowNodes().get(activity), is(averageDuration));
  }

  private HeatMapQueryDto createHeatMapQueryWithDateFilter(String processDefinitionId, String operator, String type, Date dateValue) {
    logger.debug("Preparing query on [{}] with operator [{}], type [{}], date [{}]", processDefinitionId, operator, type, dateValue);
    HeatMapQueryDto dto = new HeatMapQueryDto();
    dto.setProcessDefinitionId(processDefinitionId);
    DataUtilHelper.addDateFilter(operator, type, dateValue, dto);
    return dto;
  }

  private HeatMapResponseDto getHeatMapResponseDto(String testDefinition) {
    String token = embeddedOptimizeRule.getAuthenticationToken();
    Response response =
        embeddedOptimizeRule.target("process-definition/" + testDefinition + "/heatmap/duration")
            .request()
            .header(HttpHeaders.AUTHORIZATION,"Bearer " + token)
            .get();
    return response.readEntity(HeatMapResponseDto.class);
  }

  private HeatMapResponseDto getHeatMapResponseDto(HeatMapQueryDto dto) {
    String token = embeddedOptimizeRule.getAuthenticationToken();
    Response response = getResponse(token, dto);

    // then the status code is okay
    return response.readEntity(HeatMapResponseDto.class);
  }

  private Response getResponse(String token, HeatMapQueryDto dto) {
    Entity<HeatMapQueryDto> entity = Entity.entity(dto, MediaType.APPLICATION_JSON);
    return embeddedOptimizeRule.target("process-definition/heatmap/duration")
        .request()
        .header(HttpHeaders.AUTHORIZATION,"Bearer " + token)
        .post(entity);
  }
  
}
