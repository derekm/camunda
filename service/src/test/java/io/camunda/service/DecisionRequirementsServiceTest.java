/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.camunda.search.clients.DecisionRequirementSearchClient;
import io.camunda.service.entities.DecisionRequirementsEntity;
import io.camunda.service.search.query.DecisionRequirementsQuery;
import io.camunda.service.search.query.SearchQueryBuilders;
import io.camunda.service.search.query.SearchQueryResult;
import io.camunda.zeebe.broker.client.api.BrokerClient;
import io.camunda.zeebe.util.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public final class DecisionRequirementsServiceTest {

  private DecisionRequirementsServices services;
  private DecisionRequirementSearchClient client;

  @BeforeEach
  public void before() {
    client = mock(DecisionRequirementSearchClient.class);
    services = new DecisionRequirementsServices(mock(BrokerClient.class), client, null);
  }

  @Test
  public void shouldReturnDecisionRequirements() {
    // given
    final DecisionRequirementsQuery searchQuery =
        SearchQueryBuilders.decisionRequirementsSearchQuery().build();

    // when
    var result = mock(SearchQueryResult.class);
    when(client.searchDecisionRequirements(any(), any())).thenReturn(Either.right(result));
    final SearchQueryResult<DecisionRequirementsEntity> searchQueryResult =
        services.search(searchQuery);

    // then
    assertThat(result).isEqualTo(searchQueryResult);
  }
}
