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
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests for section "10 Owner Use Cases" of XEP-0045: "Multi-User Chat"
 *
 * @see <a href="https://xmpp.org/extensions/xep-0045.html#owner">XEP-0045 Section 10</a>
 */
@SpecificationReference(document = "XEP-0045", version = "1.34.6")
public class MultiUserChatOwnerIntegrationTest extends AbstractMultiUserChatIntegrationTest
{
    public MultiUserChatOwnerIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
        InterruptedException, TestNotPossibleException, MultiUserChatException.MucAlreadyJoinedException, MultiUserChatException.MissingMucCreationAcknowledgeException, MultiUserChatException.NotAMucServiceException, XmppStringprepException
    {
        super(environment);
    }

    /**
     * Verifies that a room has an owner, even after the original owner leaves the room.
     */
    @SmackIntegrationTest(section = "10", quote = "Every room MUST have at least one owner, and that owner (or a successor) is a long-lived attribute of the room for as long as the room exists (e.g., the owner does not lose ownership on exiting a persistent room).")
    public void testRoomHasOwnerAfterOriginalOwnerLeaves() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-after-leave");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameAdmin = Resourcepart.from("participant-" + randomString);

        final EntityFullJid ownerMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameOwner);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            mucAsSeenByParticipant.join(nicknameAdmin);

            final SimpleResultSyncPoint participantSeesOwnerLeave = new SimpleResultSyncPoint();
            mucAsSeenByParticipant.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void left(EntityFullJid participant) {
                    if (participant.equals(ownerMucAddress)) {
                        participantSeesOwnerLeave.signal();
                    }
                }
            });

            // Execute system under test.
            mucAsSeenByOwner.leave();
            final List<Affiliate> owners = mucAsSeenByParticipant.getOwners();

            // Verify result.
            assertFalse(owners.isEmpty(), "Room '" + mucAddress + "' unexpectedly has no owners, after the user that created the room ('" + conOne.getUser() + "') left.");
        } finally {
            // Tear down test fixture.
            mucAsSeenByOwner.join(nicknameOwner);
            tryDestroy(mucAsSeenByOwner);
        }
    }
}
