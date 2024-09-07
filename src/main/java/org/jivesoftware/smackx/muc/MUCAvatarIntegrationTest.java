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
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jivesoftware.smackx.vcardtemp.provider.VCardProvider;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for XEP-0486: "MUC Avatars"
 *
 * Note that in version 0.1.0 of the XEP (the latest at the time of writing), much of the functionality is described as
 * being optional. This prevents us from including tests (that are strict in nature) in this implementation.
 *
 * @see <a href="https://xmpp.org/extensions/xep-0486.html">XEP-0486</a>
 */
@SpecificationReference(document = "XEP-0486", version = "0.1.0")
public class MUCAvatarIntegrationTest extends AbstractMultiUserChatIntegrationTest
{
    public static final String TINY_PNG_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAABQAAAAPCAYAAADkmO9VAAABEElEQVQ4y53TO0pDQRQG4I8YNI2PIEi2IG5BECsNxAVkDQpaWCkEraztBHcgYp1WwT4WFqIi+GgUCRirJBKbES7DvdckBw7MnP9xOPOYwA0W8Y1n48USNnEE9xiEfMQxloc0OcBtQn8HF4lCMks5ZqUMzXkBzQzRQo5hFtaEeXRSutVyDGsp/C+UC/jEaYpoI8cwDTtB+28zi5eo4weKKcIi3iPuG+ZiYhU/EbGeYliPOH2sZY2yG5GfMJXAJ/EQcXb+e1+HkaCRwBo5WG5so5cYqRqyH2o9bI36nVbDYQ/C7bXD+hUrY35RZbQSI7bSbnOYmAkjXqGLs5BdXGId06MYdnCNPVQS9Qr2A9ZJE/4C57hm5CFALooAAAAASUVORK5CYII=";

    public MUCAvatarIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
        InterruptedException, TestNotPossibleException, MultiUserChatException.MissingMucCreationAcknowledgeException, MultiUserChatException.NotAMucServiceException, XmppStringprepException, MultiUserChatException.MucAlreadyJoinedException
    {
        super(environment);

        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-mucavatar-support");
        final MultiUserChat muc = mucManagerOne.getMultiUserChat(mucAddress);
        createMuc(muc, Resourcepart.from("owner-" + randomString));
        try {
            if (!ServiceDiscoveryManager.getInstanceFor(conOne).discoverInfo(mucAddress).containsFeature("vcard-temp")) {
                throw new TestNotPossibleException("Rooms of the MUC service do not advertise the 'vcard-temp' feature.");
            }
        } finally {
            tryDestroy(muc);
        }
    }

    /**
     * Verifies that when an owner publishes a VCard, it is either accepted.
     */
    @SmackIntegrationTest(section = "3.2", quote = "an owner [...] must publish a vCard-temp containing the avatar’s data, using the protocol defined in vcard-temp (XEP-0054).")
    public void testPublishAvatar() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-mucavatar-publish");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            final VCard vCard = new VCard();
            vCard.setAvatar(TINY_PNG_BASE64, "image/png");
            vCard.setTo(mucAddress);
            vCard.setType(IQ.Type.set);
            vCard.setStanzaId();

            // Execute system under test.
            conOne.sendIqRequestAndWaitForResponse(vCard);

            // Verify result.
        } catch (XMPPException.XMPPErrorException e) {
            fail("Expected '" + conOne.getUser() + "' (an owner) to be able to publish an avatar (via vCard) for '" + mucAddress + "' (but the server returned an error).", e);
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * This creates a multi-user chat room (using {@link #createMuc(MultiUserChat, Resourcepart)}). It then sets an
     * avatar for the newly created MUC. If setting the avatar fails, a TestNotPossibleException is thrown. This allows
     * this method to be used to check for pre-conditions required by tests that verify MUC-avatar behavior.
     *
     * @throws TestNotPossibleException When an avatar could not be set for the newly created chat room.
     */
    public void createMucWithVcard(MultiUserChat muc, Resourcepart resourceName) throws TestNotPossibleException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, MultiUserChatException.MucAlreadyJoinedException, XMPPException.XMPPErrorException, MultiUserChatException.MissingMucCreationAcknowledgeException, MultiUserChatException.NotAMucServiceException
    {
        createMuc(muc, resourceName);

        try {
            final VCard vCard = new VCard();
            vCard.setAvatar(TINY_PNG_BASE64, "image/png");
            vCard.setTo(muc.getRoom());
            vCard.setType(IQ.Type.set);
            vCard.setStanzaId();

            conOne.sendIqRequestAndWaitForResponse(vCard);
        } catch (XMPPException.XMPPErrorException e) {
            throw new TestNotPossibleException("Unable to set an avatar for '" + muc.getRoom() + "'.", e);
        }
    }

    /**
     * Verifies that when an owner can unpublish a vcard.
     */
    @SmackIntegrationTest(section = "3.2", quote = "Setting an empty vCard unpublishes the avatar.")
    public void testUnpublishAvatar() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-mucavatar-unpublish");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);

        createMucWithVcard(mucAsSeenByOwner, nicknameOwner);
        try {
            final VCard vCard = new VCard();
            vCard.setTo(mucAddress);
            vCard.setType(IQ.Type.set);
            vCard.setStanzaId();

            // Execute system under test.
            conOne.sendIqRequestAndWaitForResponse(vCard);

            // Verify result.
            final VCard request = new VCard();
            request.setTo(mucAddress);
            request.setType(IQ.Type.get);
            request.setStanzaId();
            final VCard response = conOne.sendIqRequestAndWaitForResponse(request);
            assertNull(response.getAvatar());
        } catch (XMPPException.XMPPErrorException e) {
            fail("Expected '" + conOne.getUser() + "' (an owner) to be able to unpublish an avatar (via vCard) for '" + mucAddress + "' (but the server returned an error).", e);
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a user can retrieve the vcard.
     */
    @SmackIntegrationTest(section = "3.4", quote = "the client [...] can retrieve the room’s vCard-temp.")
    public void testRetrieveAvatar() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-mucavatar-unpublish");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);

        createMucWithVcard(mucAsSeenByOwner, nicknameOwner);
        try {
            final VCard request = new VCard();
            request.setTo(mucAddress);
            request.setType(IQ.Type.get);
            request.setStanzaId();

            // Execute system under test.
            final VCard response = conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result.
            assertEquals(TINY_PNG_BASE64, Base64.encodeToString(response.getAvatar()), "Expected the avatar for room '" + mucAddress + "' as requested and received by '" + conOne.getUser() + "' to be equal to the avatar that was set for the room (but it was not).");
        } catch (XMPPException.XMPPErrorException e) {
            fail("Expected '" + conOne.getUser() + "' (an owner) to be able to request a vCard for '" + mucAddress + "' (but the server returned an error).", e);
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that PNG is supported.
     */
    @SmackIntegrationTest(section = "4", quote = "An application MUST support the image/png media type")
    public void testPNGSupport() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-mucavatar-pngsupport");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);

        createMuc(mucAsSeenByOwner, nicknameOwner);
        try {
            final VCard vCard = new VCard();
            vCard.setAvatar(TINY_PNG_BASE64, "image/png");
            vCard.setTo(mucAddress);
            vCard.setType(IQ.Type.set);
            vCard.setStanzaId();

            // Execute system under test.
            conOne.sendIqRequestAndWaitForResponse(vCard);

            // Verify result
        } catch (XMPPException.XMPPErrorException e) {
            fail("Expected '" + conOne.getUser() + "' (an owner) to be able to publish a PNG avatar for '" + mucAddress + "' (but the server returned an error).", e);
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }
}
