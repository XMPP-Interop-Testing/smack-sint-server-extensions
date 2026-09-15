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
import org.jivesoftware.smackx.geoloc.packet.GeoLocation;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.NodeType;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.form.FillableConfigureForm;
import org.jxmpp.jid.DomainBareJid;

import java.util.List;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The tests in this class verify requirements that are not explicitly states in XEP-0248, but in XEP-0060.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 * @see <a href="https://xmpp.org/extensions/xep-0060.html#entity-nodes">XEP-0060: Publish-Subscribe</a>
 * @see <a href="https://xmpp.org/extensions/xep-0248.html#disco-nodes">XEP-0248: PubSub Collection Nodes</a>
 */
@SpecificationReference(document = "XEP-0248", version = "0.5.0")
public class PubSubCollection_XEP0060_IntegrationTest extends AbstractSmackIntegrationTest
{
    protected final DomainBareJid pubsubServiceAddress;

    public PubSubCollection_XEP0060_IntegrationTest(SmackIntegrationTestEnvironment environment) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
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
     * Asserts that the pub/sub service responds to a disco#info request made against an existing collection node.
     *
     * This is a requirement that's not explicitly states in XEP-0248, but it is something mandated in
     * XEP-0060 Section 5.3 that also applies.
     */
    @SmackIntegrationTest(section = "5.2", // Lacking a better alternative, this more or less corresponds to section 5.2 of XEP-0248
        quote = "(XEP-0060 section 5.3): A pubsub service MUST allow entities to query individual nodes for the information associated with that node. The Service Discovery protocol MUST be used to discover this information. The \"disco#info\" result MUST include an identity with a category of \"pubsub\" and an appropriate type as registered by the XMPP registrar (e.g. \"leaf\").")
    public void testDiscoInfoCollectionNode() throws TestNotPossibleException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException
    {
        // Setup test fixture.
        final String nodeId = "testcollection-" + StringUtils.randomString(5);
        final PubSubManager pubSubManagerOne = PubSubManager.getInstanceFor(conOne);
        try {
            final FillableConfigureForm config = pubSubManagerOne.getDefaultConfiguration().getFillableForm();
            config.setNodeType(NodeType.collection);
            pubSubManagerOne.createNode(nodeId, config);
        } catch (Exception e) {
            throw new TestNotPossibleException("PubSub service does not allow test user to create collection nodes.", e);
        }
        final ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(conOne);

        try {
            // Execute system under test.
            final DiscoverInfo discoveredInfo = serviceDiscoveryManager.discoverInfo(pubsubServiceAddress, nodeId);

            // Verify results.
            assertTrue(discoveredInfo.hasIdentity("pubsub", "collection"),
                "Expected the response to the service discovery info request that was made by '" + conOne.getUser() + "' to collection node '" + nodeId + "' of service '" + pubsubServiceAddress + "' to contain an identity of category 'pubsub' and type 'collection' (but no such identity was returned).");
        } catch (SmackException.NoResponseException e) {
            fail("Expected a response to the service discovery info request that was made by '" + conOne.getUser() + "' to collection node '" + nodeId + "' of service '" + pubsubServiceAddress + "' (which advertises support for collection nodes) but no response was received.");
        } catch (XMPPException.XMPPErrorException e) {
            fail("Expected a non-error response to the service discovery info request that was made by '" + conOne.getUser() + "' to collection node '" + nodeId + "' of service '" + pubsubServiceAddress + "' (which advertises support for collection nodes), but an error was received. " + e.getStanzaError());
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
     * Asserts that the pub/sub service responds to a disco#info request made against an existing leaf node that exists
     * in a hierarchy.
     *
     * This is a requirement that's not explicitly states in XEP-0248, but it is something mandated in
     * XEP-0060 Section 5.3 that also applies.
     */
    @SmackIntegrationTest(section = "5.2", // Lacking a better alternative, this more or less corresponds to section 5.2 of XEP-0248
        quote = "(XEP-0060 section 5.3): A pubsub service MUST allow entities to query individual nodes for the information associated with that node. The Service Discovery protocol MUST be used to discover this information. The \"disco#info\" result MUST include an identity with a category of \"pubsub\" and an appropriate type as registered by the XMPP registrar (e.g. \"leaf\").")
    public void testDiscoInfoNestedLeafNode() throws TestNotPossibleException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException
    {
        // Setup test fixture.
        final String nodeIdParent = "testparent-" + StringUtils.randomString(5);
        final String nodeIdChild = "testchild-" + StringUtils.randomString(5);
        final PubSubManager pubSubManagerOne = PubSubManager.getInstanceFor(conOne);
        try {
            final FillableConfigureForm configParent = pubSubManagerOne.getDefaultConfiguration().getFillableForm();
            configParent.setNodeType(NodeType.collection);
            pubSubManagerOne.createNode(nodeIdParent, configParent);
        } catch (Exception e) {
            throw new TestNotPossibleException("PubSub service does not allow test user to create a node hierarchy.", e);
        }

        try {
            final FillableConfigureForm configChild = pubSubManagerOne.getDefaultConfiguration().getFillableForm();
            configChild.setNodeType(NodeType.leaf);
            configChild.setCollection(nodeIdParent);
            pubSubManagerOne.createNode(nodeIdChild, configChild);
        } catch (Exception e) {
            try {
                pubSubManagerOne.deleteNode(nodeIdParent);
            } catch (XMPPException.XMPPErrorException e1) {
                LOGGER.log(Level.WARNING, "Unable to delete collection node that was created in the test fixture. Node ID: " + nodeIdParent, e1);
            }
            throw new TestNotPossibleException("PubSub service does not allow test user to create a node hierarchy.", e);
        }
        final ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(conOne);

        try {
            // Execute system under test.
            final DiscoverInfo discoveredInfo = serviceDiscoveryManager.discoverInfo(pubsubServiceAddress, nodeIdChild);

            // Verify results.
            assertTrue(discoveredInfo.hasIdentity("pubsub", "leaf"),
                "Expected the response to the service discovery info request that was made by '" + conOne.getUser() + "' to leaf node '" + nodeIdChild + "' (that exists in a hierarchy, as a child of collection node '" + nodeIdParent + "') of service '" + pubsubServiceAddress + "' to contain an identity of category 'pubsub' and type 'leaf' (but no such identity was returned).");
        } catch (SmackException.NoResponseException e) {
            fail("Expected a response to the service discovery info request that was made by '" + conOne.getUser() + "' to leaf node '" + nodeIdChild + "' (that exists in a hierarchy, as a child of collection node '" + nodeIdParent + "') of service '" + pubsubServiceAddress + "' (which advertises support for leaf nodes) but no response was received.");
        } catch (XMPPException.XMPPErrorException e) {
            fail("Expected a non-error response to the service discovery info request that was made by '" + conOne.getUser() + "' to leaf node '" + nodeIdChild + "' (that exists in a hierarchy, as a child of collection node '" + nodeIdParent + "') of service '" + pubsubServiceAddress + "' (which advertises support for leaf nodes), but an error was received. " + e.getStanzaError());
        } finally {
            // Tear down test fixture.
            try {
                pubSubManagerOne.deleteNode(nodeIdChild);
            } catch (XMPPException.XMPPErrorException e) {
                LOGGER.log(Level.WARNING, "Unable to delete leaf node that was created in the test fixture. Node ID: " + nodeIdChild, e);
            }
            try {
                pubSubManagerOne.deleteNode(nodeIdParent);
            } catch (XMPPException.XMPPErrorException e) {
                LOGGER.log(Level.WARNING, "Unable to delete collection node that was created in the test fixture. Node ID: " + nodeIdParent, e);
            }
        }
    }

    /**
     * Asserts that the pub/sub service responds to a disco#info request made against an existing collection node that
     * exists in a hierarchy.
     *
     * This is a requirement that's not explicitly states in XEP-0248, but it is something mandated in
     * XEP-0060 Section 5.3 that also applies.
     */
    @SmackIntegrationTest(section = "5.2", // Lacking a better alternative, this more or less corresponds to section 5.2 of XEP-0248
        quote = "(XEP-0060 section 5.3): A pubsub service MUST allow entities to query individual nodes for the information associated with that node. The Service Discovery protocol MUST be used to discover this information. The \"disco#info\" result MUST include an identity with a category of \"pubsub\" and an appropriate type as registered by the XMPP registrar (e.g. \"leaf\").")
    public void testDiscoInfoNestedCollectionNode() throws TestNotPossibleException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException
    {
        // Setup test fixture.
        final String nodeIdParent = "testparent-" + StringUtils.randomString(5);
        final String nodeIdChild = "testchild-" + StringUtils.randomString(5);
        final PubSubManager pubSubManagerOne = PubSubManager.getInstanceFor(conOne);
        try {
            final FillableConfigureForm configParent = pubSubManagerOne.getDefaultConfiguration().getFillableForm();
            configParent.setNodeType(NodeType.collection);
            pubSubManagerOne.createNode(nodeIdParent, configParent);
        } catch (Exception e) {
            throw new TestNotPossibleException("PubSub service does not allow test user to create a node hierarchy.", e);
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
            throw new TestNotPossibleException("PubSub service does not allow test user to create a node hierarchy.", e);
        }
        final ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(conOne);

        try {
            // Execute system under test.
            final DiscoverInfo discoveredInfo = serviceDiscoveryManager.discoverInfo(pubsubServiceAddress, nodeIdChild);

            // Verify results.
            assertTrue(discoveredInfo.hasIdentity("pubsub", "collection"),
                "Expected the response to the service discovery info request that was made by '" + conOne.getUser() + "' to collection node '" + nodeIdChild + "' (that exists in a hierarchy, as a child of collection node '" + nodeIdParent + "') of service '" + pubsubServiceAddress + "' to contain an identity of category 'pubsub' and type 'leaf' (but no such identity was returned).");
        } catch (SmackException.NoResponseException e) {
            fail("Expected a response to the service discovery info request that was made by '" + conOne.getUser() + "' to collection node '" + nodeIdChild + "' (that exists in a hierarchy, as a child of collection node '" + nodeIdParent + "') of service '" + pubsubServiceAddress + "' (which advertises support for leaf nodes) but no response was received.");
        } catch (XMPPException.XMPPErrorException e) {
            fail("Expected a non-error response to the service discovery info request that was made by '" + conOne.getUser() + "' to collection node '" + nodeIdChild + "' (that exists in a hierarchy, as a child of collection node '" + nodeIdParent + "') of service '" + pubsubServiceAddress + "' (which advertises support for leaf nodes), but an error was received. " + e.getStanzaError());
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

    /**
     * Asserts that the pub/sub service responds to a disco#item request made against a leaf node (that exists in a
     * hierarchy) with items published to it.
     */
    @SmackIntegrationTest(section = "5.2", // Lacking a better alternative, this more or less corresponds to section 5.2 of XEP-0248
        quote = "(XEP-0060 section 5.5): To discover the published items which exist on the service for a specific node, an entity MAY send a \"disco#items\" request to the node itself, and the service MAY return each item as a Service Discovery <item/> element. The 'name' attribute of each Service Discovery item MUST contain its ItemID and the item MUST NOT possess a 'node' attribute.")
    public void testDiscoItemsNestedLeafNode() throws TestNotPossibleException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException
    {
        // Setup test fixture.
        final String nodeIdParent = "testparent-" + StringUtils.randomString(5);
        final String nodeIdChild = "testchild-" + StringUtils.randomString(5);
        final String itemId = "testitem-" + StringUtils.randomString(5);
        final PubSubManager pubSubManagerOne = PubSubManager.getInstanceFor(conOne);
        final LeafNode node;
        try {
            final FillableConfigureForm configParent = pubSubManagerOne.getDefaultConfiguration().getFillableForm();
            configParent.setNodeType(NodeType.collection);
            pubSubManagerOne.createNode(nodeIdParent, configParent);
        } catch (Exception e) {
            throw new TestNotPossibleException("PubSub service does not allow test user to create a node hierarchy.", e);
        }

        try {
            final FillableConfigureForm configChild = pubSubManagerOne.getDefaultConfiguration().getFillableForm();
            configChild.setNodeType(NodeType.leaf);
            configChild.setCollection(nodeIdParent);
            node = (LeafNode) pubSubManagerOne.createNode(nodeIdChild, configChild);
        } catch (Exception e) {
            try {
                pubSubManagerOne.deleteNode(nodeIdParent);
            } catch (XMPPException.XMPPErrorException e1) {
                LOGGER.log(Level.WARNING, "Unable to delete collection node that was created in the test fixture. Node ID: " + nodeIdParent, e1);
            }
            throw new TestNotPossibleException("PubSub service does not allow test user to create a node hierarchy.", e);
        }

        try {
            node.publish(new PayloadItem<>(itemId, GeoLocation.builder().setDescription(StringUtils.randomString(5)).build()));
        } catch (XMPPException.XMPPErrorException e) {
            try {
                pubSubManagerOne.deleteNode(nodeIdChild);
            } catch (XMPPException.XMPPErrorException e1) {
                LOGGER.log(Level.WARNING, "Unable to delete node that was created in the test fixture. Node ID: " + nodeIdChild, e1);
            }
            throw new TestNotPossibleException("PubSub service does not allow test user to publish an item to a leaf node.", e);
        }
        final ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(conOne);

        try {
            // Execute system under test.
            final List<DiscoverItems.Item> items = serviceDiscoveryManager.discoverItems(pubsubServiceAddress, nodeIdChild).getItems();
            if (items.isEmpty()) {
                throw new TestNotPossibleException("PubSub service does not support Discover Items for a Node");
            }

            // Verify results.
            assertTrue(items.stream().anyMatch(item -> itemId.equals(item.getName())),
                "Expected the response to the service discovery items request that was made by '" + conOne.getUser() + "' to leaf node '" + nodeIdChild + "' of service '" + pubsubServiceAddress + "' to contain an item with name '" + itemId+ "' which matches the itemID of an item that is known to have been published to the node (but no such item was returned).");
            assertTrue(items.stream().noneMatch(item -> item.getNode() != null),
                "Expected the response to the service discovery items request that was made by '" + conOne.getUser() + "' to leaf node '" + nodeIdChild + "' of service '" + pubsubServiceAddress + "' to contain no items with a 'node' attribute (but at least one item with a node attribute was returned).");
        } catch (XMPPException.XMPPErrorException e) {
            throw new TestNotPossibleException("PubSub service does not support Discover Items for a Node", e);
        } finally {
            // Tear down test fixture.
            try {
                pubSubManagerOne.deleteNode(nodeIdChild);
            } catch (XMPPException.XMPPErrorException e) {
                LOGGER.log(Level.WARNING, "Unable to delete leaf node that was created in the test fixture. Node ID: " + nodeIdChild, e);
            }
            try {
                pubSubManagerOne.deleteNode(nodeIdParent);
            } catch (XMPPException.XMPPErrorException e) {
                LOGGER.log(Level.WARNING, "Unable to delete collection node that was created in the test fixture. Node ID: " + nodeIdParent, e);
            }
        }
    }
}
