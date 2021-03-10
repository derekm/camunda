/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import React from 'react';

import {Select, SelectionPreview} from 'components';
import {t} from 'translation';

import AggregationType from './AggregationType';

import './Measure.scss';

export default function Measure({report, updateMeasure, updateAggregation}) {
  if (report.view.properties?.length === 2) {
    return (
      <>
        <li className="Measure select">
          <span className="label">{t('report.measure')}</span>
          <SelectionPreview onClick={() => updateMeasure(['duration'])}>
            <span>{t('report.view.count')}</span>
          </SelectionPreview>
        </li>
        <li className="Measure select">
          <span className="label"></span>
          <SelectionPreview onClick={() => updateMeasure(['frequency'])}>
            <span>
              {report.view.entity === 'incident'
                ? t('report.view.resolutionDuration')
                : t('report.view.duration')}
            </span>
          </SelectionPreview>
          <AggregationType report={report} onChange={updateAggregation} />
        </li>
      </>
    );
  } else {
    return (
      <li className="Measure select">
        <span className="label">{t('report.measure')}</span>
        <Select
          value={report.view.properties[0]}
          onChange={(property) => updateMeasure([property])}
        >
          <Select.Option value="frequency">{t('report.view.count')}</Select.Option>
          <Select.Option value="duration">
            {report.view.entity === 'incident'
              ? t('report.view.resolutionDuration')
              : t('report.view.duration')}
          </Select.Option>
        </Select>
        <AggregationType report={report} onChange={updateAggregation} />
      </li>
    );
  }
}
