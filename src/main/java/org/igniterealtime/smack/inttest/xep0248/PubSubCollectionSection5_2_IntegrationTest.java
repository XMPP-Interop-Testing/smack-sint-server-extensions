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
package org.igniterealtime.smack.inttest.xep0248;

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
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests as defined in paragraph 5.2 "Discover Nodes" of section 5 "Entity Use Cases" of XEP-0248 "PubSub Collection Nodes".
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 * @see <a href="https://xmpp.org/extensions/xep-0248.html#disco-nodes">XEP-0248: PubSub Collection Nodes</a>
 */
@SpecificationReference(document = "XEP-0248", version = "0.5.0")
public class PubSubCollectionSection5_2_IntegrationTest extends AbstractSmackIntegrationTest
{
    protected final DomainBareJid pubsubServiceAddress;

    public PubSubCollectionSection5_2_IntegrationTest(SmackIntegrationTestEnvironment environment) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        super(environment);
        pubsubServiceAddress = PubSubManager.getPubSubService(conOne);
        if (pubsubServiceAddress == null) {
            throw new TestNotPossibleException("No PubSub service found");
        }

        final DiscoverInfo pubsubServiceInfo;
        try {
            final ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(conOne);
            pubsubServiceInfo = serviceDiscoveryManager.discoverInfo(pubsubServiceAddress);
        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException e) {
            throw new TestNotPossibleException("PubSub service service discovery information response was missing or of type error.", e);
        }

        if (!pubsubServiceInfo.containsFeature("http://jabber.org/protocol/pubsub#collections")) {
            throw new TestNotPossibleException("PubSub service does not support collection nodes.");
        }
    }

    /**
     * Asserts that the pub/sub service responds to a disco#item request.
     */
    @SmackIntegrationTest(section = "5.2", quote = "If a service implements a hierarchy of nodes, it MUST also enable entities to discover the nodes in that hierarchy by means of the Service Discovery protocol [...]")
    public void testDiscoItemNonErrorResponse() throws SmackException.NotConnectedException, InterruptedException
    {
        // Setup test fixture.
        final ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(conOne);

        try {
            // Execute system under test.
            serviceDiscoveryManager.discoverItems(pubsubServiceAddress);

            // Verify results.
        } catch (SmackException.NoResponseException e) {
            fail("Expected a response to the service discovery items request that was made by '" + conOne.getUser() + "' to '" + pubsubServiceAddress + "' (which advertises support for collection nodes) but no response was received.");
        } catch (XMPPException.XMPPErrorException e) {
            fail("Expected a non-error response to the service discovery items request that was made by '" + conOne.getUser() + "' to '" + pubsubServiceAddress + "' (which advertises support for collection nodes), but an error was received. " + e.getStanzaError());
        }
    }

    /**
     * Asserts that the pub/sub service shows a collection node in its disco#item response.
     */
    @SmackIntegrationTest(section = "5.2", quote = "If a service implements a hierarchy of nodes, it MUST also enable entities to discover the nodes in that hierarchy by means of the Service Discovery protocol [...]")
    public void testDiscoItemContainsCollectionNode() throws SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException, TestNotPossibleException
    {
        // Setup test fixture.
        final String nodeId = "testcollection-" + StringUtils.randomString(5);
        final PubSubManager pubSubManagerOne = PubSubManager.getInstanceFor(conOne);
        try {
            final FillableConfigureForm config = pubSubManagerOne.getDefaultConfiguration().getFillableForm();
            config.setNodeType(NodeType.collection);
            pubSubManagerOne.createNode(nodeId, config);
        } catch (Exception e) {
            throw new TestNotPossibleException("Unable to create a Collection Node.", e);
        }
        final ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(conOne);

        try {
            // Execute system under test.
            final DiscoverItems discoveredItems = serviceDiscoveryManager.discoverItems(pubsubServiceAddress);

            // Verify results.
            assertContainsItemRepresentingNode(nodeId, discoveredItems.getItems(),
                "Expected the response to the service discovery items request that was made by '" + conOne.getUser() + "' to service '" + pubsubServiceAddress + "' to contain the node with id '" + nodeId + "' that was created prior to this request (but the node was not found in the disco#items response).");
        } catch (SmackException.NoResponseException e) {
            fail("Expected a response to the service discovery items request that was made by '" + conOne.getUser() + "' to '" + pubsubServiceAddress + "' (which advertises support for collection nodes) but no response was received.");
        } catch (XMPPException.XMPPErrorException e) {
            fail("Expected a non-error response to the service discovery items request that was made by '" + conOne.getUser() + "' to '" + pubsubServiceAddress + "' (which advertises support for collection nodes), but an error was received. " + e.getStanzaError());
        } finally {
            // Tear down test fixture.
            try {
                pubSubManagerOne.deleteNode(nodeId);
            } catch (XMPPException.XMPPErrorException e) {
                LOGGER.log(Level.WARNING, "Unable to delete collection node that was created in the test fixture. Node ID: " + nodeId, e);
            }
        }
    }

    /**
     * Asserts that the pub/sub service shows a collection node in its disco#item response.
     */
    @SmackIntegrationTest(section = "5.2", quote = "If a service implements a hierarchy of nodes, it MUST also enable entities to discover the nodes in that hierarchy by means of the Service Discovery protocol [...]")
    public void testDiscoItemContainsNestedCollectionNode() throws SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException, TestNotPossibleException
    {
        // Setup test fixture.
        final String nodeIdParent = "testcollection-1-" + StringUtils.randomString(5);
        final String nodeIdChild = "testcollection-2-" + StringUtils.randomString(5);
        final PubSubManager pubSubManagerOne = PubSubManager.getInstanceFor(conOne);
        try {
            final FillableConfigureForm configParent = pubSubManagerOne.getDefaultConfiguration().getFillableForm();
            configParent.setNodeType(NodeType.collection);
            pubSubManagerOne.createNode(nodeIdParent, configParent);
        } catch (Exception e) {
            throw new TestNotPossibleException("Unable to create a node hierarchy.", e);
        }

        try {
            final FillableConfigureForm configChild = pubSubManagerOne.getDefaultConfiguration().getFillableForm();
            configChild.setNodeType(NodeType.collection);
            configChild.setCollection(nodeIdParent);
            pubSubManagerOne.createNode(nodeIdChild, configChild);
        } catch (Exception e) {
            try {
                pubSubManagerOne.deleteNode(nodeIdParent);
            } catch (XMPPException.XMPPErrorException e1) {
                LOGGER.log(Level.WARNING, "Unable to delete collection node that was created in the test fixture. Node ID: " + nodeIdParent, e1);
            }
            throw new TestNotPossibleException("Unable to create a node hierarchy.", e);
        }
        final ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(conOne);

        try {
            // Execute system under test.
            final DiscoverItems discoveredItems1 = serviceDiscoveryManager.discoverItems(pubsubServiceAddress);
            final DiscoverItems discoveredItems2 = serviceDiscoveryManager.discoverItems(pubsubServiceAddress, nodeIdParent); // Note: although it's preferred to test only one invocation in a test, two invocations are needed to be able to verify the hierarchy.

            // Verify results.
            assertContainsItemRepresentingNode(nodeIdParent, discoveredItems1.getItems(),
                "Expected the response to the service discovery items request that was made by '" + conOne.getUser() + "' to service '" + pubsubServiceAddress + "' to contain the node with id '" + nodeIdParent + "' that was created prior to this request (but the node was not found in the disco#items response).");
            assertContainsItemRepresentingNode(nodeIdChild, discoveredItems2.getItems(),
                "Expected the response to the service discovery items request that was made by '" + conOne.getUser() + "' to the collection node with id '" + nodeIdParent + "' in service '" + pubsubServiceAddress + "' to contain the node with id '" + nodeIdChild + "' that was created prior to this request (but the node was not found in the disco#items response).");
        } catch (SmackException.NoResponseException e) {
            fail("Expected a response to the service discovery items request that was made by '" + conOne.getUser() + "' to '" + pubsubServiceAddress + "' (which advertises support for collection nodes) but no response was received.");
        } catch (XMPPException.XMPPErrorException e) {
            fail("Expected a non-error response to the service discovery items request that was made by '" + conOne.getUser() + "' to '" + pubsubServiceAddress + "' (which advertises support for collection nodes), but an error was received. " + e.getStanzaError());
        } finally {
            // Tear down test fixture.
            try {
                pubSubManagerOne.deleteNode(nodeIdChild);
            } catch (XMPPException.XMPPErrorException e) {
                LOGGER.log(Level.WARNING, "Unable to delete collection node that was created in the test fixture. Node ID: " + nodeIdChild, e);
            }
            try {
                pubSubManagerOne.deleteNode(nodeIdParent);
            } catch (XMPPException.XMPPErrorException e) {
                LOGGER.log(Level.WARNING, "Unable to delete collection node that was created in the test fixture. Node ID: " + nodeIdParent, e);
            }
        }
    }
}
