/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.atomix.cluster;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.atomix.utils.concurrent.Threads.namedThreads;

import io.atomix.cluster.impl.DefaultClusterSingleThreadContextFactory;
import io.atomix.utils.concurrent.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.MDC;

public class ClusterMemberContext implements AutoCloseable {

  protected final ThreadContext threadContext;
  private final String memberType; // Broker or Gateway
  private final MemberId memberId;
  private final Logger log;

  public ClusterMemberContext(final String memberType, final MemberId memberId, final Logger log) {
    this.memberType = checkNotNull(memberType, "memberType cannot be null");
    this.memberId = checkNotNull(memberId, "memberId cannot be null");
    this.log = log;

    threadContext =
        createThreadContext(
            new DefaultClusterSingleThreadContextFactory(), this.memberType, this.memberId.id());
  }

  /** Creates thread context. */
  private ThreadContext createThreadContext(
      final ClusterMemberContextFactory threadContextFactory,
      final String memberType,
      final String localMemberId) {
    final var context =
        threadContextFactory.createContext(
            namedThreads("%s-%s".formatted(memberType, localMemberId), log),
            this::notifyFailureListeners);
    context.execute(
        () -> {
          MDC.put("actor-scheduler", memberType + "-" + localMemberId);
        });
    return context;
  }

  private void notifyFailureListeners(final Throwable error) {
    log.error("An uncaught exception occurred", error);
  }

  /** Checks that the current thread is the state context thread. */
  public void checkThread() {
    threadContext.checkThread();
  }

  @Override
  public void close() throws Exception {
    threadContext.close();
  }

  public void execute(final Runnable o) {
    threadContext.execute(o);
  }
}
