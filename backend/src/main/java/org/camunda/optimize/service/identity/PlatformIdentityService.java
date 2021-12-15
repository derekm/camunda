/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.identity;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.camunda.optimize.dto.optimize.GroupDto;
import org.camunda.optimize.dto.optimize.IdentityDto;
import org.camunda.optimize.dto.optimize.UserDto;
import org.camunda.optimize.rest.engine.EngineContextFactory;
import org.camunda.optimize.service.security.ApplicationAuthorizationService;
import org.camunda.optimize.service.security.IdentityAuthorizationService;
import org.camunda.optimize.service.security.SessionListener;
import org.camunda.optimize.service.util.configuration.condition.CamundaPlatformCondition;
import org.camunda.optimize.service.util.configuration.ConfigurationReloadable;
import org.camunda.optimize.service.util.configuration.ConfigurationService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@Slf4j
@Conditional(CamundaPlatformCondition.class)
public class PlatformIdentityService extends AbstractCachedIdentityService
  implements ConfigurationReloadable, SessionListener {
  private static final int CACHE_MAXIMUM_SIZE = 10_000;

  private LoadingCache<String, List<GroupDto>> userGroupsCache;

  private final ApplicationAuthorizationService applicationAuthorizationService;
  private final IdentityAuthorizationService identityAuthorizationService;
  private final EngineContextFactory engineContextFactory;

  public PlatformIdentityService(final ApplicationAuthorizationService applicationAuthorizationService,
                                 final IdentityAuthorizationService identityAuthorizationService,
                                 final ConfigurationService configurationService,
                                 final EngineContextFactory engineContextFactory,
                                 final UserIdentityCache syncedIdentityCache) {
    super(configurationService, syncedIdentityCache);
    this.applicationAuthorizationService = applicationAuthorizationService;
    this.identityAuthorizationService = identityAuthorizationService;
    this.engineContextFactory = engineContextFactory;
    initUserGroupCache();
  }

  @Override
  public Optional<UserDto> getUserById(final String userId) {
    return syncedIdentityCache.getUserIdentityById(userId)
      .map(Optional::of)
      .orElseGet(
        () -> {
          if (applicationAuthorizationService.isUserAuthorizedToAccessOptimize(userId)) {
            final Optional<UserDto> userDto = engineContextFactory.getConfiguredEngines().stream()
              .map(engineContext -> getIdentityIdIfExistsFromEngine(
                engineContext.getEngineAlias(), userId, () -> engineContext.getUserById(userId)
              ))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .findFirst();
            userDto.ifPresent(this::addIdentity);
            return userDto;
          } else {
            return Optional.empty();
          }
        }
      );
  }

  @Override
  public Optional<GroupDto> getGroupById(final String groupId) {
    return syncedIdentityCache.getGroupIdentityById(groupId)
      .map(Optional::of)
      .orElseGet(
        () -> {
          if (applicationAuthorizationService.isGroupAuthorizedToAccessOptimize(groupId)) {
            final Optional<GroupDto> groupDto = engineContextFactory.getConfiguredEngines().stream()
              .map(engineContext -> getIdentityIdIfExistsFromEngine(
                engineContext.getEngineAlias(), groupId, () -> engineContext.getGroupById(groupId)
              ))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .findFirst();
            groupDto.ifPresent(this::addIdentity);
            return groupDto;
          } else {
            return Optional.empty();
          }
        }
      );
  }

  @Override
  public List<GroupDto> getAllGroupsOfUser(final String userId) {
    return userId != null ? userGroupsCache.get(userId) : Collections.emptyList();
  }

  @Override
  public boolean isUserAuthorizedToAccessIdentity(final String userId, final IdentityDto identity) {
    return identityAuthorizationService.isUserAuthorizedToSeeIdentity(userId, identity.getType(), identity.getId());
  }

  @Override
  public void reloadConfiguration(final ApplicationContext context) {
    cleanUpUserGroupCache();
  }

  @Override
  public void onSessionCreate(final String userId) {
    // NOOP
  }

  @Override
  public void onSessionRefresh(final String userId) {
    userGroupsCache.invalidate(userId);
  }

  @Override
  public void onSessionDestroy(final String userId) {
    userGroupsCache.invalidate(userId);
  }

  private void initUserGroupCache() {
    userGroupsCache = Caffeine.newBuilder()
      .maximumSize(CACHE_MAXIMUM_SIZE)
      .expireAfterAccess(
        configurationService.getAuthConfiguration().getTokenLifeTimeMinutes(), TimeUnit.MINUTES
      )
      .build(this::fetchUserGroups);
  }

  private void cleanUpUserGroupCache() {
    if (userGroupsCache != null) {
      userGroupsCache.invalidateAll();
    }
  }

  private List<GroupDto> fetchUserGroups(final String userId) {
    final Set<GroupDto> result = new HashSet<>();
    applicationAuthorizationService.getAuthorizedEnginesForUser(userId)
      .forEach(
        engineAlias -> engineContextFactory.getConfiguredEngineByAlias(engineAlias)
          .ifPresent(engineContext -> result.addAll(engineContext.getAllGroupsOfUser(userId)))
      );
    return new ArrayList<>(result);
  }

  private <T extends IdentityDto> Optional<T> getIdentityIdIfExistsFromEngine(
    final String engineAlias,
    final String identityId,
    final Supplier<Optional<T>> optionalSupplier) {
    try {
      return optionalSupplier.get();
    } catch (final Exception ex) {
      log.warn("Failed fetching identity with id {} from engine {}.", identityId, engineAlias);
      return Optional.empty();
    }
  }

}
