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
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;

import org.igniterealtime.smack.inttest.AbstractSmackSpecificLowLevelIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;

/**
 * Tests that verify the behavior of a server around XEP-0388: Extensible SASL Profile (SASL2) negotiation, for
 * checks that need to put raw, non-XML-element bytes on the wire (e.g. a bare whitespace character) - something
 * Smack's connection object cannot do, since only structured Nonza/Stanza objects can be sent through it.
 *
 * This is kept separate from {@link Sasl2NegotiationIntegrationTest}, whose other checks all drive the connection
 * object directly, so that the raw-socket/TLS plumbing needed here doesn't clutter those simpler tests.
 */
@SpecificationReference(document = "XEP-0388", version = "1.0.4")
public class Sasl2NegotiationLowLevelIntegrationTest extends AbstractSmackSpecificLowLevelIntegrationTest<ModularXmppClientToServerConnection> {

    @SuppressWarnings("this-escape")
    public Sasl2NegotiationLowLevelIntegrationTest(SmackIntegrationTestEnvironment environment) throws Exception {
        super(environment, ModularXmppClientToServerConnection.class);
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();
            Sasl2TestUtils.requireSasl2(connection, "PLAIN");
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest(section = "2.4", quote =
        "Clients MUST NOT send whitespace [...] Servers MUST disconnect Clients immediately if any other traffic is received.")
    public void testWhitespaceDuringNegotiationCausesDisconnect() throws Exception
    {
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        final ConnectionConfiguration configuration = connection.getConfiguration();
        final int port = configuration.getPort() != null ? configuration.getPort().intValue() : 5222;
        final String domain = configuration.getXMPPServiceDomain().toString();
        final InetSocketAddress socketAddress = configuration.getHostAddress() != null
            ? new InetSocketAddress(configuration.getHostAddress(), port)
            : new InetSocketAddress(configuration.getHost() != null ? configuration.getHost().toString() : domain, port);

        final Socket plainSocket = new Socket(Proxy.NO_PROXY);
        plainSocket.connect(socketAddress, 10000);
        plainSocket.setSoTimeout(10000);

        final String openStream = "<?xml version='1.0'?><stream:stream to='" + domain
            + "' xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' version='1.0'>";
        writeRaw(plainSocket, openStream);
        readRawUntil(plainSocket, "</stream:features>");

        writeRaw(plainSocket, "<starttls xmlns='urn:ietf:params:xml:ns:xmpp-tls'/>");
        readRawUntil(plainSocket, "proceed");

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[] {new AcceptAllTrustManager()}, null);
        final SSLSocket sslSocket = (SSLSocket) sslContext.getSocketFactory().createSocket(plainSocket, domain, plainSocket.getPort(), true);
        sslSocket.setSoTimeout(10000);
        sslSocket.startHandshake();

        try {
            final Reader reader = new InputStreamReader(sslSocket.getInputStream(), StandardCharsets.UTF_8);
            final StringBuilder buffer = new StringBuilder();

            writeRaw(sslSocket, openStream);
            readUntil(reader, buffer, "</stream:features>");

            // Start a negotiation without an initial response, so that the server is waiting for our next element.
            writeRaw(sslSocket, "<authenticate xmlns='urn:xmpp:sasl:2' mechanism='PLAIN'/>");
            final String challengeResponse = readUntil(reader, buffer, "challenge");
            assertTrue(challengeResponse.contains("<challenge"), "Expected the service to request SASL data through "
                + "a '<challenge/>' element, after an '<authenticate/>' element without an initial response. Data "
                + "received: " + challengeResponse);

            // Instead of a <response/> or <abort/>, send a single whitespace character.
            writeRaw(sslSocket, " ");

            boolean disconnected = false;
            final char[] chunk = new char[4096];
            final long deadline = System.currentTimeMillis() + 10000;
            try {
                while (System.currentTimeMillis() < deadline) {
                    final int read = reader.read(chunk);
                    if (read < 0) {
                        disconnected = true;
                        break;
                    }
                    // Keep draining; a stream error / closing tag may arrive before the socket actually closes.
                }
            } catch (SocketTimeoutException e) {
                // No reaction at all within the window; disconnected remains false.
            } catch (IOException e) {
                // E.g. a connection reset also counts as having been disconnected.
                disconnected = true;
            }

            assertTrue(disconnected, "Expected the service to disconnect the connection, after a single whitespace "
                + "character was sent while a SASL2 negotiation was in progress, but the connection remained open "
                + "for 10 seconds.");
        } finally {
            sslSocket.close();
        }
    }

    private static void writeRaw(Socket socket, String xml) throws IOException {
        socket.getOutputStream().write(xml.getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().flush();
    }

    private static void readRawUntil(Socket socket, String marker) throws IOException {
        final Reader reader = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
        readUntil(reader, new StringBuilder(), marker);
    }

    private static String readUntil(Reader reader, StringBuilder buffer, String marker) throws IOException {
        final char[] chunk = new char[4096];
        try {
            while (!buffer.toString().contains(marker)) {
                final int read = reader.read(chunk);
                if (read < 0) {
                    break;
                }
                buffer.append(chunk, 0, read);
            }
        } catch (SocketTimeoutException e) {
            throw new IOException("Timed out waiting for '" + marker + "'. Data received so far: " + buffer, e);
        }
        final String result = buffer.toString();
        buffer.setLength(0);
        return result;
    }

    private static final class AcceptAllTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
        }

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[0];
        }
    }
}
