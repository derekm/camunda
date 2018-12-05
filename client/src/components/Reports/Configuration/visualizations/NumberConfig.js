import React from 'react';

import {Switch, Input} from 'components';

import CountTargetInput from './subComponents/CountTargetInput';
import DurationTargetInput from './subComponents/DurationTargetInput';

import './NumberConfig.scss';

export default function NumberConfig({report, configuration, onChange}) {
  const targetValue = configuration.targetValue
    ? configuration.targetValue
    : NumberConfig.defaults({report}).targetValue;

  const precisionSet = typeof configuration.precision === 'number';
  const countOperation = report.data.view.operation === 'count';
  const goalSet = targetValue.active;

  return (
    <div className="NumberConfig">
      <fieldset>
        <legend>
          <Switch
            checked={precisionSet}
            onChange={evt => onChange('precision', evt.target.checked ? 1 : null)}
          />
          Limit Precision
        </legend>
        <Input
          className="precision"
          disabled={typeof configuration.precision !== 'number'}
          onChange={() => {}}
          onKeyDown={evt => {
            const number = parseInt(evt.key, 10);
            if (number) {
              onChange('precision', number);
            }
          }}
          value={precisionSet ? configuration.precision : 1}
        />
        most significant
        {countOperation ? ' digits' : ' units'}
      </fieldset>
      <fieldset>
        <legend>
          <Switch
            checked={goalSet}
            onChange={evt =>
              onChange('targetValue', {
                ...targetValue,
                active: evt.target.checked
              })
            }
          />Goal
        </legend>
        {countOperation ? (
          <CountTargetInput
            baseline={targetValue.values.baseline}
            target={targetValue.values.target}
            disabled={!goalSet}
            onChange={(type, value) =>
              onChange('targetValue', {
                ...targetValue,
                values: {...targetValue.values, [type]: value}
              })
            }
          />
        ) : (
          <DurationTargetInput
            baseline={targetValue.values.baseline}
            target={targetValue.values.target}
            disabled={!goalSet}
            onChange={(type, subType, value) =>
              onChange('targetValue', {
                ...targetValue,
                values: {
                  ...targetValue.values,
                  [type]: {
                    ...targetValue.values[type],
                    [subType]: value
                  }
                }
              })
            }
          />
        )}
      </fieldset>
    </div>
  );
}

NumberConfig.defaults = ({report}) => {
  const {operation} = report.data.view;

  return {
    precision: null,
    targetValue: {
      active: false,
      values:
        operation === 'count'
          ? {
              baseline: 0,
              target: 100
            }
          : {
              baseline: {
                value: 0,
                unit: 'hours'
              },
              target: {
                value: 2,
                unit: 'hours'
              }
            }
    }
  };
};

NumberConfig.onUpdate = (prevProps, props) => {
  if (props.report.combined) return prevProps.type !== props.type && NumberConfig.defaults(props);
  if (
    props.report.data.view.property !== prevProps.report.data.view.property ||
    prevProps.type !== props.type
  ) {
    return NumberConfig.defaults(props);
  }
};
