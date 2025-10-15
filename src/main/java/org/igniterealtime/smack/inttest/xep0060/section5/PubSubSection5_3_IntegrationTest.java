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
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.pubsub.NodeType;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.form.FillableConfigureForm;
import org.jxmpp.jid.DomainBareJid;

import java.util.logging.Level;

import static org.igniterealtime.smack.inttest.xep0060.PubSubUtils.assertContainsItemRepresentingNode;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests as defined in paragraph 5.3 "Discover Node Information" of section 5 "Entity Use Cases" of XEP-0060 "Publish-Subscribe".
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 * @see <a href="https://xmpp.org/extensions/xep-0060.html#entity-nodes">XEP-0060: Publish-Subscribe</a>
 */
@SpecificationReference(document = "XEP-0060", version = "1.28.0")
public class PubSubSection5_3_IntegrationTest extends AbstractSmackIntegrationTest
{
    protected final DomainBareJid pubsubServiceAddress;

    public PubSubSection5_3_IntegrationTest(SmackIntegrationTestEnvironment environment) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        super(environment);
        pubsubServiceAddress = PubSubManager.getPubSubService(conOne);
        if (pubsubServiceAddress == null) {
            throw new TestNotPossibleException("No PubSub service found");
        }
    }

    /**
     * Asserts that the pub/sub service responds to a disco#info request made against an existing leaf node.
     */
    @SmackIntegrationTest(section = "5.3", quote = "A pubsub service MUST allow entities to query individual nodes for the information associated with that node. The Service Discovery protocol MUST be used to discover this information. The \"disco#info\" result MUST include an identity with a category of \"pubsub\" and an appropriate type as registered by the XMPP registrar (e.g. \"leaf\").")
    public void testDiscoInfoLeafNode() throws TestNotPossibleException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException
    {
        // Setup test fixture.
        final String nodeId = "testleaf-" + StringUtils.randomString(5);
        final PubSubManager pubSubManagerOne = PubSubManager.getInstanceFor(conOne);
        try {
            final FillableConfigureForm config = pubSubManagerOne.getDefaultConfiguration().getFillableForm();
            config.setNodeType(NodeType.leaf);
            pubSubManagerOne.createNode(nodeId, config);
        } catch (Exception e) {
            e.printStackTrace();
            throw new TestNotPossibleException("PubSub service does not allow test user to create leaf nodes.", e);
        }
        final ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(conOne);

        try {
            // Execute system under test.
            final DiscoverInfo discoveredInfo = serviceDiscoveryManager.discoverInfo(pubsubServiceAddress, nodeId);

            // Verify results.
            assertTrue(discoveredInfo.hasIdentity("pubsub", "leaf"),
                "Expected the response to the service discovery info request that was made by '" + conOne.getUser() + "' to leaf node '" + nodeId + "' of service '" + pubsubServiceAddress + "' to contain an identity of category 'pubsub' and type 'leaf' (but no such identity was returned).");
        } catch (SmackException.NoResponseException e) {
            fail("Expected a response to the service discovery info request that was made by '" + conOne.getUser() + "' to leaf node '" + nodeId + "' of service '" + pubsubServiceAddress + "' (which advertises support for leaf nodes) but no response was received.");
        } catch (XMPPException.XMPPErrorException e) {
            fail("Expected a non-error response to the service discovery info request that was made by '" + conOne.getUser() + "' to leaf node '" + nodeId + "' of service '" + pubsubServiceAddress + "' (which advertises support for leaf nodes), but an error was received. " + e.getStanzaError());
        } finally {
            // Tear down test fixture.
            try {
                pubSubManagerOne.deleteNode(nodeId);
            } catch (XMPPException.XMPPErrorException e) {
                LOGGER.log(Level.WARNING, "Unable to delete leaf node that was created in the test fixture. Node ID: " + nodeId, e);
            }
        }
    }
}
