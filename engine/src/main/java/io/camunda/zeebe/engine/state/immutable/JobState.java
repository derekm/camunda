/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.state.immutable;

import io.camunda.zeebe.protocol.impl.record.value.job.JobRecord;
import io.camunda.zeebe.protocol.record.RecordValueWithTenant;
import io.camunda.zeebe.util.buffer.BufferUtil;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import org.agrona.DirectBuffer;

public interface JobState {

  void forEachTimedOutEntry(long upperBound, BiFunction<Long, JobRecord, Boolean> callback);

  boolean exists(long jobKey);

  State getState(DirectBuffer tenantId, long key);

  default boolean isInState(final long key, final State state) {
    return isInState(BufferUtil.wrapString(RecordValueWithTenant.DEFAULT_TENANT_ID), key, state);
  }

  boolean isInState(DirectBuffer tenantId, long key, State state);

  default void forEachActivatableJobs(
      final DirectBuffer type, final BiFunction<Long, JobRecord, Boolean> callback) {
    forEachActivatableJobs(
        type, BufferUtil.wrapString(RecordValueWithTenant.DEFAULT_TENANT_ID), callback);
  }

  void forEachActivatableJobs(
      DirectBuffer type, DirectBuffer tenantId, BiFunction<Long, JobRecord, Boolean> callback);

  JobRecord getJob(long key);

  void setJobsAvailableCallback(Consumer<String> callback);

  long findBackedOffJobs(final long timestamp, final BiPredicate<Long, JobRecord> callback);

  enum State {
    ACTIVATABLE((byte) 0),
    ACTIVATED((byte) 1),
    FAILED((byte) 2),
    NOT_FOUND((byte) 3),
    ERROR_THROWN((byte) 4);

    byte value;

    State(final byte value) {
      this.value = value;
    }
  }
}
