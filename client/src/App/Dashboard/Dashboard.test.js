import React from 'react';
import {shallow} from 'enzyme';
import {mockResolvedAsyncFn} from 'modules/testUtils';

import Dashboard from './Dashboard';

import * as api from './api';

api.fetchInstancesCount = mockResolvedAsyncFn(123);

describe('Dashboard', () => {
  let node;

  beforeEach(() => {
    node = shallow(<Dashboard />)
      .first()
      .shallow();
  });

  it('should render MetricPanel component', () => {
    expect(node.find('MetricPanel').exists()).toBe(true);
  });

  it('should render Header component', () => {
    expect(node.find('Header').exists()).toBe(true);
  });

  it('should render three MetricTile components', async () => {
    expect(node.find('MetricPanel').children().length).toBe(3);
  });

  it('it should request instance counts ', async () => {
    // given
    const spyFetch = jest.spyOn(node.instance(), 'fetchCounts');

    // then
    await node.instance().componentDidMount();
    expect(spyFetch).toHaveBeenCalled();
  });
});
