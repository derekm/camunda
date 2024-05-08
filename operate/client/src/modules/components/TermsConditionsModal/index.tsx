/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {observer} from 'mobx-react';
import {Modal, UnorderedList, ListItem} from '@carbon/react';

type Props = {
  isModalOpen: boolean;
  onModalClose: () => void;
};

const TermsConditionsModal: React.FC<Props> = observer(
  ({isModalOpen, onModalClose}) => {
    return (
      <>
        {isModalOpen && (
          <Modal
            open={isModalOpen}
            modalHeading="Terms & Conditions"
            passiveModal
            onRequestClose={onModalClose}
          >
            <p>Copyright Camunda Services GmbH</p>

            <p>Camunda License 1.0</p>

            <p>
              The Camunda License (the “License”) sets forth the terms and
              conditions under which Camunda Services GmbH ("the Licensor")
              grants You a license solely to the source code in this repository
              ("the Software").
            </p>

            <p>
              Acceptance By Using the Software, You agree to all the terms and
              conditions below. If Your Use of the Software does not comply with
              the terms and conditions described in this License, You must
              purchase a commercial license from the Licensor, its affiliated
              entities, or authorized resellers, or You must refrain from Using
              the Software. If You receive the Software in original or modified
              form from a third party, the terms and conditions outlined in this
              License apply to Your Use of that Software. You should have
              received a copy of this License in this case.
            </p>

            <p>
              Copyright License Subject to the terms and conditions of this
              License, the Licensor hereby grants You the non-exclusive,
              royalty-free, worldwide, non-sublicensable, non-transferable right
              to Use the Software in any way or manner that would otherwise
              infringe the Licensor’s copyright as long and insofar as You Use
              the Software only and limited to the Use in or for the purpose of
              Using the Software in Non-Production Environment. Each time you
              distribute or make otherwise publicly available the Software or
              Derivative Works thereof, the recipient automatically receives a
              license from the original Licensor to the respective Software or
              Derivative Works thereof under the terms of this License.
            </p>

            <p>
              Conditions and Restrictions All Use of the Software is explicitly
              made subject to the following conditions:
            </p>

            <UnorderedList>
              <ListItem>
                You may not move, change, disable, or circumvent the license key
                functionality in the Software, and You may not remove or obscure
                any functionality in the Software that is protected by the
                license key.
              </ListItem>
              <ListItem>
                If You distribute or make available the Software or any
                modification or Derivative Works thereof (including compiled
                versions), You must conspicuously display and attach this
                License on each original or modified copy of the Software and
                enable the recipient to obtain the source code if You have
                distributed a compiled version
              </ListItem>
            </UnorderedList>

            <p>
              Patent License Patent and trademark rights are not licensed under
              this Public License.
            </p>

            <p>
              No Liability EXCEPT FOR DAMAGES CAUSED BY INTENT OR FRAUDULENTLY
              CONCEALED DEFECTS, AND EXCEPT FOR DAMAGES RESULTING FROM BREACH OF
              ANY WARRANTY OR GUARANTEE EXPRESSLY GIVEN BY LICENSOR IN THIS
              LICENCE, IN NO EVENT WILL LICENSOR BE LIABLE TO YOU ON ANY LEGAL
              THEORY FOR ANY DAMAGES ARISING OUT OF THIS LICENSE OR THE USE OF
              THE WORK. ANY MANDATORY STATUTORY LIABILITY UNDER APPLICABLE LAW
              REMAINS UNAFFECTED.
            </p>

            <p>
              No Warranty EXCEPT AS EXPRESSLY STATED IN THIS LICENSE OR REQUIRED
              BY APPLICABLE LAW, THE WORKS ARE PROVIDED ON AN "AS IS" BASIS,
              WITHOUT WARRANTIES OF ANY KIND INCLUDING WITHOUT LIMITATION, ANY
              WARRANTIES REGARDING THE CONTENTS, ACCURACY, OR FITNESS FOR A
              PARTICULAR PURPOSE.
            </p>

            <p>
              Definitions You refer to the individual or entity agreeing to
              these terms. Use means any action concerning the Software that,
              without permission, would make Youliable for infringement under
              applicable copyright law. Use within the meaning of this License
              includes, but is not limited to, copying, distribution (with or
              without modification), making available to the public, and
              modifying the Software.
            </p>

            <p>
              Non-Production Environment means a setting in which the Software
              is used for development, staging, testing, quality assurance,
              demonstration, or evaluation purposes, and not for any live or
              production systems.
            </p>
          </Modal>
        )}
      </>
    );
  },
);

export {TermsConditionsModal};