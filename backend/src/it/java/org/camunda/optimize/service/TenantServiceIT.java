/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service;

import com.google.common.collect.ImmutableList;
import org.camunda.optimize.dto.engine.AuthorizationDto;
import org.camunda.optimize.dto.optimize.persistence.TenantDto;
import org.camunda.optimize.service.util.configuration.engine.DefaultTenant;
import org.camunda.optimize.test.it.extension.ElasticSearchIntegrationTestExtensionRule;
import org.camunda.optimize.test.it.extension.EmbeddedOptimizeExtensionRule;
import org.camunda.optimize.test.it.extension.EngineIntegrationExtensionRule;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.camunda.optimize.service.util.configuration.EngineConstantsUtil.ALL_PERMISSION;
import static org.camunda.optimize.service.util.configuration.EngineConstantsUtil.ALL_RESOURCES_RESOURCE_ID;
import static org.camunda.optimize.service.util.configuration.EngineConstantsUtil.AUTHORIZATION_TYPE_GRANT;
import static org.camunda.optimize.service.util.configuration.EngineConstantsUtil.AUTHORIZATION_TYPE_REVOKE;
import static org.camunda.optimize.service.util.configuration.EngineConstantsUtil.RESOURCE_TYPE_TENANT;
import static org.camunda.optimize.test.it.extension.EmbeddedOptimizeExtensionRule.DEFAULT_ENGINE_ALIAS;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.TENANT_INDEX_NAME;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TenantServiceIT {

  @RegisterExtension
  @Order(1)
  public ElasticSearchIntegrationTestExtensionRule elasticSearchIntegrationTestExtensionRule = new ElasticSearchIntegrationTestExtensionRule();
  @RegisterExtension
  @Order(2)
  public EngineIntegrationExtensionRule engineIntegrationExtensionRule = new EngineIntegrationExtensionRule();
  @RegisterExtension
  @Order(3)
  public EmbeddedOptimizeExtensionRule embeddedOptimizeExtensionRule = new EmbeddedOptimizeExtensionRule();

  @Test
  public void getStoredTenants() {
    // given
    final String tenantId = "1";
    final String tenantName = "My Tenant";

    addTenantToElasticsearch(new TenantDto(tenantId, tenantName, DEFAULT_ENGINE_ALIAS));

    // when
    final List<TenantDto> tenants = embeddedOptimizeExtensionRule.getTenantService().getTenants();

    // then
    assertThat(tenants.size(), is(2));
    assertThat(tenants.get(0), is(TenantService.TENANT_NOT_DEFINED));
    assertThat(tenants.get(1).getId(), is(tenantId));
    assertThat(tenants.get(1).getName(), is(tenantName));
  }

  @Test
  public void getDefaultTenants() {
    // given
    final String tenantId = "myTenantId";
    final String tenantName = "Default Tenant";
    setDefaultTenant(new DefaultTenant(tenantId, tenantName));
    embeddedOptimizeExtensionRule.reloadConfiguration();

    // when
    final List<TenantDto> tenants = embeddedOptimizeExtensionRule.getTenantService().getTenants();

    // then
    assertThat(tenants.size(), is(2));
    assertThat(tenants.get(0), is(TenantService.TENANT_NOT_DEFINED));
    assertThat(tenants.get(1).getId(), is(tenantId));
    assertThat(tenants.get(1).getName(), is(tenantName));
  }

  @Test
  public void getDefaultTenantsNoCustomNameDefaultsToId() {
    // given
    final String tenantId = "myTenantId";
    setDefaultTenant(new DefaultTenant(tenantId));
    embeddedOptimizeExtensionRule.reloadConfiguration();

    // when
    final List<TenantDto> tenants = embeddedOptimizeExtensionRule.getTenantService().getTenants();

    // then
    assertThat(tenants.size(), is(2));
    assertThat(tenants.get(0), is(TenantService.TENANT_NOT_DEFINED));
    assertThat(tenants.get(1).getId(), is(tenantId));
    assertThat(tenants.get(1).getName(), is(tenantId));
  }

  @Test
  public void getStoredAndDefaultTenants() {
    // given
    final String storedTenantId = "1";
    final String storedTenantName = "My Tenant";
    final String defaultTenantId = "myTenantId";
    final String defaultTenantName = "Default Tenant";

    addTenantToElasticsearch(new TenantDto(storedTenantId, storedTenantName, DEFAULT_ENGINE_ALIAS));
    setDefaultTenant(new DefaultTenant(defaultTenantId, defaultTenantName));
    embeddedOptimizeExtensionRule.reloadConfiguration();

    // when
    final List<TenantDto> tenants = embeddedOptimizeExtensionRule.getTenantService().getTenants();

    // then
    assertThat(tenants.size(), is(3));
    assertThat(tenants.get(0), is(TenantService.TENANT_NOT_DEFINED));
    assertThat(tenants.get(1).getId(), is(defaultTenantId));
    assertThat(tenants.get(1).getName(), is(defaultTenantName));
    assertThat(tenants.get(2).getId(), is(storedTenantId));
    assertThat(tenants.get(2).getName(), is(storedTenantName));
  }

  @Test
  public void isAuthorizedToAccessTenant_noTenantAccess() {
    // given
    final String tenantId = "1";
    final String tenantName = "My Tenant";
    addTenantToElasticsearch(new TenantDto(tenantId, tenantName, DEFAULT_ENGINE_ALIAS));
    final String tenantUser = "tenantUser";

    createOptimizeUser(tenantUser);

    // when
    final boolean isAuthorized = embeddedOptimizeExtensionRule.getTenantService().isAuthorizedToSeeTenant(tenantUser, tenantId);
    // then
    assertThat(isAuthorized, is(false));
  }

  @Test
  public void isAuthorizedToAccessTenant_allTenantAccessGranted() {
    // given
    final String tenantId = "1";
    final String tenantName = "My Tenant";
    addTenantToElasticsearch(new TenantDto(tenantId, tenantName, DEFAULT_ENGINE_ALIAS));
    final String tenantUser = "tenantUser";

    createUserWithTenantAuthorization(tenantUser, ImmutableList.of(ALL_PERMISSION), ALL_RESOURCES_RESOURCE_ID);

    // when
    final boolean isAuthorized = embeddedOptimizeExtensionRule.getTenantService().isAuthorizedToSeeTenant(tenantUser, tenantId);
    // then
    assertThat(isAuthorized, is(true));
  }

  @Test
  public void isAuthorizedToAccessTenant_specificResourceAccessGranted() {
    // given
    final String tenantId = "1";
    final String tenantName = "My Tenant";
    addTenantToElasticsearch(new TenantDto(tenantId, tenantName, DEFAULT_ENGINE_ALIAS));
    final String tenantUser = "tenantUser";

    createUserWithTenantAuthorization(tenantUser, ImmutableList.of(ALL_PERMISSION), tenantId);

    // when
    final boolean isAuthorized = embeddedOptimizeExtensionRule.getTenantService().isAuthorizedToSeeTenant(tenantUser, tenantId);
    // then
    assertThat(isAuthorized, is(true));
  }

  @Test
  public void isAuthorizedToAccessTenant_specificResourceAccessRevoked() {
    // given
    final String tenantId = "1";
    final String tenantName = "My Tenant";
    addTenantToElasticsearch(new TenantDto(tenantId, tenantName, DEFAULT_ENGINE_ALIAS));
    final String tenantUser = "tenantUser";

    createUserWithTenantAuthorization(tenantUser, ImmutableList.of(ALL_PERMISSION), ALL_RESOURCES_RESOURCE_ID);
    createTenantAuthorization(tenantUser, ImmutableList.of(ALL_PERMISSION), tenantId, AUTHORIZATION_TYPE_REVOKE);

    // when
    final boolean isAuthorized = embeddedOptimizeExtensionRule.getTenantService().isAuthorizedToSeeTenant(tenantUser, tenantId);
    // then
    assertThat(isAuthorized, is(false));
  }

  @Test
  public void getAuthorizedTenantsOnly() {
    // given
    final String storedTenantId1 = "1";
    final String storedTenantName1 = "My Tenant 1";
    final String storedTenantId2 = "2";
    final String storedTenantName2 = "My Tenant 2";

    final String tenantUserId = "tenantUser";

    createUserWithTenantAuthorization(tenantUserId, ImmutableList.of(ALL_PERMISSION), storedTenantId1);
    addTenantToElasticsearch(new TenantDto(storedTenantId1, storedTenantName1, DEFAULT_ENGINE_ALIAS));
    addTenantToElasticsearch(new TenantDto(storedTenantId2, storedTenantName2, DEFAULT_ENGINE_ALIAS));

    //when
    final List<TenantDto> tenantsForUser = embeddedOptimizeExtensionRule.getTenantService().getTenantsForUserByEngine(
      tenantUserId, DEFAULT_ENGINE_ALIAS
    );

    //then
    assertThat(tenantsForUser.size(), is(2));
    assertThat(tenantsForUser.get(0), is(TenantService.TENANT_NOT_DEFINED));
    assertThat(tenantsForUser.get(1).getId(), is(storedTenantId1));
  }

  @Test
  public void getAuthorizedTenantsOnly_withDefaultEngineTenant() {
    // given
    final String storedTenantId1 = "1";
    final String storedTenantName1 = "My Tenant 1";
    final String storedTenantId2 = "2";
    final String storedTenantName2 = "My Tenant 2";
    final String defaultTenantId = "myTenantId";
    final String defaultTenantName = "Default Tenant";
    setDefaultTenant(new DefaultTenant(defaultTenantId, defaultTenantName));
    embeddedOptimizeExtensionRule.reloadConfiguration();

    final String tenantUserId = "tenantUser";

    createUserWithTenantAuthorization(tenantUserId, ImmutableList.of(ALL_PERMISSION), storedTenantId1);
    addTenantToElasticsearch(new TenantDto(storedTenantId1, storedTenantName1, DEFAULT_ENGINE_ALIAS));
    addTenantToElasticsearch(new TenantDto(storedTenantId2, storedTenantName2, DEFAULT_ENGINE_ALIAS));

    //when
    final List<TenantDto> tenantsForUser = embeddedOptimizeExtensionRule.getTenantService().getTenantsForUserByEngine(
      tenantUserId, DEFAULT_ENGINE_ALIAS
    );

    //then
    assertThat(tenantsForUser.size(), is(3));
    assertThat(tenantsForUser.get(0), is(TenantService.TENANT_NOT_DEFINED));
    assertThat(tenantsForUser.get(1).getId(), is(defaultTenantId));
    assertThat(tenantsForUser.get(2).getId(), is(storedTenantId1));
  }

  private void createUserWithTenantAuthorization(final String tenantUser,
                                                 final ImmutableList<String> permissions,
                                                 final String resourceId) {
    createOptimizeUser(tenantUser);
    createTenantAuthorization(tenantUser, permissions, resourceId, AUTHORIZATION_TYPE_GRANT);
  }

  private void createTenantAuthorization(final String tenantUser, final ImmutableList<String> permissions,
                                         final String resourceId, int type) {
    AuthorizationDto authorizationDto = new AuthorizationDto();
    authorizationDto.setResourceType(RESOURCE_TYPE_TENANT);
    authorizationDto.setPermissions(permissions);
    authorizationDto.setResourceId(resourceId);
    authorizationDto.setType(type);
    authorizationDto.setUserId(tenantUser);
    engineIntegrationExtensionRule.createAuthorization(authorizationDto);
  }

  private void createOptimizeUser(final String tenantUser) {
    engineIntegrationExtensionRule.addUser(tenantUser, tenantUser);
    engineIntegrationExtensionRule.grantUserOptimizeAccess(tenantUser);
  }

  private void setDefaultTenant(final DefaultTenant defaultTenant) {
    embeddedOptimizeExtensionRule.getConfigurationService()
      .getConfiguredEngines()
      .values()
      .iterator()
      .next()
      .setDefaultTenant(defaultTenant);
  }

  private void addTenantToElasticsearch(final TenantDto engine) {
    elasticSearchIntegrationTestExtensionRule.addEntryToElasticsearch(TENANT_INDEX_NAME, engine.getId(), engine);
  }

}
