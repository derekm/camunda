/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.message;

import io.camunda.zeebe.engine.processing.bpmn.behavior.BpmnBehaviors;
import io.camunda.zeebe.engine.processing.common.EventHandle;
import io.camunda.zeebe.engine.processing.message.MessageCorrelateBehavior.MessageData;
import io.camunda.zeebe.engine.processing.message.command.SubscriptionCommandSender;
import io.camunda.zeebe.engine.processing.streamprocessor.TypedRecordProcessor;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.StateWriter;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.TypedRejectionWriter;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.TypedResponseWriter;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.Writers;
import io.camunda.zeebe.engine.state.immutable.EventScopeInstanceState;
import io.camunda.zeebe.engine.state.immutable.MessageStartEventSubscriptionState;
import io.camunda.zeebe.engine.state.immutable.MessageState;
import io.camunda.zeebe.engine.state.immutable.MessageSubscriptionState;
import io.camunda.zeebe.engine.state.immutable.ProcessState;
import io.camunda.zeebe.protocol.impl.record.value.message.MessageCorrelationRecord;
import io.camunda.zeebe.protocol.impl.record.value.message.MessageRecord;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.MessageCorrelationIntent;
import io.camunda.zeebe.protocol.record.intent.MessageIntent;
import io.camunda.zeebe.stream.api.records.TypedRecord;
import io.camunda.zeebe.stream.api.state.KeyGenerator;

public final class MessageCorrelationCorrelateProcessor
    implements TypedRecordProcessor<MessageCorrelationRecord> {

  private static final String SUBSCRIPTION_NOT_FOUND =
      "Expected to find subscription for message with name '%s' and correlation key '%s', but none was found.";

  private final MessageCorrelateBehavior correlateBehavior;
  private final KeyGenerator keyGenerator;
  private final StateWriter stateWriter;
  private final TypedResponseWriter responseWriter;
  private final TypedRejectionWriter rejectionWriter;

  public MessageCorrelationCorrelateProcessor(
      final Writers writers,
      final KeyGenerator keyGenerator,
      final EventScopeInstanceState eventScopeInstanceState,
      final ProcessState processState,
      final BpmnBehaviors bpmnBehaviors,
      final MessageStartEventSubscriptionState startEventSubscriptionState,
      final MessageState messageState,
      final MessageSubscriptionState messageSubscriptionState,
      final SubscriptionCommandSender commandSender) {
    stateWriter = writers.state();
    responseWriter = writers.response();
    rejectionWriter = writers.rejection();
    this.keyGenerator = keyGenerator;
    final var eventHandle =
        new EventHandle(
            keyGenerator,
            eventScopeInstanceState,
            writers,
            processState,
            bpmnBehaviors.eventTriggerBehavior(),
            bpmnBehaviors.stateBehavior());
    correlateBehavior =
        new MessageCorrelateBehavior(
            startEventSubscriptionState,
            messageState,
            eventHandle,
            stateWriter,
            messageSubscriptionState,
            commandSender);
  }

  @Override
  public void processRecord(final TypedRecord<MessageCorrelationRecord> command) {
    final var messageCorrelationRecord = command.getValue();
    final long messageKey = keyGenerator.nextKey();
    messageCorrelationRecord
        .setMessageKey(messageKey)
        .setRequestId(command.getRequestId())
        .setRequestStreamId(command.getRequestStreamId());

    final var messageRecord =
        new MessageRecord()
            .setName(command.getValue().getName())
            .setCorrelationKey(command.getValue().getCorrelationKey())
            .setVariables(command.getValue().getVariablesBuffer())
            .setTenantId(command.getValue().getTenantId())
            .setTimeToLive(-1L);
    stateWriter.appendFollowUpEvent(messageKey, MessageIntent.PUBLISHED, messageRecord);

    stateWriter.appendFollowUpEvent(
        messageKey, MessageCorrelationIntent.CORRELATING, messageCorrelationRecord);

    final var correlatingSubscriptions = new Subscriptions();
    correlateToMessageStartEventSubscriptions(command, messageKey, correlatingSubscriptions);
    correlateToMessageEventSubscriptions(command.getValue(), messageKey, correlatingSubscriptions);

    if (correlatingSubscriptions.isEmpty()) {
      final var errorMessage =
          SUBSCRIPTION_NOT_FOUND.formatted(
              command.getValue().getName(), command.getValue().getCorrelationKey());
      rejectionWriter.appendRejection(command, RejectionType.NOT_FOUND, errorMessage);
      responseWriter.writeRejectionOnCommand(command, RejectionType.NOT_FOUND, errorMessage);
    }

    // Message Correlate command cannot have a TTL. As a result the message expires immediately.
    stateWriter.appendFollowUpEvent(messageKey, MessageIntent.EXPIRED, messageRecord);
  }

  private void correlateToMessageStartEventSubscriptions(
      final TypedRecord<MessageCorrelationRecord> command,
      final long messageKey,
      final Subscriptions correlatingSubscriptions) {
    final var messageCorrelationRecord = command.getValue();
    final var correlatedSubscriptions =
        correlateBehavior.correlateToMessageStartEvents(
            new MessageData(
                messageKey,
                messageCorrelationRecord.getNameBuffer(),
                messageCorrelationRecord.getCorrelationKeyBuffer(),
                messageCorrelationRecord.getVariablesBuffer(),
                messageCorrelationRecord.getTenantId()));
    correlatingSubscriptions.addAll(correlatedSubscriptions);

    if (!correlatedSubscriptions.isEmpty()) {
      final var subscription = correlatedSubscriptions.peek();
      messageCorrelationRecord.setProcessInstanceKey(subscription.getProcessInstanceKey());

      stateWriter.appendFollowUpEvent(
          messageKey, MessageCorrelationIntent.CORRELATED, messageCorrelationRecord);
      responseWriter.writeEventOnCommand(
          messageKey, MessageCorrelationIntent.CORRELATED, messageCorrelationRecord, command);
    }
  }

  private void correlateToMessageEventSubscriptions(
      final MessageCorrelationRecord messageCorrelationRecord,
      final long messageKey,
      final Subscriptions correlatingSubscriptions) {
    correlatingSubscriptions.addAll(
        correlateBehavior.correlateToMessageEvents(
            new MessageData(
                messageKey,
                messageCorrelationRecord.getNameBuffer(),
                messageCorrelationRecord.getCorrelationKeyBuffer(),
                messageCorrelationRecord.getVariablesBuffer(),
                messageCorrelationRecord.getTenantId())));
  }
}