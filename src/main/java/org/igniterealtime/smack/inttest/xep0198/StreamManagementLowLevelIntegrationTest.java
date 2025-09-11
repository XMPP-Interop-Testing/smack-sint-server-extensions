/*
 * Copyright 2024 Guus der Kinderen
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
package org.igniterealtime.smack.inttest.xep0198;

import org.igniterealtime.smack.inttest.AbstractSmackSpecificLowLevelIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.sm.packet.StreamManagement;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ping.packet.Ping;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the XEP-0198: Stream Management
 *
 * @see <a href="https://xmpp.org/extensions/xep-0198.html">XEP-0198: Stream Management</a>
 */
@SpecificationReference(document = "XEP-0198", version = "1.6.1")
public class StreamManagementLowLevelIntegrationTest extends AbstractSmackSpecificLowLevelIntegrationTest<XMPPTCPConnection>
{
    public StreamManagementLowLevelIntegrationTest(final SmackIntegrationTestEnvironment environment) throws XMPPException, SmackException, InterruptedException, IOException, TestNotPossibleException
    {
        super(environment, XMPPTCPConnection.class);
        final XMPPTCPConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect().login();
            if (!connection.isSmAvailable()) {
                throw new TestNotPossibleException("Domain does not seem support XEP-0198 Stream Management.");
            }
        } finally {
            recycle(connection);
        }
    }

    /**
     * Asserts that the server understands an 'enable' request (without any configuration) and responds to it appropriately.
     */
    @SmackIntegrationTest(section = "3", quote = "To enable use of stream management, the client sends an <enable/> command to the server. [...] Upon receiving the enable request, the server MUST reply with an <enabled/> element or a <failed/> element qualified by the 'urn:xmpp:sm:3' namespace.")
    public void testEnable() throws XMPPException, SmackException, InterruptedException, IOException
    {
        // Setup test fixture.
        final XMPPTCPConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.setUseStreamManagement(true);
            connection.setUseStreamManagementResumption(false);

            // Execute system under test.
            try {
                connection.connect().login();
            } catch (SmackException | XMPPException e) {
                // Verify result.
                fail("Expected the server to respond with either 'enabled' or 'failed' after '" + connection.getUser() + "' attempted to enable the use of stream management (but it did not).");
            }
        } finally {
            recycle(connection);
        }
    }

    /**
     * Asserts that the server understands an 'enable' request (with 'resumption' enabled) and responds to it appropriately.
     */
    @SmackIntegrationTest(section = "3", quote = "To enable use of stream management, the client sends an <enable/> command to the server. [...] If the client wants to be allowed to resume the stream, it includes a boolean 'resume' attribute [...] Upon receiving the enable request, the server MUST reply with an <enabled/> element or a <failed/> element qualified by the 'urn:xmpp:sm:3' namespace.")
    public void testEnableWithResumption() throws XMPPException, SmackException, InterruptedException, IOException
    {
        // Setup test fixture.
        final XMPPTCPConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.setUseStreamManagement(true);
            connection.setUseStreamManagementResumption(true);

            // Execute system under test.
            try {
                connection.connect().login();
            } catch (SmackException | XMPPException e) {
                // Verify result.
                fail("Expected the server to respond with either 'enabled' or 'failed' after '" + connection.getUser() + "' attempted to enable the use of stream management with the resumption feature enabled (but it did not).");
            }
        } finally {
            recycle(connection);
        }
    }

    /**
     * Asserts that the server understands an 'enable' request (with 'resumption' enabled and a preferred maximum resumption time provided) and responds to it appropriately.
     */
    @SmackIntegrationTest(section = "3", quote = "To enable use of stream management, the client sends an <enable/> command to the server. [...] If the client wants to be allowed to resume the stream, it includes a boolean 'resume' attribute [...] The <enable/> element MAY include a 'max' attribute to specify the client's preferred maximum resumption time [...] Upon receiving the enable request, the server MUST reply with an <enabled/> element or a <failed/> element qualified by the 'urn:xmpp:sm:3' namespace.")
    public void testEnableWithResumptionAndMaxResumptionTime() throws XMPPException, SmackException, InterruptedException, IOException
    {
        // Setup test fixture.
        final XMPPTCPConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.setUseStreamManagement(true);
            connection.setUseStreamManagementResumption(true);
            connection.setPreferredResumptionTime(30);

            // Execute system under test.
            try {
                connection.connect().login();
            } catch (SmackException | XMPPException e) {
                // Verify result.
                fail("Expected the server to respond with either 'enabled' or 'failed' after '" + connection.getUser() + "' attempted to enable the use of stream management with the resumption feature enabled and a preferred maximum resumption time provided (but it did not).");
            }
        } finally {
            recycle(connection);
        }
    }

    /**
     * Asserts that the server rejects an 'enable' prior to authentication.
     */
    @SmackIntegrationTest(section = "3", quote = "The client MUST NOT attempt to negotiate stream management until it is authenticated [...] The server SHALL enforce this order and return a <failed/> element in response if the order is violated")
    public void testEnableBeforeLogin() throws XMPPException, SmackException, InterruptedException, IOException, TimeoutException
    {
        // Setup test fixture.
        final XMPPTCPConnection connection = getSpecificUnconnectedConnection();
        try {
            // Execute system under test.
            connection.connect();
            connection.sendNonza(new StreamManagement.Enable(false, -1));

            // Verify result.
            final String assertionFailedMessage = "Expected the server to return a '<failed/>' element on the connection with stream-id '" + connection.getStreamId() + "' after it sent an '<enabled/>' element without/before authenticating (but the server did not send such an element).";
            final SmackException.SmackWrappedException wrappedException = assertThrows(SmackException.SmackWrappedException.class, connection::login, assertionFailedMessage);
            assertNotNull(wrappedException.getCause(), assertionFailedMessage);
            assertEquals(XMPPException.FailedNonzaException.class, wrappedException.getCause().getClass(), assertionFailedMessage);
            assertEquals("failed", ((XMPPException.FailedNonzaException) wrappedException.getCause()).getNonza().getElementName(), assertionFailedMessage);
        } finally {
            recycle(connection);
        }
    }

// FIXME Find a way to make Smack do authentication without resource binding.
//    /**
//     * Asserts that the server rejects an 'enable' prior to resource binding.
//     */
//    @SmackIntegrationTest(section = "3", quote = "For client-to-server connections, the client MUST NOT attempt to enable stream management until after it has completed Resource Binding [...] The server SHALL enforce this order and return a <failed/> element in response if the order is violated")
//    public void testEnableBeforeResourceBinding() throws XMPPException, SmackException, InterruptedException, IOException, TimeoutException
//    {
//        // Setup test fixture.
//        final XMPPTCPConnection connection = getSpecificUnconnectedConnection();
//        try {
//            // Execute system under test.
//            connection.connect();
//            connection.loginWithoutResourceBinding();
//            connection.sendNonza(new StreamManagement.Enable(false, -1));
//
//            // Verify result.
//            final XMPPException.FailedNonzaException failedNonzaException = assertThrows(XMPPException.FailedNonzaException.class, connection::login, "Expected the server to return a '<failed/>' element on the connection for '" + connection.getUser() + "' after it sent an '<enabled/>' element without/before resource binding (but the server did not send such an element).");
//            assertEquals("failed", failedNonzaException.getNonza().getElementName(), "Expected the server to return a '<failed/>' element on the connection with stream-id '" + connection.getStreamId() + "' after it sent an '<enabled/>' element without/before resource binding (but the server did not send such an element).");
//        } finally {
//            recycle(connection);
//        }
//    }

    /**
     * Asserts that a server sends an SM ack when requested.
     */
    @SmackIntegrationTest(section = "4", quote = "When an <r/> element (\"request\") is received, the recipient MUST acknowledge it by sending an <a/> element to the sender")
    public void testRequestsAreAcknowledged() throws XMPPException, SmackException, InterruptedException, IOException, TimeoutException
    {
        // Setup test fixture.
        final XMPPTCPConnection connection = getSpecificUnconnectedConnection();
        final SimpleResultSyncPoint anyAckReceived = new SimpleResultSyncPoint();
        final StanzaListener ackListener = s -> anyAckReceived.signal();
        try {
            connection.setUseStreamManagement(true);
            connection.addRequestAckPredicate(stanza -> false); // Never send request by default.

            connection.connect().login();

            connection.addStanzaAcknowledgedListener(ackListener);
            connection.sendStanza(new Ping(connection.getXMPPServiceDomain()));

            // Execute system under test.
            connection.requestSmAcknowledgement();

            // Verify result.
            assertResult(anyAckReceived, timeout, "Expected the server to send an a stream management ack in response to the request that was sent by '" + connection.getUser() + "' (but no ack was received).");

        } finally {
            connection.removeStanzaAcknowledgedListener(ackListener);
            recycle(connection);
        }
    }

    /**
     * Asserts that the server send an 'id' when allowing the stream to be resumed.
     */
    @SmackIntegrationTest(section = "5", quote = "If the server will allow the stream to be resumed, it [...] MUST include an 'id' attribute that specifies an identifier for the stream.")
    public void testEnabledWithResumptionHasId() throws XMPPException, SmackException, InterruptedException, IOException
    {
        // Setup test fixture.
        final XMPPTCPConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.setUseStreamManagement(true);
            connection.setUseStreamManagementResumption(true);

            // Execute system under test
            try {
                connection.connect().login();
            } catch (SmackException.SmackMessageException e) {
                // Verify result.
                if ("Stream Management 'enabled' element with resume attribute but without session id received".equals(e.getMessage())) { // TODO This is brittle. Find a better way than depending on a Smack exception message to test this.
                    fail("Server allows the stream from '" + connection.getUser() + "' to be resumed, but does not include an 'id' attribute.", e);
                }
                throw e;
            }
        } finally {
            recycle(connection);
        }
    }

    /**
     * Asserts that the server does not re-use the stream management ID for subsequent sessions.
     */
    @SmackIntegrationTest(section = "5", quote = "The SM-ID MUST NOT be reused for [...] subsequent sessions")
    public void testStreamManagementIdUniqueInSubsequentSessions() throws XMPPException, SmackException, InterruptedException, IOException, NoSuchFieldException, TestNotPossibleException, IllegalAccessException
    {
        // Setup test fixture.
        final int distinctConnectionCount = 5;
        final List<XMPPTCPConnection> connections = getSpecificUnconnectedConnections(distinctConnectionCount);
        final Set<String> uniqueIds = new HashSet<>();
        try {
            for (final XMPPTCPConnection connection : connections) {
                connection.setUseStreamManagement(true);
                connection.setUseStreamManagementResumption(true);

                // Execute system under test
                connection.connect().login();
                if (!connection.isSmAvailable()) {
                    throw new TestNotPossibleException("Service does not allow streams to be resumed.");
                }
                if (!connection.isSmEnabled()) {
                    throw new TestNotPossibleException("Unable to enable stream management with the service.");
                }

                final Field f = connection.getClass().getDeclaredField("smSessionId"); //NoSuchFieldException
                f.setAccessible(true);
                try {
                    final String smSessionId = (String) f.get(connection);
                    uniqueIds.add(smSessionId);
                } finally {
                    f.setAccessible(false);
                }

                connection.disconnect();
            }

            // Verify result.
            assertEquals(distinctConnectionCount, uniqueIds.size(), "Expected the server to use distinct stream management IDs for subsequent sessions, but only " + uniqueIds.size() + " unique IDs were used between " + distinctConnectionCount + " connections.");
        } finally {
            connections.forEach(this::recycle);
        }
    }

    /**
     * Asserts that the server does not re-use the stream management ID for simultaneous sessions.
     */
    @SmackIntegrationTest(section = "5", quote = "The SM-ID MUST NOT be reused for simultaneous [...] sessions")
    public void testStreamManagementIdUniqueInSimultaneousSessions() throws XMPPException, SmackException, InterruptedException, IOException, NoSuchFieldException, IllegalAccessException, TestNotPossibleException
    {
        // Setup test fixture.
        final int distinctConnectionCount = 5;
        final List<XMPPTCPConnection> connections = getSpecificUnconnectedConnections(distinctConnectionCount);
        final Set<String> uniqueIds = new HashSet<>();
        try {
            for (final XMPPTCPConnection connection : connections) {
                connection.setUseStreamManagement(true);
                connection.setUseStreamManagementResumption(true);

                // Execute system under test
                connection.connect().login();
                if (!connection.isSmAvailable()) {
                    throw new TestNotPossibleException("Service does not allow streams to be resumed.");
                }
                if (!connection.isSmEnabled()) {
                    throw new TestNotPossibleException("Unable to enable stream management with the service.");
                }


                uniqueIds.add((String) getDeclaredFieldValueThroughReflection(connection, "smSessionId"));
            }

            // Verify result.
            assertEquals(distinctConnectionCount, uniqueIds.size(), "Expected the server to use distinct stream management IDs for simultaneous sessions, but only " + uniqueIds.size() + " unique IDs were used between " + distinctConnectionCount + " connections.");
        } finally {
            connections.forEach(this::recycle);
        }
    }

    /**
     * Asserts that the server understands an 'resume' request and responds to it appropriately.
     */
    @SmackIntegrationTest(section = "5", quote = "To request resumption of the former stream, the client sends a <resume/> element [...] If the server can resume the former stream, it MUST return a <resumed/> element [...] If the server does not support session resumption, it MUST return a <failed/> element")
    public void testResumeResponse() throws XMPPException, SmackException, InterruptedException, IOException, NoSuchFieldException, IllegalAccessException, TestNotPossibleException
    {
        // Setup test fixture.
        final XMPPTCPConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.setUseStreamManagement(true);
            connection.setUseStreamManagementResumption(true);
            connection.connect().login();
            if (!connection.isSmAvailable()) {
                throw new TestNotPossibleException("Service does not allow streams to be resumed.");
            }
            if (!connection.isSmEnabled()) {
                throw new TestNotPossibleException("Unable to enable stream management with the service.");
            }

            connection.instantShutdown(); // Leaves the connection in a resumable state
            if (!connection.isDisconnectedButSmResumptionPossible()) {
                throw new TestNotPossibleException("Service does not allow streams to be resumed.");
            }

            // Execute system under test.
            connection.connect().login(); // Smack will attempt resumption.

            // Verify result
            final boolean receivedResumed = connection.streamWasResumed();
            final boolean receivedFailed = getDeclaredFieldValueThroughReflection(connection, "smResumptionFailed") != null;
            assertTrue(receivedResumed || receivedFailed, "Expected " + connection.getUser() + " to receive either 'resumed' or or 'failed' after a it requested a stream to be resumed (but it did not).");
        } finally {
            recycle(connection);
        }
    }

    /**
     * Asserts that the server rejects an unrecognized 'previd' in a 'resume' request appropriately.
     */
    @SmackIntegrationTest(section = "5", quote = "To request resumption of the former stream, the client sends a <resume/> element [...] If the server does not recognize the 'previd' as an earlier session [...] it MUST return a <failed/> element,")
    public void testRejectInvalidResume() throws XMPPException, SmackException, InterruptedException, IOException, NoSuchFieldException, IllegalAccessException, TestNotPossibleException
    {
        // Setup test fixture.
        final XMPPTCPConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.setUseStreamManagement(true);
            connection.setUseStreamManagementResumption(true);
            connection.connect();
            connection.instantShutdown(); // Smack's fix for SMACK-954 requires that a connection has been instantly shutdown, before SM resumption is even considered.
            setDeclaredFieldValueThroughReflection(connection, "smSessionId", "non-existing-previd-" + StringUtils.randomString(23));

            // Execute system under test.
            connection.connect().login(); // Smack will attempt resumption.

            // Verify result
            final boolean receivedFailed = getDeclaredFieldValueThroughReflection(connection, "smResumptionFailed") != null;
            assertTrue(receivedFailed, "Expected " + connection.getUser() + " to receive 'failed' after a it requested a stream to be resumed using a previd that can not have been recognized by the server (but it did not).");
        } finally {
            recycle(connection);
        }
    }

    static Object getDeclaredFieldValueThroughReflection(final Object object, final String fieldName) throws NoSuchFieldException, IllegalAccessException
    {
        final Field f = object.getClass().getDeclaredField(fieldName);
        final boolean wasAccessible = f.canAccess(object);
        if (!wasAccessible) {
            f.setAccessible(true);
        }
        try {
            return f.get(object);
        } finally {
            if (!wasAccessible) {
                f.setAccessible(wasAccessible);
            }
        }
    }

    static void setDeclaredFieldValueThroughReflection(final Object object, final String fieldName, final Object value) throws NoSuchFieldException, IllegalAccessException
    {
        final Field f = object.getClass().getDeclaredField(fieldName);
        final boolean wasAccessible = f.canAccess(object);
        if (!wasAccessible) {
            f.setAccessible(true);
        }
        try {
            f.set(object, value);
        } finally {
            if (!wasAccessible) {
                f.setAccessible(wasAccessible);
            }
        }
    }
}
