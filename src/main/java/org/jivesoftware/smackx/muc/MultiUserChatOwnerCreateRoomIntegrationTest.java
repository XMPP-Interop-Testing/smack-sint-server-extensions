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
import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smackx.muc.packet.MUCInitialPresence;
import org.jivesoftware.smackx.muc.packet.MUCOwner;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.form.FillableForm;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.List;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for section "10.1 Owner Use Cases: Creating a Room" of XEP-0045: "Multi-User Chat"
 *
 * @see <a href="https://xmpp.org/extensions/xep-0045.html#createroom">XEP-0045 Section 10.1</a>
 */
@SpecificationReference(document = "XEP-0045", version = "1.35.1")
public class MultiUserChatOwnerCreateRoomIntegrationTest extends AbstractMultiUserChatIntegrationTest
{
    public MultiUserChatOwnerCreateRoomIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
        InterruptedException, TestNotPossibleException, MultiUserChatException.MucAlreadyJoinedException, MultiUserChatException.MissingMucCreationAcknowledgeException, MultiUserChatException.NotAMucServiceException, XmppStringprepException
    {
        super(environment);
    }

    /**
     * Verifies that a newly created room is locked.
     */
    @SmackIntegrationTest(section = "10.1.1", quote = "The user sends presence to <room@service/nick> [...] the service MUST create the room [...] not allow anyone else to enter the room (effectively \"locking\" the room)")
    public void testRoomLockedWhenCreated() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("owner-locked-when-created");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);

        final EntityFullJid ownerMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameOwner);

        final Presence createMucStanza = StanzaBuilder.buildPresence()
                .to(ownerMucAddress)
                .addExtension(new MUCInitialPresence(null, 0, 0, 0, null))
                .build();

        final StanzaFilter responseFilter = new AndFilter(StanzaTypeFilter.PRESENCE, FromMatchesFilter.createBare(ownerMucAddress));

        // Execute system under test.
        try (final StanzaCollector collector = conOne.createStanzaCollectorAndSend(responseFilter, createMucStanza)) {
            collector.nextResult();

            // Verify result.
            final XMPPException.XMPPErrorException xmppErrorException = assertThrows(XMPPException.XMPPErrorException.class, () -> mucAsSeenByParticipant.join(nicknameParticipant), "Expected an error to be returned to '" + conTwo.getUser() + "' when they tried to join room '" + mucAddress + "' that should currently be 'locked' (but no error was returned).");
            assertEquals(StanzaError.Condition.item_not_found, xmppErrorException.getStanzaError().getCondition(), "Unexpected condition in (expected) error that was returned to '" + conTwo.getUser() + "' when they tried to join room '" + mucAddress + "' that should currently be 'locked'.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a newly created room is responded to by an acknowledgement that the room has been created.
     */
    @SmackIntegrationTest(section = "10.1.1", quote = "The user sends presence to <room@service/nick> [...] The initial presence stanza received by the owner from the room MUST include extended presence information [...] acknowledging that the room has been created (via status code 201) and is awaiting configuration.")
    public void testRoomResponseIndicatesAcknowledgement() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("owner-response-indicates-acknowledgement");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final EntityFullJid ownerMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameOwner);

        final Presence createMucStanza = StanzaBuilder.buildPresence()
            .to(ownerMucAddress)
            .addExtension(new MUCInitialPresence(null, 0, 0, 0, null))
            .build();

        final StanzaFilter responseFilter = new AndFilter(StanzaTypeFilter.PRESENCE, FromMatchesFilter.createBare(ownerMucAddress));

        // Execute system under test.
        try (final StanzaCollector collector = conOne.createStanzaCollectorAndSend(responseFilter, createMucStanza)) {
            final Presence response = collector.nextResult();

            // Verify result.
            final MUCUser extension = response.getExtension(MUCUser.class);
            assertNotNull(extension, "Expected the initial presence returned by '" + mucAddress + "' to owner '" + conOne.getUser() + "' (MUC address '" + ownerMucAddress + "') to include extended presence information (but it does not).");
            assertTrue(extension.getStatus().contains(MUCUser.Status.ROOM_CREATED_201), "Expected the initial presence returned by '" + mucAddress + "' to owner '" + conOne.getUser() + "' (MUC address '" + ownerMucAddress + "') to acknowledge that the room has been created by including the '201' status code (but it does not).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a newly created room is owned by the requesting user.
     */
    @SmackIntegrationTest(section = "10.1.1", quote = "The user sends presence to <room@service/nick> [...] The initial presence stanza received by the owner from the room MUST include extended presence information indicating the user's status as an owner [...]")
    public void testRoomResponseIndicatesCreatorIsOwner() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("owner-response-indicates-creator-is-owner");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final EntityFullJid ownerMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameOwner);

        final Presence createMucStanza = StanzaBuilder.buildPresence()
            .to(ownerMucAddress)
            .addExtension(new MUCInitialPresence(null, 0, 0, 0, null))
            .build();

        final StanzaFilter responseFilter = new AndFilter(StanzaTypeFilter.PRESENCE, FromMatchesFilter.createBare(ownerMucAddress));

        // Execute system under test.
        try (final StanzaCollector collector = conOne.createStanzaCollectorAndSend(responseFilter, createMucStanza)) {
            final Presence response = collector.nextResult();

            // Verify result.
            final MUCUser extension = response.getExtension(MUCUser.class);
            assertNotNull(extension, "Expected the initial presence returned by '" + mucAddress + "' to owner '" + conOne.getUser() + "' (MUC address '" + ownerMucAddress + "') to include extended presence information (but it does not).");
            assertTrue(extension.getStatus().contains(MUCUser.Status.PRESENCE_TO_SELF_110), "Expected the initial presence returned by '" + mucAddress + "' to owner '" + conOne.getUser() + "' (MUC address '" + ownerMucAddress + "') to indicate the user's status as 'owner' by including the '110' status code (but it does not).");
            assertNotNull(extension.getItem(), "Expected the initial presence returned by '" + mucAddress + "' to owner '" + conOne.getUser() + "' (MUC address '" + ownerMucAddress + "') to include extended presence information that contains an item (but no item was found).");
            assertEquals(MUCAffiliation.owner, extension.getItem().getAffiliation(), "Expected the initial presence returned by '" + mucAddress + "' to owner '" + conOne.getUser() + "' (MUC address '" + ownerMucAddress + "') to indicate the user's status as 'owner' by including the 'owner' affiliation (but it does not).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a newly created room returns a configuration form response when requested.
     */
    @SmackIntegrationTest(section = "10.1.1", quote = "If the room owner requested a configuration form, the service MUST send an IQ result to the room owner")
    public void testRoomConfigurationFormRequest() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("owner-request-conf-form");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final EntityFullJid ownerMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameOwner);

        final Presence createMucStanza = StanzaBuilder.buildPresence()
            .to(ownerMucAddress)
            .addExtension(new MUCInitialPresence(null, 0, 0, 0, null))
            .build();

        final StanzaFilter responseFilter = new AndFilter(StanzaTypeFilter.PRESENCE, FromMatchesFilter.createBare(ownerMucAddress));

        final MUCOwner requestConfigurationFormStanza = new MUCOwner();
        requestConfigurationFormStanza.setTo(mucAddress);
        requestConfigurationFormStanza.setType(IQ.Type.get);

        try (final StanzaCollector collector = conOne.createStanzaCollectorAndSend(responseFilter, createMucStanza)) {
            collector.nextResult();

            // Execute system under test.
            final IQ response = conOne.sendIqRequestAndWaitForResponse(requestConfigurationFormStanza);

            // Verify result.
            assertTrue(response instanceof MUCOwner || response.hasExtension(MUCOwner.ELEMENT, MUCOwner.NAMESPACE), "Expected the response with stanza ID '" + response.getStanzaId() + "' from '" + mucAddress + "' to the request for a configuration form from '" + conOne.getUser() + "' (MUC address '" + ownerMucAddress + "') to be a stanza that includes a child element named '" + MUCOwner.ELEMENT + "' qualified by the namespace '" + MUCOwner.NAMESPACE + "' (but it does not).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a newly created room returns a configuration form with options, or no form at all, when requested.
     */
    @SmackIntegrationTest(section = "10.1.1", quote = "If the room owner requested a configuration form, the service MUST send an IQ result to the room owner containing a configuration form qualified by the 'jabber:x:data' namespace. If there are no configuration options available, the room MUST return an empty query element to the room owner.")
    public void testRoomConfigurationFormResponse() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("owner-request-conf-form-response");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final EntityFullJid ownerMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameOwner);

        final Presence createMucStanza = StanzaBuilder.buildPresence()
            .to(ownerMucAddress)
            .addExtension(new MUCInitialPresence(null, 0, 0, 0, null))
            .build();

        final StanzaFilter responseFilter = new AndFilter(StanzaTypeFilter.PRESENCE, FromMatchesFilter.createBare(ownerMucAddress));

        final MUCOwner requestConfigurationFormStanza = new MUCOwner();
        requestConfigurationFormStanza.setTo(mucAddress);
        requestConfigurationFormStanza.setType(IQ.Type.get);

        try (final StanzaCollector collector = conOne.createStanzaCollectorAndSend(responseFilter, createMucStanza)) {
            collector.nextResult();

            // Execute system under test.
            final IQ response = conOne.sendIqRequestAndWaitForResponse(requestConfigurationFormStanza);
            final DataForm form = response.getExtension(DataForm.class);

            // Verify result (a null form is a valid response!)
            boolean hasConfigurationOptions = false;
            if (form != null) {
                hasConfigurationOptions = form.getFields().stream().anyMatch(formField -> !List.of(FormField.Type.hidden, FormField.Type.fixed).contains(formField.getType()));
            }
            assertTrue(form == null || hasConfigurationOptions, "Expected the response with stanza ID '" + response.getStanzaId() + "' from '" + mucAddress + "' to the request for a configuration form from '" + conOne.getUser() + "' (MUC address '" + ownerMucAddress + "') to either contain no configuration form, or a form that includes one or more configuration options. The form that was returned has no configuration options.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that submitting a form unlocks a previously locked room.
     *
     * This test attempts to fill out a retrieved configuration form, and submit that. As the provided data is hard-coded,
     * the service might reject this. This test will throw TestNotPossible if the room creation fails, and asserts only
     * that once a configuration form was successfully submitted, another user can join the room (thus proving that it
     * is 'unlocked').
     * 
     * @see #testRoomRequestInitialConfigForm()
     */
    @SmackIntegrationTest(section = "10.1.1", quote = "Once the service receives the completed configuration form from the initial room owner [...], the service MUST \"unlock\" the room (i.e., allow other users to enter the room) and send an IQ of type \"result\" to the room owner.")
    public void testRoomConfigurationUnlocksRoom() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("owner-config-unlocks");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);

        final EntityFullJid ownerMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameOwner);

        final Presence createMucStanza = StanzaBuilder.buildPresence()
            .to(ownerMucAddress)
            .addExtension(new MUCInitialPresence(null, 0, 0, 0, null))
            .build();

        final StanzaFilter responseFilter = new AndFilter(StanzaTypeFilter.PRESENCE, FromMatchesFilter.createBare(ownerMucAddress));

        final MUCOwner requestConfigurationFormStanza = new MUCOwner();
        requestConfigurationFormStanza.setTo(mucAddress);
        requestConfigurationFormStanza.setType(IQ.Type.get);

        try (final StanzaCollector collector = conOne.createStanzaCollectorAndSend(responseFilter, createMucStanza)) {
            collector.nextResult();
            final IQ response = conOne.sendIqRequestAndWaitForResponse(requestConfigurationFormStanza);
            final DataForm form = response.getExtension(DataForm.class);
            if (form == null) {
                // If this happens, #testRoomRequestInitialConfigForm() will have failed.
                throw new TestNotPossibleException("MUC service does not offer configuration options.");
            }

            // Execute system under test.
            final DataForm dataFormToSubmit;
            final FillableForm fillableForm = new FillableForm(form);
            try {
                form.getFields().stream() // Attempt to formulate an answer.
                    .filter(FormField::isRequired)
                    .forEach(formField -> {
                        switch (formField.getType()) {
                            case bool:
                                fillableForm.setAnswer(formField.getFieldName(), false);
                                return;
                            case jid_multi:
                            case jid_single:
                                fillableForm.setAnswer(formField.getFieldName(), JidCreate.bareFromOrThrowUnchecked("test@example.org"));
                                return;

                            case text_single:
                            case text_multi:
                            case text_private:
                                fillableForm.setAnswer(formField.getFieldName(), "integration test");
                        }
                    });

                dataFormToSubmit = fillableForm.getDataFormToSubmit();
            } catch (Throwable t) {
                // This is a test implementation failure, not a failure of the system under test.
                LOGGER.log(Level.WARNING, "Test implementation failure: unable to provide answers to required configuration options", t);
                throw new TestNotPossibleException("Test implementation failure: unable to provide answers to required configuration options: " + t.getMessage());
            }

            final MUCOwner unlockRequest = new MUCOwner();
            unlockRequest.setTo(mucAddress);
            unlockRequest.setType(IQ.Type.set);
            unlockRequest.addExtension(dataFormToSubmit);

            try {
                conOne.sendIqRequestAndWaitForResponse(unlockRequest);
            } catch (XMPPException.XMPPErrorException e) {
                // This is (likely) a test implementation failure, not a failure of the system under test.
                LOGGER.log(Level.WARNING, "Test implementation failure: unable to provide answers acceptable to service to create MUC room", e);
                throw new TestNotPossibleException("Test implementation failure: unable to provide answers acceptable to service to create MUC room: " + e.getMessage());
            }

            // Verify result
            try {
                mucAsSeenByParticipant.join(nicknameParticipant);
            } catch (XMPPException.XMPPErrorException e) {
                if (StanzaError.Condition.item_not_found.equals(e.getStanzaError().getCondition())) {
                    fail("Expected '" + mucAddress + "' to be unlocked after '" + conOne.getUser() + "' (MUC address '" + ownerMucAddress + "') successfully submitted a configuration form, but '" + conTwo.getUser()+ "' (using nickname '" + nicknameParticipant + "') cannot join the room.");
                }
                throw e;
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that creating an instant room yields a 'result' IQ response.
     */
    @SmackIntegrationTest(section = "10.1.1", quote = "Once the service [...] or receives a request for an instant room [...] the service [...] send an IQ of type \"result\" to the room owner.")
    public void testRoomRequestInstantRoom() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("owner-request-instant");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);

        final EntityFullJid ownerMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameOwner);

        final Presence createMucStanza = StanzaBuilder.buildPresence()
            .to(ownerMucAddress)
            .addExtension(new MUCInitialPresence(null, 0, 0, 0, null))
            .build();

        final StanzaFilter responseFilter = new AndFilter(StanzaTypeFilter.PRESENCE, FromMatchesFilter.createBare(ownerMucAddress));

        final MUCOwner requestConfigurationFormStanza = new MUCOwner();
        requestConfigurationFormStanza.setTo(mucAddress);
        requestConfigurationFormStanza.setType(IQ.Type.get);

        try (final StanzaCollector collector = conOne.createStanzaCollectorAndSend(responseFilter, createMucStanza)) {
            collector.nextResult();

            // Execute system under test.
            final MUCOwner instantRoomRequest = new MUCOwner();
            instantRoomRequest.setTo(mucAddress);
            instantRoomRequest.setType(IQ.Type.set);
            instantRoomRequest.addExtension(DataForm.builder().build());

            IQ response = null;
            try {
                response = conOne.sendIqRequestAndWaitForResponse(instantRoomRequest);
            } catch (XMPPException.XMPPErrorException e) {
                // Verify result
                fail("Expected '" + mucAddress + "' to receive an IQ response of type 'result' after '" + conOne.getUser() + "' (MUC address '" + ownerMucAddress + "') requested an instant room to be created, but an error was returned instead: " + e.getStanzaError());
            }
            assertEquals(IQ.Type.result, response.getType(), "Expected '" + mucAddress + "' to receive an IQ response of type 'result' after '" + conOne.getUser() + "' (MUC address '" + ownerMucAddress + "') requested an instant room to be created (but another type was received).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that cancelling a room configuration destroys a room.
     */
    @SmackIntegrationTest(section = "10.1.1", quote = "Once the service receives the completed configuration form [...] If the service receives a cancellation, it MUST destroy the room.")
    public void testRoomConfigCancelDestroysRoom() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("owner-cancel-destroys");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final EntityFullJid ownerMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameOwner);

        final Presence createMucStanza = StanzaBuilder.buildPresence()
            .to(ownerMucAddress)
            .addExtension(new MUCInitialPresence(null, 0, 0, 0, null))
            .build();

        final StanzaFilter responseFilter = new AndFilter(StanzaTypeFilter.PRESENCE, FromMatchesFilter.createBare(ownerMucAddress));

        final MUCOwner requestConfigurationFormStanza = new MUCOwner();
        requestConfigurationFormStanza.setTo(mucAddress);
        requestConfigurationFormStanza.setType(IQ.Type.get);

        try (final StanzaCollector collector = conOne.createStanzaCollectorAndSend(responseFilter, createMucStanza)) {
            collector.nextResult();

            // Execute system under test.
            final MUCOwner cancelRequest = new MUCOwner();
            cancelRequest.setTo(mucAddress);
            cancelRequest.setType(IQ.Type.set);
            cancelRequest.addExtension(DataForm.builder(DataForm.Type.cancel).build());

            final SimpleResultSyncPoint ownerSeesDestruction = new SimpleResultSyncPoint();
            final StanzaListener destructionListener = stanza -> { if (MUCUser.from(stanza).getDestroy() != null) { ownerSeesDestruction.signal(); } };
            final StanzaFilter destructionFilter = new AndFilter(PresenceTypeFilter.UNAVAILABLE, FromMatchesFilter.create(ownerMucAddress), new ExtensionElementFilter<>(MUCUser.class));
            conOne.addAsyncStanzaListener(destructionListener, destructionFilter);

            conOne.sendStanza(cancelRequest);

            // Verify result
            assertResult(ownerSeesDestruction, "Expected '" + mucAddress + "' to be destroyed after '" + conOne.getUser() + "' (MUC address '" + ownerMucAddress + "') cancelled the configuration of the room, but no destruction stanza was received by the user.");
        } catch (Throwable t) { // Only tear down on a failure, as the MUC will otherwise already have been destroyed!
            // Tear down test fixture.
            try {
                tryDestroy(mucAsSeenByOwner);
            } catch (XMPPException.XMPPErrorException e) {
                LOGGER.warning("Error returned while destroying room that should already be destroyed in the course of the test: " + e.getStanzaError());
            }
            throw t;
        }
    }

    /**
     * Verifies that creating an instant room unlocks a previously locked room.
     *
     * @see #testRoomRequestInstantRoom()
     */
    @SmackIntegrationTest(section = "10.1.2", quote = "If the initial room owner wants to accept the default room configuration (i.e., create an \"instant room\") [...] by sending an IQ set [...] The service MUST then unlock the room and allow other entities to join it.")
    public void testRoomRequestInstantUnlocksRoom() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("owner-instant-unlocks");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);

        final EntityFullJid ownerMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameOwner);

        final Presence createMucStanza = StanzaBuilder.buildPresence()
            .to(ownerMucAddress)
            .addExtension(new MUCInitialPresence(null, 0, 0, 0, null))
            .build();

        final StanzaFilter responseFilter = new AndFilter(StanzaTypeFilter.PRESENCE, FromMatchesFilter.createBare(ownerMucAddress));

        final MUCOwner requestConfigurationFormStanza = new MUCOwner();
        requestConfigurationFormStanza.setTo(mucAddress);
        requestConfigurationFormStanza.setType(IQ.Type.get);

        try (final StanzaCollector collector = conOne.createStanzaCollectorAndSend(responseFilter, createMucStanza)) {
            collector.nextResult();

            final MUCOwner instantRoomRequest = new MUCOwner();
            instantRoomRequest.setTo(mucAddress);
            instantRoomRequest.setType(IQ.Type.set);
            instantRoomRequest.addExtension(DataForm.builder().build());

            try {
                conOne.sendIqRequestAndWaitForResponse(instantRoomRequest);
            } catch (XMPPException.XMPPErrorException e) {
                // If this happens, #testRoomRequestInstantRoom() will have failed.
                throw new TestNotPossibleException("Unable to create a room. Expected '" + mucAddress + "' to be created after '" + conOne.getUser() + "' (MUC address '" + ownerMucAddress + "') requested an instant room to be created, but an error was returned instead: " + e.getStanzaError());
            }

            // Execute system under test.
            try {
                mucAsSeenByParticipant.join(nicknameParticipant);
            } catch (XMPPException.XMPPErrorException e) {
                // Verify result
                if (StanzaError.Condition.item_not_found.equals(e.getStanzaError().getCondition())) {
                    fail("Expected '" + mucAddress + "' to be unlocked after '" + conOne.getUser() + "' (MUC address '" + ownerMucAddress + "') requested an instant room to be created, but '" + conTwo.getUser()+ "' (using nickname '" + nicknameParticipant + "') cannot join the room.");
                }
                throw e;
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that requesting an initial configuration form causes the service the return a (possibly empty) initial
     * room configuration form.
     */
    @SmackIntegrationTest(section = "10.1.3", quote = "If the initial room owner wants to create and configure a reserved room, the room owner MUST request an initial configuration form [...] the service MUST return an initial room configuration form to the user.")
    public void testRoomRequestInitialConfigForm() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("owner-get-config");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);

        final EntityFullJid ownerMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameOwner);

        final Presence createMucStanza = StanzaBuilder.buildPresence()
            .to(ownerMucAddress)
            .addExtension(new MUCInitialPresence(null, 0, 0, 0, null))
            .build();

        final StanzaFilter responseFilter = new AndFilter(StanzaTypeFilter.PRESENCE, FromMatchesFilter.createBare(ownerMucAddress));

        final MUCOwner requestConfigurationFormStanza = new MUCOwner();
        requestConfigurationFormStanza.setTo(mucAddress);
        requestConfigurationFormStanza.setType(IQ.Type.get);

        // Execute system under test.
        try (final StanzaCollector collector = conOne.createStanzaCollectorAndSend(responseFilter, createMucStanza)) {
            collector.nextResult();
            final IQ response = conOne.sendIqRequestAndWaitForResponse(requestConfigurationFormStanza);

            // Verify result
            final DataForm form = response.getExtension(DataForm.class);
            assertNotNull(form, "Expected '" + conOne.getUser() + "' (MUC address '" + ownerMucAddress + "') to receive a (possibly empty) initial configuration form after requesting one from '" + mucAddress + "' (but none was received).");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }
}
