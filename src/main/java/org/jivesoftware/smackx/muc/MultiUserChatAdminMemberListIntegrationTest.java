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
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.muc.packet.MUCAdmin;
import org.jivesoftware.smackx.muc.packet.MUCItem;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for section "9.5 Admin Use Cases: Modifying the Member List" of XEP-0045: "Multi-User Chat"
 *
 * @see <a href="https://xmpp.org/extensions/xep-0045.html#modifymember">XEP-0045 Section 9.5</a>
 */
@SpecificationReference(document = "XEP-0045", version = "1.34.6")
public class MultiUserChatAdminMemberListIntegrationTest extends AbstractMultiUserChatIntegrationTest
{
    public MultiUserChatAdminMemberListIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
        InterruptedException, TestNotPossibleException, MultiUserChatException.MucAlreadyJoinedException, MultiUserChatException.MissingMucCreationAcknowledgeException, MultiUserChatException.NotAMucServiceException, XmppStringprepException
    {
        super(environment);
    }

    /**
     * Asserts that a member list can be obtained.
     */
    @SmackIntegrationTest(section = "9.5", quote = "The admin [...] requests the member list by querying the room for all users with an affiliation of \"member\"")
    public void mucTestAdminRequestsMemberList() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-requests-memberlist");
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
            iq.addItem(new MUCItem(MUCAffiliation.member));
            conTwo.sendIqRequestAndWaitForResponse(iq);

            // Verify result.
        } catch (XMPPException.XMPPErrorException e) {
            fail("Expected admin '" + conTwo.getUser() + "' to be able to receive the member list from '" + mucAddress + "' (but the server returned an error).", e);
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that a member list item has 'affiliation' and 'jid' attributes.
     */
    @SmackIntegrationTest(section = "9.5", quote = "each item MUST include the 'affiliation' and 'jid' attributes")
    public void mucTestAdminMemberListItemCheck() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-memberlist-attr");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress = JidCreate.entityBareFrom("test@example.org");

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantMembership(targetAddress);
            mucAsSeenByAdmin.join(nicknameAdmin);

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.get);
            iq.addItem(new MUCItem(MUCAffiliation.member));
            final MUCAdmin response = conTwo.sendIqRequestAndWaitForResponse(iq);

            // Verify result.
            assertFalse(response.getItems().stream().anyMatch(i -> i.getJid() == null), "The member list for '" + mucAddress + "' contains an item that does not have a 'jid' attribute (but all items must have one).");
            assertFalse(response.getItems().stream().anyMatch(i -> i.getAffiliation() == null), "The member list for '" + mucAddress + "' contains an item that does not have an 'affiliation' attribute (but all items must have one).");
            assertTrue(response.getItems().stream().anyMatch(i -> i.getJid().equals(targetAddress)), "Expected the member list requested by '" + conTwo.getUser() + "' from '" + mucAddress + "' to include the recently added member '" + targetAddress + "' (but it did not).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that a member list modification can contain more than one item.
     */
    @SmackIntegrationTest(section = "9.5", quote = "The admin can then modify the member list if desired. In order to do so, the admin MUST send the changed items [...] to the service; [...] The service MUST modify the member list and then inform the moderator of success:")
    public void mucTestAdminMemberListMultipleItems() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-memberlist-multiple");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress1 = JidCreate.entityBareFrom("test1@example.org");
        final EntityBareJid targetAddress2 = JidCreate.entityBareFrom("test2@example.com");

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByAdmin.join(nicknameAdmin);

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.set);
            iq.addItem(new MUCItem(MUCAffiliation.member, targetAddress1));
            iq.addItem(new MUCItem(MUCAffiliation.member, targetAddress2));

            try {
                conTwo.sendIqRequestAndWaitForResponse(iq);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected the service to inform '" + conTwo.getUser() + "' of success after they modified the member list of room '" + mucAddress + "' (but instead, an error was returned).", e);
            }
            assertTrue(mucAsSeenByAdmin.getMembers().stream().anyMatch(i -> i.getAffiliation() == MUCAffiliation.member && i.getJid().equals(targetAddress1)), "Expected the member list for '" + mucAddress + "' to contain '" + targetAddress1 + "' that was just added to the member list by '" + conTwo.getUser() + "' (but does not appear on the member list).");
            assertTrue(mucAsSeenByAdmin.getMembers().stream().anyMatch(i -> i.getAffiliation() == MUCAffiliation.member && i.getJid().equals(targetAddress2)), "Expected the member list for '" + mucAddress + "' to contain '" + targetAddress2 + "' that was just added to the member list by '" + conTwo.getUser() + "' (but does not appear on the member list).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that a member list modification can be used to remove people from the member list.
     */
    @SmackIntegrationTest(section = "9.5", quote = "The admin can then modify the member list if desired. [...] each item MUST include the 'affiliation' attribute (normally set to a value of \"member\" or \"none\")")
    public void mucTestAdminMemberListMultipleItemsRevoke() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-memberlist-revoke");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress1 = JidCreate.entityBareFrom("test1@example.org");
        final EntityBareJid targetAddress2 = JidCreate.entityBareFrom("test2@example.com");

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByAdmin.join(nicknameAdmin);

            mucAsSeenByAdmin.grantMembership(List.of(targetAddress1, targetAddress2));

            // Execute system under test
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.set);
            iq.addItem(new MUCItem(MUCAffiliation.none, targetAddress1));
            iq.addItem(new MUCItem(MUCAffiliation.none, targetAddress2));

            try {
                conTwo.sendIqRequestAndWaitForResponse(iq);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected the service to inform '" + conTwo.getUser() + "' of success after they modified the member list of room '" + mucAddress + "' (but instead, an error was returned).", e);
            }
            assertTrue(mucAsSeenByAdmin.getMembers().stream().noneMatch(i -> i.getAffiliation() == MUCAffiliation.member && i.getJid().equals(targetAddress1)), "Expected the member list for '" + mucAddress + "' to no longer contain '" + targetAddress1 + "' that was just removed from the member list by '" + conTwo.getUser() + "' (but does appear on the member list).");
            assertTrue(mucAsSeenByAdmin.getMembers().stream().noneMatch(i -> i.getAffiliation() == MUCAffiliation.member && i.getJid().equals(targetAddress2)), "Expected the member list for '" + mucAddress + "' to no longer contain '" + targetAddress2 + "' that was just removed from the member list by '" + conTwo.getUser() + "' (but does appear on the member list).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that a member list modification is a delta: it shouldn't affect entries already on the member list that are
     * not included in the delta.
     */
    @SmackIntegrationTest(section = "9.5", quote = "The admin can then modify the member list if desired. In order to do so, the admin MUST send the changed items (i.e., only the \"delta\")")
    public void mucTestAdminMemberListIsDelta() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-memberlist-delta");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress1 = JidCreate.entityBareFrom("test1@example.org");
        final EntityBareJid targetAddress2 = JidCreate.entityBareFrom("test2@example.com");
        final EntityBareJid targetAddress3 = JidCreate.entityBareFrom("test3@example.net");

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByAdmin.join(nicknameAdmin);
            mucAsSeenByAdmin.grantMembership(List.of(targetAddress1, targetAddress2));

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.set);
            iq.addItem(new MUCItem(MUCAffiliation.none, targetAddress1));
            iq.addItem(new MUCItem(MUCAffiliation.member, targetAddress3));

            try {
                conTwo.sendIqRequestAndWaitForResponse(iq);
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Expected the service to inform '" + conTwo.getUser() + "' of success after they modified the member list of room '" + mucAddress + "' (but instead, an error was returned).");
            }

            // Verify result.
            final Set<Jid> members = mucAsSeenByAdmin.getMembers().stream().filter(i -> i.getAffiliation().equals(MUCAffiliation.member)).map(Affiliate::getJid).collect(Collectors.toSet());
            assertFalse(members.contains(targetAddress1), "Expected '" + targetAddress1 + "' to no longer be on the member list of '" + mucAddress + "', after '" + conTwo.getUser() + "' updated the member list that previously contained them with their removal (but does still appear on the member list).");
            assertTrue(members.contains(targetAddress2), "Expected '" + targetAddress2 + "' to be on the member list of '" + mucAddress + "', after the member list that previously contained them got updated by '" + conTwo.getUser() + "' with different items (which should have been applied as a delta).");
            assertTrue(members.contains(targetAddress3), "Expected '" + targetAddress3 + "' to be on the member list of '" + mucAddress + "', after the member list that previously did not contain them got updated by '" + conTwo.getUser() + "' with items that include them.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that occupants that get their membership revoked by modification of the member list are prevented
     * from accessing the room
     */
    @SmackIntegrationTest(section = "9.5", quote = "If a removed member is currently in a members-only room [..] The service MUST subsequently refuse entry to the user.")
    public void mucTestMemberListNoEntryAfterRevoke() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-memberlist-noentry");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerThree.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress = conThree.getUser().asEntityBareJid();

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);

        createMembersOnlyMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantMembership(targetAddress);

            // Execute system under test.
            mucAsSeenByOwner.revokeMembership(targetAddress);

            // Verify result.
            assertThrows(XMPPException.XMPPErrorException.class, () -> mucAsSeenByTarget.join(nicknameTarget), "Expected '" + conThree.getUser() + "' to receive an error when trying to join room '" + mucAddress + "' after '" + conOne.getUser() + "' removed them from the member list (but no error was received).'");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that other occupants are notified when member list changes are made.
     */
    @SmackIntegrationTest(section = "9.5", quote = "[for a removed member] For all room types, the service MUST send updated presence from this individual to all occupants, indicating the change in affiliation by including an <x/> element qualified by the 'http://jabber.org/protocol/muc#user' namespace and containing an <item/> child with the 'affiliation' attribute set to a value of \"none\".")
    public void mucTestMemberListRemainingOccupantsInformedOfRevokeOpenRoom() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-memberlist-broadcast-revoke-open");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerThree.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress1 = conThree.getUser().asEntityBareJid();
        final EntityBareJid targetAddress2 = JidCreate.entityBareFrom("test2@example.com");

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);

        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByOwner.grantMembership(List.of(targetAddress1, targetAddress2));

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

            final SimpleResultSyncPoint ownerSeesRevoke = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint adminSeesRevoke = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void membershipRevoked(EntityFullJid participant) {
                    if (targetMucAddress.equals(participant)) {
                        ownerSeesRevoke.signal();
                    }
                }
            });
            mucAsSeenByAdmin.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void membershipRevoked(EntityFullJid participant) {
                    if (targetMucAddress.equals(participant)) {
                        adminSeesRevoke.signal();
                    }
                }
            });

            // Execute system under test.
            mucAsSeenByAdmin.revokeMembership(List.of(targetAddress1, targetAddress2));

            // Verify result.
            assertResult(ownerSeesRevoke, "Expected '" + conOne.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the change in affiliation by including an <x/> element qualified by the 'http://jabber.org/protocol/muc#user' namespace and containing an <item/> child with the 'affiliation' attribute set to a value of \"none\" for '" + targetAddress1 + "' after its membership was revoked by '" + conTwo.getUser() + "' in '" + mucAddress + "' which is configured to be an open room (but no such stanza was received).");
            assertResult(adminSeesRevoke, "Expected '" + conTwo.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the change in affiliation by including an <x/> element qualified by the 'http://jabber.org/protocol/muc#user' namespace and containing an <item/> child with the 'affiliation' attribute set to a value of \"none\" for '" + targetAddress1 + "' after its membership was revoked by '" + conTwo.getUser() + "' in '" + mucAddress + "' which is configured to be an open room (but no such stanza was received).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that other occupants are notified when member list changes are made.
     */
    @SmackIntegrationTest(section = "9.5", quote = "[for a removed member] For all room types, the service MUST send updated presence from this individual to all occupants, indicating the change in affiliation by including an <x/> element qualified by the 'http://jabber.org/protocol/muc#user' namespace and containing an <item/> child with the 'affiliation' attribute set to a value of \"none\".")
    public void mucTestMemberListRemainingOccupantsInformedOfRevokeMemberOnlyRoom() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-memberlist-broadcast-revoke-memberonly");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerThree.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress1 = conThree.getUser().asEntityBareJid();
        final EntityBareJid targetAddress2 = JidCreate.entityBareFrom("test2@example.com");

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);

        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);

        createMembersOnlyMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByOwner.grantMembership(List.of(targetAddress1, targetAddress2));

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

            final ResultSyncPoint<Presence, Exception> ownerSeesRevoke = new ResultSyncPoint<>();
            final ResultSyncPoint<Presence, Exception> adminSeesRevoke = new ResultSyncPoint<>();
            final StanzaFilter notificationFilter = new AndFilter(new FromMatchesFilter(targetMucAddress, false), StanzaTypeFilter.PRESENCE, new ExtensionElementFilter<>(MUCUser.class));
            conOne.addAsyncStanzaListener(stanza -> ownerSeesRevoke.signal((Presence) stanza), notificationFilter);
            conTwo.addAsyncStanzaListener(stanza -> adminSeesRevoke.signal((Presence) stanza), notificationFilter);

            // Execute system under test.
            mucAsSeenByAdmin.revokeMembership(List.of(targetAddress1, targetAddress2));

            // Verify result.
            final Presence ownerReceivedPresence = assertResult(ownerSeesRevoke, "Expected '" + conOne.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the change in affiliation by including an <x/> element qualified by the 'http://jabber.org/protocol/muc#user' namespace and containing an <item/> child with the 'affiliation' attribute set to a value of \"none\" for '" + targetAddress1 + "' after its membership was revoked by '" + conTwo.getUser() + "' in '" + mucAddress + "' which is configured to be a member-only room (but no such stanza was received).");
            final Presence adminReceivedPresence = assertResult(adminSeesRevoke, "Expected '" + conTwo.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the change in affiliation by including an <x/> element qualified by the 'http://jabber.org/protocol/muc#user' namespace and containing an <item/> child with the 'affiliation' attribute set to a value of \"none\" for '" + targetAddress1 + "' after its membership was revoked by '" + conTwo.getUser() + "' in '" + mucAddress + "' which is configured to be a member-only room (but no such stanza was received).");
            assertTrue(ownerReceivedPresence.getExtension(MUCUser.class).getItem() != null && ownerReceivedPresence.getExtension(MUCUser.class).getItem().getAffiliation().equals(MUCAffiliation.none), "Expected to find an item with affiliation 'none' in the presence stanza received by '" + conOne.getUser() + "' when '" + conThree.getUser() + "' was removed from members-only room '" + mucAddress + "' as a result of '" + conTwo.getUser() + "' revoking their membership (but it was not).");
            assertTrue(adminReceivedPresence.getExtension(MUCUser.class).getItem() != null && ownerReceivedPresence.getExtension(MUCUser.class).getItem().getAffiliation().equals(MUCAffiliation.none), "Expected to find an item with affiliation 'none' in the presence stanza received by '" + conTwo.getUser() + "' when '" + conThree.getUser() + "' was removed from members-only room '" + mucAddress + "' as a result of '" + conTwo.getUser() + "' revoking their membership (but it was not).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that sending an invitation in an open room does not automatically add the invitee to the member list.
     */
    @SmackIntegrationTest(section = "9.5", quote = "Invitations sent through an open room MUST NOT trigger the addition of the invitee to the member list.")
    public void mucTestMemberListNoChangeWithInviteInOpenROom() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-memberlist-invite-open");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);

        final EntityBareJid targetAddress = conThree.getUser().asEntityBareJid();

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            // Execute system under test.
            mucAsSeenByOwner.invite(targetAddress, "Invitation as part of an integration test.");

            // Verify result.
            assertFalse(mucAsSeenByOwner.getMembers().stream().anyMatch(i -> i.getJid().equals(targetAddress)), "Did not expect '" + targetAddress + "' to be on the member list of open room '" + mucAddress + "' after they were invited by '" + conOne.getUser() + "' (but they are on the member list).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that other occupants are notified when member list changes are made.
     */
    @SmackIntegrationTest(section = "9.5", quote = "If a user is added to the member list of an open room and the user is in the room, the service MUST send updated presence from this individual to all occupants, indicating the change in affiliation by including an <x/> element qualified by the 'http://jabber.org/protocol/muc#user' namespace and containing an <item/> child with the 'affiliation' attribute set to a value of \"member\".")
    public void mucTestMemberListOccupantsInformedOfGrantOpenRoom() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-memberlist-broadcast-grant-open");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget1 = mucManagerThree.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress1 = conThree.getUser().asEntityBareJid();
        final EntityBareJid targetAddress2 = JidCreate.entityBareFrom("test2@example.com");

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
            mucAsSeenByTarget1.join(nicknameTarget);
            adminSeesTarget.waitForResult(timeout);

            final SimpleResultSyncPoint ownerSeesGrant = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint adminSeesGrant = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint target1SeesGrant = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void membershipGranted(EntityFullJid participant) {
                    if (targetMucAddress.equals(participant)) {
                        ownerSeesGrant.signal();
                    }
                }
            });
            mucAsSeenByAdmin.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void membershipGranted(EntityFullJid participant) {
                    if (targetMucAddress.equals(participant)) {
                        adminSeesGrant.signal();
                    }
                }
            });
            mucAsSeenByTarget1.addUserStatusListener(new UserStatusListener() {
                @Override
                public void membershipGranted() {
                    target1SeesGrant.signal();
                }
            });

            // Execute system under test.
            mucAsSeenByAdmin.grantMembership(List.of(targetAddress1, targetAddress2));

            // Verify result.
            assertResult(ownerSeesGrant, "Expected '" + conOne.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the change in affiliation by including an <x/> element qualified by the 'http://jabber.org/protocol/muc#user' namespace and containing an <item/> child with the 'affiliation' attribute set to a value of \"member\" for '" + targetAddress1 + "' after its membership was granted by '" + conTwo.getUser() + "' in '" + mucAddress + "' which is configured to be an open room (but no such stanza was received).");
            assertResult(adminSeesGrant, "Expected '" + conTwo.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the change in affiliation by including an <x/> element qualified by the 'http://jabber.org/protocol/muc#user' namespace and containing an <item/> child with the 'affiliation' attribute set to a value of \"member\" for '" + targetAddress1 + "' after its membership was granted by '" + conTwo.getUser() + "' in '" + mucAddress + "' which is configured to be an open room (but no such stanza was received).");
            assertResult(target1SeesGrant, "Expected '" + conThree.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the change in affiliation by including an <x/> element qualified by the 'http://jabber.org/protocol/muc#user' namespace and containing an <item/> child with the 'affiliation' attribute set to a value of \"member\" for '" + targetAddress1 + "' after its membership was granted by '" + conTwo.getUser() + "' in '" + mucAddress + "' which is configured to be an open room (but no such stanza was received).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

}
