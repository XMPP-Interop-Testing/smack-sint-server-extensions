/*
 * Copyright 2025 Guus der Kinderen
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
package org.igniterealtime.smack.inttest.xep0421;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.ResultSyncPoint;
import org.igniterealtime.smack.inttest.xep0421.provider.OccupantId;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.muc.*;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Integration Tests for XEP-0421: Occupant identifiers for semi-anonymous MUCs
 *
 * @see <a href="https://xmpp.org/extensions/xep-0421.html">XEP-0421</a>
 */
@SpecificationReference(document = "XEP-0421", version = "1.0.0")
public class OccupantIdIntegrationTest extends AbstractSmackIntegrationTest
{
    private EntityBareJid testRoomAddress;
    private MultiUserChat ownerRoom;

    public OccupantIdIntegrationTest(SmackIntegrationTestEnvironment environment)
    {
        super(environment);
        ProviderManager.addExtensionProvider(OccupantId.ELEMENT_NAME, OccupantId.NAMESPACE, new OccupantId.Provider());
    }

    public void createRoom() throws TestNotPossibleException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        createRoom(false);
    }

    public void createRoom(boolean moderated) throws TestNotPossibleException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        final MultiUserChatManager mucManager = MultiUserChatManager.getInstanceFor(conThree);
        final DomainBareJid mucDomain = mucManager.getMucServiceDomains().stream().findFirst().orElseThrow(() -> new TestNotPossibleException("Unable to find a MUC service domain"));

        try {
            final String roomNameLocal = String.join("-", "smack-inttest-xep0421", testRunId);
            testRoomAddress = JidCreate.entityBareFrom(Localpart.from(roomNameLocal), mucDomain);
            ownerRoom = mucManager.getMultiUserChat(testRoomAddress);
            final MucConfigFormManager configFormManager = ownerRoom.create(Resourcepart.from("test-admin")).getConfigFormManager();
            if (moderated) {
                configFormManager.makeModerated();
            }
            configFormManager.submitConfigurationForm();

            final boolean supportsFeature = ServiceDiscoveryManager.getInstanceFor(conThree).supportsFeature(ownerRoom.getRoom(), OccupantId.NAMESPACE);
            if (!supportsFeature) {
                throw new TestNotPossibleException("Rooms created on the service do not support the 'urn:xmpp:occupant-id:0' feature.");
            }
        } catch (Exception e) {
            if (e instanceof TestNotPossibleException) {
                throw (TestNotPossibleException) e;
            }
            throw new TestNotPossibleException("Unable to create MUC room.", e);
        }
    }

    public void removeRoom() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XmppStringprepException
    {
        if (ownerRoom != null) {
            ownerRoom.destroy();
        }
    }

    @SmackIntegrationTest(section = "3.1", quote = "When a user enters a room, they send a presence to claim the nickname in the MUC. A MUC that supports occupant identifiers attaches an <occupant-id> element within the \"urn:xmpp:occupant-id:0\" namespace to the presence sent to all occupants in the room.")
    public void testOccupantIdInReflectedJoinPresence() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException, XmppStringprepException, MultiUserChatException.NotAMucServiceException, MultiUserChatException.MucNotJoinedException
    {
        fixtureForReflectedJoinPresence(reflectedPresence -> {
            final OccupantId extension = reflectedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence sent back to user '" + conOne.getUser() + "' after they joined room '" + testRoomAddress + "' to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInReflectedJoinPresence() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException, XmppStringprepException, MultiUserChatException.NotAMucServiceException, MultiUserChatException.MucNotJoinedException
    {
        fixtureForReflectedJoinPresence(reflectedPresence -> {
            final List<OccupantId> extensions = reflectedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence sent back to user '" + conOne.getUser() + "' after they joined room '" + testRoomAddress + "' to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForReflectedJoinPresence(final Function<Presence, Void> assertion) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException, XmppStringprepException, MultiUserChatException.NotAMucServiceException, MultiUserChatException.MucNotJoinedException
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChat room = mucManagerOne.getMultiUserChat(testRoomAddress);

        try {
            // Execute system under test.
            final Presence reflectedPresence = room.join(Resourcepart.from("test-user"));

            // Verify result.
            assertion.apply(reflectedPresence);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "3.1", quote = "When a user enters a room, they send a presence to claim the nickname in the MUC. A MUC that supports occupant identifiers attaches an <occupant-id> element within the \"urn:xmpp:occupant-id:0\" namespace to the presence sent to all occupants in the room.")
    public void testOccupantIdInBroadcastJoinPresence() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");

        fixtureForBroadcastJoinPresence(recipientNickname, broadcastedPresence -> {
            final OccupantId extension = broadcastedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' joined the room to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInBroadcastJoinPresence() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");

        fixtureForBroadcastJoinPresence(recipientNickname, broadcastedPresence -> {
            final List<OccupantId> extensions = broadcastedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' joined the room to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    public void fixtureForBroadcastJoinPresence(final Resourcepart recipientNickname, final Function<Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChatManager mucManagerTwo = MultiUserChatManager.getInstanceFor(conTwo);
        final MultiUserChat roomOne = mucManagerOne.getMultiUserChat(testRoomAddress);
        final MultiUserChat roomTwo = mucManagerTwo.getMultiUserChat(testRoomAddress);
        final Resourcepart joinerNickname = Resourcepart.from("test-joiner");
        final FullJid joinerRoomAddress = JidCreate.fullFrom(testRoomAddress, joinerNickname);

        try {
            roomTwo.join(recipientNickname);

            final ResultSyncPoint<Presence, Exception> presenceReceived = new ResultSyncPoint<>();

            roomTwo.addParticipantListener(presence -> {
                if (presence.getFrom().equals(joinerRoomAddress)) {
                    presenceReceived.signal(presence);
                }
            });

            // Execute system under test.
            roomOne.join(joinerNickname);
            final Presence result = presenceReceived.waitForResult(timeout);

            // Verify result.
            assertion.apply(result);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "3.2", quote = "A MUC supporting occupant identifiers attaches an <occupant-id> element within the \"urn:xmpp:occupant-id:0\" to the message sent to all occupants in the room.")
    public void testOccupantIdInReflectedMessage() throws Exception
    {
        fixtureForReflectedMessage(reflectedMessage -> {
            final XmlElement extension = reflectedMessage.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the message sent back to user '" + conOne.getUser() + "' after they sent it in room '" + testRoomAddress + "' to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInReflectedMessage() throws Exception
    {
        fixtureForReflectedMessage(reflectedMessage -> {
            final List<OccupantId> extensions = reflectedMessage.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the message sent back to user '" + conOne.getUser() + "' after they sent it in room '" + testRoomAddress + "' to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForReflectedMessage(final Function<Message, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChat room = mucManagerOne.getMultiUserChat(testRoomAddress);

        try {
            room.join(Resourcepart.from("test-user"));
            final ResultSyncPoint<Message, Exception> messageReceived = new ResultSyncPoint<>();

            room.addMessageListener(message -> {
                if (message.getFrom().equals(room.getMyRoomJid())) {
                    messageReceived.signal(message);
                }
            });

            // Execute system under test.
            room.sendMessage("test");
            final Message reflectedMessage = messageReceived.waitForResult(timeout);

            // Verify result.
            assertion.apply(reflectedMessage);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "3.2", quote = "A MUC supporting occupant identifiers attaches an <occupant-id> element within the \"urn:xmpp:occupant-id:0\" to the message sent to all occupants in the room.")
    public void testOccupantIdInBroadcastMessage() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");

        fixtureForBroadcastMessage(recipientNickname, broadcastedMessage -> {
            final OccupantId extension = broadcastedMessage.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the message received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' sent that message in room to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInBroadcastMessage() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");

        fixtureForBroadcastMessage(recipientNickname, broadcastedMessage -> {
            final List<OccupantId> extensions = broadcastedMessage.getExtensions(OccupantId.class);
            assertNotNull(extensions, "Expected the message received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' sent that message in room to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForBroadcastMessage(final Resourcepart recipientNickname, final Function<Message, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChatManager mucManagerTwo = MultiUserChatManager.getInstanceFor(conTwo);
        final MultiUserChat roomOne = mucManagerOne.getMultiUserChat(testRoomAddress);
        final MultiUserChat roomTwo = mucManagerTwo.getMultiUserChat(testRoomAddress);
        final Resourcepart senderNickname = Resourcepart.from("test-sender");
        final FullJid senderRoomAddress = JidCreate.fullFrom(testRoomAddress, senderNickname);

        try {
            roomOne.join(senderNickname);
            roomTwo.join(recipientNickname);

            final ResultSyncPoint<Message, Exception> messageReceived = new ResultSyncPoint<>();

            roomTwo.addMessageListener(message -> {
                if (message.getFrom().equals(senderRoomAddress)) {
                    messageReceived.signal(message);
                }
            });

            // Execute system under test.
            roomOne.sendMessage("test message");
            final Message result = messageReceived.waitForResult(timeout);

            // Verify result.
            assertion.apply(result);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "If the message or presence received by the MUC service already contains <occupant-id> element, the MUC service MUST replace such element before reflecting the message or presence including it.")
    public void testOccupantIdInReflectedMessageReplaced() throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChat room = mucManagerOne.getMultiUserChat(testRoomAddress);

        try {
            room.join(Resourcepart.from("test-user"));
            final ResultSyncPoint<Message, Exception> messageReceived = new ResultSyncPoint<>();

            room.addMessageListener(message -> {
                if (message.getFrom().equals(room.getMyRoomJid())) {
                    messageReceived.signal(message);
                }
            });

            final String needle = "ReplaceMe" + StringUtils.randomString(5);
            final Message message = room.buildMessage()
                .setBody("test")
                .addExtension(new OccupantId(needle))
                .build();

            // Execute system under test.
            conOne.sendStanza(message);
            final Message reflectedMessage = messageReceived.waitForResult(timeout);

            // Verify result.
            final List<OccupantId> extensions = reflectedMessage.getExtensions(OccupantId.class);
            assertFalse(extensions.isEmpty(), "Expected the message sent back to user '" + conOne.getUser() + "' after they sent it in room '" + testRoomAddress + "' to contain an occupant-id element (but it did not).");
            final Set<String> receivedOccupantIDs = extensions.stream().map(OccupantId::getId).collect(Collectors.toSet());
            assertFalse(receivedOccupantIDs.contains(needle), "Expected the message sent back to user '" + conOne.getUser() + "' after they sent it in room '" + testRoomAddress + "' to no longer contain the occupant-id provided by the client (but it did) The server should have replaced the client-provided occupant ID.");
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "If the message or presence received by the MUC service already contains <occupant-id> element, the MUC service MUST replace such element before reflecting the message or presence including it.")
    public void testOccupantIdInReflectedMessageReplacedMultiple() throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChat room = mucManagerOne.getMultiUserChat(testRoomAddress);

        try {
            room.join(Resourcepart.from("test-user"));
            final ResultSyncPoint<Message, Exception> messageReceived = new ResultSyncPoint<>();

            room.addMessageListener(message -> {
                if (message.getFrom().equals(room.getMyRoomJid())) {
                    messageReceived.signal(message);
                }
            });

            final String needleA = "ReplaceMe" + StringUtils.randomString(5);
            final String needleB = "ReplaceMe" + StringUtils.randomString(5);
            final Message message = room.buildMessage()
                .setBody("test")
                .addExtension(new OccupantId(needleA))
                .addExtension(new OccupantId(needleB))
                .build();

            // Execute system under test.
            conOne.sendStanza(message);
            final Message reflectedMessage = messageReceived.waitForResult(timeout);

            // Verify result.
            final List<OccupantId> extensions = reflectedMessage.getExtensions(OccupantId.class);
            assertFalse(extensions.isEmpty(), "Expected the message sent back to user '" + conOne.getUser() + "' after they sent it in room '" + testRoomAddress + "' to contain an occupant-id element (but it did not).");
            final Set<String> receivedOccupantIDs = extensions.stream().map(OccupantId::getId).collect(Collectors.toSet());
            assertFalse(receivedOccupantIDs.contains(needleA), "Expected the message sent back to user '" + conOne.getUser() + "' after they sent it in room '" + testRoomAddress + "' to no longer contain any of the occupant-ids provided by the client (but at least one of the received occupant-id was the same) The server should have replaced all client-provided occupant IDs.");
            assertFalse(receivedOccupantIDs.contains(needleB), "Expected the message sent back to user '" + conOne.getUser() + "' after they sent it in room '" + testRoomAddress + "' to no longer contain any of the occupant-ids provided by the client (but at least one of the received occupant-id was the same) The server should have replaced all client-provided occupant IDs.");
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "If the message or presence received by the MUC service already contains <occupant-id> element, the MUC service MUST replace such element before reflecting the message or presence including it.")
    public void testOccupantIdInBroadcastMessageReplaced() throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChatManager mucManagerTwo = MultiUserChatManager.getInstanceFor(conTwo);
        final MultiUserChat roomOne = mucManagerOne.getMultiUserChat(testRoomAddress);
        final MultiUserChat roomTwo = mucManagerTwo.getMultiUserChat(testRoomAddress);
        final Resourcepart senderNickname = Resourcepart.from("test-sender");
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");
        final FullJid senderRoomAddress = JidCreate.fullFrom(testRoomAddress, senderNickname);

        try {
            roomOne.join(senderNickname);
            roomTwo.join(recipientNickname);

            final ResultSyncPoint<Message, Exception> messageReceived = new ResultSyncPoint<>();

            roomTwo.addMessageListener(message -> {
                if (message.getFrom().equals(senderRoomAddress)) {
                    messageReceived.signal(message);
                }
            });

            final String needle = "ReplaceMe" + StringUtils.randomString(5);
            final Message message = roomOne.buildMessage()
                .setBody("test")
                .addExtension(new OccupantId(needle))
                .build();

            // Execute system under test.
            conOne.sendStanza(message);
            final Message result = messageReceived.waitForResult(timeout);

            // Verify result.
            final List<OccupantId> extensions = result.getExtensions(OccupantId.class);
            assertFalse(extensions.isEmpty(), "Expected the message received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' sent that message in room to contain an occupant-id element (but it did not).");
            assertFalse(extensions.size() > 1, "Expected the message received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' sent that message in room to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            assertNotEquals(needle, extensions.get(0).getId(), "Expected the message received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' sent that message in room to contain a different occupant-id than the occupant-id provided by the client (but the received occupant-id was the same).");
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "If the message or presence received by the MUC service already contains <occupant-id> element, the MUC service MUST replace such element before reflecting the message or presence including it.")
    public void testOccupantIdInReflectedAvailabilityStatusChangeReplaced() throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChat room = mucManagerOne.getMultiUserChat(testRoomAddress);

        try {
            room.join(Resourcepart.from("test-user"));
            final ResultSyncPoint<Presence, Exception> presenceReceived = new ResultSyncPoint<>();

            final String needleA = "ReplaceMe" + StringUtils.randomString(5);
            final String needleB = "test status " + StringUtils.randomString(5);
            room.addParticipantListener(presence -> {
                if (presence.getFrom().equals(room.getMyRoomJid()) && needleB.equals(presence.getStatus())) {
                    presenceReceived.signal(presence);
                }
            });

            // Execute system under test.
            final Presence changePresence = connection.getStanzaFactory().buildPresenceStanza()
                .to(room.getMyRoomJid())
                .ofType(Presence.Type.available)
                .setStatus(needleB)
                .setMode(Presence.Mode.away)
                .addExtension(new OccupantId(needleA))
                .build();
            conOne.sendStanza(changePresence);
            final Presence reflectedPresence = presenceReceived.waitForResult(timeout);

            // Verify result.
            final List<OccupantId> extensions = reflectedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.isEmpty(), "Expected the presence sent back to user '" + conOne.getUser() + "' after they changed their availability status in room '" + testRoomAddress + "' to contain an occupant-id element (but it did not).");
            assertFalse(extensions.size() > 1, "Expected the presence sent back to user '" + conOne.getUser() + "' after they changed their availability status in room '" + testRoomAddress + "' to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            assertNotEquals(needleA, extensions.get(0).getId(), "Expected the presence sent back to user '" + conOne.getUser() + "' after they changed their availability status in room '" + testRoomAddress + "' to contain a different occupant-id than the occupant-id provided by the client (but the received occupant-id was the same).");
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "If the message or presence received by the MUC service already contains <occupant-id> element, the MUC service MUST replace such element before reflecting the message or presence including it.")
    public void testOccupantIdInBroadcastAvailabilityStatusChangeReplaced() throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChatManager mucManagerTwo = MultiUserChatManager.getInstanceFor(conTwo);
        final MultiUserChat roomOne = mucManagerOne.getMultiUserChat(testRoomAddress);
        final MultiUserChat roomTwo = mucManagerTwo.getMultiUserChat(testRoomAddress);
        final Resourcepart joinerNickname = Resourcepart.from("test-joiner");
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");
        final FullJid joinerRoomAddress = JidCreate.fullFrom(testRoomAddress, joinerNickname);

        try {
            roomTwo.join(recipientNickname);
            roomOne.join(joinerNickname);

            final ResultSyncPoint<Presence, Exception> presenceReceived = new ResultSyncPoint<>();

            final String needleA = "ReplaceMe" + StringUtils.randomString(5);
            final String needleB = "test status " + StringUtils.randomString(5);
            roomTwo.addParticipantListener(presence -> {
                if (presence.getFrom().equals(joinerRoomAddress) && needleB.equals(presence.getStatus())) {
                    presenceReceived.signal(presence);
                }
            });

            // Execute system under test.
            final Presence changePresence = connection.getStanzaFactory().buildPresenceStanza()
                .to(roomOne.getMyRoomJid())
                .ofType(Presence.Type.available)
                .setStatus(needleB)
                .setMode(Presence.Mode.away)
                .addExtension(new OccupantId(needleA))
                .build();
            conOne.sendStanza(changePresence);

            roomOne.changeAvailabilityStatus(needleB, Presence.Mode.away);
            final Presence result = presenceReceived.waitForResult(timeout);

            // Verify result.
            final List<OccupantId> extensions = result.getExtensions(OccupantId.class);
            assertFalse(extensions.isEmpty(), "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' changed their availability status in the room to contain an occupant-id element (but it did not).");
            assertFalse(extensions.size() > 1, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' changed their availability status in the room to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            assertNotEquals(needleA, extensions.get(0).getId(), "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' changed their availability status in the room to contain a different occupant-id than the occupant-id provided by the client (but the received occupant-id was the same).");
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "The <occupant-id> element MUST be attached to [...] every presence sent by a MUC.")
    public void testOccupantIdInReflectedAvailabilityStatusChange() throws Exception
    {
        fixtureForReflectedAvailabilityStatusChange(reflectedPresence -> {
            final OccupantId extension = reflectedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence sent back to user '" + conOne.getUser() + "' after they changed their availability status in room '" + testRoomAddress + "' to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInReflectedAvailabilityStatusChange() throws Exception
    {
        fixtureForReflectedAvailabilityStatusChange(reflectedPresence -> {
            final List<OccupantId> extensions = reflectedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence sent back to user '" + conOne.getUser() + "' after they changed their availability status in room '" + testRoomAddress + "' to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForReflectedAvailabilityStatusChange(final Function<Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChat room = mucManagerOne.getMultiUserChat(testRoomAddress);

        try {
            room.join(Resourcepart.from("test-user"));
            final ResultSyncPoint<Presence, Exception> presenceReceived = new ResultSyncPoint<>();

            final String needle = "test status " + StringUtils.randomString(5);
            room.addParticipantListener(presence -> {
                if (presence.getFrom().equals(room.getMyRoomJid()) && needle.equals(presence.getStatus())) {
                    presenceReceived.signal(presence);
                }
            });

            // Execute system under test.
            room.changeAvailabilityStatus(needle, Presence.Mode.away);
            final Presence reflectedPresence = presenceReceived.waitForResult(timeout);

            // Verify result.
            assertion.apply(reflectedPresence);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "The <occupant-id> element MUST be attached to [...] every presence sent by a MUC.")
    public void testOccupantIdInBroadcastAvailabilityStatusChange() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");

        fixtureForBroadcastAvailabilityStatusChange(recipientNickname, broadcastedPresence -> {
            final OccupantId extension = broadcastedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' changed their availability status in the room to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInBroadcastAvailabilityStatusChange() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");

        fixtureForBroadcastAvailabilityStatusChange(recipientNickname, broadcastedPresence -> {
            final List<OccupantId> extensions = broadcastedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' changed their availability status in the room to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForBroadcastAvailabilityStatusChange(final Resourcepart recipientNickname, final Function<Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChatManager mucManagerTwo = MultiUserChatManager.getInstanceFor(conTwo);
        final MultiUserChat roomOne = mucManagerOne.getMultiUserChat(testRoomAddress);
        final MultiUserChat roomTwo = mucManagerTwo.getMultiUserChat(testRoomAddress);
        final Resourcepart joinerNickname = Resourcepart.from("test-joiner");
        final FullJid joinerRoomAddress = JidCreate.fullFrom(testRoomAddress, joinerNickname);

        try {
            roomTwo.join(recipientNickname);
            roomOne.join(joinerNickname);

            final ResultSyncPoint<Presence, Exception> presenceReceived = new ResultSyncPoint<>();

            final String needle = "test status " + StringUtils.randomString(5);
            roomTwo.addParticipantListener(presence -> {
                if (presence.getFrom().equals(joinerRoomAddress) && needle.equals(presence.getStatus())) {
                    presenceReceived.signal(presence);
                }
            });

            // Execute system under test.
            roomOne.changeAvailabilityStatus(needle, Presence.Mode.away);
            final Presence result = presenceReceived.waitForResult(timeout);

            // Verify result.
            assertion.apply(result);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "The <occupant-id> element MUST be attached to [...] every presence sent by a MUC.")
    public void testOccupantIdInReflectedNicknameChange() throws Exception
    {
        fixtureForReflectedNicknameChange(reflectedPresence -> {
            final OccupantId extension = reflectedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence sent back to user '" + conOne.getUser() + "' after they changed their nickname in room '" + testRoomAddress + "' to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInReflectedNicknameChange() throws Exception
    {
        fixtureForReflectedNicknameChange(reflectedPresence -> {
            final List<OccupantId> extensions = reflectedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence sent back to user '" + conOne.getUser() + "' after they changed their nickname in room '" + testRoomAddress + "' to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForReflectedNicknameChange(final Function<Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChat room = mucManagerOne.getMultiUserChat(testRoomAddress);

        try {
            room.join(Resourcepart.from("test-user"));
            final ResultSyncPoint<Presence, Exception> presenceReceived = new ResultSyncPoint<>();

            final Resourcepart needle = Resourcepart.from("test nick " + StringUtils.randomString(5));
            room.addParticipantListener(presence -> {
                if (presence.getFrom().getResourceOrEmpty().equals(needle)) {
                    presenceReceived.signal(presence);
                }
            });

            // Execute system under test.
            room.changeNickname(needle);
            final Presence reflectedPresence = presenceReceived.waitForResult(timeout);

            // Verify result.
            assertion.apply(reflectedPresence);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "The <occupant-id> element MUST be attached to [...] every presence sent by a MUC.")
    public void testOccupantIdInBroadcastNicknameChange() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");

        fixtureForBroadcastNicknameChange(recipientNickname, broadcastedPresence -> {
            final OccupantId extension = broadcastedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' changed their nickname in the room to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInBroadcastNicknameChange() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");
        fixtureForBroadcastNicknameChange(recipientNickname, broadcastedPresence -> {
            final List<OccupantId> extensions = broadcastedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' changed their nickname in the room to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForBroadcastNicknameChange(final Resourcepart recipientNickname, final Function<Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChatManager mucManagerTwo = MultiUserChatManager.getInstanceFor(conTwo);
        final MultiUserChat roomOne = mucManagerOne.getMultiUserChat(testRoomAddress);
        final MultiUserChat roomTwo = mucManagerTwo.getMultiUserChat(testRoomAddress);
        final Resourcepart joinerNickname = Resourcepart.from("test-joiner");
        final FullJid joinerRoomAddress = JidCreate.fullFrom(testRoomAddress, joinerNickname);

        try {
            roomTwo.join(recipientNickname);
            roomOne.join(joinerNickname);

            final ResultSyncPoint<Presence, Exception> presenceReceived = new ResultSyncPoint<>();

            final Resourcepart needle = Resourcepart.from("test nick " + StringUtils.randomString(5));
            roomTwo.addParticipantListener(presence -> {
                if (presence.getFrom().getResourceOrEmpty().equals(needle)) {
                    presenceReceived.signal(presence);
                }
            });

            // Execute system under test.
            roomOne.changeNickname(needle);
            final Presence result = presenceReceived.waitForResult(timeout);

            // Verify result.
            assertion.apply(result);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "The <occupant-id> element MUST be attached to [...] every presence sent by a MUC.")
    public void testOccupantIdInReflectedLeavePresence() throws Exception
    {
        fixtureForReflectedLeavePresence(reflectedPresence -> {
            final OccupantId extension = reflectedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence sent back to user '" + conOne.getUser() + "' after they leave room '" + testRoomAddress + "' to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInReflectedLeavePresence() throws Exception
    {
        fixtureForReflectedLeavePresence(reflectedPresence -> {
            final List<OccupantId> extensions = reflectedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence sent back to user '" + conOne.getUser() + "' after they leave room '" + testRoomAddress + "' to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForReflectedLeavePresence(final Function<Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChat room = mucManagerOne.getMultiUserChat(testRoomAddress);
        final Resourcepart nickname = Resourcepart.from("test-user");

        try {
            room.join(nickname);

            // Execute system under test.
            final Presence reflectedPresence = room.leave();

            // Verify result.
            assertion.apply(reflectedPresence);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "The <occupant-id> element MUST be attached to [...] every presence sent by a MUC.")
    public void testOccupantIdInBroadcastLeavePresence() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");

        fixtureForBroadcastLeavePresence(recipientNickname, broadcastedPresence -> {
            final OccupantId extension = broadcastedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' leaves the room to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInBroadcastLeavePresence() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");

        fixtureForBroadcastLeavePresence(recipientNickname, broadcastedPresence -> {
            final List<OccupantId> extensions = broadcastedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' leaves the room to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForBroadcastLeavePresence(final Resourcepart recipientNickname, final Function<Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChatManager mucManagerTwo = MultiUserChatManager.getInstanceFor(conTwo);
        final MultiUserChat roomOne = mucManagerOne.getMultiUserChat(testRoomAddress);
        final MultiUserChat roomTwo = mucManagerTwo.getMultiUserChat(testRoomAddress);
        final Resourcepart joinerNickname = Resourcepart.from("test-joiner");
        final FullJid joinerRoomAddress = JidCreate.fullFrom(testRoomAddress, joinerNickname);

        try {
            roomTwo.join(recipientNickname);
            roomOne.join(joinerNickname);

            final ResultSyncPoint<Presence, Exception> presenceReceived = new ResultSyncPoint<>();

            roomTwo.addParticipantListener(presence -> {
                if (presence.getFrom().equals(joinerRoomAddress) && presence.getType() == Presence.Type.unavailable) {
                    presenceReceived.signal(presence);
                }
            });

            // Execute system under test.
            roomOne.leave();
            final Presence result = presenceReceived.waitForResult(timeout);

            // Verify result.
            assertion.apply(result);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "The <occupant-id> element MUST be attached to [...] every presence sent by a MUC.")
    public void testOccupantIdInReflectedRoleChangeToParticipant() throws Exception
    {
        fixtureForReflectedRoleChangeToParticipant(reflectedPresence -> {
            final OccupantId extension = reflectedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence sent back to user '" + conOne.getUser() + "' after they where granted voice in room '" + testRoomAddress + "' to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInReflectedRoleChangeToParticipant() throws Exception
    {
        fixtureForReflectedRoleChangeToParticipant(reflectedPresence -> {
            final List<OccupantId> extensions = reflectedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence sent back to user '" + conOne.getUser() + "' after they where granted voice in room '" + testRoomAddress + "' to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForReflectedRoleChangeToParticipant(final Function<Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom(true);
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChat room = mucManagerOne.getMultiUserChat(testRoomAddress);
        final Resourcepart nickname = Resourcepart.from("test-user");

        try {
            room.join(nickname);
            final ResultSyncPoint<Presence, Exception> presenceReceived = new ResultSyncPoint<>();

            room.addParticipantListener(presence -> {
                if (presence.getFrom().equals(room.getMyRoomJid()) && MUCUser.from(presence).getItem().getRole().equals(MUCRole.participant)) {
                    presenceReceived.signal(presence);
                }
            });

            // Execute system under test.
            ownerRoom.grantVoice(nickname);
            final Presence reflectedPresence = presenceReceived.waitForResult(timeout);

            // Verify result.
            assertion.apply(reflectedPresence);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "The <occupant-id> element MUST be attached to [...] every presence sent by a MUC.")
    public void testOccupantIdInBroadcastRoleChangeToParticipant() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");

        fixtureForBroadcastRoleChangeToParticipant(recipientNickname, broadcastedPresence -> {
            final OccupantId extension = broadcastedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' where granted voice in the room to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInBroadcastRoleChangeToParticipant() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");

        fixtureForBroadcastRoleChangeToParticipant(recipientNickname, broadcastedPresence -> {
            final List<OccupantId> extensions = broadcastedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' where granted voice in the room to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForBroadcastRoleChangeToParticipant(final Resourcepart recipientNickname, final Function<Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom(true);
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChatManager mucManagerTwo = MultiUserChatManager.getInstanceFor(conTwo);
        final MultiUserChat roomOne = mucManagerOne.getMultiUserChat(testRoomAddress);
        final MultiUserChat roomTwo = mucManagerTwo.getMultiUserChat(testRoomAddress);
        final Resourcepart joinerNickname = Resourcepart.from("test-joiner");
        final FullJid joinerRoomAddress = JidCreate.fullFrom(testRoomAddress, joinerNickname);

        try {
            roomTwo.join(recipientNickname);
            roomOne.join(joinerNickname);

            final ResultSyncPoint<Presence, Exception> presenceReceived = new ResultSyncPoint<>();

            roomTwo.addParticipantListener(presence -> {
                if (presence.getFrom().equals(joinerRoomAddress) && MUCUser.from(presence).getItem().getRole().equals(MUCRole.participant)) {
                    presenceReceived.signal(presence);
                }
            });

            // Execute system under test.
            ownerRoom.grantVoice(joinerNickname);
            final Presence result = presenceReceived.waitForResult(timeout);

            // Verify result.
            assertion.apply(result);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "The <occupant-id> element MUST be attached to [...] every presence sent by a MUC.")
    public void testOccupantIdInReflectedRoleChangeToVisitor() throws Exception
    {
        fixtureForReflectedRoleChangeToVisitor(reflectedPresence -> {
            final OccupantId extension = reflectedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence sent back to user '" + conOne.getUser() + "' after voice was revoked in room '" + testRoomAddress + "' to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInReflectedRoleChangeToVisitor() throws Exception
    {
        fixtureForReflectedRoleChangeToVisitor(reflectedPresence -> {
            final List<OccupantId> extensions = reflectedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence sent back to user '" + conOne.getUser() + "' after voice was revoked in room '" + testRoomAddress + "' to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForReflectedRoleChangeToVisitor(final Function<Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom(true);
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChat room = mucManagerOne.getMultiUserChat(testRoomAddress);
        final Resourcepart nickname = Resourcepart.from("test-user");

        try {
            room.join(nickname);

            final ResultSyncPoint<Presence, Exception> becomesParticipant = new ResultSyncPoint<>();
            room.addParticipantListener(presence -> {
                if (presence.getFrom().equals(room.getMyRoomJid()) && MUCUser.from(presence).getItem().getRole().equals(MUCRole.participant)) {
                    becomesParticipant.signal(presence);
                }
            });
            ownerRoom.grantVoice(nickname);
            becomesParticipant.waitForResult(timeout);

            final ResultSyncPoint<Presence, Exception> becomesVisitor = new ResultSyncPoint<>();
            room.addParticipantListener(presence -> {
                if (presence.getFrom().equals(room.getMyRoomJid()) && MUCUser.from(presence).getItem().getRole().equals(MUCRole.visitor)) {
                    becomesVisitor.signal(presence);
                }
            });

            // Execute system under test.
            ownerRoom.revokeVoice(nickname);
            final Presence reflectedPresence = becomesVisitor.waitForResult(timeout);

            // Verify result.
            assertion.apply(reflectedPresence);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "The <occupant-id> element MUST be attached to [...] every presence sent by a MUC.")
    public void testOccupantIdInBroadcastRoleChangeToVisitor() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");

        fixtureForBroadcastRoleChangeToVisitor(recipientNickname, broadcastedPresence -> {
            final OccupantId extension = broadcastedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' voice was revoked in the room to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInBroadcastRoleChangeToVisitor() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");

        fixtureForBroadcastRoleChangeToVisitor(recipientNickname, broadcastedPresence -> {
            final List<OccupantId> extensions = broadcastedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' voice was revoked in the room to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForBroadcastRoleChangeToVisitor(final Resourcepart recipientNickname, final Function<Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom(true);
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChatManager mucManagerTwo = MultiUserChatManager.getInstanceFor(conTwo);
        final MultiUserChat roomOne = mucManagerOne.getMultiUserChat(testRoomAddress);
        final MultiUserChat roomTwo = mucManagerTwo.getMultiUserChat(testRoomAddress);
        final Resourcepart joinerNickname = Resourcepart.from("test-joiner");
        final FullJid joinerRoomAddress = JidCreate.fullFrom(testRoomAddress, joinerNickname);

        try {
            roomTwo.join(recipientNickname);
            roomOne.join(joinerNickname);

            final ResultSyncPoint<Presence, Exception> becomesParticipant = new ResultSyncPoint<>();

            roomTwo.addParticipantListener(presence -> {
                if (presence.getFrom().equals(joinerRoomAddress) && MUCUser.from(presence).getItem().getRole().equals(MUCRole.participant)) {
                    becomesParticipant.signal(presence);
                }
            });
            ownerRoom.grantVoice(joinerNickname);
            becomesParticipant.waitForResult(timeout);

            final ResultSyncPoint<Presence, Exception> becomesVisitor = new ResultSyncPoint<>();
            roomTwo.addParticipantListener(presence -> {
                if (presence.getFrom().equals(joinerRoomAddress) && MUCUser.from(presence).getItem().getRole().equals(MUCRole.visitor)) {
                    becomesVisitor.signal(presence);
                }
            });

            // Execute system under test.
            ownerRoom.revokeVoice(joinerNickname);
            final Presence result = becomesVisitor.waitForResult(timeout);

            // Verify result.
            assertion.apply(result);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "The <occupant-id> element MUST be attached to [...] every presence sent by a MUC.")
    public void testOccupantIdInReflectedRoleChangeToModerator() throws Exception
    {
        fixtureForReflectedRoleChangeToModerator(reflectedPresence -> {
            final OccupantId extension = reflectedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence sent back to user '" + conOne.getUser() + "' after they where granted moderator privileges in room '" + testRoomAddress + "' to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInReflectedRoleChangeToModerator() throws Exception
    {
        fixtureForReflectedRoleChangeToModerator(reflectedPresence -> {
            final List<OccupantId> extensions = reflectedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence sent back to user '" + conOne.getUser() + "' after they where granted moderator privileges in room '" + testRoomAddress + "' to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForReflectedRoleChangeToModerator(final Function<Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChat room = mucManagerOne.getMultiUserChat(testRoomAddress);
        final Resourcepart nickname = Resourcepart.from("test-user");

        try {
            room.join(nickname);
            final ResultSyncPoint<Presence, Exception> presenceReceived = new ResultSyncPoint<>();

            room.addParticipantListener(presence -> {
                if (presence.getFrom().equals(room.getMyRoomJid()) && MUCUser.from(presence).getItem().getRole().equals(MUCRole.moderator)) {
                    presenceReceived.signal(presence);
                }
            });

            // Execute system under test.
            ownerRoom.grantModerator(nickname);
            final Presence reflectedPresence = presenceReceived.waitForResult(timeout);

            // Verify result.
            assertion.apply(reflectedPresence);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "The <occupant-id> element MUST be attached to [...] every presence sent by a MUC.")
    public void testOccupantIdInBroadcastRoleChangeToModerator() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");
        fixtureForBroadcastRoleChangeToModerator(recipientNickname, broadcastedPresence -> {
            final OccupantId extension = broadcastedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' where granted moderator privileges in the room to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInBroadcastRoleChangeToModerator() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");
        fixtureForBroadcastRoleChangeToModerator(recipientNickname, broadcastedPresence -> {
            final List<OccupantId> extensions = broadcastedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' where granted moderator privileges in the room to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForBroadcastRoleChangeToModerator(final Resourcepart recipientNickname, final Function<Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChatManager mucManagerTwo = MultiUserChatManager.getInstanceFor(conTwo);
        final MultiUserChat roomOne = mucManagerOne.getMultiUserChat(testRoomAddress);
        final MultiUserChat roomTwo = mucManagerTwo.getMultiUserChat(testRoomAddress);
        final Resourcepart joinerNickname = Resourcepart.from("test-joiner");
        final FullJid joinerRoomAddress = JidCreate.fullFrom(testRoomAddress, joinerNickname);

        try {
            roomTwo.join(recipientNickname);
            roomOne.join(joinerNickname);

            final ResultSyncPoint<Presence, Exception> presenceReceived = new ResultSyncPoint<>();

            roomTwo.addParticipantListener(presence -> {
                if (presence.getFrom().equals(joinerRoomAddress) && MUCUser.from(presence).getItem().getRole().equals(MUCRole.moderator)) {
                    presenceReceived.signal(presence);
                }
            });

            // Execute system under test.
            ownerRoom.grantModerator(joinerNickname);
            final Presence result = presenceReceived.waitForResult(timeout);

            // Verify result.
            assertion.apply(result);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "The <occupant-id> element MUST be attached to [...] every presence sent by a MUC.")
    public void testOccupantIdInReflectedAffiliationChangeToOutcast() throws Exception
    {
        fixtureForReflectedAffiliationChangeToOutcast(reflectedPresence -> {
            final OccupantId extension = reflectedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence sent back to user '" + conOne.getUser() + "' after they where banned in room '" + testRoomAddress + "' to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInReflectedAffiliationChangeToOutcast() throws Exception
    {
        fixtureForReflectedAffiliationChangeToOutcast(reflectedPresence -> {
            final List<OccupantId> extensions = reflectedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence sent back to user '" + conOne.getUser() + "' after they where banned in room '" + testRoomAddress + "' to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForReflectedAffiliationChangeToOutcast(final Function<Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChat room = mucManagerOne.getMultiUserChat(testRoomAddress);
        final Resourcepart nickname = Resourcepart.from("test-user");
        final EntityFullJid roomJid;

        try {
            room.join(nickname);
            roomJid = room.getMyRoomJid();
            final ResultSyncPoint<Presence, Exception> presenceReceived = new ResultSyncPoint<>();

            conOne.addStanzaListener(stanza -> { // No longer a participant, cannot use room.participantListener
                final Presence presence = (Presence) stanza;
                if (presence.getFrom().equals(roomJid) && MUCUser.from(presence).getItem().getAffiliation() == MUCAffiliation.outcast) {
                    presenceReceived.signal(presence);
                }
            }, StanzaTypeFilter.PRESENCE);

            // Execute system under test.
            ownerRoom.banUser(conOne.getUser().asBareJid(), "banning for testing purposes");
            final Presence reflectedPresence = presenceReceived.waitForResult(timeout);

            // Verify result.
            assertion.apply(reflectedPresence);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "The <occupant-id> element MUST be attached to [...] every presence sent by a MUC.")
    public void testOccupantIdInBroadcastAffiliationChangeToOutcast() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");

        fixtureForBroadcastAffiliationChangeToOutcast(recipientNickname, broadcastedPresence -> {
            final OccupantId extension = broadcastedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' was banned in the room to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInBroadcastAffiliationChangeToOutcast() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");

        fixtureForBroadcastAffiliationChangeToOutcast(recipientNickname, broadcastedPresence -> {
            final List<OccupantId> extensions = broadcastedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' was banned in the room to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForBroadcastAffiliationChangeToOutcast(final Resourcepart recipientNickname, final Function<Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChatManager mucManagerTwo = MultiUserChatManager.getInstanceFor(conTwo);
        final MultiUserChat roomOne = mucManagerOne.getMultiUserChat(testRoomAddress);
        final MultiUserChat roomTwo = mucManagerTwo.getMultiUserChat(testRoomAddress);
        final Resourcepart joinerNickname = Resourcepart.from("test-joiner");
        final FullJid joinerRoomAddress = JidCreate.fullFrom(testRoomAddress, joinerNickname);

        try {
            roomTwo.join(recipientNickname);
            roomOne.join(joinerNickname);

            final ResultSyncPoint<Presence, Exception> presenceReceived = new ResultSyncPoint<>();

            conTwo.addStanzaListener(stanza -> { // No longer a participant, cannot use room.participantListener
                final Presence presence = (Presence) stanza;
                if (presence.getFrom().equals(joinerRoomAddress) && MUCUser.from(presence).getItem().getAffiliation() == MUCAffiliation.outcast) {
                    presenceReceived.signal(presence);
                }
            }, StanzaTypeFilter.PRESENCE);

            // Execute system under test.
            ownerRoom.banUser(conOne.getUser().asBareJid(), "banning for testing purposes");
            final Presence result = presenceReceived.waitForResult(timeout);

            // Verify result.
            assertion.apply(result);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "The <occupant-id> element MUST be attached to [...] every presence sent by a MUC.")
    public void testOccupantIdInReflectedAffiliationChangeToNone() throws Exception
    {
        fixtureForReflectedAffiliationChangeToNone(reflectedPresence -> {
            final OccupantId extension = reflectedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence sent back to user '" + conOne.getUser() + "' after they where revoked membership in room '" + testRoomAddress + "' to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInReflectedAffiliationChangeToNone() throws Exception
    {
        fixtureForReflectedAffiliationChangeToNone(reflectedPresence -> {
            final List<OccupantId> extensions = reflectedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence sent back to user '" + conOne.getUser() + "' after they where revoked membership in room '" + testRoomAddress + "' to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForReflectedAffiliationChangeToNone(final Function<Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChat room = mucManagerOne.getMultiUserChat(testRoomAddress);
        final Resourcepart nickname = Resourcepart.from("test-user");

        try {
            room.join(nickname);

            final ResultSyncPoint<Presence, Exception> seeMembership = new ResultSyncPoint<>();
            room.addParticipantListener(presence -> {
                if (presence.getFrom().equals(room.getMyRoomJid()) && MUCUser.from(presence).getItem().getAffiliation() == MUCAffiliation.member) {
                    seeMembership.signal(presence);
                }
            });
            ownerRoom.grantMembership(conOne.getUser().asBareJid());
            seeMembership.waitForResult(timeout);

            final ResultSyncPoint<Presence, Exception> seeRevoke = new ResultSyncPoint<>();
            room.addParticipantListener(presence -> {
                if (presence.getFrom().equals(room.getMyRoomJid()) && MUCUser.from(presence).getItem().getAffiliation() == MUCAffiliation.none) {
                    seeRevoke.signal(presence);
                }
            });

            // Execute system under test.
            ownerRoom.revokeMembership(conOne.getUser().asBareJid());
            final Presence reflectedPresence = seeRevoke.waitForResult(timeout);

            // Verify result.
            assertion.apply(reflectedPresence);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "The <occupant-id> element MUST be attached to [...] every presence sent by a MUC.")
    public void testOccupantIdInBroadcastAffiliationChangeToNone() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");

        fixtureForBroadcastAffiliationChangeToNone(recipientNickname, broadcastedPresence -> {
            final OccupantId extension = broadcastedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' where revoked membership in the room to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInBroadcastAffiliationChangeToNone() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");

        fixtureForBroadcastAffiliationChangeToNone(recipientNickname, broadcastedPresence -> {
            final List<OccupantId> extensions = broadcastedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' where revoked membership in the room to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForBroadcastAffiliationChangeToNone(final Resourcepart recipientNickname, final Function<Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChatManager mucManagerTwo = MultiUserChatManager.getInstanceFor(conTwo);
        final MultiUserChat roomOne = mucManagerOne.getMultiUserChat(testRoomAddress);
        final MultiUserChat roomTwo = mucManagerTwo.getMultiUserChat(testRoomAddress);
        final Resourcepart joinerNickname = Resourcepart.from("test-joiner");
        final FullJid joinerRoomAddress = JidCreate.fullFrom(testRoomAddress, joinerNickname);

        try {
            roomTwo.join(recipientNickname);
            roomOne.join(joinerNickname);

            final ResultSyncPoint<Presence, Exception> seeMembership = new ResultSyncPoint<>();
            roomTwo.addParticipantListener(presence -> {
                if (presence.getFrom().equals(joinerRoomAddress) && MUCUser.from(presence).getItem().getAffiliation() == MUCAffiliation.member) {
                    seeMembership.signal(presence);
                }
            });
            ownerRoom.grantMembership(conOne.getUser().asBareJid());
            seeMembership.waitForResult(timeout);

            final ResultSyncPoint<Presence, Exception> seeRevoke = new ResultSyncPoint<>();
            roomTwo.addParticipantListener(presence -> {
                if (presence.getFrom().equals(joinerRoomAddress) && MUCUser.from(presence).getItem().getAffiliation() == MUCAffiliation.none) {
                    seeRevoke.signal(presence);
                }
            });

            // Execute system under test.
            ownerRoom.revokeMembership(conOne.getUser().asBareJid());
            final Presence result = seeRevoke.waitForResult(timeout);

            // Verify result.
            assertion.apply(result);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "The <occupant-id> element MUST be attached to [...] every presence sent by a MUC.")
    public void testOccupantIdInReflectedAffiliationChangeToMember() throws Exception
    {
        fixtureForReflectedAffiliationChangeToMember(reflectedPresence -> {
            final OccupantId extension = reflectedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence sent back to user '" + conOne.getUser() + "' after they where granted membership in room '" + testRoomAddress + "' to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInReflectedAffiliationChangeToMember() throws Exception
    {
        fixtureForReflectedAffiliationChangeToMember(reflectedPresence -> {
            final List<OccupantId> extensions = reflectedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence sent back to user '" + conOne.getUser() + "' after they where granted membership in room '" + testRoomAddress + "' to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForReflectedAffiliationChangeToMember(final Function<Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChat room = mucManagerOne.getMultiUserChat(testRoomAddress);
        final Resourcepart nickname = Resourcepart.from("test-user");

        try {
            room.join(nickname);
            final ResultSyncPoint<Presence, Exception> presenceReceived = new ResultSyncPoint<>();

            room.addParticipantListener(presence -> {
                if (presence.getFrom().equals(room.getMyRoomJid()) && MUCUser.from(presence).getItem().getAffiliation() == MUCAffiliation.member) {
                    presenceReceived.signal(presence);
                }
            });

            // Execute system under test.
            ownerRoom.grantMembership(conOne.getUser().asBareJid());
            final Presence reflectedPresence = presenceReceived.waitForResult(timeout);

            // Verify result.
            assertion.apply(reflectedPresence);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "The <occupant-id> element MUST be attached to [...] every presence sent by a MUC.")
    public void testOccupantIdInBroadcastAffiliationChangeToMember() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");

        fixtureForBroadcastAffiliationChangeToMember(recipientNickname, broadcastedPresence -> {
            final OccupantId extension = broadcastedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' where granted membership in the room to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInBroadcastAffiliationChangeToMember() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");

        fixtureForBroadcastAffiliationChangeToMember(recipientNickname, broadcastedPresence -> {
            final List<OccupantId> extensions = broadcastedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' where granted membership in the room to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForBroadcastAffiliationChangeToMember(final Resourcepart recipientNickname, final Function<Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChatManager mucManagerTwo = MultiUserChatManager.getInstanceFor(conTwo);
        final MultiUserChat roomOne = mucManagerOne.getMultiUserChat(testRoomAddress);
        final MultiUserChat roomTwo = mucManagerTwo.getMultiUserChat(testRoomAddress);
        final Resourcepart joinerNickname = Resourcepart.from("test-joiner");
        final FullJid joinerRoomAddress = JidCreate.fullFrom(testRoomAddress, joinerNickname);

        try {
            roomTwo.join(recipientNickname);
            roomOne.join(joinerNickname);

            final ResultSyncPoint<Presence, Exception> presenceReceived = new ResultSyncPoint<>();

            roomTwo.addParticipantListener(presence -> {
                if (presence.getFrom().equals(joinerRoomAddress) && MUCUser.from(presence).getItem().getAffiliation() == MUCAffiliation.member) {
                    presenceReceived.signal(presence);
                }
            });

            // Execute system under test.
            ownerRoom.grantMembership(conOne.getUser().asBareJid());
            final Presence result = presenceReceived.waitForResult(timeout);

            // Verify result.
            assertion.apply(result);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "The <occupant-id> element MUST be attached to [...] every presence sent by a MUC.")
    public void testOccupantIdInReflectedAffiliationChangeToAdmin() throws Exception
    {
        fixtureForReflectedAffiliationChangeToAdmin(reflectedPresence -> {
            final OccupantId extension = reflectedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence sent back to user '" + conOne.getUser() + "' after they where granted admin in room '" + testRoomAddress + "' to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInReflectedAffiliationChangeToAdmin() throws Exception
    {
        fixtureForReflectedAffiliationChangeToAdmin(reflectedPresence -> {
            final List<OccupantId> extensions = reflectedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence sent back to user '" + conOne.getUser() + "' after they where granted admin in room '" + testRoomAddress + "' to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForReflectedAffiliationChangeToAdmin(final Function<Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChat room = mucManagerOne.getMultiUserChat(testRoomAddress);
        final Resourcepart nickname = Resourcepart.from("test-user");

        try {
            room.join(nickname);
            final ResultSyncPoint<Presence, Exception> presenceReceived = new ResultSyncPoint<>();

            room.addParticipantListener(presence -> {
                if (presence.getFrom().equals(room.getMyRoomJid()) && MUCUser.from(presence).getItem().getAffiliation() == MUCAffiliation.admin) {
                    presenceReceived.signal(presence);
                }
            });

            // Execute system under test.
            ownerRoom.grantAdmin(conOne.getUser().asBareJid());
            final Presence reflectedPresence = presenceReceived.waitForResult(timeout);

            // Verify result.
            assertion.apply(reflectedPresence);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "The <occupant-id> element MUST be attached to [...] every presence sent by a MUC.")
    public void testOccupantIdInBroadcastAffiliationChangeToAdmin() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");

        fixtureForBroadcastAffiliationChangeToAdmin(recipientNickname, broadcastedPresence -> {
            final OccupantId extension = broadcastedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' where granted admin in the room to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInBroadcastAffiliationChangeToAdmin() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");

        fixtureForBroadcastAffiliationChangeToAdmin(recipientNickname, broadcastedPresence -> {
            final List<OccupantId> extensions = broadcastedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' where granted admin in the room to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForBroadcastAffiliationChangeToAdmin(final Resourcepart recipientNickname, final Function<Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChatManager mucManagerTwo = MultiUserChatManager.getInstanceFor(conTwo);
        final MultiUserChat roomOne = mucManagerOne.getMultiUserChat(testRoomAddress);
        final MultiUserChat roomTwo = mucManagerTwo.getMultiUserChat(testRoomAddress);
        final Resourcepart joinerNickname = Resourcepart.from("test-joiner");
        final FullJid joinerRoomAddress = JidCreate.fullFrom(testRoomAddress, joinerNickname);

        try {
            roomTwo.join(recipientNickname);
            roomOne.join(joinerNickname);

            final ResultSyncPoint<Presence, Exception> presenceReceived = new ResultSyncPoint<>();

            roomTwo.addParticipantListener(presence -> {
                if (presence.getFrom().equals(joinerRoomAddress) && MUCUser.from(presence).getItem().getAffiliation() == MUCAffiliation.admin) {
                    presenceReceived.signal(presence);
                }
            });

            // Execute system under test.
            ownerRoom.grantAdmin(conOne.getUser().asBareJid());
            final Presence result = presenceReceived.waitForResult(timeout);

            // Verify result.
            assertion.apply(result);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "The <occupant-id> element MUST be attached to [...] every presence sent by a MUC.")
    public void testOccupantIdInReflectedAffiliationChangeToOwner() throws Exception
    {
        fixtureForReflectedAffiliationChangeToOwner(reflectedPresence -> {
            final OccupantId extension = reflectedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence sent back to user '" + conOne.getUser() + "' after they where granted ownership in room '" + testRoomAddress + "' to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInReflectedAffiliationChangeToOwner() throws Exception
    {
        fixtureForReflectedAffiliationChangeToOwner(reflectedPresence -> {
            final List<OccupantId> extensions = reflectedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence sent back to user '" + conOne.getUser() + "' after they where granted ownership in room '" + testRoomAddress + "' to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForReflectedAffiliationChangeToOwner(final Function<Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChat room = mucManagerOne.getMultiUserChat(testRoomAddress);
        final Resourcepart nickname = Resourcepart.from("test-user");

        try {
            room.join(nickname);
            final ResultSyncPoint<Presence, Exception> presenceReceived = new ResultSyncPoint<>();

            room.addParticipantListener(presence -> {
                if (presence.getFrom().equals(room.getMyRoomJid()) && MUCUser.from(presence).getItem().getAffiliation() == MUCAffiliation.owner) {
                    presenceReceived.signal(presence);
                }
            });

            // Execute system under test.
            ownerRoom.grantOwnership(conOne.getUser().asBareJid());
            final Presence reflectedPresence = presenceReceived.waitForResult(timeout);

            // Verify result.
            assertion.apply(reflectedPresence);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4", quote = "The <occupant-id> element MUST be attached to [...] every presence sent by a MUC.")
    public void testOccupantIdInBroadcastAffiliationChangeToOwner() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");

        fixtureForBroadcastAffiliationChangeToOwner(recipientNickname, broadcastedPresence -> {
            final OccupantId extension = broadcastedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' where granted ownership in the room to contain an occupant-id element (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInBroadcastAffiliationChangeToOwner() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");

        fixtureForBroadcastAffiliationChangeToOwner(recipientNickname, broadcastedPresence -> {
            final List<OccupantId> extensions = broadcastedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' where granted ownership in the room to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForBroadcastAffiliationChangeToOwner(final Resourcepart recipientNickname, final Function<Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChatManager mucManagerTwo = MultiUserChatManager.getInstanceFor(conTwo);
        final MultiUserChat roomOne = mucManagerOne.getMultiUserChat(testRoomAddress);
        final MultiUserChat roomTwo = mucManagerTwo.getMultiUserChat(testRoomAddress);
        final Resourcepart joinerNickname = Resourcepart.from("test-joiner");
        final FullJid joinerRoomAddress = JidCreate.fullFrom(testRoomAddress, joinerNickname);

        try {
            roomTwo.join(recipientNickname);
            roomOne.join(joinerNickname);

            final ResultSyncPoint<Presence, Exception> presenceReceived = new ResultSyncPoint<>();

            roomTwo.addParticipantListener(presence -> {
                if (presence.getFrom().equals(joinerRoomAddress) && MUCUser.from(presence).getItem().getAffiliation() == MUCAffiliation.owner) {
                    presenceReceived.signal(presence);
                }
            });

            // Execute system under test.
            ownerRoom.grantOwnership(conOne.getUser().asBareJid());
            final Presence result = presenceReceived.waitForResult(timeout);

            // Verify result.
            assertion.apply(result);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4.1", quote = "The occupant identifier MUST be generated such that it is stable. This means that if a user joins the same room a second time, the occupant identifier MUST be the same as was assigned the first time.")
    public void testOccupantIdInReflectedJoinPresenceAfterRejoin() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException, XmppStringprepException, MultiUserChatException.NotAMucServiceException, MultiUserChatException.MucNotJoinedException
    {
        fixtureForReflectedJoinPresenceAfterRejoin((firstReflectedPresence, secondReflectedPresence) -> {
            final OccupantId firstOccupantId = firstReflectedPresence.getExtension(OccupantId.class);
            final OccupantId secondOccupantId = secondReflectedPresence.getExtension(OccupantId.class);
            assertEquals(firstOccupantId.getId(), secondOccupantId.getId(), "Expected the presence sent back to user '" + conOne.getUser() + "' after they joined room '" + testRoomAddress + "' to contain the same occupant-id value when they re-join the same room (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInReflectedJoinPresenceAfterRejoin() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException, XmppStringprepException, MultiUserChatException.NotAMucServiceException, MultiUserChatException.MucNotJoinedException
    {
        fixtureForReflectedJoinPresenceAfterRejoin((firstReflectedPresence, secondReflectedPresence) -> {
            final List<OccupantId> extensions = secondReflectedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence sent back to user '" + conOne.getUser() + "' after they rejoined room '" + testRoomAddress + "' to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForReflectedJoinPresenceAfterRejoin(final BiFunction<Presence, Presence, Void> assertion) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException, XmppStringprepException, MultiUserChatException.NotAMucServiceException, MultiUserChatException.MucNotJoinedException
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChat room = mucManagerOne.getMultiUserChat(testRoomAddress);

        try {
            // Execute system under test.
            final Presence firstReflectedPresence = room.join(Resourcepart.from("test-user"));
            room.leave();

            Thread.sleep(100); // FIXME: Delete this line that's put in to work around a race condition in Smack.

            final Presence secondReflectedPresence = room.join(Resourcepart.from("test-user"));

            // Verify result.
            assertion.apply(firstReflectedPresence, secondReflectedPresence);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4.1", quote = "The occupant identifier MUST be generated such that it is stable. This means that if a user joins the same room a second time, the occupant identifier MUST be the same as was assigned the first time.")
    public void testOccupantIdInBroadcastJoinPresenceAfterRejoin() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");
        fixtureForBroadcastJoinPresenceAfterRejoin(recipientNickname, (firstBroadcastedPresence, secondBroadcastedPresence) -> {
            final OccupantId firstOccupantId = firstBroadcastedPresence.getExtension(OccupantId.class);
            final OccupantId secondOccupantId = secondBroadcastedPresence.getExtension(OccupantId.class);
            assertEquals(firstOccupantId.getId(), secondOccupantId.getId(), "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' joined the room to contain the same occupant-id value when the same user leaves and re-join the room (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInBroadcastJoinPresenceAfterRejoin() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");
        fixtureForBroadcastJoinPresenceAfterRejoin(recipientNickname, (firstBroadcastedPresence, secondBroadcastedPresence) -> {
            final List<OccupantId> extensions = secondBroadcastedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' rejoined the room to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForBroadcastJoinPresenceAfterRejoin(final Resourcepart recipientNickname, final BiFunction<Presence, Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChatManager mucManagerTwo = MultiUserChatManager.getInstanceFor(conTwo);
        final MultiUserChat roomOne = mucManagerOne.getMultiUserChat(testRoomAddress);
        final MultiUserChat roomTwo = mucManagerTwo.getMultiUserChat(testRoomAddress);
        final Resourcepart joinerNickname = Resourcepart.from("test-joiner");
        final FullJid joinerRoomAddress = JidCreate.fullFrom(testRoomAddress, joinerNickname);

        try {
            roomTwo.join(recipientNickname);

            final ResultSyncPoint<Presence, Exception> presenceReceived = new ResultSyncPoint<>();

            roomTwo.addParticipantListener(presence -> {
                if (presence.getFrom().equals(joinerRoomAddress) && presence.isAvailable()) {
                    presenceReceived.signal(presence);
                }
            });

            // Execute system under test.
            roomOne.join(joinerNickname);
            final Presence firstPresence = presenceReceived.waitForResult(timeout);
            roomOne.leave();
            roomOne.join(joinerNickname);
            final Presence secondPresence = presenceReceived.waitForResult(timeout);

            // Verify result.
            assertion.apply(firstPresence, secondPresence);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4.1", quote = "The occupant identifier MUST be generated such that it is stable. This means that if a user joins the same room a second time, the occupant identifier MUST be the same as was assigned the first time. A user in the sense of this specification is identified by its real bare JID. ")
    public void testOccupantIdInReflectedJoinPresenceAfterNicknameChangeAndRejoin() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException, XmppStringprepException, MultiUserChatException.NotAMucServiceException, MultiUserChatException.MucNotJoinedException
    {
        final Resourcepart firstNickname = Resourcepart.from("test-user-A");
        final Resourcepart secondNickname = Resourcepart.from("test-user-B");

        fixtureForReflectedJoinPresenceAfterNicknameChangeAndRejoin(firstNickname, secondNickname, (firstReflectedPresence, secondReflectedPresence) -> {
            final OccupantId firstOccupantId = firstReflectedPresence.getExtension(OccupantId.class);
            final OccupantId secondOccupantId = secondReflectedPresence.getExtension(OccupantId.class);
            assertEquals(firstOccupantId.getId(), secondOccupantId.getId(), "Expected the presence sent back to user '" + conOne.getUser() + "' after they joined room '" + testRoomAddress + "' (using nickname '" + firstNickname + "') to contain the same occupant-id value when they re-join the same room using a different nickname ('" + secondNickname + "') (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInReflectedJoinPresenceAfterNicknameChangeAndRejoin() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException, XmppStringprepException, MultiUserChatException.NotAMucServiceException, MultiUserChatException.MucNotJoinedException
    {
        final Resourcepart firstNickname = Resourcepart.from("test-user-A");
        final Resourcepart secondNickname = Resourcepart.from("test-user-B");

        fixtureForReflectedJoinPresenceAfterNicknameChangeAndRejoin(firstNickname, secondNickname, (firstReflectedPresence, secondReflectedPresence) -> {
            final List<OccupantId> extensions = secondReflectedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence sent back to user '" + conOne.getUser() + "' after they re joined room '" + testRoomAddress + "' (using nickname '" + secondNickname + "', having used '" + firstNickname + "' earlier) to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForReflectedJoinPresenceAfterNicknameChangeAndRejoin(final Resourcepart firstNickname, final Resourcepart secondNickname, final BiFunction<Presence, Presence, Void> assertion) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException, XmppStringprepException, MultiUserChatException.NotAMucServiceException, MultiUserChatException.MucNotJoinedException
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChat room = mucManagerOne.getMultiUserChat(testRoomAddress);

        try {
            // Execute system under test.
            final Presence firstReflectedPresence = room.join(firstNickname);
            room.leave();

            Thread.sleep(100); // FIXME: Delete this line that's put in to work around a race condition in Smack.

            final Presence secondReflectedPresence = room.join(secondNickname);

            // Verify result.
            assertion.apply(firstReflectedPresence, secondReflectedPresence);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4.1", quote = "The occupant identifier MUST be generated such that it is stable. This means that if a user joins the same room a second time, the occupant identifier MUST be the same as was assigned the first time. A user in the sense of this specification is identified by its real bare JID. ")
    public void testOccupantIdInBroadcastJoinPresenceAfterNicknameChangeAndRejoin() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");
        final Resourcepart joinerFirstNickname = Resourcepart.from("test-joiner-A");
        final Resourcepart joinerSecondNickname = Resourcepart.from("test-joiner-B");

        fixtureForBroadcastJoinPresenceAfterNicknameChangeAndRejoin(recipientNickname, joinerFirstNickname, joinerSecondNickname, (firstBroadcastedPresence, secondBroadcastedPresence) -> {
            final OccupantId firstOccupantId = firstBroadcastedPresence.getExtension(OccupantId.class);
            final OccupantId secondOccupantId = secondBroadcastedPresence.getExtension(OccupantId.class);
            assertEquals(firstOccupantId.getId(), secondOccupantId.getId(), "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' joined the room (using nickname '" + joinerFirstNickname + "') to contain the same occupant-id value when the same user leaves and re-join the room using a different nickname ('" + joinerSecondNickname + "') (but it did not).");
            return null;
        });
    }

    @SmackIntegrationTest(section = "4", quote = "Messages and presences MUST NOT contain more then one <occupant-id> element.")
    public void testSingleOccupantIdInBroadcastJoinPresenceAfterNicknameChangeAndRejoin() throws Exception
    {
        final Resourcepart recipientNickname = Resourcepart.from("test-recipient");
        final Resourcepart joinerFirstNickname = Resourcepart.from("test-joiner-A");
        final Resourcepart joinerSecondNickname = Resourcepart.from("test-joiner-B");

        fixtureForBroadcastJoinPresenceAfterNicknameChangeAndRejoin(recipientNickname, joinerFirstNickname, joinerSecondNickname, (firstBroadcastedPresence, secondBroadcastedPresence) -> {
            final List<OccupantId> extensions = secondBroadcastedPresence.getExtensions(OccupantId.class);
            assertFalse(extensions.size() > 1, "Expected the presence received by occupant '" + conTwo.getUser() + "' (using nickname '" + recipientNickname + "') in room '" + testRoomAddress + "' after user '" + conOne.getUser()+ "' rejoined the room (using nickname '" + joinerFirstNickname + "', having used '" + joinerSecondNickname + "' earlier) to contain not more than one occupant-id (but it contained " + extensions.size() + " occupant-id elements).");
            return null;
        });
    }

    void fixtureForBroadcastJoinPresenceAfterNicknameChangeAndRejoin(final Resourcepart recipientNickname, final Resourcepart joinerFirstNickname, final Resourcepart joinerSecondNickname, final BiFunction<Presence, Presence, Void> assertion) throws Exception
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChatManager mucManagerTwo = MultiUserChatManager.getInstanceFor(conTwo);
        final MultiUserChat roomOne = mucManagerOne.getMultiUserChat(testRoomAddress);
        final MultiUserChat roomTwo = mucManagerTwo.getMultiUserChat(testRoomAddress);
        final FullJid firstJoinerRoomAddress = JidCreate.fullFrom(testRoomAddress, joinerFirstNickname);
        final FullJid secondJoinerRoomAddress = JidCreate.fullFrom(testRoomAddress, joinerSecondNickname);

        try {
            roomTwo.join(recipientNickname);

            final ResultSyncPoint<Presence, Exception> firstPresenceReceived = new ResultSyncPoint<>();
            roomTwo.addParticipantListener(presence -> {
                if (presence.getFrom().equals(firstJoinerRoomAddress) && presence.isAvailable()) {
                    firstPresenceReceived.signal(presence);
                }
            });

            final ResultSyncPoint<Presence, Exception> secondPresenceReceived = new ResultSyncPoint<>();
            roomTwo.addParticipantListener(presence -> {
                if (presence.getFrom().equals(secondJoinerRoomAddress) && presence.isAvailable()) {
                    secondPresenceReceived.signal(presence);
                }
            });

            // Execute system under test.
            roomOne.join(joinerFirstNickname);
            final Presence firstPresence = firstPresenceReceived.waitForResult(timeout);
            roomOne.leave();
            roomOne.join(joinerSecondNickname);
            final Presence secondPresence = secondPresenceReceived.waitForResult(timeout);

            // Verify result.
            assertion.apply(firstPresence, secondPresence);
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4.1", quote = "The occupant identifier MUST be generated such that it is pseudonymous. This means that it MUST be sufficiently hard to determine the real bare JID of an occupant from its occupant identifier.")
    public void testOccupantIdDoesntContainUsername() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException, XmppStringprepException, MultiUserChatException.NotAMucServiceException, MultiUserChatException.MucNotJoinedException
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChat room = mucManagerOne.getMultiUserChat(testRoomAddress);

        try {
            // Execute system under test.
            final Presence reflectedPresence = room.join(Resourcepart.from("test-user"));

            // Verify result.
            final OccupantId extension = reflectedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence sent back to user '" + conOne.getUser() + "' after they joined room '" + testRoomAddress + "' to contain an occupant-id element (but it did not).");
            assertFalse(extension.getId().contains(conOne.getUser().getLocalpart().toString()), "Expected the occupant-id value ('" + extension.getId() + "') for user '" + conOne.getUser() + "' in room '" + testRoomAddress + "' to be psuedonymous. However, the local-part of the user's real JID is a literal part of the occupant-id.");
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }

    @SmackIntegrationTest(section = "4.1", quote = "The occupant identifier MUST have a maximum length of 128 characters.")
    public void testOccupantIdLength() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException, XmppStringprepException, MultiUserChatException.NotAMucServiceException, MultiUserChatException.MucNotJoinedException
    {
        // Setup test fixture.
        createRoom();
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChat room = mucManagerOne.getMultiUserChat(testRoomAddress);

        try {
            // Execute system under test.
            final Presence reflectedPresence = room.join(Resourcepart.from("test-user"));

            // Verify result.
            final OccupantId extension = reflectedPresence.getExtension(OccupantId.class);
            assertNotNull(extension, "Expected the presence sent back to user '" + conOne.getUser() + "' after they joined room '" + testRoomAddress + "' to contain an occupant-id element (but it did not).");
            assertTrue(extension.getId().length() <= 128, "Expected the occupant-id value for user '" + conOne.getUser() + "' in room '" + testRoomAddress + "' to have at most 128 characters. However, value consists of more: " + extension.getId().length() + ". Offending occupant-id: '" + extension.getId() + "'");
        } finally {
            // Tear down test fixture.
            removeRoom();
        }
    }
}
