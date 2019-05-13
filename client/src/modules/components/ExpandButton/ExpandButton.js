/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import React from 'react';
import PropTypes from 'prop-types';
import * as Styled from './styled';
import {ExpandButtonThemes} from 'modules/theme';

export default function ExpandButton({children, isExpanded, ...props}) {
  return (
    <Styled.Button {...props}>
      <Styled.Transition timeout={400} in={isExpanded} appear>
        <Styled.Icon expandTheme={props.expandTheme}>
          <Styled.RightIcon />
        </Styled.Icon>
      </Styled.Transition>

      {children}
    </Styled.Button>
  );
}

ExpandButton.propTypes = {
  isExpanded: PropTypes.bool,
  expandTheme: PropTypes.oneOf(Object.keys(ExpandButtonThemes)).isRequired
};

ExpandButton.defaultProps = {
  isExpanded: false
};
