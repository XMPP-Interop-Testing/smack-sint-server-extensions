/**
 * Copyright 2026 Ignite Realtime Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smack;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.jivesoftware.smack.bind2.element.Bind2Elements;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.sasl.packet.Sasl2Nonza;
import org.jivesoftware.smack.util.stringencoder.Base64;

import org.igniterealtime.smack.inttest.AbstractSmackSpecificLowLevelIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;

import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

/**
 * Tests that verify the shape of the {@code <authorization-identifier/>} element a server includes in a XEP-0388:
 * Extensible SASL Profile (SASL2) {@code <success/>} response.
 *
 * These tests build a raw PLAIN {@code <authenticate/>} rather than using the high-level login() API, both to
 * control precisely whether a XEP-0386 Bind 2 request is included, and to sidestep Smack's automatic mechanism
 * selection (which currently fails with 'malformed-request' for channel-binding mechanisms in this environment).
 */
@SpecificationReference(document = "XEP-0388", version = "1.0.4")
public class Sasl2AuthorizationIdentifierIntegrationTest extends AbstractSmackSpecificLowLevelIntegrationTest<ModularXmppClientToServerConnection> {

    @SuppressWarnings("this-escape")
    public Sasl2AuthorizationIdentifierIntegrationTest(SmackIntegrationTestEnvironment environment) throws Exception {
        super(environment, ModularXmppClientToServerConnection.class);
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();
            Sasl2TestUtils.requireSasl2(connection, "PLAIN");
        } finally {
            connection.disconnect();
        }
    }

    private static String plainInitialResponse(ModularXmppClientToServerConnection connection) {
        final CharSequence username = connection.getConfiguration().getUsername();
        final String password = connection.getConfiguration().getPassword();
        final String plainPayload = '\0' + username.toString() + '\0' + password;
        return Base64.encodeToString(plainPayload.getBytes(StandardCharsets.UTF_8));
    }

    @SmackIntegrationTest(section = "2.6.1", quote =
        "the Server sends a <success/> element, which contains an <authorization-identifier/> element containing "
      + "the negotiated identity - this is a bare JID, unless resource binding has occurred, in which case it is a "
      + "full JID.")
    public void testAuthorizationIdentifierIsBareJidWithoutResourceBinding() throws Exception
    {
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();

            // A plain <authenticate/> without a XEP-0386 <bind/> request performs no resource binding.
            final Sasl2Nonza.Authenticate authenticate = new Sasl2Nonza.Authenticate("PLAIN", plainInitialResponse(connection), null);
            final Sasl2Nonza.Success success = connection.sendAndWaitForResponse(authenticate, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class);
            assertNotNull(success, "Expected the service to authenticate using PLAIN.");

            final CharSequence authzid = success.getAuthorizationIdentifier();
            assertNotNull(authzid, "Expected the '<success/>' element to contain an '<authorization-identifier/>' element.");
            final Jid jid = JidCreate.from(authzid);
            assertFalse(jid.hasResource(), "Expected the '<authorization-identifier/>' value ('" + authzid
                + "') to be a bare JID, since no resource binding occurred as part of SASL2 negotiation (but it had a resource).");
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest(section = "2.6.1", quote =
        "the Server sends a <success/> element, which contains an <authorization-identifier/> element containing "
      + "the negotiated identity - this is a bare JID, unless resource binding has occurred, in which case it is a "
      + "full JID.")
    public void testAuthorizationIdentifierIsFullJidWithResourceBinding() throws Exception
    {
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();
            Sasl2TestUtils.requireSasl2(connection); // Bind2 needs the default module set; re-check inline support.

            final Bind2Elements.Bind bind = new Bind2Elements.Bind(null, null);
            final Sasl2Nonza.Authenticate authenticate = new Sasl2Nonza.Authenticate("PLAIN", plainInitialResponse(connection), null, Collections.singletonList(bind));
            final Sasl2Nonza.Success success = connection.sendAndWaitForResponse(authenticate, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class);
            assertNotNull(success, "Expected the service to authenticate using PLAIN with an inline XEP-0386 bind request.");
            assertNotNull(success.getExtension(Bind2Elements.Bound.class), "Expected the '<success/>' element to contain a '<bound/>' element in response to the inline bind request.");

            final CharSequence authzid = success.getAuthorizationIdentifier();
            assertNotNull(authzid, "Expected the '<success/>' element to contain an '<authorization-identifier/>' element.");
            final Jid jid = JidCreate.from(authzid);
            assertTrue(jid.hasResource(), "Expected the '<authorization-identifier/>' value ('" + authzid
                + "') to be a full JID, since resource binding occurred as part of SASL2 negotiation (but it had no resource).");
        } finally {
            connection.disconnect();
        }
    }
}
