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
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smackx.muc.packet.MUCAdmin;
import org.jivesoftware.smackx.muc.packet.MUCItem;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for section "10.7 Owner Use Cases: Revoking Admin Status" of XEP-0045: "Multi-User Chat"
 *
 * @see <a href="https://xmpp.org/extensions/xep-0045.html#revokeadmin">XEP-0045 Section 10.7</a>
 */
@SpecificationReference(document = "XEP-0045", version = "1.34.6")
public class MultiUserChatOwnerRevokeAdminIntegrationTest extends AbstractMultiUserChatIntegrationTest
{
    public MultiUserChatOwnerRevokeAdminIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws SmackException.NoResponseException, XMPPException.XMPPErrorException,
        SmackException.NotConnectedException, InterruptedException, TestNotPossibleException, XmppStringprepException, MultiUserChatException.MucAlreadyJoinedException, MultiUserChatException.MissingMucCreationAcknowledgeException, MultiUserChatException.NotAMucServiceException
    {
        super(environment);

        // The specification reads (in section 5.2) "Support for the admin [...] is RECOMMENDED."
        // which suggests that this is optional functionality. The specification does not explicitly say how to test for
        // support. This implementation will use any XMPP error in response to a change request as an indication that
        // the feature is not supported by the server under test.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-admin-revoke-support");
        final MultiUserChat muc = mucManagerOne.getMultiUserChat(mucAddress);
        createMuc(muc, Resourcepart.from("owner-" + randomString));
        try {
            muc.grantAdmin(conTwo.getUser().asBareJid());
            muc.revokeAdmin(conTwo.getUser().asEntityBareJid());
        } catch (XMPPException.XMPPErrorException e) {
            if (StanzaError.Condition.not_authorized.equals(e.getStanzaError().getCondition())) {
                throw new TestNotPossibleException("Service does not support granting and/or revokation of admin status functionality.");
            } else {
                throw e;
            }
        } finally {
            tryDestroy(muc);
        }
    }

    /**
     * Verifies that an owner can revoke admin status from a user that is currently not in the room.
     */
    @SmackIntegrationTest(section = "10.7", quote = "An owner might want to revoke a user's admin status; this is done by changing the user's affiliation to something other than \"admin\" or \"owner\"")
    public void testRevokeAdmin() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-admin-revoke");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            final MUCAdmin grantRequest = new MUCAdmin();
            grantRequest.setTo(mucAddress);
            grantRequest.setType(IQ.Type.set);
            grantRequest.addItem(new MUCItem(MUCAffiliation.admin, conTwo.getUser().asBareJid()));

            conOne.sendIqRequestAndWaitForResponse(grantRequest);

            // Execute system under test.
            final MUCAdmin revokeRequest = new MUCAdmin();
            revokeRequest.setTo(mucAddress);
            revokeRequest.setType(IQ.Type.set);
            revokeRequest.addItem(new MUCItem(MUCAffiliation.none, conTwo.getUser().asBareJid()));
            try {
                conOne.sendIqRequestAndWaitForResponse(revokeRequest);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to revoke admin status from '" + conTwo.getUser().asBareJid() + "' in '" + mucAddress + "' (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that an owner can revoke admin status from a user that is currently a participant in the room.
     */
    @SmackIntegrationTest(section = "10.7", quote = "An owner might want to revoke a user's admin status; this is done by changing the user's affiliation to something other than \"admin\" or \"owner\"")
    public void testRevokeAdminWhileInRoom() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-admin-revoke-inroom");
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

            final MUCAdmin grantRequest = new MUCAdmin();
            grantRequest.setTo(mucAddress);
            grantRequest.setType(IQ.Type.set);
            grantRequest.addItem(new MUCItem(MUCAffiliation.admin, conTwo.getUser().asBareJid()));

            conOne.sendIqRequestAndWaitForResponse(grantRequest);

            // Execute system under test.
            final MUCAdmin revokeRequest = new MUCAdmin();
            revokeRequest.setTo(mucAddress);
            revokeRequest.setType(IQ.Type.set);
            revokeRequest.addItem(new MUCItem(MUCAffiliation.none, conTwo.getUser().asBareJid()));

            try {
                conOne.sendIqRequestAndWaitForResponse(revokeRequest);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to revoke admin status from '" + conTwo.getUser().asBareJid() + "' (that is currently joined as '" + nicknameTarget+ "') in '" + mucAddress + "' (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that an owner can revoke admin status from a user that is currently not in the room, and give a reason.
     */
    @SmackIntegrationTest(section = "10.7", quote = "An owner might want to revoke a user's admin status; this is done by changing the user's affiliation to something other than \"admin\" or \"owner\" [...] The <reason/> element is OPTIONAL.")
    public void testRevokeAdminOptionalReason() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-admin-revoke-reason");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            final MUCAdmin grantRequest = new MUCAdmin();
            grantRequest.setTo(mucAddress);
            grantRequest.setType(IQ.Type.set);
            grantRequest.addItem(new MUCItem(MUCAffiliation.admin, conTwo.getUser().asBareJid()));

            conOne.sendIqRequestAndWaitForResponse(grantRequest);

            // Execute system under test.
            final MUCAdmin revokeRequest = new MUCAdmin();
            revokeRequest.setTo(mucAddress);
            revokeRequest.setType(IQ.Type.set);
            revokeRequest.addItem(new MUCItem(MUCAffiliation.none, conTwo.getUser().asBareJid(), "Revoking Admin Status as part of an integration test."));
            try {
                conOne.sendIqRequestAndWaitForResponse(revokeRequest);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to revoke admin status from '" + conTwo.getUser().asBareJid() + "' in '" + mucAddress + "' (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that an owner can revoke admin status from a user that is currently a participant in the room, and provide a reason.
     */
    @SmackIntegrationTest(section = "10.7", quote = "An owner might want to revoke a user's admin status; this is done by changing the user's affiliation to something other than \"admin\" or \"owner\" [...] The <reason/> element is OPTIONAL.")
    public void testRevokeAdminOptionalReasonWhileInRoom() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-admin-revoke-reason-inroom");
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

            final MUCAdmin grantRequest = new MUCAdmin();
            grantRequest.setTo(mucAddress);
            grantRequest.setType(IQ.Type.set);
            grantRequest.addItem(new MUCItem(MUCAffiliation.admin, conTwo.getUser().asBareJid()));

            conOne.sendIqRequestAndWaitForResponse(grantRequest);

            // Execute system under test.
            final MUCAdmin revokeRequest = new MUCAdmin();
            revokeRequest.setTo(mucAddress);
            revokeRequest.setType(IQ.Type.set);
            revokeRequest.addItem(new MUCItem(MUCAffiliation.owner, conTwo.getUser().asBareJid(), "Revoking Admin Status as part of an integration test."));
            try {
                conOne.sendIqRequestAndWaitForResponse(revokeRequest);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to revoke admin status from '" + conTwo.getUser().asBareJid() + "' (that is currently joined as '" + nicknameTarget+ "') in '" + mucAddress + "' (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    // TODO enable these tests after https://github.com/xsf/xeps/pull/1370 gets merged. Until then, the specification does not seem to restrict revokation of admin status to owners.
//    /**
//     * Verifies that a non-owner, non-joined user cannot revoke someone's admin status (when the target is not in the room).
//     */
//    @SmackIntegrationTest(section = "10.7", quote = "If the <user@host> of the 'from' address does not match the bare JID of a room owner, the service MUST return a <forbidden/> error to the sender.")
//    public void testUserNotAllowedToRevokeAdminStatus() throws Exception
//    {
//        // Setup test fixture.
//        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-admin-revoke-user-notallowed");
//        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
//
//        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
//
//        createMuc(mucAsSeenByOwner, nicknameOwner);
//        try {
//            final MUCAdmin grantRequest = new MUCAdmin();
//            grantRequest.setTo(mucAddress);
//            grantRequest.setType(IQ.Type.set);
//            grantRequest.addItem(new MUCItem(MUCAffiliation.admin, conThree.getUser().asBareJid()));
//
//            conOne.sendIqRequestAndWaitForResponse(grantRequest);
//
//            // Execute system under test.
//            final MUCAdmin revokeRequest = new MUCAdmin();
//            revokeRequest.setTo(mucAddress);
//            revokeRequest.setType(IQ.Type.set);
//            revokeRequest.addItem(new MUCItem(MUCAffiliation.none, conThree.getUser().asBareJid()));
//
//            // Verify result.
//            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
//                conTwo.sendIqRequestAndWaitForResponse(revokeRequest);
//            }, "Expected an error after '" + conTwo.getUser() + "' (that is not an owner) tried to revoke admin status from another user ('" + conThree.getUser().asBareJid() + "') in room '" + mucAddress + "' (but none occurred).");
//            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' after it tried to revoke admin status from user ('" + conThree.getUser().asBareJid() + "') in room '" + mucAddress + "' while not being an owner.");
//        } finally {
//            // Tear down test fixture.
//            tryDestroy(mucAsSeenByOwner);
//        }
//    }
//
//    /**
//     * Verifies that a non-owner, non-joined user cannot revoke someone's admin status (when the target is in the room).
//     */
//    @SmackIntegrationTest(section = "10.7", quote = "If the <user@host> of the 'from' address does not match the bare JID of a room owner, the service MUST return a <forbidden/> error to the sender.")
//    public void testUserNotAllowedToRevokeAdminStatusInRoom() throws Exception
//    {
//        // Setup test fixture.
//        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-admin-revoke-user-notallowed-inroom");
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
//            final MUCAdmin grantRequest = new MUCAdmin();
//            grantRequest.setTo(mucAddress);
//            grantRequest.setType(IQ.Type.set);
//            grantRequest.addItem(new MUCItem(MUCAffiliation.admin, conThree.getUser().asBareJid()));
//
//            conOne.sendIqRequestAndWaitForResponse(grantRequest);
//
//            // Execute system under test.
//            final MUCAdmin revokeRequest = new MUCAdmin();
//            revokeRequest.setTo(mucAddress);
//            revokeRequest.setType(IQ.Type.set);
//            revokeRequest.addItem(new MUCItem(MUCAffiliation.none, conThree.getUser().asBareJid()));
//
//            // Verify result.
//            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
//                conTwo.sendIqRequestAndWaitForResponse(revokeRequest);
//            }, "Expected an error after '" + conTwo.getUser() + "' (that is not an owner) tried to revoke admin status from another user ('" + conThree.getUser().asBareJid() + "', joined as '" + nicknameTarget + "') in room '" + mucAddress + "' (but none occurred).");
//            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' after it tried to revoke admin status from user ('" + conThree.getUser().asBareJid() + "', joined as '" + nicknameTarget + "') in room '" + mucAddress + "' while not being an owner.");
//        } finally {
//            // Tear down test fixture.
//            tryDestroy(mucAsSeenByOwner);
//        }
//    }
//
//    /**
//     * Verifies that a non-owner (that has joined the room) cannot revoke someone's admin status (when the target is not in the room).
//     */
//    @SmackIntegrationTest(section = "10.7", quote = "If the <user@host> of the 'from' address does not match the bare JID of a room owner, the service MUST return a <forbidden/> error to the sender.")
//    public void testParticipantNotAllowedToRevokeAdminStatus() throws Exception
//    {
//        // Setup test fixture.
//        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-admin-revoke-participant-notallowed");
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
//            final MUCAdmin grantRequest = new MUCAdmin();
//            grantRequest.setTo(mucAddress);
//            grantRequest.setType(IQ.Type.set);
//            grantRequest.addItem(new MUCItem(MUCAffiliation.admin, conThree.getUser().asBareJid()));
//
//            conOne.sendIqRequestAndWaitForResponse(grantRequest);
//
//            // Execute system under test.
//            final MUCAdmin revokeRequest = new MUCAdmin();
//            revokeRequest.setTo(mucAddress);
//            revokeRequest.setType(IQ.Type.set);
//            revokeRequest.addItem(new MUCItem(MUCAffiliation.none, conThree.getUser().asBareJid()));
//
//            // Verify result.
//            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
//                conTwo.sendIqRequestAndWaitForResponse(revokeRequest);
//            }, "Expected an error after '" + conTwo.getUser() + "' (that is not an owner, but joined the room as '" + nicknameParticipant + "') tried to revoke admin status from another user ('" + conThree.getUser().asBareJid() + "') in room '" + mucAddress + "' (but none occurred).");
//            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' (joined as '" + nicknameParticipant + "') after it tried to revoke admin status from user ('" + conThree.getUser().asBareJid() + "') in room '" + mucAddress + "' while not being an owner.");
//        } finally {
//            // Tear down test fixture.
//            tryDestroy(mucAsSeenByOwner);
//        }
//    }
//
//    /**
//     * Verifies that a non-owner (that has joined the room) cannot revoke someone's admin status (when the target is in the room).
//     */
//    @SmackIntegrationTest(section = "10.7", quote = "If the <user@host> of the 'from' address does not match the bare JID of a room owner, the service MUST return a <forbidden/> error to the sender.")
//    public void testParticipantNotAllowedToRevokeAdminStatusInRoom() throws Exception
//    {
//        // Setup test fixture.
//        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-admin-revoke-participant-notallowed-inroom");
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
//            final MUCAdmin grantRequest = new MUCAdmin();
//            grantRequest.setTo(mucAddress);
//            grantRequest.setType(IQ.Type.set);
//            grantRequest.addItem(new MUCItem(MUCAffiliation.admin, conThree.getUser().asBareJid()));
//
//            conOne.sendIqRequestAndWaitForResponse(grantRequest);
//
//            // Execute system under test.
//            final MUCAdmin revokeRequest = new MUCAdmin();
//            revokeRequest.setTo(mucAddress);
//            revokeRequest.setType(IQ.Type.set);
//            revokeRequest.addItem(new MUCItem(MUCAffiliation.none, conThree.getUser().asBareJid()));
//
//            // Verify result.
//            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
//                conTwo.sendIqRequestAndWaitForResponse(revokeRequest);
//            }, "Expected an error after '" + conTwo.getUser() + "' (that is not an owner, but joined the room as '" + nicknameParticipant + "') tried to revoke admin status from another user ('" + conThree.getUser().asBareJid() + "', joined as '" + nicknameTarget + "') in room '" + mucAddress + "' (but none occurred).");
//            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' (joined as '" + nicknameParticipant + "') after it tried to revoke admin status from user ('" + conThree.getUser().asBareJid() + "', joined as '" + nicknameTarget + "') in room '" + mucAddress + "' while not being an owner.");
//        } finally {
//            // Tear down test fixture.
//            tryDestroy(mucAsSeenByOwner);
//        }
//    }

    /**
     * Verifies that an admin that got its admin status removed no longer exists on the admin list.
     */
    @SmackIntegrationTest(section = "10.7", quote = "An owner might want to revoke a user's admin status [...] The service MUST remove the user from the admin list [...] ")
    public void testOwnerNotOnOwnerList() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-admin-not-on-admin-list");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            final MUCAdmin grantRequest = new MUCAdmin();
            grantRequest.setTo(mucAddress);
            grantRequest.setType(IQ.Type.set);
            grantRequest.addItem(new MUCItem(MUCAffiliation.admin, conTwo.getUser().asBareJid()));

            conOne.sendIqRequestAndWaitForResponse(grantRequest);

            // Execute system under test.
            final MUCAdmin revokeRequest = new MUCAdmin();
            revokeRequest.setTo(mucAddress);
            revokeRequest.setType(IQ.Type.set);
            revokeRequest.addItem(new MUCItem(MUCAffiliation.none, conTwo.getUser().asBareJid()));

            try {
                conOne.sendIqRequestAndWaitForResponse(revokeRequest);
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Expected owner '" + conOne.getUser() + "' to be able to revoke admin status from '" + conTwo.getUser() + "' in '" + mucAddress + "' (but the server returned an error).");
            }

            // Verify result.
            assertFalse(mucAsSeenByOwner.getAdmins().stream().anyMatch(owner -> owner.getJid().equals(conTwo.getUser().asBareJid())), "Expected '" + conTwo.getUser().asBareJid() + "' to no longer be on the Admin List after their admin status was removed '" + conOne.getUser() + "' in '" + mucAddress + "' (but the JID does still appear on the Admin List).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that occupants are notified when an existing occupant gets its admin status revoked
     */
    @SmackIntegrationTest(section = "10.7", quote = "If the user is in the room, the service MUST then send updated presence from this individual to all occupants, indicating the loss of admin status by sending a presence element that contains an <x/> element qualified by the 'http://jabber.org/protocol/muc#user' namespace and containing an <item/> child with the 'affiliation' attribute set to a value other than \"admin\" or \"owner\" [...]")
    public void testOccupantsInformed() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-admin-broadcast");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);

        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            final MUCAdmin requestGrant = new MUCAdmin();
            requestGrant.setTo(mucAddress);
            requestGrant.setType(IQ.Type.set);
            requestGrant.addItem(new MUCItem(MUCAffiliation.admin, conThree.getUser().asBareJid()));

            conOne.sendIqRequestAndWaitForResponse(requestGrant);

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

            final SimpleResultSyncPoint targetSeesRevoke = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint ownerSeesRevoke = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint participantSeesRevoke = new SimpleResultSyncPoint();
            mucAsSeenByTarget.addUserStatusListener(new UserStatusListener() {
                @Override
                public void adminRevoked() {
                    targetSeesRevoke.signal();
                }
            });
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void adminRevoked(EntityFullJid participant) {
                    if (targetMucAddress.equals(participant)) {
                        ownerSeesRevoke.signal();
                    }
                }
            });
            mucAsSeenByParticipant.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void adminRevoked(EntityFullJid participant) {
                    if (targetMucAddress.equals(participant)) {
                        participantSeesRevoke.signal();
                    }
                }
            });

            // Execute system under test.
            final MUCAdmin requestRevoke = new MUCAdmin();
            requestRevoke.setTo(mucAddress);
            requestRevoke.setType(IQ.Type.set);
            requestRevoke.addItem(new MUCItem(MUCAffiliation.none, conThree.getUser().asBareJid()));

            conOne.sendIqRequestAndWaitForResponse(requestRevoke);

            // Verify result.
            assertResult(targetSeesRevoke, "Expected '" + conThree.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the revokation of admin status, after their admin status is revoked by '" + conOne.getUser() + "' in '" + mucAddress + "' (but no such stanza was received).");
            assertResult(ownerSeesRevoke, "Expected '" + conOne.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the revokation of admin status, after '" + targetMucAddress + "' has their admin status revoked by '" + conOne.getUser() + "' in '" + mucAddress + "' (but no such stanza was received).");
            assertResult(participantSeesRevoke, "Expected '" + conTwo.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the revokation of admin status, after '" + targetMucAddress + "' has their admin status revoked by '" + conOne.getUser() + "' in '" + mucAddress + "' (but no such stanza was received).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }
}
