/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {mockApplyDeleteDefinitionOperation} from 'modules/mocks/api/decisions/operations';
import {decisionDefinitionStore} from 'modules/stores/decisionDefinition';
import {
  render,
  screen,
  waitFor,
  waitForElementToBeRemoved,
} from 'modules/testing-library';
import {ThemeProvider} from 'modules/theme/ThemeProvider';
import {useEffect} from 'react';
import {DecisionOperations} from '.';
import {panelStatesStore} from 'modules/stores/panelStates';
import {operationsStore} from 'modules/stores/operations';

const mockDisplayNotification = jest.fn();

jest.mock('modules/notifications', () => {
  return {
    useNotifications: () => {
      return {displayNotification: mockDisplayNotification};
    },
  };
});

const mockOperation: OperationEntity = {
  id: '2251799813687094',
  name: 'Delete MyDecisionDefinition - Version 1',
  type: 'DELETE_DECISION_DEFINITION',
  startDate: '2023-02-16T14:23:45.306+0100',
  endDate: null,
  instancesCount: 10,
  operationsTotalCount: 10,
  operationsFinishedCount: 0,
};

const Wrapper: React.FC<{children?: React.ReactNode}> = ({children}) => {
  useEffect(() => {
    decisionDefinitionStore.setDefinition({
      name: 'My Definition',
      id: 'myDefinition',
    });

    return () => {
      decisionDefinitionStore.reset();
      panelStatesStore.reset();
      operationsStore.reset();
    };
  }, []);
  return <ThemeProvider>{children}</ThemeProvider>;
};

describe('<DecisionOperations />', () => {
  it('should open modal and show content', async () => {
    const {user} = render(
      <DecisionOperations
        decisionName="myDecision"
        decisionVersion="2"
        decisionDefinitionId="2251799813687094"
      />,
      {wrapper: Wrapper}
    );

    await user.click(
      screen.getByRole('button', {
        name: /^delete decision definition "myDecision - version 2"$/i,
      })
    );

    expect(
      screen.getByText(/You are about to delete the following DRD:/)
    ).toBeInTheDocument();
    expect(screen.getByText(/My Definition/)).toBeInTheDocument();
    expect(
      screen.getByText(
        /Deleting a decision definition will delete the DRD and will impact the following/
      )
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        /^Yes, I confirm I want to delete this DRD and all related instances.$/
      )
    ).toBeInTheDocument();
  });

  it('should apply delete definition operation', async () => {
    mockApplyDeleteDefinitionOperation().withSuccess(mockOperation);

    const {user} = render(
      <DecisionOperations
        decisionName="myDecision"
        decisionVersion="2"
        decisionDefinitionId="2251799813687094"
      />,
      {wrapper: Wrapper}
    );

    await user.click(
      screen.getByRole('button', {
        name: /^delete decision definition "myDecision - version 2"$/i,
      })
    );

    expect(panelStatesStore.state.isOperationsCollapsed).toBe(true);

    await user.click(
      await screen.findByLabelText(
        'Yes, I confirm I want to delete this DRD and all related instances.'
      )
    );

    await user.click(screen.getByRole('button', {name: /danger Delete/}));

    await waitFor(() =>
      expect(operationsStore.state.operations).toEqual([mockOperation])
    );
    expect(panelStatesStore.state.isOperationsCollapsed).toBe(false);
  });

  it('should show notification on operation error', async () => {
    mockApplyDeleteDefinitionOperation().withServerError(500);

    const {user} = render(
      <DecisionOperations
        decisionName="myDecision"
        decisionVersion="2"
        decisionDefinitionId="2251799813687094"
      />,
      {wrapper: Wrapper}
    );

    await user.click(
      screen.getByRole('button', {
        name: /^delete decision definition "myDecision - version 2"$/i,
      })
    );

    expect(panelStatesStore.state.isOperationsCollapsed).toBe(true);

    await user.click(
      await screen.findByLabelText(
        'Yes, I confirm I want to delete this DRD and all related instances.'
      )
    );

    await user.click(screen.getByRole('button', {name: /danger Delete/}));

    await waitFor(() => {
      expect(mockDisplayNotification).toHaveBeenCalledWith('error', {
        headline: 'Operation could not be created',
      });
    });
    expect(panelStatesStore.state.isOperationsCollapsed).toBe(true);
  });

  it('should show notification on operation auth error', async () => {
    mockApplyDeleteDefinitionOperation().withServerError(403);

    const {user} = render(
      <DecisionOperations
        decisionName="myDecision"
        decisionVersion="2"
        decisionDefinitionId="2251799813687094"
      />,
      {wrapper: Wrapper}
    );

    await user.click(
      screen.getByRole('button', {
        name: /^delete decision definition "myDecision - version 2"$/i,
      })
    );

    expect(panelStatesStore.state.isOperationsCollapsed).toBe(true);

    await user.click(
      await screen.findByLabelText(
        'Yes, I confirm I want to delete this DRD and all related instances.'
      )
    );

    await user.click(screen.getByRole('button', {name: /danger Delete/}));

    await waitFor(() => {
      expect(mockDisplayNotification).toHaveBeenCalledWith('error', {
        headline: 'Operation could not be created',
        description: 'You do not have permission',
      });
    });
    expect(panelStatesStore.state.isOperationsCollapsed).toBe(true);
  });

  it('should disable button and show spinner when delete operation is triggered', async () => {
    mockApplyDeleteDefinitionOperation().withSuccess(mockOperation);

    const {user} = render(
      <DecisionOperations
        decisionName="myDecision"
        decisionVersion="2"
        decisionDefinitionId="2251799813687094"
      />,
      {wrapper: Wrapper}
    );

    await user.click(
      screen.getByRole('button', {
        name: /^delete decision definition "myDecision - version 2"$/i,
      })
    );

    await user.click(
      await screen.findByLabelText(
        'Yes, I confirm I want to delete this DRD and all related instances.'
      )
    );

    await user.click(screen.getByRole('button', {name: /danger Delete/}));
    expect(
      await screen.findByTestId('delete-operation-spinner')
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', {
        name: /^delete decision definition "myDecision - version 2"$/i,
      })
    ).toBeDisabled();
  });

  it('should enable button and remove spinner when delete operation failed', async () => {
    mockApplyDeleteDefinitionOperation().withNetworkError();

    const {user} = render(
      <DecisionOperations
        decisionName="myDecision"
        decisionVersion="2"
        decisionDefinitionId="2251799813687094"
      />,
      {wrapper: Wrapper}
    );

    await user.click(
      screen.getByRole('button', {
        name: /^delete decision definition "myDecision - version 2"$/i,
      })
    );

    await user.click(
      await screen.findByLabelText(
        'Yes, I confirm I want to delete this DRD and all related instances.'
      )
    );

    await user.click(screen.getByRole('button', {name: /danger Delete/}));
    expect(
      await screen.findByTestId('delete-operation-spinner')
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', {
        name: /^delete decision definition "myDecision - version 2"$/i,
      })
    ).toBeDisabled();

    await waitForElementToBeRemoved(() =>
      screen.getByTestId('delete-operation-spinner')
    );

    expect(
      screen.getByRole('button', {
        name: /^delete decision definition "myDecision - version 2"$/i,
      })
    ).toBeEnabled();
  });

  it('should show warning when clicking apply without confirmation', async () => {
    mockApplyDeleteDefinitionOperation().withServerError(500);

    const {user} = render(
      <DecisionOperations
        decisionName="myDecision"
        decisionVersion="2"
        decisionDefinitionId="2251799813687094"
      />,
      {wrapper: Wrapper}
    );

    await user.click(
      screen.getByRole('button', {
        name: /^delete decision definition "myDecision - version 2"$/i,
      })
    );

    expect(panelStatesStore.state.isOperationsCollapsed).toBe(true);

    await user.click(screen.getByRole('button', {name: /danger Delete/}));

    expect(
      await screen.findByText('Please tick this box if you want to proceed.')
    ).toBeInTheDocument();

    await user.click(screen.getByRole('button', {name: /close/}));

    await user.click(
      screen.getByRole('button', {
        name: /^delete decision definition "myDecision - version 2"$/i,
      })
    );

    expect(
      screen.queryByText('Please tick this box if you want to proceed.')
    ).not.toBeInTheDocument();
  });
});
