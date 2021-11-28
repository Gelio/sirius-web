/*******************************************************************************
 * Copyright (c) 2019, 2020 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
import { ApolloClient, DefaultOptions, HttpLink, InMemoryCache, split } from '@apollo/client';
import { WebSocketLink } from '@apollo/client/link/ws';
import { getMainDefinition } from '@apollo/client/utilities';
import { httpOrigin, wsOrigin } from '@eclipse-sirius/sirius-components';

/** @see https://github.com/eclipse-sirius/sirius-web/blob/0866885f5ed3c3f4ee758c756d4c25c2d7f8fb94/backend/sirius-web-sample-application/src/main/java/org/eclipse/sirius/web/sample/filters/SiriusWebAuthenticationFilter.java#L46 */
const rawCredentials = 'system:012345678910';
const authHeaders = {
  Authorization: `Basic ${btoa(rawCredentials)}`,
};

const httpLink = new HttpLink({
  uri: `${httpOrigin}/api/graphql`,
  headers: authHeaders,
});

const wsLink = new WebSocketLink({
  uri: `${wsOrigin}/subscriptions`,
  options: {
    reconnect: true,
    lazy: true,
  },
});

const splitLink = split(
  ({ query }) => {
    const definition = getMainDefinition(query);
    return definition.kind === 'OperationDefinition' && definition.operation === 'subscription';
  },
  wsLink,
  httpLink
);

const defaultOptions: DefaultOptions = {
  watchQuery: {
    fetchPolicy: 'no-cache',
  },
  query: {
    fetchPolicy: 'no-cache',
  },
  mutate: {
    fetchPolicy: 'no-cache',
  },
};

export const ApolloGraphQLClient = new ApolloClient({
  link: splitLink,
  cache: new InMemoryCache(),
  connectToDevTools: true,
  defaultOptions,
});
