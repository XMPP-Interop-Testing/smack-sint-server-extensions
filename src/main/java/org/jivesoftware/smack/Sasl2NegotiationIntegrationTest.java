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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;

import org.jivesoftware.smack.XMPPException.FailedNonzaException;
import org.jivesoftware.smack.XMPPException.StreamErrorException;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.sasl.packet.Sasl2Nonza;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.stringencoder.Base64;

import org.igniterealtime.smack.inttest.AbstractSmackSpecificLowLevelIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.ResultSyncPoint;

/**
 * Tests that verify the behavior of a server around the core negotiation rules of XEP-0388: Extensible SASL Profile
 * (SASL2), beyond the happy-path and simple-failure cases already covered by Smack's own
 * {@code org.jivesoftware.smack.sasl.sasl2.Sasl2IntegrationTest}.
 *
 * This test is placed in the {@code org.jivesoftware.smack} package (rather than under
 * {@code org.igniterealtime.smack.inttest}) so that it can access the package-private / protected low-level nonza
 * exchange methods ({@code AbstractXMPPConnection#sendAndWaitForResponse}) that Smack does not expose publicly, in
 * order to drive raw SASL2 negotiations that the high-level login() API cannot express.
 */
@SpecificationReference(document = "XEP-0388", version = "1.0.4")
public class Sasl2NegotiationIntegrationTest extends AbstractSmackSpecificLowLevelIntegrationTest<ModularXmppClientToServerConnection> {

    @SuppressWarnings("this-escape")
    public Sasl2NegotiationIntegrationTest(SmackIntegrationTestEnvironment environment) throws Exception {
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
        "If the \"mechanism\" attribute contains a string not previously announced by the server in the stream "
      + "feature, the server MUST fail the authentication.")
    public void testUnadvertisedMechanismFails() throws Exception
    {
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();

            final String bogusMechanism = "X-DOES-NOT-EXIST-" + StringUtils.insecureRandomString(8);
            final Sasl2Nonza.Authenticate authenticate = new Sasl2Nonza.Authenticate(bogusMechanism, null, null);

            assertThrows(FailedNonzaException.class,
                () -> connection.sendAndWaitForResponse(authenticate, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class),
                "Expected the service to reject an '<authenticate/>' element naming a mechanism ('" + bogusMechanism
                    + "') it had not advertised, with a '<failure/>' element.");
            assertFalse(connection.isAuthenticated(), "Expected the connection to not be authenticated after authentication was attempted using an unadvertised mechanism.");
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest(section = "2.3 / 4.4", quote =
        "If the stream's from attribute (if present) does not match the non-empty authorization string, the server "
      + "MUST fail the authentication. / Any non-empty authorization string MUST be considered an error if the "
      + "stream's from attribute (if present) does not match.")
    public void testMismatchedAuthzidFails() throws Exception
    {
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();

            // By the time authentication starts, Smack has already re-opened the stream with a 'from' attribute of
            // '<username>@<domain>' (derived from the account it registered for this connection). Authenticate with
            // a PLAIN authzid that deliberately does not match that value.
            final CharSequence username = connection.getConfiguration().getUsername();
            final String password = connection.getConfiguration().getPassword();
            final String mismatchedAuthzid = "not-" + username + "@" + connection.getXMPPServiceDomain();
            final String plainPayload = mismatchedAuthzid + '\0' + username + '\0' + password;
            final String initialResponse = Base64.encodeToString(plainPayload.getBytes(StandardCharsets.UTF_8));

            final Sasl2Nonza.Authenticate authenticate = new Sasl2Nonza.Authenticate("PLAIN", initialResponse, null);

            assertThrows(FailedNonzaException.class,
                () -> connection.sendAndWaitForResponse(authenticate, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class),
                "Expected the service to reject a PLAIN authentication whose authzid ('" + mismatchedAuthzid
                    + "') did not match the stream's 'from' attribute, with a '<failure/>' element.");
            assertFalse(connection.isAuthenticated(), "Expected the connection to not be authenticated after authentication was attempted with a mismatched authzid.");
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest(section = "2.6.2", quote =
        "It MUST contain one of the SASL error codes from RFC 6120 Section 6.5.")
    public void testFailureConditionIsRfc6120SaslError() throws Exception
    {
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();

            final CharSequence username = connection.getConfiguration().getUsername();
            final String invalidPassword = "invalid-password-" + StringUtils.insecureRandomString(16);

            final SASLErrorException errorException = assertThrows(SASLErrorException.class,
                () -> connection.login(username, invalidPassword),
                "Expected a failed authentication attempt to be reported as a SASLErrorException.");

            final Sasl2Nonza.Failure failure = errorException.getSasl2Failure();
            assertNotNull(failure, "Expected a SASL2 '<failure/>' element to be present on the exception thrown after a failed authentication attempt.");
            assertNotNull(failure.getSASLError(),
                "Expected the condition element in the '<failure/>' sent by the service ('" + failure.getSASLErrorString()
                    + "') to be one of the SASL error codes defined in RFC 6120 Section 6.5, but it did not parse as one.");
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest(section = "4.8", quote =
        "once <success/> or <continue/> has been sent by the server, any further <authenticate/> element MUST "
      + "result in a stream error.")
    public void testFurtherAuthenticateAfterSuccessResultsInStreamError() throws Exception
    {
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();

            // Authenticate 'manually' (rather than through login()) using PLAIN, to sidestep automatic mechanism
            // selection picking a channel-binding ('*-PLUS') mechanism, which is otherwise irrelevant to this test.
            final CharSequence username = connection.getConfiguration().getUsername();
            final String password = connection.getConfiguration().getPassword();
            final String plainPayload = '\0' + username.toString() + '\0' + password;
            final String initialResponse = Base64.encodeToString(plainPayload.getBytes(StandardCharsets.UTF_8));
            final Sasl2Nonza.Authenticate firstAuthenticate = new Sasl2Nonza.Authenticate("PLAIN", initialResponse, null);
            final Sasl2Nonza.Success success = connection.sendAndWaitForResponse(firstAuthenticate, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class);
            assertNotNull(success, "Expected the service to authenticate '" + username + "' using PLAIN.");

            final ResultSyncPoint<XMPPException.StreamErrorException, Exception> errorReceived = new ResultSyncPoint<>();
            final ConnectionListener listener = new ConnectionListener() {
                @Override
                public void connectionClosedOnError(Exception e) {
                    if (e instanceof StreamErrorException) {
                        errorReceived.signal((StreamErrorException) e);
                    } else {
                        errorReceived.signal(new Exception("Connection closed with an error other than a stream error", e));
                    }
                }

                @Override
                public void connectionClosed() {
                    errorReceived.signal(new Exception("Connection closed without an error, but a stream error was expected."));
                }
            };
            connection.addConnectionListener(listener);

            final Sasl2Nonza.Authenticate secondAuthenticate = new Sasl2Nonza.Authenticate("PLAIN", null, null);
            connection.sendNonza(secondAuthenticate);

            assertResult(errorReceived, "Expected the service to close the stream with a stream error, after '" + username
                + "' sent a second '<authenticate/>' element on an already-authenticated connection.");
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest(section = "2.4", quote =
        "Servers MUST disconnect Clients immediately if any other traffic is received.")
    public void testStrayTrafficDuringNegotiationCausesDisconnect() throws Exception
    {
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();

            final ResultSyncPoint<Boolean, Exception> disconnected = new ResultSyncPoint<>();
            final ConnectionListener listener = new ConnectionListener() {
                @Override
                public void connectionClosedOnError(Exception e) {
                    disconnected.signal(true);
                }

                @Override
                public void connectionClosed() {
                    disconnected.signal(true);
                }
            };
            connection.addConnectionListener(listener);

            // Start a negotiation without an initial response, so that the server is waiting for our next element.
            final Sasl2Nonza.Authenticate authenticate = new Sasl2Nonza.Authenticate("PLAIN", null, null);
            final Sasl2Nonza.Challenge challenge = connection.sendAndWaitForResponse(authenticate, Sasl2Nonza.Challenge.class, Sasl2Nonza.Failure.class);
            assertNotNull(challenge, "Expected the service to request SASL data through a '<challenge/>' element, after an '<authenticate/>' element without an initial response.");

            // Instead of a <response/> or <abort/>, send a stanza. This is 'other traffic' that is not permitted
            // while authentication is in progress.
            final Presence stray = StanzaBuilder.buildPresence().build();
            connection.sendStanza(stray);

            assertResult(disconnected, "Expected the service to disconnect the connection immediately, after it sent a stanza while a SASL2 negotiation was in progress.");
        } finally {
            connection.disconnect();
        }
    }
}
