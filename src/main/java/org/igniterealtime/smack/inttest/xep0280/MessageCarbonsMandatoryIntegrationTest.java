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
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

import static org.junit.jupiter.api.Assertions.*;

@SpecificationReference(document = "XEP-0280", version = "1.0.1")
public class MessageCarbonsMandatoryIntegrationTest extends AbstractSmackIntegrationTest
{
    public MessageCarbonsMandatoryIntegrationTest(SmackIntegrationTestEnvironment environment) throws XMPPException, SmackException, InterruptedException, TestNotPossibleException
    {
        super(environment);
        final CarbonManager carbonManager = CarbonManager.getInstanceFor(environment.conOne);
        if (!carbonManager.isSupportedByServer()) {
            throw new TestNotPossibleException("Domain does not seem to support XEP-0280 Message Carbons.");
        }

        if (!ServiceDiscoveryManager.getInstanceFor(environment.conOne).supportsFeature(environment.conOne.getXMPPServiceDomain(), "urn:xmpp:carbons:rules:0")) {
            throw new TestNotPossibleException("Domain does not seem to support the XEP-0280 Message Carbons 'urn:xmpp:carbons:rules:0' feature.");
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
}
