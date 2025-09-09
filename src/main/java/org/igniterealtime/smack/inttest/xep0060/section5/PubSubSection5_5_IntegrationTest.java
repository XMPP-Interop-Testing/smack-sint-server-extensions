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
import org.jivesoftware.smackx.geoloc.packet.GeoLocation;
import org.jivesoftware.smackx.pubsub.*;
import org.jivesoftware.smackx.pubsub.form.FillableConfigureForm;
import org.jxmpp.jid.DomainBareJid;

import java.util.List;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests as defined in paragraph 5.5 "Discover Items for a Node" of section 5 "Entity Use Cases" of XEP-0060 "Publish-Subscribe".
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 * @see <a href="https://xmpp.org/extensions/xep-0060.html#entity-discoveritems">XEP-0060: Publish-Subscribe</a>
 */
// TODO: Some of this (around 'Collection Nodes' and hierarchy) should be part for XEP-0248, not XEP-0060, although as of version 1.26.0 of XEP-0060 it is in that specification. Monitor later versions of the specification for changes (as suggested in https://mail.jabber.org/hyperkitty/list/standards@xmpp.org/thread/COEJQNNCEHHT2WFF46CWYYYVCL2NIOE4/ )
@SpecificationReference(document = "XEP-0060", version = "1.26.0")
public class PubSubSection5_5_IntegrationTest extends AbstractSmackIntegrationTest
{
    protected final DomainBareJid pubsubServiceAddress;

    public PubSubSection5_5_IntegrationTest(SmackIntegrationTestEnvironment environment) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        super(environment);
        pubsubServiceAddress = PubSubManager.getPubSubService(conOne);
        if (pubsubServiceAddress == null) {
            throw new TestNotPossibleException("No PubSub service found");
        }
    }

    /**
     * Asserts that the pub/sub service responds to a disco#item request made against a leaf node with items published
     * to it.
     */
    @SmackIntegrationTest(section = "5.5", quote = "To discover the published items which exist on the service for a specific node, an entity MAY send a \"disco#items\" request to the node itself, and the service MAY return each item as a Service Discovery <item/> element. The 'name' attribute of each Service Discovery item MUST contain its ItemID and the item MUST NOT possess a 'node' attribute.")
    public void testDiscoItemsLeafNode() throws TestNotPossibleException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException
    {
        // Setup test fixture.
        final String nodeId = "testleaf-" + StringUtils.randomString(5);
        final String itemId = "testitem-" + StringUtils.randomString(5);
        final PubSubManager pubSubManagerOne = PubSubManager.getInstanceFor(conOne);
        final LeafNode node;
        try {
            final FillableConfigureForm config = pubSubManagerOne.getDefaultConfiguration().getFillableForm();
            config.setNodeType(NodeType.leaf);
            node = (LeafNode) pubSubManagerOne.createNode(nodeId, config);
        } catch (Exception e) {
            throw new TestNotPossibleException("Unable to create a Leaf Node.", e);
        }
        try {
            node.publish(new PayloadItem<>(itemId, GeoLocation.builder().setDescription(StringUtils.randomString(5)).build()));
        } catch (XMPPException.XMPPErrorException e) {
            try {
                pubSubManagerOne.deleteNode(nodeId);
            } catch (XMPPException.XMPPErrorException e1) {
                LOGGER.log(Level.WARNING, "Unable to delete node that was created in the test fixture. Node ID: " + nodeId, e1);
            }
            throw new TestNotPossibleException("Unable to publish an Item to a Leaf Node.", e);
        }
        final ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(conOne);

        try {
            // Execute system under test.
            final List<DiscoverItems.Item> items = serviceDiscoveryManager.discoverItems(pubsubServiceAddress, nodeId).getItems();
            if (items.isEmpty()) {
                throw new TestNotPossibleException("PubSub service does not support Discover Items for a Node");
            }

            // Verify results.
            assertTrue(items.stream().anyMatch(item -> itemId.equals(item.getName())),
                "Expected the response to the service discovery items request that was made by '" + conOne.getUser() + "' to leaf node '" + nodeId + "' of service '" + pubsubServiceAddress + "' to contain an item with name '" + itemId+ "' which matches the itemID of an item that is known to have been published to the node (but no such item was returned).");
            assertTrue(items.stream().noneMatch(item -> item.getNode() != null),
                "Expected the response to the service discovery items request that was made by '" + conOne.getUser() + "' to leaf node '" + nodeId + "' of service '" + pubsubServiceAddress + "' to contain no items with a 'node' attribute (but at least one item with a node attribute was returned).");
        } catch (XMPPException.XMPPErrorException e) {
            throw new TestNotPossibleException("PubSub service does not support Discover Items for a Node", e);
        } finally {
            // Tear down test fixture.
            try {
                pubSubManagerOne.deleteNode(nodeId);
            } catch (XMPPException.XMPPErrorException e) {
                LOGGER.log(Level.WARNING, "Unable to delete leaf node that was created in the test fixture. Node ID: " + nodeId, e);
            }
        }
    }

    /**
     * Asserts that the pub/sub service responds to a disco#item request made against a leaf node (that exists in a
     * hierarchy) with items published to it.
     */
    @SmackIntegrationTest(section = "5.5", quote = "To discover the published items which exist on the service for a specific node, an entity MAY send a \"disco#items\" request to the node itself, and the service MAY return each item as a Service Discovery <item/> element. The 'name' attribute of each Service Discovery item MUST contain its ItemID and the item MUST NOT possess a 'node' attribute.")
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
            throw new TestNotPossibleException("Unable to create a node hierarchy.", e);
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
            throw new TestNotPossibleException("Unable to create a node hierarchy.", e);
        }

        try {
            node.publish(new PayloadItem<>(itemId, GeoLocation.builder().setDescription(StringUtils.randomString(5)).build()));
        } catch (XMPPException.XMPPErrorException e) {
            try {
                pubSubManagerOne.deleteNode(nodeIdChild);
            } catch (XMPPException.XMPPErrorException e1) {
                LOGGER.log(Level.WARNING, "Unable to delete node that was created in the test fixture. Node ID: " + nodeIdChild, e1);
            }
            throw new TestNotPossibleException("Unable to publish an Item to a Leaf Node.", e);
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
