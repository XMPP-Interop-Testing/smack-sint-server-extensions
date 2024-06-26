/**
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
package org.igniterealtime.smack.inttest.xep0115;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.igniterealtime.smack.inttest.util.ResultSyncPoint;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.AbstractPresenceEventListener;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.caps.EntityCapsManager;
import org.jivesoftware.smackx.caps.packet.CapsExtension;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jxmpp.jid.FullJid;

import javax.xml.namespace.QName;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Integration tests for the Caps Optimization functionality as described in XEP-0115: Entity Capabilities
 *
 * @see <a href="https://xmpp.org/extensions/xep-0115.html#impl-optimize">XEP-0115: Entity Capabilities § 8.4 Caps Optimization</a>
 */
@SpecificationReference(document = "XEP-0115", version = "1.6.0")
public class EntityCapabilitiesOptimizationIntegrationTest extends AbstractSmackIntegrationTest
{
    public EntityCapabilitiesOptimizationIntegrationTest(SmackIntegrationTestEnvironment environment) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        super(environment);

        if (!EntityCapsManager.getInstanceFor(conOne).areEntityCapsSupportedByServer()) {
            throw new TestNotPossibleException("Domain does not seem support XEP-0115 Entity Capabilities.");
        }

        if (!ServiceDiscoveryManager.getInstanceFor(environment.conOne).supportsFeature(environment.conOne.getXMPPServiceDomain(), "http://jabber.org/protocol/caps#optimize")) {
            throw new TestNotPossibleException("Domain does not seem support the Caps Optimization feature of XEP-0115 Entity Capabilities.");
        }
    }

    /**
     * Asserts that the <em>first</em> presence notification received by a subscriber contains a XEP-0115 child element.
     */
    @SmackIntegrationTest(section = "8.4", quote = "If the server performs caps optimization, it MUST ensure that the first presence notification each subscriber receives contains the annotation.")
    public void testFirstPresence() throws Exception
    {
        // Prepare test fixture
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo); // Unsubscribe, to be able to capture the _first_ presence notification post (re)subscription.

        final ResultSyncPoint<Presence, Exception> oneReceivedFromTwo = new ResultSyncPoint<>();
        Roster.getInstanceFor(conOne).addPresenceEventListener(new AbstractPresenceEventListener() {
            @Override
            public void presenceAvailable(FullJid fullJid, Presence presence) {
                if (fullJid.equals(conTwo.getUser())) {
                    oneReceivedFromTwo.signal(presence);
                }
            }
        });

        // Execute system under test.
        IntegrationTestRosterUtil.ensureSubscribedTo(conTwo, conOne, timeout);

        // Verify results.
        final Presence result = assertResult(oneReceivedFromTwo, "Expected '" + conOne.getUser() + "' to receive presence notification from '" + conTwo.getUser() + "' after subscribing (but no presence notification was received).");
        assertNotNull(result.getExtension(new QName(EntityCapsManager.NAMESPACE, EntityCapsManager.ELEMENT)), "Expected the presence notification as received by '" + conOne.getUser() + "' after subscribing to '" + conTwo.getUser() + "' to contain a entity capabilities annotation (but it did not).");
    }

    /**
     * Asserts that the <em>first</em> presence notification received by a subscriber contains a XEP-0115 child element.
     */
    @SmackIntegrationTest(section = "8.4", quote = "The server MUST [...] ensure that any changes in the caps information (e.g., an updated 'ver' attribute) are sent to all subscribers.")
    public void testUpdated() throws Exception
    {
        // Prepare test fixture
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo); // Unsubscribe, to be able to capture the _first_ presence notification post (re)subscription.

        final ResultSyncPoint<Presence, Exception> origPoint = new ResultSyncPoint<>();
        Roster.getInstanceFor(conOne).addPresenceEventListener(new AbstractPresenceEventListener() {
            @Override
            public void presenceAvailable(FullJid fullJid, Presence presence) {
                if (fullJid.equals(conTwo.getUser())) {
                    origPoint.signal(presence);
                }
            }
        });
        IntegrationTestRosterUtil.ensureSubscribedTo(conTwo, conOne, timeout);
        final String originalVer = ((CapsExtension) origPoint.waitForResult(timeout).getExtension(new QName(EntityCapsManager.NAMESPACE, EntityCapsManager.ELEMENT))).getVer();

        final ResultSyncPoint<Presence, Exception> updatePoint = new ResultSyncPoint<>();
        Roster.getInstanceFor(conOne).addPresenceEventListener(new AbstractPresenceEventListener() {
            @Override
            public void presenceAvailable(FullJid fullJid, Presence presence) {
                if (fullJid.equals(conTwo.getUser())) {
                    updatePoint.signal(presence);
                }
            }
        });

        // Execute system under test.
        ServiceDiscoveryManager.getInstanceFor(conTwo).addFeature("urn:example:xmppinteroptesting:" + StringUtils.randomString(17));

        // Verify results.
        final Presence result = assertResult(updatePoint, "Expected '" + conOne.getUser() + "' to receive presence notification from '" + conTwo.getUser() + "' after its 'ver' value changed (but no presence notification was received).");
        final CapsExtension caps = (CapsExtension) result.getExtension(new QName(EntityCapsManager.NAMESPACE, EntityCapsManager.ELEMENT));
        assertNotNull(caps, "Expected the presence notification as received by '" + conOne.getUser() + "' after '" + conTwo.getUser() + "' updated its 'ver' value to contain a entity capabilities annotation (but it did not).");
        assertNotSame(originalVer, caps.getVer(), "Expected the 'ver' as received by '" + conOne.getUser() + "' after '" + conTwo.getUser() + "' updated its 'ver' value to be different from the value that was received previously (but it was not).");
    }
}
