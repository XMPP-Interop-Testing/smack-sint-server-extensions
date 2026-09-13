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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.jivesoftware.smack.XMPPException.FailedNonzaException;
import org.jivesoftware.smack.bind2.element.Bind2Elements;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.sasl.packet.Sasl2Nonza;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.stringencoder.Base64;

import org.igniterealtime.smack.inttest.AbstractSmackSpecificLowLevelIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;

import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

/**
 * Tests that verify the behavior of a server around processing a XEP-0386: Bind 2 {@code <bind/>} request that is
 * included inline in a XEP-0388: Extensible SASL Profile (SASL2) {@code <authenticate/>} element.
 */
@SpecificationReference(document = "XEP-0386", version = "1.1.0")
public class Bind2NegotiationIntegrationTest extends AbstractSmackSpecificLowLevelIntegrationTest<ModularXmppClientToServerConnection> {

    @SuppressWarnings("this-escape")
    public Bind2NegotiationIntegrationTest(SmackIntegrationTestEnvironment environment) throws Exception {
        super(environment, ModularXmppClientToServerConnection.class);
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();
            Sasl2TestUtils.requireSasl2(connection, "PLAIN");
        } finally {
            connection.disconnect();
        }
    }

    private static String plainInitialResponse(CharSequence username, String password) {
        final String plainPayload = '\0' + username.toString() + '\0' + password;
        return Base64.encodeToString(plainPayload.getBytes(StandardCharsets.UTF_8));
    }

    /** A minimal, empty {@code <name xmlns='namespace'/>} element, for building small inline bind requests. */
    private static final class SimpleElement implements XmlElement {
        private final String elementName;
        private final String namespace;

        SimpleElement(String elementName, String namespace) {
            this.elementName = elementName;
            this.namespace = namespace;
        }

        @Override
        public String getElementName() {
            return elementName;
        }

        @Override
        public String getNamespace() {
            return namespace;
        }

        @Override
        public CharSequence toXML(XmlEnvironment xmlEnvironment) {
            return "<" + elementName + " xmlns='" + namespace + "'/>";
        }
    }

    @SmackIntegrationTest(section = "3.2", quote =
        "the bind request MUST NOT be processed (i.e. it should be ignored) if the authentication is not successful.")
    public void testBindNotProcessedOnFailedAuth() throws Exception
    {
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();

            final CharSequence username = connection.getConfiguration().getUsername();
            final String password = connection.getConfiguration().getPassword();

            // A failed authentication attempt that also requests a bind.
            final String invalidPassword = "invalid-password-" + StringUtils.insecureRandomString(16);
            final Bind2Elements.Bind bind = new Bind2Elements.Bind(null, null);
            final Sasl2Nonza.Authenticate badAuthenticate = new Sasl2Nonza.Authenticate("PLAIN",
                plainInitialResponse(username, invalidPassword), null, Collections.singletonList(bind));
            assertThrows(FailedNonzaException.class,
                () -> connection.sendAndWaitForResponse(badAuthenticate, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class),
                "Expected the failed authentication attempt (with an inline bind request) to result in a '<failure/>' element.");

            // A subsequent, successful authentication on the same stream that does NOT itself request a bind. If the
            // earlier bind request had incorrectly been processed despite the failed authentication, a resource
            // would already be bound, and this '<authorization-identifier/>' would be a full JID instead of bare.
            final Sasl2Nonza.Authenticate goodAuthenticate = new Sasl2Nonza.Authenticate("PLAIN", plainInitialResponse(username, password), null);
            final Sasl2Nonza.Success success = connection.sendAndWaitForResponse(goodAuthenticate, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class);
            assertNotNull(success, "Expected the service to authenticate '" + username + "' using the correct password.");

            final Jid jid = JidCreate.from(success.getAuthorizationIdentifier());
            assertFalse(jid.hasResource(), "Expected the '<authorization-identifier/>' value ('" + jid
                + "') to be a bare JID, since this successful authentication request did not itself include a bind "
                + "request - a resource here would indicate that the earlier failed attempt's bind request had "
                + "incorrectly been processed.");
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest(section = "3.2.1", quote =
        "The SASL2 <user-agent> id itself MUST NOT be exposed by the server in the generated resource identifier.")
    public void testUserAgentIdNotExposedInGeneratedResourceIdentifier() throws Exception
    {
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();

            final String userAgentId = UUID.randomUUID().toString();
            final Sasl2Nonza.UserAgent userAgent = new Sasl2Nonza.UserAgent(userAgentId, "smack-sint-server-extensions", "test-device");
            // No <tag/>, so the resource identifier is fully server-generated.
            final Bind2Elements.Bind bind = new Bind2Elements.Bind(null, null);
            final CharSequence username = connection.getConfiguration().getUsername();
            final String password = connection.getConfiguration().getPassword();
            final Sasl2Nonza.Authenticate authenticate = new Sasl2Nonza.Authenticate("PLAIN",
                plainInitialResponse(username, password), userAgent, Collections.singletonList(bind));
            final Sasl2Nonza.Success success = connection.sendAndWaitForResponse(authenticate, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class);
            assertNotNull(success, "Expected the service to authenticate '" + username + "' using PLAIN with a '<user-agent/>' element and an inline bind request.");

            final Jid jid = JidCreate.from(success.getAuthorizationIdentifier());
            assertTrue(jid.hasResource(), "Test setup error: expected the '<authorization-identifier/>' ('" + jid + "') to have a bound resource.");
            final String resource = jid.getResourceOrEmpty().toString();
            assertFalse(resource.contains(userAgentId), "Expected the generated resource identifier ('" + resource
                + "') to not contain the SASL2 '<user-agent id=\"...\"/>' value ('" + userAgentId + "') supplied during authentication (but it did).");
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest(section = "3.2", quote =
        "If the client included a XEP-0352 <active/> or <inactive/> element, the session must begin in that state.")
    public void testCsiStateAcceptedInline() throws Exception
    {
        // Note: this only verifies that the service accepts and processes an inline XEP-0352 request without
        // erroring; it is not practical to verify from the client side that the session's internal CSI state
        // ('active'/'inactive', which mainly affects server-side traffic throttling) was actually applied.
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();

            final SimpleElement inactive = new SimpleElement("inactive", "urn:xmpp:csi:0");
            final Bind2Elements.Bind bind = new Bind2Elements.Bind(null, Collections.singletonList(inactive));
            final CharSequence username = connection.getConfiguration().getUsername();
            final String password = connection.getConfiguration().getPassword();
            final Sasl2Nonza.Authenticate authenticate = new Sasl2Nonza.Authenticate("PLAIN",
                plainInitialResponse(username, password), null, Collections.singletonList(bind));
            final Sasl2Nonza.Success success = connection.sendAndWaitForResponse(authenticate, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class);
            assertNotNull(success, "Expected the service to authenticate '" + username
                + "' using PLAIN with an inline bind request that included a XEP-0352 '<inactive/>' element.");
            assertNotNull(success.getExtension(Bind2Elements.Bound.class),
                "Expected a '<bound/>' element in the '<success/>' response to a bind request that included a XEP-0352 '<inactive/>' element.");
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest(section = "3.2", quote =
        "Clear the offline messages for this user, if any, without sending them (as they will be provided by MAM).")
    public void testOfflineMessagesNotDeliveredOnBind() throws Exception
    {
        final List<ModularXmppClientToServerConnection> connections = getSpecificUnconnectedConnections(2);
        final ModularXmppClientToServerConnection sender = connections.get(0);
        final ModularXmppClientToServerConnection recipient = connections.get(1);
        try {
            sender.connect();
            sender.login();

            final EntityBareJid recipientBareJid = JidCreate.entityBareFrom(
                recipient.getConfiguration().getUsername() + "@" + recipient.getConfiguration().getXMPPServiceDomain());

            // Send a message to the recipient while it is genuinely offline (never yet connected), so the server
            // has to queue it rather than deliver it live.
            final String marker = "smack-sint-offline-marker-" + StringUtils.insecureRandomString(16);
            final Message offlineMessage = sender.getStanzaFactory().buildMessageStanza()
                .to(recipientBareJid).ofType(Message.Type.chat).setBody(marker).build();
            sender.sendStanza(offlineMessage);
            Thread.sleep(1000); // give the server a moment to store the offline message before the recipient binds.

            // Bind the recipient via Bind 2 (this also sends the connection's initial available presence, by
            // default, as part of login()) and listen for the marker message arriving unprompted - whether
            // immediately upon bind, or upon presence, which is the classic legacy trigger for an offline-message
            // flood, in case an implementation only moved *part* of its flush logic to this newer requirement.
            recipient.connect();
            final StanzaFilter markerFilter = stanza -> stanza instanceof Message && marker.equals(((Message) stanza).getBody());
            try (StanzaCollector collector = recipient.createStanzaCollector(markerFilter)) {
                recipient.login();

                final Stanza delivered = collector.nextResult(5000);
                assertNull(delivered, "Expected the previously-queued offline message (marker '" + marker + "') to "
                    + "not be delivered at all as a result of binding via Bind 2 (including the connection's "
                    + "initial presence, sent automatically as part of login()), since the server MUST clear "
                    + "queued offline messages without sending them. Received: " + delivered);
            }
        } finally {
            sender.disconnect();
            recipient.disconnect();
        }
    }
}
