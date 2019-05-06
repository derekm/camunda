/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import styled from 'styled-components';

import BasicTextInput from 'modules/components/Input';

export const VariableFilterInput = styled.div`
  display: flex;
  justify-content: space-between;
`;

export const TextInput = styled(BasicTextInput)`
  min-width: 0;
  &:first-child {
    border-top-right-radius: 0;
    border-bottom-right-radius: 0;
  }

  &:nth-child(2) {
    border-top-left-radius: 0;
    border-bottom-left-radius: 0;
    border-left: none;
  }

  :focus {
    z-index: 2;
  }
`;
