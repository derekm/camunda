/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.search.rdbms;

import io.camunda.db.rdbms.RdbmsService;
import io.camunda.db.rdbms.domain.ProcessDefinitionModel;
import io.camunda.db.rdbms.domain.ProcessInstanceFilter;
import io.camunda.db.rdbms.domain.ProcessInstanceFilter.VariableFilter;
import io.camunda.search.clients.CamundaSearchClient;
import io.camunda.search.clients.core.SearchQueryRequest;
import io.camunda.search.clients.core.SearchQueryResponse;
import io.camunda.service.entities.AuthorizationEntity;
import io.camunda.service.entities.DecisionDefinitionEntity;
import io.camunda.service.entities.DecisionRequirementsEntity;
import io.camunda.service.entities.IncidentEntity;
import io.camunda.service.entities.ProcessInstanceEntity;
import io.camunda.service.entities.UserEntity;
import io.camunda.service.entities.UserTaskEntity;
import io.camunda.service.entities.VariableEntity;
import io.camunda.service.search.query.AuthorizationQuery;
import io.camunda.service.search.query.DecisionDefinitionQuery;
import io.camunda.service.search.query.DecisionRequirementsQuery;
import io.camunda.service.search.query.IncidentQuery;
import io.camunda.service.search.query.ProcessInstanceQuery;
import io.camunda.service.search.query.SearchQueryResult;
import io.camunda.service.search.query.UserQuery;
import io.camunda.service.search.query.UserTaskQuery;
import io.camunda.service.search.query.VariableQuery;
import io.camunda.service.security.auth.Authentication;
import io.camunda.zeebe.util.Either;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RdbmsSearchClient implements CamundaSearchClient {

  private static final Logger LOG = LoggerFactory.getLogger(RdbmsSearchClient.class);

  private final RdbmsService rdbmsService;

  public RdbmsSearchClient(final RdbmsService rdbmsService) {
    this.rdbmsService = rdbmsService;
  }

  @Override
  public Either<Exception, SearchQueryResult<AuthorizationEntity>> searchAuthorizations(final AuthorizationQuery filter, final Authentication authentication) {
    return null;
  }

  @Override
  public Either<Exception, SearchQueryResult<DecisionDefinitionEntity>> searchDecisionDefinitions(final DecisionDefinitionQuery filter, final Authentication authentication) {
    return null;
  }

  @Override
  public Either<Exception, SearchQueryResult<DecisionRequirementsEntity>> searchDecisionRequirements(final DecisionRequirementsQuery filter, final Authentication authentication) {
    return null;
  }

  @Override
  public Either<Exception, SearchQueryResult<IncidentEntity>> searchIncidents(final IncidentQuery filter, final Authentication authentication) {
    return null;
  }

  @Override
  public Either<Exception, SearchQueryResult<ProcessInstanceEntity>> searchProcessInstances(final ProcessInstanceQuery query, final Authentication authentication) {
    LOG.debug("[RDBMS Search Client] Search for processInstance: {}", query);

    final var searchResult = rdbmsService.getProcessInstanceRdbmsService().search(new ProcessInstanceFilter(
        (query.filter().bpmnProcessIds().isEmpty()) ? null : query.filter().bpmnProcessIds().getFirst(),
        query.filter().variable() != null ? new VariableFilter(query.filter().variable().name(), query.filter().variable().values()) : null,
        query.page().size(),
        query.page().from()
    ));

    return Either.right(new SearchQueryResult<>(searchResult.total(),
        searchResult.hits().stream().map(pi ->
            new ProcessInstanceEntity(
                pi.processInstanceKey(),
                rdbmsService.getProcessDeploymentRdbmsService().findOne(pi.processDefinitionKey(), pi.version()).map(ProcessDefinitionModel::name).orElse(pi.bpmnProcessId()),
                pi.version(), pi.bpmnProcessId(),
                pi.parentProcessInstanceKey(), pi.parentElementInstanceKey(), pi.startDate().toString(),
                Optional.ofNullable(pi.endDate()).map(OffsetDateTime::toString).orElse(null), pi.state().name(), null,
                null, pi.processDefinitionKey(), pi.tenantId(),
                null, null, null
            )
        ).toList(),
        null
    ));
  }

  @Override
  public Either<Exception, SearchQueryResult<UserEntity>> searchUsers(final UserQuery filter, final Authentication authentication) {
    return null;
  }

  @Override
  public Either<Exception, SearchQueryResult<UserTaskEntity>> searchUserTasks(final UserTaskQuery filter, final Authentication authentication) {
    return null;
  }

  @Override
  public Either<Exception, SearchQueryResult<VariableEntity>> searchVariables(final VariableQuery filter, final Authentication authentication) {
    return null;
  }

  @Override
  public void close() throws Exception {
  }
}
