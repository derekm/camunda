/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.rest.providers;

import org.camunda.optimize.dto.optimize.importing.DecisionDefinitionOptimizeDto;
import org.camunda.optimize.service.security.util.LocalDateUtil;
import org.camunda.optimize.test.it.extension.ElasticSearchIntegrationTestExtensionRule;
import org.camunda.optimize.test.it.extension.EmbeddedOptimizeExtensionRule;
import org.camunda.optimize.test.it.extension.EngineIntegrationExtensionRule;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.time.OffsetDateTime;

import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.DECISION_DEFINITION_INDEX_NAME;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class CacheRequestIT {

  @RegisterExtension
  @Order(1)
  public ElasticSearchIntegrationTestExtensionRule elasticSearchIntegrationTestExtensionRule = new ElasticSearchIntegrationTestExtensionRule();
  @RegisterExtension
  @Order(2)
  public EngineIntegrationExtensionRule engineIntegrationExtensionRule = new EngineIntegrationExtensionRule();
  @RegisterExtension
  @Order(3)
  public EmbeddedOptimizeExtensionRule embeddedOptimizeExtensionRule = new EmbeddedOptimizeExtensionRule();

  @Test
  public void getDecisionDefinitionXmlRequest_cacheControlHeadersAreSetCorrectly() {
    // given
    OffsetDateTime now = OffsetDateTime.parse("2019-04-23T18:00:00+01:00");
    LocalDateUtil.setCurrentTime(now);

    String key = "test", version = "1";
    DecisionDefinitionOptimizeDto expectedDefinitionDto = createDecisionDefinitionDto(key, version);
    elasticSearchIntegrationTestExtensionRule.addEntryToElasticsearch(
      DECISION_DEFINITION_INDEX_NAME,
      expectedDefinitionDto.getId(),
      expectedDefinitionDto
    );

    // when
    Response response =
      embeddedOptimizeExtensionRule
        .getRequestExecutor()
        .buildGetDecisionDefinitionXmlRequest(key, version)
        .execute();

    // then
    final MultivaluedMap<String, Object> headers = response.getHeaders();

    assertThat(headers, is(notNullValue()));
    assertThat((String) headers.getFirst(HttpHeaders.CACHE_CONTROL), containsString("max-age=21600"));
    assertThat(headers.get(HttpHeaders.CACHE_CONTROL).size(), is(1));
  }


  private DecisionDefinitionOptimizeDto createDecisionDefinitionDto(String key, String version) {
    DecisionDefinitionOptimizeDto decisionDefinitionDto = new DecisionDefinitionOptimizeDto();
    decisionDefinitionDto.setDmn10Xml("DecisionModelXml");
    decisionDefinitionDto.setKey(key);
    decisionDefinitionDto.setVersion(version);
    decisionDefinitionDto.setId("id-" + key + "-version-" + version);
    elasticSearchIntegrationTestExtensionRule.addEntryToElasticsearch(
      DECISION_DEFINITION_INDEX_NAME, decisionDefinitionDto.getId(), decisionDefinitionDto
    );
    return decisionDefinitionDto;
  }
}