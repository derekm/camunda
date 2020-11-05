/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import React from 'react';
import pluralSuffix from 'modules/utils/pluralSuffix';
import {EXPAND_STATE} from 'modules/constants';
import * as Styled from './styled';
import {useParams} from 'react-router-dom';
import {incidentsStore} from 'modules/stores/incidents';
import {observer} from 'mobx-react';

type Props = {
  onClick?: () => void;
  isOpen?: boolean;
  expandState?: string;
};

const IncidentsBanner: React.FC<Props> = observer(
  ({onClick, isOpen, expandState}) => {
    // @ts-expect-error ts-migrate(2339) FIXME: Property 'id' does not exist on type '{}'.
    const {id} = useParams();
    const {incidentsCount} = incidentsStore;

    const errorMessage = `There ${
      incidentsCount === 1 ? 'is' : 'are'
    } ${pluralSuffix(incidentsCount, 'Incident')} in Instance ${id}. `;
    const title = `View ${pluralSuffix(
      incidentsCount,
      'Incident'
    )} in Instance ${id}. `;

    if (expandState === EXPAND_STATE.COLLAPSED) {
      return null;
    }

    return (
      <Styled.IncidentsBanner
        data-testid="incidents-banner"
        // @ts-expect-error ts-migrate(2769) FIXME: Property 'onClick' does not exist on type 'Intrins... Remove this comment to see the full error message
        onClick={onClick}
        title={title}
        isExpanded={isOpen}
        iconButtonTheme="incidentsBanner"
      >
        {errorMessage}
      </Styled.IncidentsBanner>
    );
  }
);

export {IncidentsBanner};
