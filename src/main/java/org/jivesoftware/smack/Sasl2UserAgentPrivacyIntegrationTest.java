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

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.jivesoftware.smack.bind2.element.Bind2Elements;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.sasl.packet.Sasl2Nonza;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.stringencoder.Base64;

import org.igniterealtime.smack.inttest.AbstractSmackSpecificLowLevelIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;

import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

/**
 * Tests that verify a server does not leak the software/device information supplied in a XEP-0388: Extensible SASL
 * Profile (SASL2) {@code <user-agent/>} element to other entities.
 *
 * This test is placed in the {@code org.jivesoftware.smack} package so that it can drive a raw SASL2 negotiation
 * that includes an explicit {@code <user-agent/>} element, which the high-level login() API in this version of
 * Smack does not (yet) send by default.
 */
@SpecificationReference(document = "XEP-0388", version = "1.0.4")
public class Sasl2UserAgentPrivacyIntegrationTest extends AbstractSmackSpecificLowLevelIntegrationTest<ModularXmppClientToServerConnection> {

    @SuppressWarnings("this-escape")
    public Sasl2UserAgentPrivacyIntegrationTest(SmackIntegrationTestEnvironment environment) throws Exception {
        super(environment, ModularXmppClientToServerConnection.class);
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();
            Sasl2TestUtils.requireSasl2(connection, "PLAIN");
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest(section = "2.3", quote =
        "Servers MUST NOT expose this information to other entities.")
    public void testUserAgentInfoNotLeakedToOtherEntities() throws Exception
    {
        final List<ModularXmppClientToServerConnection> connections = getSpecificUnconnectedConnections(2);
        final ModularXmppClientToServerConnection connectionOne = connections.get(0);
        final ModularXmppClientToServerConnection connectionTwo = connections.get(1);
        try {
            connectionOne.connect();

            final String marker = "smack-sint-marker-" + StringUtils.insecureRandomString(16);
            final Sasl2Nonza.UserAgent userAgent = new Sasl2Nonza.UserAgent(UUID.randomUUID().toString(), marker, marker);
            final CharSequence username = connectionOne.getConfiguration().getUsername();
            final String password = connectionOne.getConfiguration().getPassword();
            final String plainPayload = '\0' + username.toString() + '\0' + password;
            final String initialResponse = Base64.encodeToString(plainPayload.getBytes(StandardCharsets.UTF_8));
            // Also bind a resource, since a bare-JID IQ target with no bound resource is bounced by the server
            // itself (service-unavailable) before it could ever reach (or fail to reach) the queried entity.
            final Bind2Elements.Bind bind = new Bind2Elements.Bind(null, null);
            final Sasl2Nonza.Authenticate authenticate = new Sasl2Nonza.Authenticate("PLAIN", initialResponse, userAgent, Collections.singletonList(bind));
            final Sasl2Nonza.Success success = connectionOne.sendAndWaitForResponse(authenticate, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class);
            assertNotNull(success, "Expected the service to authenticate '" + username + "' using PLAIN with a '<user-agent/>' element and an inline bind request.");

            final Jid connectionOneJid = JidCreate.from(success.getAuthorizationIdentifier());

            // Authenticate the second connection 'manually' too, to sidestep automatic mechanism selection picking
            // a channel-binding ('*-PLUS') mechanism, which is otherwise irrelevant to this test. A resource is
            // bound (via an inline XEP-0386 request) since ServiceDiscoveryManager needs a full JID to be set as
            // this connection's user, which going through the raw nonza API (rather than login()) does not do for
            // us automatically.
            connectionTwo.connect();
            final CharSequence usernameTwo = connectionTwo.getConfiguration().getUsername();
            final String passwordTwo = connectionTwo.getConfiguration().getPassword();
            final String plainPayloadTwo = '\0' + usernameTwo.toString() + '\0' + passwordTwo;
            final String initialResponseTwo = Base64.encodeToString(plainPayloadTwo.getBytes(StandardCharsets.UTF_8));
            final Bind2Elements.Bind bindTwo = new Bind2Elements.Bind(null, null);
            final Sasl2Nonza.Authenticate authenticateTwo = new Sasl2Nonza.Authenticate("PLAIN", initialResponseTwo, null, Collections.singletonList(bindTwo));
            final Sasl2Nonza.Success successTwo = connectionTwo.sendAndWaitForResponse(authenticateTwo, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class);
            assertNotNull(successTwo, "Expected the service to authenticate '" + usernameTwo + "' using PLAIN with an inline bind request.");
            final EntityFullJid fullJidTwo = JidCreate.from(successTwo.getAuthorizationIdentifier()).asEntityFullJidIfPossible();
            assertNotNull(fullJidTwo, "Expected the second connection's '<authorization-identifier/>' ('" + successTwo.getAuthorizationIdentifier() + "') to be a full JID.");
            connectionTwo.user = fullJidTwo;

            final DiscoverInfo discoverInfo = ServiceDiscoveryManager.getInstanceFor(connectionTwo).discoverInfo(connectionOneJid);
            final String rawResponse = discoverInfo.toXML().toString();

            assertFalse(rawResponse.contains(marker), "Expected a disco#info response about '" + connectionOneJid
                + "', queried by a different entity ('" + usernameTwo + "'), to not contain the "
                + "distinctive software/device value ('" + marker + "') supplied in that account's SASL2 "
                + "'<user-agent/>' element (but it did).");
        } finally {
            connectionOne.disconnect();
            connectionTwo.disconnect();
        }
    }
}
