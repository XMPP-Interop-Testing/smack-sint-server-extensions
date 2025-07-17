package org.igniterealtime.smack.inttest.rfc6121;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.PresenceBuilder;
import org.jivesoftware.smack.packet.SimpleIQ;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.roster.AbstractRosterListener;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.util.StringUtils;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.Collection;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that verify that behavior defined in section 2.4 "Updating a Roster Item" of section 2 "Managing the Roster" of RFC6121.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
@SpecificationReference(document = "RFC6121")
public class RFC6121Section2dot4RosterIntegrationTest extends AbstractSmackIntegrationTest
{
    private final boolean isSendPresence;

    private final SmackIntegrationTestEnvironment environment;

    public RFC6121Section2dot4RosterIntegrationTest(SmackIntegrationTestEnvironment environment) throws SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
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

    @SmackIntegrationTest(section = "2.4.1", quote = "Updating an existing roster item is done in the same way as adding a new roster item, i.e., by sending a roster set to the server. Because a roster item is atomic, the item MUST be updated exactly as provided in the roster set. There are several reasons why a client might update a roster item: 1.  Adding a group [...]")
    public void testRosterUpdateAddGroup() throws XmppStringprepException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, SmackException.NotLoggedInException
    {
        // Setup test fixture
        final BareJid target = JidCreate.bareFrom( Localpart.from("romeo-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );
        Roster.getInstanceFor(conOne).createItem(target, "Romeo", new String[] { "Friends" });

        // Execute system under test
        try {
            final RosterPacket request = new RosterPacket();
            request.setType(IQ.Type.set);
            final RosterPacket.Item item = new RosterPacket.Item(target, "Romeo");
            item.addGroupName("Friends");
            item.addGroupName("Lovers");
            request.addRosterItem(item);
            conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result
            final Roster roster = Roster.getInstanceFor(conOne);
            roster.reloadAndWait();

            final RosterEntry rosterItem = roster.getEntry(target);
            assertEquals("Romeo", rosterItem.getName(), "Unexpected name for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to name to be equal to that what was used in a Roster Update that intended to add a group to a preexisting roster item (but it was not).");
            assertEquals(2, rosterItem.getGroups().size(), "Unexpected amount of groups for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to amount to be equal to the amount of groups used in the Roster Update that intended to add a group to a preexisting roster item (but it was not).");
            assertTrue(rosterItem.getGroups().stream().map(RosterGroup::getName).collect(Collectors.toSet()).contains("Friends"), "Expected the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "' to have the group named 'Friends' (but it did not).");
            assertTrue(rosterItem.getGroups().stream().map(RosterGroup::getName).collect(Collectors.toSet()).contains("Lovers"), "Expected the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "' to have the group named 'Lovers' (but it did not).");
        } catch (XMPPException.XMPPErrorException e) {
            fail("Unexpected error response received by '" + conOne.getUser() + "' after it sent a a Roster Update that intended to add a group to a preexisting roster item.");
        } finally {
            // Tear down test fixture
            final RosterEntry entry = Roster.getInstanceFor(conOne).getEntry(target);
            if (entry != null) {
                Roster.getInstanceFor(conOne).removeEntry(entry);
            }
        }
    }

    @SmackIntegrationTest(section = "2.4.1", quote = "Updating an existing roster item is done in the same way as adding a new roster item, i.e., by sending a roster set to the server. Because a roster item is atomic, the item MUST be updated exactly as provided in the roster set. There are several reasons why a client might update a roster item: [...] 2.  Deleting a group [...]")
    public void testRosterUpdateDeleteGroup() throws XmppStringprepException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, SmackException.NotLoggedInException
    {
        // Setup test fixture
        final BareJid target = JidCreate.bareFrom( Localpart.from("romeo-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );
        Roster.getInstanceFor(conOne).createItem(target, "Romeo", new String[] { "Friends", "Lovers" });

        // Execute system under test
        try {
            final RosterPacket request = new RosterPacket();
            request.setType(IQ.Type.set);
            final RosterPacket.Item item = new RosterPacket.Item(target, "Romeo");
            item.addGroupName("Friends");
            request.addRosterItem(item);
            conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result
            final Roster roster = Roster.getInstanceFor(conOne);
            roster.reloadAndWait();

            final RosterEntry rosterItem = roster.getEntry(target);
            assertEquals("Romeo", rosterItem.getName(), "Unexpected name for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to name to be equal to that what was used in a Roster Update that intended to delete a group from a preexisting roster item (but it was not).");
            assertEquals(1, rosterItem.getGroups().size(), "Unexpected amount of groups for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to amount to be equal to the amount of groups used in the Roster Update that intended to delete a group from a preexisting roster item (but it was not).");
            assertTrue(rosterItem.getGroups().stream().map(RosterGroup::getName).collect(Collectors.toSet()).contains("Friends"), "Expected the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "' to have the group named 'Friends' (but it did not).");
        } catch (XMPPException.XMPPErrorException e) {
            fail("Unexpected error response received by '" + conOne.getUser() + "' after it sent a a Roster Update that intended to remove a group from a preexisting roster item.");
        } finally {
            // Tear down test fixture
            final RosterEntry entry = Roster.getInstanceFor(conOne).getEntry(target);
            if (entry != null) {
                Roster.getInstanceFor(conOne).removeEntry(entry);
            }
        }
    }

    @SmackIntegrationTest(section = "2.4.1", quote = "Updating an existing roster item is done in the same way as adding a new roster item, i.e., by sending a roster set to the server. Because a roster item is atomic, the item MUST be updated exactly as provided in the roster set. There are several reasons why a client might update a roster item: [...] 2.  Deleting a group [...]")
    public void testRosterUpdateDeleteAllGroups() throws XmppStringprepException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, SmackException.NotLoggedInException
    {
        // Setup test fixture
        final BareJid target = JidCreate.bareFrom( Localpart.from("romeo-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );
        Roster.getInstanceFor(conOne).createItem(target, "Romeo", new String[] { "Friends" });

        // Execute system under test
        try {
            final RosterPacket request = new RosterPacket();
            request.setType(IQ.Type.set);
            final RosterPacket.Item item = new RosterPacket.Item(target, "Romeo");
            request.addRosterItem(item);
            conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result
            final Roster roster = Roster.getInstanceFor(conOne);
            roster.reloadAndWait();

            final RosterEntry rosterItem = roster.getEntry(target);
            assertEquals("Romeo", rosterItem.getName(), "Unexpected name for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to name to be equal to that what was used in a Roster Update that intended to add a group to a preexisting roster item (but it was not).");
            assertEquals(0, rosterItem.getGroups().size(), "Unexpected amount of groups for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to find no groups after removing all groups from a preexisting roster item (but the group list is not empty).");
        } catch (XMPPException.XMPPErrorException e) {
            fail("Unexpected error response received by '" + conOne.getUser() + "' after it sent a a Roster Update that intended to remove the last group from a preexisting roster item.");
        } finally {
            // Tear down test fixture
            final RosterEntry entry = Roster.getInstanceFor(conOne).getEntry(target);
            if (entry != null) {
                Roster.getInstanceFor(conOne).removeEntry(entry);
            }
        }
    }

    @SmackIntegrationTest(section = "2.4.1", quote = "Updating an existing roster item is done in the same way as adding a new roster item, i.e., by sending a roster set to the server. Because a roster item is atomic, the item MUST be updated exactly as provided in the roster set. There are several reasons why a client might update a roster item: [...] 3.  Changing the handle [...]")
    public void testRosterUpdateChangeHandle() throws XmppStringprepException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, SmackException.NotLoggedInException
    {
        // Setup test fixture
        final BareJid target = JidCreate.bareFrom( Localpart.from("romeo-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );
        Roster.getInstanceFor(conOne).createItem(target, "Romeo", new String[0]);

        // Execute system under test
        try {
            final RosterPacket request = new RosterPacket();
            request.setType(IQ.Type.set);
            final RosterPacket.Item item = new RosterPacket.Item(target, "MyRomeo");
            request.addRosterItem(item);
            conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result
            final Roster roster = Roster.getInstanceFor(conOne);
            roster.reloadAndWait();

            final RosterEntry rosterItem = roster.getEntry(target);
            assertEquals("MyRomeo", rosterItem.getName(), "Unexpected name for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to name to be equal to that what was used in a Roster Update that intended to update the handle of a preexisting roster item (but it was not).");
        } catch (XMPPException.XMPPErrorException e) {
            fail("Unexpected error response received by '" + conOne.getUser() + "' after it sent a a Roster Update that intended to add a group to a preexisting roster item.");
        } finally {
            // Tear down test fixture
            final RosterEntry entry = Roster.getInstanceFor(conOne).getEntry(target);
            if (entry != null) {
                Roster.getInstanceFor(conOne).removeEntry(entry);
            }
        }
    }

    @SmackIntegrationTest(section = "2.4.1", quote = "Updating an existing roster item is done in the same way as adding a new roster item, i.e., by sending a roster set to the server. Because a roster item is atomic, the item MUST be updated exactly as provided in the roster set. There are several reasons why a client might update a roster item: [...] 3.  Deleting the handle [...]")
    public void testRosterUpdateDeleteHandle() throws XmppStringprepException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, SmackException.NotLoggedInException
    {
        // Setup test fixture
        final BareJid target = JidCreate.bareFrom( Localpart.from("romeo-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );
        Roster.getInstanceFor(conOne).createItem(target, "Romeo", new String[0]);

        // Execute system under test
        try {
            final RosterPacket request = new RosterPacket();
            request.setType(IQ.Type.set);
            final RosterPacket.Item item = new RosterPacket.Item(target, null);
            request.addRosterItem(item);
            conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result
            final Roster roster = Roster.getInstanceFor(conOne);
            roster.reloadAndWait();

            final String updatedHandle = roster.getEntry(target).getName();
            assertTrue(updatedHandle == null || updatedHandle.isEmpty(), "Unexpected name for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to name to be absent or empty after a Roster Update that intended to remove the handle of a preexisting roster item (but the handle was '" + updatedHandle + "').");
        } catch (XMPPException.XMPPErrorException e) {
            fail("Unexpected error response received by '" + conOne.getUser() + "' after it sent a a Roster Update that intended to add a group to a preexisting roster item.");
        } finally {
            // Tear down test fixture
            final RosterEntry entry = Roster.getInstanceFor(conOne).getEntry(target);
            if (entry != null) {
                Roster.getInstanceFor(conOne).removeEntry(entry);
            }
        }
    }

    @SmackIntegrationTest(section = "2.4.2", quote = "if the roster item can be successfully processed then the server MUST [...] send an IQ result to the initiating resource")
    public void testRosterUpdateResult() throws XmppStringprepException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, SmackException.NotLoggedInException
    {
        // Setup test fixture
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );
        Roster.getInstanceFor(conOne).createItem(target, "Test User", new String[] { "Test Group" });

        final RosterPacket request = new RosterPacket();
        request.setType(IQ.Type.set);
        final RosterPacket.Item item = new RosterPacket.Item(target, "Test User Update");
        item.addGroupName("Test Group");
        request.addRosterItem(item);

        // Execute system under test
        try {
            final IQ result = conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result
            assertEquals(IQ.Type.result, result.getType(), "Unexpected response type received by '" + conOne.getUser() + "' after it sent a normal Roster Update.");
        } catch (XMPPException.XMPPErrorException e) {
            fail("Unexpected error response received by '" + conOne.getUser() + "' after it sent a normal Roster Update.");
        } finally {
            // Tear down test fixture
            final RosterEntry entry = Roster.getInstanceFor(conOne).getEntry(target);
            if (entry != null) {
                Roster.getInstanceFor(conOne).removeEntry(entry);
            }
        }
    }

    @SmackIntegrationTest(section = "2.4.2", quote = "As with adding a roster item, if the roster item can be successfully processed then the server MUST [...] send a roster push to all of the user's interested resources")
    public void testRosterUpdateGeneratesPushToInterestedResourceSelfWithInitialPresence() throws Exception
    {
        // Setup test fixture.
        if (!isSendPresence) {
            // Ensure that initial presence is sent.
            conOne.sendStanza(PresenceBuilder.buildPresence().build());
        }
        final Roster rosterOne = Roster.getInstanceFor(conOne); // Ensure that this resource is an 'interested resource' by loading the roster (Smack probably already did this through Roster#rosterLoadedAtLoginDefault, but it doesn't hurt to be certain).

        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );
        final String rosterItemOriginalName = "Test User";
        final String rosterItemUpdatedName = "Test User Update";
        final String rosterItemOriginalGroupName = "Test Group";
        final String rosterItemUpdatedGroupName = "Test Group Update";

        final SimpleResultSyncPoint receivedSet = new SimpleResultSyncPoint();
        final SimpleResultSyncPoint receivedUpdate = new SimpleResultSyncPoint();
        final AbstractRosterListener rosterListener = new AbstractRosterListener()
        {
            @Override
            public void entriesAdded(Collection<Jid> collection)
            {
                if (collection.contains(target)) {
                    receivedSet.signal();
                }
            }

            @Override
            public void entriesUpdated(Collection<Jid> collection)
            {
                if (collection.contains(target)) {
                    receivedUpdate.signal();
                }
            }
        };
        rosterOne.addRosterListener(rosterListener);

        try {
            rosterOne.createItem(target, rosterItemOriginalName, new String[]{rosterItemOriginalGroupName});
            receivedSet.waitForResult(timeout); // Wait for the push for the original set to have arrived, before waiting for the push for the update.

            final RosterPacket request = new RosterPacket();
            request.setType(IQ.Type.set);
            final RosterPacket.Item item = new RosterPacket.Item(target, rosterItemUpdatedName);
            item.addGroupName(rosterItemUpdatedGroupName);
            request.addRosterItem(item);

            // Execute system under test.
            conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result.
            assertResult(receivedUpdate, "Expected '" + conOne.getUser() + "' to receive a roster push after a roster item was updated through the same resource, after the connection had earlier send initial presence and obtained the roster, thus making it an 'interested resource'. The roster push was not received.");

            // After the roster push was received, the roster should now contain the updated item, which is inspected below to see if the expected changes
            // are applied. Ideally, we'd inspect the pushed item directly, but Smack doesn't make that easy to do.
            final RosterEntry pushedEntry = rosterOne.getEntry(target);
            if (pushedEntry == null) {
                // This is a bug in the test, not in the system-under-test.
                throw new IllegalStateException("Expected the roster of '" + conOne.getUser() + "' to contain an item for '" + target + "' after having received a roster push (but no such roster item was found)");
            }
            assertEquals(rosterItemUpdatedName, pushedEntry.getName(), "Unexpected name for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to name to be equal to that what was used in a Roster Update by the same user (but it was not).");
            assertEquals(1, pushedEntry.getGroups().size(), "Unexpected amount of groups for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to amount to be equal to the amount of groups used in the Roster Update by the same user (but it was not).");
            assertEquals(rosterItemUpdatedGroupName, pushedEntry.getGroups().get(0).getName(), "Unexpected group name for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to group name to be equal to that what was used in a Roster Update by the same user (but it was not).");
        } finally {
            // Tear down test fixture.
            rosterOne.removeRosterListener(rosterListener);
            final RosterEntry entry = rosterOne.getEntry(target);
            if (entry != null) {
                rosterOne.removeEntry(entry);
            }
        }
    }

    @SmackIntegrationTest(section = "2.4.2", quote = "As with adding a roster item, if the roster item can be successfully processed then the server MUST [...] send a roster push to all of the user's interested resources")
    public void testRosterUpdateGeneratesPushToInterestedResourceSelfWithoutInitialPresence() throws Exception
    {
        if (isSendPresence) {
            throw new TestNotPossibleException("The test implementation requires the connection used for testing to not have sent initial presence (but current configuration causes initial presence to be sent automatically).");
        }

        // Setup test fixture.
        final Roster rosterOne = Roster.getInstanceFor(conOne); // Ensure that this resource is an 'interested resource' by loading the roster (Smack probably already did this through Roster#rosterLoadedAtLoginDefault, but it doesn't hurt to be certain).

        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );
        final String rosterItemOriginalName = "Test User";
        final String rosterItemUpdatedName = "Test User Update";
        final String rosterItemOriginalGroupName = "Test Group";
        final String rosterItemUpdatedGroupName = "Test Group Update";

        final SimpleResultSyncPoint receivedSet = new SimpleResultSyncPoint();
        final SimpleResultSyncPoint receivedUpdate = new SimpleResultSyncPoint();
        final AbstractRosterListener rosterListener = new AbstractRosterListener()
        {
            @Override
            public void entriesAdded(Collection<Jid> collection)
            {
                if (collection.contains(target)) {
                    receivedSet.signal();
                }
            }

            @Override
            public void entriesUpdated(Collection<Jid> collection)
            {
                if (collection.contains(target)) {
                    receivedUpdate.signal();
                }
            }
        };
        rosterOne.addRosterListener(rosterListener);

        try {
            rosterOne.createItem(target, rosterItemOriginalName, new String[]{rosterItemOriginalGroupName});
            receivedSet.waitForResult(timeout); // Wait for the push for the original set to have arrived, before waiting for the push for the update.

            final RosterPacket updateRequest = new RosterPacket();
            updateRequest.setType(IQ.Type.set);
            final RosterPacket.Item item = new RosterPacket.Item(target, rosterItemUpdatedName);
            item.addGroupName(rosterItemUpdatedGroupName);
            updateRequest.addRosterItem(item);

            // Execute system under test.
            conOne.sendIqRequestAndWaitForResponse(updateRequest);

            // Verify result.
            assertResult(receivedUpdate, "Expected '" + conOne.getUser() + "' to receive a roster push after a roster item was updated through the same resource, after the connection had earlier obtained the roster (without sending initial presence), thus making it an 'interested resource'. The roster push was not received.");

            // After the roster push was received, the roster should now contain the updated item, which is inspected below to see if the expected changes
            // are applied. Ideally, we'd inspect the pushed item directly, but Smack doesn't make that easy to do.
            final RosterEntry pushedEntry = rosterOne.getEntry(target);
            if (pushedEntry == null) {
                // This is a bug in the test, not in the system-under-test.
                throw new IllegalStateException("Expected the roster of '" + conOne.getUser() + "' to contain an item for '" + target + "' after having received a roster push (but no such roster item was found)");
            }
            assertEquals(rosterItemUpdatedName, pushedEntry.getName(), "Unexpected name for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to name to be equal to that what was used in a Roster Update by the same user (but it was not).");
            assertEquals(1, pushedEntry.getGroups().size(), "Unexpected amount of groups for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to amount to be equal to the amount of groups used in the Roster Update by the same user (but it was not).");
            assertEquals(rosterItemUpdatedGroupName, pushedEntry.getGroups().get(0).getName(), "Unexpected group name for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to group name to be equal to that what was used in a Roster Update by the same user (but it was not).");
        } finally {
            // Tear down test fixture.
            rosterOne.removeRosterListener(rosterListener);
            final RosterEntry entry = rosterOne.getEntry(target);
            if (entry != null) {
                rosterOne.removeEntry(entry);
            }
        }
    }

    @SmackIntegrationTest(section = "2.4.2", quote = "As with adding a roster item, if the roster item can be successfully processed then the server MUST [...] send a roster push to all of the user's interested resources")
    public void testRosterUpdateGeneratesPushToInterestedResourceOtherResourceWithInitialPresence() throws Exception
    {
        final AbstractXMPPConnection conOneSecondary = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
        try {
            // Setup test fixture.
            conOneSecondary.connect();
            conOneSecondary.login(((AbstractXMPPConnection)conOne).getConfiguration().getUsername(), ((AbstractXMPPConnection)conOne).getConfiguration().getPassword(), Resourcepart.from(StringUtils.randomString(7)));
            if (!isSendPresence) {
                // Ensure that initial presence is sent.
                conOneSecondary.sendStanza(PresenceBuilder.buildPresence().build());
            }
            final Roster rosterOnePrimary = Roster.getInstanceFor(conOne);
            final Roster rosterOneSecondary = Roster.getInstanceFor(conOneSecondary); // Ensure that this resource is an 'interested resource' by loading the roster (Smack probably already did this through Roster#rosterLoadedAtLoginDefault but it doesn't hurt to be certain).

            final BareJid target = JidCreate.bareFrom(Localpart.from("test-target-" + StringUtils.randomString(5)), conOneSecondary.getXMPPServiceDomain());
            final String rosterItemOriginalName = "Test User";
            final String rosterItemUpdatedName = "Test User Update";
            final String rosterItemOriginalGroupName = "Test Group";
            final String rosterItemUpdatedGroupName = "Test Group Update";

            final SimpleResultSyncPoint receivedSet = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint receivedUpdate = new SimpleResultSyncPoint();
            final AbstractRosterListener rosterListener = new AbstractRosterListener()
            {
                @Override
                public void entriesAdded(Collection<Jid> collection)
                {
                    if (collection.contains(target)) {
                        receivedSet.signal();
                    }
                }

                @Override
                public void entriesUpdated(Collection<Jid> collection)
                {
                    if (collection.contains(target)) {
                        receivedUpdate.signal();
                    }
                }
            };
            rosterOneSecondary.addRosterListener(rosterListener);

            try {
                rosterOnePrimary.createItem(target, rosterItemOriginalName, new String[]{rosterItemOriginalGroupName});
                receivedSet.waitForResult(timeout); // Wait for the push for the original set to have arrived, before waiting for the push for the update.

                final RosterPacket updateRequest = new RosterPacket();
                updateRequest.setType(IQ.Type.set);
                final RosterPacket.Item item = new RosterPacket.Item(target, rosterItemUpdatedName);
                item.addGroupName(rosterItemUpdatedGroupName);
                updateRequest.addRosterItem(item);

                // Execute system under test.
                conOne.sendIqRequestAndWaitForResponse(updateRequest);

                // Verify result.
                assertResult(receivedUpdate, "Expected '" + conOneSecondary.getUser() + "' to receive a roster push after a roster item was updated by a different resource of that user ('" + conOne.getUser() + "'), after the connection had earlier send initial presence and obtained the roster, thus making it an 'interested resource'. The roster push was not received.");

                // After the roster push was received, the roster should now contain the updated item, which is inspected below to see if the expected changes
                // are applied. Ideally, we'd inspect the pushed item directly, but Smack doesn't make that easy to do.
                final RosterEntry pushedEntry = rosterOneSecondary.getEntry(target);
                if (pushedEntry == null) {
                    // This is a bug in the test, not in the system-under-test.
                    throw new IllegalStateException("Expected the roster of '" + conOne.getUser() + "' to contain an item for '" + target + "' after having received a roster push (but no such roster item was found)");
                }
                assertEquals(rosterItemUpdatedName, pushedEntry.getName(), "Unexpected name for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to name to be equal to that what was used in a Roster Update by a different resource of the same user (but it was not).");
                assertEquals(1, pushedEntry.getGroups().size(), "Unexpected amount of groups for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to amount to be equal to the amount of groups used in the Roster Update by a different resource of the same user (but it was not).");
                assertEquals(rosterItemUpdatedGroupName, pushedEntry.getGroups().get(0).getName(), "Unexpected group name for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to group name to be equal to that what was used in a Roster Update by a different resource of the same user (but it was not).");
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

    @SmackIntegrationTest(section = "2.4.2", quote = "As with adding a roster item, if the roster item can be successfully processed then the server MUST [...] send a roster push to all of the user's interested resources")
    public void testRosterUpdateGeneratesPushToInterestedResourceOtherResourceWithoutInitialPresence() throws Exception
    {
        if (isSendPresence) {
            throw new TestNotPossibleException("The test implementation requires the connection used for testing to not have sent initial presence (but current configuration causes initial presence to be sent automatically).");
        }

        final AbstractXMPPConnection conOneSecondary = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
        try {
            // Setup test fixture.
            conOneSecondary.connect();
            conOneSecondary.login(((AbstractXMPPConnection)conOne).getConfiguration().getUsername(), ((AbstractXMPPConnection)conOne).getConfiguration().getPassword(), Resourcepart.from(StringUtils.randomString(7)));

            final Roster rosterOnePrimary = Roster.getInstanceFor(conOne);
            final Roster rosterOneSecondary = Roster.getInstanceFor(conOneSecondary); // Ensure that this resource is an 'interested resource' by loading the roster (Smack probably already did this through Roster#rosterLoadedAtLoginDefault but it doesn't hurt to be certain).

            final BareJid target = JidCreate.bareFrom(Localpart.from("test-target-" + StringUtils.randomString(5)), conOneSecondary.getXMPPServiceDomain());
            final String rosterItemOriginalName = "Test User";
            final String rosterItemUpdatedName = "Test User Update";
            final String rosterItemOriginalGroupName = "Test Group";
            final String rosterItemUpdatedGroupName = "Test Group Update";

            final SimpleResultSyncPoint receivedSet = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint receivedUpdate = new SimpleResultSyncPoint();
            final AbstractRosterListener rosterListener = new AbstractRosterListener()
            {
                @Override
                public void entriesAdded(Collection<Jid> collection)
                {
                    if (collection.contains(target)) {
                        receivedSet.signal();
                    }
                }

                @Override
                public void entriesUpdated(Collection<Jid> collection)
                {
                    if (collection.contains(target)) {
                        receivedUpdate.signal();
                    }
                }
            };
            rosterOneSecondary.addRosterListener(rosterListener);

            try {
                rosterOnePrimary.createItem(target, rosterItemOriginalName, new String[]{rosterItemOriginalGroupName});
                receivedSet.waitForResult(timeout); // Wait for the push for the original set to have arrived, before waiting for the push for the update.

                final RosterPacket updateRequest = new RosterPacket();
                updateRequest.setType(IQ.Type.set);
                final RosterPacket.Item item = new RosterPacket.Item(target, rosterItemUpdatedName);
                item.addGroupName(rosterItemUpdatedGroupName);
                updateRequest.addRosterItem(item);

                // Execute system under test.
                conOne.sendIqRequestAndWaitForResponse(updateRequest);

                // Verify result.
                assertResult(receivedUpdate, "Expected '" + conOneSecondary.getUser() + "' to receive a roster push after a roster item was updated by a different resource of that user ('" + conOne.getUser() + "'), after the connection had earlier obtained the roster (without sending initial presence), thus making it an 'interested resource'. The roster push was not received.");

                // After the roster push was received, the roster should now contain the updated item, which is inspected below to see if the expected changes
                // are applied. Ideally, we'd inspect the pushed item directly, but Smack doesn't make that easy to do.
                final RosterEntry pushedEntry = rosterOneSecondary.getEntry(target);
                if (pushedEntry == null) {
                    // This is a bug in the test, not in the system-under-test.
                    throw new IllegalStateException("Expected the roster of '" + conOne.getUser() + "' to contain an item for '" + target + "' after having received a roster push (but no such roster item was found)");
                }
                assertEquals(rosterItemUpdatedName, pushedEntry.getName(), "Unexpected name for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to name to be equal to that what was used in a Roster Update by a different resource of the same user (but it was not).");
                assertEquals(1, pushedEntry.getGroups().size(), "Unexpected amount of groups for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to amount to be equal to the amount of groups used in the Roster Update by a different resource of the same user (but it was not).");
                assertEquals(rosterItemUpdatedGroupName, pushedEntry.getGroups().get(0).getName(), "Unexpected group name for the roster item of '" + target + "' on the roster of '" + conOne.getUser() + "': Expected to group name to be equal to that what was used in a Roster Update by a different resource of the same user (but it was not).");
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

    @SmackIntegrationTest(section = "2.4.3", quote = "The error cases described under Section 2.3.3 also apply to updating a roster item. [from 2.3.3:] The server MUST return a <bad-request/> stanza error to the client if the roster set contains any of the following violations: [...] The <query/> element contains more than one <item/> child element.")
    public void testRosterUpdateMultipleItems() throws XmppStringprepException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, SmackException.NotLoggedInException
    {
        // Setup test fixture
        final BareJid targetA = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );
        final BareJid targetB = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );

        final RosterPacket requestSetA = new RosterPacket();
        requestSetA.setType(IQ.Type.set);
        requestSetA.addRosterItem(new RosterPacket.Item(targetA, "Test User A"));
        conOne.sendIqRequestAndWaitForResponse(requestSetA);

        final RosterPacket requestSetB = new RosterPacket();
        requestSetB.setType(IQ.Type.set);
        requestSetB.addRosterItem(new RosterPacket.Item(targetB, "Test User B"));
        conOne.sendIqRequestAndWaitForResponse(requestSetB);

        final RosterPacket requestUpdate = new RosterPacket();
        requestUpdate.setType(IQ.Type.set);
        requestUpdate.addRosterItem(new RosterPacket.Item(targetA, "Test User Update A"));
        requestUpdate.addRosterItem(new RosterPacket.Item(targetB, "Test User Update B"));

        // Execute system under test / verify result
        try {
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> conOne.sendIqRequestAndWaitForResponse(requestUpdate),
                "Expected user '" + conOne.getUser() + "' to receive an error in response to the Roster Update stanza that contained more than one <item/> child element (but the server did not return an error).");
            assertEquals(StanzaError.Condition.bad_request, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conOne.getUser() + "' after it sent a Roster Update that contained more than one <item/> child element.");
        } finally {
            // Tear down test fixture.
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

    @SmackIntegrationTest(section = "2.4.3", quote = "The error cases described under Section 2.3.3 also apply to updating a roster item. [from 2.3.3:] The server MUST return a <forbidden/> stanza error to the client if the sender of the roster set is not authorized to update the roster.")
    public void testRosterUpdateNotAuthorized() throws XmppStringprepException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, SmackException.NotLoggedInException, InterruptedException
    {
        // Setup test fixture
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );

        final Roster rosterTwo = Roster.getInstanceFor(conTwo);
        rosterTwo.createItem(target, "Test User", new String[0]);

        final RosterPacket request = new RosterPacket();
        request.setType(IQ.Type.set);
        request.addRosterItem(new RosterPacket.Item(target, "Test User Update"));
        request.setTo(conTwo.getUser().asBareJid()); // Address to a _different_ user than the one that will be sending the request.

        try {
            // Execute system under test / verify result
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> conOne.sendIqRequestAndWaitForResponse(request),
                "Expected user '" + conOne.getUser() + "' to receive an error in response to the Roster Update stanza that it sent that was addressed to '" + request.getTo() + "' (where it is assumed that '" + conOne.getUser().asBareJid() + "' is not authorized to update the roster of '" + request.getTo().asBareJid() + "') but the server did not return an error.");
            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conOne.getUser() + "' after it sent a Roster Update that was addressed to '" + request.getTo() + "' (where it is assumed that '" + conOne.getUser().asBareJid() + "' is not authorized to update the roster of '" + request.getTo().asBareJid() + "'");
        } finally {
            // Tear down test fixture
            final Roster rosterOne = Roster.getInstanceFor(conOne);
            final RosterEntry entryA = rosterOne.getEntry(target);
            if (entryA != null) {
                rosterOne.removeEntry(entryA);
            }
            final RosterEntry entryB = rosterTwo.getEntry(target);
            if (entryB != null) {
                rosterTwo.removeEntry(entryB);
            }
        }
    }

    @SmackIntegrationTest(section = "2.4.3", quote = "The error cases described under Section 2.3.3 also apply to updating a roster item. [from 2.3.3:] The server MUST return a <bad-request/> stanza error to the client if the roster set contains any of the following violations: [...] The <item/> element contains more than one <group/> element, but there are duplicate groups")
    public void testRosterUpdateDuplicateGroups() throws XmppStringprepException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, SmackException.NotLoggedInException, InterruptedException
    {
        // Setup test fixture
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );

        final Roster rosterOne = Roster.getInstanceFor(conOne);
        rosterOne.createItem(target, "Test User", new String[] { "Test Group" });

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
                "Expected user '" + conOne.getUser() + "' to receive an error in response to the Roster Update stanza that contained an <item/> child element that has two groups with an identical name (but the server did not return an error).");
            assertEquals(StanzaError.Condition.bad_request, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conOne.getUser() + "' after it sent a Roster Update that contained an <item/> child element that has two groups with an identical name.");
        } finally {
            // Tear down test fixture.
            final RosterEntry entry = rosterOne.getEntry(target);
            if (entry != null) {
                rosterOne.removeEntry(entry);
            }
        }
    }

    @SmackIntegrationTest(section = "2.4.3", quote = "The error cases described under Section 2.3.3 also apply to updating a roster item. [from 2.3.3:] The server MUST return a <not-acceptable/> stanza error to the client if the roster set contains any of the following violations: [...] The XML character data of the <group/> element is of zero length")
    public void testRosterUpdateZeroLengthGroup() throws XmppStringprepException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, SmackException.NotLoggedInException, InterruptedException
    {
        // Setup test fixture
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );

        final Roster rosterOne = Roster.getInstanceFor(conOne);
        rosterOne.createItem(target, "Test User", new String[] { "Test Group" });

        final RosterPacket request = new RosterPacket();
        request.setType(IQ.Type.set);
        final RosterPacket.Item item = new RosterPacket.Item(target, null);
        item.addGroupName("");
        request.addRosterItem(item);

        try {
            // Execute system under test / verify result
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> conOne.sendIqRequestAndWaitForResponse(request),
                "Expected user '" + conOne.getUser() + "' to receive an error in response to the Roster Update stanza that contained an empty group element (but the server did not return an error).");
            assertEquals(StanzaError.Condition.not_acceptable, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conOne.getUser() + "' after it sent a Roster Update that contained an empty group element.");
        } finally {
            // Tear down test fixture.
            final RosterEntry entry = rosterOne.getEntry(target);
            if (entry != null) {
                rosterOne.removeEntry(entry);
            }
        }
    }
}
