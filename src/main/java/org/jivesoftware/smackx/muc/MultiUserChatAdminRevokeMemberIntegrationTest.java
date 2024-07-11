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
import org.igniterealtime.smack.inttest.util.ResultSyncPoint;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromMatchesFilter;
import org.jivesoftware.smack.filter.PresenceTypeFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smackx.muc.packet.MUCAdmin;
import org.jivesoftware.smackx.muc.packet.MUCItem;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for section "9.4 Admin Use Cases: Revoking Membership" of XEP-0045: "Multi-User Chat"
 *
 * @see <a href="https://xmpp.org/extensions/xep-0045.html#revokemember">XEP-0045 Section 9.4</a>
 */
@SpecificationReference(document = "XEP-0045", version = "1.34.6")
public class MultiUserChatAdminRevokeMemberIntegrationTest extends AbstractMultiUserChatIntegrationTest
{
    public MultiUserChatAdminRevokeMemberIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws SmackException.NoResponseException, XMPPException.XMPPErrorException,
        SmackException.NotConnectedException, InterruptedException, TestNotPossibleException
    {
        super(environment);
    }

    /**
     * Verifies that an admin can revoke membership of a user.
     */
    @SmackIntegrationTest(section = "9.4", quote = "An admin might want to revoke a user's membership; this is done by changing the user's affiliation to \"none\"")
    public void testRevokeMembership() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-member-revoke");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress = JidCreate.entityBareFrom("member-user-" + randomString + "@example.org");

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByAdmin.join(nicknameAdmin);
            mucAsSeenByAdmin.grantMembership(targetAddress);

            // Execute system under test.
            final MUCAdmin request = new MUCAdmin();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.addItem(new MUCItem(MUCAffiliation.none, targetAddress));

            try {
                conTwo.sendIqRequestAndWaitForResponse(request);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected admin '" + conTwo.getUser() + "' to be able to revoke membership from '" + targetAddress + "' in '" + mucAddress + "' (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }


    /**
     * Verifies that an admin can revoke membership from a user while providing an optional reason.
     */
    @SmackIntegrationTest(section = "9.4", quote = "An admin might want to revoke a user's membership; [...] The <reason/> element is OPTIONAL.")
    public void testRevokeMembershipWithReason() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-member-revoke-reason");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress = JidCreate.entityBareFrom("member-user-" + randomString + "@example.org");

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByAdmin.join(nicknameAdmin);
            mucAsSeenByAdmin.grantMembership(targetAddress);

            // Execute system under test.
            final MUCAdmin request = new MUCAdmin();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.addItem(new MUCItem(MUCAffiliation.none, null, null, "Revoke membership as part of a test.", targetAddress, null, null));

            try {
                conTwo.sendIqRequestAndWaitForResponse(request);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected admin '" + conTwo.getUser() + "' to be able to revoke membership (using the optional 'reason' element) from '" + targetAddress + "' in '" + mucAddress + "' (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a non-admin cannot revoke someone's membership.
     */
    @SmackIntegrationTest(section = "9", quote = "[...] revoke membership [...] MUST be denied if the <user@host> of the 'from' address of the request does not match the bare JID of one of the room admins; in this case, the service MUST return a <forbidden/> error.")
    public void testParticipantNotAllowedToRevokeMembership() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-member-revoke-notallowed");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress = JidCreate.entityBareFrom("member-user-" + randomString + "@example.org");

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantMembership(targetAddress);
            mucAsSeenByParticipant.join(nicknameParticipant);

            final MUCAdmin request = new MUCAdmin();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.addItem(new MUCItem(MUCAffiliation.none, null, null, null, targetAddress, null, null));

            // Execute system under test & Verify result.
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
                conTwo.sendIqRequestAndWaitForResponse(request);
            }, "Expected an error after '" + conTwo.getUser() + "' (that is not an admin) tried to revoke membership from another participant ('" + targetAddress + "') for room '" + mucAddress + "' (but none occurred).");
            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' after it tried to revoke membership from another participant ('" + targetAddress + "') for room '" + mucAddress + "' while not being an admin.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a bare JID (of an occupant) from which membership is revoked by an admin no longer appears on the Member List.
     */
    @SmackIntegrationTest(section = "9.4", quote = "An admin might want to revoke a user's membership; [...] The service MUST remove the user from the member list [...] ")
    public void testMemberNotOnMemberList() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-member-not-on-memberlist");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress = JidCreate.entityBareFrom("member-user-" + randomString + "@example.org");

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByAdmin.join(nicknameAdmin);
            mucAsSeenByAdmin.grantMembership(targetAddress);

            // Execute system under test.
            try {
                mucAsSeenByAdmin.revokeMembership(targetAddress);
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Expected admin '" + conTwo.getUser() + "' to be able to revoke membership from '" + targetAddress + "' in '" + mucAddress + "' (but the server returned an error).");
            }

            // Verify result.
            assertFalse(mucAsSeenByAdmin.getMembers().stream().anyMatch(affiliate -> affiliate.getJid().equals(targetAddress)), "Expected '" + targetAddress + "' to no longer be on the Member List after their membership was revoked by '" + conTwo + "' from '" + mucAddress + "' (but the JID does appear on the Member List).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that occupants are notified when membership is revoked from an existing occupant.
     */
    @SmackIntegrationTest(section = "9.4", quote = "An admin might want to revoke a user's membership; [...] The service MUST then send updated presence from this individual to all occupants, indicating the loss of membership by sending a presence element that contains an <x/> element qualified by the 'http://jabber.org/protocol/muc#user' namespace and containing an <item/> child with the 'affiliation' attribute set to a value of \"none\".")
    public void mucTestOccupantsInformedRevoke() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-memberrevoke-broadcast");
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
            mucAsSeenByAdmin.grantMembership(targetAddress);

            final SimpleResultSyncPoint adminSeesTarget = new SimpleResultSyncPoint();
            mucAsSeenByAdmin.addParticipantStatusListener(new ParticipantStatusListener()
            {
                @Override
                public void joined(EntityFullJid participant)
                {
                    if (participant.equals(targetMucAddress)) {
                        adminSeesTarget.signal();
                    }
                }
            });
            mucAsSeenByTarget.join(nicknameTarget);
            adminSeesTarget.waitForResult(timeout);

            final SimpleResultSyncPoint targetSeesRevoke = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint ownerSeesRevoke = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint adminSeesRevoke = new SimpleResultSyncPoint();
            mucAsSeenByTarget.addUserStatusListener(new UserStatusListener()
            {
                @Override
                public void membershipRevoked()
                {
                    targetSeesRevoke.signal();
                }
            });
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener()
            {
                @Override
                public void membershipRevoked(EntityFullJid participant)
                {
                    if (targetMucAddress.equals(participant)) {
                        ownerSeesRevoke.signal();
                    }
                }
            });
            mucAsSeenByAdmin.addParticipantStatusListener(new ParticipantStatusListener()
            {
                @Override
                public void membershipRevoked(EntityFullJid participant)
                {
                    if (targetMucAddress.equals(participant)) {
                        adminSeesRevoke.signal();
                    }
                }
            });

            // Execute system under test.
            try {
                mucAsSeenByAdmin.revokeMembership(targetAddress);
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Expected admin '" + conTwo.getUser() + "' to be able to revoke membership from '" + targetAddress + "' in '" + mucAddress + "' (but the server returned an error).");
            }

            // Verify result.
            assertResult(targetSeesRevoke, "Expected '" + conThree.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the revokation of membership, after it was revoked by '" + conTwo.getUser() + "' in '" + mucAddress + "' (but no such stanza was received).");
            assertResult(ownerSeesRevoke, "Expected '" + conOne.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the revokation of membership, after '" + targetAddress + "' was revoked by '" + conTwo.getUser() + "' in '" + mucAddress + "' (but no such stanza was received).");
            assertResult(adminSeesRevoke, "Expected '" + conTwo.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the revokation of membership, after '" + targetAddress + "' was revoked by '" + conTwo.getUser() + "' in '" + mucAddress + "' (but no such stanza was received).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a user from which membership is revoked is kicked from the room, if the room is members-only.
     */
    @SmackIntegrationTest(section = "9.4", quote = "An admin might want to revoke a user's membership; [...] If the room is members-only, the service MUST remove the user from the room, including a status code of 321 [...]")
    public void mucTestRemovalOfUserInMembersOnlyRoom() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-revoke-removal");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerThree.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress = conThree.getUser().asEntityBareJid();

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);

        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);

        createMembersOnlyMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantMembership(targetAddress);

            final SimpleResultSyncPoint ownerSeesTarget = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener()
            {
                @Override
                public void joined(EntityFullJid participant)
                {
                    if (participant.equals(targetMucAddress)) {
                        ownerSeesTarget.signal();
                    }
                }
            });
            mucAsSeenByTarget.join(nicknameTarget);
            ownerSeesTarget.waitForResult(timeout);

            ResultSyncPoint<Presence, Exception> targetSeesRemoval = new ResultSyncPoint<>();
            final StanzaFilter leaveRoomFilter = new AndFilter(new FromMatchesFilter(targetMucAddress, false), PresenceTypeFilter.UNAVAILABLE);
            conThree.addAsyncStanzaListener(stanza -> targetSeesRemoval.signal((Presence) stanza), leaveRoomFilter);

            // Execute system under test.
            mucAsSeenByOwner.revokeMembership(targetAddress);

            // Verify result
            final Presence presence = assertResult(targetSeesRemoval, "Expected '" + conThree.getUser() + "' to receive a presence 'unavailable' stanza from room '" + mucAddress + "' indicating that they removed from the room, after '" + conOne.getUser() + "' revoked their membership from the (members-only) room (but that presence stanza did not arrive).");
            final MUCUser extension = presence.getExtension(MUCUser.class);
            assertNotNull(extension, "Expected an <x/> child element qualified by the 'http://jabber.org/protocol/muc#user' namespace to exist in the presence stanza received by '" + conThree.getUser() + "' when they were removed from members-only room '" + mucAddress + "' when '" + conOne.getUser() + "' revoked their membership (but no such child element was detected).");
            assertTrue(extension.getItem() != null && extension.getItem().getAffiliation().equals(MUCAffiliation.none), "Expected to find an item with affiliation 'none' in the presence stanza received by '" + conThree.getUser() + "' when they were removed from members-only room '" + mucAddress + "' when '" + conOne.getUser() + "' revoked their membership (but it was not).");
            assertTrue(extension.getStatus().stream().anyMatch(status -> status.getCode() == 321), "Expected to find status code 321 in the presence stanza received by '" + conThree.getUser() + "' when they were removed from members-only room '" + mucAddress + "' when '" + conOne.getUser() + "' revoked their membership (but it was not).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that other occupants are notified when a user is kicked from a members-only room whein their membership is revoked.
     */
    @SmackIntegrationTest(section = "9.4", quote = "An admin might want to revoke a user's membership; [...] If the room is members-only, the service MUST remove the user from the room, including a status code of 321 [...] and inform all remaining occupants")
    public void mucTestOccupantsInformedKick() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-revoke-removal-notif");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByMember = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerThree.getMultiUserChat(mucAddress);
        final EntityBareJid memberAddress = conTwo.getUser().asEntityBareJid();
        final EntityBareJid targetAddress = conThree.getUser().asEntityBareJid();

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameMember = Resourcepart.from("member-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);

        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);

        createMembersOnlyMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantMembership(memberAddress);
            mucAsSeenByOwner.grantMembership(targetAddress);
            mucAsSeenByMember.join(nicknameMember);

            final SimpleResultSyncPoint memberSeesTarget = new SimpleResultSyncPoint();
            mucAsSeenByMember.addParticipantStatusListener(new ParticipantStatusListener()
            {
                @Override
                public void joined(EntityFullJid participant)
                {
                    if (participant.equals(targetMucAddress)) {
                        memberSeesTarget.signal();
                    }
                }
            });
            mucAsSeenByTarget.join(nicknameTarget);
            memberSeesTarget.waitForResult(timeout);

            ResultSyncPoint<Presence, Exception> memberSeesRemoval = new ResultSyncPoint<>();
            final StanzaFilter leaveRoomFilter = new AndFilter(new FromMatchesFilter(targetMucAddress, false), PresenceTypeFilter.UNAVAILABLE);
            conTwo.addAsyncStanzaListener(stanza -> memberSeesRemoval.signal((Presence) stanza), leaveRoomFilter);

            // Execute system under test.
            mucAsSeenByOwner.revokeMembership(targetAddress);

            // Verify result
            final Presence presence = assertResult(memberSeesRemoval, "Expected '" + conTwo.getUser() + "' to receive a presence 'unavailable' stanza from room '" + mucAddress + "' indicating that '" + conThree.getUser() + "' was removed from the room, as a result of '" + conOne.getUser() + "' revoking their membership from the (members-only) room (but that presence stanza did not arrive).");
            final MUCUser extension = presence.getExtension(MUCUser.class);
            assertNotNull(extension, "Expected an <x/> child element qualified by the 'http://jabber.org/protocol/muc#user' namespace to exist in the presence stanza received by '" + conTwo.getUser() + "' when '" + conThree.getUser() + "' was removed from members-only room '" + mucAddress + "' as a result of '" + conOne.getUser() + "' revoking their membership (but no such child element was detected).");
            assertTrue(extension.getItem() != null && extension.getItem().getAffiliation().equals(MUCAffiliation.none), "Expected to find an item with affiliation 'none' in the presence stanza received by '" + conTwo.getUser() + "' when '" + conThree.getUser() + "' was removed from members-only room '" + mucAddress + "' as a result of '" + conOne.getUser() + "' revoking their membership (but it was not).");
            assertTrue(extension.getStatus().stream().anyMatch(status -> status.getCode() == 321), "Expected to find status code 321 in the presence stanza received by '" + conTwo.getUser() + "' when '" + conThree.getUser() + "' was removed from members-only room '" + mucAddress + "' as a result of '" + conOne.getUser() + "' revoking their membership (but it was not).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }
}
