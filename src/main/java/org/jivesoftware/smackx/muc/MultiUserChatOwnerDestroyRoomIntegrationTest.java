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
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smackx.muc.packet.Destroy;
import org.jivesoftware.smackx.muc.packet.MUCOwner;
import org.jivesoftware.smackx.xdata.form.FillableForm;
import org.jivesoftware.smackx.xdata.form.Form;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for section "10.9 Owner Use Cases: Destroying a Room" of XEP-0045: "Multi-User Chat"
 *
 * @see <a href="https://xmpp.org/extensions/xep-0045.html#destroyroom">XEP-0045 Section 10.9</a>
 */
@SpecificationReference(document = "XEP-0045", version = "1.34.6")
public class MultiUserChatOwnerDestroyRoomIntegrationTest extends AbstractMultiUserChatIntegrationTest
{
    public MultiUserChatOwnerDestroyRoomIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
        InterruptedException, TestNotPossibleException, MultiUserChatException.MucAlreadyJoinedException, MultiUserChatException.MissingMucCreationAcknowledgeException, MultiUserChatException.NotAMucServiceException, XmppStringprepException
    {
        super(environment);
    }

    /**
     * Verifies that a room that is persistent can be destroyed.
     */
    @SmackIntegrationTest(section = "10.9", quote = "A room owner MUST be able to destroy a room, especially if the room is persistent.")
    public void testDestroyPersistentRoom() throws MultiUserChatException.MucAlreadyJoinedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, MultiUserChatException.MissingMucCreationAcknowledgeException, SmackException.NoResponseException, InterruptedException, MultiUserChatException.NotAMucServiceException, XmppStringprepException, TestNotPossibleException
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-destroy-persistent");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        try {
            createMuc(mucAsSeenByOwner, nicknameOwner);

            // TODO find a way to configure the room as persistent upon creation, not immediately after it was created.
            final Form configForm = mucAsSeenByOwner.getConfigurationForm();
            if (configForm.hasField("muc#roomconfig_persistentroom") && !configForm.readBoolean("muc#roomconfig_persistentroom")) {
                final FillableForm answerForm = configForm.getFillableForm();
                answerForm.setAnswer("muc#roomconfig_persistentroom", true);
                try {
                    mucAsSeenByOwner.sendConfigurationForm(answerForm);
                } catch (XMPPException.XMPPErrorException e) {
                    throw new TestNotPossibleException("Service does not allow room to be configured as a persistent room.");
                }
            }

            // Execute system under test.
            final MUCOwner request = new MUCOwner();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.setDestroy(new Destroy(null, null));

            try {
                conOne.sendIqRequestAndWaitForResponse(request);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to destroy the persistent room '" + mucAddress + "' (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            try {
                if (mucAsSeenByOwner.isJoined()) {
                    tryDestroy(mucAsSeenByOwner); // If the test fails, then this is also likely to fail.
                }
            } catch (XMPPException.XMPPErrorException e) { // TODO remove this catch after SMACK-949 gets fixed.
                // Room was likely already destroyed.
            }
        }
    }

    /**
     * Verifies that a room that is temporary can be destroyed.
     */
    @SmackIntegrationTest(section = "10.9", quote = "A room owner MUST be able to destroy a room")
    public void testDestroyTemporaryRoom() throws MultiUserChatException.MucAlreadyJoinedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, MultiUserChatException.MissingMucCreationAcknowledgeException, SmackException.NoResponseException, InterruptedException, MultiUserChatException.NotAMucServiceException, XmppStringprepException, TestNotPossibleException
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-destroy-temporary");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        try {
            createMuc(mucAsSeenByOwner, nicknameOwner);

            // TODO find a way to configure the room as temporary upon creation, not immediately after it was created.
            final Form configForm = mucAsSeenByOwner.getConfigurationForm();
            if (configForm.hasField("muc#roomconfig_persistentroom") && configForm.readBoolean("muc#roomconfig_persistentroom")) {
                final FillableForm answerForm = configForm.getFillableForm();
                answerForm.setAnswer("muc#roomconfig_persistentroom", false);
                try {
                    mucAsSeenByOwner.sendConfigurationForm(answerForm);
                } catch (XMPPException.XMPPErrorException e) {
                    throw new TestNotPossibleException("Service does not allow room to be configured as a temporary room.");
                }
            }

            // Execute system under test.
            final MUCOwner request = new MUCOwner();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.setDestroy(new Destroy(null, null));
            try {
                conOne.sendIqRequestAndWaitForResponse(request);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to destroy the temporary room '" + mucAddress + "' (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            try {
                if (mucAsSeenByOwner.isJoined()) {
                    tryDestroy(mucAsSeenByOwner); // If the test fails, then this is also likely to fail.
                }
            } catch (XMPPException.XMPPErrorException e) { // TODO remove this catch after SMACK-949 gets fixed.
                // Room was likely already destroyed.
            }
        }
    }

    /**
     * Verifies that a room destruction request can contain an alternate venue.
     */
    @SmackIntegrationTest(section = "10.9", quote = "In order to destroy a room, the room owner MUST send an IQ set to the address of the room to be destroyed. [...] The address of the alternate venue MAY be provided as the value of the <destroy/> element's 'jid' attribute.")
    public void testAlternateVenue() throws MultiUserChatException.MucAlreadyJoinedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, MultiUserChatException.MissingMucCreationAcknowledgeException, SmackException.NoResponseException, InterruptedException, MultiUserChatException.NotAMucServiceException, XmppStringprepException, TestNotPossibleException
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-destroy-alternatevenue");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        try {
            createMuc(mucAsSeenByOwner, nicknameOwner);

            // Execute system under test.
            final MUCOwner request = new MUCOwner();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.setDestroy(new Destroy(JidCreate.entityBareFrom("alternate@muc.example.org"), null));
            try {
                conOne.sendIqRequestAndWaitForResponse(request);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to destroy room '" + mucAddress + "' while providing an alternative venue (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            try {
                if (mucAsSeenByOwner.isJoined()) {
                    tryDestroy(mucAsSeenByOwner); // If the test fails, then this is also likely to fail.
                }
            } catch (XMPPException.XMPPErrorException e) { // TODO remove this catch after SMACK-949 gets fixed.
                // Room was likely already destroyed.
            }
        }
    }

// TODO enable this once SMACK-950 gets fixed.
//    /**
//     * Verifies that a room destruction request can contain an alternate venue and alternate venue password.
//     */
//    @SmackIntegrationTest(section = "10.9", quote = "In order to destroy a room, the room owner MUST send an IQ set to the address of the room to be destroyed. [...] The address of the alternate venue MAY be provided as the value of the <destroy/> element's 'jid' attribute. A password for the alternate venue MAY be provided as the XML character data of a <password/> child element of the <destroy/> element.")
//    public void testAlternateVenuePassword() throws MultiUserChatException.MucAlreadyJoinedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, MultiUserChatException.MissingMucCreationAcknowledgeException, SmackException.NoResponseException, InterruptedException, MultiUserChatException.NotAMucServiceException, XmppStringprepException, TestNotPossibleException
//    {
//        // Setup test fixture.
//        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-destroy-alternatevenuepassword");
//        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
//        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
//        try {
//            createMuc(mucAsSeenByOwner, nicknameOwner);
//
//            // Execute system under test.
//            final MUCOwner request = new MUCOwner();
//            request.setTo(mucAddress);
//            request.setType(IQ.Type.set);
//            request.setDestroy(new Destroy(JidCreate.entityBareFrom("alternate@muc.example.org"), "secret", null));
//            try {
//                conOne.sendIqRequestAndWaitForResponse(request);
//
//                // Verify result.
//            } catch (XMPPException.XMPPErrorException e) {
//                fail("Expected owner '" + conOne.getUser() + "' to be able to destroy room '" + mucAddress + "' while providing an alternative venue and password (but the server returned an error).", e);
//            }
//        } finally {
//            // Tear down test fixture.
//            try {
//                if (mucAsSeenByOwner.isJoined()) {
//                    tryDestroy(mucAsSeenByOwner); // If the test fails, then this is also likely to fail.
//                }
//            } catch (XMPPException.XMPPErrorException e) { // TODO remove this catch after SMACK-949 gets fixed.
//                // Room was likely already destroyed.
//            }
//        }
//    }

    /**
     * Verifies that a room destruction request can contain a reason.
     */
    @SmackIntegrationTest(section = "10.9", quote = "In order to destroy a room, the room owner MUST send an IQ set to the address of the room to be destroyed. [...] The reason for the room destruction MAY be provided as the XML character data of a <reason/> child element of the <destroy/> element.")
    public void testReason() throws MultiUserChatException.MucAlreadyJoinedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, MultiUserChatException.MissingMucCreationAcknowledgeException, SmackException.NoResponseException, InterruptedException, MultiUserChatException.NotAMucServiceException, XmppStringprepException, TestNotPossibleException
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-destroy-reason");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        try {
            createMuc(mucAsSeenByOwner, nicknameOwner);

            // Execute system under test.
            final MUCOwner request = new MUCOwner();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.setDestroy(new Destroy(null, "Room destroyed as part of an integration test."));
            try {
                conOne.sendIqRequestAndWaitForResponse(request);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to destroy room '" + mucAddress + "' while providing reason for the destruction (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            try {
                if (mucAsSeenByOwner.isJoined()) {
                    tryDestroy(mucAsSeenByOwner); // If the test fails, then this is also likely to fail.
                }
            } catch (XMPPException.XMPPErrorException e) { // TODO remove this catch after SMACK-949 gets fixed.
                // Room was likely already destroyed.
            }
        }
    }

    /**
     * Verifies that a room destruction request causes all occupants to be removed.
     */
    @SmackIntegrationTest(section = "10.9", quote = "The service is responsible for removing all the occupants. [...] sending only one presence stanza of type \"unavailable\" to each occupant so that the user knows he or she has been removed from the room.")
    public void testPresence() throws MultiUserChatException.MucAlreadyJoinedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, MultiUserChatException.MissingMucCreationAcknowledgeException, SmackException.NoResponseException, InterruptedException, MultiUserChatException.NotAMucServiceException, XmppStringprepException, TestNotPossibleException, TimeoutException
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-destroy-presence");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);

        final SimpleResultSyncPoint ownerSeesDestroy = new SimpleResultSyncPoint();
        final SimpleResultSyncPoint participantSeesDestroy = new SimpleResultSyncPoint();
        final UserStatusListener ownerListener = new UserStatusListener() {
            @Override
            public void roomDestroyed(MultiUserChat alternateMUC, String reason) {
                ownerSeesDestroy.signal();
            }
        };
        final UserStatusListener participantListener = new UserStatusListener() {
            @Override
            public void roomDestroyed(MultiUserChat alternateMUC, String reason) {
                participantSeesDestroy.signal();
            }
        };
        mucAsSeenByOwner.addUserStatusListener(ownerListener);
        mucAsSeenByParticipant.addUserStatusListener(participantListener);
        try {
            createMuc(mucAsSeenByOwner, nicknameOwner);
            mucAsSeenByParticipant.join(nicknameParticipant);

            // Execute system under test.
            final MUCOwner request = new MUCOwner();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.setDestroy(new Destroy(null, null));

            conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result.
            assertResult(ownerSeesDestroy, "Expected owner '" + conOne.getUser() + "' (joined as '" + nicknameOwner + "') to be notified of destruction of room '" + mucAddress + "' (but no such notification was received).");
            assertResult(participantSeesDestroy, "Expected participant '" + conTwo.getUser() + "' (joined as '" + nicknameParticipant + "') to be notified of destruction of room '" + mucAddress + "' (but no such notification was received).");
        } finally {
            // Tear down test fixture.
            mucAsSeenByOwner.removeUserStatusListener(ownerListener);
            mucAsSeenByParticipant.removeUserStatusListener(participantListener);
            try {
                if (mucAsSeenByOwner.isJoined()) {
                    tryDestroy(mucAsSeenByOwner); // If the test fails, then this is also likely to fail.
                }
            } catch (XMPPException.XMPPErrorException e) { // TODO remove this catch after SMACK-949 gets fixed.
                // Room was likely already destroyed.
            }
        }
    }

    /**
     * Verifies that a room destruction request causes all occupants to be removed, receiving a notification that contains
     * all optional data.
     */
    @SmackIntegrationTest(section = "10.9", quote = "The service is responsible for removing all the occupants. [...] sending only one presence stanza of type \"unavailable\" to each occupant so that the user knows he or she has been removed from the room. If extended presence information specifying the JID of an alternate location and the reason for the room destruction was provided by the room owner, the presence stanza MUST include that information.")
    public void testPresenceOptional() throws MultiUserChatException.MucAlreadyJoinedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, MultiUserChatException.MissingMucCreationAcknowledgeException, SmackException.NoResponseException, InterruptedException, MultiUserChatException.NotAMucServiceException, XmppStringprepException, TestNotPossibleException, TimeoutException
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-destroy-presenceext");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);

        final EntityBareJid alternateVenue = JidCreate.entityBareFrom("alternate@muc.example.org");
        final String password = "secret"; // TODO use this after SMACK-950 gets resolved.
        final String reason = "Destroying room as part of an integration test";

        final ResultSyncPoint<Destroy, Exception> ownerSeesDestroy = new ResultSyncPoint<>();
        final ResultSyncPoint<Destroy, Exception> participantSeesDestroy = new ResultSyncPoint<>();
        final UserStatusListener ownerListener = new UserStatusListener() {
            @Override
            public void roomDestroyed(MultiUserChat alternateMUC, String reason) {
                ownerSeesDestroy.signal(new Destroy(alternateMUC == null ? null : alternateMUC.getRoom(), reason));
            }
        };
        final UserStatusListener participantListener = new UserStatusListener() {
            @Override
            public void roomDestroyed(MultiUserChat alternateMUC, String reason) {
                participantSeesDestroy.signal(new Destroy(alternateMUC == null ? null : alternateMUC.getRoom(), reason));
            }
        };
        mucAsSeenByOwner.addUserStatusListener(ownerListener);
        mucAsSeenByParticipant.addUserStatusListener(participantListener);
        try {
            createMuc(mucAsSeenByOwner, nicknameOwner);
            mucAsSeenByParticipant.join(nicknameParticipant);

            // Execute system under test.
            final MUCOwner request = new MUCOwner();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.setDestroy(new Destroy(alternateVenue, reason));

            conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result.
            final Destroy ownerDestroy = assertResult(ownerSeesDestroy, "Expected owner '" + conOne.getUser() + "' (joined as '" + nicknameOwner + "') to be notified of destruction of room '" + mucAddress + "' (but no such notification was received).");
            assertEquals(alternateVenue, ownerDestroy.getJid(), "Expected the presence received by owner '" + conOne.getUser() + "' (joined as '" + nicknameOwner + "') after room '" + mucAddress + "' got destroyed to include the alternate venue address that was provided in the destruction request (but that was found in the received presence).");
            // TODO enable this after SMACK-950 gets resolved // assertEquals(password, ownerDestroy.getPassword(), "Expected the presence received by owner '" + conOne.getUser() + "' (joined as '" + nicknameOwner + "') after room '" + mucAddress + "' got destroyed to include the alternate venue password that was provided in the destruction request (but that was found in the received presence).");
            assertEquals(reason, ownerDestroy.getReason(), "Expected the presence received by owner '" + conOne.getUser() + "' (joined as '" + nicknameOwner + "') after room '" + mucAddress + "' got destroyed to include the reason that was provided in the destruction request (but that was found in the received presence).");
            final Destroy participantDestroy = assertResult(participantSeesDestroy, "Expected participant '" + conTwo.getUser() + "' (joined as '" + nicknameParticipant + "') to be notified of destruction of room '" + mucAddress + "' (but no such notification was received).");
            assertEquals(alternateVenue, participantDestroy.getJid(), "Expected the presence received by participant '" + conTwo.getUser() + "' (joined as '" + nicknameParticipant + "') after room '" + mucAddress + "' got destroyed to include the alternate venue address that was provided in the destruction request (but that was found in the received presence).");
            // TODO enable this after SMACK-950 gets resolved // assertEquals(password, participantDestroy.getPassword(), "Expected the presence received by participant '" + conTwo.getUser() + "' (joined as '" + nicknameParticipant + "') after room '" + mucAddress + "' got destroyed to include the alternate venue password that was provided in the destruction request (but that was found in the received presence).");
            assertEquals(reason, participantDestroy.getReason(), "Expected the presence received by participant '" + conTwo.getUser() + "' (joined as '" + nicknameParticipant + "') after room '" + mucAddress + "' got destroyed to include the reason that was provided in the destruction request (but that was found in the received presence).");
        } finally {
            // Tear down test fixture.
            mucAsSeenByOwner.removeUserStatusListener(ownerListener);
            mucAsSeenByParticipant.removeUserStatusListener(participantListener);
            try {
                if (mucAsSeenByOwner.isJoined()) {
                    tryDestroy(mucAsSeenByOwner); // If the test fails, then this is also likely to fail.
                }
            } catch (XMPPException.XMPPErrorException e) { // TODO remove this catch after SMACK-949 gets fixed.
                // Room was likely already destroyed.
            }
        }
    }

    /**
     * Verifies that a room destruction request cannot be issued by a non-owner.
     */
    @SmackIntegrationTest(section = "10.9", quote = "If the <user@host> of the 'from' address received on a destroy request does not match the bare JID of a room owner, the service MUST return a <forbidden/> error to the sender")
    public void testAuthorization() throws MultiUserChatException.MucAlreadyJoinedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, MultiUserChatException.MissingMucCreationAcknowledgeException, SmackException.NoResponseException, InterruptedException, MultiUserChatException.NotAMucServiceException, XmppStringprepException, TestNotPossibleException, TimeoutException
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-destroy-auth");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);

        try {
            createMuc(mucAsSeenByOwner, nicknameOwner);
            mucAsSeenByParticipant.join(nicknameParticipant);

            // Execute system under test.
            final MUCOwner request = new MUCOwner();
            request.setTo(mucAddress);
            request.setType(IQ.Type.set);
            request.setDestroy(new Destroy(null, null));

            // Verify result.
            final XMPPException.XMPPErrorException xmppErrorException = assertThrows(XMPPException.XMPPErrorException.class, () -> { conTwo.sendIqRequestAndWaitForResponse(request); },
                "Expected an error to be returned when '" + conTwo.getUser() + "' (joined as '" + nicknameParticipant + "', not a room owner) tried to destroy room '" + mucAddress + "' (but no error was received).");
            assertEquals(StanzaError.Condition.forbidden, xmppErrorException.getStanzaError().getCondition(), "Unexpected condition in the (expected) error after '" + conTwo.getUser() + "' (joined as '" + nicknameParticipant + "', not a room owner) tried to destroy room '" + mucAddress);
        } finally {
            // Tear down test fixture.
            try {
                if (mucAsSeenByOwner.isJoined()) {
                    tryDestroy(mucAsSeenByOwner);
                }
            } catch (XMPPException.XMPPErrorException e) { // TODO remove this catch after SMACK-949 gets fixed.
                // Room was likely already destroyed.
            }
        }
    }
}
