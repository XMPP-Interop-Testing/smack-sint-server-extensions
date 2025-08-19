package org.igniterealtime.smack.inttest.rfc6121.section2;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.AccountUtilities;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ListenerHandle;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromMatchesFilter;
import org.jivesoftware.smack.filter.PresenceTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.PresenceBuilder;
import org.jivesoftware.smack.packet.StanzaError;
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

import java.util.Collection;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that verify that behavior defined in section 2.5 "Deleting a Roster Item" of section 2 "Managing the Roster" of RFC6121.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
@SpecificationReference(document = "RFC6121")
public class RFC6121Section2_5_DeleteIntegrationTest extends AbstractSmackIntegrationTest
{
    private final boolean isSendPresence;

    private final SmackIntegrationTestEnvironment environment;

    public RFC6121Section2_5_DeleteIntegrationTest(SmackIntegrationTestEnvironment environment) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
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

    @SmackIntegrationTest(section = "2.5.2", quote = "As with adding a roster item, if the server can successfully process the roster set then it MUST [...] send an IQ result to the initiating resource")
    public void testRosterDeleteResult() throws XmppStringprepException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, SmackException.NotLoggedInException
    {
        // Setup test fixture
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );
        Roster.getInstanceFor(conOne).createItem(target, "Test User", new String[] { "Test Group" });

        final RosterPacket request = new RosterPacket();
        request.setType(IQ.Type.set);
        final RosterPacket.Item item = new RosterPacket.Item(target, null);
        item.setItemType(RosterPacket.ItemType.remove);
        request.addRosterItem(item);

        // Execute system under test
        try {
            final IQ result = conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result
            assertEquals(IQ.Type.result, result.getType(), "Unexpected response type received by '" + conOne.getUser() + "' after it sent a normal Roster Item Delete.");
        } catch (XMPPException.XMPPErrorException e) {
            fail("Unexpected error response received by '" + conOne.getUser() + "' after it sent a normal Roster Item Delete.");
        } finally {
            // Tear down test fixture
            final RosterEntry entry = Roster.getInstanceFor(conOne).getEntry(target);
            if (entry != null) {
                Roster.getInstanceFor(conOne).removeEntry(entry);
            }
        }
    }

    @SmackIntegrationTest(section = "2.5.2", quote = "As with adding a roster item, if the server can successfully process the roster set then it MUST [...] send a roster push to all of the user's interested resources (with the 'subscription' attribute set to a value of \"remove\")")
    public void testRosterDeleteGeneratesPushToInterestedResourceSelfWithInitialPresence() throws Exception
    {
        // Setup test fixture.
        if (!isSendPresence) {
            // Ensure that initial presence is sent.
            conOne.sendStanza(PresenceBuilder.buildPresence().build());
        }
        final Roster rosterOne = Roster.getInstanceFor(conOne); // Ensure that this resource is an 'interested resource' by loading the roster (Smack probably already did this through Roster#rosterLoadedAtLoginDefault, but it doesn't hurt to be certain).

        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );

        final SimpleResultSyncPoint receivedSet = new SimpleResultSyncPoint();
        final SimpleResultSyncPoint receivedDelete = new SimpleResultSyncPoint();
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
            public void entriesDeleted(Collection<Jid> collection)
            {
                // org.jivesoftware.smack.roster.Roster.RosterPushListener triggers this event listener if an item in a roster push has a 'remove' attribute.
                if (collection.contains(target)) {
                    receivedDelete.signal();
                }
            }
        };
        rosterOne.addRosterListener(rosterListener);

        try {
            rosterOne.createItem(target, "Test User", new String[] { "Test Group" });
            receivedSet.waitForResult(timeout); // Wait for the push for the original set to have arrived.

            // Execute system under test.
            final RosterPacket request = new RosterPacket();
            request.setType(IQ.Type.set);
            final RosterPacket.Item item = new RosterPacket.Item(target, null);
            item.setItemType(RosterPacket.ItemType.remove);
            request.addRosterItem(item);

            conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result.
            assertResult(receivedDelete, "Expected '" + conOne.getUser() + "' to receive a roster push after a roster item was deleted through the same resource, after the connection had earlier send initial presence and obtained the roster, thus making it an 'interested resource'. The roster push was not received.");

            // After the roster push was received, the roster should no longer contain the item.
            final Roster roster = Roster.getInstanceFor(conOne);
            roster.reloadAndWait();
            final RosterEntry removedEntry = rosterOne.getEntry(target);
            assertNull(removedEntry, "Expected the roster of '" + conOne.getUser() + "' to no longer contain an item for '" + target + "' after it was removed (but a roster item was still found)");
        } finally {
            // Tear down test fixture.
            rosterOne.removeRosterListener(rosterListener);
            final RosterEntry entry = rosterOne.getEntry(target);
            if (entry != null) {
                rosterOne.removeEntry(entry);
            }
        }
    }

    @SmackIntegrationTest(section = "2.5.2", quote = "As with adding a roster item, if the server can successfully process the roster set then it MUST [...] send a roster push to all of the user's interested resources (with the 'subscription' attribute set to a value of \"remove\")")
    public void testRosterDeleteGeneratesPushToInterestedResourceSelfWithoutInitialPresence() throws Exception
    {
        if (isSendPresence) {
            throw new TestNotPossibleException("The test implementation requires the connection used for testing to not have sent initial presence (but current configuration causes initial presence to be sent automatically).");
        }

        // Setup test fixture.
        final Roster rosterOne = Roster.getInstanceFor(conOne); // Ensure that this resource is an 'interested resource' by loading the roster (Smack probably already did this through Roster#rosterLoadedAtLoginDefault, but it doesn't hurt to be certain).

        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );
        final SimpleResultSyncPoint receivedSet = new SimpleResultSyncPoint();
        final SimpleResultSyncPoint receivedDelete = new SimpleResultSyncPoint();
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
            public void entriesDeleted(Collection<Jid> collection)
            {
                if (collection.contains(target)) {
                    receivedDelete.signal();
                }
            }
        };
        rosterOne.addRosterListener(rosterListener);

        try {
            rosterOne.createItem(target, "Test User", new String[] { "Test Group" });
            receivedSet.waitForResult(timeout); // Wait for the push for the original set to have arrived.

            // Execute system under test.
            final RosterPacket request = new RosterPacket();
            request.setType(IQ.Type.set);
            final RosterPacket.Item item = new RosterPacket.Item(target, null);
            item.setItemType(RosterPacket.ItemType.remove);
            request.addRosterItem(item);

            conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result.
            assertResult(receivedDelete, "Expected '" + conOne.getUser() + "' to receive a roster push after a roster item was deleted through the same resource, after the connection had earlier obtained the roster (without sending initial presence), thus making it an 'interested resource'. The roster push was not received.");

            // After the roster push was received, the roster should no longer contain the item.
            final Roster roster = Roster.getInstanceFor(conOne);
            roster.reloadAndWait();
            final RosterEntry removedEntry = rosterOne.getEntry(target);
            assertNull(removedEntry, "Expected the roster of '" + conOne.getUser() + "' to no longer contain an item for '" + target + "' after it was removed (but a roster item was still found)");
        } finally {
            // Tear down test fixture.
            rosterOne.removeRosterListener(rosterListener);
            final RosterEntry entry = rosterOne.getEntry(target);
            if (entry != null) {
                rosterOne.removeEntry(entry);
            }
        }
    }

    @SmackIntegrationTest(section = "2.5.2", quote = "As with adding a roster item, if the server can successfully process the roster set then it MUST [...] send a roster push to all of the user's interested resources (with the 'subscription' attribute set to a value of \\\"remove\\\")\"")
    public void testRosterDeleteGeneratesPushToInterestedResourceOtherResourceWithInitialPresence() throws Exception
    {
        final AbstractXMPPConnection conOneSecondary = AccountUtilities.spawnNewConnection(environment, sinttestConfiguration);
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
            final SimpleResultSyncPoint receivedSet = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint receivedDelete = new SimpleResultSyncPoint();
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
                public void entriesDeleted(Collection<Jid> collection)
                {
                    if (collection.contains(target)) {
                        receivedDelete.signal();
                    }
                }
            };
            rosterOneSecondary.addRosterListener(rosterListener);

            try {
                rosterOnePrimary.createItem(target, "Test User", new String[] { "Test Group" });
                receivedSet.waitForResult(timeout); // Wait for the push for the original set to have arrived.

                // Execute system under test.
                final RosterPacket request = new RosterPacket();
                request.setType(IQ.Type.set);
                final RosterPacket.Item item = new RosterPacket.Item(target, null);
                item.setItemType(RosterPacket.ItemType.remove);
                request.addRosterItem(item);

                conOne.sendIqRequestAndWaitForResponse(request);

                // Verify result.
                assertResult(receivedDelete, "Expected '" + conOneSecondary.getUser() + "' to receive a roster push after a roster item was deleted by a different resource of that user ('" + conOne.getUser() + "'), after the connection had earlier send initial presence and obtained the roster, thus making it an 'interested resource'. The roster push was not received.");

                // After the roster push was received, the roster should no longer contain the item.
                rosterOneSecondary.reloadAndWait();
                final RosterEntry removedEntry = rosterOneSecondary.getEntry(target);
                assertNull(removedEntry, "Expected the roster of '" + conOneSecondary.getUser() + "' to no longer contain an item for '" + target + "' after it was removed (but a roster item was still found)");
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

    @SmackIntegrationTest(section = "2.5.2", quote = "As with adding a roster item, if the server can successfully process the roster set then it MUST [...] send a roster push to all of the user's interested resources (with the 'subscription' attribute set to a value of \\\"remove\\\")\"")
    public void testRosterDeleteGeneratesPushToInterestedResourceOtherResourceWithoutInitialPresence() throws Exception
    {
        if (isSendPresence) {
            throw new TestNotPossibleException("The test implementation requires the connection used for testing to not have sent initial presence (but current configuration causes initial presence to be sent automatically).");
        }

        final AbstractXMPPConnection conOneSecondary = AccountUtilities.spawnNewConnection(environment, sinttestConfiguration);
        try {
            // Setup test fixture.
            conOneSecondary.connect();
            conOneSecondary.login(((AbstractXMPPConnection)conOne).getConfiguration().getUsername(), ((AbstractXMPPConnection)conOne).getConfiguration().getPassword(), Resourcepart.from(StringUtils.randomString(7)));

            final Roster rosterOnePrimary = Roster.getInstanceFor(conOne);
            final Roster rosterOneSecondary = Roster.getInstanceFor(conOneSecondary); // Ensure that this resource is an 'interested resource' by loading the roster (Smack probably already did this through Roster#rosterLoadedAtLoginDefault but it doesn't hurt to be certain).

            final BareJid target = JidCreate.bareFrom(Localpart.from("test-target-" + StringUtils.randomString(5)), conOneSecondary.getXMPPServiceDomain());
            final SimpleResultSyncPoint receivedSet = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint receivedDelete = new SimpleResultSyncPoint();
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
                public void entriesDeleted(Collection<Jid> collection)
                {
                    if (collection.contains(target)) {
                        receivedDelete.signal();
                    }
                }
            };
            rosterOneSecondary.addRosterListener(rosterListener);

            try {
                rosterOnePrimary.createItem(target, "Test User", new String[] { "Test Group" });
                receivedSet.waitForResult(timeout); // Wait for the push for the original set to have arrived.

                // Execute system under test.
                final RosterPacket request = new RosterPacket();
                request.setType(IQ.Type.set);
                final RosterPacket.Item item = new RosterPacket.Item(target, null);
                item.setItemType(RosterPacket.ItemType.remove);
                request.addRosterItem(item);

                conOne.sendIqRequestAndWaitForResponse(request);

                // Verify result.
                assertResult(receivedDelete, "Expected '" + conOneSecondary.getUser() + "' to receive a roster push after a roster item was deleted by a different resource of that user ('" + conOne.getUser() + "'), after the connection had earlier obtained the roster (without sending initial presence), thus making it an 'interested resource'. The roster push was not received.");

                // After the roster push was received, the roster should no longer contain the item.
                rosterOneSecondary.reloadAndWait();
                final RosterEntry removedEntry = rosterOneSecondary.getEntry(target);
                assertNull(removedEntry, "Expected the roster of '" + conOneSecondary.getUser() + "' to no longer contain an item for '" + target + "' after it was removed (but a roster item was still found)");
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

    @SmackIntegrationTest(section = "2.5.2", quote = "If the user has a presence subscription to the contact, then the user's server MUST send a presence stanza of type \"unsubscribe\" to the contact")
    public void testRosterDeleteCausesUnsubscribe() throws Exception
    {
        // Setup test fixture
        final BareJid target = conTwo.getUser().asBareJid();
        Roster.getInstanceFor(conOne).createItem(target, "Test User", new String[] { "Test Group" });
        IntegrationTestRosterUtil.ensureSubscribedTo(conTwo, conOne, timeout);

        final SimpleResultSyncPoint unsubscribeReceived = new SimpleResultSyncPoint();
        try (final ListenerHandle ignored = conTwo.addStanzaListener(stanza -> unsubscribeReceived.signal(), new AndFilter(PresenceTypeFilter.UNSUBSCRIBE, FromMatchesFilter.createBare(conOne.getUser()))))
        {
            final RosterPacket request = new RosterPacket();
            request.setType(IQ.Type.set);
            final RosterPacket.Item item = new RosterPacket.Item(target, null);
            item.setItemType(RosterPacket.ItemType.remove);
            request.addRosterItem(item);

            // Execute system under test
            try {
                conOne.sendIqRequestAndWaitForResponse(request);

                // Verify result
                assertResult(unsubscribeReceived, "Expected contact '" + conTwo.getUser() + "' to receive a presence stanza of type 'unsubscribe' from '" + conOne.getUser() + "' after the latter removed the former from their roster (but no such presence stanza was received).");
            } finally {
                // Tear down test fixture
                IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);
            }
        }
    }

    @SmackIntegrationTest(section = "2.5.2", quote = "If the contact has a presence subscription to the user, then the user's server MUST send a presence stanza of type \"unsubscribed\" to the contact (in order to cancel the contact's subscription to the user).")
    public void testRosterDeleteCausesUnsubscribed() throws Exception
    {
        // Setup test fixture
        final BareJid target = conTwo.getUser().asBareJid();
        Roster.getInstanceFor(conOne).createItem(target, "Test User", new String[] { "Test Group" });
        IntegrationTestRosterUtil.ensureSubscribedTo(conOne, conTwo, timeout);

        final SimpleResultSyncPoint unsubscribedReceived = new SimpleResultSyncPoint();
        try (final ListenerHandle ignored = conTwo.addStanzaListener(stanza -> unsubscribedReceived.signal(), new AndFilter(PresenceTypeFilter.UNSUBSCRIBED, FromMatchesFilter.createBare(conOne.getUser()))))
        {
            final RosterPacket request = new RosterPacket();
            request.setType(IQ.Type.set);
            final RosterPacket.Item item = new RosterPacket.Item(target, null);
            item.setItemType(RosterPacket.ItemType.remove);
            request.addRosterItem(item);

            // Execute system under test
            try {
                conOne.sendIqRequestAndWaitForResponse(request);

                // Verify result
                assertResult(unsubscribedReceived, "Expected contact '" + conTwo.getUser() + "' to receive a presence stanza of type 'unsubscribed' from '" + conOne.getUser() + "' after the latter removed the former from their roster (but no such presence stanza was received).");
            } finally {
                // Tear down test fixture
                IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);
            }
        }
    }

    @SmackIntegrationTest(section = "2.5.2", quote = "If the presence subscription is mutual, then the user's server MUST send both a presence stanza of type \"unsubscribe\" and a presence stanza of type \"unsubscribed\" to the contact.")
    public void testRosterDeleteCausesUnsubscribeAndUnsubscribed() throws Exception
    {
        // Setup test fixture
        final BareJid target = conTwo.getUser().asBareJid();
        Roster.getInstanceFor(conOne).createItem(target, "Test User", new String[] { "Test Group" });
        IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(conOne, conTwo, timeout);

        final SimpleResultSyncPoint unsubscribeReceived = new SimpleResultSyncPoint();
        final SimpleResultSyncPoint unsubscribedReceived = new SimpleResultSyncPoint();
        try (final ListenerHandle ignored = conTwo.addStanzaListener(stanza -> unsubscribeReceived.signal(), new AndFilter(PresenceTypeFilter.UNSUBSCRIBE, FromMatchesFilter.createBare(conOne.getUser())));
             final ListenerHandle ignored2 = conTwo.addStanzaListener(stanza -> unsubscribedReceived.signal(), new AndFilter(PresenceTypeFilter.UNSUBSCRIBED, FromMatchesFilter.createBare(conOne.getUser()))) )
        {
            final RosterPacket request = new RosterPacket();
            request.setType(IQ.Type.set);
            final RosterPacket.Item item = new RosterPacket.Item(target, null);
            item.setItemType(RosterPacket.ItemType.remove);
            request.addRosterItem(item);

            // Execute system under test
            try {
                conOne.sendIqRequestAndWaitForResponse(request);

                // Verify result
                assertResult(unsubscribeReceived, "Expected contact '" + conTwo.getUser() + "' to receive a presence stanza of type 'unsubscribe' from '" + conOne.getUser() + "' after the latter removed the former from their roster (but no such presence stanza was received).");
                assertResult(unsubscribedReceived, "Expected contact '" + conTwo.getUser() + "' to receive a presence stanza of type 'unsubscribed' from '" + conOne.getUser() + "' after the latter removed the former from their roster (but no such presence stanza was received).");
            } finally {
                // Tear down test fixture
                IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);
            }
        }
    }

    @SmackIntegrationTest(section = "2.5.3", quote = "If the value of the 'jid' attribute specifies an item that is not in the roster, then the server MUST return an <item-not-found/> stanza error.")
    public void testRosterDeleteNonExistingItem() throws XmppStringprepException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, SmackException.NotLoggedInException
    {
        // Setup test fixture
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );

        final RosterPacket request = new RosterPacket();
        request.setType(IQ.Type.set);
        final RosterPacket.Item item = new RosterPacket.Item(target, null);
        item.setItemType(RosterPacket.ItemType.remove);
        request.addRosterItem(item);

        // Execute system under test / verify result
        final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> conOne.sendIqRequestAndWaitForResponse(request),
            "Expected user '" + conOne.getUser() + "' to receive an error in response to the Roster Item Delete stanza that referenced an item that is not on the roster (with jid '" + target + "') (but the server did not return an error).");
        assertEquals(StanzaError.Condition.item_not_found, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conOne.getUser() + "' after it sent a Roster Item Delete that referenced an item that is not on the roster (with jid '" + target + "')");
    }
}
