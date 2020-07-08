/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import React, {useEffect, useState} from 'react';
import PropTypes from 'prop-types';

import {PILL_TYPE, LOADING_STATE, SUBSCRIPTION_TOPIC} from 'modules/constants';
import useSubscription from 'modules/hooks/useSubscription';
import {withFlowNodeTimeStampContext} from 'modules/contexts/FlowNodeTimeStampContext';

import * as Styled from './styled';
import {flowNodeInstance} from 'modules/stores/flowNodeInstance';
import {observer} from 'mobx-react';

const TimeStampPill = observer(function TimeStampPill(props) {
  const {showTimeStamp, onTimeStampToggle} = props;
  const [isDefLoaded, setIsDefLoaded] = useState(false);
  const {subscribe} = useSubscription();
  const {isInitialLoadComplete: isTreeLoaded} = flowNodeInstance.state;

  useEffect(() => {
    const unsubscribeDefinitions = subscribe(
      SUBSCRIPTION_TOPIC.LOAD_STATE_DEFINITIONS,
      LOADING_STATE.LOADED,
      () => setIsDefLoaded(true)
    );
    return () => {
      unsubscribeDefinitions();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const isDisabled = !isTreeLoaded && !isDefLoaded;

  return (
    <Styled.Pill
      isActive={showTimeStamp}
      onClick={onTimeStampToggle}
      type={PILL_TYPE.TIMESTAMP}
      isDisabled={isDisabled}
    >
      {`${showTimeStamp ? 'Hide' : 'Show'} End Time`}
    </Styled.Pill>
  );
});

TimeStampPill.propTypes = {
  onTimeStampToggle: PropTypes.func.isRequired,
  showTimeStamp: PropTypes.bool.isRequired,
};

export default withFlowNodeTimeStampContext(TimeStampPill);
