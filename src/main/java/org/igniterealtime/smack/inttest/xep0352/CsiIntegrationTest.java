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
package org.igniterealtime.smack.inttest.xep0352;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smackx.csi.ClientStateIndicationManager;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpecificationReference(document = "XEP-0352", version = "1.0.0")
public class CsiIntegrationTest extends AbstractSmackIntegrationTest
{
    public CsiIntegrationTest(final SmackIntegrationTestEnvironment environment) throws TestNotPossibleException
    {
        super(environment);

        if (!ClientStateIndicationManager.isSupported(environment.conOne)) {
            throw new TestNotPossibleException("Domain does not seem support XEP-0352 Client State Indication.");
        }
    }

    @SmackIntegrationTest(section = "4.2", quote = "If a client wishes to inform the server that it has become inactive, it sends an <inactive/> element in the 'urn:xmpp:csi:0' namespace")
    public void markActiveTest() throws SmackException.NotConnectedException, InterruptedException
    {
        ClientStateIndicationManager.inactive(conOne);
        // TODO do we need to reset the CSI-state of conOne to not affect other tests?
    }

    /**
     * This test implements the 'ping' based example in section 5.1, where a connection is marked inactive, gets sent
     * some data, then is marked active
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     */
    @SmackIntegrationTest(section = "4.2", quote = "[...] when the client is active again it sends an <active/> element.")
    public void markInactiveTest() throws SmackException.NotConnectedException, InterruptedException
    {
        ClientStateIndicationManager.inactive(conOne);
        ClientStateIndicationManager.active(conOne);
        // TODO do we need to reset the CSI-state of conOne to not affect other tests?
    }

    @SmackIntegrationTest(section = "6", quote = "To protect the privacy of users, servers MUST NOT reveal the clients active/inactive state to other entities on the network.")
    public void detectDiscoInfoChangeTestUnsubscribed() throws SmackException.NotConnectedException, InterruptedException, XMPPException.XMPPErrorException, SmackException.NoResponseException, SmackException.NotLoggedInException, TestNotPossibleException
    {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);
        detectDiscoInfoChangeTest();
    }

    @SmackIntegrationTest(section = "6", quote = "To protect the privacy of users, servers MUST NOT reveal the clients active/inactive state to other entities on the network.")
    public void detectDiscoInfoChangeTestSubscribed() throws Exception
    {
        IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(conOne, conTwo, timeout);
        detectDiscoInfoChangeTest();
    }

    protected void detectDiscoInfoChangeTest() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        final String node = null; // Use a 'null' node to bypass Smack's caching mechanism, triggering a fresh lookup.
        final ServiceDiscoveryManager discoManagerTwo = ServiceDiscoveryManager.getInstanceFor(conTwo);
        final DiscoverInfo infoResponse1 = discoManagerTwo.discoverInfo(conOne.getUser(), node);
        final DiscoverInfo infoResponse2 = discoManagerTwo.discoverInfo(conOne.getUser(), node);
        if (infoResponse1 == infoResponse2) {
            // Both responses should not be the exact same instance if Smack's caching was busted. Apparently, the implementation of this test
            // does no longer bust the cache. This makes the rest of the test pointless.
            throw new UnsupportedOperationException("The test implementation contains a bug.");
        }

        if (!areSemanticallyEqual(infoResponse1, infoResponse2)) {
            throw new TestNotPossibleException("No consistent disco#info response from '" + conOne.getUser() + "' as received by '" + conTwo.getUser() + "' (when no changes are applied).");
        }

        ClientStateIndicationManager.inactive(conOne);
        final DiscoverInfo infoResponseAfterInactivation = discoManagerTwo.discoverInfo(conOne.getUser(), node);
        assertTrue(areSemanticallyEqual(infoResponse1, infoResponseAfterInactivation), "Expected the disco#info responses from '" + conOne.getUser() + "' as received by '" + conTwo.getUser() + "' before and after marking '" + conOne.getUser() + "''s client state as 'inactive' to be semantically equal to each-other (but they were not).");

        // TODO do we need to reset the CSI-state of conOne to not affect other tests?
    }

    @SmackIntegrationTest(section = "6", quote = "To protect the privacy of users, servers MUST NOT reveal the clients active/inactive state to other entities on the network.")
    public void detectDiscoItemsChangeTestUnsubscribed() throws SmackException.NotConnectedException, InterruptedException, XMPPException.XMPPErrorException, SmackException.NoResponseException, SmackException.NotLoggedInException, TestNotPossibleException
    {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);
        detectDiscoItemsChangeTest();
    }

    @SmackIntegrationTest(section = "6", quote = "To protect the privacy of users, servers MUST NOT reveal the clients active/inactive state to other entities on the network.")
    public void detectDiscoItemsChangeTestSubscribed() throws Exception
    {
        IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(conOne, conTwo, timeout);
        detectDiscoItemsChangeTest();
    }

    protected void detectDiscoItemsChangeTest() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        final ServiceDiscoveryManager discoManagerTwo = ServiceDiscoveryManager.getInstanceFor(conTwo);
        final DiscoverItems itemsResponse1 = discoManagerTwo.discoverItems(conOne.getUser());
        final DiscoverItems itemsResponse2 = discoManagerTwo.discoverItems(conOne.getUser());
        if (itemsResponse1 == itemsResponse2) {
            // Both responses should not be the exact same instance if Smack's caching was busted. Apparently, the implementation of this test
            // does no longer bust the cache. This makes the rest of the test pointless.
            throw new UnsupportedOperationException("The test implementation contains a bug.");
        }

        if (!areSemanticallyEqual(itemsResponse1, itemsResponse2)) {
            throw new TestNotPossibleException("No consistent disco#info response from '" + conOne.getUser() + "' as received by '" + conTwo.getUser() + "' (when no changes are applied).");
        }

        ClientStateIndicationManager.inactive(conOne);
        final DiscoverItems infoResponseAfterInactivation = discoManagerTwo.discoverItems(conOne.getUser());
        assertTrue(areSemanticallyEqual(itemsResponse1, infoResponseAfterInactivation), "Expected the disco#items responses from '" + conOne.getUser() + "' as received by '" + conTwo.getUser() + "' before and after marking '" + conOne.getUser() + "''s client state as 'inactive' to be semantically equal to each-other (but they were not).");

        // TODO do we need to reset the CSI-state of conOne to not affect other tests?
    }

    // TODO add a test that compares presence data of a connection that toggles between active and inactive (the presence should not change). How does one test for the absence of a change, without adding an undesirable delay?

    public static boolean areSemanticallyEqual(final DiscoverInfo one, final DiscoverInfo two) {
        return new HashSet<>(one.getFeatures()).equals(new HashSet<>(two.getFeatures()))
            && new HashSet<>(one.getIdentities()).equals(new HashSet<>(two.getIdentities()));
    }

    public static boolean areSemanticallyEqual(final DiscoverItems one, final DiscoverItems two) {
        return new HashSet<>(one.getItems()).equals(new HashSet<>(two.getItems()))
            && Objects.equals(one.getNode(), two.getNode());
    }
}
