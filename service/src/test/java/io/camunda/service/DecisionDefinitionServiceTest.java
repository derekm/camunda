/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.camunda.search.clients.DecisionDefinitionSearchClient;
import io.camunda.search.clients.DecisionRequirementSearchClient;
import io.camunda.service.entities.DecisionDefinitionEntity;
import io.camunda.service.entities.DecisionRequirementsEntity;
import io.camunda.service.exception.NotFoundException;
import io.camunda.service.search.query.DecisionDefinitionQuery;
import io.camunda.service.search.query.DecisionRequirementsQuery;
import io.camunda.service.search.query.SearchQueryBuilders;
import io.camunda.service.search.query.SearchQueryResult;
import io.camunda.zeebe.broker.client.api.BrokerClient;
import io.camunda.zeebe.util.Either;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public final class DecisionDefinitionServiceTest {

  private DecisionDefinitionServices services;
  private DecisionDefinitionSearchClient decisionDefinitionSearchClient;
  private DecisionRequirementSearchClient decisionRequirementSearchClient;

  @BeforeEach
  public void before() {
    decisionDefinitionSearchClient = mock(DecisionDefinitionSearchClient.class);
    decisionRequirementSearchClient = mock(DecisionRequirementSearchClient.class);
    services =
        new DecisionDefinitionServices(
            mock(BrokerClient.class),
            decisionDefinitionSearchClient,
            decisionRequirementSearchClient,
            null);
  }

  @Test
  public void shouldReturnDecisionDefinition() {
    // given
    final var result = mock(SearchQueryResult.class);
    when(decisionDefinitionSearchClient.searchDecisionDefinitions(any(), any()))
        .thenReturn(Either.right(result));

    final DecisionDefinitionQuery searchQuery =
        SearchQueryBuilders.decisionDefinitionSearchQuery().build();

    // when
    final SearchQueryResult<DecisionDefinitionEntity> searchQueryResult =
        services.search(searchQuery);

    // then
    assertThat(searchQueryResult).isEqualTo(result);
  }

  @Test
  public void shouldReturnDecisionDefinitionXml() {
    // given
    final var definitionEntity = mock(DecisionDefinitionEntity.class);
    when(definitionEntity.decisionRequirementsKey()).thenReturn(42L);
    final var definitionResult = mock(SearchQueryResult.class);
    when(definitionResult.items()).thenReturn(List.of(definitionEntity));
    when(decisionDefinitionSearchClient.searchDecisionDefinitions(any(), any()))
        .thenReturn(Either.right(definitionResult));

    final var requirementEntity = mock(DecisionRequirementsEntity.class);
    when(requirementEntity.xml()).thenReturn("<foo>bar</foo>");
    final var requirementResult = mock(SearchQueryResult.class);
    when(requirementResult.items()).thenReturn(List.of(requirementEntity));
    when(decisionRequirementSearchClient.searchDecisionRequirements(any(), any()))
        .thenReturn(Either.right(requirementResult));

    // when
    final var xml = services.getDecisionDefinitionXml(42L);

    // then
    assertThat(xml).isEqualTo("<foo>bar</foo>");
  }

  @Test
  public void shouldThorwNotFoundExceptionOnUnmatchedDecisionKey() {
    // given
    when(decisionDefinitionSearchClient.searchDecisionDefinitions(any(), any()))
        .thenReturn(Either.right(new SearchQueryResult<>(0, List.of(), new Object[] {})));

    // then
    final var exception =
        assertThrows(NotFoundException.class, () -> services.getDecisionDefinitionXml(1L));
    assertThat(exception.getMessage())
        .isEqualTo("DecisionDefinition with decisionKey=1 cannot be found");
    verify(decisionDefinitionSearchClient)
        .searchDecisionDefinitions(any(DecisionDefinitionQuery.class), any());
    verify(decisionRequirementSearchClient, never())
        .searchDecisionRequirements(any(DecisionRequirementsQuery.class), any());
  }

  @Test
  public void shouldThorwNotFoundExceptionOnUnmatchedDecisionRequirementsKey() {
    // given
    final var definitionEntity = mock(DecisionDefinitionEntity.class);
    when(definitionEntity.decisionRequirementsKey()).thenReturn(1L);
    final var definitionResult = mock(SearchQueryResult.class);
    when(definitionResult.items()).thenReturn(List.of(definitionEntity));
    when(decisionDefinitionSearchClient.searchDecisionDefinitions(any(), any()))
        .thenReturn(Either.right(definitionResult));
    when(decisionRequirementSearchClient.searchDecisionRequirements(any(), any()))
        .thenReturn(Either.right(new SearchQueryResult<>(0, List.of(), new Object[] {})));

    // then
    final var exception =
        assertThrows(NotFoundException.class, () -> services.getDecisionDefinitionXml(1L));
    assertThat(exception.getMessage())
        .isEqualTo("DecisionRequirements with decisionRequirementsKey=1 cannot be found");
  }
}
