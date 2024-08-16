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
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.form.FillableForm;
import org.jivesoftware.smackx.xdata.form.Form;
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
 * Tests for section "9.2 Admin Use Cases: Modifying the Ban List" of XEP-0045: "Multi-User Chat"
 *
 * @see <a href="https://xmpp.org/extensions/xep-0045.html#modifyban">XEP-0045 Section 9.2</a>
 */
@SpecificationReference(document = "XEP-0045", version = "1.34.6")
public class MultiUserChatAdminBanListIntegrationTest extends AbstractMultiUserChatIntegrationTest
{
    public MultiUserChatAdminBanListIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
        InterruptedException, TestNotPossibleException, MultiUserChatException.MucAlreadyJoinedException, MultiUserChatException.MissingMucCreationAcknowledgeException, MultiUserChatException.NotAMucServiceException, XmppStringprepException
    {
        super(environment);
    }

    /**
     * Asserts that a ban list can be obtained.
     */
    @SmackIntegrationTest(section = "9.2", quote = "The admin first requests the ban list by querying the room for all users with an affiliation of 'outcast'.")
    public void mucTestAdminRequestsBanList() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-requests-banlist");
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
            iq.addItem(new MUCItem(MUCAffiliation.outcast));
            conTwo.sendIqRequestAndWaitForResponse(iq);

            // Verify result.
        } catch (XMPPException.XMPPErrorException e) {
            fail("Expected admin '" + conTwo.getUser() + "' to be able to receive the ban list from '" + mucAddress + "' (but the server returned an error).", e);
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }


    /**
     * Asserts that a ban list is always based on a bare JID.
     *
     * This test attempts to add a full JID to the ban list (which may/should not be possible, in which case the test
     * stops as 'not possible'), and then retrieves the ban list, asserting that the entry on it either doesn't exist,
     * or is a bare jid.
     */
    @SmackIntegrationTest(section = "9.2", quote = "The ban list is always based on a user's bare JID.")
    public void mucTestAdminBanListFullJid() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-banlist-fulljid");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final EntityFullJid targetAddress = JidCreate.entityFullFrom("test@example.org/foobar");

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByAdmin.join(nicknameAdmin);
            try {
                mucAsSeenByAdmin.banUser(targetAddress, "Attempt to create a ban list with a full JID (which shouldn't be possible).");
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Unable to create a ban list using a full JID.");
            }

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.get);
            iq.addItem(new MUCItem(MUCAffiliation.outcast));

            final MUCAdmin response;
            try {
                response = conTwo.sendIqRequestAndWaitForResponse(iq);
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Expected admin '" + conTwo.getUser() + "' to be able to receive the ban list from '" + mucAddress + "' (but the server returned an error).");
            }

            // Verify result.
            assertFalse(response.getItems().stream().anyMatch(i -> i.getJid().isEntityFullJid()), "The ban list for '" + mucAddress + "' unexpectedly contained an item with a full JID (where only bare JIDs are allowed).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that a ban list item has 'affiliation' and 'jid' attributes.
     */
    @SmackIntegrationTest(section = "9.2", quote = "each item MUST include the 'affiliation' and 'jid' attributes")
    public void mucTestAdminBanListItemCheck() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-banlist-attr");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress = JidCreate.entityBareFrom("test@example.org");

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.banUser(targetAddress, null);
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByAdmin.join(nicknameAdmin);

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.get);
            iq.addItem(new MUCItem(MUCAffiliation.outcast));

            final MUCAdmin response;
            try {
                response = conTwo.sendIqRequestAndWaitForResponse(iq);
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Expected admin '" + conTwo.getUser() + "' to be able to receive the ban list from '" + mucAddress + "' (but the server returned an error).");
            }

            // Verify result.
            assertFalse(response.getItems().stream().anyMatch(i -> i.getJid() == null), "The ban list for '" + mucAddress + "' contains an item that does not have a 'jid' attribute (but all items must have one).");
            assertFalse(response.getItems().stream().anyMatch(i -> i.getAffiliation() == null), "The ban list for '" + mucAddress + "' contains an item that does not have an 'affiliation' attribute (but all items must have one).");
            assertTrue(response.getItems().stream().anyMatch(i -> i.getJid().equals(targetAddress)), "Expected the ban list requested by '" + conTwo.getUser() + "' from '" + mucAddress + "' to include the recently added outcast '" + targetAddress + "' (but it did not).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that a ban list modification can contain more than one item.
     */
    @SmackIntegrationTest(section = "9.2", quote = "The admin can then modify the ban list if desired. In order to do so, the admin MUST send the changed items [...] back to the service; After updating the ban list, the service MUST inform the admin of success.")
    public void mucTestAdminBanListMultipleItems() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-banlist-multiple");
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
            iq.addItem(new MUCItem(MUCAffiliation.outcast, targetAddress1));
            iq.addItem(new MUCItem(MUCAffiliation.outcast, targetAddress2));

            try {
                conTwo.sendIqRequestAndWaitForResponse(iq);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected the service to inform '" + conTwo.getUser() + "' of success after they modified the ban list of room '" + mucAddress + "' (but instead, an error was returned).", e);
            }
            assertTrue(mucAsSeenByAdmin.getOutcasts().stream().anyMatch(i -> i.getAffiliation() == MUCAffiliation.outcast && i.getJid().equals(targetAddress1)), "Expected the ban list for '" + mucAddress + "' to contain '" + targetAddress1 + "' that was just added to the ban list by '" + conTwo.getUser() + "' (but does not appear on the ban list).");
            assertTrue(mucAsSeenByAdmin.getOutcasts().stream().anyMatch(i -> i.getAffiliation() == MUCAffiliation.outcast && i.getJid().equals(targetAddress2)), "Expected the ban list for '" + mucAddress + "' to contain '" + targetAddress2 + "' that was just added to the ban list by '" + conTwo.getUser() + "' (but does not appear on the ban list).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a non-admin cannot modify the ban list.
     */
    @SmackIntegrationTest(section = "9", quote = "modify the list of users who are banned from the room [...] MUST be denied if the <user@host> of the 'from' address of the request does not match the bare JID of one of the room admins; in this case, the service MUST return a <forbidden/> error.")
    public void testParticipantNotAllowedToModifyBanList() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-banlist-notallowed");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress1 = JidCreate.entityBareFrom("banned-user-" + randomString + "@example.org");
        final EntityBareJid targetAddress2 = JidCreate.entityBareFrom("banned-user-" + randomString + "@example.net");

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("admin-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByParticipant.join(nicknameParticipant);

            // Execute system under test & Verify result.
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
                mucAsSeenByParticipant.banUsers(List.of(targetAddress1, targetAddress2));
            }, "Expected an error after '" + conTwo.getUser() + "' (that is not an admin) tried to modify the ban list of room '" + mucAddress + "' (but none occurred).");
            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' after it tried to modify the ban list of room '" + mucAddress + "' while not being an admin.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that a ban list modification can be used to remove people from the banlist.
     */
    @SmackIntegrationTest(section = "9.2", quote = "The admin can then modify the ban list if desired. [...] each item MUST include the 'affiliation' attribute (normally set to a value of \"outcast\" to ban or \"none\" to remove ban)")
    public void mucTestAdminBanListMultipleItemsUnban() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-banlist-multiple");
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

            mucAsSeenByAdmin.banUsers(List.of(targetAddress1, targetAddress2));

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
                fail("Expected the service to inform '" + conTwo.getUser() + "' of success after they modified the ban list of room '" + mucAddress + "' (but instead, an error was returned).", e);
            }
            assertTrue(mucAsSeenByAdmin.getOutcasts().stream().noneMatch(i -> i.getAffiliation() == MUCAffiliation.outcast && i.getJid().equals(targetAddress1)), "Expected the ban list for '" + mucAddress + "' to no longer contain '" + targetAddress1 + "' that was just removed from the ban list by '" + conTwo.getUser() + "' (but does appear on the ban list).");
            assertTrue(mucAsSeenByAdmin.getOutcasts().stream().noneMatch(i -> i.getAffiliation() == MUCAffiliation.outcast && i.getJid().equals(targetAddress2)), "Expected the ban list for '" + mucAddress + "' to no longer contain '" + targetAddress2 + "' that was just removed from the ban list by '" + conTwo.getUser() + "' (but does appear on the ban list).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that a ban list modification can contain more than one item, that have optional attributes set.
     */
    @SmackIntegrationTest(section = "9.2", quote = "The admin can then modify the ban list if desired. In order to do so, the admin MUST send the changed items [...] back to the service; After updating the ban list, the service MUST inform the admin of success.")
    public void mucTestAdminBanListMultipleItemsWithOptionalAttributes() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-banlist-multiple-opt");
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
            iq.addItem(new MUCItem(MUCAffiliation.outcast, null, conTwo.getUser().asBareJid(), "Testing optional reason A for banning", targetAddress1, null, nicknameAdmin));
            iq.addItem(new MUCItem(MUCAffiliation.outcast, null, conTwo.getUser().asBareJid(), "Testing optional reason B for banning", targetAddress2, null, nicknameAdmin));

            try {
                conTwo.sendIqRequestAndWaitForResponse(iq);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected the service to inform '" + conTwo.getUser() + "' of success after they modified the ban list of room '" + mucAddress + "' (but instead, an error was returned).", e);
            }
            assertTrue(mucAsSeenByAdmin.getOutcasts().stream().anyMatch(i -> i.getAffiliation() == MUCAffiliation.outcast && i.getJid().equals(targetAddress1)), "Expected the ban list for '" + mucAddress + "' to contain '" + targetAddress1 + "' that was just added to the ban list by '" + conTwo.getUser() + "' (but does not appear on the ban list).");
            assertTrue(mucAsSeenByAdmin.getOutcasts().stream().anyMatch(i -> i.getAffiliation() == MUCAffiliation.outcast && i.getJid().equals(targetAddress2)), "Expected the ban list for '" + mucAddress + "' to contain '" + targetAddress2 + "' that was just added to the ban list by '" + conTwo.getUser() + "' (but does not appear on the ban list).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that a ban list modification is a delta: it shouldn't affect entries already on the ban list that are
     * not included in the delta.
     */
    @SmackIntegrationTest(section = "9.2", quote = "The admin can then modify the ban list if desired. In order to do so, the admin MUST send the changed items (i.e., only the \"delta\") [...]")
    public void mucTestAdminBanListIsDelta() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-banlist-delta");
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
            mucAsSeenByAdmin.banUsers(List.of(targetAddress1, targetAddress2));

            // Execute system under test.
            final MUCAdmin iq = new MUCAdmin();
            iq.setTo(mucAddress);
            iq.setType(IQ.Type.set);
            iq.addItem(new MUCItem(MUCAffiliation.none, targetAddress1));
            iq.addItem(new MUCItem(MUCAffiliation.outcast, targetAddress3));

            try {
                conTwo.sendIqRequestAndWaitForResponse(iq);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected the service to inform '" + conTwo.getUser() + "' of success after they modified the ban list of room '" + mucAddress + "' (but instead, an error was returned).", e);
            }

            final Set<Jid> outcasts = mucAsSeenByAdmin.getOutcasts().stream().filter(i -> i.getAffiliation().equals(MUCAffiliation.outcast)).map(Affiliate::getJid).collect(Collectors.toSet());
            assertFalse(outcasts.contains(targetAddress1), "Expected '" + targetAddress1 + "' to no longer be on the ban list of '" + mucAddress + "', after '" + conTwo.getUser() + "' updated the ban list that previously contained them with their removal (but does still appear on the ban list).");
            assertTrue(outcasts.contains(targetAddress2), "Expected '" + targetAddress2 + "' to be on the ban list of '" + mucAddress + "', after the ban list that previously contained them got updated by '" + conTwo.getUser() + "' with different items (which should have been applied as a delta).");
            assertTrue(outcasts.contains(targetAddress3), "Expected '" + targetAddress3 + "' to be on the ban list of '" + mucAddress + "', after the ban list that previously did not contain them got updated by '" + conTwo.getUser() + "' with items that include them.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that occupants that are banned by modification of the banlist are removed from the room.
     */
    @SmackIntegrationTest(section = "9.2", quote = "After updating the ban list [...] The service MUST then remove the affected occupants (if they are in the room)")
    public void mucTestBanListOccupantsInformedOfBan() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-banlist-notification");
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

            final SimpleResultSyncPoint targetSeesBan = new SimpleResultSyncPoint();
            mucAsSeenByTarget.addUserStatusListener(new UserStatusListener() {
                @Override
                public void banned(Jid actor, String reason) { // Invoked only when presence with 301 is received.
                    targetSeesBan.signal();
                }
            });

            // Execute system under test.
            mucAsSeenByAdmin.banUsers(List.of(targetAddress1, targetAddress2));

            // Verify result.
            assertResult(targetSeesBan, "Expected '" + conThree.getUser() + "' to receive a presence stanza of type \"unavailable\" including status code 301 in the extended presence information after being banned by '" + conTwo.getUser() + "' from '" + mucAddress + "' (but no such stanza was received).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that other occupants are notified when banlist changes are made.
     */
    @SmackIntegrationTest(section = "9.2", quote = "After updating the ban list [...] The service MUST then remove the affected occupants [...] and send updated presence (including the appropriate status code) from them to all the remaining occupants")
    public void mucTestBanListRemainingOccupantsInformedOfBan() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-banlist-broadcast");
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

            final SimpleResultSyncPoint ownerSeesBan = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint adminSeesBan = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void banned(EntityFullJid participant, Jid actor, String reason) { // Invoked when presence with 301 is received.
                    if (targetMucAddress.equals(participant)) {
                        ownerSeesBan.signal();
                    }
                }
            });
            mucAsSeenByAdmin.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void banned(EntityFullJid participant, Jid actor, String reason) { // Invoked when presence with 301 is received.
                    if (targetMucAddress.equals(participant)) {
                        adminSeesBan.signal();
                    }
                }
            });

            // Execute system under test.
            mucAsSeenByAdmin.banUsers(List.of(targetAddress1, targetAddress2));

            // Verify result.
            assertResult(ownerSeesBan, "Expected '" + conOne.getUser() + "' to receive a presence stanza of type \"unavailable\" of '" + targetMucAddress + "' including status code 301 in the extended presence information after '" + targetAddress1 + "' is banned by '" + conTwo.getUser() + "' from '" + mucAddress + "' (but no such stanza was received).");
            assertResult(adminSeesBan, "Expected '" + conTwo.getUser() + "' to receive a presence stanza of type \"unavailable\" of '" + targetMucAddress + "' including status code 301 in the extended presence information after '" + targetAddress1 + "' is banned by '" + conTwo.getUser() + "' from '" + mucAddress + "' (but no such stanza was received).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that entities banned from a room through ban list modifications get their nickname removed from the list
     * of registered nicknames.
     */
    @SmackIntegrationTest(section = "9.2", quote = "After updating the ban list [...] The service MUST also remove each banned user's reserved nickname from the list of reserved roomnicks, if appropriate.")
    public void mucTestBanListRemovesRegisteredNickname() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-admin-banlist-removes-registered-nickname");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget1 = mucManagerThree.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress1 = conThree.getUser().asEntityBareJid();
        final EntityBareJid targetAddress2 = JidCreate.entityBareFrom("test2@example.com");

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);
        final Resourcepart nicknameTarget1 = Resourcepart.from("target-" + randomString);
        final Resourcepart nicknameTarget2 = Resourcepart.from("target-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());

            try {
                final Form registrationForm = mucAsSeenByTarget1.getRegistrationForm();
                final Set<String> requiredFieldNames = registrationForm.getDataForm().getFields()
                    .stream().filter(FormField::isRequired).map(FormField::getFieldName).collect(Collectors.toSet());
                final FillableForm submitForm = registrationForm.getFillableForm();
                for (final String requiredFieldName : requiredFieldNames) {
                    submitForm.setAnswer(requiredFieldName, "test");
                }

                if (submitForm.hasField("username")) {
                    submitForm.setAnswer("username", nicknameTarget1);
                } else if (submitForm.hasField("muc#register_roomnick")) {
                    submitForm.setAnswer("muc#register_roomnick", nicknameTarget1);
                } else {
                    throw new TestNotPossibleException("Unable to register with the room (cannot identify the appropriate field for registering a nickname).");
                }
                mucAsSeenByTarget1.sendRegistrationForm(submitForm);

                // FIXME: Find a way to get 'target2' registered as a member.
            } catch (Throwable e) {
                throw new TestNotPossibleException("Unable to register with the room.");
            }

            mucAsSeenByAdmin.join(nicknameAdmin);

            // Execute system under test.
            mucAsSeenByAdmin.banUsers(List.of(targetAddress1, targetAddress2));

            // Verify result.
            assertTrue(mucAsSeenByAdmin.getMembers().stream().noneMatch(affiliate -> nicknameTarget1.equals(affiliate.getNick())), "Expected the registered nickname ('" + nicknameTarget1 + "') of '" + targetAddress1 + "' to no longer be on the list of registered nicknames after the were banned by '" + conTwo + "' from '" + mucAddress + "' (but the nickname does still appear on the list).");
            assertTrue(mucAsSeenByAdmin.getMembers().stream().noneMatch(affiliate -> nicknameTarget2.equals(affiliate.getNick())), "Expected the registered nickname ('" + nicknameTarget2 + "') of '" + targetAddress2 + "' to no longer be on the list of registered nicknames after the were banned by '" + conTwo + "' from '" + mucAddress + "' (but the nickname does still appear on the list).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    // TODO Determine if tests are needed that verify behavior when through banlist modifications, an admin tries to ban an owner, or when an admin or owner bans themselves.
    //      This seems to be undesirable, but isn't specifically mentioned in section 9.2. This also raises the question on what to do when a banlist modification consists
    //      of 'valid' and 'invalid' modifications. Should those all be rejected, or should only the valid ones be applied?
    //      Question asked on https://mail.jabber.org/hyperkitty/list/standards@xmpp.org/thread/UJSUVFKHN6LXR33ZIY34SYFQHNRVEB7R/
}
