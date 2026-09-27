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

import org.igniterealtime.smack.inttest.Configuration;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.util.AccountUtilities;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.StanzaIdFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.iqregister.packet.Registration;
import org.jxmpp.jid.parts.Resourcepart;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared fixtures for the XEP-0077: In-Band Registration integration tests.
 */
final class InBandRegistrationTestUtils
{
    private static final Logger LOGGER = Logger.getLogger(InBandRegistrationTestUtils.class.getName());

    /**
     * The names of all elements that the XML schema of XEP-0077 (section 14.1) defines as children of the
     * {@code <query xmlns='jabber:iq:register'/>} element.
     */
    static final Set<String> SCHEMA_ELEMENT_NAMES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "registered", "instructions", "username", "nick", "password", "name", "first", "last", "email", "address",
        "city", "state", "zip", "phone", "url", "date", "misc", "text", "key", "remove")));

    private InBandRegistrationTestUtils() {}

    /**
     * Throws a TestNotPossibleException when the (connected, but not necessarily authenticated) connection has not
     * received the XEP-0077 stream feature, which is how a server advertises that it offers In-Band Registration to
     * entities that have not yet authenticated.
     */
    static void requireStreamFeature(final AbstractXMPPConnection connection) throws TestNotPossibleException
    {
        if (!connection.hasFeature(Registration.Feature.QNAME)) {
            throw new TestNotPossibleException("Domain '" + connection.getXMPPServiceDomain() + "' does not advertise support for XEP-0077 In-Band Registration as a stream feature ('" + Registration.Feature.NAMESPACE + "').");
        }
    }

    /**
     * Throws a TestNotPossibleException when the domain does not advertise the XEP-0077 service discovery feature,
     * which is how a server advertises In-Band Registration support to entities that are already connected.
     */
    static void requireDiscoFeature(final XMPPConnection connection) throws TestNotPossibleException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        if (!ServiceDiscoveryManager.getInstanceFor(connection).serverSupportsFeature(Registration.NAMESPACE)) {
            throw new TestNotPossibleException("Domain '" + connection.getXMPPServiceDomain() + "' does not advertise support for XEP-0077 In-Band Registration as a service discovery feature ('" + Registration.NAMESPACE + "').");
        }
    }

    /**
     * Returns a new connection that is connected to the domain, but not authenticated.
     */
    static AbstractXMPPConnection connectUnauthenticated(final SmackIntegrationTestEnvironment environment, final Configuration sinttestConfiguration) throws Exception
    {
        final AbstractXMPPConnection connection = AccountUtilities.spawnNewConnection(environment, sinttestConfiguration);
        try {
            connection.connect();
            return connection;
        } catch (Exception e) {
            connection.disconnect();
            throw e;
        }
    }

    /**
     * Returns a new connection that is authenticated using the provided credentials.
     */
    static AbstractXMPPConnection connectAndLogin(final SmackIntegrationTestEnvironment environment, final Configuration sinttestConfiguration, final String username, final String password) throws Exception
    {
        final AbstractXMPPConnection connection = AccountUtilities.spawnNewConnection(environment, sinttestConfiguration);
        try {
            connection.connect();
            connection.login(username, password, Resourcepart.from("ibr-" + StringUtils.randomString(5)));
            return connection;
        } catch (Exception e) {
            connection.disconnect();
            throw e;
        }
    }

    /**
     * Checks if the provided credentials can be used to authenticate to the domain.
     *
     * @return true if authentication succeeded, false if the server rejected the credentials.
     */
    static boolean canLogIn(final SmackIntegrationTestEnvironment environment, final Configuration sinttestConfiguration, final String username, final String password) throws Exception
    {
        final AbstractXMPPConnection connection = AccountUtilities.spawnNewConnection(environment, sinttestConfiguration);
        try {
            connection.connect();
            connection.login(username, password, Resourcepart.from("ibr-" + StringUtils.randomString(5)));
            return true;
        } catch (SASLErrorException e) {
            return false;
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Creates a new account (using the provisioning mechanism configured for the test framework, which is not
     * necessarily In-Band Registration).
     */
    static void createAccount(final SmackIntegrationTestEnvironment environment, final String username, final String password) throws Exception
    {
        AccountUtilities.createNonConnectedLocalUser(environment, username, password);
    }

    /**
     * Removes an account, trying each of the provided passwords (only relevant when accounts are managed through
     * In-Band Registration, where the account needs to log in to remove itself). Failures are logged, not thrown.
     */
    static void removeAccount(final SmackIntegrationTestEnvironment environment, final String username, final String... candidatePasswords)
    {
        Exception lastException = null;
        for (final String password : candidatePasswords) {
            try {
                if (environment.configuration.accountRegistration != Configuration.AccountRegistration.inBandRegistration
                    || canLogIn(environment, environment.configuration, username, password))
                {
                    AccountUtilities.removeNonConnectedLocalUser(environment, username, password);
                    return;
                }
            } catch (Exception e) {
                lastException = e;
            }
        }
        // The account may legitimately be gone already (e.g. after a test that cancels its registration).
        LOGGER.log(Level.WARNING, "Unable to remove test account '" + username + "' (none of the candidate passwords could be used to log in). It may already have been removed.", lastException);
    }

    /**
     * Disconnects a connection after its account was (possibly) removed. A server that removes an account typically
     * terminates the session. This waits briefly for that to happen, to avoid disconnecting while the server is
     * already closing the stream (which can make the disconnect wait for the full reply timeout).
     */
    static void disconnectAfterRemoval(final AbstractXMPPConnection connection) throws InterruptedException
    {
        final long deadline = System.currentTimeMillis() + 2000;
        while (connection.isConnected() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        if (connection.isConnected()) {
            connection.disconnect();
        }
    }

    static String randomUsername()
    {
        return ("ibr-test-" + StringUtils.randomString(8)).toLowerCase();
    }

    static String randomPassword()
    {
        return StringUtils.randomString(16);
    }

    /**
     * Sends an IQ request and waits for its response. Unlike XMPPConnection#sendIqRequestAndWaitForResponse, this can
     * be used on a connection that has not authenticated (and thus has no local JID).
     */
    static <I extends IQ> I sendAndWait(final XMPPConnection connection, final IQ request) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        return connection.createStanzaCollectorAndSend(new StanzaIdFilter(request.getStanzaId()), request).nextResultOrThrow();
    }

    /**
     * Requests the registration fields from the domain, using an IQ get.
     */
    static Registration requestRegistrationFields(final XMPPConnection connection) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        final Registration request = new Registration();
        request.setTo(connection.getXMPPServiceDomain());
        return sendAndWait(connection, request);
    }

    /**
     * Builds values for all of the fields that were requested by the host, using the provided username and password
     * for those fields, and plausible values for any others. Returns null if the host requested no iq:register
     * fields that can be filled in (for example, when it redirects registration elsewhere).
     */
    static Map<String, String> fillRequestedFields(final Registration fieldsResponse, final String username, final String password)
    {
        final Map<String, String> requested = fieldsResponse.getAttributes();
        if (requested == null || !requested.containsKey("username")) {
            return null;
        }
        final Map<String, String> result = new HashMap<>();
        for (final Map.Entry<String, String> field : requested.entrySet()) {
            switch (field.getKey()) {
                case "registered":
                case "remove":
                    break;
                case "username":
                    result.put("username", username);
                    break;
                case "password":
                    result.put("password", password);
                    break;
                case "email":
                    result.put("email", username + "@example.org");
                    break;
                default:
                    // Echo back a pre-filled value if the host provided one (e.g. the obsolete 'key' element).
                    result.put(field.getKey(), field.getValue() != null && !field.getValue().isEmpty() ? field.getValue() : "test");
                    break;
            }
        }
        return result;
    }
}
