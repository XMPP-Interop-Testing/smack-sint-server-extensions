/*
 * Copyright 2025 Guus der Kinderen
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
package org.igniterealtime.smack.inttest.xep0280;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smackx.carbons.CarbonManager;

import static org.junit.jupiter.api.Assertions.*;

@SpecificationReference(document = "XEP-0280", version = "1.0.1")
public class MessageCarbonsIntegrationTest extends AbstractSmackIntegrationTest
{
    public MessageCarbonsIntegrationTest(SmackIntegrationTestEnvironment environment) throws XMPPException, SmackException, InterruptedException, TestNotPossibleException
    {
        super(environment);
        final CarbonManager carbonManager = CarbonManager.getInstanceFor(environment.conOne);
        if (!carbonManager.isSupportedByServer()) {
            throw new TestNotPossibleException("Domain does not seem to support XEP-0280 Message Carbons.");
        }

        try {
            boolean wasEnabled = carbonManager.getCarbonsEnabled();
            if (!wasEnabled) {
                carbonManager.enableCarbons();
            }
            carbonManager.disableCarbons();
            if (wasEnabled) {
                carbonManager.enableCarbons();
            }
        } catch (XMPPException.XMPPErrorException e) {
            if (e.getStanzaError().getCondition().equals(StanzaError.Condition.forbidden)) {
                throw new TestNotPossibleException("Domain does not allow test account to enable/disable XEP-0280 Message Carbons.");
            } else {
                // Ignore (and let the tests in this class error out instead).
            }
        }
    }

    @SmackIntegrationTest(section = "4", quote = "When a client wants to participate in the Carbons protocol, it enables the protocol by sending an IQ-set containing a child element <enable/> qualified by the namespace \"urn:xmpp:carbons:2\" [...] The server will respond with an IQ-result when Carbons are enabled")
    public void enableTest() throws SmackException, InterruptedException, XMPPException, TestNotPossibleException
    {
        // Setup test fixture.
        final CarbonManager carbonManager = CarbonManager.getInstanceFor(conOne);
        if (carbonManager.getCarbonsEnabled()) {
            // Apparently, already enabled. No need to test this.
            return;
        }

        // Execute system under test.
        try
        {
            carbonManager.enableCarbons();

            // Verify result.
            assertTrue(carbonManager.getCarbonsEnabled(), "Expected carbons to be enabled for '" + conOne.getUser() + "' after it sent a request to enable that feature (but the feature wasn't enabled)."); // This arguably is more of a test of Smack's API than of the server.
        }
        catch (XMPPException.XMPPErrorException e) {
            fail("After trying to enable carbons, '" + conOne.getUser() + "' unexpectedly received an error: " + e.getStanzaError());
        }
        finally {
            // Tear down test fixture.
            carbonManager.disableCarbons();
        }
    }

    @SmackIntegrationTest(section = "5", quote = "When a client wants to participate in the Carbons protocol, it enables the protocol by sending an IQ-set containing a child element <enable/> qualified by the namespace \"urn:xmpp:carbons:2\" [...] The server will respond with an IQ-result when Carbons are enabled")
    public void disableTest() throws SmackException, InterruptedException, XMPPException, TestNotPossibleException
    {
        // Setup test fixture.
        final CarbonManager carbonManager = CarbonManager.getInstanceFor(conOne);
        final boolean wasEnabled = carbonManager.getCarbonsEnabled();
        if (!wasEnabled) {
            carbonManager.enableCarbons();
        }

        // Execute system under test.
        try
        {
            carbonManager.disableCarbons();

            // Verify result.
            assertFalse(carbonManager.getCarbonsEnabled(), "Expected carbons to be disabled for '" + conOne.getUser() + "' after it sent a request to disable that feature (but the feature wasn't enabled)."); // This arguably is more of a test of Smack's API than of the server.
        }
        catch (XMPPException.XMPPErrorException e) {
            fail("After trying to disable carbons, '" + conOne.getUser() + "' unexpectedly received an error: " + e.getStanzaError());
        }
        finally {
            // Tear down test fixture.
            if (wasEnabled) {
                carbonManager.enableCarbons();
            }
        }
    }

    @SmackIntegrationTest(section = "10.1", quote = "If a client is permitted to enable Carbons during its login session, the server MUST allow the client to enable and disable the protocol multiple times within a session.")
    public void enableDisableMultipleTimesTest() throws SmackException, InterruptedException, XMPPException, TestNotPossibleException
    {
        // Setup test fixture.
        final CarbonManager carbonManager = CarbonManager.getInstanceFor(conOne);
        final boolean wasEnabled = carbonManager.getCarbonsEnabled();

        // Execute system under test.
        try
        {
            for (int i=0; i<4; i++) {
                if (carbonManager.getCarbonsEnabled()) {
                    carbonManager.disableCarbons();
                } else {
                    carbonManager.enableCarbons();
                }
            }
        }
        catch (XMPPException.XMPPErrorException e) {
            // Verify results
            fail("While trying to enable and disable carbons repeatedly, '" + conOne.getUser() + "' unexpectedly received an error: " + e.getStanzaError());
        }
        finally {
            // Tear down test fixture.
            if (wasEnabled && !carbonManager.getCarbonsEnabled()) {
                carbonManager.enableCarbons();
            }
            if (!wasEnabled && carbonManager.getCarbonsEnabled()) {
                carbonManager.disableCarbons();
            }
        }
    }
}
