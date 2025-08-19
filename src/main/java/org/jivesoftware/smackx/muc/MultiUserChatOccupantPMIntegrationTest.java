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
import org.jivesoftware.smack.ListenerHandle;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.StanzaIdFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jivesoftware.smackx.xdata.form.FillableForm;
import org.jivesoftware.smackx.xdata.form.Form;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for section "7.5 Occupant Use Cases: Sending a Private Message" of XEP-0045: "Multi-User Chat"
 *
 * @see <a href="https://xmpp.org/extensions/xep-0045.html#privatemessage">XEP-0045 Section 7.5</a>
 */
@SpecificationReference(document = "XEP-0045", version = "1.35.1")
public class MultiUserChatOccupantPMIntegrationTest extends AbstractMultiUserChatIntegrationTest
{
    public MultiUserChatOccupantPMIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
        InterruptedException, TestNotPossibleException, MultiUserChatException.MucAlreadyJoinedException, MultiUserChatException.MissingMucCreationAcknowledgeException, MultiUserChatException.NotAMucServiceException, XmppStringprepException
    {
        super(environment);
    }

    /**
     * Check if the MUC allows for private messages to be sent (allowpm != 'none'), attempting to reconfigure if that's
     * not the case. When reconfiguration is not possible, a TestNotPossibleException is thrown.
     *
     * If private messages are to be enabled, this code attempts to enable them for 'moderators', which is assumed to be
     * the most acceptable configuration for services (as it's the most restrictive, next to 'none').
     */
    static void configureAllowPM(final MultiUserChat muc) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        final Form configForm = muc.getConfigurationForm();
        if (configForm.hasField("muc#roomconfig_allowpm") && "none".equals(configForm.readFirstValue("muc#roomconfig_allowpm"))) {
            final FillableForm answerForm = configForm.getFillableForm();
            answerForm.setAnswer("muc#roomconfig_allowpm", "moderators");
            try {
                muc.sendConfigurationForm(answerForm);
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Service does not allow room to be configured to allow PMs to be sent.");
            }
        }
    }

    /**
     * Verifies that a private message can be sent and received.
     */
    @SmackIntegrationTest(section = "7.5", quote = "an occupant can send a \"private message\" to a selected occupant via the service by sending a message to the intended recipient's occupant JID. [...] The service is responsible for changing the 'from' address to the sender's occupant JID and delivering the message to the intended recipient's full JID.")
    public void testSendPM() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("occupant-pm");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);

        final EntityFullJid ownerMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameOwner);
        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            configureAllowPM(mucAsSeenByOwner);

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

            final ResultSyncPoint<Stanza, Exception> targetReceivedPrivateMessage = new ResultSyncPoint<>();
            try (final ListenerHandle ignored = conTwo.addStanzaListener(targetReceivedPrivateMessage::signal, new AndFilter(StanzaTypeFilter.MESSAGE, new StanzaIdFilter(randomString))))
            {
                // Execute system under test.
                final MessageBuilder pmBuilder = MessageBuilder.buildMessage(randomString)
                    .ofType(Message.Type.chat)
                    .addExtension(new MUCUser())
                    .to(targetMucAddress)
                    .setBody("A private message sent as part of an integration test.");

                conOne.sendStanza(pmBuilder.build());

                // Verify result.
                final Stanza receivedMessage = assertResult(targetReceivedPrivateMessage, "Expected '" + conTwo.getUser() + "' (using nickname '" + nicknameTarget + "') to receive the private message that was sent by '" + conOne.getUser() + "' (using nickname '" + nicknameOwner + "') in '" + mucAddress + "' (but the message was not received).");
                assertEquals(ownerMucAddress, receivedMessage.getFrom(), "Expected the 'from' address of the private message sent by '" + conOne.getUser() + "' (using nickname '" + nicknameOwner + "') to '" + conTwo.getUser() + "' (using nickname '" + nicknameTarget + "') in '" + mucAddress + "' to match the occupant JID of the sender (but it did not).");
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a private message of type 'groupchat' is rejected.
     */
    @SmackIntegrationTest(section = "7.5", quote = "If the sender attempts to send a private message of type \"groupchat\" to a particular occupant, the service MUST refuse to deliver the message (since the recipient's client would expect in-room messages to be of type \"groupchat\") and return a <bad-request/> error to the sender")
    public void testSendGroupchatPM() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("occupant-pm-groupchat");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);

        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            configureAllowPM(mucAsSeenByOwner);

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

            final ResultSyncPoint<Stanza, Exception> ownerSeesError = new ResultSyncPoint<>();
            try (final ListenerHandle ignored = conOne.addStanzaListener(ownerSeesError::signal, new AndFilter(new StanzaIdFilter(randomString), MessageTypeFilter.ERROR))) {

                // Execute system under test.
                final MessageBuilder pmBuilder = MessageBuilder.buildMessage(randomString)
                    .ofType(Message.Type.groupchat)
                    .addExtension(new MUCUser())
                    .to(targetMucAddress)
                    .setBody("A private message sent as part of an integration test.");

                conOne.sendStanza(pmBuilder.build());

                // Verify result.
                final Stanza error = assertResult(ownerSeesError, "Expected '" + conOne.getUser() + "' (using nickname '" + nicknameOwner + "') to receive an error after it tried to send a private message of type 'groupchat' to '" + conTwo.getUser() + "' (using nickname '" + nicknameTarget + "') in '" + mucAddress + "' (but no error was received).");
                assertEquals(StanzaError.Condition.bad_request, error.getError().getCondition(), "Unexpected error condition in the (expected) error that was received by '" + conOne.getUser() + "' (using nickname '" + nicknameOwner + "') after it tried to send a private message of type 'groupchat' to '" + conTwo.getUser() + "' (using nickname '" + nicknameTarget + "') in '" + mucAddress + "'.");
            }
       } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a private message sent to a non-existing occupant gets an error in return.
     */
    @SmackIntegrationTest(section = "7.5", quote = "If the sender attempts to send a private message to an occupant JID that does not exist, the service MUST return an <item-not-found/> error to the sender.")
    public void testSendNonOccupantTargetPM() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("occupant-pm-nonoccupant-target");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("doesnotexist-" + randomString);

        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            configureAllowPM(mucAsSeenByOwner);

            final ResultSyncPoint<Stanza, Exception> ownerSeesError = new ResultSyncPoint<>();
            try (final ListenerHandle ignored = conOne.addStanzaListener(ownerSeesError::signal, new AndFilter(new StanzaIdFilter(randomString), MessageTypeFilter.ERROR)))
            {
                // Execute system under test.
                final MessageBuilder pmBuilder = MessageBuilder.buildMessage(randomString)
                    .ofType(Message.Type.chat)
                    .addExtension(new MUCUser())
                    .to(targetMucAddress)
                    .setBody("A private message sent as part of an integration test.");

                conOne.sendStanza(pmBuilder.build());

                // Verify result.
                final Stanza error = assertResult(ownerSeesError, "Expected '" + conOne.getUser() + "' (using nickname '" + nicknameOwner + "') to receive an error after it tried to send a private message to '" + nicknameTarget + "' in '" + mucAddress + "' that is not an occupant (but no error was received).");
                assertEquals(StanzaError.Condition.item_not_found, error.getError().getCondition(), "Unexpected error condition in the (expected) error that was received by '" + conOne.getUser() + "' (using nickname '" + nicknameOwner + "') after it tried to send a private message to '" + nicknameTarget + "' in '" + mucAddress + "' that is not an occupant.");
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a private message sent by a non-occupant is rejected.
     */
    @SmackIntegrationTest(section = "7.5", quote = "If the sender is not an occupant of the room in which the intended recipient is visiting, the service MUST return a <not-acceptable/> error to the sender.")
    public void testSendNonOccupantSenderPM() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("occupant-pm-nonoccupant-sender");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);

        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            configureAllowPM(mucAsSeenByOwner);

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

            mucAsSeenByOwner.leave();

            final ResultSyncPoint<Stanza, Exception> ownerSeesError = new ResultSyncPoint<>();
            try (final ListenerHandle ignored = conOne.addStanzaListener(ownerSeesError::signal, new AndFilter(new StanzaIdFilter(randomString), MessageTypeFilter.ERROR)))
            {
                // Execute system under test.
                final MessageBuilder pmBuilder = MessageBuilder.buildMessage(randomString)
                    .ofType(Message.Type.chat)
                    .addExtension(new MUCUser())
                    .to(targetMucAddress)
                    .setBody("A private message sent as part of an integration test.");

                conOne.sendStanza(pmBuilder.build());

                // Verify result.
                final Stanza error = assertResult(ownerSeesError, "Expected '" + conOne.getUser() + "' (that currently is not an occupant) to receive an error after it tried to send a private message to '" + conTwo.getUser() + "' (using nickname '" + nicknameTarget + "') in '" + mucAddress + "' (but no error was received).");
                assertEquals(StanzaError.Condition.not_acceptable, error.getError().getCondition(), "Unexpected error condition in the (expected) error that was received by '" + conOne.getUser() + "' (that currently is not an occupant) after it tried to send a private message to '" + conTwo.getUser() + "' (using nickname '" + nicknameTarget + "') in '" + mucAddress + "'.");
            }
        } finally {
            // Tear down test fixture.
            mucAsSeenByOwner.join(nicknameOwner);
            tryDestroy(mucAsSeenByOwner);
        }
    }
}
