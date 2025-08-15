package org.igniterealtime.smack.inttest.rfc6121.section2;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.AccountUtilities;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.roster.AbstractRosterListener;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.util.StringUtils;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that verify that behavior defined in section 2.3 "Adding a Roster Item" of section 2 "Managing the Roster" of RFC6121.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
@SpecificationReference(document = "RFC6121")
public class RFC6121Section2_3_AddIntegrationTest extends AbstractSmackIntegrationTest
{
    private final boolean isSendPresence;

    private final SmackIntegrationTestEnvironment environment;

    public RFC6121Section2_3_AddIntegrationTest(SmackIntegrationTestEnvironment environment) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        super(environment);

        this.environment = environment;

        try {
            conOne.sendIqRequestAndWaitForResponse(new RosterPacket());
        } catch (XMPPException.XMPPErrorException e) {
            if (e.getStanzaError().getCondition() == StanzaError.Condition.service_unavailable) {
                throw new TestNotPossibleException("Server does not support the roster namespace."); // This error is defined in RFC6121 Section 2.2
            }
        }

        // Check if the connections used in this test will have sent initial presence when they authenticated.
        isSendPresence = environment.conOne.getConfiguration().isSendPresence();
    }

    @SmackIntegrationTest(section = "2.3.2", quote = "If the server can successfully process the roster set for the new item [...] The server MUST return an IQ stanza of type \"result\" to the connected resource that sent the roster set.")
    public void testRosterSetResult() throws XmppStringprepException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, SmackException.NotLoggedInException
    {
        // Setup test fixture
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );
        final String rosterItemName = "Test User";
        final String rosterItemGroupName = "Test Group";

        final RosterPacket request = new RosterPacket();
        request.setType(IQ.Type.set);
        final RosterPacket.Item item = new RosterPacket.Item(target, rosterItemName);
        item.addGroupName(rosterItemGroupName);
        request.addRosterItem(item);

        // Execute system under test
        try {
            final IQ result = conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result
            assertEquals(IQ.Type.result, result.getType(), "Unexpected response type received by '" + conOne.getUser() + "' after it sent a normal Roster Set.");
        } catch (XMPPException.XMPPErrorException e) {
            fail("Unexpected error response received by '" + conOne.getUser() + "' after it sent a normal Roster Set.");
        } finally {
            // Tear down test fixture
            final RosterEntry entry = Roster.getInstanceFor(conOne).getEntry(target);
            if (entry != null) {
                Roster.getInstanceFor(conOne).removeEntry(entry);
            }
        }
    }

    @SmackIntegrationTest(section = "2.3.2", quote = "After a connected resource sends initial presence [...] it is referred to as an \"available resource\". If a connected resource or available resource requests the roster, it is referred to as an \"interested resource\". [...] The server MUST also send a roster push containing the new roster item to all of the user's interested resources, including the resource that generated the roster set.")
    public void testRosterSetGeneratesPushToInterestedResourceSelfWithInitialPresence() throws XmppStringprepException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, SmackException.NotLoggedInException, TimeoutException
    {
        // Setup test fixture.
        if (!isSendPresence) {
            // Ensure that initial presence is sent.
            conOne.sendStanza(PresenceBuilder.buildPresence().build());
        }
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );
        final String rosterItemName = "Test User";
        final String rosterItemGroupName = "Test Group";

        final Roster rosterOne = Roster.getInstanceFor(conOne); // Ensure that this resource is an 'interested resource' by loading the roster (Smack probably already did this through Roster#rosterLoadedAtLoginDefault, but it doesn't hurt to be certain).

        final SimpleResultSyncPoint receivedPush = new SimpleResultSyncPoint();
        final AbstractRosterListener rosterListener = new AbstractRosterListener()
        {
            @Override
            public void entriesAdded(Collection<Jid> collection)
            {
                if (collection.contains(target)) {
                    receivedPush.signal();
                }
            }
        };
        rosterOne.addRosterListener(rosterListener);

        try {
            // Execute system under test.
            rosterOne.createItem(target, rosterItemName, new String[]{rosterItemGroupName});

            // Verify result.
            assertResult(receivedPush, "Expected '" + conOne.getUser() + "' to receive a roster push after a roster item was added through the same resource, after the connection had earlier send initial presence and obtained the roster, thus making it an 'interested resource'. The roster push was not received.");

            // After the roster push was received, the roster should now contain the updated item, which is inspected below to see if the expected changes
            // are applied. Ideally, we'd inspect the pushed item directly, but Smack doesn't make that easy to do.
            final RosterEntry pushedEntry = rosterOne.getEntry(target);
            if (pushedEntry == null) {
                // This is a bug in the test, not in the system-under-test.
                throw new IllegalStateException("Expected the roster of '" + conOne.getUser() + "' to contain an item for '" + target + "' after having received a roster push (but no such roster item was found)");
            }
            assertEquals(rosterItemName, pushedEntry.getName(), "Unexpected name for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to name to be equal to that what was used in a Roster Set by the same user (but it was not).");
            assertEquals(1, pushedEntry.getGroups().size(), "Unexpected amount of groups for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to amount to be equal to the amount of groups used in the Roster Set by the same user (but it was not).");
            assertEquals(rosterItemGroupName, pushedEntry.getGroups().get(0).getName(), "Unexpected group name for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to group name to be equal to that what was used in a Roster Set by the same user (but it was not).");
        } finally {
            // Tear down test fixture.
            rosterOne.removeRosterListener(rosterListener);
            final RosterEntry entry = rosterOne.getEntry(target);
            if (entry != null) {
                rosterOne.removeEntry(entry);
            }
        }
    }

    @SmackIntegrationTest(section = "2.3.2", quote = "After a connected resource sends initial presence [...] it is referred to as an \"available resource\". If a connected resource or available resource requests the roster, it is referred to as an \"interested resource\". [...] The server MUST also send a roster push containing the new roster item to all of the user's interested resources, including the resource that generated the roster set.")
    public void testRosterSetGeneratesPushToInterestedResourceSelfWithoutInitialPresence() throws XmppStringprepException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, SmackException.NotLoggedInException, TimeoutException, TestNotPossibleException
    {
        if (isSendPresence) {
            throw new TestNotPossibleException("The test implementation requires the connection used for testing to not have sent initial presence (but current configuration causes initial presence to be sent automatically).");
        }

        // Setup test fixture.
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );
        final String rosterItemName = "Test User";
        final String rosterItemGroupName = "Test Group";

        final Roster rosterOne = Roster.getInstanceFor(conOne); // Ensure that this resource is an 'interested resource' by loading the roster (Smack probably already did this through Roster#rosterLoadedAtLoginDefault, but it doesn't hurt to be certain).

        final SimpleResultSyncPoint receivedPush = new SimpleResultSyncPoint();
        final AbstractRosterListener rosterListener = new AbstractRosterListener()
        {
            @Override
            public void entriesAdded(Collection<Jid> collection)
            {
                if (collection.contains(target)) {
                    receivedPush.signal();
                }
            }
        };
        rosterOne.addRosterListener(rosterListener);

        try {
            // Execute system under test.
            rosterOne.createItem(target, rosterItemName, new String[]{rosterItemGroupName});

            // Verify result.
            assertResult(receivedPush, "Expected '" + conOne.getUser() + "' to receive a roster push after a roster item was added through the same resource, after the connection had earlier obtained the roster (without sending initial presence), thus making it an 'interested resource'. The roster push was not received.");

            // After the roster push was received, the roster should now contain the updated item, which is inspected below to see if the expected changes
            // are applied. Ideally, we'd inspect the pushed item directly, but Smack doesn't make that easy to do.
            final RosterEntry pushedEntry = rosterOne.getEntry(target);
            if (pushedEntry == null) {
                // This is a bug in the test, not in the system-under-test.
                throw new IllegalStateException("Expected the roster of '" + conOne.getUser() + "' to contain an item for '" + target + "' after having received a roster push (but no such roster item was found)");
            }
            assertEquals(rosterItemName, pushedEntry.getName(), "Unexpected name for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to name to be equal to that what was used in a Roster Set by the same user (but it was not).");
            assertEquals(1, pushedEntry.getGroups().size(), "Unexpected amount of groups for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to amount to be equal to the amount of groups used in the Roster Set by the same user (but it was not).");
            assertEquals(rosterItemGroupName, pushedEntry.getGroups().get(0).getName(), "Unexpected group name for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to group name to be equal to that what was used in a Roster Set by the same user (but it was not).");
        } finally {
            // Tear down test fixture.
            rosterOne.removeRosterListener(rosterListener);
            final RosterEntry entry = rosterOne.getEntry(target);
            if (entry != null) {
                rosterOne.removeEntry(entry);
            }
        }
    }

    @SmackIntegrationTest(section = "2.3.2", quote = "After a connected resource sends initial presence [...] it is referred to as an \"available resource\". If a connected resource or available resource requests the roster, it is referred to as an \"interested resource\". [...] The server MUST also send a roster push containing the new roster item to all of the user's interested resources, including the resource that generated the roster set.")
    public void testRosterSetGeneratesPushToInterestedResourceOtherResourceWithInitialPresence() throws IOException, XMPPException, SmackException, InterruptedException, TimeoutException, InvocationTargetException, InstantiationException, IllegalAccessException
    {
        final AbstractXMPPConnection conOneSecondary = AccountUtilities.spawnNewConnection(environment, sinttestConfiguration);
        try {
            // Setup test fixture.
            final BareJid target = JidCreate.bareFrom(Localpart.from("test-target-" + StringUtils.randomString(5)), conOneSecondary.getXMPPServiceDomain());
            final String rosterItemName = "Test User";
            final String rosterItemGroupName = "Test Group";

            final Roster rosterOnePrimary = Roster.getInstanceFor(conOne);

            conOneSecondary.connect();
            conOneSecondary.login(((AbstractXMPPConnection)conOne).getConfiguration().getUsername(), ((AbstractXMPPConnection)conOne).getConfiguration().getPassword(), Resourcepart.from(StringUtils.randomString(7)));
            if (!isSendPresence) {
                // Ensure that initial presence is sent.
                conOneSecondary.sendStanza(PresenceBuilder.buildPresence().build());
            }
            final Roster rosterOneSecondary = Roster.getInstanceFor(conOneSecondary); // Ensure that this resource is an 'interested resource' by loading the roster (Smack probably already did this through Roster#rosterLoadedAtLoginDefault but it doesn't hurt to be certain).

            final SimpleResultSyncPoint receivedPush = new SimpleResultSyncPoint();
            final AbstractRosterListener rosterListener = new AbstractRosterListener()
            {
                @Override
                public void entriesAdded(Collection<Jid> collection)
                {
                    if (collection.contains(target)) {
                        receivedPush.signal();
                    }
                }
            };
            rosterOneSecondary.addRosterListener(rosterListener);

            try {
                // Execute system under test.
                rosterOnePrimary.createItem(target, rosterItemName, new String[]{rosterItemGroupName});

                // Verify result.
                assertResult(receivedPush, "Expected '" + conOneSecondary.getUser() + "' to receive a roster push after a roster item was added by a different resource of that user ('" + conOne.getUser() + "'), after the connection had earlier send initial presence and obtained the roster, thus making it an 'interested resource'. The roster push was not received.");

                // After the roster push was received, the roster should now contain the updated item, which is inspected below to see if the expected changes
                // are applied. Ideally, we'd inspect the pushed item directly, but Smack doesn't make that easy to do.
                final RosterEntry pushedEntry = rosterOneSecondary.getEntry(target);
                if (pushedEntry == null) {
                    // This is a bug in the test, not in the system-under-test.
                    throw new IllegalStateException("Expected the roster of '" + conOne.getUser() + "' to contain an item for '" + target + "' after having received a roster push (but no such roster item was found)");
                }
                assertEquals(rosterItemName, pushedEntry.getName(), "Unexpected name for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to name to be equal to that what was used in a Roster Set by a different resource of the same user (but it was not).");
                assertEquals(1, pushedEntry.getGroups().size(), "Unexpected amount of groups for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to amount to be equal to the amount of groups used in the Roster Set by a different resource of the same user (but it was not).");
                assertEquals(rosterItemGroupName, pushedEntry.getGroups().get(0).getName(), "Unexpected group name for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to group name to be equal to that what was used in a Roster Set by a different resource of the same user (but it was not).");
            } finally {
                // Tear down test fixture.
                rosterOneSecondary.removeRosterListener(rosterListener);
                final RosterEntry entry = rosterOnePrimary.getEntry(target);
                if (entry != null) {
                    rosterOnePrimary.removeEntry(entry);
                }
            }
        } finally {
            if (conOneSecondary != null) {
                conOneSecondary.disconnect();
            }
        }
    }

    @SmackIntegrationTest(section = "2.3.2", quote = "After a connected resource sends initial presence [...] it is referred to as an \"available resource\". If a connected resource or available resource requests the roster, it is referred to as an \"interested resource\". [...] The server MUST also send a roster push containing the new roster item to all of the user's interested resources, including the resource that generated the roster set.")
    public void testRosterSetGeneratesPushToInterestedResourceOtherResourceWithoutInitialPresence() throws IOException, XMPPException, SmackException, InterruptedException, TimeoutException, TestNotPossibleException, InvocationTargetException, InstantiationException, IllegalAccessException
    {
        if (isSendPresence) {
            throw new TestNotPossibleException("The test implementation requires the connection used for testing to not have sent initial presence (but current configuration causes initial presence to be sent automatically).");
        }

        final BareJid target = JidCreate.bareFrom(Localpart.from("test-target-" + StringUtils.randomString(5)), conOne.getXMPPServiceDomain());
        final String rosterItemName = "Test User";
        final String rosterItemGroupName = "Test Group";

        final AbstractXMPPConnection conOneSecondary = AccountUtilities.spawnNewConnection(environment, sinttestConfiguration);
        try {
            // Setup test fixture.
            conOneSecondary.connect();
            conOneSecondary.login(((AbstractXMPPConnection)conOne).getConfiguration().getUsername(), ((AbstractXMPPConnection)conOne).getConfiguration().getPassword(), Resourcepart.from(StringUtils.randomString(7)));

            final Roster rosterOnePrimary = Roster.getInstanceFor(conOne);
            final Roster rosterOneSecondary = Roster.getInstanceFor(conOneSecondary); // Ensure that this resource is an 'interested resource' by loading the roster (Smack probably already did this through Roster#rosterLoadedAtLoginDefault but it doesn't hurt to be certain).

            final SimpleResultSyncPoint receivedPush = new SimpleResultSyncPoint();
            final AbstractRosterListener rosterListener = new AbstractRosterListener()
            {
                @Override
                public void entriesAdded(Collection<Jid> collection)
                {
                    if (collection.contains(target)) {
                        receivedPush.signal();
                    }
                }
            };
            rosterOneSecondary.addRosterListener(rosterListener);

            try {
                // Execute system under test.
                rosterOnePrimary.createItem(target, rosterItemName, new String[]{rosterItemGroupName});

                // Verify result.
                assertResult(receivedPush, "Expected '" + conOneSecondary.getUser() + "' to receive a roster push after a roster item was added by a different resource of that user ('" + conOne.getUser() + "'), after the connection had earlier obtained the roster (without sending initial presence), thus making it an 'interested resource'. The roster push was not received.");

                // After the roster push was received, the roster should now contain the updated item, which is inspected below to see if the expected changes
                // are applied. Ideally, we'd inspect the pushed item directly, but Smack doesn't make that easy to do.
                final RosterEntry pushedEntry = rosterOneSecondary.getEntry(target);
                if (pushedEntry == null) {
                    // This is a bug in the test, not in the system-under-test.
                    throw new IllegalStateException("Expected the roster of '" + conOne.getUser() + "' to contain an item for '" + target + "' after having received a roster push (but no such roster item was found)");
                }
                assertEquals(rosterItemName, pushedEntry.getName(), "Unexpected name for the roster item of '" + target + "' on the roster of '" + conOneSecondary.getUser() + "': Expected to name to be equal to that what was used in a Roster Set by a different resource of the same user (but it was not).");
                assertEquals(1, pushedEntry.getGroups().size(), "Unexpected amount of groups for the roster item of '" + target + "' on the roster of '" + conOneSecondary.getUser() + "': Expected to amount to be equal to the amount of groups used in the Roster Set by a different resource of the same user (but it was not).");
                assertEquals(rosterItemGroupName, pushedEntry.getGroups().get(0).getName(), "Unexpected group name for the roster item of '" + target + "' on the roster of '" + conOneSecondary.getUser() + "': Expected to group name to be equal to that what was used in a Roster Set by a different resource of the same user (but it was not).");

            } finally {
                // Tear down test fixture.
                rosterOneSecondary.removeRosterListener(rosterListener);
                final RosterEntry entry = rosterOnePrimary.getEntry(target);
                if (entry != null) {
                    rosterOnePrimary.removeEntry(entry);
                }
            }
        } finally {
            if (conOneSecondary != null) {
                conOneSecondary.disconnect();
            }
        }
    }

    @SmackIntegrationTest(section = "2.3.3", quote = "The server MUST return a <bad-request/> stanza error to the client if the roster set contains any of the following violations: [...] The <query/> element contains more than one <item/> child element.")
    public void testRosterSetMultipleItems() throws XmppStringprepException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, SmackException.NotLoggedInException, InterruptedException
    {
        // Setup test fixture
        final BareJid targetA = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );
        final BareJid targetB = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );

        try {
            final RosterPacket request = new RosterPacket();
            request.setType(IQ.Type.set);
            request.addRosterItem(new RosterPacket.Item(targetA, null));
            request.addRosterItem(new RosterPacket.Item(targetB, null));

            // Execute system under test / verify result
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> conOne.sendIqRequestAndWaitForResponse(request),
                "Expected user '" + conOne.getUser() + "' to receive an error in response to the Roster Set stanza that contained more than one <item/> child element (but the server did not return an error).");
            assertEquals(StanzaError.Condition.bad_request, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conOne.getUser() + "' after it sent a Roster Set that contained more than one <item/> child element.");
        } finally {
            // Tear down test fixture
            final Roster rosterOne = Roster.getInstanceFor(conOne);
            final RosterEntry entryA = rosterOne.getEntry(targetA);
            if (entryA != null) {
                rosterOne.removeEntry(entryA);
            }
            final RosterEntry entryB = rosterOne.getEntry(targetB);
            if (entryB != null) {
                rosterOne.removeEntry(entryB);
            }
        }
    }

    @SmackIntegrationTest(section = "2.3.3", quote = "The server MUST return a <forbidden/> stanza error to the client if the sender of the roster set is not authorized to update the roster.")
    public void testRosterSetNotAuthorized() throws XmppStringprepException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, SmackException.NotLoggedInException, InterruptedException
    {
        // Setup test fixture
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );

        try {
            final RosterPacket request = new RosterPacket();
            request.setType(IQ.Type.set);
            request.addRosterItem(new RosterPacket.Item(target, null));
            request.setTo(conTwo.getUser().asBareJid()); // Address to a _different_ user than the one that will be sending the request.

            // Execute system under test / verify result
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> conOne.sendIqRequestAndWaitForResponse(request),
                "Expected user '" + conOne.getUser() + "' to receive an error in response to the Roster Set stanza that it sent that was addressed to '" + request.getTo() + "' (where it is assumed that '" + conOne.getUser().asBareJid() + "' is not authorized to update the roster of '" + request.getTo().asBareJid() + "') but the server did not return an error.");
            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conOne.getUser() + "' after it sent a Roster Set that was addressed to '" + request.getTo() + "' (where it is assumed that '" + conOne.getUser().asBareJid() + "' is not authorized to update the roster of '" + request.getTo().asBareJid() + "'");
        } finally {
            // Tear down test fixture
            final Roster rosterOne = Roster.getInstanceFor(conOne);
            final RosterEntry entryA = rosterOne.getEntry(target);
            if (entryA != null) {
                rosterOne.removeEntry(entryA);
            }
            final Roster rosterTwo = Roster.getInstanceFor(conTwo);
            final RosterEntry entryB = rosterTwo.getEntry(target);
            if (entryB != null) {
                rosterTwo.removeEntry(entryB);
            }
        }
    }

    @SmackIntegrationTest(section = "2.3.3", quote = "The server MUST return a <bad-request/> stanza error to the client if the roster set contains any of the following violations: [...] The <item/> element contains more than one <group/> element, but there are duplicate groups")
    public void testRosterSetDuplicateGroups() throws XmppStringprepException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, SmackException.NotLoggedInException, InterruptedException
    {
        // Setup test fixture
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );

        final SimpleIQ request = new SimpleIQ(RosterPacket.ELEMENT, RosterPacket.NAMESPACE) { // Smack's RosterPacket doesn't allow us to set the same group name twice.
            @Override
            protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
                xml.rightAngleBracket();

                xml.halfOpenElement(RosterPacket.Item.ELEMENT);
                xml.attribute("jid", target);
                xml.rightAngleBracket();
                xml.element(RosterPacket.Item.GROUP, "invalid-duplicate-group");
                xml.element(RosterPacket.Item.GROUP, "invalid-duplicate-group");
                xml.closeElement(RosterPacket.Item.ELEMENT);
                return xml;
            }
        };
        request.setType(IQ.Type.set);

        try {
            // Execute system under test / verify result
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> conOne.sendIqRequestAndWaitForResponse(request),
                "Expected user '" + conOne.getUser() + "' to receive an error in response to the Roster Set stanza that contained an <item/> child element that has two groups with an identical name (but the server did not return an error).");
            assertEquals(StanzaError.Condition.bad_request, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conOne.getUser() + "' after it sent a Roster Set that contained an <item/> child element that has two groups with an identical name.");
        } finally {
            // Tear down test fixture
            final Roster roster = Roster.getInstanceFor(conOne);
            final RosterEntry entry = roster.getEntry(target);
            if (entry != null) {
                roster.removeEntry(entry);
            }
        }
    }

    @SmackIntegrationTest(section = "2.3.3", quote = "The server MUST return a <not-acceptable/> stanza error to the client if the roster set contains any of the following violations: [...] The XML character data of the <group/> element is of zero length")
    public void testRosterSetZeroLengthGroup() throws XmppStringprepException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, SmackException.NotLoggedInException, InterruptedException
    {
        // Setup test fixture
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );

        final RosterPacket request = new RosterPacket();
        request.setType(IQ.Type.set);
        final RosterPacket.Item item = new RosterPacket.Item(target, null);
        item.addGroupName("");
        request.addRosterItem(item);

        try {
            // Execute system under test / verify result
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> conOne.sendIqRequestAndWaitForResponse(request),
                "Expected user '" + conOne.getUser() + "' to receive an error in response to the Roster Set stanza that contained an empty group element (but the server did not return an error).");
            assertEquals(StanzaError.Condition.not_acceptable, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conOne.getUser() + "' after it sent a Roster Set that contained an empty group element.");
        } finally {
            // Tear down test fixture
            final Roster roster = Roster.getInstanceFor(conOne);
            final RosterEntry entry = roster.getEntry(target);
            if (entry != null) {
                roster.removeEntry(entry);
            }
        }
    }
}
