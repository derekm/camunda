/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.service;

import io.camunda.search.clients.DecisionRequirementSearchClient;
import io.camunda.search.entities.DecisionRequirementsEntity;
import io.camunda.search.query.DecisionRequirementsQuery;
import io.camunda.search.query.SearchQueryBuilders;
import io.camunda.search.query.SearchQueryResult;
import io.camunda.search.security.auth.Authentication;
import io.camunda.service.search.core.SearchQueryService;
import io.camunda.util.ObjectBuilder;
import io.camunda.zeebe.broker.client.api.BrokerClient;
import java.util.function.Function;

public final class DecisionRequirementsServices
    extends SearchQueryService<
        DecisionRequirementsServices, DecisionRequirementsQuery, DecisionRequirementsEntity> {

  private final DecisionRequirementSearchClient decisionRequirementSearchClient;

  public DecisionRequirementsServices(
      final BrokerClient brokerClient,
      final DecisionRequirementSearchClient decisionRequirementSearchClient,
      final Authentication authentication) {
    super(brokerClient, authentication);
    this.decisionRequirementSearchClient = decisionRequirementSearchClient;
  }

  @Override
  public DecisionRequirementsServices withAuthentication(final Authentication authentication) {
    return new DecisionRequirementsServices(
        brokerClient, decisionRequirementSearchClient, authentication);
  }

  @Override
  public SearchQueryResult<DecisionRequirementsEntity> search(
      final DecisionRequirementsQuery query) {
    return decisionRequirementSearchClient.searchDecisionRequirements(query, authentication);
  }

  public SearchQueryResult<DecisionRequirementsEntity> search(
      final Function<DecisionRequirementsQuery.Builder, ObjectBuilder<DecisionRequirementsQuery>>
          fn) {
    return search(SearchQueryBuilders.decisionRequirementsSearchQuery(fn));
  }
}
