/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it;

import io.camunda.qa.util.cluster.TestClient;
import io.camunda.qa.util.cluster.TestStandaloneCamunda;
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration;
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration.TestZeebe;

@ZeebeIntegration
public class StandaloneCamundaTest extends AbstractStandaloneCamundaTest {

  @TestZeebe final TestStandaloneCamunda testStandaloneCamunda = new TestStandaloneCamunda();

  @Override
  TestStandaloneCamunda getTestStandaloneCamunda() {
    return testStandaloneCamunda;
  }

  @Override
  TestClient getTestClient() {
    return testStandaloneCamunda.newOperateClient();
  }
}
