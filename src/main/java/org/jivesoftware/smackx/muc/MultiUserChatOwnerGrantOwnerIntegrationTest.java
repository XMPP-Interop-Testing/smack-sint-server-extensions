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
package org.jivesoftware.smackx.muc;

import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.muc.packet.MUCAdmin;
import org.jivesoftware.smackx.muc.packet.MUCItem;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for section "10.3 Owner Use Cases: Granting Owner Status" of XEP-0045: "Multi-User Chat"
 *
 * @see <a href="https://xmpp.org/extensions/xep-0045.html#grantowner">XEP-0045 Section 10.3</a>
 */
@SpecificationReference(document = "XEP-0045", version = "1.34.6")
public class MultiUserChatOwnerGrantOwnerIntegrationTest extends AbstractMultiUserChatIntegrationTest
{
    public MultiUserChatOwnerGrantOwnerIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws SmackException.NoResponseException, XMPPException.XMPPErrorException,
        SmackException.NotConnectedException, InterruptedException, TestNotPossibleException, XmppStringprepException, MultiUserChatException.MucAlreadyJoinedException, MultiUserChatException.MissingMucCreationAcknowledgeException, MultiUserChatException.NotAMucServiceException
    {
        super(environment);

        // The specification reads "If allowed by an implementation, an owner MAY grant owner status to another user"
        // which suggests that this is optional functionality. The specification does not explicitly say how to test for
        // support. This implementation will use any XMPP error in response to a change request as an indication that
        // the feature is not supported by the server under test.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-owner-grant-support");
        final MultiUserChat muc = mucManagerOne.getMultiUserChat(mucAddress);
        createMuc(muc, Resourcepart.from("owner-" + randomString));
        try {
            muc.grantOwnership(conTwo.getUser().asBareJid());
        } catch (XMPPException.XMPPErrorException e) {
            throw new TestNotPossibleException("Service does not support granting of owner status functionality.");
        } finally {
            tryDestroy(muc);
        }
    }

    /**
     * Verifies that an owner can grant owner status to a user that is currently not in the room.
     */
    @SmackIntegrationTest(section = "10.3", quote = "an owner MAY grant owner status to another user; this is done by changing the user's affiliation to \"owner\"")
    public void testGrantOwner() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-owner-grant");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        
        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            // Execute system under test.
            final MUCAdmin request = new MUCAdmin();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.addItem(new MUCItem(MUCAffiliation.owner, conTwo.getUser().asBareJid()));
            try {
                conOne.sendIqRequestAndWaitForResponse(request);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to grant owner status to '" + conTwo.getUser().asBareJid() + "' in '" + mucAddress + "' (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }
    
    /**
     * Verifies that an owner can grant owner status to a user that is currently a participant in the room.
     */
    @SmackIntegrationTest(section = "10.3", quote = "an owner MAY grant owner status to another user; this is done by changing the user's affiliation to \"owner\"")
    public void testGrantOwnerWhileInRoom() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-owner-grant-inroom");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);

        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            final SimpleResultSyncPoint ownerSeesTarget = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        ownerSeesTarget.signal();
                    }
                }
            });
            mucAsSeenByTarget.join(nicknameTarget);
            ownerSeesTarget.waitForResult(timeout);

            // Execute system under test.
            final MUCAdmin request = new MUCAdmin();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.addItem(new MUCItem(MUCAffiliation.owner, conTwo.getUser().asBareJid()));
            try {
                conOne.sendIqRequestAndWaitForResponse(request);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to grant owner status to '" + conTwo.getUser().asBareJid() + "' (that is currently joined as '" + nicknameTarget+ "') in '" + mucAddress + "' (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that an owner can grant owner status to a user that is currently not in the room, while providing a 'reason'.
     */
    @SmackIntegrationTest(section = "10.3", quote = "an owner MAY grant owner status to another user; this is done by changing the user's affiliation to \"owner\" [...] The <reason/> element is OPTIONAL.")
    public void testGrantOwnerOptionalReason() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-owner-grant-reason");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            // Execute system under test.
            final MUCAdmin request = new MUCAdmin();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.addItem(new MUCItem(MUCAffiliation.owner, conTwo.getUser().asBareJid(), "Granting Owner Status as part of an integration test."));
            try {
                conOne.sendIqRequestAndWaitForResponse(request);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to grant owner status to '" + conTwo.getUser().asBareJid() + "' in '" + mucAddress + "' (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that an owner can grant owner status to a user that is currently a participant in the room, while providing a 'reason'.
     */
    @SmackIntegrationTest(section = "10.3", quote = "an owner MAY grant owner status to another user; this is done by changing the user's affiliation to \"owner\" [...] The <reason/> element is OPTIONAL.")
    public void testGrantOwnerOptionalReasonWhileInRoom() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-owner-grant-reason-inroom");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);

        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            final SimpleResultSyncPoint ownerSeesTarget = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        ownerSeesTarget.signal();
                    }
                }
            });
            mucAsSeenByTarget.join(nicknameTarget);
            ownerSeesTarget.waitForResult(timeout);

            // Execute system under test.
            final MUCAdmin request = new MUCAdmin();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.addItem(new MUCItem(MUCAffiliation.owner, conTwo.getUser().asBareJid(), "Granting Owner Status as part of an integration test."));
            try {
                conOne.sendIqRequestAndWaitForResponse(request);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to grant owner status to '" + conTwo.getUser().asBareJid() + "' (that is currently joined as '" + nicknameTarget+ "') in '" + mucAddress + "' (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    // TODO enable these tests after https://github.com/xsf/xeps/pull/1370 gets merged. Until then, the specification does not seem to restrict granting of owner status to owners.
//    /**
//     * Verifies that a non-owner, non-joined user cannot grant someone owner status (when the target is not in the room).
//     */
//    @SmackIntegrationTest(section = "10.3", quote = "If the <user@host> of the 'from' address does not match the bare JID of a room owner, the service MUST return a <forbidden/> error to the sender.")
//    public void testUserNotAllowedToGrantOwnerStatus() throws Exception
//    {
//        // Setup test fixture.
//        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-owner-grant-user-notallowed");
//        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
//
//        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
//
//        createMuc(mucAsSeenByOwner, nicknameOwner);
//        try {
//            // Execute system under test.
//            final MUCAdmin request = new MUCAdmin();
//            request.setTo(mucAddress);
//            request.setType(IQ.Type.set);
//            request.addItem(new MUCItem(MUCAffiliation.owner, conThree.getUser().asBareJid(), "Granting Owner Status as part of an integration test."));
//
//            // Verify result.
//            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
//                conTwo.sendIqRequestAndWaitForResponse(request);
//            }, "Expected an error after '" + conTwo.getUser() + "' (that is not an owner) tried to grant owner status to another user ('" + conThree.getUser().asBareJid() + "') in room '" + mucAddress + "' (but none occurred).");
//            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' after it tried to grant owner status to user ('" + conThree.getUser().asBareJid() + "') in room '" + mucAddress + "' while not being an owner.");
//        } finally {
//            // Tear down test fixture.
//            tryDestroy(mucAsSeenByOwner);
//        }
//    }
//
//    /**
//     * Verifies that a non-owner, non-joined user cannot grant someone owner status (when the target is in the room).
//     */
//    @SmackIntegrationTest(section = "10.3", quote = "If the <user@host> of the 'from' address does not match the bare JID of a room owner, the service MUST return a <forbidden/> error to the sender.")
//    public void testUserNotAllowedToGrantOwnerStatusInRoom() throws Exception
//    {
//        // Setup test fixture.
//        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-owner-grant-user-notallowed-inroom");
//        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
//        final MultiUserChat mucAsSeenByTarget = mucManagerThree.getMultiUserChat(mucAddress);
//
//        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
//        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);
//
//        createMuc(mucAsSeenByOwner, nicknameOwner);
//        try {
//            mucAsSeenByTarget.join(nicknameTarget);
//
//            // Execute system under test.
//            final MUCAdmin request = new MUCAdmin();
//            request.setTo(mucAddress);
//            request.setType(IQ.Type.set);
//            request.addItem(new MUCItem(MUCAffiliation.owner, conThree.getUser().asBareJid(), "Granting Owner Status as part of an integration test."));
//
//            // Verify result.
//            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
//                conTwo.sendIqRequestAndWaitForResponse(request);
//            }, "Expected an error after '" + conTwo.getUser() + "' (that is not an owner) tried to grant owner status to another user ('" + conThree.getUser().asBareJid() + "', joined as '" + nicknameTarget + "') in room '" + mucAddress + "' (but none occurred).");
//            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' after it tried to grant owner status to user ('" + conThree.getUser().asBareJid() + "', joined as '" + nicknameTarget + "') in room '" + mucAddress + "' while not being an owner.");
//        } finally {
//            // Tear down test fixture.
//            tryDestroy(mucAsSeenByOwner);
//        }
//    }
//
//    /**
//     * Verifies that a non-owner (that has joined the room) cannot grant someone owner status (when the target is not in the room).
//     */
//    @SmackIntegrationTest(section = "10.3", quote = "If the <user@host> of the 'from' address does not match the bare JID of a room owner, the service MUST return a <forbidden/> error to the sender.")
//    public void testParticipantNotAllowedToGrantOwnerStatus() throws Exception
//    {
//        // Setup test fixture.
//        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-owner-grant-participant-notallowed");
//        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
//        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);
//
//        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
//        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);
//
//        createMuc(mucAsSeenByOwner, nicknameOwner);
//        try {
//            mucAsSeenByParticipant.join(nicknameParticipant);
//
//            // Execute system under test.
//            final MUCAdmin request = new MUCAdmin();
//            request.setTo(mucAddress);
//            request.setType(IQ.Type.set);
//            request.addItem(new MUCItem(MUCAffiliation.owner, conThree.getUser().asBareJid(), "Granting Owner Status as part of an integration test."));
//
//            // Verify result.
//            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
//                conTwo.sendIqRequestAndWaitForResponse(request);
//            }, "Expected an error after '" + conTwo.getUser() + "' (that is not an owner, but joined the room as '" + nicknameParticipant + "') tried to grant owner status to another user ('" + conThree.getUser().asBareJid() + "') in room '" + mucAddress + "' (but none occurred).");
//            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' (joined as '" + nicknameParticipant + "') after it tried to grant owner status to user ('" + conThree.getUser().asBareJid() + "') in room '" + mucAddress + "' while not being an owner.");
//        } finally {
//            // Tear down test fixture.
//            tryDestroy(mucAsSeenByOwner);
//        }
//    }
//
//    /**
//     * Verifies that a non-owner (that has joined the room) cannot grant someone owner status (when the target is in the room).
//     */
//    @SmackIntegrationTest(section = "10.3", quote = "If the <user@host> of the 'from' address does not match the bare JID of a room owner, the service MUST return a <forbidden/> error to the sender.")
//    public void testParticipantNotAllowedToGrantOwnerStatusInRoom() throws Exception
//    {
//        // Setup test fixture.
//        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-owner-grant-participant-notallowed-inroom");
//        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
//        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);
//        final MultiUserChat mucAsSeenByTarget = mucManagerThree.getMultiUserChat(mucAddress);
//
//        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
//        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);
//        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);
//
//        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);
//
//        createMuc(mucAsSeenByOwner, nicknameOwner);
//        try {
//            mucAsSeenByParticipant.join(nicknameParticipant);
//
//            final SimpleResultSyncPoint participantSeesTarget = new SimpleResultSyncPoint();
//            mucAsSeenByParticipant.addParticipantStatusListener(new ParticipantStatusListener() {
//                @Override
//                public void joined(EntityFullJid participant) {
//                    if (participant.equals(targetMucAddress)) {
//                        participantSeesTarget.signal();
//                    }
//                }
//            });
//            mucAsSeenByTarget.join(nicknameTarget);
//            participantSeesTarget.waitForResult(timeout);
//
//            // Execute system under test.
//            final MUCAdmin request = new MUCAdmin();
//            request.setTo(mucAddress);
//            request.setType(IQ.Type.set);
//            request.addItem(new MUCItem(MUCAffiliation.owner, conThree.getUser().asBareJid(), "Granting Owner Status as part of an integration test."));
//
//            // Verify result.
//            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
//                conTwo.sendIqRequestAndWaitForResponse(request);
//            }, "Expected an error after '" + conTwo.getUser() + "' (that is not an owner, but joined the room as '" + nicknameParticipant + "') tried to grant owner status to another user ('" + conThree.getUser().asBareJid() + "', joined as '" + nicknameTarget + "') in room '" + mucAddress + "' (but none occurred).");
//            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' (joined as '" + nicknameParticipant + "') after it tried to grant owner status to user ('" + conThree.getUser().asBareJid() + "', joined as '" + nicknameTarget + "') in room '" + mucAddress + "' while not being an owner.");
//        } finally {
//            // Tear down test fixture.
//            tryDestroy(mucAsSeenByOwner);
//        }
//    }

    /**
     * Verifies that a (bare) JID that is granted owner status by an owner appears on the Owner List.
     */
    @SmackIntegrationTest(section = "10.3", quote = "an owner MAY grant owner status to another user [...] The service MUST add the user to the owner list [...]")
    public void testOwnerOnOwnerList() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-owner-on-ownerlist");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            // Execute system under test.
            final MUCAdmin request = new MUCAdmin();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.addItem(new MUCItem(MUCAffiliation.owner, conTwo.getUser().asBareJid()));

            conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result.
            assertTrue(mucAsSeenByOwner.getOwners().stream().anyMatch(owner -> owner.getJid().equals(conTwo.getUser().asBareJid())), "Expected '" + conTwo.getUser().asBareJid() + "' to be on the Owner List after they were granted owner status by '" + conOne.getUser() + "' in '" + mucAddress + "' (but the JID does not appear on the Owner List).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that occupants are notified when an existing occupant is granted owner status.
     */
    @SmackIntegrationTest(section = "10.3", quote = "If the user is in the room, the service MUST then send updated presence from this individual to all occupants, indicating the granting of owner status by including an <x/> element qualified by the 'http://jabber.org/protocol/muc#user' namespace and containing an <item/> child with the 'affiliation' attribute set to a value of \"owner\" [...]")
    public void testOccupantsInformed() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-owner-broadcast");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);

        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByParticipant.join(nicknameParticipant);

            final SimpleResultSyncPoint ownerSeesTarget = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        ownerSeesTarget.signal();
                    }
                }
            });

            final SimpleResultSyncPoint participantSeesTarget = new SimpleResultSyncPoint();
            mucAsSeenByParticipant.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        participantSeesTarget.signal();
                    }
                }
            });
            mucAsSeenByTarget.join(nicknameTarget);
            ownerSeesTarget.waitForResult(timeout);
            participantSeesTarget.waitForResult(timeout);

            final SimpleResultSyncPoint targetSeesGrant = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint ownerSeesGrant = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint participantSeeSGrant = new SimpleResultSyncPoint();
            mucAsSeenByTarget.addUserStatusListener(new UserStatusListener() {
                @Override
                public void ownershipGranted() {
                    targetSeesGrant.signal();
                }
            });
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void ownershipGranted(EntityFullJid participant) {
                    if (targetMucAddress.equals(participant)) {
                        ownerSeesGrant.signal();
                    }
                }
            });
            mucAsSeenByParticipant.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void ownershipGranted(EntityFullJid participant) {
                    if (targetMucAddress.equals(participant)) {
                        participantSeeSGrant.signal();
                    }
                }
            });

            // Execute system under test.
            final MUCAdmin request = new MUCAdmin();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.addItem(new MUCItem(MUCAffiliation.owner, conThree.getUser().asBareJid()));

            conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result.
            assertResult(targetSeesGrant, "Expected '" + conThree.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the granting of owner status, after they are granted owner status by '" + conOne.getUser() + "' in '" + mucAddress + "' (but no such stanza was received).");
            assertResult(ownerSeesGrant, "Expected '" + conOne.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the granting of owner status, after '" + targetMucAddress + "' is granted owner status by '" + conOne.getUser() + "' in '" + mucAddress + "' (but no such stanza was received).");
            assertResult(participantSeeSGrant, "Expected '" + conTwo.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the granting of owner status, after '" + targetMucAddress + "' is granted owner status by '" + conOne.getUser() + "' in '" + mucAddress + "' (but no such stanza was received).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }
}
