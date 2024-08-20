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
 * Tests for section "9.3 Admin Use Cases: Granting Membership" of XEP-0045: "Multi-User Chat"
 *
 * @see <a href="https://xmpp.org/extensions/xep-0045.html#grantmember">XEP-0045 Section 9.3</a>
 */
@SpecificationReference(document = "XEP-0045", version = "1.34.6")
public class MultiUserChatAdminGrantMemberIntegrationTest extends AbstractMultiUserChatIntegrationTest
{
    public MultiUserChatAdminGrantMemberIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
        InterruptedException, TestNotPossibleException, MultiUserChatException.MucAlreadyJoinedException, MultiUserChatException.MissingMucCreationAcknowledgeException, MultiUserChatException.NotAMucServiceException, XmppStringprepException
    {
        super(environment);
    }

    /**
     * Verifies that an admin can grant membership to a user.
     */
    @SmackIntegrationTest(section = "9.3", quote = "An admin can grant membership to a user; this is done by changing the affiliation for the user's bare JID to \"member\"")
    public void testGrantMembership() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-member-grant");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress = JidCreate.entityBareFrom("target-user-" + randomString + "@example.org");

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByAdmin.join(nicknameAdmin);

            // Execute system under test.
            final MUCAdmin request = new MUCAdmin();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.addItem(new MUCItem(MUCAffiliation.member, targetAddress));

            try {
                conTwo.sendIqRequestAndWaitForResponse(request);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected '" + conTwo.getUser() + "'(an admin) to be able to grant membership to '" + targetAddress + "' in '" + mucAddress + "' (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that an admin can grant membership to a user while providing an optional nick.
     */
    @SmackIntegrationTest(section = "9.3", quote = "An admin can grant membership to a user; this is done by changing the affiliation for the user's bare JID to \"member\" (if a nick is provided, that nick becomes the user's default nick in the room if that functionality is supported by the implementation).")
    public void testGrantMembershipWithNick() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-member-grant-nick");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress = JidCreate.entityBareFrom("member-user-" + randomString + "@example.org");

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("member-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByAdmin.join(nicknameAdmin);

            // Execute system under test.
            final MUCAdmin request = new MUCAdmin();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.addItem(new MUCItem(MUCAffiliation.member, null, null, null, targetAddress, nicknameTarget, null));

            try {
                conTwo.sendIqRequestAndWaitForResponse(request);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected '" + conTwo.getUser() + "' (an admin) to be able to grant membership to '" + targetAddress + "' in '" + mucAddress + "' (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that an admin can grant membership to a user while providing an optional reason.
     */
    @SmackIntegrationTest(section = "9.3", quote = "An admin can grant membership to a user; [...] The <reason/> element is OPTIONAL.")
    public void testGrantMembershipWithReason() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-member-grant-reason");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress = JidCreate.entityBareFrom("member-user-" + randomString + "@example.org");

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByAdmin.join(nicknameAdmin);

            // Execute system under test.
            final MUCAdmin request = new MUCAdmin();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.addItem(new MUCItem(MUCAffiliation.member, null, null, "Granted membership as part of a test.", targetAddress, null, null));

            try {
                conTwo.sendIqRequestAndWaitForResponse(request);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected '" + conTwo.getUser() + "' (an admin) to be able to grant membership to '" + targetAddress + "' in '" + mucAddress + "' (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a non-admin cannot grant someone membership.
     */
    @SmackIntegrationTest(section = "9", quote = "grant [...] membership [...] MUST be denied if the <user@host> of the 'from' address of the request does not match the bare JID of one of the room admins; in this case, the service MUST return a <forbidden/> error.")
    public void testParticipantNotAllowedToGrantMembership() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-member-grant-notallowed");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress = JidCreate.entityBareFrom("member-user-" + randomString + "@example.org");

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByParticipant.join(nicknameParticipant);

            // Execute system under test.
            final MUCAdmin request = new MUCAdmin();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.addItem(new MUCItem(MUCAffiliation.member, targetAddress));

            // Execute system under test & Verify result.
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
                conTwo.sendIqRequestAndWaitForResponse(request);
            }, "Expected an error after '" + conTwo.getUser() + "' (that is not an admin) tried to grant membership to another participant ('" + targetAddress + "') for room '" + mucAddress + "' (but none occurred).");
            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' after it tried to grant membership to another participant ('" + targetAddress + "') for room '" + mucAddress + "' while not being an admin.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a bare JID (of an occupant) that is granted membership by an admin appears on the Member List.
     */
    @SmackIntegrationTest(section = "9.3", quote = "An admin can grant membership to a user; [...] The service MUST add the user to the member list [...] ")
    public void testMemberOnMemberList() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-member-on-memberlist");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress = JidCreate.entityBareFrom("member-user-" + randomString + "@example.org");

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByAdmin.join(nicknameAdmin);

            // Execute system under test.
            final MUCAdmin request = new MUCAdmin();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.addItem(new MUCItem(MUCAffiliation.member, targetAddress));

            try {
                conTwo.sendIqRequestAndWaitForResponse(request);
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Expected '" + conTwo.getUser() + "' (an admin)to be able to grant membership to '" + targetAddress + "' in '" + mucAddress + "' (but the server returned an error).");
            }

            // Verify result.
            assertTrue(mucAsSeenByAdmin.getMembers().stream().anyMatch(affiliate -> affiliate.getJid().equals(targetAddress)), "Expected '" + targetAddress + "' to be on the Member List after the were granted membership by '" + conTwo.getUser() + "' (an admin) to '" + mucAddress + "' (but the JID does not appear on the Member List).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that occupants are notified when an existing occupant is granted membership by an admin.
     */
    @SmackIntegrationTest(section = "9.3", quote = "If the user is in the room, the service MUST then send updated presence from this individual to all occupants, indicating the granting of membership by including an <x/> element qualified by the 'http://jabber.org/protocol/muc#user' namespace and containing an <item/> child with the 'affiliation' attribute set to a value of \"member\".")
    public void mucTestOccupantsInformed() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-member-broadcast");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerThree.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress = conThree.getUser().asEntityBareJid();

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);

        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByAdmin.join(nicknameAdmin);

            final SimpleResultSyncPoint adminSeesTarget = new SimpleResultSyncPoint();
            mucAsSeenByAdmin.addParticipantStatusListener(new ParticipantStatusListener()
            {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        adminSeesTarget.signal();
                    }
                }
            });
            mucAsSeenByTarget.join(nicknameTarget);
            adminSeesTarget.waitForResult(timeout);

            final SimpleResultSyncPoint targetSeesMembership = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint ownerSeesMembership = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint adminSeesMembership = new SimpleResultSyncPoint();
            mucAsSeenByTarget.addUserStatusListener(new UserStatusListener()
            {
                @Override
                public void membershipGranted()
                {
                    targetSeesMembership.signal();
                }
            });
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void membershipGranted(EntityFullJid participant) {
                    if (targetMucAddress.equals(participant)) {
                        ownerSeesMembership.signal();
                    }
                }
            });
            mucAsSeenByAdmin.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void membershipGranted(EntityFullJid participant) {
                    if (targetMucAddress.equals(participant)) {
                        adminSeesMembership.signal();
                    }
                }
            });

            // Execute system under test.
            try {
                mucAsSeenByAdmin.grantMembership(targetAddress);
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Expected '" + conTwo.getUser() + "' (an admin) to be able to grant membership to '" + targetAddress + "' in '" + mucAddress + "' (but the server returned an error).");
            }

            // Verify result.
            assertResult(targetSeesMembership, "Expected '" + conThree.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the granting of membership, after they are granted membership by '" + conTwo.getUser() + "' (an admin) in '" + mucAddress + "' (but no such stanza was received).");
            assertResult(ownerSeesMembership, "Expected '" + conOne.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the granting of membership, after '" + targetAddress + "' is granted membership by '" + conTwo.getUser() + "' (an admin) in '" + mucAddress + "' (but no such stanza was received).");
            assertResult(adminSeesMembership, "Expected '" + conTwo.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the granting of membership, after '" + targetAddress + "' is granted membership by '" + conTwo.getUser() + "' (an admin) in '" + mucAddress + "' (but no such stanza was received).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }
}
