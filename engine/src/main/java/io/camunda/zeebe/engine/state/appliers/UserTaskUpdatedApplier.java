/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.state.appliers;

import io.camunda.zeebe.engine.state.TypedEventApplier;
import io.camunda.zeebe.engine.state.immutable.UserTaskState.LifecycleState;
import io.camunda.zeebe.engine.state.mutable.MutableProcessingState;
import io.camunda.zeebe.engine.state.mutable.MutableUserTaskState;
import io.camunda.zeebe.protocol.impl.record.value.usertask.UserTaskRecord;
import io.camunda.zeebe.protocol.record.intent.UserTaskIntent;

public final class UserTaskUpdatedApplier
    implements TypedEventApplier<UserTaskIntent, UserTaskRecord> {

  private final MutableUserTaskState userTaskState;

  public UserTaskUpdatedApplier(final MutableProcessingState processingState) {
    userTaskState = processingState.getUserTaskState();
  }

  @Override
  public void applyState(final long key, final UserTaskRecord value) {
    // prepare the changeset
    final UserTaskRecord updateRecord = new UserTaskRecord();
    updateRecord.wrapChangedAttributes(value, true);
    // update the changed attributes
    final UserTaskRecord userTask = userTaskState.getUserTask(key);
    userTask.wrapChangedAttributes(updateRecord, false);
    userTaskState.update(userTask);
    userTaskState.updateUserTaskLifecycleState(key, LifecycleState.CREATED);
  }
}
