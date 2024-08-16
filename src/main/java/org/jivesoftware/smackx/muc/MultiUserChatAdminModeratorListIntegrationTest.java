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
@SpecificationReference(document = "XEP-0045", version = "1.34.6")
public class MultiUserChatAdminModeratorListIntegrationTest extends AbstractMultiUserChatIntegrationTest
{
    public MultiUserChatAdminModeratorListIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
        InterruptedException, TestNotPossibleException, MultiUserChatException.MucAlreadyJoinedException, MultiUserChatException.MissingMucCreationAcknowledgeException, MultiUserChatException.NotAMucServiceException, XmppStringprepException
    {
        super(environment);
    }

    /**
     * Asserts that a moderator list can be obtained.
     */
    @SmackIntegrationTest(section = "9.8", quote = " the admin [...] requests the moderator list by querying the room for all users with a role of 'moderator'.")
    public void mucTestAdminRequestsModeratorList() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-requests-moderatorlist");
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
            fail("Expected admin '" + conTwo.getUser() + "' to be able to receive the moderator list from '" + mucAddress + "' (but the server returned an error).", e);
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that a moderator list item has 'nick' and 'role' attributes.
     */
    @SmackIntegrationTest(section = "9.8", quote = "each item MUST include the 'nick' and 'role' attributes")
    public void mucTestAdminModeratorListItemCheck() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-moderator-not-on-moderator-list");
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
                throw new TestNotPossibleException("Expected admin '" + conTwo.getUser() + "' to be able to grant moderator status to '" + targetMucAddress + "' in '" + mucAddress + "' (but the server returned an error).");
            }

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.get);
            iq.addItem(new MUCItem(MUCRole.moderator));
            final MUCAdmin response = conTwo.sendIqRequestAndWaitForResponse(iq);

            // Verify result.
            assertFalse(response.getItems().stream().anyMatch(i -> i.getNick() == null), "The moderator list for '" + mucAddress + "' contains an item that does not have a 'nick' attribute (but all items must have one).");
            assertFalse(response.getItems().stream().anyMatch(i -> i.getRole() == null), "The moderator list for '" + mucAddress + "' contains an item that does not have a 'role' attribute (but all items must have one).");
            assertTrue(response.getItems().stream().anyMatch(i -> i.getNick().equals(nicknameTarget)), "Expected the moderator list requested by '" + conTwo.getUser() + "' from '" + mucAddress + "' to include '" + nicknameTarget + "' (but it did not).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that a moderator list modification can contain more than one item.
     */
    @SmackIntegrationTest(section = "9.8", quote = "The admin can then modify the moderator list if desired. In order to do so, the admin MUST send the changed items [...] back to the service [...] The service MUST modify the moderator list and then inform the admin of success")
    public void mucTestAdminModeratorListMultipleItems() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-moderatorlist-multiple");
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
                conOne.sendIqRequestAndWaitForResponse(iq);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected the service to inform '" + conOne.getUser() + "' of success after they modified the moderator list of room '" + mucAddress + "' (but instead, an error was returned).", e);
            }
            assertTrue(mucAsSeenByOwner.getModerators().stream().anyMatch(i -> i.getRole() == MUCRole.moderator && i.getNick().equals(nicknameTarget1)), "Expected the moderator list for '" + mucAddress + "' to contain '" + nicknameTarget1 + "' that was just added to the moderator list by '" + conOne.getUser() + "' (but does not appear on the moderator list).");
            assertTrue(mucAsSeenByOwner.getModerators().stream().anyMatch(i -> i.getRole() == MUCRole.moderator && i.getNick().equals(nicknameTarget2)), "Expected the moderator list for '" + mucAddress + "' to contain '" + nicknameTarget2 + "' that was just added to the moderator list by '" + conOne.getUser() + "' (but does not appear on the moderator list).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that a moderator list modification can be used to remove people from the moderator list.
     */
    @SmackIntegrationTest(section = "9.8", quote = "The admin can then modify the moderator list if desired. [...] set to a value of \"moderator\" to grant moderator status or \"participant\" to revoke moderator status)")
    public void mucTestAdminModeratorListMultipleItemsRevoke() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-moderatorlist-revoke");
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
                throw new TestNotPossibleException("Unable to grant '" + conTwo.getUser() + "' and/or '" + conThree + "' the moderator role in room '" + mucAddress + "'.");
            }

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.set);
            iq.addItem(new MUCItem(MUCRole.participant, nicknameTarget1));
            iq.addItem(new MUCItem(MUCRole.participant, nicknameTarget2));

            try {
                conOne.sendIqRequestAndWaitForResponse(iq);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected the service to inform '" + conOne.getUser() + "' of success after they modified the moderator list of room '" + mucAddress + "' (but instead, an error was returned).", e);
            }
            assertTrue(mucAsSeenByOwner.getModerators().stream().noneMatch(i -> i.getRole() == MUCRole.moderator && i.getNick().equals(nicknameTarget1)), "Expected the moderator list for '" + mucAddress + "' to no longer contain '" + nicknameTarget1 + "' that was just removed from the moderator list by '" + conOne.getUser() + "' (but does appear on the moderator list).");
            assertTrue(mucAsSeenByOwner.getModerators().stream().noneMatch(i -> i.getRole() == MUCRole.moderator && i.getNick().equals(nicknameTarget2)), "Expected the moderator list for '" + mucAddress + "' to no longer contain '" + nicknameTarget2 + "' that was just removed from the moderator list by '" + conOne.getUser() + "' (but does appear on the moderator list).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that a moderator list modification is a delta: it shouldn't affect entries already on the moderator list
     * that are not included in the delta.
     */
    @SmackIntegrationTest(section = "9.8", quote = "The admin can then modify the moderator list if desired. In order to do so, the admin MUST send the changed items (i.e., only the \"delta\")")
    public void mucTestAdminModeratorListIsDelta() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-moderatorlist-delta");
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
                conOne.sendIqRequestAndWaitForResponse(iq);

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
     * Verifies that other occupants are notified when moderator list changes are made.
     */
    @SmackIntegrationTest(section = "9.8", quote = "The admin can then modify the moderator list [...] The service MUST then send updated presence for any affected individuals to all occupants, indicating the change in moderator status by sending the appropriate extended presence stanzas")
    public void mucTestAdminModeratorListBroadcast() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-moderatorlist-broadcast");
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

            final SimpleResultSyncPoint ownerSeesRevokeTarget1 = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint ownerSeesGrantTarget2 = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint target1SeesRevokeTarget1 = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint target1SeesGrantTarget2 = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint target2SeesRevokeTarget1 = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint target2SeesGrantTarget2 = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void moderatorGranted(EntityFullJid participant) {
                    if (participant.equals(target2MucAddress)) {
                        ownerSeesGrantTarget2.signal();
                    }
                }

                @Override
                public void moderatorRevoked(EntityFullJid participant) {
                    if (participant.equals(target1MucAddress)) {
                        ownerSeesRevokeTarget1.signal();
                    }
                }
            });
            mucAsSeenByTarget1.addUserStatusListener(new UserStatusListener() {
                @Override
                public void moderatorRevoked() {
                    target1SeesRevokeTarget1.signal();
                }
            });
            mucAsSeenByTarget1.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void moderatorGranted(EntityFullJid participant) {
                    if (participant.equals(target2MucAddress)) {
                        target1SeesGrantTarget2.signal();
                    }
                }
            });
            mucAsSeenByTarget2.addUserStatusListener(new UserStatusListener() {
                @Override
                public void moderatorGranted() {
                    target2SeesGrantTarget2.signal();
                }
            });
            mucAsSeenByTarget2.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void moderatorRevoked(EntityFullJid participant) {
                    if (participant.equals(target1MucAddress)) {
                        target2SeesRevokeTarget1.signal();
                    }
                }
            });

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.set);
            iq.addItem(new MUCItem(MUCRole.participant, nicknameTarget1));
            iq.addItem(new MUCItem(MUCRole.moderator, nicknameTarget2));

            try {
                conOne.sendIqRequestAndWaitForResponse(iq);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected the service to inform '" + conOne.getUser() + "' of success after they modified the moderator list of room '" + mucAddress + "' (but instead, an error was returned).", e);
            }

            assertResult(ownerSeesRevokeTarget1, "Expected '" + conOne.getUser() + "' to receive a presence stanza from '" + mucAddress + "' indicating the change in moderator status of '" + nicknameTarget1 + "' (but did not).");
            assertResult(ownerSeesGrantTarget2, "Expected '" + conOne.getUser() + "' to receive a presence stanza from '" + mucAddress + "' indicating the change in moderator status of '" + nicknameTarget2 + "' (but did not).");
            assertResult(target1SeesRevokeTarget1, "Expected '" + conTwo.getUser() + "' to receive a presence stanza from '" + mucAddress + "' indicating the change in moderator status for themself (but did not).");
            assertResult(target1SeesGrantTarget2, "Expected '" + conTwo.getUser() + "' to receive a presence stanza from '" + mucAddress + "' indicating the change in moderator status of '" + nicknameTarget2 + "' (but did not).");
            assertResult(target2SeesRevokeTarget1, "Expected '" + conThree.getUser() + "' to receive a presence stanza from '" + mucAddress + "' indicating the change in moderator status of '" + nicknameTarget1 + "' (but did not).");
            assertResult(target2SeesGrantTarget2, "Expected '" + conThree.getUser() + "' to receive a presence stanza from '" + mucAddress + "' indicating the change in moderator status for themself' (but did not).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that an admin cannot revoke moderator status from another admin
     */
    @SmackIntegrationTest(section = "9.8", quote = "[...] moderator status cannot be revoked from a [...] room admin. If a room admin attempts to revoke moderator status from such a user by modifying the moderator list, the service MUST deny the request and return a <not-allowed/> error to the sender")
    public void testAdminNotAllowedToRevokeModeratorFromAdmin() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-moderatorlist-revoke-admin-rejected");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTargetAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameTargetAdmin = Resourcepart.from("targetAdmin-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);

        final EntityFullJid targetAdminMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTargetAdmin);
        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);

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
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());

            final SimpleResultSyncPoint ownerSeesTargetAdmin = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint ownerSeesTarget = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(targetAdminMucAddress)) {
                        ownerSeesTargetAdmin.signal();
                    }
                    if (participant.equals(targetMucAddress)) {
                        ownerSeesTarget.signal();
                    }
                }
            });

            mucAsSeenByTargetAdmin.join(nicknameTargetAdmin);
            mucAsSeenByTarget.join(nicknameTarget);

            ownerSeesTargetAdmin.waitForResult(timeout);
            ownerSeesTarget.waitForResult(timeout);

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.set);
            iq.addItem(new MUCItem(MUCRole.participant, nicknameTargetAdmin)); // Should be rejected
            iq.addItem(new MUCItem(MUCRole.moderator, nicknameTarget)); // Should be valid

            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> conOne.sendIqRequestAndWaitForResponse(iq), "Expected Moderator List modification submitted by '" + conOne.getUser() + "' to be rejected, as it illegally attempts to revoke moderator status from '" + nicknameTargetAdmin + "' which is an admin in room '" + mucAddress + "' (but no error was received).");
            assertEquals(StanzaError.Condition.not_allowed, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conOne.getUser() + "' after it tried to modify the moderator list of room '" + mucAddress + "' by removing moderator status of '" + nicknameTargetAdmin + "', another admin.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that an admin cannot revoke moderator status from an owner
     */
    @SmackIntegrationTest(section = "9.8", quote = "[...] moderator status cannot be revoked from a room owner [...]. If a room admin attempts to revoke moderator status from such a user by modifying the moderator list, the service MUST deny the request and return a <not-allowed/> error to the sender")
    public void testAdminNotAllowedToRevokeModeratorFromOwner() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-moderatorlist-revoke-owner-rejected");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTargetOwner = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameTargetOwner = Resourcepart.from("targetOwner-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);

        final EntityFullJid targetOwnerMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTargetOwner);
        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);

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
            mucAsSeenByOwner.grantOwnership(conTwo.getUser().asBareJid());

            final SimpleResultSyncPoint ownerSeesTargetAdmin = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint ownerSeesTarget = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(targetOwnerMucAddress)) {
                        ownerSeesTargetAdmin.signal();
                    }
                    if (participant.equals(targetMucAddress)) {
                        ownerSeesTarget.signal();
                    }
                }
            });

            mucAsSeenByTargetOwner.join(nicknameTargetOwner);
            mucAsSeenByTarget.join(nicknameTarget);

            ownerSeesTargetAdmin.waitForResult(timeout);
            ownerSeesTarget.waitForResult(timeout);

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.set);
            iq.addItem(new MUCItem(MUCRole.participant, nicknameTargetOwner)); // Should be rejected
            iq.addItem(new MUCItem(MUCRole.moderator, nicknameTarget)); // Should be valid

            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> conOne.sendIqRequestAndWaitForResponse(iq), "Expected Moderator List modification submitted by '" + conOne.getUser() + "' to be rejected, as it illegally attempts to revoke moderator status from '" + nicknameTargetOwner + "' that is an owner in room '" + mucAddress + "' (but no error was received).");
            assertEquals(StanzaError.Condition.not_allowed, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conOne.getUser() + "' after it tried to modify the moderator list of room '" + mucAddress + "' by removing moderator status of '" + nicknameTargetOwner + "', that is an owner.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }
}
