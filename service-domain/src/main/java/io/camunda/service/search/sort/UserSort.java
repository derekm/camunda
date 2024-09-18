/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.service.search.sort;

import io.camunda.util.ObjectBuilder;
import java.util.List;
import java.util.function.Function;

public record UserSort(List<FieldSorting> orderings) implements SortOption {

  @Override
  public List<FieldSorting> getFieldSortings() {
    return orderings;
  }

  public static UserSort of(final Function<Builder, ObjectBuilder<UserSort>> fn) {
    return SortOptionBuilders.user(fn);
  }

  public static final class Builder extends AbstractBuilder<Builder>
      implements ObjectBuilder<UserSort> {

    public Builder key() {
      currentOrdering = new FieldSorting("key", null);
      return this;
    }

    public Builder username() {
      currentOrdering = new FieldSorting("username", null);
      return this;
    }

    public Builder name() {
      currentOrdering = new FieldSorting("name", null);
      return this;
    }

    public Builder email() {
      currentOrdering = new FieldSorting("email", null);
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    @Override
    public Builder asc() {
      return addOrdering(SortOrder.ASC);
    }

    @Override
    public Builder desc() {
      return addOrdering(SortOrder.DESC);
    }

    @Override
    public UserSort build() {
      return new UserSort(orderings);
    }
  }
}