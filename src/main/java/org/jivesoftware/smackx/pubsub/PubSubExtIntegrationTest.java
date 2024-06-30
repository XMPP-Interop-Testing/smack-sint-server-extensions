/*
 * Copyright 2015-2024 Florian Schmaus, Guus der Kinderen, Paul Schaub
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
package org.jivesoftware.smackx.pubsub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.geoloc.packet.GeoLocation;
import org.jivesoftware.smackx.pubsub.form.ConfigureForm;
import org.jivesoftware.smackx.pubsub.form.FillableConfigureForm;
import org.jivesoftware.smackx.pubsub.form.FillableSubscribeForm;
import org.jivesoftware.smackx.pubsub.packet.PubSub;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

@SpecificationReference(document = "XEP-0060")
public class PubSubExtIntegrationTest extends AbstractSmackIntegrationTest {

    private final PubSubManager pubSubManagerOne;
    private final PubSubManager pubSubManagerTwo;

    public PubSubExtIntegrationTest(SmackIntegrationTestEnvironment environment)
            throws TestNotPossibleException, SmackException.NoResponseException, XMPPErrorException,
            NotConnectedException, InterruptedException {
        super(environment);
        DomainBareJid pubSubService = PubSubManager.getPubSubService(conOne);
        if (pubSubService == null) {
            throw new TestNotPossibleException("No PubSub service found");
        }
        pubSubManagerOne = PubSubManager.getInstanceFor(conOne, pubSubService);
        if (!pubSubManagerOne.canCreateNodesAndPublishItems()) {
            throw new TestNotPossibleException("PubSub service does not allow node creation");
        }
        pubSubManagerTwo = PubSubManager.getInstanceFor(conTwo, pubSubService);
    }

    /**
     * Asserts that an item can be published to a node with default configuration.
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    @SmackIntegrationTest
    public void publishItemTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        final String nodeName = "sinttest-publish-item-nodename-" + testRunId;
        final String needle = "test content " + Math.random();
        LeafNode node = pubSubManagerOne.createNode(nodeName);
        try {
            // Publish a new item.
            node.publish(new PayloadItem<>(GeoLocation.builder().setDescription(needle).build()));

            // Retrieve items and assert that the item that was just published is among them.
            final List<Item> items = node.getItems();
            assertTrue(items.stream().anyMatch(stanza -> stanza.toXML().toString().contains(needle)), "After publishing that item to node '" + nodeName + "', it was expected to find the item with body '" + needle + "' in the items obtained from the node (but it was not).");
        } finally {
            pubSubManagerOne.deleteNode(nodeName);
        }
    }

    /**
     * Asserts that one can subscribe to an existing node.
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    @SmackIntegrationTest
    public void subscribeTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotAPubSubNodeException
    {
        final String nodeName = "sinttest-subscribe-nodename-" + testRunId;
        pubSubManagerOne.createNode(nodeName);
        try {
            // Subscribe to the node, using a different user than the owner of the node.
            final Node subscriberNode = pubSubManagerTwo.getNode(nodeName);
            final EntityBareJid subscriber = conTwo.getUser().asEntityBareJid();
            final Subscription subscription = subscriberNode.subscribe(subscriber);
            assertNotNull(subscription, "After subscribing to '" + nodeName + "', it was expected to find a subscription for '" + subscriber + "' in the node (but it was not).");

            // Assert that subscription is correctly reported when the subscriber requests its subscriptions.
            final List<Subscription> subscriptions = pubSubManagerTwo.getNode(nodeName).getSubscriptions();
            assertNotNull(subscription, "After subscribing to '" + nodeName + "', it was expected to find subscriptions in the node (but not one was found).");
            assertTrue(subscriptions.stream().anyMatch(s -> subscriber.equals(s.getJid())), "After subscribing to '" + nodeName + "', it was expected to find a subscription for '" + subscriber + "' in the node (but it was not).");
        } finally {
            pubSubManagerOne.deleteNode(nodeName);
        }
    }

    /**
     * Asserts that the server returns a 'bad request' error to a subscription
     * request in which the JIDs do not match.
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws XmppStringprepException if the hard-coded test JID cannot be instantiated.
     * @throws PubSubException.NotAPubSubNodeException if the node cannot be accessed.
     */
    @SmackIntegrationTest(section = "6.1.3.1", quote =
        "If the specified JID is a bare JID or full JID, the service MUST at a minimum check the bare JID portion " +
        "against the bare JID portion of the 'from' attribute on the received IQ request to make sure that the " +
        "requesting entity has the same identity as the JID which is being requested to be added to the subscriber " +
        "list. If the bare JID portions of the JIDs do not match as described above and the requesting entity does " +
        "not have some kind of admin or proxy privilege as defined by the implementation, the service MUST return a " +
        "<bad-request/> error (...)")
    public void subscribeJIDsDoNotMatchTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, XmppStringprepException, PubSubException.NotAPubSubNodeException {
        final String nodeName = "sinttest-subscribe-nodename-" + testRunId;
        pubSubManagerOne.createNode(nodeName);
        final EntityBareJid subscriber = JidCreate.entityBareFrom("this-jid-does-not-match@example.org");
        try {
            // Subscribe to the node, using a different user than the owner of the node.
            final Node subscriberNode = pubSubManagerTwo.getNode(nodeName);
            subscriberNode.subscribe(subscriber);
            fail("The server should have returned a <bad-request/> error when '" + conTwo.getUser() + "' attempted to subscribe to node '" + nodeName + "' using subscriber-JID '" + subscriber + "', but did not.");
        } catch (XMPPErrorException e) {
            assertEquals(StanzaError.Condition.bad_request, e.getStanzaError().getCondition(),
                "Unexpected condition in the (expected) error returned by the server when '" + conTwo.getUser() + "' attempted to subscribe to node '" + nodeName + "' using subscriber-JID '" + subscriber + "'.");
        } finally {
            pubSubManagerOne.deleteNode(nodeName);
        }
    }

    /**
     * Asserts that the server returns a 'not-authorized' error to a subscription
     * request where required presence subscription is missing.
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws PubSubException.NotAPubSubNodeException if the node cannot be accessed.
     * @throws TestNotPossibleException if the server does not support the functionality required for this test.
     */
    @SmackIntegrationTest(section = "6.1.3.2", quote =
        "For nodes with an access model of \"presence\", if the requesting entity is not subscribed to the owner's " +
        "presence then the pubsub service MUST respond with a <not-authorized/> error (...)")
    public void subscribePresenceSubscriptionRequiredTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotAPubSubNodeException, TestNotPossibleException, SmackException.NotLoggedInException {
        final String nodeName = "sinttest-subscribe-nodename-" + testRunId;
        final ConfigureForm defaultConfiguration = pubSubManagerOne.getDefaultConfiguration();
        final FillableConfigureForm config = defaultConfiguration.getFillableForm();
        config.setAccessModel(AccessModel.presence);
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);
        try {
            pubSubManagerOne.createNode(nodeName, config);
        } catch (XMPPErrorException e) {
            throw new TestNotPossibleException("Access model 'presence' not supported on the server.");
        }
        final EntityBareJid subscriber = conTwo.getUser().asEntityBareJid();
        try {
            // Subscribe to the node, using a different user than the owner of the node.
            final Node subscriberNode = pubSubManagerTwo.getNode(nodeName);
            subscriberNode.subscribe(subscriber);
            fail("The server should have returned a <not-authorized/> error when '" + subscriber + "' attempted to subscribe to node '" + nodeName + "', but did not.");
        } catch (XMPPErrorException e) {
            assertEquals(StanzaError.Condition.not_authorized, e.getStanzaError().getCondition(),
                "Unexpected condition in the (expected) error returned by the server when '" + subscriber + "' attempted to subscribe to node '" + nodeName + "'.");
        } finally {
            pubSubManagerOne.deleteNode(nodeName);
        }
    }

    /**
     * Asserts that the server returns a 'not-authorized' error to a subscription
     * request where required roster items are missing.
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws PubSubException.NotAPubSubNodeException if the node cannot be accessed.
     * @throws TestNotPossibleException if the server does not support the functionality required for this test.
     */
    @SmackIntegrationTest(section = "6.1.3.3", quote =
        "For nodes with an access model of \"roster\", if the requesting entity is not in one of the authorized " +
        "roster groups then the pubsub service MUST respond with a <not-authorized/> error (...)")
    public void subscribeNotInRosterGroupTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotAPubSubNodeException, TestNotPossibleException {
        final String nodeName = "sinttest-subscribe-nodename-" + testRunId;
        final ConfigureForm defaultConfiguration = pubSubManagerOne.getDefaultConfiguration();
        final FillableConfigureForm config = defaultConfiguration.getFillableForm();
        config.setAccessModel(AccessModel.roster);
        try {
            pubSubManagerOne.createNode(nodeName, config);
        } catch (XMPPErrorException e) {
            throw new TestNotPossibleException("Access model 'roster' not supported on the server.");
        }
        final EntityBareJid subscriber = conTwo.getUser().asEntityBareJid();
        try {
            // Subscribe to the node, using a different user than the owner of the node.
            final Node subscriberNode = pubSubManagerTwo.getNode(nodeName);
            subscriberNode.subscribe(subscriber);
            fail("The server should have returned a <not-authorized/> error when '" + subscriber + "' attempted to subscribe to node '" + nodeName + "', but did not.");
        } catch (XMPPErrorException e) {
            assertEquals(StanzaError.Condition.not_authorized, e.getStanzaError().getCondition(),
                "Unexpected condition in the (expected) error returned by the server when '" + subscriber + "' attempted to subscribe to node '" + nodeName + "'.");
        } finally {
            pubSubManagerOne.deleteNode(nodeName);
        }
    }

    /**
     * Asserts that the server returns a 'not-allowed' error to a subscription
     * request where required whitelisting is missing.
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws PubSubException.NotAPubSubNodeException if the node cannot be accessed.
     * @throws TestNotPossibleException if the server does not support the functionality required for this test.
     */
    @SmackIntegrationTest(section = "6.1.3.4", quote =
        "For nodes with a node access model of \"whitelist\", if the requesting entity is not on the whitelist then " +
        "the service MUST return a <not-allowed/> error, specifying a pubsub-specific error condition of <closed-node/>.")
    public void subscribeNotOnWhitelistTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotAPubSubNodeException, TestNotPossibleException {
        final String nodeName = "sinttest-subscribe-nodename-" + testRunId;
        final ConfigureForm defaultConfiguration = pubSubManagerOne.getDefaultConfiguration();
        final FillableConfigureForm config = defaultConfiguration.getFillableForm();
        config.setAccessModel(AccessModel.whitelist);
        try {
            pubSubManagerOne.createNode(nodeName, config);
        } catch (XMPPErrorException e) {
            throw new TestNotPossibleException("Access model 'whitelist' not supported on the server.");
        }
        final EntityBareJid subscriber = conTwo.getUser().asEntityBareJid();
        try {
            // Subscribe to the node, using a different user than the owner of the node.
            final Node subscriberNode = pubSubManagerTwo.getNode(nodeName);
            subscriberNode.subscribe(subscriber);
            fail("The server should have returned a <not-allowed/> error when '" + subscriber + "' attempted to subscribe to node '" + nodeName + "', but did not.");
        } catch (XMPPErrorException e) {
            assertEquals(StanzaError.Condition.not_allowed, e.getStanzaError().getCondition(),
                "Unexpected condition in the (expected) error returned by the server when '" + subscriber + "' attempted to subscribe to node '" + nodeName + "'.");
            assertNotNull(e.getStanzaError().getExtension("closed-node", "http://jabber.org/protocol/pubsub#errors"),
                "The (expected) error returned by the server when '" + subscriber + "' attempted to subscribe to node '" + nodeName + "' should have included a qualified 'closed-node' extension (but did not).");
        } finally {
            pubSubManagerOne.deleteNode(nodeName);
        }
    }

    /**
     * Asserts that the server returns a 'not-authorized' error to a subscription
     * request when the subscriber already has a pending subscription.
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws PubSubException.NotAPubSubNodeException if the node cannot be accessed.
     * @throws TestNotPossibleException if the server does not support the functionality required for this test.
     */
    @SmackIntegrationTest(section = "6.1.3.7", quote =
        "If the requesting entity has a pending subscription, the service MUST return a <not-authorized/> error to " +
        "the subscriber, specifying a pubsub-specific error condition of <pending-subscription/>."
    )
    public void subscribePendingSubscriptionTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotAPubSubNodeException, TestNotPossibleException {
        final String nodeName = "sinttest-subscribe-nodename-" + testRunId;
        final ConfigureForm defaultConfiguration = pubSubManagerOne.getDefaultConfiguration();
        final FillableConfigureForm config = defaultConfiguration.getFillableForm();
        config.setAccessModel(AccessModel.authorize);
        try {
            pubSubManagerOne.createNode(nodeName, config);
        } catch (XMPPErrorException e) {
            throw new TestNotPossibleException("Access model 'authorize' not supported on the server.");
        }
        final EntityBareJid subscriber = conTwo.getUser().asEntityBareJid();
        try {
            // Subscribe to the node, using a different user than the owner of the node.
            final Node subscriberNode = pubSubManagerTwo.getNode(nodeName);
            subscriberNode.subscribe(subscriber);
            subscriberNode.subscribe(subscriber);
            fail("The server should have returned a <not-authorized/> error when '" + subscriber + "' attempted to subscribe to node '" + nodeName + "' for the second time, but did not.");
        } catch (XMPPErrorException e) {
            assertEquals(StanzaError.Condition.not_authorized, e.getStanzaError().getCondition(),
                "Unexpected condition in the (expected) error returned by the server when '" + subscriber + "' attempted to subscribe to node '" + nodeName + "' for the second time.");
            assertNotNull(e.getStanzaError().getExtension("pending-subscription", "http://jabber.org/protocol/pubsub#errors"),
                "The (expected) error returned by the server when '" + subscriber + "' attempted to subscribe to node '" + nodeName + "' for the second time should have included a qualified 'pending-subscription' extension (but did not).");

        } finally {
            pubSubManagerOne.deleteNode(nodeName);
        }
    }

    /**
     * Asserts that the server returns a pending notification to the subscriber
     * when subscribing to a node that requires authorization
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws PubSubException.NotAPubSubNodeException if the node cannot be accessed.
     * @throws TestNotPossibleException if the server does not support the functionality required for this test.
     */
    @SmackIntegrationTest(section = "6.1.4", quote =
        "Because the subscription request may or may not be approved, the service MUST return a pending notification to the subscriber.")
    public void subscribeApprovalRequiredGeneratesNotificationTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotAPubSubNodeException, TestNotPossibleException {
        final String nodeName = "sinttest-subscribe-nodename-" + testRunId;
        final ConfigureForm defaultConfiguration = pubSubManagerOne.getDefaultConfiguration();
        final FillableConfigureForm config = defaultConfiguration.getFillableForm();
        config.setAccessModel(AccessModel.authorize);
        try {
            pubSubManagerOne.createNode(nodeName, config);
        } catch (XMPPErrorException e) {
            throw new TestNotPossibleException("Access model 'authorize' not supported on the server.");
        }
        try {
            // Subscribe to the node, using a different user than the owner of the node.
            final Node subscriberNode = pubSubManagerTwo.getNode(nodeName);
            final EntityBareJid subscriber = conTwo.getUser().asEntityBareJid();
            final Subscription result = subscriberNode.subscribe(subscriber);

            assertEquals(Subscription.State.pending, result.getState(), "Unexpected subscription state after '" + subscriber + "' submitted a subscribe request for '" + nodeName + "'.");
        } finally {
            pubSubManagerOne.deleteNode(nodeName);
        }
    }

    /**
     * Asserts that the server returns non-null, unique subscription IDs when
     * subscribing twice to the same node (with different options).
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws PubSubException.NotAPubSubNodeException if the node cannot be accessed.
     * @throws TestNotPossibleException if the server does not support the functionality required for this test.
     */
    @SmackIntegrationTest(section = "6.1.6", quote =
        "If multiple subscriptions for the same JID are allowed, the service MUST use the 'subid' attribute to " +
        "differentiate between subscriptions for the same entity (therefore the SubID MUST be unique for each " +
        "node+JID combination and the SubID MUST be present on the <subscription/> element any time it is sent to " +
        "the subscriber).")
    public void subscribeMultipleSubscriptionsTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotAPubSubNodeException, TestNotPossibleException {
        if (!pubSubManagerOne.getSupportedFeatures().containsFeature(PubSubFeature.multi_subscribe)) {
            throw new TestNotPossibleException("Feature 'multi-subscribe' not supported on the server.");
        }

        final String nodeName = "sinttest-multisubscribe-nodename-" + testRunId;
        pubSubManagerOne.createNode(nodeName);

        try {
            // Subscribe to the node twice, using different configuration
            final Node subscriberNode = pubSubManagerTwo.getNode(nodeName);
            final EntityBareJid subscriber = conTwo.getUser().asEntityBareJid();

            final Subscription subscriptionA = subscriberNode.subscribe(subscriber);
            final Subscription subscriptionB = subscriberNode.subscribe(subscriber);

            assertNotNull(subscriptionA.getId(), "Expected the first subscription of '" + subscriber + "' to node '" + nodeName + "' to have an ID (but it does not).");
            assertNotNull(subscriptionA.getId(), "Expected the second subscription of '" + subscriber + "' to node '" + nodeName + "' to have an ID (but it does not).");
            assertNotEquals(subscriptionA.getId(), subscriptionB.getId(), "Expected both subscriptions of '" + subscriber + "' to node '" + nodeName + "' to have an distinct IDs (but they were equal: '" + subscriptionA.getId() + "').");
        } finally {
            pubSubManagerOne.deleteNode(nodeName);
        }
    }

    /**
     * Asserts that the server returns the pre-existing, non-null, unique
     * subscription IDs when again to the same node (with different options).
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws PubSubException.NotAPubSubNodeException if the node cannot be accessed.
     * @throws TestNotPossibleException if the server does not support the functionality required for this test.
     */
    @SmackIntegrationTest(section = "6.1.6", quote =
        "If the service does not allow multiple subscriptions for the same entity and it receives an additional " +
        "subscription request, the service MUST return the current subscription state (as if the subscription was " +
        "just approved).")
    public void subscribeMultipleSubscriptionNotSupportedTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotAPubSubNodeException, TestNotPossibleException {
        if (pubSubManagerOne.getSupportedFeatures().containsFeature(PubSubFeature.multi_subscribe)) {
            throw new TestNotPossibleException("Feature 'multi-subscribe' allowed on the server (this test verifies behavior for when it's not).");
        }

        final String nodeName = "sinttest-multisubscribe-nodename-" + testRunId;
        pubSubManagerOne.createNode(nodeName);
        final EntityBareJid subscriber = conTwo.getUser().asEntityBareJid();
        try {
            // Subscribe to the node twice, using different configuration
            final Node subscriberNode = pubSubManagerTwo.getNode(nodeName);
            final FillableSubscribeForm formA = subscriberNode.getSubscriptionOptions(subscriber.toString()).getFillableForm();
            formA.setDigestFrequency(1);
            final FillableSubscribeForm formB = subscriberNode.getSubscriptionOptions(subscriber.toString()).getFillableForm();
            formB.setDigestFrequency(2);

            final Subscription subscriptionA = subscriberNode.subscribe(subscriber, formA);
            final Subscription subscriptionB = subscriberNode.subscribe(subscriber, formB);

            // A poor-man's "equal"
            final String normalizedRepresentationA = subscriptionA.toXML(XmlEnvironment.EMPTY).toString();
            final String normalizedRepresentationB = subscriptionB.toXML(XmlEnvironment.EMPTY).toString();
            assertEquals(normalizedRepresentationA, normalizedRepresentationB, "Expected the second subscription request from '" + subscriber + "' to node '" + nodeName + "' to be answered with the same subscription state as the original state (but it was not).");
        } finally {
            pubSubManagerOne.deleteNode(nodeName);
        }
    }

    /**
     * Asserts that one can unsubscribe from a node (when a previous subscription
     * existed).
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws PubSubException.NotAPubSubNodeException If an error occurred while creating the node.
     */
    @SmackIntegrationTest(section = "6.2.2", quote =
        "If the request can be successfully processed, the service MUST return an IQ result (...)")
    public void unsubscribeTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotAPubSubNodeException {
        final String nodeName = "sinttest-unsubscribe-nodename-" + testRunId;
        pubSubManagerOne.createNode(nodeName);
        final EntityBareJid subscriber = conTwo.getUser().asEntityBareJid();

        try {
            // Subscribe to the node, using a different user than the owner of the node.
            final Node subscriberNode = pubSubManagerTwo.getNode(nodeName);
            final Subscription sub = subscriberNode.subscribe(subscriber);

            if (sub.state != Subscription.State.subscribed) {
                throw new IllegalStateException("Setup failed - '" + subscriber + "' failed to subscribe to node '" + nodeName + "'. State was " + sub.state);
            }

            try {
                subscriberNode.unsubscribe(subscriber.asEntityBareJidString(), sub.id);
            } catch (NoResponseException | XMPPErrorException e) {
                throw new AssertionError("Unsubscribe from a node failed for " + nodeName + "," + subscriber.asEntityBareJidString(), e);
            }
        } finally {
            pubSubManagerOne.deleteNode(nodeName);
        }
    }

    /**
     * Asserts that the server returns a 'bad request' response when not
     * specifying a subscription ID when unsubscribing from a node to which
     * more than one subscription exist.
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws PubSubException.NotAPubSubNodeException if the node cannot be accessed.
     * @throws TestNotPossibleException if the server does not support the functionality required for this test.
     */
    @SmackIntegrationTest(section = "6.2.3.1", quote =
        "If the requesting entity has multiple subscriptions to the node but does not specify a subscription ID, the " +
        "service MUST return a <bad-request/> error (...)")
    public void unsubscribeNoSubscriptionIDTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotAPubSubNodeException, TestNotPossibleException {
        if (!pubSubManagerOne.getSupportedFeatures().containsFeature(PubSubFeature.multi_subscribe)) {
            throw new TestNotPossibleException("Feature 'multi-subscribe' not supported on the server.");
        }

        final String nodeName = "sinttest-unsubscribeNoSub-nodename-" + testRunId;
        pubSubManagerOne.createNode(nodeName);

        try {
            // Subscribe to the node twice
            final Node subscriberNode = pubSubManagerTwo.getNode(nodeName);
            final EntityBareJid subscriber = conTwo.getUser().asEntityBareJid();

            final Subscription sub1 = subscriberNode.subscribe(subscriber); //once
            final Subscription sub2 = subscriberNode.subscribe(subscriber); //twice

            if (sub1.id == null) {
                throw new IllegalStateException("Setup failed - first subscription request from '" + subscriber + "' to '" + nodeName + "' does not yield a subscription ID.");
            }
            if (sub2.id == null) {
                throw new IllegalStateException("Setup failed - second subscription request from '" + subscriber + "' to '" + nodeName + "' does not yield a subscription ID.");
            }
            if (sub1.equals(sub2)) {
                throw new IllegalStateException("Setup failed - both subscription requests from '" + subscriber + "' to '" + nodeName + "' should have received distinct subscription IDs, but they are equal to each-other: " + sub1);
            }

            try {
                subscriberNode.unsubscribe(subscriber.asEntityBareJidString());
                fail("The server should have returned a <bad_request/> error in response to the request of '" + subscriber + "' to unsubscribe from node '" + nodeName + "', but did not.");
            } catch (XMPPErrorException e) {
                assertEquals(StanzaError.Condition.bad_request, e.getStanzaError().getCondition(),
                    "Unexpected condition in the (expected) error returned by the server when '" + subscriber + "' attempted to unsubscribe from node '" + nodeName + "'");
            }
        } finally {
            pubSubManagerOne.deleteNode(nodeName);
        }
    }

    /**
     * Asserts that the server returns an error response when unsubscribing from
     * a node without having a subscription.
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws PubSubException.NotAPubSubNodeException if the node cannot be accessed.
     */
    @SmackIntegrationTest(section = "6.2.3.2", quote =
        "If the value of the 'jid' attribute does not specify an existing subscriber, the pubsub service MUST return an error stanza")
    public void unsubscribeNoSuchSubscriberTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotAPubSubNodeException {
        final String nodeName = "sinttest-unsubscribeNSS-nodename-" + testRunId;
        pubSubManagerOne.createNode(nodeName);

        try {
            // Subscribe to the node twice, using different configuration
            final Node subscriberNode = pubSubManagerTwo.getNode(nodeName);
            final EntityBareJid subscriber = conTwo.getUser().asEntityBareJid();

            subscriberNode.unsubscribe(subscriber.asEntityBareJidString());
            fail("The server should have returned an error in response to the request of '" + subscriber + "' to unsubscribe from node '" + nodeName + "', but did not.");
        } catch (XMPPErrorException e) {
            // SHOULD be <unexpected-request/> (but that's not a 'MUST')
        } finally {
            pubSubManagerOne.deleteNode(nodeName);
        }
    }

    /**
     * Asserts that the server returns a 'forbidden' error response when
     * unsubscribing a JID from a node for which the issuer has no authority.
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws PubSubException.NotAPubSubNodeException if the node cannot be accessed.
     */
    @SmackIntegrationTest(section = "6.2.3.3", quote =
        "If the requesting entity is prohibited from unsubscribing the specified JID, the service MUST return a " +
        "<forbidden/> error.")
    public void unsubscribeInsufficientPrivilegesTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotAPubSubNodeException {
        final String nodeName = "sinttest-unsubscribeInsufficient-nodename-" + testRunId;
        final PubSubManager pubSubManagerThree = PubSubManager.getInstanceFor(conThree, PubSubManager.getPubSubService(conThree));
        pubSubManagerOne.createNode(nodeName);

        try {
            // Subscribe to the node, using a different user than the owner of the node.
            final Node subscriberNode = pubSubManagerTwo.getNode(nodeName);
            final EntityBareJid subscriber = conTwo.getUser().asEntityBareJid();
            final Subscription sub = subscriberNode.subscribe(subscriber);

            final Node unprivilegedNode = pubSubManagerThree.getNode(nodeName);
            try {
                unprivilegedNode.unsubscribe(subscriber.asEntityBareJidString(), sub.id);
                fail("The server should have returned a <forbidden/> error in response to the request of '" + subscriber + "' to unsubscribe from node '" + nodeName + "', but did not.");
            } catch (XMPPErrorException e) {
                assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(),
                    "Unexpected condition in the (expected) error returned by the server when '" + subscriber + "' attempted to unsubscribe from node '" + nodeName + "'.");
            }
        } finally {
            pubSubManagerOne.deleteNode(nodeName);
        }
    }

    /**
     * Asserts that the server returns an 'item-not-found' error response when
     * unsubscribing from a node that does not exist.
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    @SmackIntegrationTest(section = "6.2.3.3", quote =
        "If the node does not exist, the pubsub service MUST return an <item-not-found/> error.")
    public void unsubscribeNodeDoesNotExistTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        final String nodeName = "sinttest-unsubscribeDoesNotExist-nodename-" + testRunId;
        try {
            // Smack righteously doesn't facilitate unsubscribing from a non-existing node. Manually crafting stanza:
            final UnsubscribeExtension ext = new UnsubscribeExtension(conOne.getUser().asEntityBareJid().asEntityBareJidString(), "I-dont-exist", null);
            final PubSub unsubscribe = PubSub.createPubsubPacket(pubSubManagerOne.getServiceJid(), IQ.Type.set, ext);
            try {
                pubSubManagerOne.sendPubsubPacket(unsubscribe);
                fail("The server should have returned a <item-not-found/> error when '" + conOne.getUser().asEntityBareJid().asEntityBareJidString() + "' attempted to unsubscsribe from node '" + nodeName + "', but did not.");
            } catch (XMPPErrorException e) {
                assertEquals(StanzaError.Condition.item_not_found, e.getStanzaError().getCondition(),
                    "Unexpected condition in the (expected) error returned by the server when '" + conOne.getUser().asEntityBareJid().asEntityBareJidString() + "' attempted to unsubscribe from node '" + nodeName + "'.");
            }
        } finally {
            pubSubManagerOne.deleteNode(nodeName);
        }
    }

    /**
     * Asserts that the server returns a 'not_acceptable' response when
     * specifying a non-existing subscription ID when unsubscribing from a node
     * to which at least one subscription (with an ID) exists.
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws PubSubException.NotAPubSubNodeException if the node cannot be accessed.
     * @throws TestNotPossibleException if the server does not support the functionality required for this test.
     */
    @SmackIntegrationTest(section = "6.2.3.5", quote =
        "(...) If the subscriber originally subscribed with a SubID but the unsubscribe request includes a SubID " +
        "that is not valid or current for the subscriber, the service MUST return a &lt;not-acceptable/&gt; error (...)")
    public void unsubscribeBadSubscriptionIDTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotAPubSubNodeException, TestNotPossibleException {
        // Depending on multi-subscribe is a fail-safe way to be sure that subscription IDs will exist.
        if (!pubSubManagerOne.getSupportedFeatures().containsFeature(PubSubFeature.multi_subscribe)) {
            throw new TestNotPossibleException("Feature 'multi-subscribe' not supported on the server.");
        }

        final String nodeName = "sinttest-unsubscribeBad-nodename-" + testRunId;
        pubSubManagerOne.createNode(nodeName);

        try {
            // Subscribe to the node twice, using different configuration
            final Node subscriberNode = pubSubManagerTwo.getNode(nodeName);
            final EntityBareJid subscriber = conTwo.getUser().asEntityBareJid();
            final Subscription sub1 = subscriberNode.subscribe(subscriber); //once
            final Subscription sub2 = subscriberNode.subscribe(subscriber); //twice

            if (sub1.id == null) {
                throw new IllegalStateException("Setup failed - first subscription request from '" + subscriber + "' to '" + nodeName + "' does not yield a subscription ID.");
            }
            if (sub2.id == null) {
                throw new IllegalStateException("Setup failed - second subscription request from '" + subscriber + "' to '" + nodeName + "' does not yield a subscription ID.");
            }
            if (sub1.equals(sub2)) {
                throw new IllegalStateException("Setup failed - both subscription requests from '" + subscriber + "' to '" + nodeName + "' should have received distinct subscription IDs, but they are equal to each-other: " + sub1);
            }

            final String subscriptionId = "this-is-not-an-existing-subscription-id";
            try {
                subscriberNode.unsubscribe(subscriber.asEntityBareJidString(), subscriptionId);
                fail("The server should have returned a <not-acceptable/> error when '" + subscriber + "' tried to unsubscribe from node '" + nodeName + "' using subscription ID '" + subscriptionId + "', but did not.");
            } catch (XMPPErrorException e) {
                assertEquals(StanzaError.Condition.not_acceptable, e.getStanzaError().getCondition(),
                    "Unexpected condition in the (expected) error returned by the server when '" + subscriber + "' attempted to unsubscribe from node '" + nodeName + "' using subscription ID '" + subscriptionId + "'.");
            }
        } finally {
            pubSubManagerOne.deleteNode(nodeName);
        }
    }

    /**
     * Asserts that an empty subscriptions collection is returned when an entity
     * requests its subscriptions from a node that it is not subscribed to.
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws PubSubException.NotAPubSubNodeException if the node cannot be accessed.
     */
    @SmackIntegrationTest(section = "5.6", quote =
        "If the requesting entity has no subscriptions, the pubsub service MUST return an empty <subscriptions/> element.")
    public void getEmptySubscriptionsTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotAPubSubNodeException {
        final String nodeName = "sinttest-get-empty-subscriptions-test-nodename-" + testRunId;
        pubSubManagerOne.createNode(nodeName);
        try {
            // Assert that subscriptions for a non-subscriber is reported as an empty list.
            final List<Subscription> subscriptions = pubSubManagerTwo.getNode(nodeName).getSubscriptions();
            assertNotNull(subscriptions, "Expected an empty subscription collection when '" + conTwo.getUser().asEntityBareJid() + "' requested its subscriptions from '" + nodeName + "' (but received a NULL-response).");
            assertTrue(subscriptions.isEmpty(), "Expected an empty subscription collection when '" + conTwo.getUser().asEntityBareJid() + "' requested its subscriptions from '" + nodeName + "' (but it was non-empty).");
        } finally {
            pubSubManagerOne.deleteNode(nodeName);
        }
    }

    /**
     * Asserts that one receives a published item, after subscribing to a node.
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws ExecutionException if waiting for the response was interrupted.
     * @throws PubSubException if the involved node is not a pubsub node.
     */
    @SmackIntegrationTest
    public void receivePublishedItemTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, ExecutionException, PubSubException {
        final String nodeName = "sinttest-receive-published-item-nodename-" + testRunId;
        final String needle = "test content " + Math.random();
        LeafNode publisherNode = pubSubManagerOne.createNode(nodeName);
        try {
            final Node subscriberNode = pubSubManagerTwo.getNode(nodeName);
            final EntityBareJid subscriber = conTwo.getUser().asEntityBareJid();
            subscriberNode.subscribe(subscriber);

            final CompletableFuture<Stanza> result = new CompletableFuture<>();
            conTwo.addAsyncStanzaListener(result::complete, stanza -> stanza.toXML().toString().contains(needle));

            publisherNode.publish(new PayloadItem<>(GeoLocation.builder().setDescription(needle).build()));

            assertNotNull(result.get(conTwo.getReplyTimeout(), TimeUnit.MILLISECONDS), "Expected '" + conTwo.getUser() + "' to receive the item that was just published by '" + conOne.getUser() + "' to node '" + nodeName + "', (but did not).");
        } catch (TimeoutException e) {
            throw new AssertionError("Expected '" + conTwo.getUser() + "' to receive the item that was just published by '" + conOne.getUser() + "' to node '" + nodeName + "', (but did not).", e);
        } finally {
            pubSubManagerOne.deleteNode(nodeName);
        }
    }

    /**
     * Asserts that an item can be deleted from a node.
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws TestNotPossibleException When the server does not announce the http://jabber.org/protocol/pubsub#delete-items feature.
     * @see <a href="https://xmpp.org/extensions/xep-0060.html#publisher-delete">7.2 Delete an Item from a Node</a>
     */
    @SmackIntegrationTest(section = "7.2.3.6", quote =
        "To delete an item, the publisher sends a retract request as shown in the following examples. The <retract/> " +
        "element MUST possess a 'node' attribute (...)")
    public void deleteItemTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, TestNotPossibleException
    {
        final DiscoverInfo discoveredInfo = ServiceDiscoveryManager.getInstanceFor(conOne).discoverInfo(pubSubManagerOne.getServiceJid());
        if (!discoveredInfo.containsFeature("http://jabber.org/protocol/pubsub#delete-items")) {
            throw new TestNotPossibleException("Deleting a pubsub node is optional functionality that does not seem to be supported by this server. It does not announce the 'http://jabber.org/protocol/pubsub#delete-items' feature.");
        }

        final String nodeName = "sinttest-delete-item-nodename-" + testRunId;
        final String needle = "test-item-" + Math.random();
        LeafNode node = pubSubManagerOne.createNode(nodeName);
        try {
            // Publish a new item.
            node.publish(new PayloadItem<>(needle, GeoLocation.builder().setDescription("This item was created during an automated test.").build()));

            // Retract the item.
            node.deleteItem(needle);

            // Retrieve items and assert that the item that was just deleted is no longer among them.
            final List<Item> items = node.getItems();
            assertFalse(items.stream().anyMatch(item -> item.getId().equals(needle)),
                "After deleting item '" + needle + "' it was expected that this item no longer was returned in a request for items of node '" + nodeName + "' (but it was).");
        } finally {
            pubSubManagerOne.deleteNode(nodeName);
        }
    }

    /**
     * Asserts that the server returns an 'item-not-found' error response when
     * deleting a node that does not exist.
     *
     * @throws NoResponseException   if there was no response from the remote
     *                               entity.
     * @throws XMPPErrorException    if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException  if the calling thread was interrupted.
     */
    @SmackIntegrationTest(section = "8.4.3.2", quote =
        "If the requesting entity attempts to delete a node that does not exist, the service MUST return an " +
        "<item-not-found/> error.")
    public void deleteNonExistentNodeTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        final String nodeName = "sinttest-delete-node-that-does-not-exist-" + testRunId;
        // Delete an non existent node
        assertFalse(pubSubManagerOne.deleteNode(nodeName), "The server should have returned a <item-not-found/> error when '" + conOne.getUser() + "' tried to delete node '" + nodeName + "', but did not.");
    }

    /**
     * Assert that the server send a notification to subscribers when deleting a
     * node that exist.
     *
     * @throws NoResponseException                     if there was no response from the remote entity.
     * @throws NotConnectedException                   if the XMPP connection is not connected.
     * @throws InterruptedException                    if the calling thread was interrupted.
     * @throws PubSubException.NotAPubSubNodeException if the node cannot be accessed.
     * @throws ExecutionException                      if the execution was aborted by an exception
     */
    @SmackIntegrationTest(section = "8.4.1", quote =
        "In order to delete a node, a node owner MUST send a node deletion request, consisting of a <delete/> element " +
        "whose 'node' attribute specifies the NodeID of the node to be deleted ")
    public void deleteNodeAndNotifySubscribersTest() throws NoResponseException, ExecutionException,
            NotConnectedException, InterruptedException, PubSubException.NotAPubSubNodeException {
        final String nodeName = "sinttest-delete-node-that-exist-" + testRunId;
        final String needle = "<event xmlns='http://jabber.org/protocol/pubsub#event'>";
        final EntityBareJid subscriber = conTwo.getUser().asEntityBareJid();
        try {
            @SuppressWarnings("unused") LeafNode node = pubSubManagerOne.createNode(nodeName);
            final Node subscriberNode = pubSubManagerTwo.getNode(nodeName);
            subscriberNode.subscribe(subscriber);
            final CompletableFuture<Stanza> result = new CompletableFuture<>();
            conTwo.addAsyncStanzaListener(result::complete, stanza -> stanza.toXML().toString().contains(needle));

            // Delete an existent node
            pubSubManagerOne.deleteNode(nodeName);

            assertNotNull(result.get(conTwo.getReplyTimeout(), TimeUnit.MILLISECONDS), "Expected '" + subscriber + "' to receive a notification after '" + conOne.getUser() + "' deleted node '" + nodeName + "' (but did not)");
        } catch (XMPPErrorException e) {
            assertEquals(StanzaError.Condition.item_not_found, e.getStanzaError().getCondition());
        } catch (TimeoutException e) {
            throw new AssertionError("Expected '" + subscriber + "' to receive a notification after '" + conOne.getUser() + "' deleted node '" + nodeName + "' (but did not)", e);
        }
    }

    /**
     * Asserts that publishing an item with the same ID overwrites the previous item with that ID.
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    @SmackIntegrationTest(section = "7.1.2", quote =
        "Note: If the publisher previously published an item with the same ItemID, successfully processing the " +
        "request means that the service MUST overwrite the old item with the new item and then proceed as follows.")
    public void publishOverrideItemTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        final String nodeName = "sinttest-publish-item-nodename-" + testRunId;
        final String itemId = "reused-id-for-testrun-" + testRunId;
        final String needleA = "test content A" + Math.random();
        final String needleB = "test content B" + Math.random();

        final FillableConfigureForm config = pubSubManagerOne.getDefaultConfiguration().getFillableForm();
        config.setPersistentItems(true);
        config.setMaxItems(2);

        LeafNode node = (LeafNode) pubSubManagerOne.createNode(nodeName, config);
        try {
            // Publish a new item.
            node.publish(new PayloadItem<>(itemId, GeoLocation.builder().setDescription(needleA).build()));
            node.publish(new PayloadItem<>(itemId, GeoLocation.builder().setDescription(needleB).build()));

            // Retrieve items and assert that the item that was just published is among them.
            final List<Item> items = node.getItems();
            assertEquals(1, items.size(), "Unexpected item count in node '" + nodeName + "'.");
            final Item item = items.iterator().next();
            assertEquals(itemId, item.getId(), "Unexpected item ID for the item published in node '" + nodeName + "'.");
            assertFalse(item.toXML().toString().contains(needleA), "The item published on node '" + nodeName + "' unexpectedly equals the first (not the second) item that was published.");
            assertTrue(item.toXML().toString().contains(needleB), "The item published on node '" + nodeName + "' unexpectedly did not equal the second (not the second) item that was published.");
        } finally {
            pubSubManagerOne.deleteNode(nodeName);
        }
    }
}

