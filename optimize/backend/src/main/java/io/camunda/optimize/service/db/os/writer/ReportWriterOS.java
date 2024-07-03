/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.os.writer;

import static io.camunda.optimize.service.db.DatabaseConstants.COMBINED_REPORT_INDEX_NAME;
import static io.camunda.optimize.service.db.DatabaseConstants.NUMBER_OF_RETRIES_ON_CONFLICT;
import static io.camunda.optimize.service.db.DatabaseConstants.SINGLE_DECISION_REPORT_INDEX_NAME;
import static io.camunda.optimize.service.db.DatabaseConstants.SINGLE_PROCESS_REPORT_INDEX_NAME;
import static io.camunda.optimize.service.db.schema.index.report.AbstractReportIndex.COLLECTION_ID;
import static io.camunda.optimize.service.db.schema.index.report.AbstractReportIndex.DATA;
import static io.camunda.optimize.service.db.schema.index.report.AbstractReportIndex.DESCRIPTION;
import static io.camunda.optimize.service.db.schema.index.report.CombinedReportIndex.REPORTS;
import static io.camunda.optimize.service.db.schema.index.report.CombinedReportIndex.REPORT_ITEM_ID;
import static io.camunda.optimize.service.db.schema.index.report.SingleProcessReportIndex.MANAGEMENT_REPORT;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.optimize.dto.optimize.query.IdResponseDto;
import io.camunda.optimize.dto.optimize.query.report.ReportDefinitionUpdateDto;
import io.camunda.optimize.dto.optimize.query.report.combined.CombinedReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.combined.CombinedReportDefinitionRequestDto;
import io.camunda.optimize.dto.optimize.query.report.single.decision.DecisionReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.decision.SingleDecisionReportDefinitionRequestDto;
import io.camunda.optimize.dto.optimize.query.report.single.decision.SingleDecisionReportDefinitionUpdateDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionRequestDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionUpdateDto;
import io.camunda.optimize.service.db.os.OptimizeOpenSearchClient;
import io.camunda.optimize.service.db.os.externalcode.client.dsl.QueryDSL;
import io.camunda.optimize.service.db.writer.DatabaseWriterUtil;
import io.camunda.optimize.service.db.writer.ReportWriter;
import io.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import io.camunda.optimize.service.security.util.LocalDateUtil;
import io.camunda.optimize.service.util.IdGenerator;
import io.camunda.optimize.service.util.configuration.condition.OpenSearchCondition;
import jakarta.json.JsonValue;
import jakarta.ws.rs.NotFoundException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch._types.Result;
import org.opensearch.client.opensearch._types.Script;
import org.opensearch.client.opensearch._types.query_dsl.ChildScoreMode;
import org.opensearch.client.opensearch._types.query_dsl.NestedQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.DeleteResponse;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.core.UpdateRequest;
import org.opensearch.client.opensearch.core.UpdateResponse;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
@Slf4j
@Conditional(OpenSearchCondition.class)
public class ReportWriterOS implements ReportWriter {

  private final ObjectMapper objectMapper;
  private final OptimizeOpenSearchClient osClient;

  @Override
  public IdResponseDto createNewCombinedReport(
      @NonNull final String userId,
      @NonNull final CombinedReportDataDto reportData,
      @NonNull final String reportName,
      final String description,
      final String collectionId) {
    log.debug("Writing new combined report to OpenSearch");
    final String id = IdGenerator.getNextId();
    final CombinedReportDefinitionRequestDto reportDefinitionDto =
        new CombinedReportDefinitionRequestDto();

    reportDefinitionDto.setId(id);
    final OffsetDateTime now = LocalDateUtil.getCurrentDateTime();
    reportDefinitionDto.setCreated(now);
    reportDefinitionDto.setLastModified(now);
    reportDefinitionDto.setOwner(userId);
    reportDefinitionDto.setLastModifier(userId);
    reportDefinitionDto.setName(reportName);
    reportDefinitionDto.setDescription(description);
    reportDefinitionDto.setData(reportData);
    reportDefinitionDto.setCollectionId(collectionId);

    IndexRequest.Builder<CombinedReportDefinitionRequestDto> request =
        new IndexRequest.Builder<CombinedReportDefinitionRequestDto>()
            .index(COMBINED_REPORT_INDEX_NAME)
            .id(id)
            .document(reportDefinitionDto)
            .refresh(Refresh.True);

    IndexResponse indexResponse = osClient.index(request);

    if (!indexResponse.result().equals(Result.Created)) {
      String message =
          String.format(
              "Could not write report with id [%s] and name [%s] to OpenSearch.", id, reportName);
      log.error(message);
      throw new OptimizeRuntimeException(message);
    }

    log.debug("Report with id [{}] has successfully been created.", id);
    return new IdResponseDto(id);
  }

  @Override
  public IdResponseDto createNewSingleProcessReport(
      final String userId,
      @NonNull final ProcessReportDataDto reportData,
      @NonNull final String reportName,
      final String description,
      final String collectionId) {
    log.debug("Writing new single report to OpenSearch");

    final String id = IdGenerator.getNextId();
    final SingleProcessReportDefinitionRequestDto reportDefinitionDto =
        new SingleProcessReportDefinitionRequestDto();

    reportDefinitionDto.setId(id);
    final OffsetDateTime now = LocalDateUtil.getCurrentDateTime();
    reportDefinitionDto.setCreated(now);
    reportDefinitionDto.setLastModified(now);
    reportDefinitionDto.setOwner(userId);
    reportDefinitionDto.setLastModifier(userId);
    reportDefinitionDto.setName(reportName);
    reportDefinitionDto.setDescription(description);
    reportDefinitionDto.setData(reportData);
    reportDefinitionDto.setCollectionId(collectionId);

    IndexRequest.Builder<SingleProcessReportDefinitionRequestDto> request =
        new IndexRequest.Builder<SingleProcessReportDefinitionRequestDto>()
            .index(SINGLE_PROCESS_REPORT_INDEX_NAME)
            .id(id)
            .document(reportDefinitionDto)
            .refresh(Refresh.True);

    IndexResponse indexResponse = osClient.index(request);

    if (!indexResponse.result().equals(Result.Created)) {
      String message =
          String.format(
              "Could not write single process report with id [%s] and name [%s] to OpenSearch.",
              id, reportName);
      log.error(message);
      throw new OptimizeRuntimeException(message);
    }

    log.debug("Single process report with id [{}] has successfully been created.", id);
    return new IdResponseDto(id);
  }

  @Override
  public IdResponseDto createNewSingleDecisionReport(
      @NonNull final String userId,
      @NonNull final DecisionReportDataDto reportData,
      @NonNull final String reportName,
      final String description,
      final String collectionId) {
    log.debug("Writing new single report to OpenSearch");

    final String id = IdGenerator.getNextId();
    final SingleDecisionReportDefinitionRequestDto reportDefinitionDto =
        new SingleDecisionReportDefinitionRequestDto();
    reportDefinitionDto.setId(id);
    final OffsetDateTime now = LocalDateUtil.getCurrentDateTime();
    reportDefinitionDto.setCreated(now);
    reportDefinitionDto.setLastModified(now);
    reportDefinitionDto.setOwner(userId);
    reportDefinitionDto.setLastModifier(userId);
    reportDefinitionDto.setName(reportName);
    reportDefinitionDto.setDescription(description);
    reportDefinitionDto.setData(reportData);
    reportDefinitionDto.setCollectionId(collectionId);

    IndexRequest.Builder<SingleDecisionReportDefinitionRequestDto> request =
        new IndexRequest.Builder<SingleDecisionReportDefinitionRequestDto>()
            .index(SINGLE_DECISION_REPORT_INDEX_NAME)
            .id(id)
            .document(reportDefinitionDto)
            .refresh(Refresh.True);

    IndexResponse indexResponse = osClient.index(request);

    if (!indexResponse.result().equals(Result.Created)) {
      String message =
          String.format(
              "Could not write single decision report with id [%s] and name [%s] to OpenSearch.",
              id, reportName);
      log.error(message);
      throw new OptimizeRuntimeException(message);
    }

    log.debug("Single decision report with id [{}] has successfully been created.", id);
    return new IdResponseDto(id);
  }

  @Override
  public void updateSingleProcessReport(final SingleProcessReportDefinitionUpdateDto reportUpdate) {
    updateReport(reportUpdate, SINGLE_PROCESS_REPORT_INDEX_NAME);
  }

  @Override
  public void updateSingleDecisionReport(
      final SingleDecisionReportDefinitionUpdateDto reportUpdate) {
    updateReport(reportUpdate, SINGLE_DECISION_REPORT_INDEX_NAME);
  }

  @Override
  public void updateCombinedReport(final ReportDefinitionUpdateDto updatedReport) {
    updateReport(updatedReport, COMBINED_REPORT_INDEX_NAME);
  }

  @Override
  public void updateProcessDefinitionXmlForProcessReportsWithKey(
      final String definitionKey, final String definitionXml) {
    final String updateItem = String.format("reports with definitionKey [%s]", definitionKey);
    log.debug("Updating definition XML in {} in OpenSearch", updateItem);

    final Script updateDefinitionXmlScript =
        OpenSearchWriterUtil.createDefaultScriptWithPrimitiveParams(
            "ctx._source.data.configuration.xml = params.newXml;",
            Collections.singletonMap("newXml", JsonData.of(definitionXml)));

    osClient.updateByQuery(
        SINGLE_PROCESS_REPORT_INDEX_NAME,
        QueryDSL.term(PROCESS_DEFINITION_PROPERTY, definitionKey),
        updateDefinitionXmlScript);
  }

  @Override
  public void deleteAllManagementReports() {
    osClient.deleteByQuery(
        QueryDSL.term(DATA + "." + MANAGEMENT_REPORT, true),
        true,
        SINGLE_PROCESS_REPORT_INDEX_NAME);
  }

  @Override
  public void deleteSingleReport(final String reportId) {
    osClient.deleteByQuery(
        QueryDSL.ids(reportId),
        true,
        SINGLE_PROCESS_REPORT_INDEX_NAME,
        SINGLE_DECISION_REPORT_INDEX_NAME);
  }

  @Override
  public void removeSingleReportFromCombinedReports(final String reportId) {
    String updateItemName = String.format("report with ID [%s]", reportId);
    log.info("Removing {} from combined report.", updateItemName);

    final Script removeReportIdFromCombinedReportsScript =
        OpenSearchWriterUtil.createDefaultScriptWithPrimitiveParams(
            "def reports = ctx._source.data.reports;"
                + "if(reports != null) {"
                + "  reports.removeIf(r -> r.id.equals(params.idToRemove)); }",
            Collections.singletonMap("idToRemove", JsonData.of(reportId)));

    Query nested =
        new NestedQuery.Builder()
            .path(String.join(".", DATA, REPORTS))
            .query(QueryDSL.term(String.join(".", DATA, REPORTS, REPORT_ITEM_ID), reportId))
            .scoreMode(ChildScoreMode.None)
            .build()
            .query();

    Query query =
        new NestedQuery.Builder()
            .path(DATA)
            .query(nested)
            .scoreMode(ChildScoreMode.None)
            .build()
            .query();

    osClient.updateByQuery(
        COMBINED_REPORT_INDEX_NAME, query, removeReportIdFromCombinedReportsScript);
  }

  @Override
  public void deleteCombinedReport(final String reportId) {
    log.debug("Deleting combined report with id [{}]", reportId);

    DeleteRequest.Builder requestBuilder =
        new DeleteRequest.Builder()
            .index(COMBINED_REPORT_INDEX_NAME)
            .id(reportId)
            .refresh(Refresh.True);

    DeleteResponse deleteResponse;

    String reason = String.format("Could not delete combined report with id [%s].", reportId);

    deleteResponse = osClient.delete(requestBuilder, reason);

    if (!deleteResponse.result().equals(Result.Deleted)) {
      String message =
          String.format(
              "Could not delete combined process report with id [%s]. "
                  + "Combined process report does not exist."
                  + "Maybe it was already deleted by someone else?",
              reportId);
      log.error(message);
      throw new NotFoundException(message);
    }
  }

  @Override
  public void deleteAllReportsOfCollection(String collectionId) {
    osClient.deleteByQuery(
        QueryDSL.term(COLLECTION_ID, collectionId),
        true,
        COMBINED_REPORT_INDEX_NAME,
        SINGLE_PROCESS_REPORT_INDEX_NAME,
        SINGLE_DECISION_REPORT_INDEX_NAME);
  }

  private void updateReport(ReportDefinitionUpdateDto updatedReport, String indexName) {
    log.debug("Updating report with id [{}] in OpenSearch", updatedReport.getId());
    final Map<String, JsonData> updateParams =
        OpenSearchWriterUtil.createFieldUpdateScriptParams(
            UPDATABLE_FIELDS, updatedReport, objectMapper);
    // We always update the description, even if the new value is null
    final JsonData descriptionJson =
        updatedReport.getDescription() == null
            ? JsonData.of(JsonValue.NULL)
            : JsonData.of(updatedReport.getDescription());
    updateParams.put(DESCRIPTION, descriptionJson);

    final Script updateScript =
        OpenSearchWriterUtil.createDefaultScriptWithPrimitiveParams(
            DatabaseWriterUtil.createUpdateFieldsScript(updateParams.keySet()), updateParams);

    final UpdateRequest.Builder request =
        new UpdateRequest.Builder()
            .index(indexName)
            .id(updatedReport.getId())
            .script(updateScript)
            .refresh(Refresh.True)
            .retryOnConflict(NUMBER_OF_RETRIES_ON_CONFLICT);

    String errorMessage =
        String.format(
            "Was not able to update report with id [%s] and name [%s]",
            updatedReport.getId(), updatedReport.getName());

    final UpdateResponse<ReportDefinitionUpdateDto> updateResponse =
        osClient.update(request, errorMessage);
    if (updateResponse.shards().failed().intValue() > 0) {
      log.error(
          "Was not able to update report with id [{}] and name [{}].",
          updatedReport.getId(),
          updatedReport.getName());
      throw new OptimizeRuntimeException("Was not able to update collection!");
    }
  }
}