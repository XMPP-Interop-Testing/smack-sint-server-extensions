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

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.sasl.packet.Sasl2Feature;
import org.jivesoftware.smack.sasl.packet.Sasl2Nonza;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.igniterealtime.smack.inttest.AbstractSmackSpecificLowLevelIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;

import org.jxmpp.JxmppContext;

/**
 * Tests that verify the behavior of a server around advertisement of the XEP-0388: Extensible SASL Profile (SASL2)
 * stream feature itself (as opposed to the negotiation that happens once it has been offered, which is covered by
 * {@link Sasl2NegotiationIntegrationTest}).
 *
 * This test is placed in the {@code org.jivesoftware.smack} package so that it can access the package-private /
 * protected {@code AbstractXMPPConnection#streamFeatures}-backed feature lookups used here.
 */
@SpecificationReference(document = "XEP-0388", version = "1.0.4")
public class Sasl2FeatureAdvertisementIntegrationTest extends AbstractSmackSpecificLowLevelIntegrationTest<ModularXmppClientToServerConnection> {

    // XEP-0440: SASL Channel-Binding Type Capability. Smack does not (yet) ship a provider for this stream feature,
    // so a minimal one is registered here purely so that its presence/absence can be observed via
    // Connection#hasFeature(QName). Its content is irrelevant to the test, only its presence is.
    private static final String XEP0440_ELEMENT = "sasl-channel-binding";
    private static final String XEP0440_NAMESPACE = "urn:xmpp:sasl-cb:0";
    private static final QName XEP0440_SASL_CHANNEL_BINDING_QNAME = new QName(XEP0440_NAMESPACE, XEP0440_ELEMENT);

    private static final class SaslChannelBindingFeature implements ExtensionElement {
        @Override
        public String getElementName() {
            return XEP0440_ELEMENT;
        }

        @Override
        public String getNamespace() {
            return XEP0440_NAMESPACE;
        }

        @Override
        public CharSequence toXML(XmlEnvironment xmlEnvironment) {
            return "<" + XEP0440_ELEMENT + " xmlns='" + XEP0440_NAMESPACE + "'/>";
        }
    }

    private static final class SaslChannelBindingFeatureProvider extends ExtensionElementProvider<SaslChannelBindingFeature> {
        @Override
        public SaslChannelBindingFeature parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment, JxmppContext jxmppContext)
                        throws XmlPullParserException, IOException {
            // The content of this element is irrelevant to the test that uses it; skip to its end.
            while (!(parser.getEventType() == XmlPullParser.Event.END_ELEMENT && parser.getDepth() == initialDepth)) {
                parser.next();
            }
            return new SaslChannelBindingFeature();
        }
    }

    static {
        ProviderManager.addStreamFeatureProvider(XEP0440_SASL_CHANNEL_BINDING_QNAME, new SaslChannelBindingFeatureProvider());
    }

    @SuppressWarnings("this-escape")
    public Sasl2FeatureAdvertisementIntegrationTest(SmackIntegrationTestEnvironment environment) throws Exception {
        super(environment, ModularXmppClientToServerConnection.class);
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();
            Sasl2TestUtils.requireSasl2(connection);
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest(section = "3", quote =
        "SASL2 MUST only be used by Clients or offered by Servers after TLS negotiation.")
    public void testSasl2NotOfferedPreTls() throws Exception
    {
        // A ConnectionConfigurationBuilderApplier cannot reliably force this one connection to skip TLS: the sinttest
        // framework applies the run's global securityMode *after* any custom applier, so it would simply overwrite
        // a per-test override back to whatever -Dsinttest.securityMode was set to for the whole run. Instead, probe
        // the pre-TLS stream features directly over a raw socket, independent of the run's TLS configuration.
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        final ConnectionConfiguration configuration = connection.getConfiguration();
        final int port = configuration.getPort() != null ? configuration.getPort().intValue() : 5222;
        final String domain = configuration.getXMPPServiceDomain().toString();

        // Build the socket address from the typed accessors directly, rather than ConnectionConfiguration's own
        // getHostString(), which (for a resolved InetAddress) returns InetAddress#toString()'s "/1.2.3.4" form -
        // not a usable hostname.
        final java.net.InetSocketAddress socketAddress;
        if (configuration.getHostAddress() != null) {
            socketAddress = new java.net.InetSocketAddress(configuration.getHostAddress(), port);
        } else if (configuration.getHost() != null) {
            socketAddress = new java.net.InetSocketAddress(configuration.getHost().toString(), port);
        } else {
            socketAddress = new java.net.InetSocketAddress(domain, port);
        }

        // Explicitly avoid the JVM's default ProxySelector: in some environments (observed under 'mvn exec:java')
        // it throws when asked to select a proxy for a plain Socket(host, port) connection.
        try (Socket socket = new Socket(java.net.Proxy.NO_PROXY)) {
            socket.connect(socketAddress, 10000);
            socket.setSoTimeout(10000);
            socket.getOutputStream().write(("<?xml version='1.0'?><stream:stream to='" + domain
                + "' xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' version='1.0'>").getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();

            final String preTlsFeatures = readUntilOrThrow(socket, "</stream:features>",
                "Timed out reading the pre-TLS '<stream:features/>' from '" + socketAddress + "'.");

            if (!preTlsFeatures.contains("urn:ietf:params:xml:ns:xmpp-tls")) {
                throw new TestNotPossibleException("Service does not advertise STARTTLS; cannot verify that SASL2 is withheld until TLS has been negotiated.");
            }

            assertFalse(preTlsFeatures.contains(Sasl2Nonza.NAMESPACE),
                "Expected the service to not advertise the SASL2 '<authentication/>' stream feature ('" + Sasl2Nonza.NAMESPACE
                    + "') before TLS has been negotiated, but the pre-TLS '<stream:features/>' did: " + preTlsFeatures);
        }
    }

    private static String readUntilOrThrow(Socket socket, String marker, String timeoutMessage) throws IOException {
        final StringBuilder buffer = new StringBuilder();
        final char[] chunk = new char[4096];
        final java.io.Reader reader = new java.io.InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
        try {
            while (!buffer.toString().contains(marker)) {
                final int read = reader.read(chunk);
                if (read < 0) {
                    break;
                }
                buffer.append(chunk, 0, read);
            }
        } catch (java.net.SocketTimeoutException e) {
            throw new IOException(timeoutMessage, e);
        }
        return buffer.toString();
    }

    @SmackIntegrationTest(section = "2.1", quote =
        "All servers and clients supporting channel-binding MUST implement [XEP-0440].")
    public void testChannelBindingMechanismsImplyXep0440Advertisement() throws Exception
    {
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();
            final Sasl2Feature sasl2Feature = Sasl2TestUtils.requireSasl2(connection);

            final boolean offersChannelBindingMechanism = sasl2Feature.getMechanisms().stream().anyMatch(mechanism -> mechanism.endsWith("-PLUS"));
            if (!offersChannelBindingMechanism) {
                throw new TestNotPossibleException("Service does not offer any SASL channel-binding ('*-PLUS') mechanism through SASL2.");
            }

            assertTrue(connection.hasFeature(XEP0440_SASL_CHANNEL_BINDING_QNAME),
                "Expected the service, which offers channel-binding SASL mechanisms (" + sasl2Feature.getMechanisms()
                    + "), to also advertise XEP-0440 support via a '<sasl-channel-binding/>' stream feature (but it did not).");
        } finally {
            connection.disconnect();
        }
    }
}
