/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it;

import io.camunda.exporter.rdbms.RdbmsExporter;
import io.camunda.qa.util.cluster.TestClient;
import io.camunda.qa.util.cluster.TestStandaloneCamunda;
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration;
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration.TestZeebe;

@ZeebeIntegration
public class StandaloneCamundaWithRdbmsH2Test extends AbstractStandaloneCamundaTest {

  @TestZeebe
  final TestStandaloneCamunda testStandaloneCamunda = TestStandaloneCamunda
      .withRdbms()
      .withProperty("camunda.database.type", "rdbms")
      .withProperty("spring.datasource.url", "jdbc:h2:mem:;MODE=PostgreSQL")
      .withProperty("spring.datasource.username", "sa")
      .withProperty("spring.datasource.password", null)
      .withProperty("spring.datasource.driverClassName", "org.h2.Driver")
      .withProperty("mybatis.mapper-locations", "classpath:mapper/**/*-mapper.xml")
      .withExporter("rdbms", cfg -> cfg.setClassName(RdbmsExporter.class.getName()));

  @Override
  TestStandaloneCamunda getTestStandaloneCamunda() {
    return testStandaloneCamunda;
  }

  @Override
  TestClient getTestClient() {
    return testStandaloneCamunda.newRestV2ApiClient();
  }
}
