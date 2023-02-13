/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {Modal} from 'modules/components/Modal';
import {useState} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import Placeholder from './placeholder.svg';
import {Container, Image, ActionableNotification, Tag} from './styled';
import {getStateLocally, storeStateLocally} from 'modules/utils/localStorage';
import {Pages} from 'modules/constants/pages';
import {tracking} from 'modules/tracking';

const FirstTimeModal: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(
    !Boolean(getStateLocally('hasConsentedToStartProcess') ?? false),
  );
  const goToInitialPage = () => {
    tracking.track({
      eventName: 'processes-consent-refused',
    });
    navigate(
      {
        ...location,
        pathname: Pages.Initial(),
      },
      {
        replace: true,
      },
    );
  };

  return (
    <Modal
      aria-label="Start your process on demand"
      modalHeading={
        <>
          <Tag
            size="sm"
            type="high-contrast"
            aria-label="This is an alpha feature"
          >
            Alpha
          </Tag>
          <br />
          Start your process on demand
        </>
      }
      secondaryButtonText="Cancel"
      primaryButtonText="Continue"
      open={isOpen}
      onRequestClose={() => {
        goToInitialPage();
      }}
      onRequestSubmit={() => {
        setIsOpen(false);
        storeStateLocally('hasConsentedToStartProcess', true);
        tracking.track({
          eventName: 'processes-consent-accepted',
        });
      }}
      onSecondarySubmit={() => {
        goToInitialPage();
      }}
      preventCloseOnClickOutside
      size="md"
    >
      {isOpen ? (
        <Container>
          <Image
            src={Placeholder}
            alt=""
            data-testid="alpha-warning-modal-image"
          />
          <div>
            <p>Start processes on demand directly from your tasklist.</p>
            <p>
              You can execute all of your processes at any time as long as you
              are eligible to work on tasks inside your project.
            </p>
            <br />
            <p>
              By starting processes on demand you are able to trigger tasks and
              directly start claiming these.
            </p>
          </div>
          <ActionableNotification
            actionButtonLabel="Read consent"
            title="Alpha feature"
            subtitle="this feature is only available for alpha releases."
            hideCloseButton
            kind="info"
            role="alert"
            lowContrast
            inline
            onActionButtonClick={() => {
              tracking.track({
                eventName: 'processes-alpha-consent-link-clicked',
              });
              window.open(
                'https://docs.camunda.io/docs/reference/early-access/#alpha',
                '_blank',
                'noopener noreferrer',
              );
            }}
          />
        </Container>
      ) : null}
    </Modal>
  );
};

export {FirstTimeModal};
