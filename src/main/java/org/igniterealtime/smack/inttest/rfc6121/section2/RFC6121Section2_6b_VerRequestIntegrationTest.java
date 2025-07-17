package org.igniterealtime.smack.inttest.rfc6121.section2;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.ResultSyncPoint;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests that verify that behavior defined in section 2.6 "Roster Versioning" of section 2 "Managing the Roster" of RFC6121.
 *
 * Specifically, the tests in this class all assert that when a roster gets updated, a client that requests roster
 * changes 'since' an older version of the roster gets sent those updates.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
@SpecificationReference(document = "RFC6121")
public class RFC6121Section2_6b_VerRequestIntegrationTest extends AbstractSmackIntegrationTest
{
    public RFC6121Section2_6b_VerRequestIntegrationTest(SmackIntegrationTestEnvironment environment) throws SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
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
     * Asserts that either a new roster or a roster push is sent to the client, when it requests the roster using a 'ver' value of a roster that is known to have since that 'ver' received an update (by adding a roster item).
     */
    @SmackIntegrationTest(section = "2.6.3", quote = "the server MUST either return the complete roster as described under Section 2.1.4 (including a 'ver' attribute that signals the latest version) or return an empty IQ-result (thus indicating that any roster modifications will be sent via roster pushes, as described below). [...] When roster versioning is enabled, the server MUST include the updated roster version with each roster push.")
    public void testRosterItemAddition() throws Exception
    {
        // Setup test fixture.
        final BareJid targetOne = JidCreate.bareFrom( Localpart.from("test-targetone-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );
        final BareJid targetTwo = JidCreate.bareFrom( Localpart.from("test-targettwo-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );

        final RosterPacket startingPoint = new RosterPacket();
        startingPoint.setType(IQ.Type.set);
        startingPoint.addRosterItem(new RosterPacket.Item(targetOne, null));

        final RosterPacket change = new RosterPacket();
        change.setType(IQ.Type.set);
        change.addRosterItem(new RosterPacket.Item(targetTwo, null));

        // Delegate 'Execute System Under test' and 'Verify result'.
        doTestRequestUpdatedRoster(startingPoint, change, "a new item for '" + targetTwo + "' was added");
    }

    /**
     * Asserts that either a new roster or a roster push is sent to the client, when it requests the roster using a 'ver' value of a roster that is known to have since that 'ver' received an update (by removing a roster item).
     */
    @SmackIntegrationTest(section = "2.6.3", quote = "the server MUST either return the complete roster as described under Section 2.1.4 (including a 'ver' attribute that signals the latest version) or return an empty IQ-result (thus indicating that any roster modifications will be sent via roster pushes, as described below). [...] When roster versioning is enabled, the server MUST include the updated roster version with each roster push.")
    public void testRosterItemRemoval() throws Exception
    {
        // Setup test fixture.
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-targetone-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );

        final RosterPacket startingPoint = new RosterPacket();
        startingPoint.setType(IQ.Type.set);
        startingPoint.addRosterItem(new RosterPacket.Item(target, null));

        final RosterPacket change = new RosterPacket();
        change.setType(IQ.Type.set);
        final RosterPacket.Item item = new RosterPacket.Item(target, null);
        item.setItemType(RosterPacket.ItemType.remove);
        change.addRosterItem(item);

        // Delegate 'Execute System Under test' and 'Verify result'.
        doTestRequestUpdatedRoster(startingPoint, change, "the pre-existing item '" + target + "' was removed");
    }

    /**
     * Asserts that either a new roster or a roster push is sent to the client, when it requests the roster using a 'ver' value of a roster that is known to have since that 'ver' received an update (by adding a name/handle to a roster item).
     */
    @SmackIntegrationTest(section = "2.6.3", quote = "the server MUST either return the complete roster as described under Section 2.1.4 (including a 'ver' attribute that signals the latest version) or return an empty IQ-result (thus indicating that any roster modifications will be sent via roster pushes, as described below). [...] When roster versioning is enabled, the server MUST include the updated roster version with each roster push.")
    public void testRosterItemModificationHandleAddition() throws Exception
    {
        // Setup test fixture.
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );

        final RosterPacket startingPoint = new RosterPacket();
        startingPoint.setType(IQ.Type.set);
        startingPoint.addRosterItem(new RosterPacket.Item(target, null));

        final RosterPacket change = new RosterPacket();
        change.setType(IQ.Type.set);
        change.addRosterItem(new RosterPacket.Item(target, "FooBar"));

        // Delegate 'Execute System Under test' and 'Verify result'.
        doTestRequestUpdatedRoster(startingPoint, change, "the name/handle of item '" + target + "' - previously unset - was set to 'FooBar'");
    }

    /**
     * Asserts that either a new roster or a roster push is sent to the client, when it requests the roster using a 'ver' value of a roster that is known to have since that 'ver' received an update (by changing the name/handle of a roster item).
     */
    @SmackIntegrationTest(section = "2.6.3", quote = "the server MUST either return the complete roster as described under Section 2.1.4 (including a 'ver' attribute that signals the latest version) or return an empty IQ-result (thus indicating that any roster modifications will be sent via roster pushes, as described below). [...] When roster versioning is enabled, the server MUST include the updated roster version with each roster push.")
    public void testRosterItemModificationHandleModification() throws Exception
    {
        // Setup test fixture.
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );

        final RosterPacket startingPoint = new RosterPacket();
        startingPoint.setType(IQ.Type.set);
        startingPoint.addRosterItem(new RosterPacket.Item(target, "Old Name"));

        final RosterPacket change = new RosterPacket();
        change.setType(IQ.Type.set);
        change.addRosterItem(new RosterPacket.Item(target, "Updated Name"));

        // Delegate 'Execute System Under test' and 'Verify result'.
        doTestRequestUpdatedRoster(startingPoint, change, "the name/handle of item '" + target + "' was changed from 'Old name' to 'Updated Name'");
    }

    /**
     * Asserts that either a new roster or a roster push is sent to the client, when it requests the roster using a 'ver' value of a roster that is known to have since that 'ver' received an update (by removing the name/handle of a roster item).
     */
    @SmackIntegrationTest(section = "2.6.3", quote = "the server MUST either return the complete roster as described under Section 2.1.4 (including a 'ver' attribute that signals the latest version) or return an empty IQ-result (thus indicating that any roster modifications will be sent via roster pushes, as described below). [...] When roster versioning is enabled, the server MUST include the updated roster version with each roster push.")
    public void testRosterItemModificationHandleRemoval() throws Exception
    {
        // Setup test fixture.
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );

        final RosterPacket startingPoint = new RosterPacket();
        startingPoint.setType(IQ.Type.set);
        startingPoint.addRosterItem(new RosterPacket.Item(target, "FooBar"));

        final RosterPacket change = new RosterPacket();
        change.setType(IQ.Type.set);
        change.addRosterItem(new RosterPacket.Item(target, null));

        // Delegate 'Execute System Under test' and 'Verify result'.
        doTestRequestUpdatedRoster(startingPoint, change, "the name/handle of item '" + target + "' - previously 'FooBar' - was removed");
    }

    /**
     * Asserts that either a new roster or a roster push is sent to the client, when it requests the roster using a 'ver' value of a roster that is known to have since that 'ver' received an update (by adding a group to a roster item).
     */
    @SmackIntegrationTest(section = "2.6.3", quote = "the server MUST either return the complete roster as described under Section 2.1.4 (including a 'ver' attribute that signals the latest version) or return an empty IQ-result (thus indicating that any roster modifications will be sent via roster pushes, as described below). [...] When roster versioning is enabled, the server MUST include the updated roster version with each roster push.")
    public void testRosterItemModificationGroupAddition() throws Exception
    {
        // Setup test fixture.
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );

        final RosterPacket startingPoint = new RosterPacket();
        startingPoint.setType(IQ.Type.set);
        startingPoint.addRosterItem(new RosterPacket.Item(target, null));

        final RosterPacket change = new RosterPacket();
        change.setType(IQ.Type.set);
        final RosterPacket.Item item = new RosterPacket.Item(target, null);
        item.addGroupName("Test Group");
        change.addRosterItem(item);

        // Delegate 'Execute System Under test' and 'Verify result'.
        doTestRequestUpdatedRoster(startingPoint, change, "item '" + target + "' - previously not in any group - was added to a group called 'Test Group'");
    }

    /**
     * Asserts that either a new roster or a roster push is sent to the client, when it requests the roster using a 'ver' value of a roster that is known to have since that 'ver' received an update (by adding another group to a roster item).
     */
    @SmackIntegrationTest(section = "2.6.3", quote = "the server MUST either return the complete roster as described under Section 2.1.4 (including a 'ver' attribute that signals the latest version) or return an empty IQ-result (thus indicating that any roster modifications will be sent via roster pushes, as described below). [...] When roster versioning is enabled, the server MUST include the updated roster version with each roster push.")
    public void testRosterItemModificationGroupAdditionAnother() throws Exception
    {
        // Setup test fixture.
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );

        final RosterPacket startingPoint = new RosterPacket();
        startingPoint.setType(IQ.Type.set);
        final RosterPacket.Item startItem = new RosterPacket.Item(target, null);
        startItem.addGroupName("Test Group");
        startingPoint.addRosterItem(startItem);

        final RosterPacket change = new RosterPacket();
        change.setType(IQ.Type.set);
        final RosterPacket.Item changeItem = new RosterPacket.Item(target, null);
        changeItem.addGroupName("Test Group");
        changeItem.addGroupName("Additional Group");
        change.addRosterItem(changeItem);

        // Delegate 'Execute System Under test' and 'Verify result'.
        doTestRequestUpdatedRoster(startingPoint, change, "item '" + target + "' - already in group 'Test Group' - was added to an additional group called 'Additional Group'");
    }

    /**
     * Asserts that either a new roster or a roster push is sent to the client, when it requests the roster using a 'ver' value of a roster that is known to have since that 'ver' received an update (by changing the group of a roster item).
     */
    @SmackIntegrationTest(section = "2.6.3", quote = "the server MUST either return the complete roster as described under Section 2.1.4 (including a 'ver' attribute that signals the latest version) or return an empty IQ-result (thus indicating that any roster modifications will be sent via roster pushes, as described below). [...] When roster versioning is enabled, the server MUST include the updated roster version with each roster push.")
    public void testRosterItemModificationGroupModification() throws Exception
    {
        // Setup test fixture.
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );

        final RosterPacket startingPoint = new RosterPacket();
        startingPoint.setType(IQ.Type.set);
        final RosterPacket.Item startItem = new RosterPacket.Item(target, null);
        startItem.addGroupName("Test Group");
        startingPoint.addRosterItem(startItem);

        final RosterPacket change = new RosterPacket();
        change.setType(IQ.Type.set);
        final RosterPacket.Item changeItem = new RosterPacket.Item(target, null);
        changeItem.addGroupName("Changed Group");
        change.addRosterItem(changeItem);

        // Delegate 'Execute System Under test' and 'Verify result'.
        doTestRequestUpdatedRoster(startingPoint, change, "item '" + target + "' was removed from group 'Test Group' and was added to a group called 'Changed Group'");
    }

    /**
     * Asserts that either a new roster or a roster push is sent to the client, when it requests the roster using a 'ver' value of a roster that is known to have since that 'ver' received an update (by removing the roster item from one of its groups).
     */
    @SmackIntegrationTest(section = "2.6.3", quote = "the server MUST either return the complete roster as described under Section 2.1.4 (including a 'ver' attribute that signals the latest version) or return an empty IQ-result (thus indicating that any roster modifications will be sent via roster pushes, as described below). [...] When roster versioning is enabled, the server MUST include the updated roster version with each roster push.")
    public void testRosterItemModificationGroupRemoval() throws Exception
    {
        // Setup test fixture.
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );

        final RosterPacket startingPoint = new RosterPacket();
        startingPoint.setType(IQ.Type.set);
        final RosterPacket.Item startItem = new RosterPacket.Item(target, null);
        startItem.addGroupName("Test Group");
        startItem.addGroupName("Additional Group");
        startingPoint.addRosterItem(startItem);

        final RosterPacket change = new RosterPacket();
        change.setType(IQ.Type.set);
        final RosterPacket.Item changeItem = new RosterPacket.Item(target, null);
        changeItem.addGroupName("Test Group");
        change.addRosterItem(changeItem);

        // Delegate 'Execute System Under test' and 'Verify result'.
        doTestRequestUpdatedRoster(startingPoint, change, "item '" + target + "' was removed from group 'Additional Group', but remains in group 'Test Group'");
    }

    /**
     * Asserts that either a new roster or a roster push is sent to the client, when it requests the roster using a 'ver' value of a roster that is known to have since that 'ver' received an update (by removing the roster item from its last group).
     */
    @SmackIntegrationTest(section = "2.6.3", quote = "the server MUST either return the complete roster as described under Section 2.1.4 (including a 'ver' attribute that signals the latest version) or return an empty IQ-result (thus indicating that any roster modifications will be sent via roster pushes, as described below). [...] When roster versioning is enabled, the server MUST include the updated roster version with each roster push.")
    public void testRosterItemModificationGroupRemovalLast() throws Exception
    {
        // Setup test fixture.
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );

        final RosterPacket startingPoint = new RosterPacket();
        startingPoint.setType(IQ.Type.set);
        startingPoint.addRosterItem(new RosterPacket.Item(target, "FooBar"));

        final RosterPacket change = new RosterPacket();
        change.setType(IQ.Type.set);
        change.addRosterItem(new RosterPacket.Item(target, null));

        // Delegate 'Execute System Under test' and 'Verify result'.
        doTestRequestUpdatedRoster(startingPoint, change, "item '" + target + "' was removed from group 'Test Group' - which was the only group it was in");
    }

    /**
     * Asserts that requesting a versioned roster using a 'ver' that represents a roster in a state known to have been
     * updated will cause the server to send an appropriate update.
     *
     * During the fixture setup of this test:
     *
     * <ol>
     * <li>An initial roster change is applied</li>
     * <li>The roster push that should happen as a direct result of this, is waited for</li>
     * <li>The roster 'ver' of the state of the roster is recorded.</li>
     * <li>An second roster change is applied</li>
     * <li>The roster push that should happen as a direct result of this, is waited for</li>
     * <li>The roster 'ver' of the state of the roster is recorded.</li>
     * </ul>
     *
     * During the execution of the system under test of this test:
     * <ol>
     * <li>A versioned roster is requested, using the 'ver' recorded after the first roster push (this represents the roster state without the second change).</li>
     * </ol>
     *
     * During the result verification of this test, it is checked that either/or:
     * <ol>
     * <li>The response to the versioned roster request is a non-empty IQ stanza that contains a roster, which includes the (second) change to the roster that was applied during setup, or</li>
     * <li>the response to the versioned roster request is an empty IQ stanza, in which case a roster push is happening that must reflect the (second) change ot the roster that was applied during setup.</li>
     * </ol>
     * And additionally:
     * <ol>
     * <li>that the received roster or roster push uses a 'ver' attribute value that equals the 'latest' version of the roster (as represented by the 'ver' value that was recorded after the (second) change was applied).</li>
     * </ol>
     *
     * The implementation of this test replaces Smack-based IQRequestHandlers that are registered by the Roster implementation.
     * This is done so that this test can detect roster pushes. The replacement IQRequestHandler delegates to the original request handler,
     * in an effort for Smack's functionality to remain functional. During the fixture teardown, the original IQRequestHandler is restored.
     * Any items that are added by the test are removed again from the roster.
     *
     * @param startingPoint A roster item that is used as the initial state.
     * @param change A roster item that is an update of the initial state.
     * @param descriptionOfChange A human-readable description of the roster item change (used for logging)
     */
    protected void doTestRequestUpdatedRoster(final RosterPacket startingPoint, final RosterPacket change, final String descriptionOfChange) throws Exception
    {
        if (startingPoint == null || startingPoint.getRosterItems().size() != 1 || change == null || change.getRosterItems().size() != 1) {
            throw new IllegalArgumentException();
        }

        // Setup test fixture
        final BareJid startingPointTarget = startingPoint.getRosterItems().iterator().next().getJid();
        final RosterPacket.Item changeItem = change.getRosterItems().iterator().next();
        final BareJid changeTarget = changeItem.getJid();

        // Setup test fixture: get the roster in a 'starting point' state.
        final String rosterVerWithoutModification = RosterPushListenerWithTarget.sendRosterChangeAndWaitForResultAndPush(conOne, timeout, startingPoint).getVersion();

        // Setup test fixture: apply a roster change.
        final String rosterVerAfterModification = RosterPushListenerWithTarget.sendRosterChangeAndWaitForResultAndPush(conOne, timeout, change).getVersion();

        final RosterPushListenerWithTarget rosterPushHandler = new RosterPushListenerWithTarget();
        final IQRequestHandler oldHandler = conOne.registerIQRequestHandler(rosterPushHandler);
        rosterPushHandler.setDelegate(oldHandler); // Allows Smack internal classes (like Roster) to keep on processing roster changes.

        try
        {
            // Execute system under test: ask for the roster, using a 'ver' that represents the 'old' state.
            final ResultSyncPoint<RosterPacket, Exception> rosterPushReceived = new ResultSyncPoint<>();
            rosterPushHandler.registerSyncPointFor(rosterPushReceived, changeTarget);

            final RosterPacket request = new RosterPacket();
            request.setType(IQ.Type.get);
            request.setVersion(rosterVerWithoutModification);
            final IQ response = conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result: Either the response contains the roster (has a 'query' element), or there will be roster pushes (in which case the response _wil not_ have a query element).
            if (response instanceof RosterPacket) {
                // Result contains the roster.
                if (RosterPacket.ItemType.remove == changeItem.getItemType()) {
                    // Item was removed: check that it's no longer on the roster.
                    assertFalse(((RosterPacket) response).getRosterItems().stream().anyMatch(item -> item.getJid().equals(changeTarget)), "Expected the roster that was returned to '" + conOne.getUser() + "' in response to a roster request (with value '" + rosterVerWithoutModification + "' in the 'ver' attribute) to no longer include an item for '" + changeTarget + "' as that item was removed in a later version of the roster (but that item was still found on the roster).");
                } else {
                    // Item was updated: check that at the roster contains an item that matches the update.
                    assertTrue(((RosterPacket) response).getRosterItems().contains(changeItem), "Expected the roster that was returned to '" + conOne.getUser() + "' in response to a roster request (with value '" + rosterVerWithoutModification + "' in the 'ver' attribute) to include an item for '" + changeTarget + "' including the changes (" + descriptionOfChange + ") that were applied to it (but those changes were not found on the roster).");
                }
                assertEquals(rosterVerAfterModification, ((RosterPacket) response).getVersion(), "Expected the roster that was returned to '" + conOne.getUser() + "' in response to a roster request (with value '" + rosterVerWithoutModification + "' in the 'ver' attribute) to include a 'ver' attribute that signals the latest version (but a different 'ver' value than the one known to be the latest version was found instead).");
            } else {
                // Result was empty: must be followed up with a roster push.
                final RosterPacket pushedRosterPacket = assertResult(rosterPushReceived, "Expected the empty IQ result that was returned to '" + conOne.getUser() + "' in response to a roster request (with value '" + rosterVerWithoutModification + "' in the 'ver' attribute) to be followed up with a roster push for an item for '" + changeTarget + "' that is known to have received a change since the provided 'ver' value (but no such roster push was received).");
                assertTrue(pushedRosterPacket.getRosterItems().contains(changeItem), "Expected the roster push that was sent to '" + conOne.getUser() + "' in response to a roster request (with value '" + rosterVerWithoutModification + "' in the 'ver' attribute) to include an item for '" + changeTarget + "' including the changes (" + descriptionOfChange + ") that were applied to it (but those changes were not found in the roster push).");
                assertEquals(rosterVerAfterModification, pushedRosterPacket.getVersion(), "Expected the roster push that was sent to '" + conOne.getUser() + "' in response to a roster request (with value '" + rosterVerWithoutModification + "' in the 'ver' attribute) to include a 'ver' attribute that signals the latest version (but a different 'ver' value than the one known to be the latest version was found instead).");
            }

            // Verify result.
            assertNotEquals(rosterVerWithoutModification, rosterVerAfterModification, "Expected that the roster 'ver' value that is pushed to '" + conOne.getUser() + "' after its roster was changed (" + descriptionOfChange + ") is different from the value that was pushed prior the change, but the same roster 'ver' value was received!");
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
            final RosterEntry startingPointEntry = roster.getEntry(startingPointTarget);
            if (startingPointEntry != null) {
                roster.removeEntry(startingPointEntry);
            }
            if (!startingPointTarget.equals(changeTarget)) {
                final RosterEntry changeEntry = roster.getEntry(changeTarget);
                if (changeEntry != null) {
                    roster.removeEntry(changeEntry);
                }
            }
        }
    }
}
