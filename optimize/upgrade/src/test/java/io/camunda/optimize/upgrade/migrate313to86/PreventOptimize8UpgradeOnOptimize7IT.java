/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.upgrade.migrate313to86;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.camunda.optimize.upgrade.exception.UpgradeRuntimeException;
import org.junit.jupiter.api.Test;

public class PreventOptimize8UpgradeOnOptimize7IT extends AbstractUpgrade86IT {

  @Test
  public void failUpgradeIfC7DataIsPresent() {
    // given
    executeBulk("steps/3.13/313-license-data.json");

    // when / then
    assertThatThrownBy(this::performUpgrade)
        .isInstanceOf(UpgradeRuntimeException.class)
        .hasMessage(
            "Detected Camunda 7 Optimize data in Elasticsearch. The upgrade to Optimize 8.6 is only applicable to "
                + "Optimize instances running with Camunda 8. For Camunda 7 Optimize, please apply the upgrade to "
                + "Optimize 3.14 instead.");
  }

  @Test
  public void doNotFailUpgradeWhenNoC7DataIsPresent() {
    // given no license data
    // when /then
    assertThatCode(this::performUpgrade).doesNotThrowAnyException();
  }
}
