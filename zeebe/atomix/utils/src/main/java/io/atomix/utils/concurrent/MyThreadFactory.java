/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.atomix.utils.concurrent;

import java.util.concurrent.ThreadFactory;

public class MyThreadFactory extends AtomixThreadFactory implements ThreadFactory {
  @Override
  public Thread newThread(final Runnable r) {
    //    MDC.put("actor-scheduler", "Factory-Broker-1231");
    return new AtomixThread(r);
  }
}
