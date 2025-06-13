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
package org.igniterealtime.smack.inttest.rfc6121;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.util.StringUtils;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Resourcepart;

import javax.swing.text.html.parser.Entity;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that verify that behavior defined in section 8.5.2.1.1 "Local User / localpart@domainpart / Available or Connected Resources / Message" of section 8 "Server Rules for Processing XML Stanzas" of RFC6121.
 *
 * The specification mentions both 'Available' and 'Connected' resources in its titles, but it does not seem to have any specifications for Connected Resources, apart from a more global description in RFC 6120 (section 8.5).
 * There, the conditions described are mostly based on 'SHOULD' keywords. As such, this implementation tests mostly scenarios with 'Available' resources, and not 'Connected' ones. 
 * See https://mail.jabber.org/hyperkitty/list/standards@xmpp.org/thread/L2JTVXQVXW4EQGFM56H5HHJBU6HVPVXK/
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
@SpecificationReference(document = "RFC6162")
public class RFC6121Section8dot5dot2dot1dot1StanzaProcessingLocalUserConnectedResourcesMessageIntegrationTest extends AbstractSmackIntegrationTest
{
    private final SmackIntegrationTestEnvironment environment;

    public RFC6121Section8dot5dot2dot1dot1StanzaProcessingLocalUserConnectedResourcesMessageIntegrationTest(SmackIntegrationTestEnvironment environment)
    {
        super(environment);
        this.environment = environment;
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\": [...] If there is one available resource with a non-negative presence priority then the server MUST deliver the message to that resource. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testNormalMessageOneResourcePrioPositive() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.normal, List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\": [...] If there is one available resource with a non-negative presence priority then the server MUST deliver the message to that resource. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testNormalMessageOneResourcePrioZero() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.normal, List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority")
    public void testNormalMessageOneResourcePrioNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.normal, List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testNormalMessageMultipleResourcesPrioPositive() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.normal, List.of(1,1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testNormalMessageMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.normal, List.of(1,1,-1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testNormalMessageMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.normal, List.of(3,1,2));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testNormalMessageMultipleResourcesPrioDifferentPositiveAndNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.normal, List.of(1,2,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testNormalMessageMultipleResourcesPrioZero() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.normal, List.of(0,0,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testNormalMessageMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.normal, List.of(0,0,-1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testNormalMessageMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.normal, List.of(3,1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testNormalMessageMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.normal, List.of(1,0,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority")
    public void testNormalMessageMultipleResourcesPrioNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.normal, List.of(-1,-1,-1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If the only available resource has a non-negative presence priority then the server MUST deliver the message to that resource. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testChatMessageOneResourcePrioPositive() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.chat, List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If the only available resource has a non-negative presence priority then the server MUST deliver the message to that resource. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testChatMessageOneResourcePrioZero() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.chat, List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority")
    public void testChatMessageOneResourcePrioNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.chat, List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources that have opted in to receive chat messages. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testChatMessageMultipleResourcesPrioPositive() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.chat, List.of(1,1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources that have opted in to receive chat messages. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testChatMessageMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.chat, List.of(1,1,-1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources that have opted in to receive chat messages. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testChatMessageMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.chat, List.of(3,1,2));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources that have opted in to receive chat messages. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testChatMessageMultipleResourcesPrioDifferentPositiveAndNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.chat, List.of(1,2,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources that have opted in to receive chat messages. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testChatMessageMultipleResourcesPrioZero() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.chat, List.of(0,0,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources that have opted in to receive chat messages. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testChatMessageMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.chat, List.of(0,0,-1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources that have opted in to receive chat messages. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testChatMessageMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.chat, List.of(3,1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources that have opted in to receive chat messages. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testChatMessageMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.chat, List.of(1,0,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority")
    public void testChatMessageMultipleResourcesPrioNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.chat, List.of(-1,-1,-1));
    }

    /**
     * Verifies that a message is either delivered at _all_ resources with non-negative priorities, _or_ at exactly one of them.
     * When negative priorities are provided, the test asserts that the message has _not_ been delivered to the corresponding
     * resource.
     *
     * The steps taken are rather involved:
     * <ol>
     * <li>First, additional resources for conTwo are created, so that this user has as many resources online as the number of priorities provided to this method.
     * <li>For all of these resources, a presence update is sent, to set a particular prio value (from the provided method argument)
     * <li>Then, the message that's the subject of the test is sent to the _bare_ JID of the conTwo user (from the conOne user).
     * <li>Then, a message is sent to the _full_ JID of each of the resources. This is done to avoid having to depend on timeouts.
     * </ol>
     *
     * After receiving the message sent to the full JID, the original message is expected to have been processed (as it
     * was sent later). Assertions can be checked after this message was received.
     */
    public void doTestMessageNormalOrChat(final Message.Type messageType, final List<Integer> resourcePriorities) throws Exception
    {
        if (resourcePriorities.isEmpty()) {
            throw new IllegalArgumentException("The resource priorities must contain at least one element.");
        }

        final Set<FullJid> allResources = new HashSet<>();
        final Set<FullJid> allNonNegativeResources = new HashSet<>();

        // Setup test fixture.
        final List<AbstractXMPPConnection> additionalConnections = new ArrayList<>(resourcePriorities.size()-1);
        for (int i = 0; i < resourcePriorities.size()-1; i++) {
            additionalConnections.add(environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration));
        }

        final Collection<StanzaListener> receivedListeners = new HashSet<>();
        StanzaListener stopListener = null;
        try {
            // Setup test fixture: create connections for the additional resources (based on the user used for 'conTwo').
            for (final AbstractXMPPConnection additionalConnection : additionalConnections) {
                additionalConnection.connect();
                additionalConnection.login(((AbstractXMPPConnection) conTwo).getConfiguration().getUsername(), ((AbstractXMPPConnection) conTwo).getConfiguration().getPassword(), Resourcepart.from(StringUtils.randomString(7)));
            }

            // Setup test fixture: configure the desired resource priority for each of the resource connections.
            for (int i = 0; i < resourcePriorities.size(); i++) {
                final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i-1);
                final int resourcePriority = resourcePriorities.get(i);

                final Presence prioritySet = PresenceBuilder.buildPresence(StringUtils.randomString(9)).setPriority(resourcePriority).build();
                try (final StanzaCollector presenceUpdateDetected = resourceConnection.createStanzaCollectorAndSend(new OrFilter(new StanzaIdFilter(prioritySet), new AndFilter(new FromMatchesFilter(resourceConnection.getUser(), false), (s -> s instanceof Presence && ((Presence) s).getPriority() == resourcePriority))), prioritySet)) {
                    resourceConnection.sendStanza(prioritySet);
                    presenceUpdateDetected.nextResult(); // Wait for echo, to be sure that presence update was processed by the server.
                }

                allResources.add(resourceConnection.getUser());
                if (resourcePriority >= 0) {
                    allNonNegativeResources.add(resourceConnection.getUser());
                }
            }

            // Setup test fixture: prepare for the message that is sent to the bare JID to be sent, and collected while being received by the various resources.
            final String needle = StringUtils.randomString(9);
            final StanzaFilter needleDetector = new AndFilter(FromMatchesFilter.createFull(conOne.getUser()), (s -> s instanceof Message && ((Message) s).getType() == messageType && ((Message) s).getBody().equals(needle)));
            final Map<EntityFullJid, Stanza> receivedBy = new ConcurrentHashMap<>(); // This is what will be evaluated by this test's assertions.

            // Setup test fixture: detect the message that's sent to signal that the test message has been sent and assertion can thus start.
            final SimpleResultSyncPoint messagesProcessedSyncPoint = new SimpleResultSyncPoint();
            stopListener = new StanzaListener() {
                final Set<Jid> recipients = new HashSet<>(allResources);

                @Override
                public void processStanza(Stanza packet) {
                    recipients.remove(packet.getTo());
                    if (recipients.isEmpty()) { // When having received a 'stop' on all resources, the test is ready to evaluate assertions.
                        messagesProcessedSyncPoint.signal();
                    }
                }
            };
            final String stopNeedle = "STOP LISTENING, STANZAS HAVE BEEN PROCESSED " + StringUtils.randomString(7);
            final StanzaFilter stopDetector = new AndFilter(FromMatchesFilter.createFull(conOne.getUser()), (s -> s instanceof Message && ((Message) s).getBody().equals(stopNeedle)));

            for (int i = 0; i < resourcePriorities.size(); i++) {
                final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i - 1);
                final StanzaListener stanzaListener = (stanza) -> receivedBy.put(resourceConnection.getUser(), stanza);
                receivedListeners.add(stanzaListener); // keep track so that the listener can be removed again.
                resourceConnection.addStanzaListener(stanzaListener, needleDetector);
                resourceConnection.addStanzaListener(stopListener, stopDetector);
            }

            // Execute system under test.
            final Message messageToBeProcessed = StanzaBuilder.buildMessage()
                .ofType(messageType)
                .to(conTwo.getUser().asBareJid())
                .setBody(needle)
                .build();

            conOne.sendStanza(messageToBeProcessed);

            // Informs intended recipients that the test is over.
            for (final FullJid recipient : allResources) {
                conOne.sendStanza(StanzaBuilder.buildMessage().setBody(stopNeedle).to(recipient).build());
            }

            try {
                // Wait for all recipients to have received the 'test is over' stanza.
                messagesProcessedSyncPoint.waitForResult(timeout);
            } catch (TimeoutException e) {
                // This is dodgy (concurrency issue? server misbehaving?) but let's not fail the test just yet. After the timeout has expired, it is likely that the test is ready to be evaluated.
                e.printStackTrace();
            }

            // Verify result.
            final Set<EntityFullJid> nonNegativeRecipients = receivedBy.keySet().stream().filter(allNonNegativeResources::contains).collect(Collectors.toSet());
            final Set<EntityFullJid> negativeRecipients = receivedBy.keySet().stream().filter(o -> allResources.contains(o) && !allNonNegativeResources.contains(o)).collect(Collectors.toSet());

            switch (allNonNegativeResources.size()) {
                case 0:
                    // Do nothing to assert.
                    break;
                case 1:
                    assertEquals(1, nonNegativeRecipients.size(), "Expected the message of type '" + messageToBeProcessed.getType() + "' that was sent by '" + conOne.getUser() + "' to the bare JID of '" + conTwo.getUser().asBareJid() +"' to have been received by the single resource that had a non-negative resource: [" + allNonNegativeResources.stream().map(Object::toString).sorted().collect(Collectors.joining(", ")) + "]. Instead the message was received by: [" + receivedBy.keySet().stream().map(Object::toString).sorted().collect(Collectors.joining(", "))+ "]" );
                    break;
                default:
                    if (messageType == Message.Type.chat) {
                        // Message type 'chat' can be influenced by an unspecified (and for the purpose of this test, undetectable) 'opt-in' mechanism. When such an opt-in mechanism is _not_ offered, then the message should be sent to either the highest priority, or _all_ resources that are non-negative. When such an opt-in is offered, it could also be sent to all those that opted-in (which may be zero resources). See https://logs.xmpp.org/xsf/2025-06-06#2025-06-06-44f4ded1943dad29 for more context.
                        assertTrue(nonNegativeRecipients.isEmpty() || nonNegativeRecipients.size() == 1 || nonNegativeRecipients.size() == allNonNegativeResources.size(), "Expected the message of type '" + messageToBeProcessed.getType() + "' that was sent by '" + conOne.getUser() + "' to the bare JID of '" + conTwo.getUser().asBareJid() +"' to have been received by either exactly one, zero (in case an opt-in mechanism is provided by the server, but not used by any of the clients), or all (in case no opt-in mechanism is provided by the server) resources that have non-negative presence: [" + allNonNegativeResources.stream().map(Object::toString).sorted().collect(Collectors.joining(", ")) + "]. Instead the message was received by: [" + receivedBy.keySet().stream().sorted().collect(Collectors.joining(", "))+ "]"  );
                    } else {
                        assertTrue(nonNegativeRecipients.size() == 1 || nonNegativeRecipients.size() == allNonNegativeResources.size(), "Expected the message of type '" + messageToBeProcessed.getType() + "' that was sent by '" + conOne.getUser() + "' to the bare JID of '" + conTwo.getUser().asBareJid() +"' to have been received by either exactly one, or all resources that have non-negative presence: [" + allNonNegativeResources.stream().map(Object::toString).sorted().collect(Collectors.joining(", ")) + "]. Instead the message was received by: [" + receivedBy.keySet().stream().sorted().collect(Collectors.joining(", "))+ "]"  );
                    }
                    break;
            }

            assertTrue(negativeRecipients.isEmpty(), "Expected the message of type '" + messageToBeProcessed.getType() + "' that was sent by '" + conOne.getUser() + "' to the bare JID of '" + conTwo.getUser().asBareJid() +"' to NOT have been received by resources that have a negative priority. Instead, it was received by this/these resource(s) that had a negative priority: [" + negativeRecipients.stream().map(Object::toString).sorted().collect(Collectors.joining(", ")) + "].");

            final Map<EntityFullJid, Jid> invalidAddressees = receivedBy.entrySet().stream().filter((entry) -> !entry.getValue().getTo().equals(messageToBeProcessed.getTo())).collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getTo()));
            final String errorMessage = invalidAddressees.entrySet().stream().map(entry -> "resource '" + entry.getKey() + "' received a stanza addressed to '" + entry.getValue() + "'").collect(Collectors.joining(", "));
            assertTrue(invalidAddressees.isEmpty(), "Expected the 'to' attribute of the message sent by '" + conOne.getUser() + "' to remain unchanged ('" + messageToBeProcessed.getTo() + "'). Instead, these resources received attribute values that were modified: " + errorMessage + ".");
        } finally {
            // Tear down test fixture.
            for (int i = 0; i < resourcePriorities.size(); i++) {
                final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i - 1);
                if (stopListener != null) { resourceConnection.removeStanzaListener(stopListener); }
                receivedListeners.forEach(resourceConnection::removeStanzaListener); // Only one of these will match.
            }

            additionalConnections.forEach(AbstractXMPPConnection::disconnect);
            conTwo.sendStanza(PresenceBuilder.buildPresence().ofType(Presence.Type.available).build()); // This intends to mimic the 'initial presence'.
        }
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"groupchat\", the server MUST NOT deliver the stanza to any of the available resources but instead MUST return a stanza error to the sender [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testGroupchatMessageOneResourcePrioPositive() throws Exception
    {
        doTestMessageGroupchat(List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"groupchat\", the server MUST NOT deliver the stanza to any of the available resources but instead MUST return a stanza error to the sender [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testGroupchatMessageOneResourcePrioZero() throws Exception
    {
        doTestMessageGroupchat(List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority")
    public void testGroupchatMessageOneResourcePrioNegative() throws Exception
    {
        doTestMessageGroupchat(List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"groupchat\", the server MUST NOT deliver the stanza to any of the available resources but instead MUST return a stanza error to the sender [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testGroupchatMessageMultipleResourcesPrioPositive() throws Exception
    {
        doTestMessageGroupchat(List.of(1,1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"groupchat\", the server MUST NOT deliver the stanza to any of the available resources but instead MUST return a stanza error to the sender [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testGroupchatMessageMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestMessageGroupchat(List.of(1,1,-1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"groupchat\", the server MUST NOT deliver the stanza to any of the available resources but instead MUST return a stanza error to the sender [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testGroupchatMessageMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestMessageGroupchat(List.of(3,1,2));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"groupchat\", the server MUST NOT deliver the stanza to any of the available resources but instead MUST return a stanza error to the sender [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testGroupchatMessageMultipleResourcesPrioDifferentPositiveAndNegative() throws Exception
    {
        doTestMessageGroupchat(List.of(1,2,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"groupchat\", the server MUST NOT deliver the stanza to any of the available resources but instead MUST return a stanza error to the sender [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testGroupchatMessageMultipleResourcesPrioZero() throws Exception
    {
        doTestMessageGroupchat(List.of(0,0,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"groupchat\", the server MUST NOT deliver the stanza to any of the available resources but instead MUST return a stanza error to the sender [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testGroupchatMessageMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestMessageGroupchat(List.of(0,0,-1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"groupchat\", the server MUST NOT deliver the stanza to any of the available resources but instead MUST return a stanza error to the sender [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testGroupchatMessageMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestMessageGroupchat(List.of(3,1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"groupchat\", the server MUST NOT deliver the stanza to any of the available resources but instead MUST return a stanza error to the sender [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testGroupchatMessageMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestMessageGroupchat(List.of(1,0,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority")
    public void testGroupchatMessageMultipleResourcesPrioNegative() throws Exception
    {
        doTestMessageGroupchat(List.of(-1,-1,-1));
    }

    /**
     * Verifies that a message is not delivered and an error is returned.
     *
     * The steps taken are rather involved:
     * <ol>
     * <li>First, additional resources for conTwo are created, so that this user has as many resources online as the number of priorities provided to this method.
     * <li>For all of these resources, a presence update is sent, to set a particular prio value (from the provided method argument)
     * <li>Then, the message that's the subject of the test is sent to the _bare_ JID of the conTwo user (from the conOne user).
     * <li>Then, a message is sent to the _full_ JID of each of the resources. This is done to avoid having to depend on timeouts.
     * </ol>
     *
     * After receiving the message sent to the full JID, the original message is expected to have been processed (as it
     * was sent later). Assertions can be checked after this message was received.
     */
    public void doTestMessageGroupchat(final List<Integer> resourcePriorities) throws Exception
    {
        if (resourcePriorities.isEmpty()) {
            throw new IllegalArgumentException("The resource priorities must contain at least one element.");
        }

        final Set<FullJid> allResources = new HashSet<>();

        // Setup test fixture.
        final List<AbstractXMPPConnection> additionalConnections = new ArrayList<>(resourcePriorities.size()-1);
        for (int i = 0; i < resourcePriorities.size()-1; i++) {
            additionalConnections.add(environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration));
        }

        StanzaListener stopListener = null;
        StanzaListener errorListener = null;
        final Collection<StanzaListener> receivedListeners = new HashSet<>();
        try {
            // Setup test fixture: create connections for the additional resources (based on the user used for 'conTwo').
            for (final AbstractXMPPConnection additionalConnection : additionalConnections) {
                additionalConnection.connect();
                additionalConnection.login(((AbstractXMPPConnection) conTwo).getConfiguration().getUsername(), ((AbstractXMPPConnection) conTwo).getConfiguration().getPassword(), Resourcepart.from(StringUtils.randomString(7)));
            }

            // Setup test fixture: configure the desired resource priority for each of the resource connections.
            for (int i = 0; i < resourcePriorities.size(); i++) {
                final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i-1);
                final int resourcePriority = resourcePriorities.get(i);

                final Presence prioritySet = PresenceBuilder.buildPresence(StringUtils.randomString(9)).setPriority(resourcePriority).build();
                try (final StanzaCollector presenceUpdateDetected = resourceConnection.createStanzaCollectorAndSend(new OrFilter(new StanzaIdFilter(prioritySet), new AndFilter(new FromMatchesFilter(resourceConnection.getUser(), false), (s -> s instanceof Presence && ((Presence) s).getPriority() == resourcePriority))), prioritySet)) {
                    resourceConnection.sendStanza(prioritySet);
                    presenceUpdateDetected.nextResult(); // Wait for echo, to be sure that presence update was processed by the server.
                }

                allResources.add(resourceConnection.getUser());
            }

            // Setup test fixture: prepare for the message that is sent to the bare JID to be sent, and collected while being received by the various resources.
            final String needle = StringUtils.randomString(9);
            final StanzaFilter needleDetector = new AndFilter(FromMatchesFilter.createFull(conOne.getUser()), (s -> s instanceof Message && ((Message) s).getType() == Message.Type.groupchat && ((Message) s).getBody().equals(needle)));
            final Map<EntityFullJid, Stanza> receivedBy = new ConcurrentHashMap<>(); // This is what will be evaluated by this test's assertions.

            // Setup test fixture: detect the message that's sent to signal that the test message has been sent and assertion can thus start.
            final SimpleResultSyncPoint messagesProcessedSyncPoint = new SimpleResultSyncPoint();
            stopListener = new StanzaListener() {
                final Set<Jid> recipients = new HashSet<>(allResources);

                @Override
                public void processStanza(Stanza packet) {
                    recipients.remove(packet.getTo());
                    if (recipients.isEmpty()) { // When having received a 'stop' on all resources, the test is ready to evaluate assertions.
                        messagesProcessedSyncPoint.signal();
                    }
                }
            };
            final String stopNeedle = "STOP LISTENING, STANZAS HAVE BEEN PROCESSED " + StringUtils.randomString(7);
            final StanzaFilter stopDetector = new AndFilter(FromMatchesFilter.createFull(conOne.getUser()), (s -> s instanceof Message && ((Message) s).getBody().equals(stopNeedle)));

            for (int i = 0; i < resourcePriorities.size(); i++) {
                final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i - 1);
                final StanzaListener stanzaListener = (stanza) -> receivedBy.put(resourceConnection.getUser(), stanza);
                receivedListeners.add(stanzaListener); // keep track so that the listener can be removed again.
                resourceConnection.addStanzaListener(stanzaListener, needleDetector);
                resourceConnection.addStanzaListener(stopListener, stopDetector);
            }

            // Setup test fixture: detect a message error that is sent back to the sender.
            final StanzaFilter errorDetector = new AndFilter((s -> s instanceof Message && ((Message) s).getType() == Message.Type.error && ((Message) s).getBody().equals(needle)));
            final SimpleResultSyncPoint errorReceived = new SimpleResultSyncPoint();
            errorListener = (stanza) -> {
                System.err.println("Received error : " + stanza.toXML());
                errorReceived.signal();
            };
            conOne.addStanzaListener(errorListener, errorDetector);

            // Execute system under test.
            final Message messageToBeProcessed = StanzaBuilder.buildMessage()
                .ofType(Message.Type.groupchat)
                .to(conTwo.getUser().asBareJid())
                .setBody(needle)
                .build();

            conOne.sendStanza(messageToBeProcessed);

            // Informs intended recipients that the test is over.
            for (final FullJid recipient : allResources) {
                conOne.sendStanza(StanzaBuilder.buildMessage().setBody(stopNeedle).to(recipient).build());
            }

            try {
                // Wait for all recipients to have received the 'test is over' stanza.
                messagesProcessedSyncPoint.waitForResult(timeout);
            } catch (TimeoutException e) {
                // This is dodgy (concurrency issue? server misbehaving?) but let's not fail the test just yet. After the timeout has expired, it is likely that the test is ready to be evaluated.
                e.printStackTrace();
            }

            // Verify result.
            assertTrue(receivedBy.isEmpty(), "Expected the message of type '" + messageToBeProcessed.getType() + "' that was sent by '" + conOne.getUser() + "' to the bare JID of '" + conTwo.getUser().asBareJid() +"' to NOT have been received by by any resource. Instead the message was received by: [" + receivedBy.keySet().stream().map(Object::toString).sorted().collect(Collectors.joining(", "))+ "]" );
            assertResult(errorReceived, "Expected '" + conOne.getUser() + "' to receive an error message after trying to send a message of type '" + messageToBeProcessed.getType() + "' to the bare JID of '" + conTwo.getUser().asBareJid() +"' (but no error message was received)." );
        } finally {
            // Tear down test fixture.
            if (errorListener != null) { conOne.removeStanzaListener(errorListener); }
            for (int i = 0; i < resourcePriorities.size(); i++) {
                final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i - 1);
                if (stopListener != null) { resourceConnection.removeStanzaListener(stopListener); }
                receivedListeners.forEach(resourceConnection::removeStanzaListener); // Only one of these will match.
            }
            additionalConnections.forEach(AbstractXMPPConnection::disconnect);
            conTwo.sendStanza(PresenceBuilder.buildPresence().ofType(Presence.Type.available).build()); // This intends to mimic the 'initial presence'.
        }
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"headline\": [...] If the only available resource has a negative presence priority then the server MUST silently ignore the stanza.")
    public void testHeadlineMessageOneResourcePrioNegative() throws Exception
    {
        doTestHeadlineMessageSingleResource(-1);
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"headline\": [...] If the only available resource has a non-negative presence priority then the server MUST deliver the message to that resource. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testHeadlineMessageOneResourcePrioZero() throws Exception
    {
        doTestHeadlineMessageSingleResource(0);
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"headline\": [...] If the only available resource has a non-negative presence priority then the server MUST deliver the message to that resource. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testHeadlineMessageOneResourcePrioPositive() throws Exception
    {
        doTestHeadlineMessageSingleResource(1);
    }

    /**
     * Verifies that a message is delivered at the singular resource that has a non-negative priority, or _not_ delivered if that resource has a negative priority.
     * Asserts that a message is not responded to with an error (is silently dropped) when it is _not_ delivered.
     *
     * The steps taken are rather involved:
     * <ol>
     * <li>First, additional resources for conTwo are created, so that this user has as many resources online as the number of priorities provided to this method.
     * <li>For all of these resources, a presence update is sent, to set a particular prio value (from the provided method argument)
     * <li>Then, the message that's the subject of the test is sent to the _bare_ JID of the conTwo user (from the conOne user).
     * <li>Then, a message is sent to the _full_ JID of each of the resources. This is done to avoid having to depend on timeouts.
     * </ol>
     *
     * After receiving the message sent to the full JID, the original message is expected to have been processed (as it
     * was sent later). Assertions can be checked after this message was received.
     */
    public void doTestHeadlineMessageSingleResource(final int resourcePriority) throws Exception
    {
        // Setup test fixture.
        StanzaListener stanzaListener = null;
        StanzaListener errorListener = null;
        StanzaListener stopListener = null;
        try {
            final Message.Type messageType = Message.Type.headline;
            final Presence prioritySet = PresenceBuilder.buildPresence(StringUtils.randomString(9)).setPriority(resourcePriority).build();
            try (final StanzaCollector presenceUpdateDetected = conTwo.createStanzaCollectorAndSend(new OrFilter(new StanzaIdFilter(prioritySet), new AndFilter(new FromMatchesFilter(conTwo.getUser(), false), (s -> s instanceof Presence && ((Presence) s).getPriority() == resourcePriority))), prioritySet)) {
                conTwo.sendStanza(prioritySet);
                presenceUpdateDetected.nextResult(); // Wait for echo, to be sure that presence update was processed by the server.
            }

            // Setup test fixture: prepare for the message that is sent to the bare JID to be sent, and collected while being received be resources of the intended target.
            final String needle = StringUtils.randomString(9);
            final StanzaFilter needleDetector = new AndFilter(FromMatchesFilter.createFull(conOne.getUser()), (s -> s instanceof Message && ((Message) s).getType() == messageType && ((Message) s).getBody().equals(needle)));
            final Map<EntityFullJid, Stanza> receivedBy = new ConcurrentHashMap<>(); // This is what will be evaluated by this test's assertions.
            stanzaListener = (stanza) -> receivedBy.put(conTwo.getUser(), stanza);
            conTwo.addStanzaListener(stanzaListener, needleDetector);

            // Setup test fixture: detect a message error that is sent back to the sender.
            final StanzaFilter errorDetector = new AndFilter((s -> s instanceof Message && ((Message) s).getType() == Message.Type.error && ((Message) s).getBody().equals(needle)));
            final AtomicBoolean errorReceived = new AtomicBoolean(false);
            errorListener = (stanza) -> errorReceived.set(true);
            conOne.addStanzaListener(errorListener, errorDetector);

            // Setup test fixture: detect the message that's sent to signal that the test message has been sent and assertion can thus start.
            final SimpleResultSyncPoint messagesProcessedSyncPoint = new SimpleResultSyncPoint();
            stopListener = packet -> messagesProcessedSyncPoint.signal();
            final String stopNeedle = "STOP LISTENING, STANZAS HAVE BEEN PROCESSED " + StringUtils.randomString(7);
            final StanzaFilter stopDetector = new AndFilter(FromMatchesFilter.createFull(conOne.getUser()), (s -> s instanceof Message && ((Message) s).getBody().equals(stopNeedle)));

            conTwo.addStanzaListener(stopListener, stopDetector);

            // Execute system under test.
            final Message messageToBeProcessed = StanzaBuilder.buildMessage()
                .ofType(messageType)
                .to(conTwo.getUser().asBareJid())
                .setBody(needle)
                .build();

            conOne.sendStanza(messageToBeProcessed);

            // Informs intended recipients that the test is over.
            conOne.sendStanza(StanzaBuilder.buildMessage().setBody(stopNeedle).to(conTwo.getUser()).build());

            // Wait for the recipient to have received the 'test is over' stanza.
            try {
                messagesProcessedSyncPoint.waitForResult(timeout);
            } catch (TimeoutException e) {
                // This is dodgy (concurrency issue? server misbehaving?) but let's not fail the test just yet. After the timeout has expired, it is likely that the test is ready to be evaluated.
                e.printStackTrace();
            }

            // Verify results.
            if (resourcePriority < 0) {
                // Assertions for negative priority.
                assertFalse(errorReceived.get(), "After '" + conOne.getUser() + "' sent a message of type '" + messageToBeProcessed.getType() + "' to the bare JID of '" + messageToBeProcessed.getTo() + "', a user that had only one resource online that had a presence priority of " + resourcePriority + ", it was expected that the server would silently ignore the stanza. Instead, '" + conOne.getUser() + "' received an error.");
                assertTrue(receivedBy.isEmpty(), "After '" + conOne.getUser() + "' sent a message of type '" + messageToBeProcessed.getType() + "' to the bare JID of '" + messageToBeProcessed.getTo() + "', a user that had only one resource online that had a presence priority of " + resourcePriority + ", it was expected that the server would silently ignore the stanza. Instead, the stanza was received by '" + receivedBy.keySet().stream().map(Object::toString).sorted().collect(Collectors.joining(", ")) + "'.");
            } else {
                // Assertions for positive priority.
                assertTrue(receivedBy.containsKey(conTwo.getUser()), "After '" + conOne.getUser() + "' sent a message of type '" + messageToBeProcessed.getType() + "' to the bare JID of '" + messageToBeProcessed.getTo() + "', a user that had only one resource online that had a presence priority of " + resourcePriority + ", it was expected that the server would deliver the stanza to that resource. Instead, the stanza was not received by '" + conTwo.getUser() + "'.");

                final Map<EntityFullJid, Jid> invalidAddressees = receivedBy.entrySet().stream().filter((entry) -> !entry.getValue().getTo().equals(messageToBeProcessed.getTo())).collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getTo()));
                final String errorMessage = invalidAddressees.entrySet().stream().map(entry -> "resource '" + entry.getKey() + "' received a stanza addressed to '" + entry.getValue() + "'").collect(Collectors.joining(", "));
                assertTrue(invalidAddressees.isEmpty(), "Expected the 'to' attribute of the message sent by '" + conOne.getUser() + "' to remain unchanged ('" + messageToBeProcessed.getTo() + "'). Instead, these resources received attribute values that were modified: " + errorMessage + ".");
            }
        } finally {
            // Tear down test fixture.
            conOne.removeStanzaListener(errorListener);
            conTwo.removeStanzaListener(stanzaListener);
            conTwo.removeStanzaListener(stopListener);
            conTwo.sendStanza(PresenceBuilder.buildPresence().ofType(Presence.Type.available).build()); // This intends to mimic the 'initial presence'.
        }
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"headline\": [...] If there is more than one resource with a non-negative presence priority then the server MUST deliver the message to all of the non-negative resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testHeadlineMessageMultipleResourcesPrioPositive() throws Exception
    {
        doTestMessageHeadline(List.of(1,1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"headline\": [...] If there is more than one resource with a non-negative presence priority then the server MUST deliver the message to all of the non-negative resources. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testHeadlineMessageMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestMessageHeadline(List.of(1,1,-1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"headline\": [...] If there is more than one resource with a non-negative presence priority then the server MUST deliver the message to all of the non-negative resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testHeadlineMessageMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestMessageHeadline(List.of(3,1,2));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"headline\": [...] If there is more than one resource with a non-negative presence priority then the server MUST deliver the message to all of the non-negative resources. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testHeadlineMessageMultipleResourcesPrioDifferentPositiveAndNegative() throws Exception
    {
        doTestMessageHeadline(List.of(1,2,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"headline\": [...] If there is more than one resource with a non-negative presence priority then the server MUST deliver the message to all of the non-negative resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testHeadlineMessageMultipleResourcesPrioZero() throws Exception
    {
        doTestMessageHeadline(List.of(0,0,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"headline\": [...] If there is more than one resource with a non-negative presence priority then the server MUST deliver the message to all of the non-negative resources. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testHeadlineMessageMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestMessageHeadline(List.of(0,0,-1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"headline\": [...] If there is more than one resource with a non-negative presence priority then the server MUST deliver the message to all of the non-negative resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testHeadlineMessageMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestMessageHeadline(List.of(3,1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"headline\": [...] If there is more than one resource with a non-negative presence priority then the server MUST deliver the message to all of the non-negative resources. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testHeadlineMessageMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestMessageHeadline(List.of(1,0,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority")
    public void testHeadlineMessageMultipleResourcesPrioNegative() throws Exception
    {
        doTestMessageHeadline(List.of(-1,-1,-1));
    }

    /**
     * Verifies that a message is delivered at _all_ resources with non-negative priorities.
     * When negative priorities are provided, the test asserts that the message has _not_ been delivered to the corresponding
     * resource.
     *
     * The steps taken are rather involved:
     * <ol>
     * <li>First, additional resources for conTwo are created, so that this user has as many resources online as the number of priorities provided to this method.
     * <li>For all of these resources, a presence update is sent, to set a particular prio value (from the provided method argument)
     * <li>Then, the message that's the subject of the test is sent to the _bare_ JID of the conTwo user (from the conOne user).
     * <li>Then, a message is sent to the _full_ JID of each of the resources. This is done to avoid having to depend on timeouts.
     * </ol>
     *
     * After receiving the message sent to the full JID, the original message is expected to have been processed (as it
     * was sent later). Assertions can be checked after this message was received.
     */
    public void doTestMessageHeadline(final List<Integer> resourcePriorities) throws Exception
    {
        if (resourcePriorities.isEmpty()) {
            throw new IllegalArgumentException("The resource priorities must contain at least one element.");
        }


        // Setup test fixture.
        final List<AbstractXMPPConnection> additionalConnections = new ArrayList<>(resourcePriorities.size()-1);
        for (int i = 0; i < resourcePriorities.size()-1; i++) {
            additionalConnections.add(environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration));
        }

        final Set<FullJid> allResources = new HashSet<>();
        final Set<EntityFullJid> allNonNegativeResources = new HashSet<>();
        StanzaListener stopListener = null;
        final Collection<StanzaListener> receivedListeners = new HashSet<>();
        try {
            // Setup test fixture: create connections for the additional resources (based on the user used for 'conTwo').
            for (final AbstractXMPPConnection additionalConnection : additionalConnections) {
                additionalConnection.connect();
                additionalConnection.login(((AbstractXMPPConnection) conTwo).getConfiguration().getUsername(), ((AbstractXMPPConnection) conTwo).getConfiguration().getPassword(), Resourcepart.from(StringUtils.randomString(7)));
            }

            // Setup test fixture: configure the desired resource priority for each of the resource connections.
            for (int i = 0; i < resourcePriorities.size(); i++) {
                final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i-1);
                final int resourcePriority = resourcePriorities.get(i);

                final Presence prioritySet = PresenceBuilder.buildPresence(StringUtils.randomString(9)).setPriority(resourcePriority).build();
                try (final StanzaCollector presenceUpdateDetected = resourceConnection.createStanzaCollectorAndSend(new OrFilter(new StanzaIdFilter(prioritySet), new AndFilter(new FromMatchesFilter(resourceConnection.getUser(), false), (s -> s instanceof Presence && ((Presence) s).getPriority() == resourcePriority))), prioritySet)) {
                    resourceConnection.sendStanza(prioritySet);
                    presenceUpdateDetected.nextResult(); // Wait for echo, to be sure that presence update was processed by the server.
                }

                allResources.add(resourceConnection.getUser());
                if (resourcePriority >= 0) {
                    allNonNegativeResources.add(resourceConnection.getUser());
                }
            }

            // Setup test fixture: prepare for the message that is sent to the bare JID to be sent, and collected while being received by the various resources.
            final String needle = StringUtils.randomString(9);
            final StanzaFilter needleDetector = new AndFilter(FromMatchesFilter.createFull(conOne.getUser()), (s -> s instanceof Message && ((Message) s).getType() == Message.Type.headline && ((Message) s).getBody().equals(needle)));
            final Map<EntityFullJid, Stanza> receivedBy = new ConcurrentHashMap<>(); // This is what will be evaluated by this test's assertions.

            // Setup test fixture: detect the message that's sent to signal that the test message has been sent and assertion can thus start.
            final SimpleResultSyncPoint messagesProcessedSyncPoint = new SimpleResultSyncPoint();
            stopListener = new StanzaListener() {
                final Set<Jid> recipients = new HashSet<>(allResources);

                @Override
                public void processStanza(Stanza packet) {
                    recipients.remove(packet.getTo());
                    if (recipients.isEmpty()) { // When having received a 'stop' on all resources, the test is ready to evaluate assertions.
                        messagesProcessedSyncPoint.signal();
                    }
                }
            };
            final String stopNeedle = "STOP LISTENING, STANZAS HAVE BEEN PROCESSED " + StringUtils.randomString(7);
            final StanzaFilter stopDetector = new AndFilter(FromMatchesFilter.createFull(conOne.getUser()), (s -> s instanceof Message && ((Message) s).getBody().equals(stopNeedle)));

            for (int i = 0; i < resourcePriorities.size(); i++) {
                final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i - 1);
                final StanzaListener stanzaListener = (stanza) -> receivedBy.put(resourceConnection.getUser(), stanza);
                receivedListeners.add(stanzaListener); // keep track so that the listener can be removed again.
                resourceConnection.addStanzaListener(stanzaListener, needleDetector);
                resourceConnection.addStanzaListener(stopListener, stopDetector);
            }

            // Execute system under test.
            final Message messageToBeProcessed = StanzaBuilder.buildMessage()
                .ofType(Message.Type.headline)
                .to(conTwo.getUser().asBareJid())
                .setBody(needle)
                .build();

            conOne.sendStanza(messageToBeProcessed);

            // Informs intended recipients that the test is over.
            for (final FullJid recipient : allResources) {
                conOne.sendStanza(StanzaBuilder.buildMessage().setBody(stopNeedle).to(recipient).build());
            }

            try {
                // Wait for all recipients to have received the 'test is over' stanza.
                messagesProcessedSyncPoint.waitForResult(timeout);
            } catch (TimeoutException e) {
                // This is dodgy (concurrency issue? server misbehaving?) but let's not fail the test just yet. After the timeout has expired, it is likely that the test is ready to be evaluated.
                e.printStackTrace();
            }

            // Verify result.
            final Set<EntityFullJid> missing = new HashSet<>(allNonNegativeResources);
            receivedBy.keySet().forEach(missing::remove);
            final Set<EntityFullJid> negativeRecipients = receivedBy.keySet().stream().filter(o -> allResources.contains(o) && !allNonNegativeResources.contains(o)).collect(Collectors.toSet());

            assertTrue(missing.isEmpty(), "Expected the message of type '" + messageToBeProcessed.getType() + "' that was sent by '" + conOne.getUser() + "' to the bare JID of '" + conTwo.getUser().asBareJid() + "' to have been received by all resources of that user that have a non-negative presence priority. However, it was not received by [" + missing.stream().map(Object::toString).sorted().collect(Collectors.joining(", ")) + "]");
            assertTrue(negativeRecipients.isEmpty(), "Expected the message of type '" + messageToBeProcessed.getType() + "' that was sent by '" + conOne.getUser() + "' to the bare JID of '" + conTwo.getUser().asBareJid() +"' to NOT have been received by resources that have a negative priority. Instead, it was received by this/these resource(s) that had a negative priority: [" + negativeRecipients.stream().map(Object::toString).sorted().collect(Collectors.joining(", ")) + "].");

            final Map<EntityFullJid, Jid> invalidAddressees = receivedBy.entrySet().stream().filter((entry) -> !entry.getValue().getTo().equals(messageToBeProcessed.getTo())).collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getTo()));
            final String errorMessage = invalidAddressees.entrySet().stream().map(entry -> "resource '" + entry.getKey() + "' received a stanza addressed to '" + entry.getValue() + "'").collect(Collectors.joining(", "));
            assertTrue(invalidAddressees.isEmpty(), "Expected the 'to' attribute of the message sent by '" + conOne.getUser() + "' to remain unchanged ('" + messageToBeProcessed.getTo() + "'). Instead, these resources received attribute values that were modified: " + errorMessage + ".");
        } finally {
            // Tear down test fixture.
            for (int i = 0; i < resourcePriorities.size(); i++) {
                final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i - 1);
                if (stopListener != null) { resourceConnection.removeStanzaListener(stopListener); }
                receivedListeners.forEach(resourceConnection::removeStanzaListener); // Only one of these will match.
            }

            additionalConnections.forEach(AbstractXMPPConnection::disconnect);
            conTwo.sendStanza(PresenceBuilder.buildPresence().ofType(Presence.Type.available).build()); // This intends to mimic the 'initial presence'.
        }
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the message. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testErrorMessageOneResourcePrioPositive() throws Exception
    {
        doTestMessageError(List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the message. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testErrorMessageOneResourcePrioZero() throws Exception
    {
        doTestMessageError(List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority")
    public void testErrorMessageOneResourcePrioNegative() throws Exception
    {
        doTestMessageError(List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the message. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testErrorMessageMultipleResourcesPrioPositive() throws Exception
    {
        doTestMessageError(List.of(1,1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the message. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testErrorMessageMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestMessageError(List.of(1,1,-1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the message. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testErrorMessageMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestMessageError(List.of(3,1,2));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the message. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testErrorMessageMultipleResourcesPrioDifferentPositiveAndNegative() throws Exception
    {
        doTestMessageError(List.of(1,2,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the message. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testErrorMessageMultipleResourcesPrioZero() throws Exception
    {
        doTestMessageError(List.of(0,0,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the message. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testErrorMessageMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestMessageError(List.of(0,0,-1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the message. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testErrorMessageMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestMessageError(List.of(3,1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the message. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testErrorMessageMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestMessageError(List.of(1,0,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority")
    public void testErrorMessageMultipleResourcesPrioNegative() throws Exception
    {
        doTestMessageError(List.of(-1,-1,-1));
    }

    /**
     * Verifies that a message is not delivered and an error is NOT returned (the message is silently dropped).
     *
     * The steps taken are rather involved:
     *
     * <ol>
     * <li>First, additional resources for conTwo are created, so that this user has as many resources online as the number of priorities provided to this method.
     * <li>For all of these resources, a presence update is sent, to set a particular prio value (from the provided method argument)
     * <li>Then, the message that's the subject of the test is sent to the _bare_ JID of the conTwo user (from the conOne user).
     * <li>Then, a message is sent to the _full_ JID of each of the resources. This is done to avoid having to depend on timeouts.
     * </ol>
     *
     * After receiving the message sent to the full JID, the original message is expected to have been processed (as it
     * was sent later). Assertions can be checked after this message was received.
     */
    public void doTestMessageError(final List<Integer> resourcePriorities) throws Exception
    {
        if (resourcePriorities.isEmpty()) {
            throw new IllegalArgumentException("The resource priorities must contain at least one element.");
        }

        final Set<FullJid> allResources = new HashSet<>();

        // Setup test fixture.
        final List<AbstractXMPPConnection> additionalConnections = new ArrayList<>(resourcePriorities.size()-1);
        for (int i = 0; i < resourcePriorities.size()-1; i++) {
            additionalConnections.add(environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration));
        }

        StanzaListener stopListener = null;
        final Collection<StanzaListener> receivedListeners = new HashSet<>();
        StanzaListener errorListener = null;
        try {
            // Setup test fixture: create connections for the additional resources (based on the user used for 'conTwo').
            for (final AbstractXMPPConnection additionalConnection : additionalConnections) {
                additionalConnection.connect();
                additionalConnection.login(((AbstractXMPPConnection) conTwo).getConfiguration().getUsername(), ((AbstractXMPPConnection) conTwo).getConfiguration().getPassword(), Resourcepart.from(StringUtils.randomString(7)));
            }

            // Setup test fixture: configure the desired resource priority for each of the resource connections.
            for (int i = 0; i < resourcePriorities.size(); i++) {
                final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i-1);
                final int resourcePriority = resourcePriorities.get(i);

                final Presence prioritySet = PresenceBuilder.buildPresence(StringUtils.randomString(9)).setPriority(resourcePriority).build();
                try (final StanzaCollector presenceUpdateDetected = resourceConnection.createStanzaCollectorAndSend(new OrFilter(new StanzaIdFilter(prioritySet), new AndFilter(new FromMatchesFilter(resourceConnection.getUser(), false), (s -> s instanceof Presence && ((Presence) s).getPriority() == resourcePriority))), prioritySet)) {
                    resourceConnection.sendStanza(prioritySet);
                    presenceUpdateDetected.nextResult(); // Wait for echo, to be sure that presence update was processed by the server.
                }

                allResources.add(resourceConnection.getUser());
            }

            // Setup test fixture: prepare for the message that is sent to the bare JID to be sent, and collected while being received by the various resources.
            final String needle = StringUtils.randomString(9);
            final StanzaFilter needleDetector = new AndFilter(FromMatchesFilter.createFull(conOne.getUser()), (s -> s instanceof Message && ((Message) s).getType() == Message.Type.error && ((Message) s).getBody().equals(needle)));
            final Map<EntityFullJid, Stanza> receivedBy = new ConcurrentHashMap<>(); // This is what will be evaluated by this test's assertions.

            // Setup test fixture: detect the message that's sent to signal that the test message has been sent and assertion can thus start.
            final SimpleResultSyncPoint messagesProcessedSyncPoint = new SimpleResultSyncPoint();
            stopListener = new StanzaListener() {
                final Set<Jid> recipients = new HashSet<>(allResources);

                @Override
                public void processStanza(Stanza packet) {
                    recipients.remove(packet.getTo());
                    if (recipients.isEmpty()) { // When having received a 'stop' on all resources, the test is ready to evaluate assertions.
                        messagesProcessedSyncPoint.signal();
                    }
                }
            };
            final String stopNeedle = "STOP LISTENING, STANZAS HAVE BEEN PROCESSED " + StringUtils.randomString(7);
            final StanzaFilter stopDetector = new AndFilter(FromMatchesFilter.createFull(conOne.getUser()), (s -> s instanceof Message && ((Message) s).getBody().equals(stopNeedle)));

            for (int i = 0; i < resourcePriorities.size(); i++) {
                final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i - 1);
                final StanzaListener stanzaListener = (stanza) -> receivedBy.put(resourceConnection.getUser(), stanza);
                receivedListeners.add(stanzaListener); // keep track so that the listener can be removed again.
                resourceConnection.addStanzaListener(stanzaListener, needleDetector);
                resourceConnection.addStanzaListener(stopListener, stopDetector);
            }

            // Setup test fixture: detect a message error that is sent back to the sender.
            final StanzaFilter errorDetector = new AndFilter((s -> s instanceof Message && ((Message) s).getType() == Message.Type.error && ((Message) s).getBody().equals(needle)));
            final AtomicBoolean errorReceived = new AtomicBoolean(false);
            errorListener = (stanza) -> errorReceived.set(true);
            conOne.addStanzaListener(errorListener, errorDetector);

            // Execute system under test.
            final Message messageToBeProcessed = StanzaBuilder.buildMessage()
                .ofType(Message.Type.error)
                .to(conTwo.getUser().asBareJid())
                .setBody(needle)
                .build();

            conOne.sendStanza(messageToBeProcessed);

            // Informs intended recipients that the test is over.
            for (final FullJid recipient : allResources) {
                conOne.sendStanza(StanzaBuilder.buildMessage().setBody(stopNeedle).to(recipient).build());
            }

            try {
                // Wait for all recipients to have received the 'test is over' stanza.
                messagesProcessedSyncPoint.waitForResult(timeout);
            } catch (TimeoutException e) {
                // This is dodgy (concurrency issue? server misbehaving?) but let's not fail the test just yet. After the timeout has expired, it is likely that the test is ready to be evaluated.
                e.printStackTrace();
            }

            // Verify result.
            assertTrue(receivedBy.isEmpty(), "Expected the message of type '" + messageToBeProcessed.getType() + "' that was sent by '" + conOne.getUser() + "' to the bare JID of '" + conTwo.getUser().asBareJid() +"' to NOT have been received by by any resource. Instead the message was received by: [" + receivedBy.keySet().stream().map(Object::toString).sorted().collect(Collectors.joining(", "))+ "]" );
            assertFalse(errorReceived.get(), "After '" + conOne.getUser() + "' sent a message of type '" + messageToBeProcessed.getType() + "' to the bare JID of '" + messageToBeProcessed.getTo() + "', it was expected that the server would silently ignore the stanza. Instead, '" + conOne.getUser() + "' received an error.");
        } finally {
            // Tear down test fixture.
            if (errorListener != null) { conOne.removeStanzaListener(errorListener); }
            for (int i = 0; i < resourcePriorities.size(); i++) {
                final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i - 1);
                if (stopListener != null) { resourceConnection.removeStanzaListener(stopListener); }
                receivedListeners.forEach(resourceConnection::removeStanzaListener); // Only one of these will match.
            }
            additionalConnections.forEach(AbstractXMPPConnection::disconnect);
            conTwo.sendStanza(PresenceBuilder.buildPresence().ofType(Presence.Type.available).build()); // This intends to mimic the 'initial presence'.
        }
    }
}
