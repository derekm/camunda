/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.processing.processinstance;

import static io.camunda.zeebe.engine.state.immutable.IncidentState.MISSING_INCIDENT;

import io.camunda.zeebe.auth.impl.TenantAuthorizationCheckerImpl;
import io.camunda.zeebe.engine.processing.deployment.model.element.ExecutableActivity;
import io.camunda.zeebe.engine.state.deployment.DeployedProcess;
import io.camunda.zeebe.engine.state.immutable.ElementInstanceState;
import io.camunda.zeebe.engine.state.immutable.EventScopeInstanceState;
import io.camunda.zeebe.engine.state.immutable.IncidentState;
import io.camunda.zeebe.engine.state.instance.ElementInstance;
import io.camunda.zeebe.engine.state.instance.EventTrigger;
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceRecord;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceMigrationRecordValue.ProcessInstanceMigrationMappingInstructionValue;
import io.camunda.zeebe.util.buffer.BufferUtil;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.agrona.DirectBuffer;

public final class ProcessInstanceMigrationPreconditionChecker {

  private static final EnumSet<BpmnElementType> SUPPORTED_ELEMENT_TYPES =
      EnumSet.of(BpmnElementType.PROCESS, BpmnElementType.SERVICE_TASK);
  private static final Set<BpmnElementType> UNSUPPORTED_ELEMENT_TYPES =
      EnumSet.complementOf(SUPPORTED_ELEMENT_TYPES);

  private static final String ERROR_MESSAGE_PROCESS_INSTANCE_NOT_FOUND =
      "Expected to migrate process instance but no process instance found with key '%d'";
  private static final String ERROR_MESSAGE_PROCESS_DEFINITION_NOT_FOUND =
      "Expected to migrate process instance to process definition but no process definition found with key '%d'";
  private static final String ERROR_MESSAGE_DUPLICATE_SOURCE_ELEMENT_IDS =
      "Expected to migrate process instance '%s' but the mapping instructions contain duplicate source element ids '%s'.";
  private static final String ERROR_CHILD_PROCESS_INSTANCE =
      "Expected to migrate process instance '%s' but process instance is a child process instance. Child process instances cannot be migrated.";
  private static final String ERROR_SOURCE_ELEMENT_ID_NOT_FOUND =
      """
              Expected to migrate process instance '%s' \
              but mapping instructions contain a non-existing source element id '%s'. \
              Elements provided in mapping instructions must exist \
              in the source process definition.""";
  private static final String ERROR_TARGET_ELEMENT_ID_NOT_FOUND =
      """
              Expected to migrate process instance '%s' \
              but mapping instructions contain a non-existing target element id '%s'. \
              Elements provided in mapping instructions must exist \
              in the target process definition.""";
  private static final String ERROR_MESSAGE_EVENT_SUBPROCESS_NOT_SUPPORTED_IN_PROCESS_INSTANCE =
      "Expected to migrate process instance but process instance has an event subprocess. Process instances with event subprocesses cannot be migrated yet.";
  private static final String ERROR_MESSAGE_EVENT_SUBPROCESS_NOT_SUPPORTED_IN_TARGET_PROCESS =
      "Expected to migrate process instance but target process has an event subprocess. Target processes with event subprocesses cannot be migrated yet.";
  private static final String ERROR_UNSUPPORTED_ELEMENT_TYPE =
      "Expected to migrate process instance '%s' but active element with id '%s' has an unsupported type. The migration of a %s is not supported.";
  private static final String ERROR_UNMAPPED_ACTIVE_ELEMENT =
      "Expected to migrate process instance '%s' but no mapping instruction defined for active element with id '%s'. Elements cannot be migrated without a mapping.";
  private static final String ERROR_ELEMENT_WITH_INCIDENT =
      """
              Expected to migrate process instance '%s' \
              but active element with id '%s' has an incident. \
              Elements cannot be migrated with an incident yet. \
              Please retry migration after resolving the incident.""";
  private static final String ERROR_ELEMENT_TYPE_CHANGED =
      """
              Expected to migrate process instance '%s' \
              but active element with id '%s' and type '%s' is mapped to \
              an element with id '%s' and different type '%s'. \
              Elements must be mapped to elements of the same type.""";
  private static final String ERROR_MESSAGE_ELEMENT_FLOW_SCOPE_CHANGED =
      """
              Expected to migrate process instance '%s' \
              but the flow scope of active element with id '%s' is changed. \
              The flow scope of the active element is expected to be '%s' but was '%s'. \
              The flow scope of an element cannot be changed during migration yet.""";
  private static final String ERROR_ACTIVE_ELEMENT_WITH_BOUNDARY_EVENT =
      """
              Expected to migrate process instance '%s' \
              but active element with id '%s' has a boundary event. \
              Migrating active elements with boundary events is not possible yet.""";
  private static final String ERROR_TARGET_ELEMENT_WITH_BOUNDARY_EVENT =
      """
              Expected to migrate process instance '%s' \
              but target element with id '%s' has a boundary event. \
              Migrating target elements with boundary events is not possible yet.""";
  private static final String ERROR_CONCURRENT_COMMAND =
      "Expected to migrate process instance '%s' but a concurrent command was executed on the process instance. Please retry the migration.";
  private static final long NO_PARENT = -1L;

  public static void requireNonNullProcessInstance(
      final ElementInstance record, final long processInstanceKey) {
    if (record == null) {
      final String reason =
          String.format(ERROR_MESSAGE_PROCESS_INSTANCE_NOT_FOUND, processInstanceKey);
      throw new ProcessInstanceMigrationPreconditionFailedException(
          reason, RejectionType.NOT_FOUND);
    }
  }

  public static void requireAuthorizedTenant(
      final Map<String, Object> authorizations,
      final String tenantId,
      final long processInstanceKey) {
    final boolean isTenantAuthorized =
        TenantAuthorizationCheckerImpl.fromAuthorizationMap(authorizations).isAuthorized(tenantId);
    if (!isTenantAuthorized) {
      final String reason =
          String.format(ERROR_MESSAGE_PROCESS_INSTANCE_NOT_FOUND, processInstanceKey);
      throw new ProcessInstanceMigrationPreconditionFailedException(
          reason, RejectionType.NOT_FOUND);
    }
  }

  /**
   * Exception that can be thrown during the migration of a process instance, in case any of the
   * process instance is a child process instance.
   */
  public static void requireNullParent(
      final long parentProcessInstanceKey, final long processInstanceKey) {
    if (parentProcessInstanceKey != NO_PARENT) {
      final String reason = String.format(ERROR_CHILD_PROCESS_INSTANCE, processInstanceKey);
      throw new ProcessInstanceMigrationPreconditionFailedException(
          reason, RejectionType.INVALID_STATE);
    }
  }

  public static void requireNonNullTargetProcessDefinition(
      final DeployedProcess targetProcessDefinition, final long targetProcessDefinitionKey) {
    if (targetProcessDefinition == null) {
      final String reason =
          String.format(ERROR_MESSAGE_PROCESS_DEFINITION_NOT_FOUND, targetProcessDefinitionKey);
      throw new ProcessInstanceMigrationPreconditionFailedException(
          reason, RejectionType.NOT_FOUND);
    }
  }

  public static void requireNonDuplicateSourceElementIds(
      final List<ProcessInstanceMigrationMappingInstructionValue> mappingInstructions,
      final long processInstanceKey) {
    final Map<String, Long> countBySourceElementId =
        mappingInstructions.stream()
            .collect(
                Collectors.groupingBy(
                    ProcessInstanceMigrationMappingInstructionValue::getSourceElementId,
                    Collectors.counting()));
    final List<String> duplicateSourceElementIds =
        countBySourceElementId.entrySet().stream()
            .filter(entry -> entry.getValue() > 1)
            .map(Entry::getKey)
            .toList();

    if (!duplicateSourceElementIds.isEmpty()) {
      final String reason =
          String.format(
              ERROR_MESSAGE_DUPLICATE_SOURCE_ELEMENT_IDS,
              processInstanceKey,
              duplicateSourceElementIds);
      throw new ProcessInstanceMigrationPreconditionFailedException(
          reason, RejectionType.INVALID_ARGUMENT);
    }
  }

  /**
   * Exception that can be thrown during the migration of a process instance, in following cases:
   *
   * <p>
   *
   * <ul>
   *   <li>A mapping instruction contains a source element id that does not exist in the source
   *       process definition.
   *   <li>A mapping instruction contains a target element id that does not exist in the target
   *       process definition.
   * </ul>
   *
   * <p>
   */
  public static void requireReferredElementsExist(
      final DeployedProcess sourceProcessDefinition,
      final DeployedProcess targetProcessDefinition,
      final List<ProcessInstanceMigrationMappingInstructionValue> mappingInstructions,
      final long processInstanceKey) {

    mappingInstructions.forEach(
        instruction -> {
          final String sourceElementId = instruction.getSourceElementId();
          if (sourceProcessDefinition.getProcess().getElementById(sourceElementId) == null) {
            final String reason =
                String.format(
                    ERROR_SOURCE_ELEMENT_ID_NOT_FOUND, processInstanceKey, sourceElementId);
            throw new ProcessInstanceMigrationPreconditionFailedException(
                reason, RejectionType.INVALID_ARGUMENT);
          }

          final String targetElementId = instruction.getTargetElementId();
          if (targetProcessDefinition.getProcess().getElementById(targetElementId) == null) {
            final String reason =
                String.format(
                    ERROR_TARGET_ELEMENT_ID_NOT_FOUND, processInstanceKey, targetElementId);
            throw new ProcessInstanceMigrationPreconditionFailedException(
                reason, RejectionType.INVALID_ARGUMENT);
          }
        });
  }

  public static void requireNoEventSubprocess(
      final DeployedProcess sourceProcessDefinition,
      final DeployedProcess targetProcessDefinition) {
    if (!sourceProcessDefinition.getProcess().getEventSubprocesses().isEmpty()) {
      throw new ProcessInstanceMigrationPreconditionFailedException(
          ERROR_MESSAGE_EVENT_SUBPROCESS_NOT_SUPPORTED_IN_PROCESS_INSTANCE,
          RejectionType.INVALID_STATE);
    }

    if (!targetProcessDefinition.getProcess().getEventSubprocesses().isEmpty()) {
      throw new ProcessInstanceMigrationPreconditionFailedException(
          ERROR_MESSAGE_EVENT_SUBPROCESS_NOT_SUPPORTED_IN_TARGET_PROCESS,
          RejectionType.INVALID_STATE);
    }
  }

  /**
   * Exception that can be thrown during the migration of a process instance, in case the engine
   * attempts to migrate an element which is not supported at this time.
   */
  public static void requireSupportedElementType(
      final ProcessInstanceRecord elementInstanceRecord, final long processInstanceKey) {
    if (UNSUPPORTED_ELEMENT_TYPES.contains(elementInstanceRecord.getBpmnElementType())) {
      final String reason =
          String.format(
              ERROR_UNSUPPORTED_ELEMENT_TYPE,
              processInstanceKey,
              elementInstanceRecord.getElementId(),
              elementInstanceRecord.getBpmnElementType());
      throw new ProcessInstanceMigrationPreconditionFailedException(
          reason, RejectionType.INVALID_STATE);
    }
  }

  /**
   * Exception that can be thrown during the migration of a process instance, in case the engine
   * attempts to migrate an element which is not mapped.
   */
  public static void requireNonNullTargetElementId(
      final String targetElementId, final long processInstanceKey, final String sourceElementId) {
    if (targetElementId == null) {
      final String reason =
          String.format(ERROR_UNMAPPED_ACTIVE_ELEMENT, processInstanceKey, sourceElementId);
      throw new ProcessInstanceMigrationPreconditionFailedException(
          reason, RejectionType.INVALID_STATE);
    }
  }

  /**
   * Exception that can be thrown during the migration of a process instance, in case the engine
   * attempts to migrate an element that has an incident.
   */
  public static void requireNoIncident(
      final IncidentState incidentState, final ElementInstance elementInstance) {
    final boolean hasIncident =
        incidentState.getProcessInstanceIncidentKey(elementInstance.getKey()) != MISSING_INCIDENT
            || (elementInstance.getJobKey() > -1L
                && incidentState.getJobIncidentKey(elementInstance.getJobKey())
                    != MISSING_INCIDENT);

    if (hasIncident) {
      final var elementInstanceRecord = elementInstance.getValue();
      final String reason =
          String.format(
              ERROR_ELEMENT_WITH_INCIDENT,
              elementInstanceRecord.getProcessInstanceKey(),
              elementInstanceRecord.getElementId());
      throw new ProcessInstanceMigrationPreconditionFailedException(
          reason, RejectionType.INVALID_STATE);
    }
  }

  /**
   * Exception that can be thrown during the migration of a process instance, in case any of the
   * mapping instructions of the command refer to a source and a target element with different
   * element type, or different event type.
   */
  public static void requireSameElementType(
      final DeployedProcess targetProcessDefinition,
      final String targetElementId,
      final ProcessInstanceRecord elementInstanceRecord,
      final long processInstanceKey) {
    final BpmnElementType targetElementType =
        targetProcessDefinition.getProcess().getElementById(targetElementId).getElementType();
    if (elementInstanceRecord.getBpmnElementType() != targetElementType) {
      final String reason =
          String.format(
              ERROR_ELEMENT_TYPE_CHANGED,
              processInstanceKey,
              elementInstanceRecord.getElementId(),
              elementInstanceRecord.getBpmnElementType(),
              targetElementId,
              targetElementType);
      throw new ProcessInstanceMigrationPreconditionFailedException(
          reason, RejectionType.INVALID_STATE);
    }
  }

  /**
   * Exception that can be thrown during the migration of a process instance, in case the engine
   * attempts to change element flow scope in the target process definition.
   */
  public static void requireUnchangedFlowScope(
      final ElementInstanceState elementInstanceState,
      final ProcessInstanceRecord elementInstanceRecord,
      final DeployedProcess targetProcessDefinition,
      final String targetElementId) {
    final ElementInstance sourceFlowScopeElement =
        elementInstanceState.getInstance(elementInstanceRecord.getFlowScopeKey());
    if (sourceFlowScopeElement != null) {
      final DirectBuffer expectedFlowScopeId =
          sourceFlowScopeElement.getValue().getElementIdBuffer();
      final DirectBuffer actualFlowScopeId =
          targetProcessDefinition
              .getProcess()
              .getElementById(targetElementId)
              .getFlowScope()
              .getId();

      if (!expectedFlowScopeId.equals(actualFlowScopeId)) {
        final String reason =
            String.format(
                ERROR_MESSAGE_ELEMENT_FLOW_SCOPE_CHANGED,
                elementInstanceRecord.getProcessInstanceKey(),
                elementInstanceRecord.getElementId(),
                BufferUtil.bufferAsString(expectedFlowScopeId),
                BufferUtil.bufferAsString(actualFlowScopeId));
        throw new ProcessInstanceMigrationPreconditionFailedException(
            reason, RejectionType.INVALID_STATE);
      }
    }
  }

  /**
   * Exception that can be thrown during the migration of a process instance, in case process
   * instance has an active element with a boundary event
   */
  public static void requireNoBoundaryEventInSource(
      final DeployedProcess sourceProcessDefinition,
      final ProcessInstanceRecord elementInstanceRecord) {
    final boolean hasBoundaryEventInSource =
        !sourceProcessDefinition
            .getProcess()
            .getElementById(elementInstanceRecord.getElementId(), ExecutableActivity.class)
            .getBoundaryEvents()
            .isEmpty();

    if (hasBoundaryEventInSource) {
      final String reason =
          String.format(
              ERROR_ACTIVE_ELEMENT_WITH_BOUNDARY_EVENT,
              elementInstanceRecord.getProcessInstanceKey(),
              elementInstanceRecord.getElementId());
      throw new ProcessInstanceMigrationPreconditionFailedException(
          reason, RejectionType.INVALID_STATE);
    }
  }

  /**
   * Exception that can be thrown during the migration of a process instance, in case target process
   * definition has an element with a boundary event
   */
  public static void requireNoBoundaryEventInTarget(
      final DeployedProcess targetProcessDefinition,
      final String targetElementId,
      final ProcessInstanceRecord elementInstanceRecord) {
    final boolean hasBoundaryEventInTarget =
        !targetProcessDefinition
            .getProcess()
            .getElementById(targetElementId, ExecutableActivity.class)
            .getBoundaryEvents()
            .isEmpty();

    if (hasBoundaryEventInTarget) {
      final String reason =
          String.format(
              ERROR_TARGET_ELEMENT_WITH_BOUNDARY_EVENT,
              elementInstanceRecord.getProcessInstanceKey(),
              elementInstanceRecord.getElementId());
      throw new ProcessInstanceMigrationPreconditionFailedException(
          reason, RejectionType.INVALID_STATE);
    }
  }

  /**
   * Exception that can be thrown during the migration of a process instance, in case the engine
   * processes another command concurrently for the process instance, for example, a job complete, a
   * timer trigger, or a message correlation. Since the concurrent command modifies the process
   * instance, it is not safe to apply the migration in between.
   */
  public static void requireNoConcurrentCommand(
      final EventScopeInstanceState eventScopeInstanceState,
      final ElementInstance elementInstance,
      final long processInstanceKey) {
    final EventTrigger eventTrigger =
        eventScopeInstanceState.peekEventTrigger(elementInstance.getKey());

    // An event trigger indicates a concurrent command. It is created when completing a job, or
    // triggering a timer/message/signal event.
    // or
    // An active sequence flow indicates a concurrent command. It is created when taking a
    // sequence flow and writing an ACTIVATE command for the next element.
    if (eventTrigger != null || elementInstance.getActiveSequenceFlows() > 0) {
      final String reason = String.format(ERROR_CONCURRENT_COMMAND, processInstanceKey);
      throw new ProcessInstanceMigrationPreconditionFailedException(
          reason, RejectionType.INVALID_STATE);
    }
  }

  public static final class ProcessInstanceMigrationPreconditionFailedException
      extends RuntimeException {
    private final RejectionType rejectionType;

    public ProcessInstanceMigrationPreconditionFailedException(
        final String message, final RejectionType rejectionType) {
      super(message);
      this.rejectionType = rejectionType;
    }

    public RejectionType getRejectionType() {
      return rejectionType;
    }
  }
}
