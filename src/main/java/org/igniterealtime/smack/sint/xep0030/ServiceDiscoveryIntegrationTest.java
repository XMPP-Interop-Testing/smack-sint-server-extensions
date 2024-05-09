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
package org.igniterealtime.smack.sint.xep0030;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.roster.RosterUtil;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpecificationReference(document = "XEP-0030", version = "2.5.0")
public class ServiceDiscoveryIntegrationTest extends AbstractSmackIntegrationTest
{
    public ServiceDiscoveryIntegrationTest(SmackIntegrationTestEnvironment environment) throws SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        super(environment);

        try {
            final DiscoverInfo discoInfoRequest = DiscoverInfo.builder(connection)
                .to(environment.configuration.service)
                .build();
            connection.sendIqRequestAndWaitForResponse(discoInfoRequest);
        } catch (XMPPException.XMPPErrorException e) {
            throw new TestNotPossibleException("XEP-0030: Service Discovery is not supported by service " + environment.configuration.service);
        }
    }

    /**
     * Asserts that the domain under test returns a service discover 'info' response that contains at least one identity.
     */
    @SmackIntegrationTest(section = "3.1", quote = "The target entity then MUST either return an IQ result, or return an error [...]. The result MUST contain a <query/> element [...], which in turn contains one or more <identity/> elements [...]")
    public void testDiscoInfoResponseContainsIdentity() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final DiscoverInfo request = DiscoverInfo.builder(conOne)
            .to(conOne.getXMPPServiceDomain())
            .build();

        // Execute system-under-test.
        final DiscoverInfo response = connection.sendIqRequestAndWaitForResponse(request);

        // Verify result.
        assertFalse(response.getIdentities().isEmpty(), "Expected the disco#info response from '" + conOne.getXMPPServiceDomain() + "' to contain at least one identity (but it did not).");
    }

    /**
     * Asserts that the domain under test returns a service discover 'info' response that contains at least one feature.
     */
    @SmackIntegrationTest(section = "3.1", quote = "The target entity then MUST either return an IQ result, or return an error [...]. The result MUST contain a <query/> element [...], which in turn contains [...] one or more <feature/> elements.")
    public void testDiscoInfoResponseContainsFeature() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final DiscoverInfo request = DiscoverInfo.builder(conOne)
            .to(conOne.getXMPPServiceDomain())
            .build();

        // Execute system-under-test.
        final DiscoverInfo response = connection.sendIqRequestAndWaitForResponse(request);

        // Verify result.
        assertFalse(response.getFeatures().isEmpty(), "Expected the disco#info response from '" + conOne.getXMPPServiceDomain() + "' to contain at least one feature (but it did not).");
    }

    /**
     * Asserts that the domain under test returns a service discover 'info' response that contains at least the feature
     * identified by the 'http://jabber.org/protocol/disco#info' namespace.
     */
    @SmackIntegrationTest(section = "3.1", quote = "[...] every entity MUST support at least the 'http://jabber.org/protocol/disco#info' feature;")
    public void testDiscoInfoResponseContainsDiscoInfoFeature() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final DiscoverInfo request = DiscoverInfo.builder(conOne)
            .to(conOne.getXMPPServiceDomain())
            .build();

        // Execute system-under-test.
        final DiscoverInfo response = connection.sendIqRequestAndWaitForResponse(request);

        // Verify result.
        final String needle = "http://jabber.org/protocol/disco#info";
        assertTrue(response.getFeatures().stream().anyMatch(feature -> feature.getVar().equals(needle)), "Expected the disco#info response from '" + conOne.getXMPPServiceDomain() + "' to contain the '" + needle + "' feature (but it did not).");
    }

    /**
     * Asserts that each identity returned in a service discover 'info' response by the domain under possesses a 'category'.
     */
    @SmackIntegrationTest(section = "3.1", quote = "Each <identity/> element MUST possess the 'category' [...] attribute[s] specifying the category [...] for the entity")
    public void testIdentitiesPossessCategory() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final DiscoverInfo request = DiscoverInfo.builder(conOne)
            .to(conOne.getXMPPServiceDomain())
            .build();

        // Execute system-under-test.
        try {
            connection.sendIqRequestAndWaitForResponse(request);
        } catch (IllegalArgumentException e) {
            // Verify result.
            // Smack's XMPP parser will catch a missing category, and will throw an exception with this particular message.
            if ("category cannot be null".equals(e.getMessage())) { // TODO: Instead of hard-coding the error message used by Smack, detect it programmatically.
                fail("Expected all 'identity' elements returned by '" + conOne.getXMPPServiceDomain() + "' to contain a category (but at least one did not).");
                return;
            }
            throw e;
        }
    }

    /**
     * Asserts that each identity returned in a service discover 'info' response by the domain under possesses a 'type'.
     */
    @SmackIntegrationTest(section = "3.1", quote = "Each <identity/> element MUST possess the [...] 'type' attribute[s] specifying the [...] type for the entity")
    public void testIdentitiesPossessType() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final DiscoverInfo request = DiscoverInfo.builder(conOne)
            .to(conOne.getXMPPServiceDomain())
            .build();

        // Execute system-under-test.
        try {
            connection.sendIqRequestAndWaitForResponse(request);
        } catch (IllegalArgumentException e) {
            // Verify result.
            // Smack's XMPP parser will catch a missing category, and will throw an exception with this particular message.
            if ("type cannot be null".equals(e.getMessage())) { // TODO: Instead of hard-coding the error message used by Smack, detect it programmatically.
                fail("Expected all 'identity' elements returned by '" + conOne.getXMPPServiceDomain() + "' to contain a type (but at least one did not).");
                return;
            }
            throw e;
        }
    }

    /**
     * Asserts that the 'info' response returned by the domain under test contains no identities that have the same
     * category, type and xml:lang, but a different name.
     */
    @SmackIntegrationTest(section = "3.1", quote = "the <query/> element MUST NOT include multiple <identity/> elements with the same category+type+xml:lang but with different 'name' values")
    public void testDiscoInfoResponseNoDifferentNameForCatTypeAndLang() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final DiscoverInfo request = DiscoverInfo.builder(conOne)
            .to(conOne.getXMPPServiceDomain())
            .build();

        // Execute system-under-test.
        final DiscoverInfo response = connection.sendIqRequestAndWaitForResponse(request);

        // Verify result.
        final Function<DiscoverInfo.Identity, String> keyFunction = identity -> identity.getCategory() + "|" + identity.getType() + "|" + identity.getLanguage();
        final ConcurrentMap<String, HashSet<String>> namesByKey = new ConcurrentHashMap<>();
        for (final DiscoverInfo.Identity identity : response.getIdentities()) {
            namesByKey.computeIfAbsent(keyFunction.apply(identity), k -> new HashSet<>()).add(identity.getName());
        }
        assertTrue(namesByKey.values().stream().noneMatch(s -> s.size() >= 2), "Expected the disco#info response from '" + conOne.getXMPPServiceDomain() + "' to contain only equally named identities for identities that share the same category, type and language (but for at least one such combination, multiple names were provided).");
    }

    /**
     * Asserts that each identity returned in a service discover 'info' response by the domain under possesses a 'type'.
     */
    @SmackIntegrationTest(section = "3.1", quote = "Each <feature/> element MUST possess a 'var' attribute [...]")
    public void testFeaturesPossessVar() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final DiscoverInfo request = DiscoverInfo.builder(conOne)
            .to(conOne.getXMPPServiceDomain())
            .build();

        // Execute system-under-test.
        try {
            connection.sendIqRequestAndWaitForResponse(request);
        } catch (IllegalArgumentException e) {
            // Verify result.
            // Smack's XMPP parser will catch a missing category, and will throw an exception with this particular message.
            if ("variable cannot be null".equals(e.getMessage())) { // TODO: Instead of hard-coding the error message used by Smack, detect it programmatically.
                fail("Expected all 'feature' elements returned by '" + conOne.getXMPPServiceDomain() + "' to contain a var (but at least one did not).");
                return;
            }
            throw e;
        }
    }

    /**
     * Asserts that a disco#items request is responded to (with either a result or error).
     */
    @SmackIntegrationTest(section = "4.1", quote = "The target entity then MUST either return its list of publicly-available items, or return an error.")
    public void testDiscoItemsResponse() throws SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException {
        Stanza response;
        try {
            // Execute system-under-test.
            response = ServiceDiscoveryManager.getInstanceFor(conOne).discoverItems(conOne.getXMPPServiceDomain());
        } catch (XMPPException.XMPPErrorException e) {
            response = e.getStanza();
        }

        // Verify result.
        assertNotNull(response, "Expected '" + conOne.getUser()  + "' to receive a response (either an error or a result) from '" + conOne.getXMPPServiceDomain() + "' on the discovery items request that it sent.");
    }

    /**
     * Asserts that all items that can be discovered through service discovery contain a 'jid' attribute.
     */
    @SmackIntegrationTest(section = "4.1", quote = "the <item/> child MUST possess a 'jid' attribute specifying the JID of the item")
    public void testDiscoItemsHaveJidNodeHierarchy() throws SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException {
        try {
            // Prepare fixture
            final TreeNode root = new TreeNode(null,null, null);

            // Execute system-under-test.
            populate(root, ServiceDiscoveryManager.getInstanceFor(conOne), new Coordinates(conOne.getXMPPServiceDomain(), null), new HashSet<>());
            final Collection<TreeNode> everything = root.getAllDescendants();

            // Verify result.
            if (everything.isEmpty()) {
                throw new TestNotPossibleException("Service " + conOne.getXMPPServiceDomain() + " returns an empty list of disco items, making it impossible to test if items have a 'jid' attribute value.");
            }

            assertFalse(everything.stream().anyMatch(treeNode -> treeNode.getCoordinates().getJid() == null), "Expected all disco items returned by '" + conOne.getXMPPServiceDomain() + "' to contain a 'jid' attribute value (but at least one did not).");
        } catch (XMPPException.XMPPErrorException e) {
            throw new TestNotPossibleException("Service " + conOne.getXMPPServiceDomain() + " returns an IQ error instead of disco items, making it impossible to test if items have a 'jid' attribute value.");
        }
    }

    /**
     * Asserts that all items in a hierarchy (where the category is used) is identified as a branch or a leaf in its identity.
     */
    @SmackIntegrationTest(section = "4.3", quote = "If the hierarchy category is used, every node in the hierarchy MUST be identified as either a branch or a leaf")
    public void testDiscoItemsHaveConsistentHierarchy() throws SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException {
        try {
            // Prepare fixture
            final TreeNode root = new TreeNode(null, null, null);

            // Execute system-under-test.
            populate(root, ServiceDiscoveryManager.getInstanceFor(conOne), new Coordinates(conOne.getXMPPServiceDomain(), null), new HashSet<>());
            final Collection<TreeNode> everything = root.getAllDescendants();

            // Verify result.
            final Set<String> hierarchyButNotBranchOrLeaf = everything.stream()
                .filter(treeNode -> treeNode.getInfo().getIdentities().stream()
                    .anyMatch(identity -> identity.getCategory().equals("hierarchy") && !Set.of("branch", "leaf").contains(identity.getType()))
                )
                .map(TreeNode::getCoordinates)
                .map(c -> c.getJid() + (c.getNode() == null ? "" : ("#" + c.getNode())))
                .collect(Collectors.toSet());
            assertTrue(hierarchyButNotBranchOrLeaf.isEmpty(), "Unexpectedly found service discovery node(s) returned by '" + conOne.getXMPPServiceDomain() + "' that define a category 'branch' but not a type of 'branch' or 'leaf'. Offending node coordinate(s): " + String.join(", ", hierarchyButNotBranchOrLeaf));

            final Set<String> branchWithChildrenOutOfHierarchy = everything.stream()
                .filter(treeNode -> treeNode.getInfo().hasIdentity("hierarchy", "branch"))
                .filter(treeNode -> treeNode.getChildren().stream().noneMatch(childNode -> childNode.getInfo().getIdentities().stream().anyMatch(identity -> identity.getCategory().equals("hierarchy"))))
                .map(TreeNode::getCoordinates)
                .map(c -> c.getJid() + (c.getNode() == null ? "" : ("#" + c.getNode())))
                .collect(Collectors.toSet());
            assertTrue(branchWithChildrenOutOfHierarchy.isEmpty(), "Unexpectedly found service discovery node(s) returned by '" + conOne.getXMPPServiceDomain() + "' that are a 'branch' but have at least one child element that does not identify as either a branch or a leaf. Offending branch node coordinate(s): " + String.join(", ", hierarchyButNotBranchOrLeaf));

            // Assuming that a hierarchy does not need to start at the very root node then:
            // - a branch, but also a leaf, doesn't have to have a parent that's in the hierarchy (as they're the 'start' of the hierarchy)
            // - a branch, but also a leaf, can exist in collection of siblings that are not in a hierarchy (as it is the 'start' of the hierarchy that the siblings aren't part of).
            // TODO: Is this rationale correct? Are more assertions needed in this test?
        } catch (XMPPException.XMPPErrorException e) {
            throw new TestNotPossibleException("Service " + conOne.getXMPPServiceDomain() + " returns an IQ error instead of disco items, making it impossible to test if items have a 'jid' attribute value.");
        }
    }

    /**
     * Asserts that a service-unavailable is returned in response to a disco#info request to a non-existing entity, while
     * the request does not specify a node. This test is executed against a bare JID 'client' address, since the server
     * is expected to respond to such requests on behalf of the account.
     */
    @SmackIntegrationTest(section = "8", quote = "The following rule[s] apply to the handling of service discovery requests sent to bare JIDs: In response to a disco#info request, the server MUST return a <service-unavailable/> error if [...] [t]he target entity does not exist.")
    public void testQueryInfoNonExistingBareJidClientEntityWithoutNode() throws SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XmppStringprepException
    {
        // Setup test fixture.
        final DiscoverInfo request = DiscoverInfo.builder(conOne)
            .to(JidCreate.entityBareFrom(Localpart.from("test-non-existing-bare-jid-" + StringUtils.randomString(19)), conOne.getXMPPServiceDomain()))
            .build();

        // Execute system-under-test.
        try {
            connection.sendIqRequestAndWaitForResponse(request);

            // Verify result.
            fail("Expected the disco#info request from '" + conOne.getUser() + "' to non-existing entity '" + request.getTo() + "' to be responded to with an error (but it was not).");
        } catch (XMPPException.XMPPErrorException e) {
            assertEquals(StanzaError.Condition.service_unavailable, e.getStanzaError().getCondition(), "Unexpected condition in (expected) error response to the disco#info request from '" + conOne.getUser() + "' to non-existing entity '" + request.getTo() + "'.");
        }
    }

    /**
     * Asserts that a service-unavailable is returned in response to a disco#info request to a non-existing entity, while
     * the request specifies a node. This test is executed against a bare JID 'client' address, since the server is
     * expected to respond to such requests on behalf of the account.
     */
    @SmackIntegrationTest(section = "8", quote = "The following rule[s] apply to the handling of service discovery requests sent to bare JIDs: In response to a disco#info request, the server MUST return a <service-unavailable/> error if [...] [t]he target entity does not exist.")
    public void testQueryInfoNonExistingBareJidClientEntityWithNode() throws SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XmppStringprepException
    {
        // Setup test fixture.
        final DiscoverInfo request = DiscoverInfo.builder(conOne)
            .to(JidCreate.entityBareFrom(Localpart.from("test-non-existing-bare-jid-" + StringUtils.randomString(19)), conOne.getXMPPServiceDomain()))
            .setNode("a-test-node-" + StringUtils.randomString(11))
            .build();

        // Execute system-under-test.
        try {
            connection.sendIqRequestAndWaitForResponse(request);

            // Verify result.
            fail("Expected the disco#info request from '" + conOne.getUser() + "' to non-existing entity '" + request.getTo() + "' to be responded to with an error (but it was not).");
        } catch (XMPPException.XMPPErrorException e) {
            assertEquals(StanzaError.Condition.service_unavailable, e.getStanzaError().getCondition(), "Unexpected condition in (expected) error response to the disco#info request from '" + conOne.getUser() + "' to non-existing entity '" + request.getTo() + "'.");
        }
    }

    /**
     * Asserts that a service-unavailable is returned in response to a disco#info request to an existing entity that to
     * which the requestor is not presence-subscribed, while the request does not specify a node. This test is executed
     * against a bare JID 'client' address, since the server is expected to respond to such requests on behalf of the
     * account.
     */
    @SmackIntegrationTest(section = "8", quote = "The following rule[s] apply to the handling of service discovery requests sent to bare JIDs: In response to a disco#info request, the server MUST return a <service-unavailable/> error if [...] [t]he requesting entity is not authorized to receive presence from the target entity.")
    public void testQueryInfoWithoutPresenceSubscriptionBareJidWithoutNode() throws SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        RosterUtil.ensureNotSubscribedToEachOther(conOne, conTwo);
        final DiscoverInfo request = DiscoverInfo.builder(conOne)
            .to(conTwo.getUser().asBareJid())
            .build();

        // Execute system-under-test.
        try {
            connection.sendIqRequestAndWaitForResponse(request);

            // Verify result.
            fail("Expected the disco#info request from '" + conOne.getUser() + "' to '" + request.getTo() + "' (which has not granted presence subscription to the requestor) to be responded to with an error (but it was not).");
        } catch (XMPPException.XMPPErrorException e) {
            assertEquals(StanzaError.Condition.service_unavailable, e.getStanzaError().getCondition(), "Unexpected condition in (expected) error response to the disco#info request from '" + conOne.getUser() + "' to '" + request.getTo() + "' (which has not granted presence subscription to the requestor).");
        }
    }

    /**
     * Asserts that a service-unavailable is returned in response to a disco#info request to an existing entity that to
     * which the requestor is not presence-subscribed, while the request specifies a node. This test is executed
     * against a bare JID 'client' address, since the server is expected to respond to such requests on behalf of the
     * account.
     */
    @SmackIntegrationTest(section = "8", quote = "The following rule[s] apply to the handling of service discovery requests sent to bare JIDs: In response to a disco#info request, the server MUST return a <service-unavailable/> error if [...] [t]he requesting entity is not authorized to receive presence from the target entity.")
    public void testQueryInfoWithoutPresenceSubscriptionBareJidWithNode() throws SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        RosterUtil.ensureNotSubscribedToEachOther(conOne, conTwo);
        final DiscoverInfo request = DiscoverInfo.builder(conOne)
            .to(conTwo.getUser().asBareJid())
            .setNode("a-test-node-" + StringUtils.randomString(11))
            .build();

        // Execute system-under-test.
        try {
            connection.sendIqRequestAndWaitForResponse(request);

            // Verify result.
            fail("Expected the disco#info request from '" + conOne.getUser() + "' to '" + request.getTo() + "' (which has not granted presence subscription to the requestor) to be responded to with an error (but it was not).");
        } catch (XMPPException.XMPPErrorException e) {
            assertEquals(StanzaError.Condition.service_unavailable, e.getStanzaError().getCondition(), "Unexpected condition in (expected) error response to the disco#info request from '" + conOne.getUser() + "' to '" + request.getTo() + "' (which has not granted presence subscription to the requestor).");
        }
    }

    /**
     * Asserts that an empty set is returned in response to a disco#items request to a non-existing entity, while
     * the request does not specify a node. This test is executed against a bare JID 'client' address, since the server
     * is expected to respond to such requests on behalf of the account.
     */
    @SmackIntegrationTest(section = "8", quote = "The following rule[s] apply to the handling of service discovery requests sent to bare JIDs: In response to a disco#items request, the server MUST return an empty result set if [...] [t]he target entity does not exist.")
    public void testQueryItemsNonExistingEntityBareJidWithoutNode() throws SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XmppStringprepException, XMPPException.XMPPErrorException
    {
        // Setup test fixture.
        final ServiceDiscoveryManager manOne = ServiceDiscoveryManager.getInstanceFor(conOne);
        final EntityBareJid nonexistingEntityAddress = JidCreate.entityBareFrom(Localpart.from("test-non-existing-bare-jid-" + StringUtils.randomString(19)), conOne.getXMPPServiceDomain());

        // Execute system-under-test.
        final DiscoverItems result = manOne.discoverItems(nonexistingEntityAddress);

        // Verify result.
        assertTrue(result.getItems().isEmpty(), "Expected the disco#items request from '" + conOne.getUser() + "' to non-existing entity '" + nonexistingEntityAddress + "' to be responded to with an empty result set (but it was not).");
    }

    /**
     * Asserts that an empty set is returned in response to a disco#items request to a non-existing entity, while
     * the request specifies a node. This test is executed against a bare JID 'client' address.
     */
    @SmackIntegrationTest(section = "8", quote = "The following rule[s] apply to the handling of service discovery requests sent to bare JIDs: In response to a disco#items request, the server MUST return an empty result set if [...] [t]he target entity does not exist.")
    public void testQueryItemsNonExistingEntityBareJidWithNode() throws SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XmppStringprepException, XMPPException.XMPPErrorException
    {
        // Setup test fixture.
        final ServiceDiscoveryManager manOne = ServiceDiscoveryManager.getInstanceFor(conOne);
        final EntityBareJid nonexistingEntityAddress = JidCreate.entityBareFrom(Localpart.from("test-non-existing-bare-jid-" + StringUtils.randomString(19)), conOne.getXMPPServiceDomain());

        // Execute system-under-test.
        final DiscoverItems result = manOne.discoverItems(nonexistingEntityAddress, "a-test-node-" + StringUtils.randomString(11));

        // Verify result.
        assertTrue(result.getItems().isEmpty(), "Expected the disco#items request from '" + conOne.getUser() + "' to non-existing entity '" + nonexistingEntityAddress + "' to be responded to with an empty result set (but it was not).");
    }

    /**
     * Asserts that an empty set is returned in response to a disco#info request to an existing entity that to
     * which the requestor is not presence-subscribed, while the request does not specify a node. This test is executed
     * against a bare JID 'client' address, since the server is expected to respond to such requests on behalf of the
     * account.
     */
    @SmackIntegrationTest(section = "8", quote = "The following rule[s] apply to the handling of service discovery requests sent to bare JIDs: In response to a disco#items request, the server MUST return an empty result set if [...] [t]he target entity does not exist.")
    public void testItemsQueryWithoutPresenceSubscriptionBareJidWithoutNode() throws SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XmppStringprepException, XMPPException.XMPPErrorException
    {
        // Setup test fixture.
        RosterUtil.ensureNotSubscribedToEachOther(conOne, conTwo);
        final ServiceDiscoveryManager manOne = ServiceDiscoveryManager.getInstanceFor(conOne);

        // Execute system-under-test.
        final DiscoverItems result = manOne.discoverItems(conTwo.getUser().asEntityBareJid());

        // Verify result.
        assertTrue(result.getItems().isEmpty(), "Expected the disco#items request from '" + conOne.getUser() + "' to '" + conTwo.getUser() + "' (which has not granted presence subscription to the requestor) to be responded to with an empty result set (but it was not).");
    }

    /**
     * Retrieves all service discovery items in a tree, including the "Node Hierarchy" as defined in XEP-0030 section 4.2.
     */
    private void populate(final TreeNode parent, final ServiceDiscoveryManager manager, final Coordinates coordinates, final Set<Coordinates> uniques) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        if (!uniques.add(coordinates)) {
            // This node has already been visited before. To avoid recursion, stop here.
            return;
        }

        final DiscoverInfo discoveredInfo = manager.discoverInfo(coordinates.getJid(), coordinates.getNode());
        final TreeNode treeNode = new TreeNode(parent, coordinates, discoveredInfo);
        parent.getChildren().add(treeNode);

        final DiscoverItems discoveredItems = manager.discoverItems(coordinates.getJid(), coordinates.getNode());
        if (!discoveredItems.getItems().isEmpty()) {
            for (final DiscoverItems.Item item : discoveredItems.getItems()) {
                final Coordinates childCoordinates = Coordinates.of(item);
                populate(treeNode, manager, childCoordinates, uniques);
            }
        }
    }

    public static class TreeNode {
        private final TreeNode parent;
        private final Coordinates coordinates;
        private final DiscoverInfo info;
        private final Collection<TreeNode> children;

        public TreeNode(TreeNode parent, Coordinates coordinates, DiscoverInfo info)
        {
            this.parent = parent;
            this.coordinates = coordinates;
            this.info = info;
            this.children = new HashSet<>();
        }

        public TreeNode getParent() {
            return parent;
        }

        public Coordinates getCoordinates()
        {
            return coordinates;
        }

        public DiscoverInfo getInfo()
        {
            return info;
        }

        public Collection<TreeNode> getChildren()
        {
            return children;
        }

        public Collection<TreeNode> getAllDescendants() {
            final HashSet<TreeNode> result = new HashSet<>();
            getDescendants(result, this);
            return result;
        }

        void getDescendants(Collection<TreeNode> result, TreeNode node) {
            for (TreeNode child : node.getChildren()) {
                if (result.add(child)) {
                    getDescendants(result, child);
                }
            }
        }
    }

    public static class Coordinates
    {
        private final Jid jid;

        private final String node;

        public static Coordinates of(final DiscoverItems.Item item) {
            return new Coordinates(item.getEntityID(), item.getNode());
        }

        public Coordinates(final Jid jid, final String node)
        {
            this.jid = jid;
            this.node = node;
        }

        public Jid getJid()
        {
            return jid;
        }

        public String getNode()
        {
            return node;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Coordinates that = (Coordinates) o;
            return Objects.equals(jid, that.jid) && Objects.equals(node, that.node);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(jid, node);
        }
    }
}
