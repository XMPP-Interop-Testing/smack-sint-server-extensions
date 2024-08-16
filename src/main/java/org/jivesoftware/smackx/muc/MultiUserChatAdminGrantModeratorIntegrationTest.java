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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for section "9.6 Admin Use Cases: Granting Moderator Status" of XEP-0045: "Multi-User Chat"
 *
 * @see <a href="https://xmpp.org/extensions/xep-0045.html#grantmod">XEP-0045 Section 9.6</a>
 */
@SpecificationReference(document = "XEP-0045", version = "1.34.6")
public class MultiUserChatAdminGrantModeratorIntegrationTest extends AbstractMultiUserChatIntegrationTest
{
    public MultiUserChatAdminGrantModeratorIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
        InterruptedException, TestNotPossibleException, MultiUserChatException.MucAlreadyJoinedException, MultiUserChatException.MissingMucCreationAcknowledgeException, MultiUserChatException.NotAMucServiceException, XmppStringprepException
    {
        super(environment);
    }

    /**
     * Verifies that an admin can grant moderator status to a user.
     */
    @SmackIntegrationTest(section = "9.6", quote = "An admin might want to grant moderator status to a participant or visitor; this is done by changing the user's role to \"moderator\"")
    public void testGrantModerator() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-moderator-grant");
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

            // Execute system under test.
            final MUCAdmin request = new MUCAdmin();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.addItem(new MUCItem(MUCRole.moderator, nicknameTarget));
            try {
                conTwo.sendIqRequestAndWaitForResponse(request);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected admin '" + conTwo.getUser() + "' to be able to grant moderator status to '" + targetMucAddress + "' in '" + mucAddress + "' (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that an admin can grant moderator status to a user.
     */
    @SmackIntegrationTest(section = "9.6", quote = "An admin might want to grant moderator status to a participant or visitor; this is done by changing the user's role to \"moderator\" [...] The <reason/> element is OPTIONAL.")
    public void testGrantModeratorOptionalReason() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-moderator-grant-reason");
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

            // Execute system under test.
            final MUCAdmin request = new MUCAdmin();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.addItem(new MUCItem(MUCRole.moderator, nicknameTarget, "Granting Moderator Status as part of an integration test."));

            try {
                conTwo.sendIqRequestAndWaitForResponse(request);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected admin '" + conTwo.getUser() + "' to be able to grant moderator status to '" + targetMucAddress + "' in '" + mucAddress + "' when providing an optional 'reason' element (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a non-admin cannot grant someone moderator status.
     */
    @SmackIntegrationTest(section = "9", quote = "grant [...] moderator status [...] MUST be denied if the <user@host> of the 'from' address of the request does not match the bare JID of one of the room admins; in this case, the service MUST return a <forbidden/> error.")
    public void testParticipantNotAllowedToGrantModeratorStatus() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-moderator-notallowed");
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
            participantSeesTarget.waitForResult(timeout);

            // Execute system under test.
            final MUCAdmin request = new MUCAdmin();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.addItem(new MUCItem(MUCRole.moderator, nicknameTarget, "Granting Moderator Status as part of an integration test."));

            // Verify result.
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
                conTwo.sendIqRequestAndWaitForResponse(request);
            }, "Expected an error after '" + conTwo.getUser() + "' (that is not an admin) tried to grant moderator status to another participant ('" + targetMucAddress + "') for room '" + mucAddress + "' (but none occurred).");
            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' after it tried to grant moderator status to another participant ('" + targetMucAddress + "') for room '" + mucAddress + "' while not being an admin.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a bare JID (of an occupant) that is granted moderator status by an admin appears on the Moderator List.
     */
    @SmackIntegrationTest(section = "9.6", quote = "An admin might want to grant moderator status to a participant or visitor; [...] The service MUST add the user to the moderator list  [...] ")
    public void testModeratorOnModeratorList() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-moderator-on-moderatorlist");
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

            // Execute system under test.
            final MUCAdmin request = new MUCAdmin();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.addItem(new MUCItem(MUCRole.moderator, nicknameTarget));

            try {
                conTwo.sendIqRequestAndWaitForResponse(request);
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Expected admin '" + conTwo.getUser() + "' to be able to grant moderator status to '" + nicknameTarget + "' in '" + mucAddress + "' (but the server returned an error).");
            }

            // Verify result.
            assertTrue(mucAsSeenByAdmin.getModerators().stream().anyMatch(affiliate -> affiliate.getNick().equals(nicknameTarget)), "Expected '" + nicknameTarget + "' to be on the Moderator List after the were granted moderator status by '" + conTwo + "' in '" + mucAddress + "' (but the nickname does not appear on the Moderator List).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that occupants are notified when an existing occupant is granted moderator status.
     */
    @SmackIntegrationTest(section = "9.6", quote = "The service MUST then send updated presence from this individual to all occupants, indicating the addition of moderator status by including an <x/> element qualified by the 'http://jabber.org/protocol/muc#user' namespace and containing an <item/> child with the 'role' attribute set to a value of \"moderator\".")
    public void mucTestOccupantsInformed() throws Exception {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-moderator-broadcast");
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

            final SimpleResultSyncPoint targetSeesModerator = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint ownerSeesModerator = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint adminSeesModerator = new SimpleResultSyncPoint();
            mucAsSeenByTarget.addUserStatusListener(new UserStatusListener()
            {
                @Override
                public void moderatorGranted()
                {
                    targetSeesModerator.signal();
                }
            });
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void moderatorGranted(EntityFullJid participant) {
                    if (targetMucAddress.equals(participant)) {
                        ownerSeesModerator.signal();
                    }
                }
            });
            mucAsSeenByAdmin.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void moderatorGranted(EntityFullJid participant) {
                    if (targetMucAddress.equals(participant)) {
                        adminSeesModerator.signal();
                    }
                }
            });

            // Execute system under test.
            final MUCAdmin request = new MUCAdmin();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.addItem(new MUCItem(MUCRole.moderator, nicknameTarget));

            try {
                conTwo.sendIqRequestAndWaitForResponse(request);
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Expected admin '" + conTwo.getUser() + "' to be able to grant moderator status to '" + nicknameTarget + "' in '" + mucAddress + "' (but the server returned an error).");
            }

            // Verify result.
            assertResult(targetSeesModerator, "Expected '" + conThree.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the granting of moderator status, after they are granted moderator status by '" + conTwo.getUser() + "' in '" + mucAddress + "' (but no such stanza was received).");
            assertResult(ownerSeesModerator, "Expected '" + conOne.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the granting of moderator status, after '" + targetMucAddress + "' is granted moderator status by '" + conTwo.getUser() + "' in '" + mucAddress + "' (but no such stanza was received).");
            assertResult(adminSeesModerator, "Expected '" + conTwo.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the granting of moderator status, after '" + targetMucAddress + "' is granted moderator status by '" + conTwo.getUser() + "' in '" + mucAddress + "' (but no such stanza was received).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }
}
