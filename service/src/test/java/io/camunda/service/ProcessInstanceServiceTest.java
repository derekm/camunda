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

import io.camunda.search.clients.ProcessInstanceSearchClient;
import io.camunda.service.entities.ProcessInstanceEntity;
import io.camunda.service.search.query.ProcessInstanceQuery;
import io.camunda.service.search.query.SearchQueryBuilders;
import io.camunda.service.search.query.SearchQueryResult;
import io.camunda.zeebe.broker.client.api.BrokerClient;
import io.camunda.zeebe.util.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public final class ProcessInstanceServiceTest {

  private ProcessInstanceServices services;
  private ProcessInstanceSearchClient client;

  @BeforeEach
  public void before() {
    client = mock(ProcessInstanceSearchClient.class);
    services = new ProcessInstanceServices(mock(BrokerClient.class), client, null);
  }

  @Test
  public void shouldReturnProcessInstance() {
    // given
    final var result = mock(SearchQueryResult.class);
    when(client.searchProcessInstances(any(), any())).thenReturn(Either.right(result));

    final ProcessInstanceQuery searchQuery =
        SearchQueryBuilders.processInstanceSearchQuery().build();

    // when
    final SearchQueryResult<ProcessInstanceEntity> searchQueryResult = services.search(searchQuery);

    // then
    assertThat(searchQueryResult).isEqualTo(result);
  }
}
