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
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.util.StringUtils;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for section "8.1 Moderator Use Cases: Modifying the Room Subject" of XEP-0045: "Multi-User Chat"
 *
 * @see <a href="https://xmpp.org/extensions/xep-0045.html#subject-mod">XEP-0045 Section 8.1</a>
 */
@SpecificationReference(document = "XEP-0045", version = "1.34.6")
public class MultiUserChatModeratorSubjectModIntegrationTest extends AbstractMultiUserChatIntegrationTest
{
    public MultiUserChatModeratorSubjectModIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
        InterruptedException, TestNotPossibleException, MultiUserChatException.MucAlreadyJoinedException, MultiUserChatException.MissingMucCreationAcknowledgeException, MultiUserChatException.NotAMucServiceException, XmppStringprepException
    {
        super(environment);
    }

    /**
     * Asserts that a moderator is able to change the room subject.
     */
    @SmackIntegrationTest(section = "8.1", quote =
        "moderators are stipulated to have privileges to [...] modify the subject")
    public void mucTestModeratorAllowedToChangeSubject() throws Exception
    {
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-moderator-change-subject");
        final MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);
        try {
            mucAsSeenByTwo.join(nicknameTwo);

            final SimpleResultSyncPoint twoGetsModerator = new SimpleResultSyncPoint();
            mucAsSeenByTwo.addUserStatusListener(new UserStatusListener() {
                @Override
                public void moderatorGranted() {
                    twoGetsModerator.signal();
                }
            });

            mucAsSeenByOne.grantModerator(nicknameTwo);
            twoGetsModerator.waitForResult(timeout);

            assertDoesNotThrow(() -> {
                // TODO Assert that a very specific exception, representing a <forbidden/> error, is not thrown. All other exceptions should still make the test error out, but this test should assert explicitly that the moderator is allowed to change the subject.
                mucAsSeenByTwo.changeSubject("Test Subject Change " +  StringUtils.insecureRandomString(6));
            }, "Expected '" + conTwo.getUser() + "' that is a moderator of room '" + mucAddress + "' to be able to change the room subject (but was not).");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a non-moderator is <em>not</em> able to change the room subject.
     */
    @SmackIntegrationTest(section = "8.1", quote =
        "If someone without appropriate privileges attempts to change the room subject, the service MUST return a " +
        "message of type \"error\" specifying a <forbidden/> error condition")
    public void mucTestParticipantNotAllowedToChangeSubject() throws Exception
    {
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-participant-change-subject");
        final MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);
        try {
            final MucConfigFormManager configFormManager = mucAsSeenByOne.getConfigFormManager();
            if (configFormManager.occupantsAreAllowedToChangeSubject()) {
                configFormManager.disallowOccupantsToChangeSubject().submitConfigurationForm();
            }
            mucAsSeenByTwo.join(nicknameTwo);

            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
                mucAsSeenByTwo.changeSubject("Test Subject Change " +  StringUtils.insecureRandomString(6));
            }, "Expected an error after '" + conTwo.getUser() + "' (that is not a moderator) tried to change the subject of room '" + mucAddress + "' (but none occurred).");
            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' after it tried to change to subject of room '" + mucAddress + "' while not being a moderator.");
        } catch (MultiUserChatException.MucConfigurationNotSupportedException e) {
            throw new TestNotPossibleException(e);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a subject change is reflected to all other occupants.
     */
    @SmackIntegrationTest(section = "8.1", quote =
        "The MUC service MUST reflect the [subject change] to all other occupants with a 'from' address equal to the room JID or to the occupant JID that corresponds to the sender of the subject change")
    public void mucTestChangeSubjectIsReflected() throws Exception
    {
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-change-subject-reflection");
        final MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);

        final String needle = "Test Subject Change " +  StringUtils.insecureRandomString(6);

        createMuc(mucAsSeenByOne, nicknameOne);
        try {
            final SimpleResultSyncPoint oneSeesTwo = new SimpleResultSyncPoint();
            mucAsSeenByOne.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(JidCreate.entityFullFrom(mucAddress, nicknameTwo))) {
                        oneSeesTwo.signal();
                    }
                }
            });
            mucAsSeenByTwo.join(nicknameTwo);

            // After 'one' sees 'two' join, perform the subject change (that we then can safely expect to end up with 'two').
            oneSeesTwo.waitForResult(timeout);

            final ResultSyncPoint<Jid, Exception> twoSeesSubjectChange = new ResultSyncPoint<>();
            mucAsSeenByTwo.addSubjectUpdatedListener((subject, from) -> {
                if (needle.equals(subject)) {
                    twoSeesSubjectChange.signal(from);
                }
            });

            mucAsSeenByOne.changeSubject(needle);

            final Jid from = assertResult(twoSeesSubjectChange, "Expected '" + conTwo.getUser() + "' to see the subject change by '" + conOne.getUser() + "' in room '" + mucAddress + "' (but did not)");
            final Set<Jid> validFroms = new HashSet<>();
            validFroms.add(null); // Smack will replace the rooms' (bare) JID with a null value.
            validFroms.add(mucAsSeenByOne.getMyRoomJid());
            assertTrue(validFroms.contains(from), "Expected the subject update received by '" + conTwo.getUser() + "' from MUC '" + mucAddress + "' after '" + conOne.getUser() + "' changed the subject, to have a 'from' value that is either the room JID ('" + mucAddress + "'), or the occupant JID the sender of the subject change ('" + mucAsSeenByOne.getMyRoomJid() + "'). Instead, the value was: '" + from + "'");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a moderator is able to remove the room subject.
     */
    @SmackIntegrationTest(section = "8.1", quote =
        "In order to remove the existing subject but not provide a new subject (i.e., set the subject to be empty), the client shall send an empty <subject/> element")
    public void mucTestModeratorAllowedToRemoveSubject() throws Exception
    {
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-remove-subject");
        final MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);
        try {
            mucAsSeenByOne.changeSubject("Initial subject to be removed " + randomString);

            final SimpleResultSyncPoint oneSeesTwo = new SimpleResultSyncPoint();
            mucAsSeenByOne.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(JidCreate.entityFullFrom(mucAddress, nicknameTwo))) {
                        oneSeesTwo.signal();
                    }
                }
            });
            mucAsSeenByTwo.join(nicknameTwo);

            // After 'one' sees 'two' join, perform the subject change (that we then can safely expect to end up with 'two').
            oneSeesTwo.waitForResult(timeout);

            final ResultSyncPoint<Jid, Exception> twoSeesSubjectChange = new ResultSyncPoint<>();
            mucAsSeenByTwo.addSubjectUpdatedListener((subject, from) -> {
                if (subject == null || subject.isEmpty()) {
                    twoSeesSubjectChange.signal(from);
                }
            });

            mucAsSeenByOne.changeSubject(""); // Setting an empty subject is how Smack removes a subject.

            final Jid from = assertResult(twoSeesSubjectChange, "Expected '" + conTwo.getUser() + "' to see the subject removal by '" + conOne.getUser() + "' in room '" + mucAddress + "' (but did not)");
            final Set<Jid> validFroms = new HashSet<>();
            validFroms.add(null); // Smack will replace the rooms' (bare) JID with a null value.
            validFroms.add(mucAsSeenByOne.getMyRoomJid());
            assertTrue(validFroms.contains(from), "Expected the subject update received by '" + conTwo.getUser() + "' from MUC '" + mucAddress + "' after '" + conOne.getUser() + "' removed the subject, to have a 'from' value that is either the room JID ('" + mucAddress + "'), or the occupant JID the sender of the subject removal ('" + mucAsSeenByOne.getMyRoomJid() + "'). Instead, the value was: '" + from + "'");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a non-moderator is <em>not</em> able to remove the room subject.
     */
    @SmackIntegrationTest(section = "8.1", quote =
        "If someone without appropriate privileges attempts to [remove] the room subject, the service MUST return a " +
            "message of type \"error\" specifying a <forbidden/> error condition")
    public void mucTestParticipantNotAllowedToRemoveSubject() throws Exception
    {
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-participant-change-subject");
        final MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);
        try {
            final MucConfigFormManager configFormManager = mucAsSeenByOne.getConfigFormManager();
            if (configFormManager.occupantsAreAllowedToChangeSubject()) {
                configFormManager.disallowOccupantsToChangeSubject().submitConfigurationForm();
            }
            mucAsSeenByOne.changeSubject("Initial subject to be removed " + randomString);
            mucAsSeenByTwo.join(nicknameTwo);

            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
                mucAsSeenByTwo.changeSubject(""); // Setting an empty subject is how Smack removes a subject.
            }, "Expected an error after '" + conTwo.getUser() + "' (that is not a moderator) tried to change the subject of room '" + mucAddress + "' (but none occurred).");
            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' after it tried to change to subject of room '" + mucAddress + "' while not being a moderator.");
        } catch (MultiUserChatException.MucConfigurationNotSupportedException e) {
            throw new TestNotPossibleException(e);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a subject removal is reflected to all other occupants.
     */
    @SmackIntegrationTest(section = "8.1", quote =
        "The MUC service MUST reflect the [subject removal] to all other occupants with a 'from' address equal to the room JID or to the occupant JID that corresponds to the sender of the subject change")
    public void mucTestRemoveSubjectIsReflected() throws Exception
    {
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-remove-subject-reflection");
        final MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);

        final String needle = ""; // Setting an empty subject is how Smack removes a subject.

        createMuc(mucAsSeenByOne, nicknameOne);
        try {
            final SimpleResultSyncPoint oneSeesTwo = new SimpleResultSyncPoint();
            mucAsSeenByOne.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(JidCreate.entityFullFrom(mucAddress, nicknameTwo))) {
                        oneSeesTwo.signal();
                    }
                }
            });
            mucAsSeenByTwo.join(nicknameTwo);

            // After 'one' sees 'two' join, perform the subject change (that we then can safely expect to end up with 'two').
            oneSeesTwo.waitForResult(timeout);

            final ResultSyncPoint<Jid, Exception> twoSeesSubjectChange = new ResultSyncPoint<>();
            mucAsSeenByTwo.addSubjectUpdatedListener((subject, from) -> {
                if (needle.equals(subject)) {
                    twoSeesSubjectChange.signal(from);
                }
            });

            mucAsSeenByOne.changeSubject(needle);

            final Jid from = assertResult(twoSeesSubjectChange, "Expected '" + conTwo.getUser() + "' to see the subject removal by '" + conOne.getUser() + "' in room '" + mucAddress + "' (but did not)");
            final Set<Jid> validFroms = new HashSet<>();
            validFroms.add(null); // Smack will replace the rooms' (bare) JID with a null value.
            validFroms.add(mucAsSeenByOne.getMyRoomJid());
            assertTrue(validFroms.contains(from), "Expected the subject update received by '" + conTwo.getUser() + "' from MUC '" + mucAddress + "' after '" + conOne.getUser() + "' removed the subject, to have a 'from' value that is either the room JID ('" + mucAddress + "'), or the occupant JID the sender of the subject change ('" + mucAsSeenByOne.getMyRoomJid() + "'). Instead, the value was: '" + from + "'");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }
}
