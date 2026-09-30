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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.stringencoder.Base64;

import org.igniterealtime.smack.inttest.AbstractSmackSpecificLowLevelIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.RawXmppConnection;

/**
 * Tests that verify a server's handling of features requested inline in a XEP-0386: Bind 2 {@code <bind/>} request,
 * as defined by XEP-0386's interactions with XEP-0198 (Stream Management) and XEP-0280 (Message Carbons).
 *
 * These tests speak raw XML over a socket, rather than using Smack's connection object, because the specific
 * checks performed here (inspecting the exact content of a {@code <bound/>} response, and observing a Carbons copy
 * arrive out of band) are not representable through Smack's current object model for {@code Bind2Elements.Bound}
 * (which only exposes MAM metadata, discarding other children), and because registering a replacement provider for
 * it would change parsing behavior for every other test in the same JVM process.
 */
@SpecificationReference(document = "XEP-0386", version = "1.1.0")
public class Bind2InlineFeatureIntegrationTest extends AbstractSmackSpecificLowLevelIntegrationTest<ModularXmppClientToServerConnection> {

    @SuppressWarnings("this-escape")
    public Bind2InlineFeatureIntegrationTest(SmackIntegrationTestEnvironment environment) throws Exception {
        super(environment, ModularXmppClientToServerConnection.class);
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();
            Sasl2TestUtils.requireSasl2(connection, "PLAIN");
        } finally {
            connection.disconnect();
        }
    }

    /** Connects and authenticates using the credentials of a freshly-registered, dedicated test account. */
    private AuthenticatedConnection connectAndAuthenticate(String bindInlineXml) throws Exception {
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        return connectAndAuthenticate(connection.getConfiguration(), connection.getConfiguration().getUsername(),
            connection.getConfiguration().getPassword(), bindInlineXml);
    }

    /**
     * Connects and authenticates using the given, explicit credentials - so that two raw connections can act as two
     * resources of the *same* account (each call to {@link #getSpecificUnconnectedConnection()} registers and
     * returns a distinct, freshly-registered account, so it cannot be used directly to obtain a second resource for
     * an already-used account).
     */
    private AuthenticatedConnection connectAndAuthenticate(ConnectionConfiguration configuration, CharSequence username, String password, String bindInlineXml) throws Exception {
        final RawXmppConnection raw = RawXmppConnection.connect(configuration, sinttestConfiguration);
        try {
            final String plainPayload = '\0' + username.toString() + '\0' + password;
            final String initialResponse = Base64.encodeToString(plainPayload.getBytes(StandardCharsets.UTF_8));

            final String authenticateXml = "<authenticate xmlns='urn:xmpp:sasl:2' mechanism='PLAIN'>"
                + "<initial-response>" + initialResponse + "</initial-response>"
                + "<bind xmlns='urn:xmpp:bind:0'>" + bindInlineXml + "</bind>"
                + "</authenticate>";
            raw.send(authenticateXml);
            final String response = raw.readUntil("</success>");
            return new AuthenticatedConnection(raw, response);
        } catch (Exception e) {
            raw.close();
            throw e;
        }
    }

    /** A raw connection that has authenticated, together with the {@code <success/>} response of the server. */
    private static final class AuthenticatedConnection implements AutoCloseable {
        private final RawXmppConnection raw;
        final String successResponse;

        AuthenticatedConnection(RawXmppConnection raw, String successResponse) {
            this.raw = raw;
            this.successResponse = successResponse;
        }

        void send(String xml) throws IOException {
            raw.send(xml);
        }

        String readWhatArrives(int timeoutMillis) throws IOException {
            return raw.readWhatArrives(timeoutMillis);
        }

        @Override
        public void close() throws IOException {
            raw.close();
        }
    }

    @SmackIntegrationTest(section = "3.2", quote =
        "The <bound/> response MUST contain the resulting XEP-0198 <enabled/> (or <failed/>) element.")
    public void testStreamManagementEnableReflectedInBound() throws Exception
    {
        try (AuthenticatedConnection connection = connectAndAuthenticate("<enable xmlns='urn:xmpp:sm:3'/>")) {
            final String response = connection.successResponse;
            final boolean hasEnabled = response.contains("xmlns=\"urn:xmpp:sm:3\"") || response.contains("xmlns='urn:xmpp:sm:3'");
            final boolean hasEnabledOrFailedTag = response.contains("<enabled") || response.contains("<failed");
            assertTrue(hasEnabled && hasEnabledOrFailedTag,
                "Expected the '<success/>' response to a bind request that included an inline XEP-0198 '<enable/>' "
                    + "element to contain a resulting '<enabled/>' (or '<failed/>') element in the 'urn:xmpp:sm:3' "
                    + "namespace, but it did not. Full response: " + response);
        }
    }

    @SmackIntegrationTest(section = "3.2", quote = "Message Carbons MUST be enabled at this point.")
    public void testCarbonsActuallyEnabled() throws Exception
    {
        // Two resources of the SAME account (both Carbons-enabled), plus a genuinely separate, third-party account
        // that sends the triggering message - matching XEP-0280's own "Receiving Messages" example (Juliet sends
        // Romeo a message; Romeo's other Carbons-enabled resource receives a forwarded copy).
        final ModularXmppClientToServerConnection recipientAccount = getSpecificUnconnectedConnection();
        final ConnectionConfiguration recipientConfiguration = recipientAccount.getConfiguration();
        final CharSequence recipientUsername = recipientConfiguration.getUsername();
        final String recipientPassword = recipientConfiguration.getPassword();
        final String recipientBareJid = recipientUsername + "@" + recipientConfiguration.getXMPPServiceDomain();

        try (AuthenticatedConnection resourceA = connectAndAuthenticate(recipientConfiguration, recipientUsername, recipientPassword,
                 "<tag>carbons-a</tag><enable xmlns='urn:xmpp:carbons:2'/>");
             AuthenticatedConnection resourceB = connectAndAuthenticate(recipientConfiguration, recipientUsername, recipientPassword,
                 "<tag>carbons-b</tag><enable xmlns='urn:xmpp:carbons:2'/>");
             AuthenticatedConnection sender = connectAndAuthenticate("")) {

            // Mark all resources available: a Carbons copy is only pushed live to available resources, and RFC 6121
            // bare-JID delivery needs an available resource to route the triggering message to in the first place.
            resourceA.send("<presence/>");
            resourceB.send("<presence/>");
            sender.send("<presence/>");
            Thread.sleep(1000);

            final String marker = "smack-sint-carbons-marker-" + StringUtils.insecureRandomString(16);
            // The third-party sender addresses resource A's bare JID; resource B (also Carbons-enabled, same
            // account) should receive a forwarded '<received/>' copy, per XEP-0280 "Receiving Messages".
            sender.send("<message type='chat' to='" + recipientBareJid + "'><body>" + marker + "</body></message>");

            final StringBuilder received = new StringBuilder();
            final long deadline = System.currentTimeMillis() + 10000;
            while (System.currentTimeMillis() < deadline && !received.toString().contains(marker)) {
                received.append(resourceB.readWhatArrives(2000));
            }

            assertTrue(received.toString().contains(marker),
                "Expected resource B (which enabled XEP-0280 Message Carbons inline during Bind 2) to receive a "
                    + "carbon copy of a message a third party sent to resource A's bare JID ('" + recipientBareJid
                    + "', the same account as resource B, also Carbons-enabled), but no such copy arrived within 10 "
                    + "seconds. Data received on resource B's connection: " + received);
        }
    }
}
