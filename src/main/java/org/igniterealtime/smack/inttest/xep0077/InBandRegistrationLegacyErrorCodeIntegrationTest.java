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
package org.igniterealtime.smack.inttest.xep0077;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.AccountUtilities;
import org.igniterealtime.smack.inttest.util.RawXmppConnection;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;

import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for XEP-0077: In-Band Registration, that verify that error responses carry the old-style
 * (HTTP-style) error code, in addition to the XMPP error condition.
 *
 * These tests interpret the requirement for a server to "support [...] old-style [...] error handling" as a
 * requirement to include the 'code' attribute (as mapped by XEP-0086) in the error stanzas that it sends.
 *
 * These tests speak raw XML over a socket, rather than using a Smack connection, as Smack does not retain the legacy
 * 'code' attribute when parsing errors.
 *
 * All tests require the server to advertise In-Band Registration as a stream feature.
 */
@SpecificationReference(document = "XEP-0077", version = "2.4")
public class InBandRegistrationLegacyErrorCodeIntegrationTest extends AbstractSmackIntegrationTest
{
    private static final String CODE_QUOTE = "As defined herein, the 'jabber:iq:register' namespace supports both the old (HTTP-style) error codes and the extensible error classes and conditions specified in XMPP Core. A compliant server or service implementation MUST support both old-style and new-style error handling.";

    private final SmackIntegrationTestEnvironment environment;

    public InBandRegistrationLegacyErrorCodeIntegrationTest(SmackIntegrationTestEnvironment environment)
    {
        super(environment);
        this.environment = environment;
    }

    /**
     * Asserts that the not-acceptable error that is returned for a registration request with an empty password
     * element carries the legacy error code 406.
     */
    @SmackIntegrationTest(section = "9", quote = CODE_QUOTE)
    public void testEmptyPasswordErrorHasCode406() throws Exception
    {
        doTestPasswordErrorHasCode406(true);
    }

    /**
     * Asserts that the not-acceptable error that is returned for a registration request without a password element
     * carries the legacy error code 406.
     */
    @SmackIntegrationTest(section = "9", quote = CODE_QUOTE)
    public void testMissingPasswordErrorHasCode406() throws Exception
    {
        doTestPasswordErrorHasCode406(false);
    }

    private void doTestPasswordErrorHasCode406(final boolean includeEmptyPasswordElement) throws Exception
    {
        final String username = InBandRegistrationTestUtils.randomUsername();
        final String description = includeEmptyPasswordElement ? "an empty password element" : "no password element";
        try (RawXmppConnection connection = openUnauthenticatedStream()) {
            // Setup test fixture.
            final String fieldsResponse = connection.sendIq("<iq type='get' id='fields'><query xmlns='jabber:iq:register'/></iq>", "fields");
            if (!fieldsResponse.contains("<password")) {
                throw new TestNotPossibleException("Domain '" + connection.getDomain() + "' does not request a password during registration.");
            }
            final String passwordElement = includeEmptyPasswordElement ? "<password/>" : "";

            // Execute system-under-test.
            final String response;
            try {
                response = connection.sendIq("<iq type='set' id='reg'><query xmlns='jabber:iq:register'><username>" + username + "</username>" + passwordElement + "</query></iq>", "reg");
            } catch (SocketTimeoutException e) {
                fail("Expected the domain '" + connection.getDomain() + "' to respond with an error to a registration request that contained " + description + " (but it did not respond at all).");
                return;
            }

            // Verify result.
            final ParsedError error = ParsedError.from(response);
            if (error == null) {
                InBandRegistrationTestUtils.removeAccount(environment, username, "");
            }
            assertNotNull(error, "Expected the domain '" + connection.getDomain() + "' to respond with an error to a registration request that contained " + description + " (but it did not). Response: " + response);
            assertEquals("not-acceptable", error.condition, "Unexpected error condition in the response of domain '" + connection.getDomain() + "' to a registration request that contained " + description + ". Response: " + response);
            assertEquals("406", error.code, "Expected the <not-acceptable/> error returned by domain '" + connection.getDomain() + "' to carry the old-style error code '406' (but it did not). Response: " + response);
        }
    }

    /**
     * Asserts that the conflict error that is returned for a registration request of an existing username carries
     * the legacy error code 409.
     */
    @SmackIntegrationTest(section = "9", quote = CODE_QUOTE)
    public void testUsernameConflictErrorHasCode409() throws Exception
    {
        final String username = InBandRegistrationTestUtils.randomUsername();
        final String password = InBandRegistrationTestUtils.randomPassword();
        // Setup test fixture.
        InBandRegistrationTestUtils.createAccount(environment, username, password);
        try {
            try (RawXmppConnection connection = openUnauthenticatedStream()) {
                // Execute system-under-test.
                final String response;
                try {
                    response = connection.sendIq("<iq type='set' id='reg'><query xmlns='jabber:iq:register'><username>" + username + "</username><password>" + InBandRegistrationTestUtils.randomPassword() + "</password></query></iq>", "reg");
                } catch (SocketTimeoutException e) {
                    fail("Expected the domain '" + connection.getDomain() + "' to respond with an error to a registration request for username '" + username + "', which is already registered (but it did not respond at all).");
                    return;
                }

                // Verify result.
                final ParsedError error = ParsedError.from(response);
                if (error == null || !"conflict".equals(error.condition)) {
                    // Whether a <conflict/> is returned is verified by InBandRegistrationIntegrationTest.
                    throw new TestNotPossibleException("Domain '" + connection.getDomain() + "' did not respond to the registration of an existing username with a <conflict/> error. Response: " + response);
                }
                assertEquals("409", error.code, "Expected the <conflict/> error returned by domain '" + connection.getDomain() + "' to carry the old-style error code '409' (but it did not). Response: " + response);
            }
        } finally {
            InBandRegistrationTestUtils.removeAccount(environment, username, password);
        }
    }

    /**
     * Opens an unauthenticated stream to the domain, and verifies that In-Band Registration is advertised as a stream
     * feature.
     */
    private RawXmppConnection openUnauthenticatedStream() throws Exception
    {
        // The connection is only used to obtain the connection configuration. It is never connected.
        final RawXmppConnection connection = RawXmppConnection.connect(AccountUtilities.spawnNewConnection(environment, sinttestConfiguration).getConfiguration(), sinttestConfiguration);
        if (!connection.getStreamFeatures().contains("http://jabber.org/features/iq-register")) {
            connection.close();
            throw new TestNotPossibleException("Domain '" + connection.getDomain() + "' does not advertise support for XEP-0077 In-Band Registration as a stream feature ('http://jabber.org/features/iq-register').");
        }
        return connection;
    }

    /**
     * The error condition and legacy code of an IQ error response.
     */
    private static final class ParsedError
    {
        final String code;
        final String condition;

        private ParsedError(final String code, final String condition)
        {
            this.code = code;
            this.condition = condition;
        }

        /**
         * Parses an IQ stanza, returning null if it is not an error.
         */
        static ParsedError from(final String iq) throws Exception
        {
            final XmlPullParser parser = PacketParserUtils.getParserFor(iq);
            if (!"error".equals(parser.getAttributeValue("", "type"))) {
                return null;
            }
            String code = null;
            String condition = null;
            boolean inError = false;
            while (true) {
                final XmlPullParser.Event event = parser.next();
                if (event == XmlPullParser.Event.END_DOCUMENT) {
                    break;
                }
                if (event == XmlPullParser.Event.START_ELEMENT) {
                    if ("error".equals(parser.getName())) {
                        inError = true;
                        code = parser.getAttributeValue("", "code");
                    } else if (inError && "urn:ietf:params:xml:ns:xmpp-stanzas".equals(parser.getNamespace()) && !"text".equals(parser.getName()) && condition == null) {
                        condition = parser.getName();
                    }
                } else if (event == XmlPullParser.Event.END_ELEMENT && "iq".equals(parser.getName())) {
                    break;
                }
            }
            return new ParsedError(code, condition);
        }
    }
}
