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
 * Tests for section "9.7 Admin Use Cases: Revoking Moderator Status" of XEP-0045: "Multi-User Chat"
 *
 * @see <a href="https://xmpp.org/extensions/xep-0045.html#revokemod">XEP-0045 Section 9.7</a>
 */
@SpecificationReference(document = "XEP-0045", version = "1.35.1")
public class MultiUserChatAdminRevokeModeratorIntegrationTest extends AbstractMultiUserChatIntegrationTest
{
    public MultiUserChatAdminRevokeModeratorIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
        InterruptedException, TestNotPossibleException, MultiUserChatException.MucAlreadyJoinedException, MultiUserChatException.MissingMucCreationAcknowledgeException, MultiUserChatException.NotAMucServiceException, XmppStringprepException
    {
        super(environment);
    }

    /**
     * Verifies that an admin can revoke moderator status of a user.
     */
    @SmackIntegrationTest(section = "9.7", quote = "An admin might want to revoke a user's moderator status. [...] The status is revoked by changing the user's role to \"participant\"")
    public void testRevokeModerator() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-moderator-revoke");
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
            final MUCAdmin request = new MUCAdmin();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.addItem(new MUCItem(MUCRole.participant, nicknameTarget));

            try {
                conTwo.sendIqRequestAndWaitForResponse(request);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected '" + conTwo.getUser() + "' (an admin) to be able to revoke moderator status from '" + targetMucAddress + "' in '" + mucAddress + "' (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that an admin can revoke moderator status of a user.
     */
    @SmackIntegrationTest(section = "9.7", quote = "An admin might want to revoke a user's moderator status. [...] The status is revoked by changing the user's role to \"participant\" [...] The <reason/> element is OPTIONAL.")
    public void testRevokeModeratorWithReason() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-moderator-revoke-reason");
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
            final MUCAdmin request = new MUCAdmin();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.addItem(new MUCItem(MUCRole.participant, nicknameTarget, "Revoking moderator status as part of an integration test."));

            try {
                conTwo.sendIqRequestAndWaitForResponse(request);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected '" + conTwo.getUser() + "' (an admin) to be able to revoke moderator status (using the optional 'reason' element) from '" + targetMucAddress + "' in '" + mucAddress + "' (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a non-admin cannot revoke someone's moderator status.
     */
    @SmackIntegrationTest(section = "9", quote = "[...] revoke moderator status [...] MUST be denied if the <user@host> of the 'from' address of the request does not match the bare JID of one of the room admins; in this case, the service MUST return a <forbidden/> error.")
    public void testParticipantNotAllowedToRevokeModerator() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-moderator-revoke-notallowed");
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
            mucAsSeenByTarget.join(nicknameTarget);
            ownerSeesTarget.waitForResult(timeout);

            try {
                mucAsSeenByOwner.grantModerator(nicknameTarget);
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Expected owner '" + conOne.getUser() + "' to be able to grant moderator status to '" + targetMucAddress + "' in '" + mucAddress + "' (but the server returned an error).");
            }

            final MUCAdmin request = new MUCAdmin();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.addItem(new MUCItem(MUCRole.participant, nicknameTarget));

            // Execute system under test & Verify result.
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
                conTwo.sendIqRequestAndWaitForResponse(request);
            }, "Expected an error after '" + conTwo.getUser() + "' (that is not an admin) tried to revoke moderator status from another participant ('" + targetMucAddress + "') for room '" + mucAddress + "' (but none occurred).");
            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' (that is not an admin) after it tried to revoke moderator status from another participant ('" + targetMucAddress + "') for room '" + mucAddress + "' while not being an admin.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a participant that got its moderator status removed no longer exists on the moderator list.
     */
    @SmackIntegrationTest(section = "9.7", quote = "An admin might want to revoke a user's moderator status. [...] The service MUST remove the user from the moderator list [...] ")
    public void testModeratorNotOnModeratorList() throws Exception
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
                throw new TestNotPossibleException("Expected admin '" + conTwo.getUser() + "' to be able to grant moderator status to '" + targetMucAddress + "' in '" + mucAddress + "' (but the server returned an error).");
            }

            // Execute system under test.
            try {
                mucAsSeenByAdmin.revokeModerator(nicknameTarget);
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Expected admin '" + conTwo.getUser() + "' to be able to revoke moderator status from '" + targetMucAddress + "' in '" + mucAddress + "' (but the server returned an error).");
            }

            // Verify result.
            assertFalse(mucAsSeenByAdmin.getModerators().stream().anyMatch(affiliate -> affiliate.getNick().equals(nicknameTarget)), "Expected '" + nicknameTarget + "' to no longer be on the Moderator List after their moderator status was revoked by '" + conTwo.getUser() + "' (an admin) from '" + mucAddress + "' (but their nickname does appear on the Moderator List).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that occupants are notified when moderator status is revoked from an occupant.
     */
    @SmackIntegrationTest(section = "9.7", quote = "The service MUST then send updated presence from this individual to all occupants, indicating the removal of moderator status by sending a presence element that contains an <x/> element qualified by the 'http://jabber.org/protocol/muc#user' namespace and containing an <item/> child with the 'role' attribute set to a value of \"participant\".")
    public void mucTestOccupantsInformed() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-moderator-revoke-broadcast");
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

            final SimpleResultSyncPoint targetSeesRevoke = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint ownerSeesRevoke = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint adminSeesRevoke = new SimpleResultSyncPoint();
            mucAsSeenByTarget.addUserStatusListener(new UserStatusListener()
            {
                @Override
                public void moderatorRevoked()
                {
                    targetSeesRevoke.signal();
                }
            });
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void moderatorRevoked(EntityFullJid participant) {
                    if (targetMucAddress.equals(participant)) {
                        ownerSeesRevoke.signal();
                    }
                }
            });
            mucAsSeenByAdmin.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void moderatorRevoked(EntityFullJid participant) {
                    if (targetMucAddress.equals(participant)) {
                        adminSeesRevoke.signal();
                    }
                }
            });

            // Execute system under test.
            try {
                mucAsSeenByAdmin.revokeModerator(nicknameTarget);
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Expected admin '" + conTwo.getUser() + "' to be able to revoke moderator status from '" + targetMucAddress + "' in '" + mucAddress + "' (but the server returned an error).");
            }

            // Verify result.
            assertResult(targetSeesRevoke, "Expected '" + conThree.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the revokation of moderator status, after their status was revoked by '" + conTwo.getUser() + "' (an admin) in '" + mucAddress + "' (but no such stanza was received).");
            assertResult(ownerSeesRevoke, "Expected '" + conOne.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the revokation of moderator status, after '" + targetMucAddress + "'s status was revoked by '" + conTwo.getUser() + "' (an admin) in '" + mucAddress + "' (but no such stanza was received).");
            assertResult(adminSeesRevoke, "Expected '" + conTwo.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the revokation of moderator status, after '" + targetMucAddress + "'s status was revoked by '" + conTwo.getUser() + "' (an admin) in '" + mucAddress + "' (but no such stanza was received).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that an admin cannot revoke moderator status from another admin
     */
    @SmackIntegrationTest(section = "9.7", quote = "[...] an admin MUST NOT be allowed to revoke moderator status from a user whose affiliation is [...] \"admin\". If an admin attempts to revoke moderator status from such a user, the service MUST deny the request and return a <not-allowed/> error to the sender")
    public void testAdminNotAllowedToRevokeModeratorFromAdmin() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-moderator-revoke-admin-notallowed");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("targetadmin-" + randomString);

        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(List.of(conTwo.getUser().asBareJid(), conThree.getUser().asBareJid()));
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
                mucAsSeenByOwner.grantModerator(List.of(nicknameAdmin, nicknameTarget));
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Expected owner '" + conOne.getUser() + "' to be able to grant moderator status to '" + nicknameTarget + "' and '" + nicknameAdmin +"' in '" + mucAddress + "' (but the server returned an error).");
            }

            final MUCAdmin request = new MUCAdmin();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.addItem(new MUCItem(MUCRole.participant, nicknameTarget));

            // Execute system under test & Verify result.
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
                conTwo.sendIqRequestAndWaitForResponse(request);
            }, "Expected an error after '" + conTwo.getUser() + "' (that is an admin) tried to revoke moderator status from another admin ('" + targetMucAddress + "') for room '" + mucAddress + "' (but none occurred).");
            assertEquals(StanzaError.Condition.not_allowed, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' (that is an admin) after it tried to revoke moderator status from another admin ('" + targetMucAddress + "') for room '" + mucAddress + "'.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that an admin cannot revoke moderator status from an owner.
     */
    @SmackIntegrationTest(section = "9.7", quote = "[...] an admin MUST NOT be allowed to revoke moderator status from a user whose affiliation is \"owner\" [...]. If an admin attempts to revoke moderator status from such a user, the service MUST deny the request and return a <not-allowed/> error to the sender")
    public void testAdminNotAllowedToRevokeModeratorFromOwner() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-moderator-revoke-owner-notallowed");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("targetowner-" + randomString);

        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantOwnership(conThree.getUser().asBareJid());
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
                mucAsSeenByOwner.grantModerator(List.of(nicknameAdmin, nicknameTarget));
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Expected owner '" + conOne.getUser() + "' to be able to grant moderator status to '" + nicknameTarget + "' and '" + nicknameAdmin +"' in '" + mucAddress + "' (but the server returned an error).");
            }

            final MUCAdmin request = new MUCAdmin();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.addItem(new MUCItem(MUCRole.participant, nicknameTarget));

            // Execute system under test & Verify result.
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
                conTwo.sendIqRequestAndWaitForResponse(request);
            }, "Expected an error after '" + conTwo.getUser() + "' (that is an admin) tried to revoke moderator status from an owner ('" + targetMucAddress + "') for room '" + mucAddress + "' (but none occurred).");
            assertEquals(StanzaError.Condition.not_allowed, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' (that is an admin) after it tried to revoke moderator status from an owner ('" + targetMucAddress + "') for room '" + mucAddress + "'.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }
}
