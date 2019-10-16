/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.retrieval;

import org.camunda.optimize.dto.optimize.IdentityType;
import org.camunda.optimize.dto.optimize.ReportType;
import org.camunda.optimize.dto.optimize.RoleType;
import org.camunda.optimize.dto.optimize.query.IdDto;
import org.camunda.optimize.dto.optimize.query.collection.CollectionRoleDto;
import org.camunda.optimize.dto.optimize.query.collection.PartialCollectionDataDto;
import org.camunda.optimize.dto.optimize.query.collection.PartialCollectionDefinitionDto;
import org.camunda.optimize.dto.optimize.query.collection.ResolvedCollectionDataDto;
import org.camunda.optimize.dto.optimize.query.collection.ResolvedCollectionDefinitionDto;
import org.camunda.optimize.dto.optimize.query.dashboard.DashboardDefinitionDto;
import org.camunda.optimize.dto.optimize.query.entity.EntityDto;
import org.camunda.optimize.dto.optimize.query.entity.EntityType;
import org.camunda.optimize.dto.optimize.query.report.combined.CombinedReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.single.decision.SingleDecisionReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionDto;
import org.camunda.optimize.service.security.util.LocalDateUtil;
import org.camunda.optimize.test.it.extension.ElasticSearchIntegrationTestExtensionRule;
import org.camunda.optimize.test.it.extension.EmbeddedOptimizeExtensionRule;
import org.camunda.optimize.test.it.extension.EngineIntegrationExtensionRule;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.RequestOptions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import static org.camunda.optimize.service.es.writer.CollectionWriter.DEFAULT_COLLECTION_NAME;
import static org.camunda.optimize.test.it.extension.TestEmbeddedCamundaOptimize.DEFAULT_USERNAME;
import static org.camunda.optimize.test.util.ProcessReportDataBuilderHelper.createCombinedReport;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.COLLECTION_INDEX_NAME;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class CollectionHandlingIT {

  @RegisterExtension
  @Order(1)
  public ElasticSearchIntegrationTestExtensionRule elasticSearchIntegrationTestExtensionRule = new ElasticSearchIntegrationTestExtensionRule();
  @RegisterExtension
  @Order(2)
  public EngineIntegrationExtensionRule engineIntegrationExtensionRule = new EngineIntegrationExtensionRule();
  @RegisterExtension
  @Order(3)
  public EmbeddedOptimizeExtensionRule embeddedOptimizeExtensionRule = new EmbeddedOptimizeExtensionRule();

  @Test
  public void collectionIsWrittenToElasticsearch() throws IOException {
    // given
    String id = createNewCollection();

    // then
    GetRequest getRequest = new GetRequest(COLLECTION_INDEX_NAME, COLLECTION_INDEX_NAME, id);
    GetResponse getResponse = elasticSearchIntegrationTestExtensionRule.getOptimizeElasticClient().get(getRequest, RequestOptions.DEFAULT);

    // then
    assertThat(getResponse.isExists(), is(true));
  }

  @Test
  public void newCollectionIsCorrectlyInitialized() {
    // given
    String id = createNewCollection();

    // when
    ResolvedCollectionDefinitionDto collection = getCollectionById(id);

    // then
    assertThat(collection.getId(), is(id));
    assertThat(collection.getName(), is(DEFAULT_COLLECTION_NAME));
    assertThat(collection.getData().getEntities(), notNullValue());
    assertThat(collection.getData().getEntities().size(), is(0));
    assertThat(collection.getData().getConfiguration(), notNullValue());
    // author is automatically added as manager
    assertThat(collection.getData().getRoles(), notNullValue());
    assertThat(collection.getData().getRoles().size(), is(1));
    final CollectionRoleDto roleDto = collection.getData().getRoles().get(0);
    assertThat(roleDto.getId(), is(notNullValue()));
    assertThat(roleDto.getIdentity(), is(notNullValue()));
    assertThat(roleDto.getIdentity().getId(), is(DEFAULT_USERNAME));
    assertThat(roleDto.getIdentity().getType(), is(IdentityType.USER));
    assertThat(roleDto.getRole(), is(RoleType.MANAGER));
  }

  @Test
  public void getResolvedCollection() {
    //given
    final String collectionId = createNewCollection();
    final String dashboardId = createNewDashboardInCollection(collectionId);
    final String reportId = createNewSingleProcessReportInCollection(collectionId);

    elasticSearchIntegrationTestExtensionRule.refreshAllOptimizeIndices();

    // when
    ResolvedCollectionDefinitionDto collection = getCollectionById(collectionId);

    // then
    assertThat(collection, is(notNullValue()));
    assertThat(collection.getId(), is(collectionId));
    assertThat(collection.getData().getEntities().size(), is(2));
    assertThat(
      collection.getData().getEntities().stream().map(EntityDto::getId).collect(Collectors.toList()),
      containsInAnyOrder(dashboardId, reportId)
    );
  }

  @Test
  public void getResolvedCollectionContainsCombinedReportSubEntityCounts() {
    //given
    final String collectionId = createNewCollection();
    final String reportId1 = createNewSingleProcessReportInCollection(collectionId);
    final String reportId2 = createNewSingleProcessReportInCollection(collectionId);
    final String combinedReportId = createNewCombinedReportInCollection(collectionId);


    final CombinedReportDefinitionDto combinedReportUpdate = new CombinedReportDefinitionDto();
    combinedReportUpdate.setData(createCombinedReport(reportId1, reportId2));
    embeddedOptimizeExtensionRule
      .getRequestExecutor()
      .buildUpdateCombinedProcessReportRequest(combinedReportId, combinedReportUpdate)
      .execute();

    // when
    ResolvedCollectionDefinitionDto collection = getCollectionById(collectionId);

    // then
    assertThat(collection, is(notNullValue()));
    assertThat(collection.getId(), is(collectionId));
    assertThat(collection.getData().getEntities().size(), is(3));
    final EntityDto combinedReportEntityDto = collection.getData().getEntities().stream()
      .filter(EntityDto::getCombined)
      .findFirst()
      .get();
    assertThat(combinedReportEntityDto.getData().getSubEntityCounts().size(), is(1));
    assertThat(combinedReportEntityDto.getData().getSubEntityCounts().get(EntityType.REPORT), is(2L));
  }

  @Test
  public void updateCollection() {
    // given
    String id = createNewCollection();
    OffsetDateTime now = OffsetDateTime.parse("2019-04-23T18:00:00+01:00");
    LocalDateUtil.setCurrentTime(now);

    PartialCollectionDefinitionDto collectionUpdate = new PartialCollectionDefinitionDto();
    collectionUpdate.setName("MyCollection");
    final Map<String, String> configuration = Collections.singletonMap("Foo", "Bar");
    final PartialCollectionDataDto data = new PartialCollectionDataDto();
    data.setConfiguration(configuration);
    collectionUpdate.setData(data);


    // when
    updateCollectionRequest(id, collectionUpdate);
    ResolvedCollectionDefinitionDto collection = getCollectionById(id);

    // then
    assertThat(collection.getId(), is(id));
    assertThat(collection.getName(), is("MyCollection"));
    assertThat(collection.getLastModifier(), is("demo"));
    assertThat(collection.getLastModified(), is(now));
    final ResolvedCollectionDataDto resultCollectionData = collection.getData();
    assertThat(resultCollectionData.getConfiguration(), equalTo(configuration));
    assertThat(resultCollectionData.getEntities().size(), is(0));
  }

  @Test
  public void updatePartialCollection() {
    // given
    String id = createNewCollection();
    OffsetDateTime now = OffsetDateTime.parse("2019-04-23T18:00:00+01:00");
    LocalDateUtil.setCurrentTime(now);

    // when (update only name)
    PartialCollectionDefinitionDto collectionUpdate = new PartialCollectionDefinitionDto();
    collectionUpdate.setName("MyCollection");

    updateCollectionRequest(id, collectionUpdate);
    ResolvedCollectionDefinitionDto collection = getCollectionById(id);

    // then
    assertThat(collection.getId(), is(id));
    assertThat(collection.getName(), is("MyCollection"));
    assertThat(collection.getLastModifier(), is("demo"));
    assertThat(collection.getLastModified(), is(now));

    // when (update only configuration)
    collectionUpdate = new PartialCollectionDefinitionDto();
    final Map<String, String> configuration = Collections.singletonMap("Foo", "Bar");
    PartialCollectionDataDto data = new PartialCollectionDataDto();
    data.setConfiguration(configuration);
    collectionUpdate.setData(data);

    updateCollectionRequest(id, collectionUpdate);
    collection = getCollectionById(id);

    // then
    assertThat(collection.getId(), is(id));
    assertThat(collection.getName(), is("MyCollection"));
    assertThat(collection.getLastModifier(), is("demo"));
    assertThat(collection.getLastModified(), is(now));
    ResolvedCollectionDataDto resultCollectionData = collection.getData();
    assertThat(resultCollectionData.getConfiguration(), is(configuration));


    // when (again only update name)
    collectionUpdate = new PartialCollectionDefinitionDto();
    collectionUpdate.setName("TestNewCollection");

    updateCollectionRequest(id, collectionUpdate);
    collection = getCollectionById(id);

    // then
    assertThat(collection.getId(), is(id));
    assertThat(collection.getName(), is("TestNewCollection"));
    assertThat(collection.getLastModifier(), is("demo"));
    assertThat(collection.getLastModified(), is(now));
    resultCollectionData = collection.getData();
    assertThat(resultCollectionData.getConfiguration(), is(configuration));
  }

  @Test
  public void singleProcessReportCanBeCreatedInsideCollection() {
    // given
    String collectionId = createNewCollection();
    String reportId = createNewSingleProcessReportInCollection(collectionId);

    // when
    ResolvedCollectionDefinitionDto collection = getCollectionById(collectionId);

    // then
    EntityDto report = collection.getData().getEntities().get(0);
    assertThat(report.getId(), is(reportId));
    assertThat(report.getEntityType(), is(EntityType.REPORT));
    assertThat(report.getReportType(), is(ReportType.PROCESS));
    assertThat(report.getCombined(), is(false));
  }

  @Test
  public void singleDecisionReportCanBeCreatedInsideCollection() {
    // given
    String collectionId = createNewCollection();
    String reportId = createNewSingleDecisionReportInCollection(collectionId);

    // when
    ResolvedCollectionDefinitionDto collection = getCollectionById(collectionId);

    // then
    EntityDto report = collection.getData().getEntities().get(0);
    assertThat(report.getId(), is(reportId));
    assertThat(report.getEntityType(), is(EntityType.REPORT));
    assertThat(report.getReportType(), is(ReportType.DECISION));
    assertThat(report.getCombined(), is(false));
  }

  @Test
  public void combinedProcessReportCanBeCreatedInsideCollection() {
    // given
    String collectionId = createNewCollection();
    String reportId = createNewCombinedReportInCollection(collectionId);

    // when
    ResolvedCollectionDefinitionDto collection = getCollectionById(collectionId);

    // then
    EntityDto report = collection.getData().getEntities().get(0);
    assertThat(report.getId(), is(reportId));
    assertThat(report.getEntityType(), is(EntityType.REPORT));
    assertThat(report.getReportType(), is(ReportType.PROCESS));
    assertThat(report.getCombined(), is(true));
  }

  @Test
  public void dashboardCanBeCreatedInsideCollection() {
    // given
    String collectionId = createNewCollection();
    String dashboardId = createNewDashboardInCollection(collectionId);

    // when
    ResolvedCollectionDefinitionDto collection = getCollectionById(collectionId);

    // then
    EntityDto dashboard = collection.getData().getEntities().get(0);
    assertThat(dashboard.getId(), is(dashboardId));
    assertThat(dashboard.getEntityType(), is(EntityType.DASHBOARD));
    assertThat(dashboard.getReportType(), is(nullValue()));
    assertThat(dashboard.getCombined(), is(nullValue()));
  }

  @Test
  public void singleProcessReportCanNotBeCreatedForInvalidCollection() {
    // given
    SingleProcessReportDefinitionDto singleProcessReportDefinitionDto = new SingleProcessReportDefinitionDto();
    singleProcessReportDefinitionDto.setCollectionId("invalidId");

    // when
    final Response createResponse = embeddedOptimizeExtensionRule
      .getRequestExecutor()
      .buildCreateSingleProcessReportRequest(singleProcessReportDefinitionDto)
      .execute();

    // then
    assertThat(createResponse.getStatus(), is(HttpServletResponse.SC_NOT_FOUND));
  }

  @Test
  public void singleDecisionReportCanNotBeCreatedForInvalidCollection() {
    // given
    SingleDecisionReportDefinitionDto singleDecisionReportDefinitionDto = new SingleDecisionReportDefinitionDto();
    singleDecisionReportDefinitionDto.setCollectionId("invalidId");

    // when
    final Response createResponse = embeddedOptimizeExtensionRule
      .getRequestExecutor()
      .buildCreateSingleDecisionReportRequest(singleDecisionReportDefinitionDto)
      .execute();

    // then
    assertThat(createResponse.getStatus(), is(HttpServletResponse.SC_NOT_FOUND));
  }

  @Test
  public void combinedProcessReportCanNotBeCreatedForInvalidCollection() {
    // given
    CombinedReportDefinitionDto combinedReportDefinitionDto = new CombinedReportDefinitionDto();
    combinedReportDefinitionDto.setCollectionId("invalidId");

    // when
    final Response createResponse = embeddedOptimizeExtensionRule
      .getRequestExecutor()
      .buildCreateCombinedReportRequest(combinedReportDefinitionDto)
      .execute();

    // then
    assertThat(createResponse.getStatus(), is(HttpServletResponse.SC_NOT_FOUND));
  }

  @Test
  public void dashboardCanNotBeCreatedForInvalidCollection() {
    // given
    DashboardDefinitionDto dashboardDefinitionDto = new DashboardDefinitionDto();
    dashboardDefinitionDto.setCollectionId("invalidId");

    // when
    final Response createResponse = embeddedOptimizeExtensionRule
      .getRequestExecutor()
      .buildCreateDashboardRequest(dashboardDefinitionDto)
      .execute();

    // then
    assertThat(createResponse.getStatus(), is(HttpServletResponse.SC_NOT_FOUND));
  }

  @Test
  public void collectionItemsAreOrderedByTypeAndModificationDateDescending() {
    // given
    String collectionId = createNewCollection();
    String reportId1 = createNewSingleProcessReportInCollection(collectionId);
    String reportId2 = createNewSingleProcessReportInCollection(collectionId);
    String dashboardId1 = createNewDashboardInCollection(collectionId);
    String dashboardId2 = createNewDashboardInCollection(collectionId);

    updateReport(reportId1, new SingleProcessReportDefinitionDto());

    elasticSearchIntegrationTestExtensionRule.refreshAllOptimizeIndices();

    // when
    ResolvedCollectionDefinitionDto collection = getCollectionById(collectionId);

    // then
    assertThat(collection.getData().getEntities().get(0).getId(), is(dashboardId2));
    assertThat(collection.getData().getEntities().get(1).getId(), is(dashboardId1));
    assertThat(collection.getData().getEntities().get(2).getId(), is(reportId1));
    assertThat(collection.getData().getEntities().get(3).getId(), is(reportId2));
  }

  @Test
  public void doNotUpdateNullFieldsInCollection() {
    // given
    String id = createNewCollection();
    PartialCollectionDefinitionDto collection = new PartialCollectionDefinitionDto();

    // when
    updateCollectionRequest(id, collection);
    ResolvedCollectionDefinitionDto storedCollection = getCollectionById(id);

    // then
    assertThat(storedCollection.getId(), is(id));
    assertThat(storedCollection.getCreated(), is(notNullValue()));
    assertThat(storedCollection.getLastModified(), is(notNullValue()));
    assertThat(storedCollection.getLastModifier(), is(notNullValue()));
    assertThat(storedCollection.getName(), is(notNullValue()));
    assertThat(storedCollection.getOwner(), is(notNullValue()));
  }

  @Test
  public void deletedReportsAreRemovedFromCollectionWhenForced() {
    // given
    String collectionId = createNewCollection();
    String singleReportIdToDelete = createNewSingleProcessReportInCollection(collectionId);
    String combinedReportIdToDelete = createNewCombinedReportInCollection(collectionId);

    elasticSearchIntegrationTestExtensionRule.refreshAllOptimizeIndices();

    // when
    deleteReport(singleReportIdToDelete);
    deleteReport(combinedReportIdToDelete);

    // then
    ResolvedCollectionDefinitionDto collection = getCollectionById(collectionId);
    assertThat(collection.getData().getEntities().size(), is(0));
  }

  @Test
  public void deletedDashboardsAreRemovedFromCollectionWhenForced() {
    // given
    String collectionId = createNewCollection();
    String dashboardIdToDelete = createNewDashboardInCollection(collectionId);

    elasticSearchIntegrationTestExtensionRule.refreshAllOptimizeIndices();

    // when
    deleteDashboard(dashboardIdToDelete);

    // then
    ResolvedCollectionDefinitionDto collection = getCollectionById(collectionId);
    assertThat(collection.getData().getEntities().size(), is(0));
  }

  @Test
  public void entitiesAreDeletedOnCollectionDelete() {
    // given
    String collectionId = createNewCollection();
    String singleReportId = createNewSingleProcessReportInCollection(collectionId);
    String combinedReportId = createNewCombinedReportInCollection(collectionId);
    String dashboardId = createNewDashboardInCollection(collectionId);

    elasticSearchIntegrationTestExtensionRule.refreshAllOptimizeIndices();

    // when
    deleteCollection(collectionId);

    // then
    final Response getCollectionByIdResponse = embeddedOptimizeExtensionRule
      .getRequestExecutor()
      .buildGetCollectionRequest(collectionId)
      .execute();
    assertThat(getCollectionByIdResponse.getStatus(), is(404));

    assertDashboardIsDeleted(dashboardId);
    assertReportIsDeleted(singleReportId);
    assertReportIsDeleted(combinedReportId);
  }

  private void assertReportIsDeleted(final String singleReportIdToDelete) {
    final Response response = embeddedOptimizeExtensionRule.getRequestExecutor()
      .buildGetReportRequest(singleReportIdToDelete)
      .execute();
    assertThat(response.getStatus(), is(404));
  }

  private void assertDashboardIsDeleted(final String dashboardIdToDelete) {
    final Response response = embeddedOptimizeExtensionRule.getRequestExecutor()
      .buildGetDashboardRequest(dashboardIdToDelete)
      .execute();
    assertThat(response.getStatus(), is(404));
  }

  private String createNewSingleProcessReportInCollection(final String collectionId) {
    SingleProcessReportDefinitionDto singleProcessReportDefinitionDto = new SingleProcessReportDefinitionDto();
    singleProcessReportDefinitionDto.setCollectionId(collectionId);
    return embeddedOptimizeExtensionRule
      .getRequestExecutor()
      .buildCreateSingleProcessReportRequest(singleProcessReportDefinitionDto)
      .execute(IdDto.class, 200)
      .getId();
  }

  private String createNewSingleDecisionReportInCollection(final String collectionId) {
    SingleDecisionReportDefinitionDto singleDecisionReportDefinitionDto = new SingleDecisionReportDefinitionDto();
    singleDecisionReportDefinitionDto.setCollectionId(collectionId);
    return embeddedOptimizeExtensionRule
      .getRequestExecutor()
      .buildCreateSingleDecisionReportRequest(singleDecisionReportDefinitionDto)
      .execute(IdDto.class, 200)
      .getId();
  }

  private String createNewDashboardInCollection(final String collectionId) {
    DashboardDefinitionDto dashboardDefinitionDto = new DashboardDefinitionDto();
    dashboardDefinitionDto.setCollectionId(collectionId);
    return embeddedOptimizeExtensionRule
      .getRequestExecutor()
      .buildCreateDashboardRequest(dashboardDefinitionDto)
      .execute(IdDto.class, 200)
      .getId();
  }

  private String createNewCombinedReportInCollection(final String collectionId) {
    CombinedReportDefinitionDto combinedReportDefinitionDto = new CombinedReportDefinitionDto();
    combinedReportDefinitionDto.setCollectionId(collectionId);
    return embeddedOptimizeExtensionRule
      .getRequestExecutor()
      .buildCreateCombinedReportRequest(combinedReportDefinitionDto)
      .execute(IdDto.class, 200)
      .getId();
  }

  private Response deleteCollection(String id) {
    return embeddedOptimizeExtensionRule.getRequestExecutor()
      .buildDeleteCollectionRequest(id, true)
      .execute();
  }

  private void deleteReport(String reportId) {
    Response response = embeddedOptimizeExtensionRule
      .getRequestExecutor()
      .buildDeleteReportRequest(reportId, true)
      .execute();

    // then the status code is okay
    assertThat(response.getStatus(), is(204));
  }

  private void deleteDashboard(String dashboardId) {
    Response response = embeddedOptimizeExtensionRule
      .getRequestExecutor()
      .buildDeleteDashboardRequest(dashboardId, true)
      .execute();

    // then the status code is okay
    assertThat(response.getStatus(), is(204));
  }

  private String createNewCollection() {
    return embeddedOptimizeExtensionRule
      .getRequestExecutor()
      .buildCreateCollectionRequest()
      .execute(IdDto.class, 200)
      .getId();
  }

  private void updateCollectionRequest(String id, PartialCollectionDefinitionDto renameCollection) {
    Response response = embeddedOptimizeExtensionRule
      .getRequestExecutor()
      .buildUpdatePartialCollectionRequest(id, renameCollection)
      .execute();
    assertThat(response.getStatus(), is(204));
  }

  private void updateReport(String id, SingleProcessReportDefinitionDto updatedReport) {
    Response response = getUpdateReportResponse(id, updatedReport);
    assertThat(response.getStatus(), is(204));
  }

  private Response getUpdateReportResponse(String id, SingleProcessReportDefinitionDto updatedReport) {
    return embeddedOptimizeExtensionRule
      .getRequestExecutor()
      .buildUpdateSingleProcessReportRequest(id, updatedReport)
      .execute();
  }

  private ResolvedCollectionDefinitionDto getCollectionById(final String collectionId) {
    return embeddedOptimizeExtensionRule
      .getRequestExecutor()
      .buildGetCollectionRequest(collectionId)
      .execute(ResolvedCollectionDefinitionDto.class, 200);
  }

}
