/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {expect} from '@playwright/test';
import {test} from '../test-fixtures';
import {Paths} from 'modules/Routes';
import {
  mockEvaluatedDrdData,
  mockEvaluatedDecisionInstance,
  mockEvaluatedXml,
  mockEvaluatedDecisionInstanceWithoutPanels,
  mockEvaluatedDrdDataWithoutPanels,
  mockEvaluatedXmlWithoutPanels,
  mockFailedDecisionInstance,
  mockFailedDrdData,
  mockFailedXml,
  mockResponses,
} from '../mocks/decisionInstance.mocks';

test.describe('decision instance page', () => {
  for (const theme of ['light', 'dark']) {
    //TODO: enable when https://github.com/camunda/operate/issues/3344 is implemented
    test.skip(`error page - ${theme}`, async ({page, commonPage}) => {
      await commonPage.changeTheme(theme);

      await page.route(/^.*\/api.*$/i, mockResponses({}));

      await page.goto(Paths.decisionInstance('1'), {
        waitUntil: 'networkidle',
      });

      await expect(page).toHaveScreenshot();
    });

    test(`evaluated - ${theme}`, async ({page, commonPage}) => {
      await commonPage.changeTheme(theme);

      await page.route(
        /^.*\/api.*$/i,
        mockResponses({
          decisionInstanceDetail: mockEvaluatedDecisionInstance,
          drdData: mockEvaluatedDrdData,
          xml: mockEvaluatedXml,
        }),
      );

      await page.goto(Paths.decisionInstance('1'), {
        waitUntil: 'networkidle',
      });

      await expect(page).toHaveScreenshot();
    });

    test(`evaluated (drd panel maximised) - ${theme}`, async ({
      page,
      commonPage,
    }) => {
      await commonPage.changeTheme(theme);

      await page.route(
        /^.*\/api.*$/i,
        mockResponses({
          decisionInstanceDetail: mockEvaluatedDecisionInstance,
          drdData: mockEvaluatedDrdData,
          xml: mockEvaluatedXml,
        }),
      );

      await page.goto(Paths.decisionInstance('1'), {
        waitUntil: 'networkidle',
      });

      await page
        .getByRole('button', {
          name: /maximize drd panel/i,
        })
        .click();
      await expect(page).toHaveScreenshot();
    });

    test(`evaluated (without input output panel) - ${theme}`, async ({
      page,
      commonPage,
    }) => {
      await commonPage.changeTheme(theme);

      await page.route(
        /^.*\/api.*$/i,
        mockResponses({
          decisionInstanceDetail: mockEvaluatedDecisionInstanceWithoutPanels,
          drdData: mockEvaluatedDrdDataWithoutPanels,
          xml: mockEvaluatedXmlWithoutPanels,
        }),
      );

      await page.goto(Paths.decisionInstance('1'), {
        waitUntil: 'networkidle',
      });

      await expect(page).toHaveScreenshot();
    });

    test(`failed - ${theme}`, async ({page, commonPage}) => {
      await commonPage.changeTheme(theme);

      await page.route(
        /^.*\/api.*$/i,
        mockResponses({
          decisionInstanceDetail: mockFailedDecisionInstance,
          drdData: mockFailedDrdData,
          xml: mockFailedXml,
        }),
      );

      await page.goto(Paths.decisionInstance('1'), {
        waitUntil: 'networkidle',
      });

      await expect(page).toHaveScreenshot();
    });

    test(`failed (result tab selected) - ${theme}`, async ({
      page,
      commonPage,
    }) => {
      await commonPage.changeTheme(theme);

      await page.route(
        /^.*\/api.*$/i,
        mockResponses({
          decisionInstanceDetail: mockFailedDecisionInstance,
          drdData: mockFailedDrdData,
          xml: mockFailedXml,
        }),
      );

      await page.goto(Paths.decisionInstance('1'), {
        waitUntil: 'networkidle',
      });

      await page
        .getByRole('tab', {
          name: /result/i,
        })
        .click();

      await expect(page).toHaveScreenshot();
    });
  }
});
