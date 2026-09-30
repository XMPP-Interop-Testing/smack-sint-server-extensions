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
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smackx.iqregister.packet.Registration;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for XEP-0077: In-Band Registration, that are executed by an entity that is registered and
 * authenticated: requesting current registration, changing the password and cancelling the registration.
 *
 * All tests require the server to advertise In-Band Registration as a service discovery feature. Tests that modify
 * an account operate on a dedicated, temporary account, to prevent affecting the accounts used by the test framework.
 */
@SpecificationReference(document = "XEP-0077", version = "2.4")
public class InBandRegistrationAccountManagementIntegrationTest extends AbstractSmackIntegrationTest
{
    private final SmackIntegrationTestEnvironment environment;

    public InBandRegistrationAccountManagementIntegrationTest(SmackIntegrationTestEnvironment environment)
    {
        super(environment);
        this.environment = environment;
    }

    /**
     * Asserts that a registered entity that requests the registration fields is informed that it is registered.
     */
    @SmackIntegrationTest(section = "3.1", quote = "If the host determines (based on the 'from' address) that the entity is already registered, the IQ result that it sends in response to the IQ get MUST contain an empty <registered/> element (indicating that the entity is already registered)")
    public void testRegisteredEntityReceivesRegisteredElement() throws Exception
    {
        InBandRegistrationTestUtils.requireDiscoFeature(conOne);

        // Execute system-under-test.
        final Registration response;
        try {
            response = InBandRegistrationTestUtils.requestRegistrationFields(conOne);
        } catch (XMPPException.XMPPErrorException e) {
            fail("Expected the domain '" + conOne.getXMPPServiceDomain() + "', which advertises In-Band Registration, to respond to a registration fields request from registered entity '" + conOne.getUser() + "' with a result (but it returned an error: " + e.getStanzaError() + ").");
            return;
        } catch (SmackException.NoResponseException e) {
            fail("Expected the domain '" + conOne.getXMPPServiceDomain() + "', which advertises In-Band Registration, to respond to a registration fields request from registered entity '" + conOne.getUser() + "' (but it did not respond at all).");
            return;
        }

        // Verify result.
        assertTrue(response.getAttributes() != null && response.getAttributes().containsKey("registered"), "Expected the response of domain '" + conOne.getXMPPServiceDomain() + "' to a registration fields request from registered entity '" + conOne.getUser() + "' to contain a <registered/> element (but it did not).");
    }

    /**
     * Asserts that a registered entity can change its password, after which only the new password can be used to
     * authenticate.
     *
     * This is not a normative requirement, but a determinable one: the domain advertises In-Band Registration, and
     * the specification defines how a domain signals that it does not allow password changes (in which case this
     * test is skipped).
     */
    @SmackIntegrationTest(section = "3.3", quote = "The 'jabber:iq:register' namespace enables a user to change his or her password with a server or service. Once registered, the user can change passwords by sending an XML stanza of the following form")
    public void testPasswordChange() throws Exception
    {
        InBandRegistrationTestUtils.requireDiscoFeature(conOne);

        // Setup test fixture.
        final String username = InBandRegistrationTestUtils.randomUsername();
        final String oldPassword = InBandRegistrationTestUtils.randomPassword();
        final String newPassword = InBandRegistrationTestUtils.randomPassword();
        InBandRegistrationTestUtils.createAccount(environment, username, oldPassword);
        try {
            final AbstractXMPPConnection connection = InBandRegistrationTestUtils.connectAndLogin(environment, sinttestConfiguration, username, oldPassword);
            try {
                final Map<String, String> fields = new HashMap<>();
                fields.put("username", username);
                fields.put("password", newPassword);
                final Registration request = new Registration(fields);
                request.setType(IQ.Type.set);
                request.setTo(connection.getXMPPServiceDomain());

                // Execute system-under-test.
                try {
                    connection.sendIqRequestAndWaitForResponse(request);
                } catch (XMPPException.XMPPErrorException e) {
                    final StanzaError.Condition condition = e.getStanzaError().getCondition();
                    if (condition == StanzaError.Condition.not_allowed || condition == StanzaError.Condition.not_authorized || DataForm.from(e.getStanza(), "jabber:iq:register:changepassword") != null) {
                        throw new TestNotPossibleException("Domain '" + connection.getXMPPServiceDomain() + "' does not allow (unassisted) in-band password changes (it responded with: " + e.getStanzaError() + ").");
                    }
                    fail("Expected the domain '" + connection.getXMPPServiceDomain() + "' to accept a password change request from '" + connection.getUser() + "' (but it returned an error: " + e.getStanzaError() + ").");
                } catch (SmackException.NoResponseException e) {
                    fail("Expected the domain '" + connection.getXMPPServiceDomain() + "' to respond to a password change request from '" + connection.getUser() + "' (but it did not respond at all).");
                }
            } finally {
                connection.disconnect();
            }

            // Verify result.
            assertTrue(InBandRegistrationTestUtils.canLogIn(environment, sinttestConfiguration, username, newPassword), "Expected to be able to authenticate as '" + username + "' using the new password, after the domain accepted a request to change the password (but authentication failed).");
            assertFalse(InBandRegistrationTestUtils.canLogIn(environment, sinttestConfiguration, username, oldPassword), "Expected to no longer be able to authenticate as '" + username + "' using the old password, after the domain accepted a request to change the password (but authentication succeeded).");
        } finally {
            InBandRegistrationTestUtils.removeAccount(environment, username, newPassword, oldPassword);
        }
    }

    /**
     * Asserts that a password change request that contains an empty password does not change the password.
     */
    @SmackIntegrationTest(section = "3.3", quote = "If the user provides an empty password element or a password element that contains no XML character data (i.e., either <password/> or <password></password>), the server or service MUST NOT change the password to a null value, but instead MUST maintain the existing password.")
    public void testEmptyPasswordChangeMaintainsExistingPassword() throws Exception
    {
        InBandRegistrationTestUtils.requireDiscoFeature(conOne);

        // Setup test fixture.
        final String username = InBandRegistrationTestUtils.randomUsername();
        final String password = InBandRegistrationTestUtils.randomPassword();
        InBandRegistrationTestUtils.createAccount(environment, username, password);
        try {
            final AbstractXMPPConnection connection = InBandRegistrationTestUtils.connectAndLogin(environment, sinttestConfiguration, username, password);
            try {
                final Map<String, String> fields = new HashMap<>();
                fields.put("username", username);
                fields.put("password", "");
                final Registration request = new Registration(fields);
                request.setType(IQ.Type.set);
                request.setTo(connection.getXMPPServiceDomain());

                // Execute system-under-test.
                try {
                    connection.sendIqRequestAndWaitForResponse(request);
                } catch (XMPPException.XMPPErrorException | SmackException.NoResponseException e) {
                    // Any response (or none) is acceptable. The test asserts the resulting state of the account.
                }
            } finally {
                connection.disconnect();
            }

            // Verify result.
            assertTrue(InBandRegistrationTestUtils.canLogIn(environment, sinttestConfiguration, username, password), "Expected to still be able to authenticate as '" + username + "' using the original password, after a password change request that contained an empty password (but authentication failed).");
        } finally {
            InBandRegistrationTestUtils.removeAccount(environment, username, password, "");
        }
    }

    /**
     * Asserts that a registered entity can cancel its registration, and that doing so removes only the account of
     * the entity that sent the request.
     *
     * The domain advertises In-Band Registration, and the specification defines how a domain signals that it does
     * not allow in-band cancellation (in which case this test is skipped).
     */
    @SmackIntegrationTest(section = "3.2", quote = "The 'jabber:iq:register' namespace also makes it possible for an entity to cancel a registration with a host by sending a <remove/> element in an IQ set. The host MUST determine the identity of the requesting entity based on the 'from' address of the IQ get.")
    public void testCancelRegistration() throws Exception
    {
        InBandRegistrationTestUtils.requireDiscoFeature(conOne);

        // Setup test fixture.
        final String usernameA = InBandRegistrationTestUtils.randomUsername();
        final String passwordA = InBandRegistrationTestUtils.randomPassword();
        final String usernameB = InBandRegistrationTestUtils.randomUsername();
        final String passwordB = InBandRegistrationTestUtils.randomPassword();
        InBandRegistrationTestUtils.createAccount(environment, usernameA, passwordA);
        try {
            InBandRegistrationTestUtils.createAccount(environment, usernameB, passwordB);
            try {
                final AbstractXMPPConnection connection = InBandRegistrationTestUtils.connectAndLogin(environment, sinttestConfiguration, usernameA, passwordA);
                try {
                    final Map<String, String> fields = new HashMap<>();
                    fields.put("remove", "");
                    final Registration request = new Registration(fields);
                    request.setType(IQ.Type.set);
                    // Per the XEP, the request to the home server SHOULD NOT include a 'to' address.

                    // Execute system-under-test.
                    try {
                        connection.sendIqRequestAndWaitForResponse(request);
                    } catch (XMPPException.XMPPErrorException e) {
                        final StanzaError.Condition condition = e.getStanzaError().getCondition();
                        if (condition == StanzaError.Condition.not_allowed || condition == StanzaError.Condition.forbidden || DataForm.from(e.getStanza(), "jabber:iq:register:cancel") != null) {
                            throw new TestNotPossibleException("Domain '" + connection.getXMPPServiceDomain() + "' does not allow (unassisted) in-band cancellation of registrations (it responded with: " + e.getStanzaError() + ").");
                        }
                        fail("Expected the domain '" + connection.getXMPPServiceDomain() + "' to accept a request from '" + connection.getUser() + "' to cancel its registration (but it returned an error: " + e.getStanzaError() + ").");
                    } catch (SmackException.NoResponseException | SmackException.NotConnectedException e) {
                        // The server SHOULD return a <not-authorized/> stream error, terminating the session.
                        if (connection.isConnected()) {
                            fail("Expected the domain '" + connection.getXMPPServiceDomain() + "' to respond to a request from '" + connection.getUser() + "' to cancel its registration (but it did not respond at all).");
                        }
                    }
                } finally {
                    InBandRegistrationTestUtils.disconnectAfterRemoval(connection);
                }

                // Verify result.
                assertFalse(InBandRegistrationTestUtils.canLogIn(environment, sinttestConfiguration, usernameA, passwordA), "Expected to no longer be able to authenticate as '" + usernameA + "' after the domain accepted a request from that entity to cancel its registration (but authentication succeeded).");
                assertTrue(InBandRegistrationTestUtils.canLogIn(environment, sinttestConfiguration, usernameB, passwordB), "Expected account '" + usernameB + "' to be unaffected by '" + usernameA + "' cancelling its own registration (but authentication as '" + usernameB + "' failed).");
            } finally {
                InBandRegistrationTestUtils.removeAccount(environment, usernameB, passwordB);
            }
        } finally {
            InBandRegistrationTestUtils.removeAccount(environment, usernameA, passwordA);
        }
    }

    /**
     * Asserts that a request to cancel a registration is rejected with a bad-request error when the query element
     * contains child elements other than the remove element, and that the account is not removed.
     *
     * The XEP does not use normative language for its 'Unregister Error Cases' table. This project has decided to
     * treat that table as normative. The XML schema for the 'jabber:iq:register' namespace (which defines the content
     * of the query element as a choice between the registration fields and the remove element) agrees with it.
     */
    @SmackIntegrationTest(section = "3.2", quote = "As specified below, several error cases are possible. [...] <bad-request/>: The <remove/> element was not the only child element of the <query/> element.")
    public void testRemoveWithOtherChildElementsRejected() throws Exception
    {
        InBandRegistrationTestUtils.requireDiscoFeature(conOne);

        // Setup test fixture.
        final String username = InBandRegistrationTestUtils.randomUsername();
        final String password = InBandRegistrationTestUtils.randomPassword();
        InBandRegistrationTestUtils.createAccount(environment, username, password);
        try {
            final AbstractXMPPConnection connection = InBandRegistrationTestUtils.connectAndLogin(environment, sinttestConfiguration, username, password);
            boolean removalAccepted = false;
            try {
                final Map<String, String> fields = new HashMap<>();
                fields.put("remove", "");
                fields.put("username", username);
                fields.put("password", password);
                final Registration request = new Registration(fields);
                request.setType(IQ.Type.set);

                // Execute system-under-test.
                try {
                    connection.sendIqRequestAndWaitForResponse(request);
                    removalAccepted = true;
                    fail("Expected the domain '" + connection.getXMPPServiceDomain() + "' to reject a request from '" + connection.getUser() + "' to cancel its registration, that contained child elements other than <remove/> (but the request was accepted).");
                } catch (XMPPException.XMPPErrorException e) {
                    // Verify result.
                    final StanzaError.Condition condition = e.getStanzaError().getCondition();
                    if (condition == StanzaError.Condition.not_allowed || condition == StanzaError.Condition.forbidden || DataForm.from(e.getStanza(), "jabber:iq:register:cancel") != null) {
                        throw new TestNotPossibleException("Domain '" + connection.getXMPPServiceDomain() + "' does not allow (unassisted) in-band cancellation of registrations (it responded with: " + e.getStanzaError() + ").");
                    }
                    assertEquals(StanzaError.Condition.bad_request, condition, "Unexpected error condition in the response of domain '" + connection.getXMPPServiceDomain() + "' to a request from '" + connection.getUser() + "' to cancel its registration, that contained child elements other than <remove/>.");
                } catch (SmackException.NoResponseException | SmackException.NotConnectedException e) {
                    if (connection.isConnected()) {
                        fail("Expected the domain '" + connection.getXMPPServiceDomain() + "' to reject a request from '" + connection.getUser() + "' to cancel its registration, that contained child elements other than <remove/> (but it did not respond at all).");
                    }
                    removalAccepted = true;
                    fail("Expected the domain '" + connection.getXMPPServiceDomain() + "' to reject a request from '" + connection.getUser() + "' to cancel its registration, that contained child elements other than <remove/> (but the server terminated the session instead, which suggests that the registration was cancelled).");
                }
            } finally {
                if (removalAccepted) {
                    InBandRegistrationTestUtils.disconnectAfterRemoval(connection);
                } else {
                    connection.disconnect();
                }
            }

            assertTrue(InBandRegistrationTestUtils.canLogIn(environment, sinttestConfiguration, username, password), "Expected account '" + username + "' to remain registered after the domain rejected a malformed request to cancel its registration (but authentication failed).");
        } finally {
            InBandRegistrationTestUtils.removeAccount(environment, username, password);
        }
    }

    /**
     * Asserts that a password change request that does not include the username element is rejected with a
     * bad-request error, if the domain requires the username element.
     *
     * The XEP does not use normative language for its 'Password Change Error Cases' table. This project has decided
     * to treat that table as normative. Whether a domain requires the username element is not advertised. When the
     * domain accepts the request, this test is therefore skipped rather than failed.
     *
     * The 'unexpected-request' row of the same table (a password change request from an entity that has not
     * registered with an instant messaging server) is not tested. An unauthenticated entity that sends a username
     * and password sends exactly the same stanza as an entity that registers a new account.
     */
    @SmackIntegrationTest(section = "3.3", quote = "Several error conditions are possible: [...] <bad-request/>: The password change request does not contain complete information (e.g., the service requires inclusion of the <username/> element).")
    public void testPasswordChangeWithoutUsernameRejected() throws Exception
    {
        InBandRegistrationTestUtils.requireDiscoFeature(conOne);

        // Setup test fixture.
        final String username = InBandRegistrationTestUtils.randomUsername();
        final String oldPassword = InBandRegistrationTestUtils.randomPassword();
        final String newPassword = InBandRegistrationTestUtils.randomPassword();
        InBandRegistrationTestUtils.createAccount(environment, username, oldPassword);
        try {
            final AbstractXMPPConnection connection = InBandRegistrationTestUtils.connectAndLogin(environment, sinttestConfiguration, username, oldPassword);
            try {
                final Map<String, String> fields = new HashMap<>();
                fields.put("password", newPassword);
                final Registration request = new Registration(fields);
                request.setType(IQ.Type.set);
                request.setTo(connection.getXMPPServiceDomain());

                // Execute system-under-test.
                try {
                    connection.sendIqRequestAndWaitForResponse(request);
                    throw new TestNotPossibleException("Domain '" + connection.getXMPPServiceDomain() + "' does not require the <username/> element in a password change request.");
                } catch (XMPPException.XMPPErrorException e) {
                    // Verify result.
                    final StanzaError.Condition condition = e.getStanzaError().getCondition();
                    if (condition == StanzaError.Condition.not_allowed || condition == StanzaError.Condition.not_authorized || DataForm.from(e.getStanza(), "jabber:iq:register:changepassword") != null) {
                        throw new TestNotPossibleException("Domain '" + connection.getXMPPServiceDomain() + "' does not allow (unassisted) in-band password changes (it responded with: " + e.getStanzaError() + ").");
                    }
                    assertEquals(StanzaError.Condition.bad_request, condition, "Unexpected error condition in the response of domain '" + connection.getXMPPServiceDomain() + "' to a password change request from '" + connection.getUser() + "' that did not contain a <username/> element.");
                } catch (SmackException.NoResponseException e) {
                    fail("Expected the domain '" + connection.getXMPPServiceDomain() + "' to respond with a <bad-request/> error to a password change request from '" + connection.getUser() + "' that did not contain a <username/> element (but it did not respond at all).");
                }
            } finally {
                connection.disconnect();
            }
        } finally {
            InBandRegistrationTestUtils.removeAccount(environment, username, oldPassword, newPassword);
        }
    }
}
