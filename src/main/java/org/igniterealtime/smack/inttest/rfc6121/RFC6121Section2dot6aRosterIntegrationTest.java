package org.igniterealtime.smack.inttest.rfc6121;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.ResultSyncPoint;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.iqrequest.IQRequestHandler;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.util.StringUtils;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that verify that behavior defined in section 2.6 "Roster Versioning" of section 2 "Managing the Roster" of RFC6121.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
@SpecificationReference(document = "RFC6121")
public class RFC6121Section2dot6aRosterIntegrationTest extends AbstractSmackIntegrationTest
{
    public RFC6121Section2dot6aRosterIntegrationTest(SmackIntegrationTestEnvironment environment) throws SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        super(environment);

        try {
            conOne.sendIqRequestAndWaitForResponse(new RosterPacket());
        } catch (XMPPException.XMPPErrorException e) {
            if (e.getStanzaError().getCondition() == StanzaError.Condition.service_unavailable) {
                throw new TestNotPossibleException("Server does not support the roster namespace."); // This error is defined in RFC6121 Section 2.2
            }
        }

        if (!Roster.getInstanceFor(conOne).isRosterVersioningSupported()) {
            throw new TestNotPossibleException("Server does not support roster versioning feature.");
        }
    }

    /**
     * Asserts that the client can send a roster request with an empty 'ver' value without the server erroring out.
     */
    @SmackIntegrationTest(section = "2.6.2", quote = "If the client has not yet cached the roster or the cache is lost or corrupted, but the client wishes to bootstrap the use of roster versioning, it MUST set the 'ver' attribute to the empty string (i.e., ver=\"\").")
    public void testRosterVerEmpty() throws SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture
        final RosterPacket request = new RosterPacket();
        request.setType(IQ.Type.get);
        request.setVersion("");

        // Execute system under test
        try {
            final IQ result = conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result
            assertEquals(IQ.Type.result, result.getType(), "Unexpected response type received by '" + conOne.getUser() + "' after it sent a request to bootstrap the use of roster versioning (by setting the 'ver' attribute to the empty string).");
        } catch (XMPPException.XMPPErrorException e) {
            fail("Unexpected response type received by '" + conOne.getUser() + "' after it sent a requested to bootstrap the use of roster versioning (by setting the 'ver' attribute to the empty string). Expected a (non-error) result, but instead received an error: " + e.getStanzaError());
        }
    }

    /**
     * Asserts that the client can send a roster request without a 'ver' element (checking that the server doesn't _require_ this functionality).
     */
    @SmackIntegrationTest(section = "2.6.2", quote = "Naturally, if the client does not support roster versioning or does not wish to bootstrap the use of roster versioning, it will not include the 'ver' attribute.")
    public void testRosterVerMissing() throws SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture
        final RosterPacket request = new RosterPacket();
        request.setType(IQ.Type.get);

        // Execute system under test
        try {
            final IQ result = conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result
            assertEquals(IQ.Type.result, result.getType(), "Unexpected response type received by '" + conOne.getUser() + "' after it sent a roster request without using the 'ver' attribute (expected clients that do not support roster versioning to be able to retrieve a roster).");
        } catch (XMPPException.XMPPErrorException e) {
            fail("Unexpected response type received by '" + conOne.getUser() + "' after it sent a roster request without using the 'ver' attribute (expected clients that do not support roster versioning to be able to retrieve a roster). Expected a (non-error) result, but instead received an error: " + e.getStanzaError());
        }
    }

    /**
     * Asserts that requesting a versioned roster using an empty 'ver' will cause the server to send a roster item that was added.
     *
     * During the fixture setup of this test:
     *
     * <ol>
     * <li>A new item is added to the roster</li>
     * <li>The roster push that should happen as a direct result of the roster add occurs (this needs to be 'out of the way', as the request for a versioned roster may result in an indistinguishable roster-push).</li>
     * </ol>
     *
     * During the execution of the system under test of this test:
     * <ol>
     * <li>A versioned roster request is placed, using an empty string for a roster version (this should cause all changes to be sent</li>
     * </ol>
     *
     * During the result verification of this test, it is checked that either/or:
     * <ol>
     * <li>The response to the versioned roster request is a non-empty IQ stanza that contains a roster, which includes the item that was added during setup, or</li>
     * <li>the response to the versioned roster request is an empty IQ stanza, in which case a roster push happens that must include the item that was added during setup.</li>
     * </ol>
     *
     * Furthermore, the implementation of this test replaces Smack-based IQRequestHandlers that are registered by the Roster implementation.
     * This is done so that this test can detect roster pushes. The replacement IQRequestHandler delegates to the original request handler,
     * in an effort for Smack's functionality to remain functional. During the fixture teardown, the original IQRequestHandler is restored.
     */
    @SmackIntegrationTest(section = "2.6.3", quote = "the server MUST either return the complete roster as described under Section 2.1.4 (including a 'ver' attribute that signals the latest version) or return an empty IQ-result (thus indicating that any roster modifications will be sent via roster pushes, as described below).")
    public void testRosterVerEmptyGetsRosterAdd() throws Exception
    {
        // Setup test fixture.
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );
        final RosterPacket addItemRequest = new RosterPacket();
        addItemRequest.setType(IQ.Type.set);
        addItemRequest.addRosterItem(new RosterPacket.Item(target, null));

        RosterPushListenerWithTarget.sendRosterChangeAndWaitForResultAndPush(conOne, timeout, addItemRequest); // Wait for the push associated to this set to have arrived (as it must not be triggering the roster push listener that intends to be triggered by the roster-ver related push)

        final RosterPushListenerWithTarget rosterPushHandler = new RosterPushListenerWithTarget();
        final IQRequestHandler oldHandler = conOne.registerIQRequestHandler(rosterPushHandler);
        rosterPushHandler.setDelegate(oldHandler); // Allows Smack internal classes (like Roster) to keep on processing roster changes.
        try {
            // Execute system under test.
            final ResultSyncPoint<RosterPacket, Exception> rosterPushReceived = new ResultSyncPoint<>();
            rosterPushHandler.registerSyncPointFor(rosterPushReceived, target);

            final RosterPacket request = new RosterPacket();
            request.setType(IQ.Type.get);
            request.setVersion("");
            final IQ response = conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result. Either the response contains the roster (has a 'query' element), or there will be roster pushes (in which case the response _wil not_ have a query element).
            if (response instanceof RosterPacket) {
                // contains the roster.
                assertTrue(((RosterPacket) response).getRosterItems().stream().anyMatch(item -> item.getJid().equals(target)), "Expected the roster that was returned to '" + conOne.getUser() + "' in response to a roster request with an empty string value in the 'ver' attribute to include an item for '" + target + "' (but that item was not found on the roster).");
            } else {
                assertResult(rosterPushReceived, "Expected the empty IQ result that was returned to '" + conOne.getUser() + "' in response to a roster request with an empty string value in the 'ver' attribute to be followed up with a roster push for an item for '" + target + "' (but such a roster push was not received).");
            }
        } finally {
            // Clean up test fixture.
            if (oldHandler != null) {
                conOne.registerIQRequestHandler(oldHandler);
            } else {
                conOne.unregisterIQRequestHandler(rosterPushHandler);
            }

            final Roster roster = Roster.getInstanceFor(conOne);
            final RosterEntry entry = roster.getEntry(target);
            if (entry != null) {
                roster.removeEntry(entry);
            }
        }
    }

    /**
     * Asserts that requesting a versioned roster using a 'ver' that represents a roster in a known state will not cause
     * the server to send updates when there are no changes to the roster.
     *
     * During the fixture setup of this test:
     *
     * <ol>
     * <li>A new item is added to the roster</li>
     * <li>The roster push that should happen as a direct result of the roster add occurs (this needs to be 'out of the way', as the request for a versioned roster may result in an indistinguishable roster-push).</li>
     * <li>The roster 'ver' of the state of the roster is recorded.</li>
     * </ol>
     *
     * During the execution of the system under test of this test:
     * <ol>
     * <li>A versioned roster request is placed, using the recorded 'ver'</li>
     * </ol>
     *
     * During the result verification of this test, it is checked that either/or:
     * <ol>
     * <li>The response to the versioned roster request is a non-empty IQ stanza that contains a roster (this is almost certainly a bug in the implementation, but allowable by the specification) which includes the same 'ver' value as the original.</li>
     * <li>the response to the versioned roster request is an empty IQ stanza</li>
     * </ol>
     *
     * Furthermore, the implementation of this test replaces Smack-based IQRequestHandlers that are registered by the Roster implementation.
     * This is done so that this test can detect roster pushes. The replacement IQRequestHandler delegates to the original request handler,
     * in an effort for Smack's functionality to remain functional. During the fixture teardown, the original IQRequestHandler is restored.
     */
    @SmackIntegrationTest(section = "2.6.3", quote = "If roster versioning is enabled and the roster has not been modified since the version ID enumerated by the client, the server will simply not send any roster pushes to the client (until and unless some relevant event triggers a roster push during the lifetime of the client's session).")
    public void testRosterVerStable() throws Exception
    {
        // Setup test fixture.
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );
        final String originalName = "Test Name";

        final RosterPacket addItemRequest = new RosterPacket();
        addItemRequest.setType(IQ.Type.set);
        addItemRequest.addRosterItem(new RosterPacket.Item(target, originalName));

        final String rosterVer = RosterPushListenerWithTarget.sendRosterChangeAndWaitForResultAndPush(conOne, timeout, addItemRequest).getVersion(); // Wait for the push associated to this set to have arrived (as it must not be triggering the roster push listener that intends to be triggered by the roster-ver related push)

        try {
            // Execute system under test.
            final RosterPacket request = new RosterPacket();
            request.setType(IQ.Type.get);
            request.setVersion(rosterVer);
            final IQ response = conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result. Either the response contains the roster (has a 'query' element), or there will be roster pushes (in which case the response _wil not_ have a query element).
            if (response instanceof RosterPacket) {
                // contains the roster.
                assertTrue(((RosterPacket) response).getRosterItems().stream().anyMatch(item -> item.getJid().equals(target) && item.getName().equals(originalName)), "Expected the roster that was returned to '" + conOne.getUser() + "' in response to a roster request with value '" + rosterVer + "' in the 'ver' attribute to include an item for '" + target + "' with a name that equals '" + originalName + "' (but that item was not found on the roster).");
                assertEquals(rosterVer, ((RosterPacket) response).getVersion(), "Unexpected roster 'ver' value in roster that was returned to '" + conOne.getUser() + "' in response to a roster request with 'ver' value '" + rosterVer + "', after which no roster changes were applied (the roster 'ver' value is expected to remain the same, yet a different value has been received)." );
            } else {
                // Implicit success - if the response is _not_ of type RosterPacket (does not have a roster-related child element), then no pushes are likely to be sent.
                // TODO Ideally, this test asserts that no pushes are sent (but we don't want to wait for a timeout, for performance reasons).
            }
        } finally {
            // Clean up test fixture.
            final Roster roster = Roster.getInstanceFor(conOne);
            final RosterEntry entry = roster.getEntry(target);
            if (entry != null) {
                roster.removeEntry(entry);
            }
        }
    }

    /**
     * Asserts that when a roster item has received more than one update, only one push (that represents the final
     * result of those modifications) is sent.
     */
    @SmackIntegrationTest(section = "2.6.3", quote = "If the roster has been modified since the version ID enumerated by the client, the server MUST then send one roster push to the client for each roster item that has been modified since the version ID enumerated by the client. [...] The interim roster pushes would not include all of the intermediate steps, only the final result of all modifications applied to each item")
    public void testRosterInterimPushesAreCondensed() throws Exception
    {
        // Setup test fixture.
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );

        final RosterPacket addItemRequest = new RosterPacket();
        addItemRequest.setType(IQ.Type.set);
        final RosterPacket.Item item1 = new RosterPacket.Item(target, "Original Name");
        item1.addGroupName("Test Group A");
        item1.addGroupName("Test Group B");
        addItemRequest.addRosterItem(item1);

        final RosterPacket changeRequestA = new RosterPacket();
        changeRequestA.setType(IQ.Type.set);
        changeRequestA.addRosterItem(new RosterPacket.Item(target, null));

        final RosterPacket changeRequestB = new RosterPacket();
        changeRequestB.setType(IQ.Type.set);
        final RosterPacket.Item item3 = new RosterPacket.Item(target, "Updated Name");
        item3.addGroupName("Test Group B");
        item3.addGroupName("Test Group C");
        changeRequestB.addRosterItem(item3);

        final RosterPacket changeRequestC = new RosterPacket();
        changeRequestC.setType(IQ.Type.set);
        final RosterPacket.Item item4 = new RosterPacket.Item(target, "Updated Name");
        item3.addGroupName("Test Group C");
        changeRequestC.addRosterItem(item4);

        final List<RosterPacket> rosterPushes = RosterPushListenerWithTarget.sendRosterChangesAndWaitForResultAndPush(conOne, timeout, addItemRequest, changeRequestA, changeRequestB, changeRequestC);
        final String startingPointVer = rosterPushes.get(0).getVersion();

        final RosterPushListenerWithTarget rosterPushHandler = new RosterPushListenerWithTarget();
        final IQRequestHandler oldHandler = conOne.registerIQRequestHandler(rosterPushHandler);
        rosterPushHandler.setDelegate(oldHandler); // Allows Smack internal classes (like Roster) to keep on processing roster changes.
        try {
            // Execute system under test.
            final ResultSyncPoint<RosterPacket, Exception> rosterPushReceived = new ResultSyncPoint<>();
            rosterPushHandler.registerSyncPointFor(rosterPushReceived, target);

            final RosterPacket request = new RosterPacket();
            request.setType(IQ.Type.get);
            request.setVersion(startingPointVer);
            final IQ response = conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result. Either the response contains the roster (has a 'query' element), or there will be roster pushes (in which case the response _wil not_ have a query element).
            if (response instanceof RosterPacket) {
                // Contains the roster, which means we won't get roster pushes.
                throw new TestNotPossibleException("Instead of individual roster pushes, the server sent the entire roster in response to a roster 'ver' request. That's acceptable behavior, but not useful for the purpose of this test.");
            }

            final RosterPacket rosterPush = assertResult(rosterPushReceived, "Expected '" + conOne.getUser() + "' to receive a roster push after requesting the roster using 'ver' value '" + startingPointVer + "' that is known to represent a roster state that was followed by changes. No push was received.");
            assertNotEquals(changeRequestA, rosterPush, "Expected the roster push to be received by '" + conOne.getUser() + "' after requesting the roster using 'ver' value '" + startingPointVer + "' to represent the final result of all modifications that are applied to the roster item for '" + target + "'. Instead, the push that was received represented the first (of several) change.");
            assertEquals(changeRequestC, rosterPush, "Expected the roster push to be received by '" + conOne.getUser() + "' after requesting the roster using 'ver' value '" + startingPointVer + "' to represent the final result of all modifications that are applied to the roster item for '" + target + "'. Instead, the push that was received respresented something unexpected.");
            // Strictly speaking, only the last assertion is required. The assertion before that allows us to give a slightly better error message.
        }
        finally {
            // Clean up test fixture.
            final Roster roster = Roster.getInstanceFor(conOne);
            final RosterEntry entry = roster.getEntry(target);
            if (entry != null) {
                roster.removeEntry(entry);
            }
        }
    }

    /**
     * Asserts that in a sequence of roster changes, the corresponding pushes are sent in the same order.
     */
    @SmackIntegrationTest(section = "2.6.3", quote = "Roster pushes MUST occur in order of modification")
    public void testRosterPushOrder() throws Exception
    {
        // Setup test fixture: send off a list of changes (and record their 'ver' identifiers).
        final BareJid targetStart = JidCreate.bareFrom( Localpart.from("test-target-start-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );
        final RosterPacket changeRequestStart = new RosterPacket();
        changeRequestStart.setType(IQ.Type.set);
        changeRequestStart.addRosterItem(new RosterPacket.Item(targetStart, "Starting Point"));

        final BareJid targetA = JidCreate.bareFrom( Localpart.from("test-target-one-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );
        final RosterPacket changeRequestA = new RosterPacket();
        changeRequestA.setType(IQ.Type.set);
        final RosterPacket.Item itemA = new RosterPacket.Item(targetA, "Test One Name");
        itemA.addGroupName("Test Group Alpha");
        itemA.addGroupName("Test Group Beta");
        changeRequestA.addRosterItem(itemA);

        final BareJid targetB = JidCreate.bareFrom( Localpart.from("test-target-two-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );
        final RosterPacket changeRequestB = new RosterPacket();
        changeRequestB.setType(IQ.Type.set);
        changeRequestB.addRosterItem(new RosterPacket.Item(targetB, null));

        final BareJid targetC = JidCreate.bareFrom( Localpart.from("test-target-three-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );
        final RosterPacket changeRequestC = new RosterPacket();
        changeRequestC.setType(IQ.Type.set);
        final RosterPacket.Item itemC = new RosterPacket.Item(targetC, "Test Three Name");
        itemC.addGroupName("Test Group Alpha");
        changeRequestC.addRosterItem(itemC);

        final BareJid targetD = JidCreate.bareFrom( Localpart.from("test-target-four-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );
        final RosterPacket changeRequestD = new RosterPacket();
        changeRequestD.setType(IQ.Type.set);
        changeRequestD.addRosterItem(new RosterPacket.Item(targetD, "Test Four Name"));

        final BareJid targetE = JidCreate.bareFrom( Localpart.from("test-target-five-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );
        final RosterPacket changeRequestE = new RosterPacket();
        changeRequestE.setType(IQ.Type.set);
        final RosterPacket.Item itemE = new RosterPacket.Item(targetE, null);
        itemE.addGroupName("Test Group Beta");
        itemE.addGroupName("Test Group Gamma");
        changeRequestE.addRosterItem(itemE);

        final String startingPointVer = RosterPushListenerWithTarget.sendRosterChangesAndWaitForResultAndPush(conOne, timeout, changeRequestStart, changeRequestA, changeRequestB, changeRequestC, changeRequestD, changeRequestE)
            .get(0)
            .getVersion();

        final RosterPushListenerForAmount rosterPushHandler = new RosterPushListenerForAmount();
        final IQRequestHandler oldHandler = conOne.registerIQRequestHandler(rosterPushHandler);
        rosterPushHandler.setDelegate(oldHandler); // Allows Smack internal classes (like Roster) to keep on processing roster changes.

        final ResultSyncPoint<List<RosterPacket>, Exception> rosterPushesReceived = new ResultSyncPoint<>();
        rosterPushHandler.registerSyncPointFor(rosterPushesReceived, 5);

        try
        {
            // Execute system under test.
            final RosterPacket request = new RosterPacket();
            request.setType(IQ.Type.get);
            request.setVersion(startingPointVer); // Request all changes after the first change!

            final IQ response = conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result. Either the response contains the roster (has a 'query' element), or there will be roster pushes (in which case the response _wil not_ have a query element).
            if (response instanceof RosterPacket) {
                // Contains the roster, which means we won't get roster pushes.
                throw new TestNotPossibleException("Instead of individual roster pushes, the server sent the entire roster in response to a roster 'ver' request. That's acceptable behavior, but not useful for the purpose of this test.");
            }

            final List<RosterPacket> rosterPushes = assertResult(rosterPushesReceived, "Expected '" + conOne.getUser() + "' to receive a series of five roster pushes after requesting the roster using 'ver' value '" + startingPointVer + "' that is known to represent a roster state that was followed by five changes. Not (any or all) pushes were received.");
            assertEquals(changeRequestA, rosterPushes.get(0), "Expected the first roster push to be received by '" + conOne.getUser() + "' after requesting the roster using 'ver' value '" + startingPointVer + "' to represent the first modification that was applied to the roster (but a different modification was received).");
            assertEquals(changeRequestB, rosterPushes.get(1), "Expected the second roster push to be received by '" + conOne.getUser() + "' after requesting the roster using 'ver' value '" + startingPointVer + "' to represent the second modification that was applied to the roster (but a different modification was received).");
            assertEquals(changeRequestC, rosterPushes.get(2), "Expected the third roster push to be received by '" + conOne.getUser() + "' after requesting the roster using 'ver' value '" + startingPointVer + "' to represent the third modification that was applied to the roster (but a different modification was received).");
            assertEquals(changeRequestD, rosterPushes.get(3), "Expected the fourth roster push to be received by '" + conOne.getUser() + "' after requesting the roster using 'ver' value '" + startingPointVer + "' to represent the fourth modification that was applied to the roster (but a different modification was received).");
            assertEquals(changeRequestE, rosterPushes.get(4), "Expected the fifth roster push to be received by '" + conOne.getUser() + "' after requesting the roster using 'ver' value '" + startingPointVer + "' to represent the fifth modification that was applied to the roster (but a different modification was received).");
        }
        finally
        {
            // Clean up test fixture.
            if (oldHandler != null) {
                conOne.registerIQRequestHandler(oldHandler);
            } else {
                conOne.unregisterIQRequestHandler(rosterPushHandler);
            }

            final Roster roster = Roster.getInstanceFor(conOne);
            final RosterEntry entryStart = roster.getEntry(targetStart);
            if (entryStart != null) {
                roster.removeEntry(entryStart);
            }
            final RosterEntry entryA = roster.getEntry(targetA);
            if (entryA != null) {
                roster.removeEntry(entryA);
            }
            final RosterEntry entryB = roster.getEntry(targetB);
            if (entryB != null) {
                roster.removeEntry(entryB);
            }
            final RosterEntry entryC = roster.getEntry(targetC);
            if (entryC != null) {
                roster.removeEntry(entryC);
            }
            final RosterEntry entryD = roster.getEntry(targetD);
            if (entryD != null) {
                roster.removeEntry(entryD);
            }
            final RosterEntry entryE = roster.getEntry(targetE);
            if (entryE != null) {
                roster.removeEntry(entryE);
            }
        }
    }
}
