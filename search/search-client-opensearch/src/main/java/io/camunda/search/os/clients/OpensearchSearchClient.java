/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.search.os.clients;

import io.camunda.search.DocumentCamundaSearchClient;
import io.camunda.search.SearchClientBasedQueryExecutor;
import io.camunda.search.clients.CamundaSearchClient;
import io.camunda.search.clients.core.SearchQueryRequest;
import io.camunda.search.clients.core.SearchQueryResponse;
import io.camunda.search.os.transformers.OpensearchTransformers;
import io.camunda.search.os.transformers.search.SearchResponseTransformer;
import io.camunda.search.transformers.SearchTransfomer;
import io.camunda.search.transformers.ServiceTransformers;
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
import java.io.IOException;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

public final class OpensearchSearchClient implements DocumentCamundaSearchClient, CamundaSearchClient {

  private final OpenSearchClient client;
  private final OpensearchTransformers transformers;

  public OpensearchSearchClient(final OpenSearchClient client) {
    this(client, new OpensearchTransformers());
  }

  public OpensearchSearchClient(
      final OpenSearchClient client, final OpensearchTransformers transformers) {
    this.client = client;
    this.transformers = transformers;
  }

  @Override
  public <T> Either<Exception, SearchQueryResponse<T>> search(
      final SearchQueryRequest searchRequest, final Class<T> documentClass) {
    try {
      final var requestTransformer = getSearchRequestTransformer();
      final var request = requestTransformer.apply(searchRequest);
      final SearchResponse<T> rawSearchResponse = client.search(request, documentClass);
      final SearchResponseTransformer<T> searchResponseTransformer = getSearchResponseTransformer();
      final SearchQueryResponse<T> response = searchResponseTransformer.apply(rawSearchResponse);
      return Either.right(response);
    } catch (final IOException ioe) {
      return Either.left(ioe);
    } catch (final OpenSearchException e) {
      return Either.left(e);
    }
  }
  @Override
  public Either<Exception, SearchQueryResult<AuthorizationEntity>> searchAuthorizations(final AuthorizationQuery filter, final Authentication authentication) {
    final var executor = new SearchClientBasedQueryExecutor(this, ServiceTransformers.newInstance(), authentication);
    return executor.search(filter, AuthorizationEntity.class);
  }

  @Override
  public Either<Exception, SearchQueryResult<DecisionDefinitionEntity>> searchDecisionDefinitions(final DecisionDefinitionQuery filter, final Authentication authentication) {
    final var executor = new SearchClientBasedQueryExecutor(this, ServiceTransformers.newInstance(), authentication);
    return executor.search(filter, DecisionDefinitionEntity.class);
  }

  @Override
  public Either<Exception, SearchQueryResult<DecisionRequirementsEntity>> searchDecisionRequirements(final DecisionRequirementsQuery filter, final Authentication authentication) {
    final var executor = new SearchClientBasedQueryExecutor(this, ServiceTransformers.newInstance(), authentication);
    return executor.search(filter, DecisionRequirementsEntity.class);
  }

  @Override
  public Either<Exception, SearchQueryResult<IncidentEntity>> searchIncidents(final IncidentQuery filter, final Authentication authentication) {
    final var executor = new SearchClientBasedQueryExecutor(this, ServiceTransformers.newInstance(), authentication);
    return executor.search(filter, IncidentEntity.class);
  }

  @Override
  public Either<Exception, SearchQueryResult<ProcessInstanceEntity>> searchProcessInstances(final ProcessInstanceQuery filter, final Authentication authentication) {
    final var executor = new SearchClientBasedQueryExecutor(this, ServiceTransformers.newInstance(), authentication);
    return executor.search(filter, ProcessInstanceEntity.class);
  }

  @Override
  public Either<Exception, SearchQueryResult<UserEntity>> searchUsers(final UserQuery filter, final Authentication authentication) {
    final var executor = new SearchClientBasedQueryExecutor(this, ServiceTransformers.newInstance(), authentication);
    return executor.search(filter, UserEntity.class);
  }

  @Override
  public Either<Exception, SearchQueryResult<UserTaskEntity>> searchUserTasks(final UserTaskQuery filter, final Authentication authentication) {
    final var executor = new SearchClientBasedQueryExecutor(this, ServiceTransformers.newInstance(), authentication);
    return executor.search(filter, UserTaskEntity.class);
  }

  @Override
  public Either<Exception, SearchQueryResult<VariableEntity>> searchVariables(final VariableQuery filter, final Authentication authentication) {
    final var executor = new SearchClientBasedQueryExecutor(this, ServiceTransformers.newInstance(), authentication);
    return executor.search(filter, VariableEntity.class);
  }

  private SearchTransfomer<SearchQueryRequest, SearchRequest> getSearchRequestTransformer() {
    return transformers.getTransformer(SearchQueryRequest.class);
  }

  private <T> SearchResponseTransformer<T> getSearchResponseTransformer() {
    return new SearchResponseTransformer<>(transformers);
  }

  @Override
  public void close() throws Exception {
    if (client != null) {
      try {
        client._transport().close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
