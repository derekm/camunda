/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.upgrade.exception;

public class UpgradeRuntimeException extends RuntimeException {
  public UpgradeRuntimeException(String message) {
    super(message);
  }

  public UpgradeRuntimeException(Exception e) {
    super(e);
  }

  public UpgradeRuntimeException(String message, Exception e) {
    super(message, e);
  }
}