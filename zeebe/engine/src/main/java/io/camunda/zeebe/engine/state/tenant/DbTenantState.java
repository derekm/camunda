/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.state.tenant;

import io.camunda.zeebe.db.ColumnFamily;
import io.camunda.zeebe.db.TransactionContext;
import io.camunda.zeebe.db.ZeebeDb;
import io.camunda.zeebe.db.impl.DbCompositeKey;
import io.camunda.zeebe.db.impl.DbForeignKey;
import io.camunda.zeebe.db.impl.DbLong;
import io.camunda.zeebe.db.impl.DbString;
import io.camunda.zeebe.engine.state.authorization.EntityTypeValue;
import io.camunda.zeebe.engine.state.mutable.MutableTenantState;
import io.camunda.zeebe.protocol.ZbColumnFamilies;
import io.camunda.zeebe.protocol.impl.record.value.tenant.TenantRecord;
import java.util.Optional;

public class DbTenantState implements MutableTenantState {

  public static final long TENANT_KEY_NOT_FOUND = -1L;

  private final DbString tenantId = new DbString();
  private final DbLong tenantKey = new DbLong();
  private final PersistedTenant persistedTenant = new PersistedTenant();
  private final ColumnFamily<DbLong, PersistedTenant> tenantsColumnFamily;

  private final DbForeignKey<DbLong> fkTenantKey;
  private final DbLong entityKey = new DbLong();
  private final EntityTypeValue entityType = new EntityTypeValue();
  private final DbCompositeKey<DbForeignKey<DbLong>, DbLong> entityByTenantKey;
  private final ColumnFamily<DbCompositeKey<DbForeignKey<DbLong>, DbLong>, EntityTypeValue>
      entityByTenantColumnFamily;

  private final ColumnFamily<DbString, DbForeignKey<DbLong>> tenantByIdColumnFamily;

  public DbTenantState(
      final ZeebeDb<ZbColumnFamilies> zeebeDb, final TransactionContext transactionContext) {
    tenantsColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.TENANTS, transactionContext, tenantKey, persistedTenant);

    fkTenantKey = new DbForeignKey<>(tenantKey, ZbColumnFamilies.TENANTS);

    entityByTenantKey = new DbCompositeKey<>(fkTenantKey, entityKey);
    entityByTenantColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.ENTITY_BY_TENANT, transactionContext, entityByTenantKey, entityType);

    tenantByIdColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.TENANT_BY_ID, transactionContext, tenantId, fkTenantKey);
  }

  @Override
  public void createTenant(final TenantRecord tenantRecord) {
    tenantKey.wrapLong(tenantRecord.getTenantKey());
    tenantId.wrapString(tenantRecord.getTenantId());
    persistedTenant.setTenant(tenantRecord);

    tenantsColumnFamily.insert(tenantKey, persistedTenant);
    tenantByIdColumnFamily.insert(tenantId, fkTenantKey);
  }

  @Override
  public Optional<TenantRecord> getTenantByKey(final long tenantKey) {
    this.tenantKey.wrapLong(tenantKey);
    final PersistedTenant persistedTenant = tenantsColumnFamily.get(this.tenantKey);
    return Optional.ofNullable(persistedTenant != null ? persistedTenant.getTenant() : null);
  }

  @Override
  public Long getTenantKeyById(final String tenantId) {
    this.tenantId.wrapString(tenantId);
    final var fkTenantKey = tenantByIdColumnFamily.get(this.tenantId);
    return fkTenantKey != null ? fkTenantKey.inner().getValue() : TENANT_KEY_NOT_FOUND;
  }
}
