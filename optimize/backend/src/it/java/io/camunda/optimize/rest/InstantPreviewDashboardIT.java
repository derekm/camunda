/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.dto.optimize.ReportConstants.ALL_VERSIONS;
import static io.camunda.optimize.dto.optimize.query.dashboard.InstantDashboardDataDto.INSTANT_DASHBOARD_DEFAULT_TEMPLATE;
import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_USERNAME;
import static io.camunda.optimize.service.dashboard.InstantPreviewDashboardService.INSTANT_PREVIEW_DASHBOARD_TEMPLATES_PATH;
import static io.camunda.optimize.service.dashboard.InstantPreviewDashboardService.TYPE_IMAGE_VALUE;
import static io.camunda.optimize.service.dashboard.InstantPreviewDashboardService.getChecksumCRC32;
import static io.camunda.optimize.service.util.importing.EngineConstants.RESOURCE_TYPE_PROCESS_DEFINITION;
import static io.camunda.optimize.service.util.importing.EngineConstants.RESOURCE_TYPE_TENANT;
import static io.camunda.optimize.test.engine.AuthorizationClient.KERMIT_USER;
import static io.camunda.optimize.util.BpmnModels.getSimpleBpmnDiagram;
import static io.camunda.optimize.util.BpmnModels.getSingleUserTaskDiagram;
import static io.camunda.optimize.util.SuppressionConstants.UNUSED;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.dto.optimize.RoleType;
import io.camunda.optimize.dto.optimize.query.dashboard.DashboardDefinitionRestDto;
import io.camunda.optimize.dto.optimize.query.dashboard.InstantDashboardDataDto;
import io.camunda.optimize.dto.optimize.query.dashboard.tile.DashboardTileType;
import io.camunda.optimize.dto.optimize.query.report.ReportDefinitionDto;
import io.camunda.optimize.dto.optimize.query.report.single.ReportDataDefinitionDto;
import io.camunda.optimize.dto.optimize.query.report.single.SingleReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.configuration.SingleReportConfigurationDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionRequestDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.InstanceEndDateFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.MapResultEntryDto;
import io.camunda.optimize.dto.optimize.rest.export.dashboard.DashboardDefinitionExportDto;
import io.camunda.optimize.dto.optimize.rest.export.report.SingleProcessReportDefinitionExportDto;
import io.camunda.optimize.dto.optimize.rest.report.AuthorizedProcessReportEvaluationResponseDto;
import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
import io.camunda.optimize.exception.OptimizeIntegrationTestException;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import io.camunda.optimize.service.dashboard.InstantPreviewDashboardService;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.assertj.core.groups.Tuple;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Tag(OPENSEARCH_PASSING)
public class InstantPreviewDashboardIT extends AbstractDashboardRestServiceIT {

  private static final String SECOND_TENANT = "secondTenant";
  private static final String FIRST_TENANT = "firstTenant";
  private static final String FIRST_DEF_KEY = "someDef";
  private static final String SECOND_DEF_KEY = "otherDef";
  private static final String TEXT_FIELD = "text";
  public static final String EXTERNAL_PATH = "/external";
  public static final String FRONTEND_EXTERNAL_RESOURCES_PATH = "../client/public";
  public static final String TEMPLATE_1_FILENAME = "template1.json";
  public static final String TEMPLATE_2_FILENAME = "template2.json";
  public static final String TEMPLATE_3_FILENAME = "template3.json";
  public static final String PROCESS_DEF_KEY = "dummy";
  private static final Map<String, List<String>> EXPECTED_TILE_STRINGS = createExpectedTileString();

  @Test
  public void instantPreviewDashboardCreatedAsExpected() {
    // given
    final InstantPreviewDashboardService instantPreviewDashboardService =
        embeddedOptimizeExtension.getInstantPreviewDashboardService();
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEF_KEY));
    importAllEngineEntitiesFromScratch();

    // when
    final Optional<InstantDashboardDataDto> instantPreviewDashboard =
        instantPreviewDashboardService.createInstantPreviewDashboard(
            PROCESS_DEF_KEY, TEMPLATE_2_FILENAME);

    // then
    assertThat(instantPreviewDashboard).isPresent();
    final InstantDashboardDataDto instantPreviewDashboardDto = instantPreviewDashboard.get();
    assertThat(instantPreviewDashboardDto.getInstantDashboardId())
        .isEqualTo(PROCESS_DEF_KEY + "_" + TEMPLATE_2_FILENAME.replace(".", ""));
    assertThat(instantPreviewDashboardDto.getProcessDefinitionKey()).isEqualTo(PROCESS_DEF_KEY);
    assertThat(instantPreviewDashboardDto.getTemplateName()).isEqualTo(TEMPLATE_2_FILENAME);
    assertThat(instantPreviewDashboardDto.getTemplateHash())
        .isEqualTo(calculateExpectedChecksum(TEMPLATE_2_FILENAME));
    // when
    DashboardDefinitionRestDto returnedDashboard =
        dashboardClient.getInstantPreviewDashboard(PROCESS_DEF_KEY, TEMPLATE_2_FILENAME);

    // then
    assertThat(returnedDashboard).isNotNull();
    assertThat(returnedDashboard.getId()).isEqualTo(instantPreviewDashboardDto.getDashboardId());
    final DashboardDefinitionRestDto dashboard =
        dashboardClient.getDashboard(returnedDashboard.getId());
    assertThat(dashboard).isNotNull();
    assertThat(dashboard.getTiles()).hasSize(5);
  }

  @ParameterizedTest
  @MethodSource("emptyTemplates")
  public void instantPreviewDashboardEmptyTemplateDefaultsToDefault(final String emptyTemplate) {
    // given
    final InstantPreviewDashboardService instantPreviewDashboardService =
        embeddedOptimizeExtension.getInstantPreviewDashboardService();
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEF_KEY));
    importAllEngineEntitiesFromScratch();

    // when
    final Optional<InstantDashboardDataDto> instantPreviewDashboard =
        instantPreviewDashboardService.createInstantPreviewDashboard(
            PROCESS_DEF_KEY, emptyTemplate);

    // then
    assertThat(instantPreviewDashboard).isPresent();
    final InstantDashboardDataDto instantPreviewDashboardDto = instantPreviewDashboard.get();
    assertThat(instantPreviewDashboardDto.getInstantDashboardId())
        .isEqualTo(
            PROCESS_DEF_KEY + "_" + INSTANT_DASHBOARD_DEFAULT_TEMPLATE.replaceAll("\\.", ""));
    assertThat(instantPreviewDashboardDto.getProcessDefinitionKey()).isEqualTo(PROCESS_DEF_KEY);
    assertThat(instantPreviewDashboardDto.getTemplateName())
        .isEqualTo(INSTANT_DASHBOARD_DEFAULT_TEMPLATE);
    assertThat(instantPreviewDashboardDto.getTemplateHash())
        .isEqualTo(calculateExpectedChecksum(INSTANT_DASHBOARD_DEFAULT_TEMPLATE));

    // when
    DashboardDefinitionRestDto returnedDashboard =
        dashboardClient.getInstantPreviewDashboard(PROCESS_DEF_KEY, emptyTemplate);

    // then
    assertThat(returnedDashboard).isNotNull();
    assertThat(returnedDashboard.getId()).isEqualTo(instantPreviewDashboardDto.getDashboardId());
    final DashboardDefinitionRestDto dashboard =
        dashboardClient.getDashboard(returnedDashboard.getId());
    assertThat(dashboard).isNotNull();
    assertThat(dashboard.getTiles()).hasSize(14);
  }

  @Test
  public void instantPreviewDashboardNonExistingDashboard() {
    // given
    String processDefKey = "never_heard_of";
    String dashboardJsonTemplateFilename = "dummy_template.json";

    // when
    String response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildGetInstantPreviewDashboardRequest(processDefKey, dashboardJsonTemplateFilename)
            .execute(String.class, Response.Status.NOT_FOUND.getStatusCode());

    // then
    assertThat(response).containsSequence("Dashboard does not exist!");
  }

  @Test
  public void getInstantPreviewDashboardWithoutAuthentication() {
    // when
    final Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildGetInstantPreviewDashboardRequest("bla", "bla")
            .withoutAuthentication()
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
  public void changeInTemplateCausesRefreshOfDashboard() {
    // given
    final InstantPreviewDashboardService instantPreviewDashboardService =
        embeddedOptimizeExtension.getInstantPreviewDashboardService();
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEF_KEY));
    importAllEngineEntitiesFromScratch();

    // when
    final Optional<InstantDashboardDataDto> instantPreviewDashboard =
        instantPreviewDashboardService.createInstantPreviewDashboard(
            PROCESS_DEF_KEY, TEMPLATE_3_FILENAME);

    // then
    assertThat(instantPreviewDashboard).isPresent();

    // given
    final InstantDashboardDataDto instantPreviewDashboardDto = instantPreviewDashboard.get();
    DashboardDefinitionRestDto originalDashboard =
        dashboardClient.getInstantPreviewDashboard(PROCESS_DEF_KEY, TEMPLATE_3_FILENAME);
    // Let's keep track of the report IDs that belong to this dashboard, this will be important
    // later
    Set<String> originalReportIds = originalDashboard.getTileIds();

    // when
    // now fiddle with the stored hash in the database so that the code thinks a change has happened
    instantPreviewDashboardDto.setTemplateHash(23L);
    embeddedOptimizeExtension
        .getInstantPreviewDashboardWriter()
        .saveInstantDashboard(instantPreviewDashboardDto);
    // Perform the check that is done at the start-up from Optimize
    embeddedOptimizeExtension
        .getInstantPreviewDashboardService()
        .deleteInstantPreviewDashboardsAndEntitiesForChangedTemplates();
    // Now get the dashboard again
    DashboardDefinitionRestDto newDashboard =
        dashboardClient.getInstantPreviewDashboard(PROCESS_DEF_KEY, TEMPLATE_3_FILENAME);

    // then
    // Since the entry had been de-validated, I expect that a new dashboard with a new ID has been
    // created
    assertThat(newDashboard.getId()).isNotEqualTo(originalDashboard.getId());
    // I expect that the reports from the new dashboard were newly generated
    assertThat(newDashboard.getTileIds()).doesNotContainAnyElementsOf(originalReportIds);
    // Moreover I expect the old dashboard to be deleted
    dashboardClient.assertDashboardIsDeleted(originalDashboard.getId());
    // I also expect the old report IDs to be deleted
    assertThat(originalReportIds)
        .allSatisfy(reportId -> reportClient.assertReportIsDeleted(reportId));
  }

  @Test
  public void aCheckForNewDashboardTemplatesDoesntDevalidateCurrentEntries() {
    // given
    final InstantPreviewDashboardService instantPreviewDashboardService =
        embeddedOptimizeExtension.getInstantPreviewDashboardService();
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEF_KEY));
    importAllEngineEntitiesFromScratch();

    // when
    final Optional<InstantDashboardDataDto> instantPreviewDashboard =
        instantPreviewDashboardService.createInstantPreviewDashboard(
            PROCESS_DEF_KEY, TEMPLATE_3_FILENAME);

    // then
    assertThat(instantPreviewDashboard).isPresent();

    // given
    DashboardDefinitionRestDto originalDashboard =
        dashboardClient.getInstantPreviewDashboard(PROCESS_DEF_KEY, TEMPLATE_3_FILENAME);
    // We keep track of the report IDs that belong to this dashboard, this will be important later
    Set<String> originalReportIds = originalDashboard.getTileIds();

    // when
    // Perform the check that is done at the start-up from Optimize
    embeddedOptimizeExtension
        .getInstantPreviewDashboardService()
        .deleteInstantPreviewDashboardsAndEntitiesForChangedTemplates();
    // Now get the dashboard again. Since the entry was still valid, I expect the same old dashboard
    // with the same ID
    DashboardDefinitionRestDto newDashboard =
        dashboardClient.getInstantPreviewDashboard(PROCESS_DEF_KEY, TEMPLATE_3_FILENAME);

    // then
    assertThat(newDashboard.getId()).isEqualTo(originalDashboard.getId());
    // I expect that the reports from the dashboard also remain the same
    assertThat(newDashboard.getTileIds()).isEqualTo(originalReportIds);
  }

  @Test
  public void existingDashboardsDontGetCreatedAgain() {
    // given
    final InstantPreviewDashboardService instantPreviewDashboardService =
        embeddedOptimizeExtension.getInstantPreviewDashboardService();
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEF_KEY));
    importAllEngineEntitiesFromScratch();

    // when
    final Optional<InstantDashboardDataDto> instantPreviewDashboard =
        instantPreviewDashboardService.createInstantPreviewDashboard(
            PROCESS_DEF_KEY, TEMPLATE_3_FILENAME);

    // then
    assertThat(instantPreviewDashboard).isPresent();

    // given
    DashboardDefinitionRestDto originalDashboard =
        dashboardClient.getInstantPreviewDashboard(PROCESS_DEF_KEY, TEMPLATE_3_FILENAME);
    // We keep track of the report IDs that belong to this dashboard, this will be important later
    Set<String> originalReportIds = originalDashboard.getTileIds();

    // when
    DashboardDefinitionRestDto refetchedDashboard =
        dashboardClient.getInstantPreviewDashboard(PROCESS_DEF_KEY, TEMPLATE_3_FILENAME);

    // then
    // I expect the same old dashboard with the same ID
    assertThat(refetchedDashboard.getId()).isEqualTo(originalDashboard.getId());
    // I expect that the reports from the dashboard also remain the same
    assertThat(refetchedDashboard.getTileIds()).isEqualTo(originalReportIds);
  }

  @Test
  @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
  public void instantPreviewReportsRespectPermissions() {
    // given
    final InstantPreviewDashboardService instantPreviewDashboardService =
        embeddedOptimizeExtension.getInstantPreviewDashboardService();
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEF_KEY));
    importAllEngineEntitiesFromScratch();

    authorizationClient.addKermitUserAndGrantAccessToOptimize();

    // when
    final Optional<InstantDashboardDataDto> instantPreviewDashboard =
        instantPreviewDashboardService.createInstantPreviewDashboard(
            PROCESS_DEF_KEY, TEMPLATE_3_FILENAME);

    // then
    assertThat(instantPreviewDashboard).isPresent();

    // given
    DashboardDefinitionRestDto originalDashboard =
        dashboardClient.getInstantPreviewDashboard(PROCESS_DEF_KEY, TEMPLATE_3_FILENAME);
    originalDashboard
        .getTileIds()
        .forEach(
            reportId -> {
              // when
              Response response =
                  reportClient.evaluateReportAsUserRawResponse(reportId, KERMIT_USER, KERMIT_USER);
              // then
              // Kermit has no access to the process definition, therefore he shall not be able to
              // evaluate the reports
              assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());

              // when
              response =
                  reportClient.evaluateReportAsUserRawResponse(
                      reportId, DEFAULT_USERNAME, DEFAULT_USERNAME);
              // then
              // The default user does have access to the process definition, therefore he is
              // allowed to evaluate it
              assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
              // Moreover we need to make sure that although he can evaluate it, he is only a viewer
              final AuthorizedProcessReportEvaluationResponseDto<Object> result =
                  reportClient.evaluateProcessReport(
                      reportClient.getSingleProcessReportById(reportId));
              // then
              assertThat(result.getCurrentUserRole()).isEqualTo(RoleType.VIEWER);
            });
  }

  @Test
  public void createInstantPreviewEntitiesManuallyNotSupported() {
    // given
    final DashboardDefinitionRestDto instantDashboardToCreate = new DashboardDefinitionRestDto();
    instantDashboardToCreate.setInstantPreviewDashboard(true);
    final SingleProcessReportDefinitionRequestDto instantReportToCreate =
        new SingleProcessReportDefinitionRequestDto();
    instantReportToCreate.getData().setInstantPreviewReport(true);

    // when
    final Response dashboardCreateResponse =
        dashboardClient.createDashboardAndReturnResponse(instantDashboardToCreate);
    final Response reportCreateResponse =
        reportClient.createSingleProcessReportAndReturnResponse(instantReportToCreate);

    // then
    assertThat(dashboardCreateResponse.getStatus())
        .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    assertThat(reportCreateResponse.getStatus())
        .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void getPrivateReportsExcludesInstantPreviewReports() {
    // given
    final InstantPreviewDashboardService instantPreviewDashboardService =
        embeddedOptimizeExtension.getInstantPreviewDashboardService();
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEF_KEY));
    importAllEngineEntitiesFromScratch();

    // when
    instantPreviewDashboardService.createInstantPreviewDashboard(
        PROCESS_DEF_KEY, TEMPLATE_2_FILENAME);

    // when
    assertThat(reportClient.getAllReportsAsUser()).isEmpty();
  }

  @Test
  public void updateInstantPreviewEntitiesNotSupported() {
    // given
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEF_KEY));
    importAllEngineEntitiesFromScratch();
    final DashboardDefinitionRestDto updatedDashboard = new DashboardDefinitionRestDto();
    updatedDashboard.setInstantPreviewDashboard(true);
    DashboardDefinitionRestDto originalDashboard =
        dashboardClient.getInstantPreviewDashboard(PROCESS_DEF_KEY, TEMPLATE_3_FILENAME);
    final Optional<String> instantReportId = originalDashboard.getTileIds().stream().findFirst();
    assertThat(instantReportId).isPresent();

    // when
    final Response dashboardUpdateResponse =
        dashboardClient.updateDashboardAndReturnResponse(
            originalDashboard.getId(), updatedDashboard);
    final Response reportUpdateResponse =
        reportClient.updateSingleProcessReport(
            instantReportId.get(), new SingleProcessReportDefinitionRequestDto());

    // then
    assertThat(dashboardUpdateResponse.getStatus())
        .isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
    assertThat(reportUpdateResponse.getStatus())
        .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void deleteInstantPreviewEntitiesNotSupported() {
    // given
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEF_KEY));
    importAllEngineEntitiesFromScratch();
    DashboardDefinitionRestDto originalDashboard =
        dashboardClient.getInstantPreviewDashboard(PROCESS_DEF_KEY, TEMPLATE_3_FILENAME);
    final Optional<String> instantReportId = originalDashboard.getTileIds().stream().findFirst();
    assertThat(instantReportId).isPresent();

    // when
    final Response dashboardDeleteResponse =
        dashboardClient.deleteDashboardAndReturnResponse(originalDashboard.getId());
    final Response reportDeleteResponse = reportClient.deleteReport(instantReportId.get(), false);

    // then
    assertThat(dashboardDeleteResponse.getStatus())
        .isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
    assertThat(reportDeleteResponse.getStatus())
        .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void importInstantPreviewEntitiesNotSupported() {
    // given
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEF_KEY));
    importAllEngineEntitiesFromScratch();
    DashboardDefinitionRestDto originalDashboard =
        dashboardClient.getInstantPreviewDashboard(PROCESS_DEF_KEY, TEMPLATE_3_FILENAME);
    final DashboardDefinitionExportDto dashboardExport =
        new DashboardDefinitionExportDto(originalDashboard);
    dashboardExport.setInstantPreviewDashboard(true);
    final SingleProcessReportDefinitionExportDto reportExport =
        new SingleProcessReportDefinitionExportDto(new SingleProcessReportDefinitionRequestDto());
    reportExport.getData().setInstantPreviewReport(true);

    // when
    final Response dashboardImportResponse = importClient.importEntity(dashboardExport);
    final Response reportImportResponse = importClient.importEntity(reportExport);

    // then
    assertThat(dashboardImportResponse.getStatus())
        .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    assertThat(reportImportResponse.getStatus())
        .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void copyInstantPreviewEntitiesNotSupported() {
    // given
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEF_KEY));
    importAllEngineEntitiesFromScratch();
    DashboardDefinitionRestDto originalDashboard =
        dashboardClient.getInstantPreviewDashboard(PROCESS_DEF_KEY, TEMPLATE_3_FILENAME);
    final Optional<String> instantReportId = originalDashboard.getTileIds().stream().findFirst();
    assertThat(instantReportId).isPresent();

    // when
    final Response dashboardCopyResponse =
        dashboardClient.copyDashboardAndReturnResponse(originalDashboard.getId());
    final Response reportCopyResponse =
        reportClient.copyReportToCollection(instantReportId.get(), null);

    // then
    assertThat(dashboardCopyResponse.getStatus())
        .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    assertThat(reportCopyResponse.getStatus())
        .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @ParameterizedTest
  @MethodSource("templateAndExpectedLocalizedReportNames")
  @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
  public void instantPreviewDashboardAndReportNamesAreLocalized(
      final String template,
      final String locale,
      final String expectedDashboardName,
      final Set<String> expectedReportNames) {
    // given
    final InstantPreviewDashboardService instantPreviewDashboardService =
        embeddedOptimizeExtension.getInstantPreviewDashboardService();
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEF_KEY));
    importAllEngineEntitiesFromScratch();

    // when
    final Optional<InstantDashboardDataDto> instantPreviewDashboard =
        instantPreviewDashboardService.createInstantPreviewDashboard(PROCESS_DEF_KEY, template);

    // then
    assertThat(instantPreviewDashboard).isPresent();

    // when
    final DashboardDefinitionRestDto localizedDashboard =
        dashboardClient.getInstantPreviewDashboardLocalized(PROCESS_DEF_KEY, template, locale);
    final Set<String> reportNames =
        localizedDashboard.getTileIds().stream()
            .map(
                tileId ->
                    reportClient
                        .evaluateProcessReportLocalized(tileId, locale)
                        .getReportDefinition()
                        .getName())
            .collect(toSet());

    // then
    assertThat(localizedDashboard.getName()).isEqualTo(expectedDashboardName);
    assertThat(reportNames).containsExactlyInAnyOrderElementsOf(expectedReportNames);
  }

  @ParameterizedTest
  @MethodSource("templateAndExpectedLocalizedReports")
  public void instantPreviewDashboardAndReportsAreLocalized(
      final String locale,
      final String expectedDashboardName,
      final List<LocalizedReportData> expectedReports) {
    // given
    final InstantPreviewDashboardService instantPreviewDashboardService =
        embeddedOptimizeExtension.getInstantPreviewDashboardService();
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEF_KEY));
    importAllEngineEntitiesFromScratch();

    // when
    final Optional<InstantDashboardDataDto> instantPreviewDashboard =
        instantPreviewDashboardService.createInstantPreviewDashboard(
            PROCESS_DEF_KEY, TEMPLATE_1_FILENAME);

    // then
    assertThat(instantPreviewDashboard).isPresent();

    // when
    final DashboardDefinitionRestDto localizedDashboard =
        dashboardClient.getInstantPreviewDashboardLocalized(
            PROCESS_DEF_KEY, TEMPLATE_1_FILENAME, locale);
    final List<LocalizedReportData> localizedReports =
        localizedDashboard.getTiles().stream()
            .filter(tile -> tile.getType() == DashboardTileType.OPTIMIZE_REPORT)
            .map(
                tile -> {
                  final ReportDefinitionDto<?> dashboardReport =
                      reportClient.getReportById(tile.getId(), locale);
                  final SingleReportConfigurationDto reportConfig =
                      ((SingleReportDataDto) dashboardReport.getData()).getConfiguration();
                  return new LocalizedReportData(
                      dashboardReport.getName(),
                      dashboardReport.getDescription(),
                      reportConfig.getYLabel(),
                      reportConfig.getXLabel());
                })
            .toList();

    // then
    assertThat(localizedDashboard.getName()).isEqualTo(expectedDashboardName);
    assertThat(localizedReports).containsExactlyInAnyOrderElementsOf(expectedReports);
  }

  @ParameterizedTest
  @MethodSource("templateAndLocales")
  public void checkLocalizationFromTextTileTexts(String template, String locale) {
    // given
    final InstantPreviewDashboardService instantPreviewDashboardService =
        embeddedOptimizeExtension.getInstantPreviewDashboardService();
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEF_KEY));
    importAllEngineEntitiesFromScratch();

    // when
    final Optional<InstantDashboardDataDto> instantPreviewDashboard =
        instantPreviewDashboardService.createInstantPreviewDashboard(PROCESS_DEF_KEY, template);

    // then
    assertThat(instantPreviewDashboard).isPresent();

    // when
    final DashboardDefinitionRestDto dashboardData =
        dashboardClient.getInstantPreviewDashboardLocalized(PROCESS_DEF_KEY, template, locale);

    dashboardData
        .getTiles()
        .forEach(
            tile -> {
              if (tile.getType() == DashboardTileType.TEXT) {
                final Map<String, Object> textTileConfiguration =
                    (Map<String, Object>) tile.getConfiguration();
                InstantPreviewDashboardService.findAndConvertTileContent(
                    textTileConfiguration, TEXT_FIELD, this::assertTileTranslation, locale);
              }
            });
  }

  @Test
  @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
  public void savedInstantPreviewReportCanBeEvaluatedAndIncludesAllTenants() {
    // given
    engineIntegrationExtension.createTenant(FIRST_TENANT);
    engineIntegrationExtension.createTenant(SECOND_TENANT);
    engineIntegrationExtension.deployAndStartProcess(
        getSingleUserTaskDiagram(FIRST_DEF_KEY), FIRST_TENANT);
    importAllEngineEntitiesFromScratch();

    final Set<String> reportIds = getInstantPreviewReportIDsForProcess(FIRST_DEF_KEY);
    reportIds.forEach(
        reportId -> {
          // when
          final AuthorizedProcessReportEvaluationResponseDto<List<MapResultEntryDto>>
              evaluationResponse = reportClient.evaluateReport(reportId);

          // then
          ProcessReportDataDto resultReportDataDto =
              evaluationResponse.getReportDefinition().getData();
          assertThat(resultReportDataDto.isInstantPreviewReport()).isTrue();
          assertThat(resultReportDataDto.getDefinitions())
              .hasSize(1)
              .extracting(
                  ReportDataDefinitionDto::getKey,
                  ReportDataDefinitionDto::getVersions,
                  ReportDataDefinitionDto::getTenantIds)
              .containsExactlyInAnyOrder(
                  // the process includes one tenant
                  Tuple.tuple(FIRST_DEF_KEY, List.of(ALL_VERSIONS), List.of(FIRST_TENANT)));
          // the result includes data from one tenant
          final ReportResultResponseDto<List<MapResultEntryDto>> resultDto =
              evaluationResponse.getResult();
          // getInstanceCountWithoutFilters() is the same as getInstanceCount() if the filter is of
          // type
          // InstanceEndDateFilterDto, so excluding that one. This is an intentional behaviour from
          // the filter
          if (resultReportDataDto.getFilter().stream()
              .noneMatch(c -> c instanceof InstanceEndDateFilterDto)) {
            assertThat(resultDto.getInstanceCountWithoutFilters()).isEqualTo(1L);
          }
        });

    engineIntegrationExtension.deployAndStartProcess(
        getSingleUserTaskDiagram(FIRST_DEF_KEY), SECOND_TENANT);
    importAllEngineEntitiesFromLastIndex();

    reportIds.forEach(
        reportId -> {
          // when
          final AuthorizedProcessReportEvaluationResponseDto<List<MapResultEntryDto>>
              evaluationResponse = reportClient.evaluateReport(reportId);

          // then
          ProcessReportDataDto resultReportDataDto =
              evaluationResponse.getReportDefinition().getData();
          assertThat(resultReportDataDto.isInstantPreviewReport()).isTrue();
          assertThat(resultReportDataDto.getDefinitions())
              .hasSize(1)
              .extracting(
                  ReportDataDefinitionDto::getKey,
                  ReportDataDefinitionDto::getVersions,
                  ReportDataDefinitionDto::getTenantIds)
              .containsExactlyInAnyOrder(
                  // the process includes both tenants
                  Tuple.tuple(
                      FIRST_DEF_KEY, List.of(ALL_VERSIONS), List.of(FIRST_TENANT, SECOND_TENANT)));
          // the result includes data from both tenants
          final ReportResultResponseDto<List<MapResultEntryDto>> resultDto =
              evaluationResponse.getResult();
          // getInstanceCountWithoutFilters() is the same as getInstanceCount() if the filter is of
          // type
          // InstanceEndDateFilterDto, so excluding that one. This is an intentional behaviour from
          // the filter
          if (resultReportDataDto.getFilter().stream()
              .noneMatch(c -> c instanceof InstanceEndDateFilterDto)) {
            assertThat(resultDto.getInstanceCountWithoutFilters()).isEqualTo(2L);
          }
        });
  }

  @Test
  @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
  public void savedInstantPreviewReportCanBeEvaluatedAndExcludesUnauthorizedTenants() {
    // given
    engineIntegrationExtension.createTenant(FIRST_TENANT);
    // kermit not authorized for second tenant
    engineIntegrationExtension.createTenant(SECOND_TENANT);
    final ProcessInstanceEngineDto processInstance =
        engineIntegrationExtension.deployAndStartProcess(
            getSingleUserTaskDiagram(FIRST_DEF_KEY), FIRST_TENANT);
    // Kermit is not authorized to see the data for these two instances
    engineIntegrationExtension.deployAndStartProcess(
        getSingleUserTaskDiagram(FIRST_DEF_KEY), SECOND_TENANT);
    engineIntegrationExtension.deployAndStartProcess(
        getSingleUserTaskDiagram(SECOND_DEF_KEY), SECOND_TENANT);

    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.grantSingleResourceAuthorizationsForUser(
        KERMIT_USER, processInstance.getProcessDefinitionKey(), RESOURCE_TYPE_PROCESS_DEFINITION);
    authorizationClient.grantSingleResourceAuthorizationsForUser(
        KERMIT_USER, FIRST_TENANT, RESOURCE_TYPE_TENANT);

    importAllEngineEntitiesFromScratch();

    final Set<String> reportIds = getInstantPreviewReportIDsForProcess(FIRST_DEF_KEY);

    reportIds.forEach(
        reportId -> {
          // when
          final AuthorizedProcessReportEvaluationResponseDto<List<MapResultEntryDto>>
              evaluationResponse = reportClient.evaluateReportAsKermit(reportId);

          // then
          ProcessReportDataDto resultReportDataDto =
              evaluationResponse.getReportDefinition().getData();
          assertThat(resultReportDataDto.isInstantPreviewReport()).isTrue();
          assertThat(resultReportDataDto.getDefinitions())
              .hasSize(1)
              .extracting(
                  ReportDataDefinitionDto::getKey,
                  ReportDataDefinitionDto::getVersions,
                  ReportDataDefinitionDto::getTenantIds)
              .containsExactlyInAnyOrder(
                  // the process includes only one tenant
                  Tuple.tuple(FIRST_DEF_KEY, List.of(ALL_VERSIONS), List.of(FIRST_TENANT)));
          // the result includes data from only from FIRST_TENANT (one instance)
          final ReportResultResponseDto<List<MapResultEntryDto>> resultDto =
              evaluationResponse.getResult();
          // getInstanceCountWithoutFilters() is the same as getInstanceCount() if the filter is of
          // type
          // InstanceEndDateFilterDto, so excluding that one. This is an intentional behaviour from
          // the filter
          if (resultReportDataDto.getFilter().stream()
              .noneMatch(c -> c instanceof InstanceEndDateFilterDto)) {
            assertThat(resultDto.getInstanceCountWithoutFilters()).isEqualTo(1L);
          }
        });
  }

  @Test
  @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
  public void allDashboardTemplatesAreValid() {
    // given
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEF_KEY));
    importAllEngineEntitiesFromScratch();
    final InstantPreviewDashboardService instantPreviewDashboardService =
        embeddedOptimizeExtension.getInstantPreviewDashboardService();
    final List<String> templateFiles = templates().collect(Collectors.toList());

    // then there are the expected number of template files
    assertThat(templateFiles)
        .hasSameSizeAs(instantPreviewDashboardService.getCurrentFileChecksums());

    templateFiles.forEach(
        templateFile -> {
          // when
          final Optional<InstantDashboardDataDto> instantPreviewDashboard =
              instantPreviewDashboardService.createInstantPreviewDashboard(
                  PROCESS_DEF_KEY, templateFile);

          // then
          assertThat(instantPreviewDashboard).isPresent();
          final InstantDashboardDataDto instantPreviewDashboardDto = instantPreviewDashboard.get();

          // when
          DashboardDefinitionRestDto returnedDashboard =
              dashboardClient.getInstantPreviewDashboard(PROCESS_DEF_KEY, templateFile);

          // then
          assertThat(returnedDashboard).isNotNull();
          assertThat(returnedDashboard.getId())
              .isEqualTo(instantPreviewDashboardDto.getDashboardId());
          assertThat(returnedDashboard.isInstantPreviewDashboard()).isTrue();
          assertThat(returnedDashboard.isManagementDashboard()).isFalse();
          final DashboardDefinitionRestDto dashboard =
              dashboardClient.getDashboard(returnedDashboard.getId());
          assertThat(dashboard).isNotNull();
          dashboard.getTileIds().stream()
              .map(tileId -> reportClient.evaluateReport(tileId).getReportDefinition().getData())
              .forEach(
                  reportData -> {
                    assertThat(reportData.getConfiguration().getTargetValue().getIsKpi()).isFalse();
                    assertThat(reportData.isManagementReport()).isFalse();
                    assertThat(reportData.isInstantPreviewReport()).isTrue();
                  });
          dashboard
              .getTiles()
              .forEach(
                  tile -> {
                    if (tile.getType() == DashboardTileType.TEXT) {
                      final Map<String, Object> textTileConfiguration =
                          (Map<String, Object>) tile.getConfiguration();
                      InstantPreviewDashboardService.findAndConvertTileContent(
                          textTileConfiguration, TYPE_IMAGE_VALUE, this::assertImagePresent, "src");
                    }
                  });
        });
  }

  private Set<String> getInstantPreviewReportIDsForProcess(final String processDefKey) {
    DashboardDefinitionRestDto originalDashboard =
        dashboardClient.getInstantPreviewDashboard(processDefKey, TEMPLATE_3_FILENAME);
    final Set<String> reportIds = originalDashboard.getTileIds();
    assertThat(reportIds).isNotEmpty();
    return reportIds;
  }

  @NotNull
  private Long calculateExpectedChecksum(final String dashboardJsonTemplate) {
    InputStream templateInputStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream(INSTANT_PREVIEW_DASHBOARD_TEMPLATES_PATH + dashboardJsonTemplate);
    try {
      return getChecksumCRC32(templateInputStream, 8192);
    } catch (IOException e) {
      throw new OptimizeIntegrationTestException(
          "Failed to calculate expected checksum for template", e);
    }
  }

  private void assertImagePresent(
      Map<String, Object> textTileConfiguration, final String imageTag) {
    // Reverse-engineering the frontend external resources URI to the frontend resources folder
    String filePath =
        ((String) textTileConfiguration.get(imageTag))
            .replace(EXTERNAL_PATH, FRONTEND_EXTERNAL_RESOURCES_PATH);
    // then
    assertThat(new File(filePath)).exists();
  }

  private static Map<String, List<String>> createExpectedTileString() {
    return Map.of(
        "de",
            List.of(
                "Instant Prozessübersicht",
                "Dieses Dashboard bietet eine Übersicht der Metriken und Anwendungsfälle, die mit Optimize abgedeckt werden können.",
                "Melde Geschäftskennzahlen",
                "Aggregiere und gruppiere Prozessausführungsdaten in Tagen, Wochen und Monaten um Metriken und Key Performance "
                    + "Indikatoren (KPI) zu überwachen und zu melden.",
                "Abonniere einen wöchentlichen E-Mail Digest der Prozessmetriken und KPIs",
                "Analysiere Probleme und finde Verbesserungen",
                "Nutze alle gesammelten Prozessausführungsdaten, um Probleme zu untersuchen oder neue Prozessverbesserungen einzuleiten.",
                "Nutze die Optimize Ausreißeranalyse, um Verbesserungspotenzial im Prozess zu finden",
                "Echtzeitnahe Überwachung der Prozess Gesundheit",
                "Überwache Prozessausführungsdaten der letzten Minuten, Stunden und Tage echtzeitnah, um korrektiv eingreifen zu können.",
                "Erstellen Sie eine Kopie dieses Dashboards und passen Sie es Ihren Bedürfnissen an",
                "Aktuell laufend",
                "Dieser Bericht zeigt den Fortschritt aller laufenden Prozesse an.",
                "In den letzten 7 Tagen eingegangene",
                "Dieser Bericht zeigt die in den letzten 7 Tagen eingegangenen Anfragen die einen Prozess gestartet haben.",
                "Aktuell laufende Prozesse",
                "Dieser Bericht zeigt die Anzahl der aktuell laufenden Prozesse.",
                "In den letzten 7 Tagen beendete Prozesse",
                "Dieser Bericht zeigt die Anzahl der in den letzten 7 Tagen beendeten Prozesse.",
                "Offene Zwischenfälle",
                "Dieser Bericht zeigt die Anzahl der offenen Zwischenfälle.",
                "Anzahl der aktuell offenen Zwischenfälle",
                "Dieser Bericht zeigt Prozessknoten mit aktuell offenen Zwischenfälle.",
                "Beendete Prozesse gruppiert in Monaten",
                "Dieser Bericht zeigt wie stark der Prozess in den letzten Monaten genutzt wurde.",
                "Flaschenhälse der in den letzten 6 Monaten beendeten Prozesse",
                "Die Heatmap hebt die Prozessknoten hervor, die im Durchschnitt die meiste Zeit bis zur Fertigstellung benötigen."),
        "en",
            List.of(
                "Instant process dashboard",
                "This dashboard provides a competitive overview of metrics and use cases that can be covered with Optimize. It is "
                    + "available for any process as soon as a process is deployed.",
                "Report business metrics",
                "Aggregate and group the process execution data into days, weeks, and months to monitor and report business metrics and"
                    + " key performance indicators (KPI).",
                "Subscribe to an email digest to obtain a weekly summary of your process metrics and KPIs",
                "Investigate problems and find improvements",
                "Use all gathered process execution data to investigate problems or kick off new process improvements.",
                "Use Optimize outlier analysis to find improvement potential in your process",
                "Monitor process health in near real-time",
                "See process execution data of the last minutes, hours, and days in near real-time to take corrective actions if needed.",
                "Create a copy of this dashboard to be able to adjust it to your needs",
                "Currently running processes",
                "This report provides an overview of the progress of all running processes. ",
                "Incoming in the last 7 days",
                "This report counts the number of incoming requests that started a process.",
                "Currently in progress",
                "This report counts all currently running processes.",
                "Ended in the last 7 days",
                "This report counts all processes that ended in the last 7 days.",
                "Open incidents",
                "This report shows the number of open incidents.",
                "Currently open incidents",
                "This report shows flow nodes with currently open incidents.",
                "Processes ended grouped by month",
                "This report provides an overview of the utilization of the process in the last months. ",
                "Bottlenecks in the process ended in the last 6 months",
                "The heatmap highlights the flow nodes that consume on average the most time to be completed."));
  }

  private void assertTileTranslation(Map<String, Object> textTileConfiguration, String locale) {
    String textContent = (String) textTileConfiguration.get(TEXT_FIELD);
    assertThat(EXPECTED_TILE_STRINGS.get(locale)).contains(textContent);
  }

  @SuppressWarnings(UNUSED)
  public static Stream<String> emptyTemplates() {
    return Stream.of("", null);
  }

  @SuppressWarnings(UNUSED)
  private static Stream<Arguments> templateAndExpectedLocalizedReportNames() {
    return Stream.of(
        Arguments.of(
            TEMPLATE_3_FILENAME,
            "en",
            "Instant preview dashboard",
            Set.of(
                "% SLA Met",
                "Which process steps take too much time? (To do: Add target values for these process steps)",
                "Is my process within control?",
                "Where are the active incidents?",
                "Incident-free rate",
                "Where are the worst incidents?",
                "99th percentile duration",
                "Are we improving incident handling?",
                "Throughput (30-day rolling)",
                "75th percentile duration",
                "How frequently is this process run?",
                "How often is each process step run?")),
        Arguments.of(
            TEMPLATE_3_FILENAME,
            "de",
            "Instant preview dashboard",
            Set.of(
                "% SLA erfüllt",
                "Welche Prozessschritte benötigen zu viel Zeit? (To Do: Ziellaufzeit festlegen)",
                "Ist mein Prozess unter Kontrolle?",
                "Wo sind die aktiven Zwischenfälle?",
                "Prozent ohne Zwischenfälle",
                "Wo sind die schwerwiegendsten Zwischenfälle?",
                "99. Perzentil der Dauer",
                "Wird unser Umgang mit Zwischenfällen besser?",
                "Durchsatz (gleitend über die letzten 30 Tage)",
                "75. Perzentil der Dauer",
                "Wie oft läuft dieser Prozess?",
                "Wie oft laufen die einzelnen Schritte dieses Prozesses?")),
        Arguments.of(
            TEMPLATE_2_FILENAME,
            "en",
            "KPI dashboard",
            Set.of(
                "% SLA Met",
                "Incident-free rate",
                "99th percentile duration",
                "Throughput (30-day rolling)",
                "75th percentile duration")),
        Arguments.of(
            TEMPLATE_2_FILENAME,
            "de",
            "KPI dashboard",
            Set.of(
                "% SLA erfüllt",
                "Prozent ohne Zwischenfälle",
                "99. Perzentil der Dauer",
                "Durchsatz (gleitend über die letzten 30 Tage)",
                "75. Perzentil der Dauer")));
  }

  @SuppressWarnings(UNUSED)
  private static Stream<Arguments> templateAndExpectedLocalizedReports() {
    return Stream.of(
        Arguments.of(
            "en",
            "Instant process dashboard",
            List.of(
                new LocalizedReportData(
                    "Currently in progress",
                    "This report counts all currently running processes.",
                    "Process instance Count",
                    "None"),
                new LocalizedReportData(
                    "Currently running processes",
                    "This report provides an overview of the progress of all running processes. ",
                    "Flow node Count",
                    "Flow nodes"),
                new LocalizedReportData(
                    "Currently open incidents",
                    "This report shows flow nodes with currently open incidents.",
                    "Incident Count",
                    "Flow nodes"),
                new LocalizedReportData(
                    "Incoming in the last 7 days",
                    "This report counts the number of incoming requests that started a process.",
                    "Process instance Count",
                    "None"),
                new LocalizedReportData(
                    "Ended in the last 7 days",
                    "This report counts all processes that ended in the last 7 days.",
                    "Process instance Count",
                    "None"),
                new LocalizedReportData(
                    "Processes ended grouped by month",
                    "This report provides an overview of the utilization of the process in the last months. ",
                    "Process instance Count",
                    "End date"),
                new LocalizedReportData(
                    "Open incidents",
                    "This report shows the number of open incidents.",
                    "Incident Count",
                    "None"),
                new LocalizedReportData(
                    "Bottlenecks in the process ended in the last 6 months",
                    "The heatmap highlights the flow nodes that consume on average the most time to be completed.",
                    "Flow node Duration",
                    "Flow nodes"))),
        Arguments.of(
            "de",
            "Instant Prozessübersicht",
            List.of(
                new LocalizedReportData(
                    "Aktuell laufende Prozesse",
                    "Dieser Bericht zeigt die Anzahl der aktuell laufenden Prozesse.",
                    "Prozessinstanz Anzahl",
                    "Keiner"),
                new LocalizedReportData(
                    "Aktuell laufend",
                    "Dieser Bericht zeigt den Fortschritt aller laufenden Prozesse an.",
                    "Prozessknoten Anzahl",
                    "Prozessknoten"),
                new LocalizedReportData(
                    "Anzahl der aktuell offenen Zwischenfälle",
                    "Dieser Bericht zeigt Prozessknoten mit aktuell offenen Zwischenfälle.",
                    "Zwischenfall Anzahl",
                    "Prozessknoten"),
                new LocalizedReportData(
                    "In den letzten 7 Tagen eingegangene",
                    "Dieser Bericht zeigt die in den letzten 7 Tagen eingegangenen Anfragen die einen Prozess gestartet haben.",
                    "Prozessinstanz Anzahl",
                    "Keiner"),
                new LocalizedReportData(
                    "In den letzten 7 Tagen beendete Prozesse",
                    "Dieser Bericht zeigt die Anzahl der in den letzten 7 Tagen beendeten Prozesse.",
                    "Prozessinstanz Anzahl",
                    "Keiner"),
                new LocalizedReportData(
                    "Beendete Prozesse gruppiert in Monaten",
                    "Dieser Bericht zeigt wie stark der Prozess in den letzten Monaten genutzt wurde.",
                    "Prozessinstanz Anzahl",
                    "Enddatum"),
                new LocalizedReportData(
                    "Offene Zwischenfälle",
                    "Dieser Bericht zeigt die Anzahl der offenen Zwischenfälle.",
                    "Zwischenfall Anzahl",
                    "Keiner"),
                new LocalizedReportData(
                    "Flaschenhälse der in den letzten 6 Monaten beendeten Prozesse",
                    "Die Heatmap hebt die Prozessknoten hervor, die im Durchschnitt die meiste Zeit bis zur Fertigstellung benötigen.",
                    "Prozessknoten Dauer",
                    "Prozessknoten"))));
  }

  @AllArgsConstructor
  @Data
  public static final class LocalizedReportData {
    private final String reportName;
    private final String reportDescription;
    private final String yLabel;
    private final String xLabel;
  }

  @SuppressWarnings(UNUSED)
  private static Stream<Arguments> templateAndLocales() {
    return Stream.of(
        Arguments.of(TEMPLATE_1_FILENAME, "en"), Arguments.of(TEMPLATE_1_FILENAME, "de"));
  }

  @SuppressWarnings(UNUSED)
  private static Stream<String> templates() {
    return Stream.of(TEMPLATE_1_FILENAME, TEMPLATE_2_FILENAME, TEMPLATE_3_FILENAME);
  }
}