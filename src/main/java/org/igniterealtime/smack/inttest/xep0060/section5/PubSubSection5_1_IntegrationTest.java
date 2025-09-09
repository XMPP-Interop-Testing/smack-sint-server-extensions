/**
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
package org.igniterealtime.smack.inttest.xep0060.section5;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jxmpp.jid.DomainBareJid;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests as defined in paragraph 5.1 "Discover Features" of section 5 "Entity Use Cases" of XEP-0060 "Publish-Subscribe".
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 * @see <a href="https://xmpp.org/extensions/xep-0060.html#entity-features">XEP-0060: Publish-Subscribe</a>
 */
@SpecificationReference(document = "XEP-0060", version = "1.26.0")
public class PubSubSection5_1_IntegrationTest extends AbstractSmackIntegrationTest
{
    protected final DomainBareJid pubsubServiceAddress;
    protected final DiscoverInfo pubsubServiceInfo;

    public PubSubSection5_1_IntegrationTest(SmackIntegrationTestEnvironment environment) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        super(environment);
        pubsubServiceAddress = PubSubManager.getPubSubService(conOne);
        if (pubsubServiceAddress == null) {
            throw new TestNotPossibleException("No PubSub service found");
        }

        // Doing this here instead of in a test, so that other tests (that depend on this information being available)
        // can be marked as 'not possible' if the data is not available. Note that #testDiscoInfoNonErrorResponse will
        // ensure that at least one tests fails in that case.
        final ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(conOne);
        DiscoverInfo discoverInfo;
        try {
            discoverInfo = serviceDiscoveryManager.discoverInfo(pubsubServiceAddress);
        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException e) {
            discoverInfo = null;
        }
        this.pubsubServiceInfo = discoverInfo;
    }

    /**
     * Asserts that the pub/sub service responds to a disco#info request.
     */
    @SmackIntegrationTest(section = "5.1", quote = "A service MUST respond to service discovery information requests qualified by the 'http://jabber.org/protocol/disco#info' namespace.")
    public void testDiscoInfoNonErrorResponse()
    {
        // Setup test fixture.
        // (This is achieved in the constructor of this class).

        // Execute system under test.
        // (This is achieved in the constructor of this class).

        // Verify results.
        assertNotNull(pubsubServiceInfo, "Expected a non-error response to the service discovery information request that was made by '" + conOne.getUser() + "' to '" + pubsubServiceAddress + "', but either no response was received, or it was of type error.");
    }

    /**
     * Asserts that the pub/sub service response to a disco#info request contains a pub/sub identity.
     */
    // This test may be redundant: if the service doesn't include such an identity, the constructor likely fails to identify a suitable system-under-test.
    @SmackIntegrationTest(section = "5.1", quote = "The \"disco#info\" result returned by a pubsub service MUST indicate the identity of the service")
    public void testDiscoInfoResponseContainsIdentity() throws TestNotPossibleException
    {
        checkIfWeCanRun();
        assertTrue(pubsubServiceInfo.getIdentities().stream().anyMatch(identity -> identity.isOfCategoryAndType("pubsub", "service")),
            "Expected the service discovery information response that was returned to '" + conOne.getUser() + "' by '" + pubsubServiceAddress + "' to contain an identity of type category 'pubsub' and type 'service' (but no such identity was found).");
    }

    /**
     * Asserts that the pub/sub service response to a disco#info request contains the 'http://jabber.org/protocol/pubsub' feature that is part of the identification of the service.
     */
    @SmackIntegrationTest(section = "5.1", quote = "The \"disco#info\" result returned by a pubsub service MUST indicate the identity of the service")
    public void testDiscoInfoResponseContainsIdentityFeature() throws TestNotPossibleException
    {
        checkIfWeCanRun();
        assertServiceDiscoveryInformationContains("http://jabber.org/protocol/pubsub");
    }

    /**
     * Asserts that the pub/sub service response to a disco#info request contains the required 'http://jabber.org/protocol/pubsub#publish' feature.
     */
    @SmackIntegrationTest(section = "5.1", quote = "The \"disco#info\" result returned by a pubsub service MUST indicate [...] which pubsub features are supported. [...] For information regarding which features are required, recommended, and optional, see the Feature Summary section of this document. [...] publish - Publishing items is supported. - REQUIRED")
    public void testDiscoInfoResponseContainsFeaturePublish() throws TestNotPossibleException
    {
        checkIfWeCanRun();
        assertServiceDiscoveryInformationContains("http://jabber.org/protocol/pubsub#publish");
    }

    /**
     * Asserts that the pub/sub service response to a disco#info request contains the required 'http://jabber.org/protocol/pubsub#subscribe' feature.
     */
    @SmackIntegrationTest(section = "5.1", quote = "The \"disco#info\" result returned by a pubsub service MUST indicate [...] which pubsub features are supported. [...] For information regarding which features are required, recommended, and optional, see the Feature Summary section of this document. [...] subscribe - Subscribing and unsubscribing are supported. - REQUIRED")
    public void testDiscoInfoResponseContainsFeatureSubscribe() throws TestNotPossibleException
    {
        checkIfWeCanRun();
        assertServiceDiscoveryInformationContains("http://jabber.org/protocol/pubsub#subscribe");
    }

    /**
     * Verifies that (most of the) tests in this class can run, throwing a TestNotPossibleException if that's not the
     * case.
     *
     * This method checks that the pub/sub service provided a non-empty service discovery information response.
     *
     * @throws TestNotPossibleException When the pub/sub service does not provide a usable service discovery information response.
     */
    public void checkIfWeCanRun() throws TestNotPossibleException
    {
        if (pubsubServiceInfo == null) {
            throw new TestNotPossibleException("PubSub service service discovery information response was missing or of type error.");
        }
    }

    /**
     * A wrapper that removes boilerplate assertion code, to assert that the recorded discovery information response
     * from the pubsub service contains a particular feature
     *
     * @param feature The feature that is expected to be in the response.
     */
    protected void assertServiceDiscoveryInformationContains(final String feature) throws TestNotPossibleException
    {
        assertTrue(pubsubServiceInfo.containsFeature(feature),
            "Expected the service discovery information response that was returned to '" + conOne.getUser() + "' by '" + pubsubServiceAddress + "' to contain the feature '" + feature + "' (but no such feature was found).");
    }
}
