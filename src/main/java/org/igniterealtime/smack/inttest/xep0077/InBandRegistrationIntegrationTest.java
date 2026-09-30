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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for XEP-0077: In-Band Registration, that are executed by an entity that has not (yet)
 * authenticated with the server: registering a new account.
 *
 * All tests require the server to advertise In-Band Registration as a stream feature. Tests that also operate on an
 * existing account additionally require the service discovery feature.
 */
@SpecificationReference(document = "XEP-0077", version = "2.4")
public class InBandRegistrationIntegrationTest extends AbstractSmackIntegrationTest
{
    private final SmackIntegrationTestEnvironment environment;

    public InBandRegistrationIntegrationTest(SmackIntegrationTestEnvironment environment)
    {
        super(environment);
        this.environment = environment;
    }

    /**
     * Asserts that an unregistered entity that requests the registration fields is informed of them.
     */
    @SmackIntegrationTest(section = "3.1", quote = "If the entity is not already registered and the host supports In-Band Registration, the host MUST inform the entity of the required registration fields. [...] If the host is redirecting registration requests to some other medium (e.g., a website), it MAY return an <instructions/> element only")
    public void testRequestRegistrationFields() throws Exception
    {
        final AbstractXMPPConnection connection = InBandRegistrationTestUtils.connectUnauthenticated(environment, sinttestConfiguration);
        try {
            InBandRegistrationTestUtils.requireStreamFeature(connection);

            // Execute system-under-test.
            final Registration response;
            try {
                response = InBandRegistrationTestUtils.requestRegistrationFields(connection);
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected the domain '" + connection.getXMPPServiceDomain() + "', which advertises In-Band Registration, to return the registration fields to an unregistered entity that requests them (but it returned an error: " + e.getStanzaError() + ").");
                return;
            } catch (SmackException.NoResponseException e) {
                fail("Expected the domain '" + connection.getXMPPServiceDomain() + "', which advertises In-Band Registration, to return the registration fields to an unregistered entity that requests them (but it did not respond at all).");
                return;
            }

            // Verify result. The <registered/> and <remove/> elements are not registration fields.
            final Set<String> fieldNames = new HashSet<>(response.getAttributes() == null ? Set.of() : response.getAttributes().keySet());
            fieldNames.remove("registered");
            fieldNames.remove("remove");
            final boolean hasFields = !fieldNames.isEmpty();
            final boolean hasDataForm = response.hasExtension(DataForm.ELEMENT, DataForm.NAMESPACE);
            final boolean hasInstructions = response.getInstructions() != null && !response.getInstructions().isBlank();
            assertTrue(hasFields || hasDataForm || hasInstructions, "Expected the domain '" + connection.getXMPPServiceDomain() + "' to inform an unregistered entity of the required registration fields (or provide redirect instructions) when it requested them (but the response contained neither fields, a data form, nor instructions).");
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Asserts that an unregistered entity can register an account by providing the requested fields, and can then
     * authenticate using the newly registered credentials.
     *
     * This is not a normative requirement, but a determinable one: the domain advertised In-Band Registration and
     * returned iq:register fields, so providing those fields is expected to result in a registered account.
     */
    @SmackIntegrationTest(section = "3.1", quote = "If the entity is not already registered, the entity SHOULD provide the required information. [...] Host Informs Entity of Successful Registration")
    public void testRegistration() throws Exception
    {
        final String username = InBandRegistrationTestUtils.randomUsername();
        final String password = InBandRegistrationTestUtils.randomPassword();
        final AbstractXMPPConnection connection = InBandRegistrationTestUtils.connectUnauthenticated(environment, sinttestConfiguration);
        boolean registered = false;
        try {
            InBandRegistrationTestUtils.requireStreamFeature(connection);

            // Setup test fixture.
            final Map<String, String> fields = fillFieldsOrSkip(connection, username, password);
            final Registration request = new Registration(fields);
            request.setType(IQ.Type.set);
            request.setTo(connection.getXMPPServiceDomain());

            // Execute system-under-test.
            try {
                InBandRegistrationTestUtils.sendAndWait(connection, request);
                registered = true;
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected the domain '" + connection.getXMPPServiceDomain() + "' to accept registration of username '" + username + "' when all requested fields (" + fields.keySet() + ") were provided (but it returned an error: " + e.getStanzaError() + ").");
            } catch (SmackException.NoResponseException e) {
                fail("Expected the domain '" + connection.getXMPPServiceDomain() + "' to accept registration of username '" + username + "' when all requested fields (" + fields.keySet() + ") were provided (but it did not respond at all).");
            }
        } finally {
            connection.disconnect();
        }

        try {
            // Verify result.
            assertTrue(InBandRegistrationTestUtils.canLogIn(environment, sinttestConfiguration, username, password), "Expected to be able to authenticate as '" + username + "' using the credentials that were just successfully registered with the domain (but authentication failed).");
        } finally {
            if (registered) {
                InBandRegistrationTestUtils.removeAccount(environment, username, password);
            }
        }
    }

    /**
     * Asserts that registration is rejected with a not-acceptable error when an empty password element is provided.
     */
    @SmackIntegrationTest(section = "3.1", quote = "If the requesting entity does not provide all of the requested information during registration [This includes providing an empty password element or a password element that contains no XML character data, i.e., either <password/> or <password></password>.] then the server or service MUST return a <not-acceptable/> error to the requesting entity.")
    public void testRegistrationWithEmptyPasswordRejected() throws Exception
    {
        doTestRegistrationWithoutPasswordRejected(true);
    }

    /**
     * Asserts that registration is rejected with a not-acceptable error when the requested password is omitted.
     */
    @SmackIntegrationTest(section = "3.1", quote = "If the requesting entity does not provide all of the requested information during registration [...] then the server or service MUST return a <not-acceptable/> error to the requesting entity.")
    public void testRegistrationWithMissingPasswordRejected() throws Exception
    {
        doTestRegistrationWithoutPasswordRejected(false);
    }

    private void doTestRegistrationWithoutPasswordRejected(final boolean includeEmptyPasswordElement) throws Exception
    {
        final String username = InBandRegistrationTestUtils.randomUsername();
        final String description = includeEmptyPasswordElement ? "an empty password element" : "no password element";
        final AbstractXMPPConnection connection = InBandRegistrationTestUtils.connectUnauthenticated(environment, sinttestConfiguration);
        try {
            InBandRegistrationTestUtils.requireStreamFeature(connection);

            // Setup test fixture.
            final Map<String, String> fields = fillFieldsOrSkip(connection, username, "");
            if (!fields.containsKey("password")) {
                throw new TestNotPossibleException("Domain '" + connection.getXMPPServiceDomain() + "' does not request a password during registration.");
            }
            if (!includeEmptyPasswordElement) {
                fields.remove("password");
            }
            final Registration request = new Registration(fields);
            request.setType(IQ.Type.set);
            request.setTo(connection.getXMPPServiceDomain());

            // Execute system-under-test.
            try {
                InBandRegistrationTestUtils.sendAndWait(connection, request);

                // Verify result.
                InBandRegistrationTestUtils.removeAccount(environment, username, "");
                fail("Expected the domain '" + connection.getXMPPServiceDomain() + "' to reject registration of username '" + username + "' that contained " + description + " (but the registration succeeded).");
            } catch (SmackException.NoResponseException e) {
                fail("Expected the domain '" + connection.getXMPPServiceDomain() + "' to respond with a <not-acceptable/> error to a registration request for username '" + username + "' that contained " + description + " (but it did not respond at all).");
            } catch (XMPPException.XMPPErrorException e) {
                assertEquals(StanzaError.Condition.not_acceptable, e.getStanzaError().getCondition(), "Unexpected error condition in the response of domain '" + connection.getXMPPServiceDomain() + "' to a registration request for username '" + username + "' that contained " + description + ".");
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Asserts that the registration fields returned by the domain are limited to those defined in the schema.
     */
    @SmackIntegrationTest(section = "4", quote = "A host MUST NOT add new fields to the 'jabber:iq:register' namespace; instead, extensibility SHOULD be pursued via the Data Forms protocol as specified herein.")
    public void testRegistrationFieldsUseOnlySchemaElements() throws Exception
    {
        final AbstractXMPPConnection connection = InBandRegistrationTestUtils.connectUnauthenticated(environment, sinttestConfiguration);
        try {
            InBandRegistrationTestUtils.requireStreamFeature(connection);

            // Execute system-under-test.
            final Registration response;
            try {
                response = InBandRegistrationTestUtils.requestRegistrationFields(connection);
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected the domain '" + connection.getXMPPServiceDomain() + "', which advertises In-Band Registration, to return the registration fields to an unregistered entity that requests them (but it returned an error: " + e.getStanzaError() + ").");
                return;
            } catch (SmackException.NoResponseException e) {
                fail("Expected the domain '" + connection.getXMPPServiceDomain() + "', which advertises In-Band Registration, to return the registration fields to an unregistered entity that requests them (but it did not respond at all).");
                return;
            }

            // Verify result.
            final Set<String> unexpected = new HashSet<>(response.getAttributes() == null ? Set.of() : response.getAttributes().keySet());
            unexpected.removeAll(InBandRegistrationTestUtils.SCHEMA_ELEMENT_NAMES);
            assertTrue(unexpected.isEmpty(), "Expected the registration fields returned by domain '" + connection.getXMPPServiceDomain() + "' to only contain elements defined in the 'jabber:iq:register' schema (but found: " + unexpected + ").");
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Asserts that an attempt to register a username that is already registered is rejected, and does not affect
     * the existing account.
     *
     * Before attempting to register the username again, the test verifies (using a normative requirement) that the
     * domain regards the account as registered.
     */
    @SmackIntegrationTest(section = "3.1", quote = "If the host determines (based on the 'from' address) that the entity is already registered, the IQ result that it sends in response to the IQ get MUST contain an empty <registered/> element (indicating that the entity is already registered) [...] Alternatively, registration may fail. Possible causes of failure include a username conflict (the desired username is already in use by another entity)")
    public void testRegistrationOfExistingUsernameRejected() throws Exception
    {
        InBandRegistrationTestUtils.requireDiscoFeature(conOne);
        final AbstractXMPPConnection probe = InBandRegistrationTestUtils.connectUnauthenticated(environment, sinttestConfiguration);
        try {
            InBandRegistrationTestUtils.requireStreamFeature(probe);
        } finally {
            probe.disconnect();
        }

        final String username = InBandRegistrationTestUtils.randomUsername();
        final String password = InBandRegistrationTestUtils.randomPassword();
        final String otherPassword = InBandRegistrationTestUtils.randomPassword();
        InBandRegistrationTestUtils.createAccount(environment, username, password);
        try {
            // Setup test fixture: verify that the domain considers the account to be registered.
            final AbstractXMPPConnection existing = InBandRegistrationTestUtils.connectAndLogin(environment, sinttestConfiguration, username, password);
            try {
                final Registration current;
                try {
                    current = InBandRegistrationTestUtils.requestRegistrationFields(existing);
                } catch (XMPPException.XMPPErrorException | SmackException.NoResponseException e) {
                    fail("Expected the domain '" + existing.getXMPPServiceDomain() + "' to respond to a registration fields request from registered entity '" + existing.getUser() + "' with a result (but it did not: " + e.getMessage() + ").");
                    return;
                }
                assertTrue(current.getAttributes() != null && current.getAttributes().containsKey("registered"), "Expected the response of domain '" + existing.getXMPPServiceDomain() + "' to a registration fields request from registered entity '" + existing.getUser() + "' to contain a <registered/> element (but it did not).");
            } finally {
                existing.disconnect();
            }

            final AbstractXMPPConnection connection = InBandRegistrationTestUtils.connectUnauthenticated(environment, sinttestConfiguration);
            try {
                InBandRegistrationTestUtils.requireStreamFeature(connection);

                final Map<String, String> fields = fillFieldsOrSkip(connection, username, otherPassword);
                final Registration request = new Registration(fields);
                request.setType(IQ.Type.set);
                request.setTo(connection.getXMPPServiceDomain());

                // Execute system-under-test.
                try {
                    InBandRegistrationTestUtils.sendAndWait(connection, request);
                    fail("Expected the domain '" + connection.getXMPPServiceDomain() + "' to reject registration of username '" + username + "', which is already registered (but the registration succeeded).");
                } catch (SmackException.NoResponseException e) {
                    fail("Expected the domain '" + connection.getXMPPServiceDomain() + "' to reject registration of username '" + username + "', which is already registered (but it did not respond at all).");
                } catch (XMPPException.XMPPErrorException e) {
                    // Verify result.
                    // The XEP does not normatively define which error condition is to be used. The <conflict/>
                    // condition is used in its example "Host Informs Entity of Failed Registration (Username Conflict)".
                    // A server that uses another condition could arguably still be compliant.
                    assertEquals(StanzaError.Condition.conflict, e.getStanzaError().getCondition(), "Unexpected error condition in the response of domain '" + connection.getXMPPServiceDomain() + "' to a registration request for username '" + username + "', which is already registered.");
                }
            } finally {
                connection.disconnect();
            }

            assertTrue(InBandRegistrationTestUtils.canLogIn(environment, sinttestConfiguration, username, password), "Expected the existing account '" + username + "' to remain usable with its original password after another entity attempted to register the same username (but authentication failed).");
        } finally {
            InBandRegistrationTestUtils.removeAccount(environment, username, password, otherPassword);
        }
    }

    /**
     * Asserts that a request to cancel a registration from an entity that has not registered (and thus has not
     * authenticated) is rejected with one of the error conditions that the XEP defines for that case.
     *
     * The XEP does not use normative language for its 'Unregister Error Cases' table. This project has decided to
     * treat that table as normative. Two rows of the table describe this case: 'registration-required' (in general)
     * and 'unexpected-request' (specifically for instant messaging servers). Both are accepted. The 'not-allowed' row
     * (no sender is allowed to cancel registrations in-band) is a policy that this test cannot determine in advance,
     * so this test is skipped when that condition is returned.
     */
    @SmackIntegrationTest(section = "3.2", quote = "As specified below, several error cases are possible. [...] <registration-required/>: The entity sending the remove request was not previously registered. <unexpected-request/>: The host is an instant messaging server and the IQ get does not contain a 'from' address because the entity is not registered with the server.")
    public void testRemoveByUnregisteredEntityRejected() throws Exception
    {
        final AbstractXMPPConnection connection = InBandRegistrationTestUtils.connectUnauthenticated(environment, sinttestConfiguration);
        try {
            InBandRegistrationTestUtils.requireStreamFeature(connection);

            // Setup test fixture.
            final Map<String, String> fields = new HashMap<>();
            fields.put("remove", "");
            final Registration request = new Registration(fields);
            request.setType(IQ.Type.set);

            // Execute system-under-test.
            try {
                InBandRegistrationTestUtils.sendAndWait(connection, request);
                fail("Expected the domain '" + connection.getXMPPServiceDomain() + "' to reject a request to cancel a registration from an entity that is not registered (but the request was accepted).");
            } catch (SmackException.NoResponseException e) {
                fail("Expected the domain '" + connection.getXMPPServiceDomain() + "' to respond with an error to a request to cancel a registration from an entity that is not registered (but it did not respond at all).");
            } catch (XMPPException.XMPPErrorException e) {
                // Verify result.
                final StanzaError.Condition condition = e.getStanzaError().getCondition();
                if (condition == StanzaError.Condition.not_allowed) {
                    throw new TestNotPossibleException("Domain '" + connection.getXMPPServiceDomain() + "' does not allow in-band cancellation of registrations (it responded with: " + e.getStanzaError() + ").");
                }
                assertTrue(condition == StanzaError.Condition.registration_required || condition == StanzaError.Condition.unexpected_request, "Expected the domain '" + connection.getXMPPServiceDomain() + "' to respond to a request to cancel a registration from an entity that is not registered with either a <registration-required/> or <unexpected-request/> error (but it responded with: " + e.getStanzaError() + ").");
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Requests the registration fields and fills them in, or throws TestNotPossibleException if the domain does not
     * offer iq:register fields that can be filled in (for example, when it only offers a data form, or redirects to
     * another medium).
     */
    private static Map<String, String> fillFieldsOrSkip(final AbstractXMPPConnection connection, final String username, final String password) throws Exception
    {
        final Registration fieldsResponse;
        try {
            fieldsResponse = InBandRegistrationTestUtils.requestRegistrationFields(connection);
        } catch (XMPPException.XMPPErrorException e) {
            throw new TestNotPossibleException("Domain '" + connection.getXMPPServiceDomain() + "' returned an error when requesting registration fields: " + e.getStanzaError());
        } catch (SmackException.NoResponseException e) {
            throw new TestNotPossibleException("Domain '" + connection.getXMPPServiceDomain() + "' did not respond when requesting registration fields.");
        }
        final Map<String, String> fields = InBandRegistrationTestUtils.fillRequestedFields(fieldsResponse, username, password);
        if (fields == null) {
            throw new TestNotPossibleException("Domain '" + connection.getXMPPServiceDomain() + "' does not request a username in iq:register fields (it may only offer a data form, or redirect registration to another medium).");
        }
        return fields;
    }
}
