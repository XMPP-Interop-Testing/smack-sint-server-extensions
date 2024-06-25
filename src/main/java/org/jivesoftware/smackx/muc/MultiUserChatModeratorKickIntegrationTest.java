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
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for section "8.2 Moderator Use Cases: Kicking an Occupant" of XEP-0045: "Multi-User Chat"
 *
 * @see <a href="https://xmpp.org/extensions/xep-0045.html#kick">XEP-0045 Section 8.2</a>
 */
@SpecificationReference(document = "XEP-0045", version = "1.34.6")
public class MultiUserChatModeratorKickIntegrationTest extends AbstractMultiUserChatIntegrationTest
{
    public MultiUserChatModeratorKickIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws SmackException.NoResponseException, XMPPException.XMPPErrorException,
        SmackException.NotConnectedException, InterruptedException, TestNotPossibleException
    {
        super(environment);
    }

    /**
     * Asserts that an occupant receives a presence type 'unavailable' after being kicked from a room.
     */
    @SmackIntegrationTest(section = "8.2", quote =
        "The service MUST remove the kicked occupant by sending a presence stanza of type \"unavailable\" to each " +
        "kicked occupant, including status code 307 in the extended presence information")
    public void mucTestOccupantInformedOfKick() throws Exception
    {
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-occupant-informed-of-kick");
        final MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);
        try {
            final SimpleResultSyncPoint oneSeesTwo = new SimpleResultSyncPoint();
            mucAsSeenByOne.addParticipantStatusListener(new ParticipantStatusListener()
            {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(JidCreate.entityFullFrom(mucAddress, nicknameTwo))) {
                        oneSeesTwo.signal();
                    }
                }
            });
            mucAsSeenByTwo.join(nicknameTwo);

            oneSeesTwo.waitForResult(timeout);

            final SimpleResultSyncPoint twoSeesKick = new SimpleResultSyncPoint();
            mucAsSeenByTwo.addUserStatusListener(new UserStatusListener() {
                @Override
                public void kicked(Jid actor, String reason) {
                    twoSeesKick.signal(); // Invoked by Smack when receiving an unavailable with a 307 status.
                }
            });
            mucAsSeenByOne.kickParticipant(nicknameTwo, "Integration test asserting occupant kick from MUC room.");

            assertResult(twoSeesKick, "Expected '" + conTwo.getUser() + "' to receive a presence 'unavailable' stanza with a 307 status code, after being kicked from room '" + mucAddress + "' by owner of the room '" + conOne.getUser() + "' (but no such presence stanza was received).");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a moderator is informed of success after successfully kicking an occupant from a room.
     */
    @SmackIntegrationTest(section = "8.2", quote =
        "After removing the kicked occupant(s), the service MUST then inform the moderator of success")
    public void mucTestModeratorInformedOfKickSuccess() throws Exception
    {
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-moderator-informed-of-kick-success");
        final MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);
        try {
            final SimpleResultSyncPoint oneSeesTwo = new SimpleResultSyncPoint();
            mucAsSeenByOne.addParticipantStatusListener(new ParticipantStatusListener()
            {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(JidCreate.entityFullFrom(mucAddress, nicknameTwo))) {
                        oneSeesTwo.signal();
                    }
                }
            });
            mucAsSeenByTwo.join(nicknameTwo);

            oneSeesTwo.waitForResult(timeout);

            final SimpleResultSyncPoint twoSeesKick = new SimpleResultSyncPoint();
            mucAsSeenByTwo.addUserStatusListener(new UserStatusListener() {
                @Override
                public void kicked(Jid actor, String reason) {
                    twoSeesKick.signal(); // Invoked by Smack when receiving an unavailable with a 307 status.
                }
            });
            assertDoesNotThrow(() -> mucAsSeenByOne.kickParticipant(nicknameTwo, "Integration test asserting occupant kick from MUC room."), // TODO Assert that specifically an XMPPException.XMPPErrorException is now thrown. All other exceptions should still make the test error out, but this test should assert explicitly that the moderator is allowed to kick a participant.
                "Expected '" + conOne.getUser() + "' that is owner of room '" + mucAddress + "' to receive a successful response after kicking '" + conTwo.getUser() + "' (that is using nickname '" + nicknameTwo + "') from the room (but no such response was received).");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that remaining occupants are informed of another occupant being kicked from a room.
     */
    @SmackIntegrationTest(section = "8.2", quote =
        "the service MUST then inform all of the remaining occupants that the kicked occupant is no longer in the " +
        "room by sending presence stanzas of type \"unavailable\" from the individual's roomnick " +
        "(<room@service/nick>) to all the remaining occupants [...] including the status code [...]")
    public void mucTestRemainingOccupantsInformedOfKick() throws Exception
    {
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-remaining-occupants-informed-of-kick-success");
        final MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);
        try {
            final SimpleResultSyncPoint oneSeesTwo = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint oneSeesThree = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint threeSeesTwo = new SimpleResultSyncPoint();
            mucAsSeenByOne.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(JidCreate.entityFullFrom(mucAddress, nicknameTwo))) {
                        oneSeesTwo.signal();
                    }
                    if (participant.equals(JidCreate.entityFullFrom(mucAddress, nicknameThree))) {
                        oneSeesThree.signal();
                    }
                }
            });

            mucAsSeenByThree.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(JidCreate.entityFullFrom(mucAddress, nicknameTwo))) {
                        threeSeesTwo.signal();
                    }
                }
            });

            mucAsSeenByTwo.join(nicknameTwo);
            mucAsSeenByThree.join(nicknameThree);

            oneSeesTwo.waitForResult(timeout);
            oneSeesThree.waitForResult(timeout);
            threeSeesTwo.waitForResult(timeout);

            final ResultSyncPoint<EntityFullJid, Exception> threeSeesKick = new ResultSyncPoint<>();
            mucAsSeenByThree.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void kicked(EntityFullJid participant, Jid actor, String reason) {
                    threeSeesKick.signal(participant); // Invoked by Smack when receiving an unavailable with a 307 status. Smack uses the stanza's 'from' attribute value as the participant value.
                }
            });

            mucAsSeenByOne.kickParticipant(nicknameTwo, "Integration test asserting occupant kick from MUC room.");

            final EntityFullJid kickedAddress = assertResult(threeSeesKick, "Expected occupant '" + conThree.getUser() + " (using nickname '" + nicknameThree + "') of room '" + mucAddress + "' to receive a presence 'unavailable' stanza with a 307 status code, after occupant '" + conTwo.getUser() + "' (using nickname '" + nicknameTwo + "') was kicked from the room (but no such presence stanza was received).");
            assertEquals(JidCreate.entityFullFrom(mucAddress, nicknameTwo), kickedAddress, "Expected the presence 'unavailable' stanza that was received by occupant '" + conThree.getUser() + " (using nickname '" + nicknameThree + "') of room '" + mucAddress + " to inform them of occupant '" + conTwo.getUser() + "' (using nickname '" + nicknameTwo + "') having been kicked from the room to have a 'from' address that matches the individual's roomnick addres (but it did not)");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a moderator (that is a member) cannot kick an admin
     */
    @SmackIntegrationTest(section = "8.2", quote =
        "A user cannot be kicked by a moderator with a lower affiliation. Therefore, if a moderator who is a member " +
        "attempts to kick an admin [...], the service MUST deny the request and return a <not-allowed/> error to the sender")
    public void mucTestModeratorMemberCannotKickAdmin() throws Exception
    {
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-moderator-member-cannot-kick-admin");
        final MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString); // Owner
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString); // Moderator that is a member
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString); // admin

        createMuc(mucAsSeenByOne, nicknameOne);
        try {
            final SimpleResultSyncPoint oneSeesTwo = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint oneSeesThree = new SimpleResultSyncPoint();
            mucAsSeenByOne.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(JidCreate.entityFullFrom(mucAddress, nicknameTwo))) {
                        oneSeesTwo.signal();
                    }
                    if (participant.equals(JidCreate.entityFullFrom(mucAddress, nicknameThree))) {
                        oneSeesThree.signal();
                    }
                }
            });

            mucAsSeenByTwo.join(nicknameTwo);
            final SimpleResultSyncPoint twoSeesThree = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint twoGetsModerator = new SimpleResultSyncPoint();
            mucAsSeenByTwo.addParticipantStatusListener(new ParticipantStatusListener()
            {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(JidCreate.entityFullFrom(mucAddress, nicknameThree))) {
                        twoSeesThree.signal();
                    }
                }
            });
            mucAsSeenByTwo.addUserStatusListener(new UserStatusListener() {
                @Override
                public void moderatorGranted() {
                    twoGetsModerator.signal();
                }
            });

            mucAsSeenByThree.join(nicknameThree);

            oneSeesTwo.waitForResult(timeout);
            oneSeesThree.waitForResult(timeout);

            // Make 'two' a member that is an admin
            mucAsSeenByOne.grantMembership(conTwo.getUser());
            mucAsSeenByOne.grantModerator(nicknameTwo);

            // Make 'three' an admin
            mucAsSeenByOne.grantAdmin(conThree.getUser());

            twoSeesThree.waitForResult(timeout);
            twoGetsModerator.waitForResult(timeout);

            // Execute System under test
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
                mucAsSeenByTwo.kickParticipant(nicknameThree, "Integration test moderator-member trying to kick admin");
            }, "Expected an error after '" + conTwo.getUser() + "' (that is a member and moderator, using nickname '" + nicknameTwo + "') tried to kick '" + conThree.getUser() + "' (that is an admin, using nickname '" + nicknameThree + "') from room '" + mucAddress + "' (but none occurred).");
            assertEquals(StanzaError.Condition.not_allowed, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' (that is a member and moderator, using nickname '" + nicknameTwo + "') after they tried to kick '" + conThree.getUser() + "' (that is an admin, using nickname '" + nicknameThree + "') from room '" + mucAddress + "'.");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a moderator (that is a member) cannot kick an owner.
     */
    @SmackIntegrationTest(section = "8.2", quote =
        "A user cannot be kicked by a moderator with a lower affiliation. Therefore, if [...] a moderator who is a " +
        "member [...] attempts to kick an owner [...], the service MUST deny the request and return a <not-allowed/> error to the sender")
    public void mucTestModeratorMemberCannotKickOwner() throws Exception
    {
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-moderator-member-cannot-kick-owner");
        final MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString); // Owner
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString); // Moderator that is a member

        createMuc(mucAsSeenByOne, nicknameOne);
        try {
            final SimpleResultSyncPoint oneSeesTwo = new SimpleResultSyncPoint();
            mucAsSeenByOne.addParticipantStatusListener(new ParticipantStatusListener()
            {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(JidCreate.entityFullFrom(mucAddress, nicknameTwo))) {
                        oneSeesTwo.signal();
                    }
                }
            });

            mucAsSeenByTwo.join(nicknameTwo);

            oneSeesTwo.waitForResult(timeout);

            // Make 'two' a member that is an admin
            final SimpleResultSyncPoint twoGetsModerator = new SimpleResultSyncPoint();
            mucAsSeenByTwo.addUserStatusListener(new UserStatusListener() {
                @Override
                public void moderatorGranted() {
                    twoGetsModerator.signal();
                }
            });

            mucAsSeenByOne.grantMembership(conTwo.getUser());
            mucAsSeenByOne.grantModerator(nicknameTwo);

            twoGetsModerator.waitForResult(timeout);

            // Execute System under test
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
                mucAsSeenByTwo.kickParticipant(nicknameOne, "Integration test moderator-member trying to kick owner");
            }, "Expected an error after '" + conTwo.getUser() + "' (that is a member and moderator, using nickname '" + nicknameTwo + "') tried to kick '" + conOne.getUser() + "' (that is an owner, using nickname '" + nicknameOne + "') from room '" + mucAddress + "' (but none occurred).");
            assertEquals(StanzaError.Condition.not_allowed, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' (that is a member and moderator, using nickname '" + nicknameTwo + "') after they tried to kick '" + conOne.getUser() + "' (that is an owner, using nickname '" + nicknameOne + "') from room '" + mucAddress + "'.");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a moderator (that is an admin) cannot kick an owner.
     */
    @SmackIntegrationTest(section = "8.2", quote =
        "A user cannot be kicked by a moderator with a lower affiliation. Therefore, if [...] a moderator who is a[n] " +
        "[...] admin attempts to kick an owner [...], the service MUST deny the request and return a <not-allowed/> error to the sender")
    public void mucTestModeratorAdminCannotKickOwner() throws Exception
    {
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-moderator-admin-cannot-kick-admin");
        final MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString); // Owner
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString); // Moderator that is an Admin

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

            oneSeesTwo.waitForResult(timeout);

            // Make 'two' a member that is an admin
            final SimpleResultSyncPoint twoGetsModerator = new SimpleResultSyncPoint();
            mucAsSeenByTwo.addUserStatusListener(new UserStatusListener() {
                @Override
                public void moderatorGranted() {
                    twoGetsModerator.signal();
                }
            });

            mucAsSeenByOne.grantAdmin(conTwo.getUser());
            mucAsSeenByOne.grantModerator(nicknameTwo);

            twoGetsModerator.waitForResult(timeout);

            // Execute System under test
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
                mucAsSeenByTwo.kickParticipant(nicknameOne, "Integration test moderator-admin trying to kick owner");
            }, "Expected an error after '" + conTwo.getUser() + "' (that is a admin and moderator, using nickname '" + nicknameTwo + "') tried to kick '" + conOne.getUser() + "' (that is an owner, using nickname '" + nicknameOne + "') from room '" + mucAddress + "' (but none occurred).");
            assertEquals(StanzaError.Condition.not_allowed, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' (that is an admin and moderator, using nickname '" + nicknameTwo + "') after they tried to kick '" + conOne.getUser() + "' (that is an owner, using nickname '" + nicknameOne + "') from room '" + mucAddress + "'.");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a non-moderator is <em>not</em> able to kick an occupant.
     */
    @SmackIntegrationTest(section = "8", quote =
        "moderators are stipulated to have privileges to perform the following action[s]: [...] kick a participant " +
        "or visitor from the room [...] requests MUST be denied if the <user@host> of the 'from' address of the " +
        "request does not match the bare JID portion of one of the moderators; in this case, the service MUST return " +
        "a <forbidden/> error.")
    public void mucTestParticipantNotAllowedToKick() throws Exception
    {
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-participant-kick");
        final MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);
        try {
            mucAsSeenByTwo.join(nicknameTwo);
            final SimpleResultSyncPoint twoSeesThree = new SimpleResultSyncPoint();
            mucAsSeenByTwo.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(JidCreate.entityFullFrom(mucAddress, nicknameThree))) {
                        twoSeesThree.signal();
                    }
                }
            });
            mucAsSeenByThree.join(nicknameThree);
            twoSeesThree.waitForResult(timeout);

            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
                mucAsSeenByTwo.kickParticipant(nicknameThree, "Integration test participant trying to kick another participant");
            }, "Expected an error after '" + conTwo.getUser() + "' (that is not a moderator) tried to kick another participant from room '" + mucAddress + "' (but none occurred).");
            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' after it tried to kick another participant from room '" + mucAddress + "' while not being a moderator.");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }
}
