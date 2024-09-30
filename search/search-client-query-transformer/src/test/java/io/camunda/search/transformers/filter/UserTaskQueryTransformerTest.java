/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.search.transformers.filter;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.search.clients.query.SearchBoolQuery;
import io.camunda.search.clients.query.SearchExistsQuery;
import io.camunda.search.clients.query.SearchHasChildQuery;
import io.camunda.search.clients.query.SearchQuery;
import io.camunda.search.clients.query.SearchQueryOption;
import io.camunda.search.clients.query.SearchTermQuery;
import io.camunda.search.filter.FilterBuilders;
import io.camunda.search.filter.UserTaskFilter;
import io.camunda.search.filter.UserTaskFilter.Builder;
import io.camunda.search.filter.VariableValueFilter;
import io.camunda.search.query.SearchQueryBuilders;
import java.util.List;
import org.junit.jupiter.api.Test;

public class UserTaskQueryTransformerTest extends AbstractTransformerTest {

  @Test
  public void shouldQueryOnlyByUserTasks() {
    // given
    final UserTaskFilter filter = new Builder().build();

    // when
    final var searchRequest = transformQuery(filter);

    // then
    final SearchQueryOption queryVariant = searchRequest.queryOption();
    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchExistsQuery.class,
            (t) -> {
              assertThat(t.field()).isEqualTo("flowNodeInstanceId"); // Retrieve only User Task
            });
  }

  @Test
  public void shouldQueryByUserTaskKey() {
    // given
    final var filter = FilterBuilders.userTask((f) -> f.userTaskKeys(4503599627370497L));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    final var queryVariant = searchRequest.queryOption();

    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (t) -> {
              assertThat(t.must().get(0).queryOption())
                  .isInstanceOfSatisfying(
                      SearchTermQuery.class,
                      (term) -> {
                        assertThat(term.field()).isEqualTo("key");
                        assertThat(term.value().longValue()).isEqualTo(4503599627370497L);
                      });
            });
  }

  @Test
  public void shouldQueryByTaskState() {
    // given
    final var filter = FilterBuilders.userTask((f) -> f.states("CREATED"));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    final var queryVariant = searchRequest.queryOption();
    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (t) -> {
              assertThat(t.must().get(0).queryOption())
                  .isInstanceOfSatisfying(
                      SearchTermQuery.class,
                      (term) -> {
                        assertThat(term.field()).isEqualTo("state");
                        assertThat(term.value().stringValue()).isEqualTo("CREATED");
                      });
            });
  }

  @Test
  public void shouldQueryByAssignee() {
    // given
    final var filter = FilterBuilders.userTask((f) -> f.assignees("assignee1"));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    final var queryVariant = searchRequest.queryOption();
    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (t) -> {
              assertThat(t.must().get(0).queryOption())
                  .isInstanceOfSatisfying(
                      SearchTermQuery.class,
                      (term) -> {
                        assertThat(term.field()).isEqualTo("assignee");
                        assertThat(term.value().stringValue()).isEqualTo("assignee1");
                      });
            });
  }

  @Test
  public void shouldQueryByTenantId() {
    // given
    final var filter = FilterBuilders.userTask((f) -> f.tenantIds("tenant1"));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    final var queryVariant = searchRequest.queryOption();

    searchRequest.queryOption();
    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (t) -> {
              assertThat(t.must().get(0).queryOption())
                  .isInstanceOfSatisfying(
                      SearchTermQuery.class,
                      (term) -> {
                        assertThat(term.field()).isEqualTo("tenantId");
                        assertThat(term.value().stringValue()).isEqualTo("tenant1");
                      });
            });
  }

  @Test
  public void shouldQueryByProcessInstanceKey() {
    // given
    final var filter = FilterBuilders.userTask((f) -> f.processInstanceKeys(12345L));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    final var queryVariant = searchRequest.queryOption();

    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (t) -> {
              assertThat(t.must().get(0).queryOption())
                  .isInstanceOfSatisfying(
                      SearchTermQuery.class,
                      (term) -> {
                        assertThat(term.field()).isEqualTo("processInstanceId");
                        assertThat(term.value().longValue()).isEqualTo(12345L);
                      });
            });
  }

  @Test
  public void shouldQueryByProcessDefinitionKey() {
    // given
    final var filter = FilterBuilders.userTask((f) -> f.processDefinitionKeys(123L));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    final var queryVariant = searchRequest.queryOption();

    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (t) -> {
              assertThat(t.must().get(0).queryOption())
                  .isInstanceOfSatisfying(
                      SearchTermQuery.class,
                      (term) -> {
                        assertThat(term.field()).isEqualTo("processDefinitionId");
                        assertThat(term.value().longValue()).isEqualTo(123L);
                      });
            });
  }

  @Test
  public void shouldQueryByBpmnProcessId() {
    // given
    final var filter = FilterBuilders.userTask((f) -> f.bpmnProcessIds("bpmnProcess1"));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    final var queryVariant = searchRequest.queryOption();

    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (t) -> {
              assertThat(t.must().get(0).queryOption())
                  .isInstanceOfSatisfying(
                      SearchTermQuery.class,
                      (term) -> {
                        assertThat(term.field()).isEqualTo("bpmnProcessId");
                        assertThat(term.value().stringValue()).isEqualTo("bpmnProcess1");
                      });
            });
  }

  @Test
  public void shouldQueryByCandidateUsers() {
    // given
    final var filter = FilterBuilders.userTask((f) -> f.candidateUsers("candidateUser1"));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    final var queryVariant = searchRequest.queryOption();

    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (t) -> {
              assertThat(t.must().get(0).queryOption())
                  .isInstanceOfSatisfying(
                      SearchTermQuery.class,
                      (term) -> {
                        assertThat(term.field()).isEqualTo("candidateUsers");
                        assertThat(term.value().stringValue()).isEqualTo("candidateUser1");
                      });
            });
  }

  @Test
  public void shouldQueryByCandidateGroups() {
    // given
    final var filter = FilterBuilders.userTask((f) -> f.candidateGroups("candidateGroup1"));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    final var queryVariant = searchRequest.queryOption();

    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (t) -> {
              assertThat(t.must().get(0).queryOption())
                  .isInstanceOfSatisfying(
                      SearchTermQuery.class,
                      (term) -> {
                        assertThat(term.field()).isEqualTo("candidateGroups");
                        assertThat(term.value().stringValue()).isEqualTo("candidateGroup1");
                      });
            });
  }

  @Test
  public void shouldQueryByVariableValueFilter() {
    // given
    final VariableValueFilter.Builder variableValueFilterBuilder =
        new VariableValueFilter.Builder();
    variableValueFilterBuilder.name("test").eq("test").build();

    final VariableValueFilter variableFilterValue = variableValueFilterBuilder.build();

    final var filter = FilterBuilders.userTask((f) -> f.variable(List.of(variableFilterValue)));
    final var searchQuery = SearchQueryBuilders.userTaskSearchQuery((b) -> b.filter(filter));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    final var queryVariant = searchRequest.queryOption();

    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            outerBoolQuery -> {
              assertThat(outerBoolQuery.must()).isNotEmpty();

              final SearchQuery outerMustQuery = outerBoolQuery.must().get(0);
              assertThat(outerMustQuery.queryOption()).isInstanceOf(SearchBoolQuery.class);

              // Drill down into the nested SearchBoolQuery
              final SearchBoolQuery nestedBoolQuery =
                  (SearchBoolQuery) outerMustQuery.queryOption();
              assertThat(nestedBoolQuery.should()).isNotEmpty();

              final SearchQuery shouldQuery = nestedBoolQuery.should().get(0);
              assertThat(shouldQuery.queryOption()).isInstanceOf(SearchHasChildQuery.class);

              final SearchHasChildQuery childQuery =
                  (SearchHasChildQuery) shouldQuery.queryOption();
              assertThat(childQuery.type()).isEqualTo("taskVariable");

              // Check the inner bool query inside the child query
              final SearchQuery innerQuery = childQuery.query();
              assertThat(innerQuery.queryOption()).isInstanceOf(SearchBoolQuery.class);

              final SearchBoolQuery innerBoolQuery = (SearchBoolQuery) innerQuery.queryOption();
              assertThat(innerBoolQuery.must()).hasSize(2);

              assertThat(innerBoolQuery.must().get(0).queryOption())
                  .isInstanceOfSatisfying(
                      SearchTermQuery.class,
                      termQuery -> {
                        assertThat(termQuery.field()).isEqualTo("name");
                        assertThat(termQuery.value().value()).isEqualTo("test");
                      });

              assertThat(innerBoolQuery.must().get(1).queryOption())
                  .isInstanceOfSatisfying(
                      SearchTermQuery.class,
                      termQuery -> {
                        assertThat(termQuery.field()).isEqualTo("value");
                        assertThat(termQuery.value().value()).isEqualTo("test");
                      });
            });
  }
}