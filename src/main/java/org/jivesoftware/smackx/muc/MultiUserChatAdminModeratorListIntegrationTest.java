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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for section "9.8 Admin Use Cases: Modifying the Moderator List" of XEP-0045: "Multi-User Chat"
 *
 * @see <a href="https://xmpp.org/extensions/xep-0045.html#modifymod">XEP-0045 Section 9.8</a>
 */
@SpecificationReference(document = "XEP-0045", version = "1.35.1")
public class MultiUserChatAdminModeratorListIntegrationTest extends AbstractMultiUserChatIntegrationTest
{
    public MultiUserChatAdminModeratorListIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
        InterruptedException, TestNotPossibleException, MultiUserChatException.MucAlreadyJoinedException, MultiUserChatException.MissingMucCreationAcknowledgeException, MultiUserChatException.NotAMucServiceException, XmppStringprepException
    {
        super(environment);
    }

    /**
     * Asserts that a moderator list can be obtained by an admin.
     */
    @SmackIntegrationTest(section = "9.8", quote = "the admin [...] requests the moderator list by querying the room for all users with a role of 'moderator'.")
    public void mucTestAdminRequestsModeratorList() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-requests-moderatorlist");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByAdmin.join(nicknameAdmin);

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.get);
            iq.addItem(new MUCItem(MUCRole.moderator));
            conTwo.sendIqRequestAndWaitForResponse(iq);

            // Verify result.
        } catch (XMPPException.XMPPErrorException e) {
            fail("Expected '" + conTwo.getUser() + "' (an admin) to be able to receive the moderator list from '" + mucAddress + "' (but the server returned an error).", e);
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that a moderator list item (as requested by an admin) has 'nick' and 'role' attributes.
     */
    @SmackIntegrationTest(section = "9.8", quote = "The service MUST then return the moderator list to the admin; each item MUST include the 'nick' and 'role' attributes")
    public void mucTestAdminModeratorListItemCheck() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-moderator-not-on-moderator-list");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);

        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByAdmin.join(nicknameAdmin);

            final SimpleResultSyncPoint adminSeesTarget = new SimpleResultSyncPoint();
            mucAsSeenByAdmin.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        adminSeesTarget.signal();
                    }
                }
            });
            mucAsSeenByTarget.join(nicknameTarget);
            adminSeesTarget.waitForResult(timeout);

            try {
                mucAsSeenByAdmin.grantModerator(nicknameTarget);
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Expected '" + conTwo.getUser() + "' (an admin) to be able to grant moderator status to '" + targetMucAddress + "' in '" + mucAddress + "' (but the server returned an error).");
            }

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.get);
            iq.addItem(new MUCItem(MUCRole.moderator));
            final MUCAdmin response = conTwo.sendIqRequestAndWaitForResponse(iq);

            // Verify result.
            assertFalse(response.getItems().stream().anyMatch(i -> i.getNick() == null), "The moderator list for '" + mucAddress + "' as requested by '" +  conTwo.getUser() + "' (an admin) contains an item that does not have a 'nick' attribute (but all items must have one).");
            assertFalse(response.getItems().stream().anyMatch(i -> i.getRole() == null), "The moderator list for '" + mucAddress + "' as requested by '" +  conTwo.getUser() + "' (an admin) contains an item that does not have a 'role' attribute (but all items must have one).");
            assertTrue(response.getItems().stream().anyMatch(i -> i.getNick().equals(nicknameTarget)), "Expected the moderator list requested by '" + conTwo.getUser() + "' from '" + mucAddress + "' to include '" + nicknameTarget + "' (but it did not).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that a moderator list modification can contain more than one item.
     *
     * The specification under test defines that a user with the affiliation of 'admin' performs the action that is being
     * tested here. As the test framework does not allow for more than three users to be used, we are one user short for
     * this to happen (the user that created the chatroom is an owner by definition. For a multi-item change, both other
     * users are needed). For this reason, the 'actor' in the implementation of this test is the room owner (the XEP
     * defines that "an owner can do anything an admin can do" in section 5.2.1). This compromise (not using an admin
     * user but an owner user) does allow for a test of a Moderator List modification that contains more than one item.
     */
    @SmackIntegrationTest(section = "9.8", quote = "The admin can then modify the moderator list if desired. In order to do so, the admin MUST send the changed items [...] back to the service [...] The service MUST modify the moderator list and then inform the admin of success")
    public void mucTestAdminModeratorListMultipleItems() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-moderatorlist-multiple");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget1 = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget2 = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameTarget1 = Resourcepart.from("target1-" + randomString);
        final Resourcepart nicknameTarget2 = Resourcepart.from("target2-" + randomString);

        final EntityFullJid target1MucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget1);
        final EntityFullJid target2MucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget2);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            try {
                // Implementations _should_ give the owner the role of 'moderator' by default, but let's check and correct for that to be sure.
                if (mucAsSeenByOwner.getModerators().stream().noneMatch(m -> m.getNick().equals(nicknameOwner))) {
                    mucAsSeenByOwner.grantModerator(nicknameOwner);
                }
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Unable to grant owner '" + conOne.getUser() + "' the moderator role in room '" + mucAddress + "'.");
            }

            final SimpleResultSyncPoint ownerSeesTarget1 = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint ownerSeesTarget2 = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(target1MucAddress)) {
                        ownerSeesTarget1.signal();
                    }
                    if (participant.equals(target2MucAddress)) {
                        ownerSeesTarget2.signal();
                    }
                }
            });

            mucAsSeenByTarget1.join(nicknameTarget1);
            mucAsSeenByTarget2.join(nicknameTarget2);

            ownerSeesTarget1.waitForResult(timeout);
            ownerSeesTarget2.waitForResult(timeout);

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.set);
            iq.addItem(new MUCItem(MUCRole.moderator, nicknameTarget1));
            iq.addItem(new MUCItem(MUCRole.moderator, nicknameTarget2));

            try {
                conOne.sendIqRequestAndWaitForResponse(iq); // As we're limited to only three connections, we're using the owner rather than an admin for this test.

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected the service to inform '" + conOne.getUser() + "' of success after they modified the moderator list of room '" + mucAddress + "' (but instead, an error was returned).", e);
            }
            assertTrue(mucAsSeenByOwner.getModerators().stream().anyMatch(i -> i.getRole() == MUCRole.moderator && i.getNick().equals(nicknameTarget1)), "Expected the moderator list for '" + mucAddress + "' to contain '" + nicknameTarget1 + "' that was just added to the moderator list by '" + conOne.getUser() + "' (an owner) (but does not appear on the moderator list).");
            assertTrue(mucAsSeenByOwner.getModerators().stream().anyMatch(i -> i.getRole() == MUCRole.moderator && i.getNick().equals(nicknameTarget2)), "Expected the moderator list for '" + mucAddress + "' to contain '" + nicknameTarget2 + "' that was just added to the moderator list by '" + conOne.getUser() + "' (an owner) (but does not appear on the moderator list).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that a moderator list modification made by can be used to remove people from the moderator list.
     *
     * The specification under test defines that a user with the affiliation of 'admin' performs the action that is being
     * tested here. As the test framework does not allow for more than three users to be used, we are one user short for
     * this to happen (the user that created the chatroom is an owner by definition. For a multi-item change, both other
     * users are needed). For this reason, the 'actor' in the implementation of this test is the room owner (the XEP
     * defines that "an owner can do anything an admin can do" in section 5.2.1). This compromise (not using an admin
     * user but an owner user) does allow for a test of a Moderator List modification that contains more than one item.
     */
    @SmackIntegrationTest(section = "9.8", quote = "The admin can then modify the moderator list if desired. [...] set to a value of \"moderator\" to grant moderator status or \"participant\" to revoke moderator status)")
    public void mucTestAdminModeratorListMultipleItemsRevoke() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-moderatorlist-revoke");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget1 = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget2 = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameTarget1 = Resourcepart.from("target1-" + randomString);
        final Resourcepart nicknameTarget2 = Resourcepart.from("target2-" + randomString);

        final EntityFullJid target1MucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget1);
        final EntityFullJid target2MucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget2);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            try {
                // Implementations _should_ give the owner the role of 'moderator' by default, but let's check and correct for that to be sure.
                if (mucAsSeenByOwner.getModerators().stream().noneMatch(m -> m.getNick().equals(nicknameOwner))) {
                    mucAsSeenByOwner.grantModerator(nicknameOwner);
                }
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Unable to grant owner '" + conOne.getUser() + "' the moderator role in room '" + mucAddress + "'.");
            }

            final SimpleResultSyncPoint ownerSeesTarget1 = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint ownerSeesTarget2 = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(target1MucAddress)) {
                        ownerSeesTarget1.signal();
                    }
                    if (participant.equals(target2MucAddress)) {
                        ownerSeesTarget2.signal();
                    }
                }
            });

            mucAsSeenByTarget1.join(nicknameTarget1);
            mucAsSeenByTarget2.join(nicknameTarget2);

            ownerSeesTarget1.waitForResult(timeout);
            ownerSeesTarget2.waitForResult(timeout);

            try {
                mucAsSeenByOwner.grantModerator(List.of(nicknameTarget1, nicknameTarget2));
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Unable to grant '" + conTwo.getUser() + "' and/or '" + conThree.getUser() + "' the moderator role in room '" + mucAddress + "'.");
            }

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.set);
            iq.addItem(new MUCItem(MUCRole.participant, nicknameTarget1));
            iq.addItem(new MUCItem(MUCRole.participant, nicknameTarget2));

            try {
                conOne.sendIqRequestAndWaitForResponse(iq); // As we're limited to only three connections, we're using the owner rather than an admin for this test.

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected the service to inform '" + conOne.getUser() + "' (an owner) of success after they modified the moderator list of room '" + mucAddress + "' (but instead, an error was returned).", e);
            }
            assertTrue(mucAsSeenByOwner.getModerators().stream().noneMatch(i -> i.getRole() == MUCRole.moderator && i.getNick().equals(nicknameTarget1)), "Expected the moderator list for '" + mucAddress + "' to no longer contain '" + nicknameTarget1 + "' that was just removed from the moderator list by '" + conOne.getUser() + "' (an owner) (but does appear on the moderator list).");
            assertTrue(mucAsSeenByOwner.getModerators().stream().noneMatch(i -> i.getRole() == MUCRole.moderator && i.getNick().equals(nicknameTarget2)), "Expected the moderator list for '" + mucAddress + "' to no longer contain '" + nicknameTarget2 + "' that was just removed from the moderator list by '" + conOne.getUser() + "' (an owner) (but does appear on the moderator list).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that a moderator list modification is a delta: it shouldn't affect entries already on the moderator list
     * that are not included in the delta.
     *
     * The specification under test defines that a user with the affiliation of 'admin' performs the action that is being
     * tested here. As the test framework does not allow for more than three users to be used, we are one user short for
     * this to happen (the user that created the chatroom is an owner by definition. For a multi-item change, both other
     * users are needed). For this reason, the 'actor' in the implementation of this test is the room owner (the XEP
     * defines that "an owner can do anything an admin can do" in section 5.2.1). This compromise (not using an admin
     * user but an owner user) does allow for a test of a Moderator List modification that contains more than one item.
     */
    @SmackIntegrationTest(section = "9.8", quote = "The admin can then modify the moderator list if desired. In order to do so, the admin MUST send the changed items (i.e., only the \"delta\")")
    public void mucTestAdminModeratorListIsDelta() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-moderatorlist-delta");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget1 = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget2 = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameTarget1 = Resourcepart.from("target1-" + randomString);
        final Resourcepart nicknameTarget2 = Resourcepart.from("target2-" + randomString);

        final EntityFullJid target1MucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget1);
        final EntityFullJid target2MucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget2);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            try {
                // Implementations _should_ give the owner the role of 'moderator' by default, but let's check and correct for that to be sure.
                if (mucAsSeenByOwner.getModerators().stream().noneMatch(m -> m.getNick().equals(nicknameOwner))) {
                    mucAsSeenByOwner.grantModerator(nicknameOwner);
                }
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Unable to grant owner '" + conOne.getUser() + "' the moderator role in room '" + mucAddress + "'.");
            }

            final SimpleResultSyncPoint ownerSeesTarget1 = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint ownerSeesTarget2 = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(target1MucAddress)) {
                        ownerSeesTarget1.signal();
                    }
                    if (participant.equals(target2MucAddress)) {
                        ownerSeesTarget2.signal();
                    }
                }
            });

            mucAsSeenByTarget1.join(nicknameTarget1);
            mucAsSeenByTarget2.join(nicknameTarget2);

            ownerSeesTarget1.waitForResult(timeout);
            ownerSeesTarget2.waitForResult(timeout);

            try {
                mucAsSeenByOwner.grantModerator(nicknameTarget1);
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Unable to grant '" + conTwo.getUser() + "' the moderator role in room '" + mucAddress + "'.");
            }

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.set);
            iq.addItem(new MUCItem(MUCRole.participant, nicknameTarget1));
            iq.addItem(new MUCItem(MUCRole.moderator, nicknameTarget2));

            try {
                conOne.sendIqRequestAndWaitForResponse(iq); // As we're limited to only three connections, we're using the owner rather than an admin for this test.

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected the service to inform '" + conOne.getUser() + "' of success after they modified the moderator list of room '" + mucAddress + "' (but instead, an error was returned).", e);
            }
            assertTrue(mucAsSeenByOwner.getModerators().stream().anyMatch(i -> i.getRole() == MUCRole.moderator && i.getNick().equals(nicknameOwner)), "Expected the moderator list for '" + mucAddress + "' to contain '" + nicknameOwner + "' after the moderator list that previously contained them got updated with different items (which should have been applied as a delta).");
            assertTrue(mucAsSeenByOwner.getModerators().stream().noneMatch(i -> i.getRole() == MUCRole.moderator && i.getNick().equals(nicknameTarget1)), "Expected the moderator list for '" + mucAddress + "' to no longer contain '" + nicknameTarget1 + "' that was just removed from the moderator list by '" + conOne.getUser() + "' (but does appear on the moderator list).");
            assertTrue(mucAsSeenByOwner.getModerators().stream().anyMatch(i -> i.getRole() == MUCRole.moderator && i.getNick().equals(nicknameTarget2)), "Expected the moderator list for '" + mucAddress + "' to contain '" + nicknameTarget2 + "' that was just added to the moderator list by '" + conOne.getUser() + "' (but does not appear on the moderator list).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that other occupants are notified when moderator list changes are made by an admin
     */
    @SmackIntegrationTest(section = "9.8", quote = "The admin can then modify the moderator list [...] The service MUST then send updated presence for any affected individuals to all occupants, indicating the change in moderator status by sending the appropriate extended presence stanzas")
    public void mucTestAdminModeratorListBroadcast() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-moderatorlist-broadcast");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);

        final EntityFullJid adminMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameAdmin);
        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());

            final SimpleResultSyncPoint ownerSeesAdmin = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(adminMucAddress)) {
                        ownerSeesAdmin.signal();
                    }
                }
            });
            mucAsSeenByAdmin.join(nicknameAdmin);
            ownerSeesAdmin.waitForResult(timeout);

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

            try {
                // Implementations _should_ give an admin the role of 'moderator' by default, but let's check and correct for that to be sure.
                if (mucAsSeenByOwner.getModerators().stream().noneMatch(m -> m.getNick().equals(nicknameAdmin))) {
                    mucAsSeenByOwner.grantModerator(nicknameAdmin);
                }
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Unable to grant '" + conTwo.getUser() + "' the moderator role in room '" + mucAddress + "'.");
            }

            final SimpleResultSyncPoint ownerSeesGrantTarget = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint adminSeesGrantTarget = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint targetSeesGrantTarget = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void moderatorGranted(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        ownerSeesGrantTarget.signal();
                    }
                }
            });
            mucAsSeenByAdmin.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void moderatorGranted(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        adminSeesGrantTarget.signal();
                    }
                }
            });
            mucAsSeenByTarget.addUserStatusListener(new UserStatusListener() {
                @Override
                public void moderatorGranted() {
                    targetSeesGrantTarget.signal();
                }
            });

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.set);
            iq.addItem(new MUCItem(MUCRole.moderator, nicknameTarget));
            iq.addItem(new MUCItem(MUCRole.moderator, nicknameOwner)); // This should be fine, as it's essentially a no-op (preferably, we'd use a different occupant for this, but the testing framework only gives us 3).

            try {
                conTwo.sendIqRequestAndWaitForResponse(iq);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected the service to inform '" + conTwo.getUser() + "' of success after they modified the moderator list of room '" + mucAddress + "' (but instead, an error was returned).", e);
            }

            assertResult(ownerSeesGrantTarget, "Expected '" + conOne.getUser() + "' to receive a presence stanza from '" + mucAddress + "' indicating the change in moderator status of '" + nicknameTarget + "' by '" + conTwo.getUser() + "' (an admin) (but did not).");
            assertResult(adminSeesGrantTarget, "Expected '" + conTwo.getUser() + "' to receive a presence stanza from '" + mucAddress + "' indicating the change in moderator status of '" + nicknameTarget + "' by '" + conTwo.getUser() + "' (an admin) (but did not).");
            assertResult(targetSeesGrantTarget, "Expected '" + conThree.getUser() + "' to receive a presence stanza from '" + mucAddress + "' indicating the change in moderator status for themself by '" + conTwo.getUser() + "' (an admin) (but did not).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that an admin cannot revoke moderator status from another admin.
     *
     * This test uses a request that targets not one but two chat room occupants (the target owner and another participant)
     * as the test intends to test the Modification of the Moderator List use-case (Using just one occupant would make
     * the request indistinguishable from the "Granting" or "Revoking Moderator Status" use-cases).
     */
    @SmackIntegrationTest(section = "9.8", quote = "[...] moderator status cannot be revoked from a [...] room admin. If a room admin attempts to revoke moderator status from such a user by modifying the moderator list, the service MUST deny the request and return a <not-allowed/> error to the sender")
    public void testAdminNotAllowedToRevokeModeratorFromAdmin() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-moderatorlist-revoke-admin-rejected");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTargetAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameTargetAdmin = Resourcepart.from("targetAdmin-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);

        final EntityFullJid targetAdminMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTargetAdmin);
        final EntityFullJid adminMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameAdmin);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByOwner.grantAdmin(conThree.getUser().asBareJid());

            final SimpleResultSyncPoint ownerSeesAdmin = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint ownerSeesTargetAdmin = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(adminMucAddress)) {
                        ownerSeesAdmin.signal();
                    }
                    if (participant.equals(targetAdminMucAddress)) {
                        ownerSeesTargetAdmin.signal();
                    }
                }
            });
            mucAsSeenByAdmin.join(nicknameAdmin);
            mucAsSeenByTargetAdmin.join(nicknameTargetAdmin);
            ownerSeesAdmin.waitForResult(timeout);
            ownerSeesTargetAdmin.waitForResult(timeout);
            try {
                // Implementations _should_ give an admin the role of 'moderator' by default, but let's check and correct for that to be sure.
                if (mucAsSeenByOwner.getModerators().stream().noneMatch(m -> m.getNick().equals(nicknameAdmin))) {
                    mucAsSeenByOwner.grantModerator(nicknameAdmin);
                }
                if (mucAsSeenByOwner.getModerators().stream().noneMatch(m -> m.getNick().equals(nicknameTargetAdmin))) {
                    mucAsSeenByOwner.grantModerator(nicknameTargetAdmin);
                }
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Unable to grant admin '" + conTwo.getUser() + "' and/or '" + conThree.getUser() + "' the moderator role in room '" + mucAddress + "'.");
            }

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.set);
            iq.addItem(new MUCItem(MUCRole.participant, nicknameTargetAdmin)); // Should cause the request to be rejected.
            iq.addItem(new MUCItem(MUCRole.moderator, nicknameOwner)); // This should be fine, as it's essentially a no-op (preferably, we'd use a different occupant for this, but the testing framework only gives us 3).

            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> conThree.sendIqRequestAndWaitForResponse(iq), "Expected Moderator List modification submitted by '" + conThree.getUser() + "' (an admin) to be rejected, as it illegally attempts to revoke moderator status from '" + nicknameOwner + "' that is an admin in room '" + mucAddress + "' (but no error was received).");
            assertEquals(StanzaError.Condition.not_allowed, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conThree.getUser() + "' (an admin) after it tried to modify the moderator list of room '" + mucAddress + "' by removing moderator status of '" + nicknameOwner + "', that is an admin.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that an admin cannot revoke moderator status from an owner.
     *
     * This test uses a request that targets not one but two chat room occupants (the target owner and another participant)
     * as the test intends to test the Modification of the Moderator List use-case (Using just one occupant would make
     * the request indistinguishable from the "Granting" or "Revoking Moderator Status" use-cases).
     */
    @SmackIntegrationTest(section = "9.8", quote = "[...] moderator status cannot be revoked from a room owner [...]. If a room admin attempts to revoke moderator status from such a user by modifying the moderator list, the service MUST deny the request and return a <not-allowed/> error to the sender")
    public void testAdminNotAllowedToRevokeModeratorFromOwner() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-moderatorlist-revoke-owner-rejected");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);

        final EntityFullJid participantMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameParticipant);
        final EntityFullJid adminMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameAdmin);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conThree.getUser().asBareJid());

            final SimpleResultSyncPoint ownerSeesAdmin = new SimpleResultSyncPoint();
            // Implementations _should_ give an admin the role of 'moderator' by default, but let's check and correct for that to be sure.
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(adminMucAddress)) {
                        ownerSeesAdmin.signal();
                    }
                }
            });
            mucAsSeenByAdmin.join(nicknameAdmin);
            ownerSeesAdmin.waitForResult(timeout);
            try {
                if (mucAsSeenByOwner.getModerators().stream().noneMatch(m -> m.getNick().equals(nicknameAdmin))) {
                    mucAsSeenByOwner.grantModerator(nicknameAdmin);
                }
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Unable to grant admin '" + conThree.getUser() + "' the moderator role in room '" + mucAddress + "'.");
            }

            // Wait for the extra target to have joined the room.
            final SimpleResultSyncPoint adminSeesParticipant = new SimpleResultSyncPoint();
            mucAsSeenByAdmin.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(participantMucAddress)) {
                        adminSeesParticipant.signal();
                    }
                }
            });
            mucAsSeenByParticipant.join(nicknameParticipant);
            adminSeesParticipant.waitForResult(timeout);

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.set);
            iq.addItem(new MUCItem(MUCRole.participant, nicknameOwner)); // Should cause the request to be rejected.
            iq.addItem(new MUCItem(MUCRole.moderator, nicknameParticipant)); // This should be fine.

            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> conThree.sendIqRequestAndWaitForResponse(iq), "Expected Moderator List modification submitted by '" + conThree.getUser() + "' (an admin) to be rejected, as it illegally attempts to revoke moderator status from '" + nicknameOwner + "' that is an owner in room '" + mucAddress + "' (but no error was received).");
            assertEquals(StanzaError.Condition.not_allowed, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conThree.getUser() + "' (an admin) after it tried to modify the moderator list of room '" + mucAddress + "' by removing moderator status of '" + nicknameOwner + "', that is an owner.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }
}
