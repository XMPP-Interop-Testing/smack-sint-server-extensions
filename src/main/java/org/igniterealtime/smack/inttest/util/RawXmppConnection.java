/*
 * Copyright 2026 Dan Caseley
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
package org.igniterealtime.smack.inttest.util;

import org.igniterealtime.smack.inttest.Configuration;
import org.jivesoftware.smack.ConnectionConfiguration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A client-to-server XMPP stream that exchanges raw XML over a socket, for tests that need to send or inspect data in
 * a way that Smack's connection API does not allow (for example, deliberately non-compliant data, or attributes that
 * Smack's parsers discard).
 *
 * A new instance has opened a stream, and has negotiated TLS (using STARTTLS) if the server offers it and the test
 * configuration does not disable security. The stream is not authenticated.
 *
 * Socket timeouts use the configured reply timeout. TLS certificates are only accepted without validation when the
 * test configuration says so ('acceptAllCertificates').
 */
public final class RawXmppConnection implements AutoCloseable
{
    private static final Pattern STREAM_FEATURES = Pattern.compile("<stream:features(?:\\s[^>]*)?(?:/>|>.*?</stream:features>)", Pattern.DOTALL);

    private final Socket socket;
    private final Reader reader;
    private final StringBuilder buffer = new StringBuilder();
    private final String domain;
    private final String streamFeatures;

    private RawXmppConnection(final Socket socket, final String domain) throws IOException
    {
        this.socket = socket;
        this.domain = domain;
        this.reader = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
        this.streamFeatures = openStream();
    }

    /**
     * Connects to the server described by the connection configuration and opens a stream, negotiating TLS if the
     * server offers it (unless the test configuration disables security).
     *
     * @param connectionConfiguration defines the server to connect to.
     * @param sinttestConfiguration defines the timeouts, security mode and certificate validation to use.
     * @return an open, unauthenticated stream.
     */
    public static RawXmppConnection connect(final ConnectionConfiguration connectionConfiguration, final Configuration sinttestConfiguration) throws IOException, GeneralSecurityException
    {
        final int port = connectionConfiguration.getPort() != null ? connectionConfiguration.getPort().intValue() : 5222;
        final String domain = connectionConfiguration.getXMPPServiceDomain().toString();
        // Don't use getHostString(): for a host that is an IP address, it returns "/127.0.0.1".
        final InetSocketAddress socketAddress = connectionConfiguration.getHostAddress() != null
            ? new InetSocketAddress(connectionConfiguration.getHostAddress(), port)
            : new InetSocketAddress(connectionConfiguration.getHost() != null ? connectionConfiguration.getHost().toString() : domain, port);
        final int timeout = sinttestConfiguration.replyTimeout;

        // Proxy.NO_PROXY avoids the default ProxySelector, which can fail with 'Failed to select a proxy' in some
        // environments.
        final Socket plainSocket = new Socket(Proxy.NO_PROXY);
        try {
            plainSocket.connect(socketAddress, timeout);
            plainSocket.setSoTimeout(timeout);

            final RawXmppConnection plain = new RawXmppConnection(plainSocket, domain);
            if (!plain.streamFeatures.contains("urn:ietf:params:xml:ns:xmpp-tls") || sinttestConfiguration.securityMode == ConnectionConfiguration.SecurityMode.disabled) {
                return plain;
            }

            plain.send("<starttls xmlns='urn:ietf:params:xml:ns:xmpp-tls'/>");
            plain.readUntil("proceed");

            final SSLContext sslContext;
            if (sinttestConfiguration.acceptAllCertificates) {
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[] { new AcceptAllTrustManager() }, null);
            } else {
                sslContext = SSLContext.getDefault();
            }
            final SSLSocket sslSocket = (SSLSocket) sslContext.getSocketFactory().createSocket(plainSocket, domain, port, true);
            sslSocket.setSoTimeout(timeout);
            sslSocket.startHandshake();
            return new RawXmppConnection(sslSocket, domain);
        } catch (IOException | GeneralSecurityException | RuntimeException e) {
            plainSocket.close();
            throw e;
        }
    }

    /**
     * The XMPP domain that this stream is opened to.
     */
    public String getDomain()
    {
        return domain;
    }

    /**
     * The stream features that the server sent after the (most recent) stream header.
     */
    public String getStreamFeatures()
    {
        return streamFeatures;
    }

    /**
     * Sends a stream header, and returns the stream features that the server sends in response.
     */
    private String openStream() throws IOException
    {
        send("<?xml version='1.0'?><stream:stream to='" + domain + "' xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' version='1.0'>");
        return read(STREAM_FEATURES);
    }

    /**
     * Sends raw data.
     */
    public void send(final String xml) throws IOException
    {
        socket.getOutputStream().write(xml.getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().flush();
    }

    /**
     * Reads data until the marker is received, and returns all data up to and including the marker. Data received
     * after the marker is kept for later reads.
     *
     * @throws SocketTimeoutException if the marker is not received within the reply timeout.
     */
    public String readUntil(final String marker) throws IOException
    {
        final Matcher matcher = await(Pattern.compile(Pattern.quote(marker)));
        final String result = buffer.substring(0, matcher.end());
        buffer.delete(0, matcher.end());
        return result;
    }

    /**
     * Reads data until the pattern is matched, and returns the matched text. Data up to the end of the match is
     * consumed, and data received after the match is kept for later reads.
     *
     * @throws SocketTimeoutException if no match is received within the reply timeout.
     */
    public String read(final Pattern pattern) throws IOException
    {
        final Matcher matcher = await(pattern);
        final String result = matcher.group();
        buffer.delete(0, matcher.end());
        return result;
    }

    /**
     * Sends an IQ request, and returns the (complete) IQ stanza with the provided stanza ID that is received in
     * response.
     *
     * @throws SocketTimeoutException if no response is received within the reply timeout.
     */
    public String sendIq(final String iq, final String id) throws IOException
    {
        send(iq);
        final String idAttribute = "id=['\"]" + Pattern.quote(id) + "['\"]";
        return read(Pattern.compile("<iq(?=[^>]*" + idAttribute + ")[^>]*?(?:/>|>.*?</iq>)", Pattern.DOTALL));
    }

    /**
     * Returns data that was already received but not yet consumed, or otherwise waits up to the provided timeout for
     * new data. Returns an empty string if no data arrives in time.
     */
    public String readWhatArrives(final int timeoutMillis) throws IOException
    {
        if (buffer.length() > 0) {
            final String result = buffer.toString();
            buffer.setLength(0);
            return result;
        }
        final int originalTimeout = socket.getSoTimeout();
        socket.setSoTimeout(timeoutMillis);
        try {
            final char[] chunk = new char[4096];
            final int read = reader.read(chunk);
            return read < 0 ? "" : new String(chunk, 0, read);
        } catch (SocketTimeoutException e) {
            return "";
        } finally {
            socket.setSoTimeout(originalTimeout);
        }
    }

    private Matcher await(final Pattern pattern) throws IOException
    {
        final char[] chunk = new char[4096];
        try {
            while (true) {
                final Matcher matcher = pattern.matcher(buffer);
                if (matcher.find()) {
                    return matcher;
                }
                final int read = reader.read(chunk);
                if (read < 0) {
                    throw new IOException("Stream closed while waiting for '" + pattern + "'. Data received so far: " + buffer);
                }
                buffer.append(chunk, 0, read);
            }
        } catch (SocketTimeoutException e) {
            final SocketTimeoutException timeout = new SocketTimeoutException("Timed out waiting for '" + pattern + "'. Data received so far: " + buffer);
            timeout.initCause(e);
            throw timeout;
        }
    }

    @Override
    public void close() throws IOException
    {
        socket.close();
    }

    /**
     * Accepts any certificate. Only used when the test configuration explicitly allows this.
     */
    private static final class AcceptAllTrustManager implements X509TrustManager
    {
        @Override
        public void checkClientTrusted(final X509Certificate[] chain, final String authType) {}

        @Override
        public void checkServerTrusted(final X509Certificate[] chain, final String authType) {}

        @Override
        public X509Certificate[] getAcceptedIssuers()
        {
            return new X509Certificate[0];
        }
    }
}
