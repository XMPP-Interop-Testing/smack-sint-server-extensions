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
package org.igniterealtime.smack.inttest.rfc6121.section8;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.MarkerExtension;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.igniterealtime.smack.inttest.xep0421.provider.OccupantId;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Resourcepart;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that verify that behavior defined in section 8.5.2.1.2 "Local User / localpart@domainpart / Available or Connected Resources / Presence" of section 8 "Server Rules for Processing XML Stanzas" of RFC6121.
 *
 * Note: this <em>only</em> tests the processing of presence stanzas with no type or of type 'unavailable'. All other types are subject of tests under other sections (Section 3 and Section 4.3), to which section 8.5.2.1.2 references (and which have a much more detailed definition of the desired functionality).
 *
 * The specification mentions both 'Available' and 'Connected' resources in its titles, but it does not seem to have any specifications for Connected Resources, apart from a more global description in RFC 6120 (section 10.5).
 * There, the conditions described are mostly based on 'SHOULD' keywords. As such, this implementation tests mostly scenarios with 'Available' resources, and not 'Connected' ones. 
 * See https://mail.jabber.org/hyperkitty/list/standards@xmpp.org/thread/L2JTVXQVXW4EQGFM56H5HHJBU6HVPVXK/
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
@SpecificationReference(document = "RFC6121")
public class RFC6121Section8_5_2_1_2_PresenceIntegrationTest extends AbstractSmackIntegrationTest
{
    private final SmackIntegrationTestEnvironment environment;

    public RFC6121Section8_5_2_1_2_PresenceIntegrationTest(SmackIntegrationTestEnvironment environment)
    {
        super(environment);
        this.environment = environment;

        ProviderManager.addExtensionProvider(MarkerExtension.ELEMENT_NAME, MarkerExtension.NAMESPACE, new MarkerExtension.Provider());
    }

    @SmackIntegrationTest(section = "8.5.2.1.2", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a presence stanza with no type [...], the server MUST deliver it to all available resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testAvailableOneResourcePrioPositive() throws Exception
    {
        doTestPresenceAvailableOrUnavailable(Presence.Type.available, List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.2", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a presence stanza with no type [...], the server MUST deliver it to all available resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testAvailableOneResourcePrioZero() throws Exception
    {
        doTestPresenceAvailableOrUnavailable(Presence.Type.available, List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.2", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a presence stanza with no type [...], the server MUST deliver it to all available resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testAvailableOneResourcePrioNegative() throws Exception
    {
        doTestPresenceAvailableOrUnavailable(Presence.Type.available, List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.2", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a presence stanza with no type [...], the server MUST deliver it to all available resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testAvailableMultipleResourcesPrioPositive() throws Exception
    {
        doTestPresenceAvailableOrUnavailable(Presence.Type.available, List.of(1,1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.2", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a presence stanza with no type [...], the server MUST deliver it to all available resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testAvailableMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestPresenceAvailableOrUnavailable(Presence.Type.available, List.of(1,1,-1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.2", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a presence stanza with no type [...], the server MUST deliver it to all available resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testAvailableMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestPresenceAvailableOrUnavailable(Presence.Type.available, List.of(3,1,2));
    }

    @SmackIntegrationTest(section = "8.5.2.1.2", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a presence stanza with no type [...], the server MUST deliver it to all available resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testAvailableMultipleResourcesPrioDifferentPositiveAndNegative() throws Exception
    {
        doTestPresenceAvailableOrUnavailable(Presence.Type.available, List.of(1,2,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.2", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a presence stanza with no type [...], the server MUST deliver it to all available resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testAvailableMultipleResourcesPrioZero() throws Exception
    {
        doTestPresenceAvailableOrUnavailable(Presence.Type.available, List.of(0,0,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.2", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a presence stanza with no type [...], the server MUST deliver it to all available resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testAvailableMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestPresenceAvailableOrUnavailable(Presence.Type.available, List.of(0,0,-1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.2", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a presence stanza with no type [...], the server MUST deliver it to all available resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testAvailableMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestPresenceAvailableOrUnavailable(Presence.Type.available, List.of(3,1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.2", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a presence stanza with no type [...], the server MUST deliver it to all available resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testAvailableMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestPresenceAvailableOrUnavailable(Presence.Type.available, List.of(1,0,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.2", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a presence stanza with no type [...], the server MUST deliver it to all available resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testAvailableMultipleResourcesPrioNegative() throws Exception
    {
        doTestPresenceAvailableOrUnavailable(Presence.Type.available, List.of(-1,-1,-1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.2", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a presence stanza with no type [...], the server MUST deliver it to all available resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testUnavailableOneResourcePrioPositive() throws Exception
    {
        doTestPresenceAvailableOrUnavailable(Presence.Type.unavailable, List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.2", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a presence stanza with no type [...], the server MUST deliver it to all available resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testUnavailableOneResourcePrioZero() throws Exception
    {
        doTestPresenceAvailableOrUnavailable(Presence.Type.unavailable, List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.2", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a presence stanza with no type [...], the server MUST deliver it to all available resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testUnavailableOneResourcePrioNegative() throws Exception
    {
        doTestPresenceAvailableOrUnavailable(Presence.Type.unavailable, List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.2", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a presence stanza with no type [...], the server MUST deliver it to all available resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testUnavailableMultipleResourcesPrioPositive() throws Exception
    {
        doTestPresenceAvailableOrUnavailable(Presence.Type.unavailable, List.of(1,1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.2", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a presence stanza with no type [...], the server MUST deliver it to all available resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testUnavailableMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestPresenceAvailableOrUnavailable(Presence.Type.unavailable, List.of(1,1,-1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.2", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a presence stanza with no type [...], the server MUST deliver it to all available resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testUnavailableMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestPresenceAvailableOrUnavailable(Presence.Type.unavailable, List.of(3,1,2));
    }

    @SmackIntegrationTest(section = "8.5.2.1.2", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a presence stanza with no type [...], the server MUST deliver it to all available resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testUnavailableMultipleResourcesPrioDifferentPositiveAndNegative() throws Exception
    {
        doTestPresenceAvailableOrUnavailable(Presence.Type.unavailable, List.of(1,2,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.2", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a presence stanza with no type [...], the server MUST deliver it to all available resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testUnavailableMultipleResourcesPrioZero() throws Exception
    {
        doTestPresenceAvailableOrUnavailable(Presence.Type.unavailable, List.of(0,0,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.2", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a presence stanza with no type [...], the server MUST deliver it to all available resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testUnavailableMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestPresenceAvailableOrUnavailable(Presence.Type.unavailable, List.of(0,0,-1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.2", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a presence stanza with no type [...], the server MUST deliver it to all available resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testUnavailableMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestPresenceAvailableOrUnavailable(Presence.Type.unavailable, List.of(3,1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.2", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a presence stanza with no type [...], the server MUST deliver it to all available resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testUnavailableMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestPresenceAvailableOrUnavailable(Presence.Type.unavailable, List.of(1,0,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.2", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a presence stanza with no type [...], the server MUST deliver it to all available resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testUnavailableMultipleResourcesPrioNegative() throws Exception
    {
        doTestPresenceAvailableOrUnavailable(Presence.Type.unavailable, List.of(-1,-1,-1));
    }

    /**
     * Executes a test in which conOne sends a presence stanza of no type (Smack uses the explicit type 'available' to
     * represent this) or the 'unavailable' type to the bare JID of conTwo. conTwo has a number of resources online that
     * matches the amount of entries in the provided list.
     *
     * Verifies that a presence stanza is delivered to _all_ (available) resources.
     *
     * This method manages (sets up, executes and tears down) a test fixture in which conOne sends a presence stanza (of
     * a type determined by a parameter) to the bare JID of conTwo, after having logged in a number of resources for conTwo.
     *
     * The amount of resources logged in for conTwo is equal to the number of resourcePriorities provided in the second
     * argument to this method (the test suite always guarantees one pre-existing connection for conTwo, this method
     * will create additional connections as needed). Each of the conTwo connection will have its presence state updated
     * with a priority value taken from the second argument to this method.
     *
     * The steps taken to set up the test fixture and execute the test, in detail:
     * <ol>
     * <li>
     *     First, additional resources for conTwo are created, so that this user has as many resources online as the
     *     number of priorities provided to this method.
     * <li>
     *     For all of these resources, a presence update is sent, to set a particular prio value (from the provided
     *     method argument)
     * <li>
     *     Then, the stanza that's the subject of the test is sent to the _bare_ JID of the conTwo user (from the
     *     conOne user).
     * </ol>
     *
     * After a test fixture has been created, and the stanza that is the subject of this test has been sent (and should
     * have been processed by the server), verification of the test result will occur.
     *
     * Finally, the test fixture is torn down. This involves resetting state, releasing all event listeners, and
     * shutting down any conTwo connections that were created as part of the test fixture setup.
     *
     * @param presenceType the type of presence stanza that is the subject of the test. The presence stanza will be sent by conOne to the bare JID of conTwo.
     * @param resourcePriorities The presence priority values of each of the resources of conTwo that will be online during the test.
     */
    public void doTestPresenceAvailableOrUnavailable(final Presence.Type presenceType, final List<Integer> resourcePriorities) throws Exception
    {
        if (resourcePriorities.isEmpty()) {
            throw new IllegalArgumentException("The resource priorities must contain at least one element.");
        }

        // Setup test fixture.
        final List<AbstractXMPPConnection> additionalConnections = new ArrayList<>(resourcePriorities.size()-1);
        for (int i = 0; i < resourcePriorities.size()-1; i++) {
            additionalConnections.add(environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration));
        }

        final Set<EntityFullJid> allResources = new HashSet<>();
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
                try (final StanzaCollector presenceUpdateDetected = resourceConnection.createStanzaCollectorAndSend(new OrFilter(new StanzaIdFilter(prioritySet), new AndFilter(FromMatchesFilter.createFull(resourceConnection.getUser()), (s -> s instanceof Presence && ((Presence) s).getPriority() == resourcePriority))), prioritySet)) {
                    resourceConnection.sendStanza(prioritySet);
                    presenceUpdateDetected.nextResult(); // Wait for echo, to be sure that presence update was processed by the server.
                }

                allResources.add(resourceConnection.getUser());
            }

            // Setup test fixture: prepare for the stanza that is sent to the bare JID to be sent, and collected while being received by the various resources.
            final String needle = StringUtils.randomString(9);
            final StanzaFilter needleDetector = new AndFilter(FromMatchesFilter.createFull(conOne.getUser()), new ExtensionElementFilter<>(MarkerExtension.class), (s -> s instanceof Presence && ((Presence) s).getType() == presenceType && s.getExtension(MarkerExtension.class).getValue().equals(needle)));
            final Map<EntityFullJid, Stanza> receivedBy = new ConcurrentHashMap<>(); // This is what will be evaluated by this test's assertions.
            final SimpleResultSyncPoint receivedOnAllResources = new SimpleResultSyncPoint();

            for (int i = 0; i < resourcePriorities.size(); i++) {
                final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i - 1);
                final StanzaListener stanzaListener = (stanza) -> {
                    receivedBy.put(resourceConnection.getUser(), stanza);
                    if (receivedBy.keySet().containsAll(allResources)) {
                        receivedOnAllResources.signal();
                    }
                };
                receivedListeners.add(stanzaListener); // keep track so that the listener can be removed again.
                resourceConnection.addStanzaListener(stanzaListener, needleDetector);
            }

            // Execute system under test.
            final Presence testStanza = StanzaBuilder.buildPresence()
                .ofType(presenceType)
                .to(conTwo.getUser().asBareJid())
                .addExtension(new MarkerExtension(needle))
                .build();

            conOne.sendStanza(testStanza);

            try {
                // Wait for all recipients to have received the test stanza.
                receivedOnAllResources.waitForResult(timeout);
            } catch (TimeoutException e) {
                // This is dodgy (concurrency issue? server misbehaving?) but let's not fail the test just yet. After the timeout has expired, it is likely that the test is ready to be evaluated.
                e.printStackTrace();
            }

            // Verify result.
            final Set<FullJid> missing = new HashSet<>(allResources);
            receivedBy.keySet().forEach(missing::remove);

            assertTrue(missing.isEmpty(), "Expected the presence stanza of type '" + testStanza.getType() + "' that was sent by '" + conOne.getUser() + "' to the bare JID of '" + conTwo.getUser().asBareJid() + "' to have been received by all available resources of that user. However, it was not received by [" + missing.stream().map(Object::toString).sorted().collect(Collectors.joining(", ")) + "]");

            final Map<EntityFullJid, Jid> invalidAddressees = receivedBy.entrySet().stream().filter((entry) -> !entry.getValue().getTo().equals(testStanza.getTo())).collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getTo()));
            final String errorMessage = invalidAddressees.entrySet().stream().map(entry -> "resource '" + entry.getKey() + "' received a stanza addressed to '" + entry.getValue() + "'").collect(Collectors.joining(", "));
            assertTrue(invalidAddressees.isEmpty(), "Expected the 'to' attribute of the presence stanza sent by '" + conOne.getUser() + "' to remain unchanged ('" + testStanza.getTo() + "'). Instead, these resources received attribute values that were modified: " + errorMessage + ".");
        } finally {
            // Tear down test fixture.
            for (int i = 0; i < resourcePriorities.size(); i++) {
                final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i - 1);
                receivedListeners.forEach(resourceConnection::removeStanzaListener); // Only one of these will match.
            }
            additionalConnections.forEach(AbstractXMPPConnection::disconnect);
            conTwo.sendStanza(PresenceBuilder.buildPresence().ofType(Presence.Type.available).build()); // This intends to mimic the 'initial presence'.
            conOne.sendStanza(PresenceBuilder.buildPresence().ofType(Presence.Type.available).build()); // As this test sends out presence stanzas from conOne, let's also 'reset' that.
        }
    }
}
