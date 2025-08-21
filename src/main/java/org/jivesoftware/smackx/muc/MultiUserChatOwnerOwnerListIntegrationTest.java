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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for section "10.5 Owner Use Cases: Modifying the Owner List" of XEP-0045: "Multi-User Chat"
 *
 * @see <a href="https://xmpp.org/extensions/xep-0045.html#modifyowner">XEP-0045 Section 10.5</a>
 */
@SpecificationReference(document = "XEP-0045", version = "1.35.1")
public class MultiUserChatOwnerOwnerListIntegrationTest extends AbstractMultiUserChatIntegrationTest
{
    public MultiUserChatOwnerOwnerListIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws SmackException.NoResponseException, XMPPException.XMPPErrorException,
        SmackException.NotConnectedException, InterruptedException, TestNotPossibleException, XmppStringprepException, MultiUserChatException.MucAlreadyJoinedException, MultiUserChatException.MissingMucCreationAcknowledgeException, MultiUserChatException.NotAMucServiceException
    {
        super(environment);

        // The specification reads "If allowed by an implementation, a room owner might want to modify the owner list"
        // which suggests that this is optional functionality. The specification does not explicitly say how to test for
        // support. This implementation will use any XMPP error in response to a change request as an indication that
        // the feature is not supported by the server under test.
        final EntityBareJid mucAddress = getRandomRoom("owner-owner-list-support");
        final MultiUserChat muc = mucManagerOne.getMultiUserChat(mucAddress);
        createMuc(muc, Resourcepart.from("owner-" + randomString));
        try {
            muc.getOwners();
            muc.grantOwnership(Set.of(conOne.getUser().asBareJid(), conTwo.getUser().asBareJid()));
        } catch (XMPPException.XMPPErrorException e) {
            throw new TestNotPossibleException("Service does not support modification of the owner list.");
        } finally {
            tryDestroy(muc);
        }
    }

    /**
     * Asserts that an owner list can be obtained.
     */
    @SmackIntegrationTest(section = "10.5", quote = "the owner [...] requests the owner list by querying the room for all users with an affiliation of 'owner'")
    public void testOwnerRequestsOwnerList() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("owner-requests-ownerlist");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.get);
            iq.addItem(new MUCItem(MUCAffiliation.owner));
            conOne.sendIqRequestAndWaitForResponse(iq);

            // Verify result.
        } catch (XMPPException.XMPPErrorException e) {
            fail("Expected owner '" + conOne.getUser() + "' to be able to receive the owner list from '" + mucAddress + "' (but the server returned an error).", e);
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that a user (not in the room, that is semi-anonymous) cannot request the owner list.
     *
     * This test uses a semi-anonymous room, as a XEP update is in the works that allows these requests for non-anonymous rooms.
     */
    @SmackIntegrationTest(section = "10.5", quote = "If the <user@host> of the 'from' address does not match the bare JID of a room owner, the service MUST return a <forbidden/> error to the sender in semi-anonymous rooms.")
    public void testUserRequestsOwnerList() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("owner-user-requests-ownerlist");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);

        createSemiAnonymousMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.get);
            iq.addItem(new MUCItem(MUCAffiliation.owner));

            // Verify result.
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> conTwo.sendIqRequestAndWaitForResponse(iq),
                "Expected user '" + conTwo.getUser() + "' (that is not in the room) to receive an error when requesting the owner list from '" + mucAddress + "' (but the server did not return an error).");
            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' (that is not in the room) after it requested the owner list of room '" + mucAddress + "'.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that a participant (that is in a semi-anonymous room) cannot request the owner list.
     *
     * This test uses a semi-anonymous room, as a XEP update is in the works that allows these requests for non-anonymous rooms.
     */
    @SmackIntegrationTest(section = "10.5", quote = "If the <user@host> of the 'from' address does not match the bare JID of a room owner, the service MUST return a <forbidden/> error to the sender in semi-anonymous rooms.")
    public void testParticipantRequestsOwnerList() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("owner-user-requests-ownerlist");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);

        createSemiAnonymousMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByParticipant.join(nicknameParticipant);

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.get);
            iq.addItem(new MUCItem(MUCAffiliation.owner));

            // Verify result.
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> conTwo.sendIqRequestAndWaitForResponse(iq),
                "Expected participant '" + conTwo.getUser() + "' to receive an error when requesting the owner list from '" + mucAddress + "' (but the server did not return an error).");
            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' (that is a participant in the room) after it requested the owner list of room '" + mucAddress + "'.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that an owner list item has 'affiliation' and 'jid' attributes.
     */
    @SmackIntegrationTest(section = "10.5", quote = "each item MUST include the 'affiliation' and 'jid' attributes")
    public void testOwnerListItemCheck() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("owner-owner-list-itemcheck");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantOwnership(conTwo.getUser().asBareJid());
            mucAsSeenByTarget.join(nicknameTarget);

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.get);
            iq.addItem(new MUCItem(MUCAffiliation.owner));
            final MUCAdmin response = conOne.sendIqRequestAndWaitForResponse(iq);

            // Verify result.
            assertFalse(response.getItems().stream().anyMatch(i -> i.getAffiliation() == null), "The owner list for '" + mucAddress + "' contains an item that does not have an 'affiliation' attribute (but all items must have one).");
            assertFalse(response.getItems().stream().anyMatch(i -> i.getJid() == null), "The owner list for '" + mucAddress + "' contains an item that does not have a 'jid' attribute (but all items must have one).");
            assertTrue(response.getItems().stream().anyMatch(i -> i.getJid().equals(conTwo.getUser().asBareJid())), "Expected the owner list requested by '" + conOne.getUser() + "' from '" + mucAddress + "' to include '" + conTwo.getUser().asBareJid() + "' (but it did not).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that an owner list modification can contain more than one item.
     */
    @SmackIntegrationTest(section = "10.5", quote = "The owner can then modify the owner list if desired. In order to do so, the owner MUST send the changed items [...] back to the service [...] the service MUST modify owner list and then inform the owner of success")
    public void testOwnerListMultipleItems() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("owner-ownerlist-multiple");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.set);
            iq.addItem(new MUCItem(MUCAffiliation.owner, conTwo.getUser().asBareJid()));
            iq.addItem(new MUCItem(MUCAffiliation.owner, conThree.getUser().asBareJid()));

            try {
                conOne.sendIqRequestAndWaitForResponse(iq);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected the service to inform '" + conOne.getUser() + "' of success after they modified the owner list of room '" + mucAddress + "' (but instead, an error was returned).", e);
            }
            assertTrue(mucAsSeenByOwner.getOwners().stream().anyMatch(i -> i.getAffiliation() == MUCAffiliation.owner && i.getJid().equals(conTwo.getUser().asBareJid())), "Expected the owner list for '" + mucAddress + "' to contain '" + conTwo.getUser().asBareJid() + "' that was just added to the owner list by '" + conOne.getUser() + "' (but does not appear on the owner list).");
            assertTrue(mucAsSeenByOwner.getOwners().stream().anyMatch(i -> i.getAffiliation() == MUCAffiliation.owner && i.getJid().equals(conThree.getUser().asBareJid())), "Expected the owner list for '" + mucAddress + "' to contain '" + conThree.getUser().asBareJid() + "' that was just added to the owner list by '" + conOne.getUser() + "' (but does not appear on the owner list).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that an owner list modification can be used to remove people from the owner list.
     */
    @SmackIntegrationTest(section = "10.5", quote = "The owner can then modify the owner list if desired. In order to do so, the owner MUST send the changed items [...] back to the service [...] the service MUST modify owner list and then inform the owner of success")
    public void testAdminOwnerListMultipleItemsRevoke() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("owner-ownerlist-revoke");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            try {
                mucAsSeenByOwner.grantOwnership(List.of(conTwo.getUser().asBareJid(), conThree.getUser().asBareJid()));
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Unable to grant '" + conTwo.getUser().asBareJid() + "' and/or '" + conThree.getUser().asBareJid() + "' owner status in room '" + mucAddress + "'.");
            }

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.set);
            iq.addItem(new MUCItem(MUCAffiliation.none, conTwo.getUser().asBareJid()));
            iq.addItem(new MUCItem(MUCAffiliation.none, conThree.getUser().asBareJid()));

            try {
                conOne.sendIqRequestAndWaitForResponse(iq);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected the service to inform '" + conOne.getUser() + "' of success after they modified the owner list of room '" + mucAddress + "' (but instead, an error was returned).", e);
            }
            assertTrue(mucAsSeenByOwner.getOwners().stream().noneMatch(i -> i.getAffiliation() == MUCAffiliation.owner && i.getJid().equals(conTwo.getUser().asBareJid())), "Expected the owner list for '" + mucAddress + "' to not contain '" + conTwo.getUser().asBareJid() + "' that was just removed from the owner list by '" + conOne.getUser() + "' (but does appear on the owner list).");
            assertTrue(mucAsSeenByOwner.getOwners().stream().noneMatch(i -> i.getAffiliation() == MUCAffiliation.owner && i.getJid().equals(conThree.getUser().asBareJid())), "Expected the owner list for '" + mucAddress + "' to not contain '" + conThree.getUser().asBareJid() + "' that was just removed from the owner list by '" + conOne.getUser() + "' (but does appear on the owner list).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that an owner list modification is a delta: it shouldn't affect entries already on the owner list
     * that are not included in the delta.
     */
    @SmackIntegrationTest(section = "10.5", quote = "The owner can then modify the owner list if desired. In order to do so, the owner MUST send the changed items (i.e., only the \"delta\")")
    public void testOwnerOwnerListIsDelta() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("owner-ownerlist-delta");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            try {
                mucAsSeenByOwner.grantOwnership(conTwo.getUser().asBareJid());
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Unable to grant '" + conTwo.getUser().asBareJid() + "' owner status in room '" + mucAddress + "'.");
            }

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.set);
            iq.addItem(new MUCItem(MUCAffiliation.none, conTwo.getUser().asBareJid()));
            iq.addItem(new MUCItem(MUCAffiliation.owner, conThree.getUser().asBareJid()));


            try {
                conOne.sendIqRequestAndWaitForResponse(iq);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected the service to inform '" + conOne.getUser() + "' of success after they modified the owner list of room '" + mucAddress + "' (but instead, an error was returned).", e);
            }
            assertTrue(mucAsSeenByOwner.getOwners().stream().anyMatch(i -> i.getAffiliation() == MUCAffiliation.owner && i.getJid().equals(conOne.getUser().asBareJid())), "Expected the owner list for '" + mucAddress + "' to contain '" + conOne.getUser().asBareJid() + "' after the owner list that previously contained them got updated with different items (which should have been applied as a delta).");
            assertTrue(mucAsSeenByOwner.getOwners().stream().noneMatch(i -> i.getAffiliation() == MUCAffiliation.owner && i.getJid().equals(conTwo.getUser().asBareJid())), "Expected the owner list for '" + mucAddress + "' to no longer contain '" + conTwo.getUser().asBareJid() + "' that was just removed from the owner list by '" + conOne.getUser() + "' (but does still appear on the owner list).");
            assertTrue(mucAsSeenByOwner.getOwners().stream().anyMatch(i -> i.getAffiliation() == MUCAffiliation.owner && i.getJid().equals(conThree.getUser().asBareJid())), "Expected the owner list for '" + mucAddress + "' to contain '" + conThree.getUser().asBareJid() + "' that was just added to the owner list by '" + conOne.getUser() + "' (but does not appear on the owner list).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that an admin (non-owner) cannot make an owner list modification.
     */
    @SmackIntegrationTest(section = "10.5", quote = "Only owners shall be allowed to modify the owner list. If a non-owner attempts to modify the owner list, the service MUST deny the request and return a <forbidden/> error to the sender")
    public void testOwnerListRejectAdmin() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("owner-ownerlist-reject-admin");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            try {
                mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Unable to grant '" + conTwo.getUser().asBareJid() + "' admin status in room '" + mucAddress + "'.");
            }
            mucAsSeenByAdmin.join(nicknameAdmin); // Not strictly needed.

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.set);
            iq.addItem(new MUCItem(MUCAffiliation.owner, conThree.getUser().asBareJid()));

            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> conTwo.sendIqRequestAndWaitForResponse(iq),
                "Expected user '" + conTwo.getUser() + "' (that is not an owner but an admin) to receive an error when they attempted to modify the owner list from '" + mucAddress + "' (but the server did not return an error).");
            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' (that is not an owner but an admin) after it attempted to modify the owner list of room '" + mucAddress + "'.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that a member (non-owner) cannot make an owner list modification.
     */
    @SmackIntegrationTest(section = "10.5", quote = "Only owners shall be allowed to modify the owner list. If a non-owner attempts to modify the owner list, the service MUST deny the request and return a <forbidden/> error to the sender")
    public void testOwnerListRejectMember() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("owner-ownerlist-reject-member");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByMember = mucManagerTwo.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameMember = Resourcepart.from("member-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            try {
                mucAsSeenByOwner.grantMembership(conTwo.getUser().asBareJid());
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Unable to grant '" + conTwo.getUser().asBareJid() + "' membership in room '" + mucAddress + "'.");
            }
            mucAsSeenByMember.join(nicknameMember); // Not strictly needed.

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.set);
            iq.addItem(new MUCItem(MUCAffiliation.owner, conThree.getUser().asBareJid()));

            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> conTwo.sendIqRequestAndWaitForResponse(iq),
                "Expected user '" + conTwo.getUser() + "' (that is not an owner but a member) to receive an error when they attempt to modify the owner list from '" + mucAddress + "' (but the server did not return an error).");
            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' (that is not an owner but a member) after it attempted to modify the owner list of room '" + mucAddress + "'.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that an outcast (non-owner) cannot make an owner list modification.
     */
    @SmackIntegrationTest(section = "10.5", quote = "Only owners shall be allowed to modify the owner list. If a non-owner attempts to modify the owner list, the service MUST deny the request and return a <forbidden/> error to the sender")
    public void testOwnerListRejectOutcast() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("owner-ownerlist-reject-member");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            try {
                mucAsSeenByOwner.banUser(conTwo.getUser().asBareJid(), "Made outcast as part of an integration test.");
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Unable to make '" + conTwo.getUser().asBareJid() + "' an outcast in room '" + mucAddress + "'.");
            }

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.set);
            iq.addItem(new MUCItem(MUCAffiliation.owner, conThree.getUser().asBareJid()));

            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> conTwo.sendIqRequestAndWaitForResponse(iq),
                "Expected user '" + conTwo.getUser() + "' (that is not an owner but an outcast) to receive an error when they attempt to modify the owner list from '" + mucAddress + "' (but the server did not return an error).");
            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' (that is not an owner but an outcast) after it attempted to modify the owner list of room '" + mucAddress + "'.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that a user without an affiliation (non-owner) cannot make an owner list modification.
     */
    @SmackIntegrationTest(section = "10.5", quote = "Only owners shall be allowed to modify the owner list. If a non-owner attempts to modify the owner list, the service MUST deny the request and return a <forbidden/> error to the sender")
    public void testOwnerListRejectNoneAffiliation() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("owner-ownerlist-reject-nonaff");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByParticipant.join(nicknameParticipant); // Not strictly needed.

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.set);
            iq.addItem(new MUCItem(MUCAffiliation.owner, conThree.getUser().asBareJid()));

            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> conTwo.sendIqRequestAndWaitForResponse(iq),
                "Expected user '" + conTwo.getUser() + "' (that is not an owner but a user without an affiliation) to receive an error when they attempt to modify the owner list from '" + mucAddress + "' (but the server did not return an error).");
            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' (that is not an owner but a user without an affiliation) after it attempted to modify the owner list of room '" + mucAddress + "'.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that the last owner cannot remove itself as an owner through a owner list modification.
     */
    @SmackIntegrationTest(section = "10.5", quote = "A service MUST NOT allow an owner to revoke his or her own owner status if there are no other owners; if an owner attempts to do this, the service MUST return a <conflict/> error to the owner.")
    public void testOwnerListRejectRemovalLastOwner() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("owner-ownerlist-remove-last");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.set);
            iq.addItem(new MUCItem(MUCAffiliation.none, conOne.getUser().asBareJid()));

            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> conOne.sendIqRequestAndWaitForResponse(iq),
                "Expected user '" + conOne.getUser() + "' (that is the only owner of the room) to receive an error when they attempt to revoke their own owner status through modification of the owner list from '" + mucAddress + "' (but the server did not return an error).");
            assertEquals(StanzaError.Condition.conflict, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conOne.getUser() + "' (that is the only owner of the room) after it attempted to revoke their own owner status through modification of the owner list room '" + mucAddress + "'.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that occupants are notified when owner list changes are made.
     */
    @SmackIntegrationTest(section = "10.5", quote = "The service MUST also send presence notifications related to any affiliation changes that result from modifying the owner list [...]")
    public void testAdminOwnerListBroadcast() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("owner-ownerlist-broadcast");
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
                mucAsSeenByOwner.grantOwnership(conTwo.getUser().asBareJid());
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Unable to grant '" + conTwo.getUser().asBareJid() + "' owner status in room '" + mucAddress + "'.");
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

            final SimpleResultSyncPoint ownerSeesRevokeTarget1 = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint ownerSeesGrantTarget2 = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint target1SeesRevokeTarget1 = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint target1SeesGrantTarget2 = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint target2SeesRevokeTarget1 = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint target2SeesGrantTarget2 = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void ownershipGranted(EntityFullJid participant) {
                    if (participant.equals(target2MucAddress)) {
                        ownerSeesGrantTarget2.signal();
                    }
                }

                @Override
                public void ownershipRevoked(EntityFullJid participant) {
                    if (participant.equals(target1MucAddress)) {
                        ownerSeesRevokeTarget1.signal();
                    }
                }
            });
            mucAsSeenByTarget1.addUserStatusListener(new UserStatusListener() {
                @Override
                public void ownershipRevoked() {
                    target1SeesRevokeTarget1.signal();
                }
            });
            mucAsSeenByTarget1.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void ownershipGranted(EntityFullJid participant) {
                    if (participant.equals(target2MucAddress)) {
                        target1SeesGrantTarget2.signal();
                    }
                }
            });
            mucAsSeenByTarget2.addUserStatusListener(new UserStatusListener() {
                @Override
                public void ownershipGranted() {
                    target2SeesGrantTarget2.signal();
                }
            });
            mucAsSeenByTarget2.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void ownershipRevoked(EntityFullJid participant) {
                    if (participant.equals(target1MucAddress)) {
                        target2SeesRevokeTarget1.signal();
                    }
                }
            });

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.set);
            iq.addItem(new MUCItem(MUCAffiliation.none, conTwo.getUser().asBareJid()));
            iq.addItem(new MUCItem(MUCAffiliation.owner, conThree.getUser().asBareJid()));

            try {
                conOne.sendIqRequestAndWaitForResponse(iq);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected the service to inform '" + conOne.getUser() + "' of success after they modified the owner list of room '" + mucAddress + "' (but instead, an error was returned).", e);
            }

            assertResult(ownerSeesRevokeTarget1, "Expected '" + conOne.getUser() + "' to receive a presence stanza from '" + mucAddress + "' indicating the change in owner status of '" + conTwo.getUser().asBareJid() + "' (but did not).");
            assertResult(ownerSeesGrantTarget2, "Expected '" + conOne.getUser() + "' to receive a presence stanza from '" + mucAddress + "' indicating the change in owner status of '" + conThree.getUser().asBareJid() + "' (but did not).");
            assertResult(target1SeesRevokeTarget1, "Expected '" + conTwo.getUser() + "' to receive a presence stanza from '" + mucAddress + "' indicating the change in owner status for themself (but did not).");
            assertResult(target1SeesGrantTarget2, "Expected '" + conTwo.getUser() + "' to receive a presence stanza from '" + mucAddress + "' indicating the change in owner status of '" + conThree.getUser().asBareJid() + "' (but did not).");
            assertResult(target2SeesRevokeTarget1, "Expected '" + conThree.getUser() + "' to receive a presence stanza from '" + mucAddress + "' indicating the change in owner status of '" + conTwo.getUser().asBareJid() + "' (but did not).");
            assertResult(target2SeesGrantTarget2, "Expected '" + conThree.getUser() + "' to receive a presence stanza from '" + mucAddress + "' indicating the change in owner status for themself' (but did not).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }
}
