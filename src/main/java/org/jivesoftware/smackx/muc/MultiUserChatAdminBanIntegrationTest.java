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
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.form.FillableForm;
import org.jivesoftware.smackx.xdata.form.Form;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for section "9.1 Admin Use Cases: Banning a User" of XEP-0045: "Multi-User Chat"
 *
 * @see <a href="https://xmpp.org/extensions/xep-0045.html#ban">XEP-0045 Section 9.1</a>
 */
@SpecificationReference(document = "XEP-0045", version = "1.35.1")
public class MultiUserChatAdminBanIntegrationTest extends AbstractMultiUserChatIntegrationTest
{
    public MultiUserChatAdminBanIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
        InterruptedException, TestNotPossibleException, MultiUserChatException.MucAlreadyJoinedException, MultiUserChatException.MissingMucCreationAcknowledgeException, MultiUserChatException.NotAMucServiceException, XmppStringprepException
    {
        super(environment);
    }

    /**
     * Verifies that a bare JID (not related to a current occupant) can be banned by an admin without providing the
     * optional 'reason' attribute of a ban request.
     */
    @SmackIntegrationTest(section = "9.1", quote = "The <reason/> element is OPTIONAL.")
    public void testBanNonOccupantWithoutOptionalReason() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-ban-nonoccupant-without-reason");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress = JidCreate.entityBareFrom("banned-user-" + randomString + "@example.org");

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByAdmin.join(nicknameAdmin);

            try {
                // Execute system under test.
                mucAsSeenByAdmin.banUser(targetAddress, null);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected '" + conTwo.getUser() + "' (an admin) to be able to ban '" + targetAddress + "' (that was not an occupant) from '" + mucAddress + "' without providing the optional 'reason' attribute (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a bare JID (of an occupant) can be banned by an admin without providing the optional 'reason'
     * attribute of a ban request.
     */
    @SmackIntegrationTest(section = "9.1", quote = "The <reason/> element is OPTIONAL.")
    public void testBanOccupantWithoutOptionalReason() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-ban-occupant-without-reason");
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

            try {
                // Execute system under test.
                mucAsSeenByAdmin.banUser(targetAddress, null);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected '" + conTwo.getUser() + "' (an admin) to be able to ban '" + targetAddress + "' (an existing occupant) from '" + mucAddress + "' without providing the optional 'reason' attribute (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a bare JID (not related to a current occupant) can be banned by an admin when providing the
     * optional 'reason' attribute of a ban request.
     */
    @SmackIntegrationTest(section = "9.1", quote = "The <reason/> element is OPTIONAL.")
    public void testBanNonOccupantWithOptionalReason() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-ban-nonoccupant-with-reason");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress = JidCreate.entityBareFrom("banned-user-" + randomString + "@example.org");

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByAdmin.join(nicknameAdmin);

            try {
                // Execute system under test.
                mucAsSeenByAdmin.banUser(targetAddress, "Banned as part of a test.");

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected admin '" + conTwo.getUser() + "' to be able to ban '" + targetAddress + "' (that was not an occupant) from '" + mucAddress + "' while providing the optional 'reason' attribute (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a bare JID (of an occupant) can be banned by an admin when providing the optional 'reason' 
     * attribute of a ban request.
     */
    @SmackIntegrationTest(section = "9.1", quote = "The <reason/> element is OPTIONAL.")
    public void testBanOccupantWithOptionalReason() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-ban-occupant-with-reason");
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

            try {
                // Execute system under test.
                mucAsSeenByAdmin.banUser(targetAddress, "Banned as part of a test.");

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected '" + conTwo.getUser() + "' (an admin) to be able to ban '" + targetAddress + "' (an existing occupant) from '" + mucAddress + "' while providing the optional 'reason' attribute (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a non-admin cannot ban someone.
     */
    @SmackIntegrationTest(section = "9", quote = "ban a user from the room [...] MUST be denied if the <user@host> of the 'from' address of the request does not match the bare JID of one of the room admins; in this case, the service MUST return a <forbidden/> error.")
    public void testParticipantNotAllowedToBan() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-ban-notallowed");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress = JidCreate.entityBareFrom("banned-user-" + randomString + "@example.org");

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("admin-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByParticipant.join(nicknameParticipant);

            // Execute system under test & Verify result.
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
                mucAsSeenByParticipant.banUser(targetAddress, "Banned as part of a test.");
            }, "Expected an error after '" + conTwo.getUser() + "' (that is not an admin) tried to ban another participant ('" + targetAddress + "') from room '" + mucAddress + "' (but none occurred).");
            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' after it tried to ban another participant ('" + targetAddress + "') from room '" + mucAddress + "' while not being an admin.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a bare JID (not related to a current occupant) that is banned by an admin appears on the Ban List.
     */
    @SmackIntegrationTest(section = "9.1", quote = "The ban MUST be performed based on the occupant's bare JID. [...] The service MUST add that bare JID to the ban list [...]")
    public void testBanNonOccupantJidOnBanList() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-nonoccupant-banned-jid-on-banlist");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress = JidCreate.entityBareFrom("banned-user-" + randomString + "@example.org");

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByAdmin.join(nicknameAdmin);

            // Execute system under test.
            mucAsSeenByAdmin.banUser(targetAddress, "Banned as part of a test.");

            // Verify result.
            assertTrue(mucAsSeenByAdmin.getOutcasts().stream().anyMatch(affiliate -> affiliate.getJid().equals(targetAddress)), "Expected '" + targetAddress +"' (that was not an occupant) to be on the Ban List after the were banned by '" + conTwo.getUser() + "' (an admin) from '" + mucAddress + "' (but the JID does not appear on the Ban List).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a bare JID (of an occupant) that is banned by an admin appears on the Ban List.
     */
    @SmackIntegrationTest(section = "9.1", quote = "The ban MUST be performed based on the occupant's bare JID. [...] The service MUST add that bare JID to the ban list [...]")
    public void testBanOccupantJidOnBanList() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-occupant-banned-jid-on-banlist");
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

            // Execute system under test.
            mucAsSeenByAdmin.banUser(targetAddress, "Banned as part of a test.");

            // Verify result.
            assertTrue(mucAsSeenByAdmin.getOutcasts().stream().anyMatch(affiliate -> affiliate.getJid().equals(targetAddress)), "Expected '" + targetAddress +"' (an existing occupant) to be on the Ban List after the were banned by '" + conTwo.getUser() + "' (an admin) from '" + mucAddress + "' (but the JID does not appear on the Ban List).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a bare JID that is banned by an admin gets its nickname removed from the list of registered nicknames.
     */
    @SmackIntegrationTest(section = "9.1", quote = "The ban MUST be performed based on the occupant's bare JID. [...] The service [...] MUST remove the outcast's nickname from the list of registered nicknames [...]")
    public void testBanOccupantRemovesRegisteredNickname() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-ban-removes-registered-nickname");
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

            try {
                final Form registrationForm = mucAsSeenByTarget.getRegistrationForm();
                final Set<String> requiredFieldNames = registrationForm.getDataForm().getFields()
                    .stream().filter(FormField::isRequired).map(FormField::getFieldName).collect(Collectors.toSet());
                final FillableForm submitForm = registrationForm.getFillableForm();
                for (final String requiredFieldName : requiredFieldNames) {
                    submitForm.setAnswer(requiredFieldName, "test");
                }

                if (submitForm.hasField("username")) {
                    submitForm.setAnswer("username", nicknameTarget);
                } else if (submitForm.hasField("muc#register_roomnick")) {
                    submitForm.setAnswer("muc#register_roomnick", nicknameTarget);
                } else {
                    throw new TestNotPossibleException("Unable to register with the room (cannot identify the appropriate field for registering a nickname).");
                }
                mucAsSeenByTarget.sendRegistrationForm(submitForm);
            } catch (Throwable e) {
                throw new TestNotPossibleException("Unable to register with the room.");
            }

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

            // Execute system under test.
            mucAsSeenByAdmin.banUser(targetAddress, "Banned as part of a test.");

            // Verify result.
            assertTrue(mucAsSeenByAdmin.getMembers().stream().noneMatch(affiliate -> nicknameTarget.equals(affiliate.getNick())), "Expected the registered nickname ('" + nicknameTarget + "') of '" + targetAddress + "' to no longer be on the list of registered nicknames after the were banned by '" + conTwo.getUser() + "' (an admin) from '" + mucAddress + "' (but the nickname does still appear on the list).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that an occupant that is banned by an admin is removed from the room.
     */
    @SmackIntegrationTest(section = "9.1", quote = "The service MUST also remove any banned users who are in the room by sending a presence stanza of type \"unavailable\" to each banned occupant, including status code 301 in the extended presence information [...]")
    public void testBannedOccupantReceivesRemoval() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-ban-notification");
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

            final SimpleResultSyncPoint targetSeesBan = new SimpleResultSyncPoint();
            mucAsSeenByTarget.addUserStatusListener(new UserStatusListener() {
                @Override
                public void banned(Jid actor, String reason) { // Invoked only when presence with 301 is received.
                    targetSeesBan.signal();
                }
            });

            // Execute system under test.
            mucAsSeenByAdmin.banUser(targetAddress, "Banned as part of a test.");

            // Verify result.
            assertResult(targetSeesBan, "Expected '" + conThree.getUser() + "' to receive a presence stanza of type \"unavailable\" including status code 301 in the extended presence information after being banned by '" + conTwo.getUser() + "' (an admin) from '" + mucAddress + "' (but no such stanza was received).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that other occupants are notified when an occupant is banned by an admin.
     */
    @SmackIntegrationTest(section = "9.1", quote = "The service MUST then inform all of the remaining occupants that the banned user is no longer in the room by sending presence stanzas of type \"unavailable\" from the banned user to all remaining occupants [...]")
    public void mucTestRemainingOccupantsInformedOfBan() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-ban-broadcast");
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
            mucAsSeenByAdmin.banUser(targetAddress, "Banned as part of a test.");

            // Verify result.
            assertResult(ownerSeesBan, "Expected '" + conOne.getUser() + "' to receive a presence stanza of type \"unavailable\" of '" + targetMucAddress + "' including status code 301 in the extended presence information after '" + targetAddress + "' is banned by '" + conTwo.getUser() + "' (an admin) from '" + mucAddress + "' (but no such stanza was received).");
            assertResult(adminSeesBan, "Expected '" + conTwo.getUser() + "' to receive a presence stanza of type \"unavailable\" of '" + targetMucAddress + "' including status code 301 in the extended presence information after '" + targetAddress + "' is banned by '" + conTwo.getUser() + "' (an admin) from '" + mucAddress + "' (but no such stanza was received).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that an admin cannot ban itself.
     */
    @SmackIntegrationTest(section = "9.1", quote = "If an admin [..] attempts to ban himself, the service MUST deny the request and return a <conflict/> error to the sender.")
    public void mucTestAdminCannotBanSelf() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-cannot-ban-self");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress = conTwo.getUser().asEntityBareJid();

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByAdmin.join(nicknameAdmin);

            // Execute system under test & Verify result.
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
                mucAsSeenByAdmin.banUser(targetAddress, "Banned as part of a test.");
            }, "Expected an error after '" + conTwo.getUser() + "' (that is an admin) tried to ban itself from room '" + mucAddress + "' (but none occurred).");
            assertEquals(StanzaError.Condition.conflict, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' (that is an admin) after it tried to ban itself from room '" + mucAddress + "'.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that an admin cannot ban an owner.
     */
    @SmackIntegrationTest(section = "9.1", quote = "[A] user cannot be banned by an admin with a lower affiliation. Therefore, if an admin attempts to ban an owner, the service MUST deny the request and return a <not-allowed/> error to the sender")
    public void mucTestAdminCannotBanOwner() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("admin-cannot-ban-owner");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByAdmin = mucManagerTwo.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress = conOne.getUser().asEntityBareJid();

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("admin-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByOwner.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByAdmin.join(nicknameAdmin);

            // Execute system under test & Verify result.
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
                mucAsSeenByAdmin.banUser(targetAddress, "Banned as part of a test.");
            }, "Expected an error after '" + conTwo.getUser() + "' (that is an admin) tried to ban an owner ('" + targetAddress + "') from room '" + mucAddress + "' (but none occurred).");
            assertEquals(StanzaError.Condition.not_allowed, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' (that is an admin) after it tried to ban an owner ('" + targetAddress + "') from room '" + mucAddress + "' while not being an admin.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Asserts that an owner cannot ban itself.
     */
    @SmackIntegrationTest(section = "9.1", quote = "If an [..] owner attempts to ban himself, the service MUST deny the request and return a <conflict/> error to the sender.")
    public void mucTestOwnerCannotBanSelf() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("owner-cannot-ban-self");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final EntityBareJid targetAddress = conOne.getUser().asEntityBareJid();

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            // Execute system under test & Verify result.
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
                mucAsSeenByOwner.banUser(targetAddress, "Banned as part of a test.");
            }, "Expected an error after '" + conTwo.getUser() + "' (that is an owner) tried to ban itself from room '" + mucAddress + "' (but none occurred).");
            assertEquals(StanzaError.Condition.conflict, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' (that is an owner) after it tried to ban itself from room '" + mucAddress + "'.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }
}
