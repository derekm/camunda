/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import React from 'react';
import {shallow} from 'enzyme';

import {CarbonEntityList} from 'components';

import GenerationModal from './GenerationModal';
import EventsSourceModal from './EventsSourceModal';
import {createProcess} from './service';

jest.mock('./service', () => ({createProcess: jest.fn().mockReturnValue('processId')}));
jest.mock('hooks', () => ({
  ...jest.requireActual('hooks'),
  useErrorHandling: () => ({
    mightFail: jest.fn().mockImplementation((data, cb) => cb(data)),
  }),
}));

it('should add/remove a source from the list', () => {
  const node = shallow(<GenerationModal />);

  node.find(CarbonEntityList).prop('action').props.onClick({});

  node.find(EventsSourceModal).prop('onConfirm')([{type: 'external', configuration: {}}]);

  expect(node.find(CarbonEntityList).prop('rows')[0].name).toBe('all events');

  node.find(CarbonEntityList).prop('rows')[0].actions[0].action();

  expect(node.find(CarbonEntityList).prop('rows').length).toBe(0);
});

it('should redirect to the process view on confirmation', () => {
  const sources = [{type: 'external', configuration: {}}];
  const node = shallow(<GenerationModal />);

  node.find(CarbonEntityList).prop('action').props.onClick({});
  node.find(EventsSourceModal).prop('onConfirm')(sources);
  node.find('Button').at(1).simulate('click');

  expect(createProcess).toHaveBeenCalledWith({
    autogenerate: true,
    eventSources: sources,
  });

  expect(node.find('Redirect')).toExist();
  expect(node.props().to).toEqual('/events/processes/processId/generated');
});
