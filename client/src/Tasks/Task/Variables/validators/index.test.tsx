/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {
  validateNameCharacters,
  validateNameComplete,
  validateDuplicateNames,
  validateValueComplete,
  validateValueJSON,
} from './index';

const mockMeta = {
  blur: vi.fn(),
  change: vi.fn(),
  focus: vi.fn(),
};

describe('Validators', () => {
  beforeEach(() => {
    vi.useFakeTimers({
      shouldAdvanceTime: true,
    });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  describe('validateNameCharacters', () => {
    it.each(['abc', '123'])('should allow: %s', (variableName) => {
      expect(
        validateNameCharacters(
          variableName,
          {newVariables: [{name: variableName}]},
          {
            ...mockMeta,
            name: 'newVariables[0].name',
          },
        ),
      ).toBeUndefined();
    });

    it.each([
      '"',
      ' ',
      'test ',
      '"test"',
      'test\twith\ttab',
      'line\nbreak',
      'carriage\rreturn',
      'form\ffeed',
    ])(`should not allow: %s`, (variableName) => {
      const result = validateNameCharacters(
        variableName,
        {newVariables: [{name: variableName}]},
        {
          ...mockMeta,
          name: 'newVariables[0].name',
        },
      );

      vi.runOnlyPendingTimers();

      expect(result).toBe('Name is invalid');
    });
  });

  describe('validateNameComplete', () => {
    it.each(['abc', 'true', '123'])(`should allow: %s`, (variableName) => {
      expect(
        validateNameComplete(
          variableName,
          {newVariables: [{name: variableName, value: '1'}]},
          {
            ...mockMeta,
            name: 'newVariables[0].name',
          },
        ),
      ).toBeUndefined();
    });

    it.each(['', ' ', '           '])(
      'should not allow empty spaces',
      (variableName) => {
        const result = validateNameComplete(
          variableName,
          {newVariables: [{name: variableName, value: '1'}]},
          {
            ...mockMeta,
            name: 'newVariables[0].name',
          },
        );

        vi.runOnlyPendingTimers();

        expect(result).resolves.toBe('Name has to be filled');
      },
    );
  });

  describe('validateDuplicateNames', () => {
    it('should allow', () => {
      expect(
        validateDuplicateNames(
          'test3',
          {
            '#test1': 'value1',
            newVariables: [
              {name: 'test2', value: 'value2'},
              {name: 'test3', value: 'value3'},
            ],
          },
          {
            ...mockMeta,
            name: '',
          },
        ),
      ).toBe(undefined);

      expect(
        validateDuplicateNames(
          'test2',
          {
            '#test1': 'value1',
            newVariables: [
              {name: 'test2', value: 'value2'},
              {name: 'test2', value: 'value3'},
            ],
          },
          {
            ...mockMeta,
            name: '',
          },
        ),
      ).toBe(undefined);
    });

    it.each(['test1', 'test2'])('should not allow: %s', (variableName) => {
      const [activeResult, errorResult, validatingResult] = [
        validateDuplicateNames(
          variableName,
          {
            '#test1': 'value1',
            newVariables: [
              {name: 'test2', value: 'value2'},
              {name: 'test2', value: 'value3'},
            ],
          },
          {
            ...mockMeta,
            name: '',
            active: true,
          },
        ),
        validateDuplicateNames(
          variableName,
          {
            '#test1': 'value1',
            newVariables: [
              {name: 'test2', value: 'value2'},
              {name: 'test2', value: 'value3'},
            ],
          },
          {
            ...mockMeta,
            name: '',
            error: 'Name must be unique',
          },
        ),
        validateDuplicateNames(
          variableName,
          {
            '#test1': 'value1',
            newVariables: [
              {name: 'test2', value: 'value2'},
              {name: 'test2', value: 'value3'},
            ],
          },
          {
            ...mockMeta,
            name: '',
            validating: true,
          },
        ),
      ];

      vi.runOnlyPendingTimers();

      expect(activeResult).resolves.toBe('Name must be unique');
      expect(errorResult).toBe('Name must be unique');
      expect(validatingResult).resolves.toBe('Name must be unique');
    });
  });

  describe('validateValueComplete', () => {
    it.each(['"abc"', '123', 'true', '{"name": "value"}', '[1, 2, 3]'])(
      'should allow: %s',
      (value) => {
        expect(
          validateValueComplete(
            value,
            {
              newVariables: [{name: 'test', value}],
            },
            {
              ...mockMeta,
              name: `newVariables[0].value`,
            },
          ),
        ).toBeUndefined();
      },
    );

    it('should allow an empty value', () => {
      expect(
        validateValueComplete(
          '',
          {
            newVariables: [{name: '', value: ''}],
          },
          {
            ...mockMeta,
            name: `newVariables[0].value`,
          },
        ),
      ).toBeUndefined();
    });

    it.each(['abc', '"abc', '{name: "value"}', '[[0]', '() => {}'])(
      'should not allow: %s',
      (value) => {
        const result = validateValueComplete(
          value,
          {
            newVariables: [{name: 'test', value}],
          },
          {
            ...mockMeta,
            name: `newVariables[0].value`,
          },
        );

        vi.runOnlyPendingTimers();

        expect(result).resolves.toBe('Value has to be JSON or a literal');
      },
    );
  });

  describe('validateValueJSON', () => {
    it.each(['"abc"', '123', 'true', '{"name": "value"}', '[1, 2, 3]'])(
      'should allow: %s',
      (value) => {
        expect(
          validateValueJSON(
            value,
            {
              newVariables: [{name: 'test', value}],
            },
            {
              ...mockMeta,
              name: 'newVariables[0].value',
            },
          ),
        ).toBeUndefined();
      },
    );

    it.each(['abc', '"abc', '{name: "value"}', '[[0]', '() => {}'])(
      'should not allow: %s',
      (value) => {
        const result = validateValueJSON(
          value,
          {
            newVariables: [{name: 'test', value}],
          },
          {
            ...mockMeta,
            name: 'newVariables[0].value',
          },
        );

        vi.runOnlyPendingTimers();

        expect(result).resolves.toBe('Value has to be JSON or a literal');
      },
    );
  });
});
