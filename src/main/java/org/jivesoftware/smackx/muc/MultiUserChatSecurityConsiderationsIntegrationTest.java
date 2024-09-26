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
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jivesoftware.smackx.xdata.form.FillableForm;
import org.jivesoftware.smackx.xdata.form.Form;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for section 14 "Security Considerations" of XEP-0045: "Multi-User Chat"
 *
 * @see <a href="https://xmpp.org/extensions/xep-0045.html#security">XEP-0045 Section 10</a>
 */
@SpecificationReference(document = "XEP-0045", version = "1.35.1")
public class MultiUserChatSecurityConsiderationsIntegrationTest extends AbstractMultiUserChatIntegrationTest
{
    public MultiUserChatSecurityConsiderationsIntegrationTest(SmackIntegrationTestEnvironment environment) throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, TestNotPossibleException, MultiUserChatException.MucAlreadyJoinedException, MultiUserChatException.MissingMucCreationAcknowledgeException, MultiUserChatException.NotAMucServiceException, XmppStringprepException
    {
        super(environment);

        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-security-setup");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        try {
            createMuc(mucAsSeenByOwner, nicknameOwner);
        } catch (Exception e) {
            throw new TestNotPossibleException("The service at '" + mucAddress.asDomainBareJid() + "' does not allow for a room to be created." + e.getMessage());
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Checks that the room adds a status code with value 170 to the initial presence of users joining a room in which
     * public logging is enabled.
     */
    @SmackIntegrationTest(section = "14.3", quote = "A service MUST warn the user that the room is publicly logged by returning a status code of \"170\" with the user's initial presence")
    public void testPublicLoggingStatusOnInitialPresence() throws XmppStringprepException, MultiUserChatException.MucAlreadyJoinedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, MultiUserChatException.MissingMucCreationAcknowledgeException, SmackException.NoResponseException, InterruptedException, MultiUserChatException.NotAMucServiceException, TestNotPossibleException
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-security-initial-170");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);
        try {
            mucAsSeenByOwner.create(nicknameOwner).getConfigFormManager()
                .enablePublicLogging()
                .submitConfigurationForm();

            // Execute system under test
            final Presence initialPresence = mucAsSeenByParticipant.join(nicknameParticipant);

            // Verify result
            final MUCUser extension = MUCUser.from(initialPresence);
            assertTrue(extension.getStatus().stream().anyMatch(status -> status.getCode() == 170), "Expected the initial presence received by '" + conTwo.getUser()+ "' when it joined room '" + mucAddress + "' (as '" + nicknameParticipant + "') to include status code '170', as the room is configured to have public logging enabled (but no such status was received).");
        } catch (MultiUserChatException.MucConfigurationNotSupportedException e) {
            throw new TestNotPossibleException("Unable to configure a room with public logging enabled.", e);
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Checks that the room broadcasts a status code with value 170 when a room that initially had public logging
     * disabled is reconfigured to enable public logging.
     */
    @SmackIntegrationTest(section = "14.3", quote = "A client MUST also warn the user if the room's configuration is subsequently modified to allow room logging (which the client will discover when the room sends status code 170).")
    public void testPublicLoggingStatusOnConfigChange() throws XmppStringprepException, MultiUserChatException.MucAlreadyJoinedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, MultiUserChatException.MissingMucCreationAcknowledgeException, SmackException.NoResponseException, InterruptedException, MultiUserChatException.NotAMucServiceException, TestNotPossibleException, TimeoutException
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-security-reconfig-170");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);

        final SimpleResultSyncPoint detected170Status = new SimpleResultSyncPoint();
        final MessageListener listener = message -> {
            final MUCUser extension = MUCUser.from(message);
            if (extension != null && extension.getStatus().stream().anyMatch(status -> status.getCode() == 170)) {
                detected170Status.signal();
            }
        };
        try {
            mucAsSeenByOwner.create(nicknameOwner).getConfigFormManager()
                .disablPublicLogging()
                .submitConfigurationForm();

            mucAsSeenByParticipant.join(nicknameParticipant);
            mucAsSeenByParticipant.addMessageListener(listener);

            // Execute system under test.
            mucAsSeenByOwner.getConfigFormManager()
                .enablePublicLogging()
                .submitConfigurationForm();

            // Verify result
            assertResult(detected170Status, "Expected '" + conTwo.getUser()+ "' that is an occupant of room '" + mucAddress + "' (as '" + nicknameParticipant + "') to receive a notification with status code '170' after room owner '" + conOne.getUser() + "' (as '" + nicknameOwner + "') reconfigured the room to have public logging enabled (but no such status was received).");
        } catch (MultiUserChatException.MucConfigurationNotSupportedException e) {
            throw new TestNotPossibleException("Unable to configure a room with public logging enabled.", e);
        } finally {
            // Tear down test fixture.
            mucAsSeenByParticipant.removeMessageListener(listener);
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Checks that the room adds a status code with value 100 to the initial presence of users joining a room in which
     * real JIDs are exposed to all occupants in the room.
     */
    @SmackIntegrationTest(section = "14.5", quote = "If real JIDs are exposed to all occupants in the room, the service MUST warn the user by returning a status code of \"100\" with the user's initial presence")
    public void testAnonymityStatusOnInitialPresence() throws XmppStringprepException, MultiUserChatException.MucAlreadyJoinedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, MultiUserChatException.MissingMucCreationAcknowledgeException, SmackException.NoResponseException, InterruptedException, MultiUserChatException.NotAMucServiceException, TestNotPossibleException
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-security-initial-100");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);
        try {
            createNonAnonymousMuc(mucAsSeenByOwner, nicknameOwner);

            // Execute system under test
            final Presence initialPresence = mucAsSeenByParticipant.join(nicknameParticipant);

            // Verify result
            final MUCUser extension = MUCUser.from(initialPresence);
            assertTrue(extension.getStatus().stream().anyMatch(status -> status.getCode() == 100), "Expected the initial presence received by '" + conTwo.getUser()+ "' when it joined room '" + mucAddress + "' (as '" + nicknameParticipant + "') to include status code '100', as the room is configured to be non-anonymous (but no such status was received).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Checks that the room broadcasts a status code with value 100 when a room is reconfigured to expose real JIDs to
     * all occupants in the room.
     */
    @SmackIntegrationTest(section = "14.5", quote = "A client MUST also warn the user if the room's configuration is modified from semi-anonymous to non-anonymous (which the client will discover when the room sends status code 172).")
    public void testAnonymityStatusOnConfigChange() throws XmppStringprepException, MultiUserChatException.MucAlreadyJoinedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, MultiUserChatException.MissingMucCreationAcknowledgeException, SmackException.NoResponseException, InterruptedException, MultiUserChatException.NotAMucServiceException, TestNotPossibleException, TimeoutException
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-security-reconfig-172");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);

        final SimpleResultSyncPoint detected172Status = new SimpleResultSyncPoint();
        final MessageListener listener = message -> {
            final MUCUser extension = MUCUser.from(message);
            if (extension != null && extension.getStatus().stream().anyMatch(status -> status.getCode() == 172)) {
                detected172Status.signal();
            }
        };
        try {
            createSemiAnonymousMuc(mucAsSeenByOwner, nicknameOwner);

            mucAsSeenByParticipant.join(nicknameParticipant);
            mucAsSeenByParticipant.addMessageListener(listener);

            // Execute system under test.
            final Form configForm = mucAsSeenByOwner.getConfigurationForm();
            final FillableForm answerForm = configForm.getFillableForm();
            answerForm.setAnswer("muc#roomconfig_whois", "anyone");
            mucAsSeenByOwner.sendConfigurationForm(answerForm);

            // Verify result
            assertResult(detected172Status, "Expected '" + conTwo.getUser()+ "' that is an occupant of room '" + mucAddress + "' (as '" + nicknameParticipant + "') to receive a notification with status code '172' after room owner '" + conOne.getUser() + "' (as '" + nicknameOwner + "') reconfigured the room from semi-anonymous to non-anonymous (but no such status was received).");
        } finally {
            // Tear down test fixture.
            mucAsSeenByParticipant.removeMessageListener(listener);
            tryDestroy(mucAsSeenByOwner);
        }
    }
}
