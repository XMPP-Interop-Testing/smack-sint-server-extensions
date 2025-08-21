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
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityJid;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for section "7.8 Inviting Another User to a Room" of XEP-0045: "Multi-User Chat"
 *
 * @see <a href="https://xmpp.org/extensions/xep-0045.html#invite">XEP-0045 Section 7.8</a>
 */
@SpecificationReference(document = "XEP-0045", version = "1.35.1")
public class MultiUserChatInvitationIntegrationTest extends AbstractMultiUserChatIntegrationTest
{
    public MultiUserChatInvitationIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
        InterruptedException, TestNotPossibleException, MultiUserChatException.MucAlreadyJoinedException, MultiUserChatException.MissingMucCreationAcknowledgeException, MultiUserChatException.NotAMucServiceException, XmppStringprepException
    {
        super(environment);
    }

    /**
     * Asserts that a MUC service delivers a mediated invitation sent by one user to the 'to' address specified in the
     * invitation.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "7.8.2", quote =
        "The <room@service> itself MUST [...] send the invitation to the invitee specified in the 'to' address;")
    public void mucTestMediatedInviteGetDelivered() throws Exception
    {
        final EntityBareJid mucAddress = getRandomRoom("mediated-invite-delivery");
        final MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        createMuc(mucAsSeenByOne, nicknameOne);

        try {
            final SimpleResultSyncPoint twoGetsInvited = new SimpleResultSyncPoint();
            mucManagerTwo.addInvitationListener((conn, room, inviter, reason, password, message, invitation) -> {
                if (room.getRoom().equals(mucAddress)) {
                    twoGetsInvited.signal();
                }
            });

            mucAsSeenByOne.invite(conTwo.getUser().asEntityBareJid(), null);

            assertResult(twoGetsInvited, "Expected '" + conTwo.getUser() + "' to receive a mediated invitation from MUC '" + mucAddress + "' after '" + conOne.getUser() + "' invited them (but no such invitation was received).");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a MUC invitation that is received has a valid 'from' value.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "7.8.2", quote =
        "The <room@service> itself MUST then add a 'from' address to the <invite/> element whose value is the bare JID, " +
        "full JID, or occupant JID of the inviter [...]")
    public void mucTestMediatedInviteFrom() throws Exception
    {
        final EntityBareJid mucAddress = getRandomRoom("mediated-invite-from");
        final MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        createMuc(mucAsSeenByOne, nicknameOne);

        try {
            final ResultSyncPoint<EntityJid, Exception> twoGetsInvited = new ResultSyncPoint<>();
            mucManagerTwo.addInvitationListener((conn, room, inviter, reason, password, message, invitation) -> {
                if (room.getRoom().equals(mucAddress)) {
                    twoGetsInvited.signal(inviter);
                }
            });

            mucAsSeenByOne.invite(conTwo.getUser().asEntityBareJid(), null);

            final EntityJid inviter = assertResult(twoGetsInvited, "Expected '" + conTwo.getUser() + "' to receive a mediated invitation from MUC '" + mucAddress + "' after '" + conOne.getUser() + "' invited them (but no such invitation was received).");
            final Set<EntityJid> validInviterAddresses = Set.of(conOne.getUser(), conOne.getUser().asEntityBareJid(), mucAsSeenByOne.getMyRoomJid());
            assertTrue(validInviterAddresses.contains(inviter), "Expected the mediated invitation received by '" + conTwo.getUser() + "' from MUC '" + mucAddress + "' after '" + conOne.getUser() + "' invited them to have a 'from' attribute value on the 'invite' element to match one of: [" + validInviterAddresses.stream().map(Object::toString).collect(Collectors.joining(", ")) + "] (but it did not). Instead, the value was: " + (inviter == null ? "(null)" : "'" + inviter + "'") );
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }
}
