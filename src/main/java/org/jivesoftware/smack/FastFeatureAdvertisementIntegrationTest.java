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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.fast.element.FastElements;
import org.jivesoftware.smack.sasl.packet.Sasl2Feature;

import org.igniterealtime.smack.inttest.AbstractSmackSpecificLowLevelIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;

/**
 * Tests that verify the behavior of a server around advertisement of the XEP-0484: Fast Authentication Streamlining
 * Tokens (FAST) inline feature itself, as opposed to the negotiation/token lifecycle covered by
 * {@link FastNegotiationIntegrationTest} and {@link FastLowLevelIntegrationTest}.
 */
@SpecificationReference(document = "XEP-0484", version = "0.2.0")
public class FastFeatureAdvertisementIntegrationTest extends AbstractSmackSpecificLowLevelIntegrationTest<ModularXmppClientToServerConnection> {

    @SuppressWarnings("this-escape")
    public FastFeatureAdvertisementIntegrationTest(SmackIntegrationTestEnvironment environment) throws Exception {
        super(environment, ModularXmppClientToServerConnection.class);
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();
            FastTestUtils.requireFast(connection);
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest(section = "Security Considerations", quote =
        "FAST authentication MUST only be performed over a secure connection (e.g. using TLS with verified certificates).")
    public void testFastNotOfferedPreTls() throws Exception
    {
        // Probed over a raw socket, independent of the run's global securityMode: the sinttest framework applies
        // that setting *after* any per-test ConnectionConfigurationBuilderApplier, so a builder override here
        // cannot reliably force this one connection to skip TLS (see Sasl2FeatureAdvertisementIntegrationTest for
        // the same reasoning, which this test mirrors).
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        final ConnectionConfiguration configuration = connection.getConfiguration();
        final int port = configuration.getPort() != null ? configuration.getPort().intValue() : 5222;
        final String domain = configuration.getXMPPServiceDomain().toString();
        final InetSocketAddress socketAddress = configuration.getHostAddress() != null
            ? new InetSocketAddress(configuration.getHostAddress(), port)
            : new InetSocketAddress(configuration.getHost() != null ? configuration.getHost().toString() : domain, port);

        try (Socket socket = new Socket(Proxy.NO_PROXY)) {
            socket.connect(socketAddress, 10000);
            socket.setSoTimeout(10000);
            socket.getOutputStream().write(("<?xml version='1.0'?><stream:stream to='" + domain
                + "' xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' version='1.0'>").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            socket.getOutputStream().flush();

            final String preTlsFeatures = readUntilOrThrow(socket, "</stream:features>",
                "Timed out reading the pre-TLS '<stream:features/>' from '" + socketAddress + "'.");

            if (!preTlsFeatures.contains("urn:ietf:params:xml:ns:xmpp-tls")) {
                throw new TestNotPossibleException("Service does not advertise STARTTLS; cannot verify that FAST is withheld until TLS has been negotiated.");
            }

            assertFalse(preTlsFeatures.contains(FastElements.NAMESPACE),
                "Expected the service to not advertise the FAST '<fast/>' inline feature ('" + FastElements.NAMESPACE
                    + "') before TLS has been negotiated, but the pre-TLS '<stream:features/>' did: " + preTlsFeatures);
        }
    }

    private static String readUntilOrThrow(Socket socket, String marker, String timeoutMessage) throws java.io.IOException {
        final StringBuilder buffer = new StringBuilder();
        final char[] chunk = new char[4096];
        final java.io.Reader reader = new java.io.InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8);
        try {
            while (!buffer.toString().contains(marker)) {
                final int read = reader.read(chunk);
                if (read < 0) {
                    break;
                }
                buffer.append(chunk, 0, read);
            }
        } catch (java.net.SocketTimeoutException e) {
            throw new java.io.IOException(timeoutMessage, e);
        }
        return buffer.toString();
    }

    @SmackIntegrationTest(section = "Server advertises support for FAST", quote =
        "There SHOULD be at least one mechanism capable of channel binding, and there SHOULD be at least one "
      + "mechanism without channel binding.")
    public void testAdvertisedMechanismsIncludeBothChannelBindingTypes() throws Exception
    {
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();
            final FastElements.Fast fast = FastTestUtils.requireFast(connection);

            final boolean hasChannelBindingCapable = fast.getMechanisms().stream().anyMatch(m -> !m.endsWith("-NONE"));
            final boolean hasNonChannelBinding = fast.getMechanisms().stream().anyMatch(m -> m.endsWith("-NONE"));

            assertTrue(hasChannelBindingCapable, "Expected at least one advertised FAST mechanism capable of "
                + "channel binding (i.e. not ending in '-NONE'), among: " + fast.getMechanisms());
            assertTrue(hasNonChannelBinding, "Expected at least one advertised FAST mechanism without channel "
                + "binding (i.e. ending in '-NONE'), among: " + fast.getMechanisms());
        } finally {
            connection.disconnect();
        }
    }
}
