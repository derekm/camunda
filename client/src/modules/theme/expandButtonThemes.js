/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import {css} from 'styled-components';
import {Colors} from '.';

const DEFAULT_THEME = 'collapse';

export function getExpandButtonTheme(themeName) {
  return ExpandButtonThemes[themeName] || ExpandButtonThemes[DEFAULT_THEME];
}

const ExpandButtonThemes = {
  // collapse is used for the incidents panels on dashbord
  collapse: {
    default: {
      background: {
        // transparent background
        dark: '',
        light: ''
      },
      arrow: {
        dark: css`
          color: #ffffff;
        `,
        light: css`
          color: ${Colors.uiDark04};
          opacity: 0.9;
        `
      }
    },
    hover: {
      background: {
        dark: css`
          background: #ffffff;
          opacity: 0.25;
        `,
        light: css`
          background: ${Colors.uiLight05};
          opacity: 0.5;
        `
      },
      arrow: {
        dark: css`
          color: ${Colors.uiLight02};
        `,
        light: css`
          color: ${Colors.uiDark04};
          opacity: 0.9;
        `
      }
    },
    active: {
      background: {
        dark: css`
          background: #ffffff;
          opacity: 0.4;
        `,
        light: css`
          background: ${Colors.uiLight05};
          opacity: 0.8;
        `
      },
      arrow: {
        dark: css`
          color: ${Colors.uiLight02};
        `,
        light: css`
          color: ${Colors.uiDark04};
        `
      }
    }
  },
  // incidentsBar is used in the red incidents bar in instance view
  incidentsBar: {
    default: {
      // transparent background
      background: {dark: '', light: ''},
      arrow: {
        dark: css`
          color: ${Colors.uiLight02};
        `,
        light: css`
          color: ${Colors.uiLight02};
        `
      }
    },
    hover: {
      background: {
        dark: css`
          background: #ffffff;
          opacity: 0.25;
        `,
        light: css`
          background: #ffffff;
          opacity: 0.25;
        `
      },
      arrow: {
        dark: css`
          color: ${Colors.uiLight02};
        `,
        light: css`
          color: ${Colors.uiLight02};
        `
      }
    },
    active: {
      background: {
        dark: css`
          background: #ffffff;
          opacity: 0.4;
        `,
        light: css`
          background: #ffffff;
          opacity: 0.4;
        `
      },
      arrow: {
        dark: css`
          color: ${Colors.uiLight02};
        `,
        light: css`
          color: ${Colors.uiLight02};
        `
      }
    }
  },
  // foldable is used in the history tree view
  foldable: {
    default: {
      // transparent background
      background: {dark: '', light: ''},
      arrow: {
        dark: css`
          color: ${Colors.uiLight02};
        `,
        light: css`
          color: ${Colors.uiDark04};
          opacity: 0.9;
        `
      }
    },
    hover: {
      background: {
        dark: css`
          background: #ffffff;
          opacity: 0.25;
        `,
        light: css`
          background: ${Colors.uiLight05};
          opacity: 0.5;
        `
      },
      arrow: {
        dark: css`
          color: ${Colors.uiLight02};
        `,
        light: css`
          color: ${Colors.uiDark04};
          opacity: 0.9;
        `
      }
    },
    active: {
      background: {
        dark: css`
          background: #ffffff;
          opacity: 0.4;
        `,
        light: css`
          background: ${Colors.uiLight05};
          opacity: 0.8;
        `
      },
      arrow: {
        dark: css`
          color: ${Colors.uiLight02};
        `,
        light: css`
          color: ${Colors.uiDark04};
        `
      }
    }
  },
  // selectionCollapsed is used for collapsed instance selections
  selectionCollapsed: {
    default: {
      // transparent background
      background: {dark: '', light: ''},
      arrow: {
        dark: css`
          color: ${Colors.uiLight02};
        `,
        light: css`
          color: ${Colors.uiDark04};
          opacity: 0.9;
        `
      }
    },
    hover: {
      background: {
        dark: css`
          background: #ffffff;
          opacity: 0.25;
        `,
        light: css`
          background: ${Colors.uiLight05};
          opacity: 0.5;
        `
      },
      arrow: {
        dark: css`
          color: ${Colors.uiLight02};
        `,
        light: css`
          color: ${Colors.uiDark04};
          opacity: 0.9;
        `
      }
    },
    active: {
      background: {
        dark: css`
          background: #ffffff;
          opacity: 0.4;
        `,
        light: css`
          background: ${Colors.uiLight05};
          opacity: 0.8;
        `
      },
      arrow: {
        dark: css`
          color: ${Colors.uiLight02};
        `,
        light: css`
          color: ${Colors.uiDark04};
        `
      }
    }
  },
  // selectionExpanded is used for expanded instance selections
  selectionExpanded: {
    default: {
      // transparent background
      background: {dark: '', light: ''},
      arrow: {
        dark: css`
          color: ${Colors.uiLight02};
        `,
        light: css`
          color: ${Colors.uiLight02};
        `
      }
    },
    hover: {
      background: {
        dark: css`
          background: #ffffff;
          opacity: 0.25;
        `,
        light: css`
          background: #ffffff;
          opacity: 0.25;
        `
      },
      arrow: {
        dark: css`
          color: ${Colors.uiLight02};
        `,
        light: css`
          color: ${Colors.uiLight02};
        `
      }
    },
    active: {
      background: {
        dark: css`
          background: #ffffff;
          opacity: 0.4;
        `,
        light: css`
          background: #ffffff;
          opacity: 0.4;
        `
      },
      arrow: {
        dark: css`
          color: ${Colors.uiLight02};
        `,
        light: css`
          color: ${Colors.uiLight02};
        `
      }
    }
  }
};

export default ExpandButtonThemes;
