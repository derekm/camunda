/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.zeebe.client.process;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.client.protocol.rest.*;
import io.camunda.zeebe.client.util.ClientRestTest;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SearchProcessInstanceTest extends ClientRestTest {

  @Test
  public void shouldSearchProcessInstanceWithEmptyQuery() {
    // when
    client.newProcessInstanceQuery().send().join();

    // then
    final ProcessInstanceSearchQueryRequest request =
        gatewayService.getLastRequest(ProcessInstanceSearchQueryRequest.class);
    assertThat(request.getFilter()).isNull();
  }

  @Test
  public void shouldSearchProcessInstanceWithFullFilters() {
    // when
    client
        .newProcessInstanceQuery()
        .filter(
            f ->
                f.key(123L)
                    .bpmnProcessId("bpmnProcessId")
                    .processName("Demo process")
                    .processVersion(7)
                    .processVersionTag("v7")
                    .processDefinitionKey(15L)
                    .rootProcessInstanceKey(20L)
                    .parentProcessInstanceKey(25L)
                    .parentFlowNodeInstanceKey(30L)
                    .treePath("PI_1")
                    .startDate("startDate")
                    .endDate("endDate")
                    .state("ACTIVE")
                    .incident(true)
                    .tenantId("tenant"))
        .send()
        .join();
    // then
    final ProcessInstanceSearchQueryRequest request =
        gatewayService.getLastRequest(ProcessInstanceSearchQueryRequest.class);
    final ProcessInstanceFilterRequest filter = request.getFilter();
    assertThat(filter).isNotNull();
    assertThat(filter.getKey()).isEqualTo(123L);
    assertThat(filter.getBpmnProcessId()).isEqualTo("bpmnProcessId");
    assertThat(filter.getProcessName()).isEqualTo("Demo process");
    assertThat(filter.getProcessVersion()).isEqualTo(7);
    assertThat(filter.getProcessVersionTag()).isEqualTo("v7");
    assertThat(filter.getProcessDefinitionKey()).isEqualTo(15L);
    assertThat(filter.getRootProcessInstanceKey()).isEqualTo(20L);
    assertThat(filter.getParentProcessInstanceKey()).isEqualTo(25L);
    assertThat(filter.getParentFlowNodeInstanceKey()).isEqualTo(30L);
    assertThat(filter.getTreePath()).isEqualTo("PI_1");
    assertThat(filter.getStartDate()).isEqualTo("startDate");
    assertThat(filter.getEndDate()).isEqualTo("endDate");
    assertThat(filter.getState()).isEqualTo(ProcessInstanceStateEnum.ACTIVE);
    assertThat(filter.getIncident()).isEqualTo(true);
    assertThat(filter.getTenantId()).isEqualTo("tenant");
  }

  @Test
  void shouldSearchProcessInstanceWithFullSorting() {
    // when
    client
        .newProcessInstanceQuery()
        .sort(
            s ->
                s.key()
                    .asc()
                    .bpmnProcessId()
                    .desc()
                    .processName()
                    .asc()
                    .processVersion()
                    .asc()
                    .processVersionTag()
                    .desc()
                    .processDefinitionKey()
                    .desc()
                    .rootProcessInstanceKey()
                    .asc()
                    .parentProcessInstanceKey()
                    .asc()
                    .parentFlowNodeInstanceKey()
                    .asc()
                    .treePath()
                    .desc()
                    .startDate()
                    .asc()
                    .endDate()
                    .asc()
                    .state()
                    .asc()
                    .incident()
                    .desc()
                    .tenantId()
                    .asc())
        .send()
        .join();

    // then
    final ProcessInstanceSearchQueryRequest request =
        gatewayService.getLastRequest(ProcessInstanceSearchQueryRequest.class);
    final List<SearchQuerySortRequest> sorts = request.getSort();
    assertThat(sorts).hasSize(15);
    assertSort(sorts.get(0), "key", "asc");
    assertSort(sorts.get(1), "bpmnProcessId", "desc");
    assertSort(sorts.get(2), "processName", "asc");
    assertSort(sorts.get(3), "processVersion", "asc");
    assertSort(sorts.get(4), "processVersionTag", "desc");
    assertSort(sorts.get(5), "processDefinitionKey", "desc");
    assertSort(sorts.get(6), "rootProcessInstanceKey", "asc");
    assertSort(sorts.get(7), "parentProcessInstanceKey", "asc");
    assertSort(sorts.get(8), "parentFlowNodeInstanceKey", "asc");
    assertSort(sorts.get(9), "treePath", "desc");
    assertSort(sorts.get(10), "startDate", "asc");
    assertSort(sorts.get(11), "endDate", "asc");
    assertSort(sorts.get(12), "state", "asc");
    assertSort(sorts.get(13), "incident", "desc");
    assertSort(sorts.get(14), "tenantId", "asc");
  }

  @Test
  void shouldSearchWithFullPagination() {
    // when
    client
        .newProcessInstanceQuery()
        .page(
            p ->
                p.from(23)
                    .limit(5)
                    .searchBefore(Collections.singletonList("b"))
                    .searchAfter(Collections.singletonList("a")))
        .send()
        .join();

    // then
    final ProcessInstanceSearchQueryRequest request =
        gatewayService.getLastRequest(ProcessInstanceSearchQueryRequest.class);
    final SearchQueryPageRequest pageRequest = request.getPage();
    assertThat(pageRequest).isNotNull();
    assertThat(pageRequest.getFrom()).isEqualTo(23);
    assertThat(pageRequest.getLimit()).isEqualTo(5);
    assertThat(pageRequest.getSearchBefore()).isEqualTo(Collections.singletonList("b"));
    assertThat(pageRequest.getSearchAfter()).isEqualTo(Collections.singletonList("a"));
  }

  private void assertSort(
      final SearchQuerySortRequest sort, final String name, final String order) {
    assertThat(sort.getField()).isEqualTo(name);
    assertThat(sort.getOrder()).isEqualTo(order);
  }
}
