/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.service;

import io.camunda.search.clients.CamundaSearchClient;
import io.camunda.service.security.auth.Authentication;
import io.camunda.zeebe.broker.client.api.BrokerClient;
import io.camunda.zeebe.gateway.impl.job.ActivateJobsHandler;

public final class CamundaServices extends ApiServices<CamundaServices> {

  public CamundaServices(final BrokerClient brokerClient, final CamundaSearchClient searchClient) {
    this(brokerClient, searchClient, null);
  }

  public CamundaServices(
      final BrokerClient brokerClient,
      final CamundaSearchClient searchClient,
      final Authentication authentication) {
    super(brokerClient, searchClient, authentication);
  }

  public <T> JobServices<T> jobServices(final ActivateJobsHandler<T> activateJobsHandler) {
    return new JobServices<>(
        brokerClient, activateJobsHandler, searchClient, authentication);
  }

  public ProcessInstanceServices processInstanceServices() {
    return new ProcessInstanceServices(brokerClient, searchClient, authentication);
  }

  public UserTaskServices userTaskServices() {
    return new UserTaskServices(brokerClient, searchClient, authentication);
  }

  public VariableServices variableServices() {
    return new VariableServices(brokerClient, searchClient, authentication);
  }

  public DecisionDefinitionServices decisionDefinitionServices() {
    return new DecisionDefinitionServices(brokerClient, searchClient, authentication);
  }

  public DecisionRequirementsServices decisionRequirementsServices() {
    return new DecisionRequirementsServices(
        brokerClient, searchClient, authentication);
  }

  public IncidentServices incidentServices() {
    return new IncidentServices(brokerClient, searchClient, authentication);
  }

  public FlowNodeInstanceServices flownodeInstanceServices() {
    return new FlowNodeInstanceServices(brokerClient, searchClient, transformers, authentication);
  }

  public UserServices userServices() {
    return new UserServices(brokerClient, searchClient, authentication);
  }

  public MessageServices messageServices() {
    return new MessageServices(brokerClient, searchClient, authentication);
  }

  public DocumentServices documentServices() {
    return new DocumentServices(brokerClient, searchClient, authentication);
  }

  public <T> AuthorizationServices<T> authorizationServices() {
    return new AuthorizationServices<>(brokerClient, searchClient, authentication);
  }

  public ClockServices clockServices() {
    return new ClockServices(brokerClient, searchClient, authentication);
  }

  public ResourceServices resourceService() {
    return new ResourceServices(brokerClient, searchClient, authentication);
  }

  public ElementInstanceServices elementServices() {
    return new ElementInstanceServices(brokerClient, searchClient, authentication);
  }

  public SignalServices signalServices() {
    return new SignalServices(brokerClient, searchClient, authentication);
  }

  @Override
  public CamundaServices withAuthentication(final Authentication authentication) {
    return new CamundaServices(brokerClient, searchClient, authentication);
  }
}
