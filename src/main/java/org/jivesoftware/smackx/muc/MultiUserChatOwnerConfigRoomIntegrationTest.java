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
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smackx.muc.filter.MUCUserStatusCodeFilter;
import org.jivesoftware.smackx.muc.packet.MUCOwner;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.ListSingleFormField;
import org.jivesoftware.smackx.xdata.form.FillableForm;
import org.jivesoftware.smackx.xdata.form.Form;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for section "10.3 Owner Use Cases: Subsequent Room Configuration" of XEP-0045: "Multi-User Chat"
 *
 * @see <a href="https://xmpp.org/extensions/xep-0045.html#roomconfig">XEP-0045 Section 10.2</a>
 */
@SpecificationReference(document = "XEP-0045", version = "1.34.6")
public class MultiUserChatOwnerConfigRoomIntegrationTest extends AbstractMultiUserChatIntegrationTest
{
    public MultiUserChatOwnerConfigRoomIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
        InterruptedException, TestNotPossibleException, MultiUserChatException.MucAlreadyJoinedException, MultiUserChatException.MissingMucCreationAcknowledgeException, MultiUserChatException.NotAMucServiceException, XmppStringprepException
    {
        super(environment);

        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-config-setup");
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
     * Verifies that a user (without role or affiliation) different from the owner cannot request a room configuration form.
     */
    @SmackIntegrationTest(section = "10.2", quote = "a room owner requests a new configuration form [...] If the <user@host> of the 'from' address does not match the bare JID of a room owner, the service MUST return a <forbidden/> error to the sender")
    public void testObtainRoomConfigWithoutRoleOrAffiliation() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-config-get-no-role-or-affiliation");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        try {
            createMuc(mucAsSeenByOwner, nicknameOwner);

            final MUCOwner requestConfigurationFormStanza = new MUCOwner();
            requestConfigurationFormStanza.setTo(mucAddress);
            requestConfigurationFormStanza.setType(IQ.Type.get);

            // Execute system under test & Verify result.
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
                conTwo.sendIqRequestAndWaitForResponse(requestConfigurationFormStanza);
            }, "Expected an error after '" + conTwo.getUser() + "' (that has no role or affiliation) tried to get a room configuration form for room '" + mucAddress + "' (but none occurred).");
            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' after it tried to get a room configuration form for room '" + mucAddress + "' while not having a role or affiliation for that room.");
       } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a user (that is a participant) different from the owner cannot request a room configuration form.
     */
    @SmackIntegrationTest(section = "10.2", quote = "a room owner requests a new configuration form [...] If the <user@host> of the 'from' address does not match the bare JID of a room owner, the service MUST return a <forbidden/> error to the sender")
    public void testObtainRoomConfigNonAffiliatedUser() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-config-get-by-participant");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);

        try {
            createMuc(mucAsSeenByOwner, nicknameOwner);
            mucManagerTwo.getMultiUserChat(mucAddress).join(nicknameParticipant);

            final MUCOwner requestConfigurationFormStanza = new MUCOwner();
            requestConfigurationFormStanza.setTo(mucAddress);
            requestConfigurationFormStanza.setType(IQ.Type.get);

            // Execute system under test & Verify result.
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
                conTwo.sendIqRequestAndWaitForResponse(requestConfigurationFormStanza);
            }, "Expected an error after '" + conTwo.getUser() + "' (that is a participant but no owner) tried to get a room configuration form for room '" + mucAddress + "' (but none occurred).");
            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' after it tried to get a room configuration form for room '" + mucAddress + "' while having joined the room, but without being an owner.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that an owner (that is not joined in the room) can request a room configuration form.
     *
     * This test implementation makes a second user join the room, to prevent the service from destroying the room after
     * its last user left.
     */
    @SmackIntegrationTest(section = "10.2", quote = "a room owner requests a new configuration form [...] the service MUST send a configuration form to the room owner ")
    public void testObtainRoomConfigOwnerNonJoined() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-config-get-by-owner-not-joined");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);

        try {
            createMuc(mucAsSeenByOwner, nicknameOwner);

            // Make a second user join before the owner leaves the room. This prevents the service from reaping empty rooms during the test.
            mucAsSeenByParticipant.join(nicknameParticipant);
            mucAsSeenByOwner.leave();

            final MUCOwner requestConfigurationFormStanza = new MUCOwner();
            requestConfigurationFormStanza.setTo(mucAddress);
            requestConfigurationFormStanza.setType(IQ.Type.get);

            try {
                // Execute system under test.
                conOne.sendIqRequestAndWaitForResponse(requestConfigurationFormStanza);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to request a configuration form for '" + mucAddress + "' without being in the room (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            if (!mucAsSeenByOwner.isJoined()) {
                mucAsSeenByOwner.join(nicknameOwner);
            }
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that an owner (that is joined in the room) can request a room configuration form.
     */
    @SmackIntegrationTest(section = "10.2", quote = "a room owner requests a new configuration form [...] the service MUST send a configuration form to the room owner")
    public void testObtainRoomConfigOwnerJoined() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-config-get-by-owner-joined");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);

        try {
            createMuc(mucAsSeenByOwner, nicknameOwner);

            final MUCOwner requestConfigurationFormStanza = new MUCOwner();
            requestConfigurationFormStanza.setTo(mucAddress);
            requestConfigurationFormStanza.setType(IQ.Type.get);

            try {
                // Execute system under test.
                conOne.sendIqRequestAndWaitForResponse(requestConfigurationFormStanza);

                // Verify result.
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to request a configuration form for '" + mucAddress + "' while being in the room (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a room configuration form contains the current options set as defaults.
     *
     * The implementation attempts to create a room with a configuration that matches any non-default option for
     * list-single fields.
     */
    @SmackIntegrationTest(section = "10.2", quote = "a room owner requests a new configuration form [...] the service MUST send a configuration form to the room owner with the current options set as defaults")
    public void testObtainRoomConfigHasCurrentOptionsAsDefaults() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-config-defaults");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);

        final Map<String, FormField.Value> optionValues = new HashMap<>();
        try {
            mucAsSeenByOwner.create(nicknameOwner);
            final Form configForm = mucAsSeenByOwner.getConfigurationForm();
            final FillableForm answerForm = configForm.getFillableForm();

            for (final FormField field : configForm.getDataForm().getFields()) {
                // Set any non-default value for list-single fields
                if (field instanceof ListSingleFormField) {
                    final List<FormField.Option> options = ((ListSingleFormField) field).getOptions();
                    final FormField.Value defaultValue = ((ListSingleFormField) field).getRawValue();
                    if (!options.isEmpty()) {
                        for (final FormField.Option option : options) {
                            if (!option.getValue().equals(defaultValue)) {
                                optionValues.put(field.getFieldName(), option.getValue());
                                answerForm.setAnswer(field.getFieldName(), option.getValue().getValue());
                                break;
                            }
                        }
                    }
                }
            }

            if (optionValues.isEmpty()) {
                throw new TestNotPossibleException("Room configuration form does not contain any options that are usable by this test.");
            }

            try {
                mucAsSeenByOwner.sendConfigurationForm(answerForm);
            } catch (XMPPException.XMPPErrorException e) {
                throw new TestNotPossibleException("Unable to create room with a semi-random pick of options for list-single fields.");
            }

            // Execute system under test.
            final MUCOwner requestConfigurationFormStanza = new MUCOwner();
            requestConfigurationFormStanza.setTo(mucAddress);
            requestConfigurationFormStanza.setType(IQ.Type.get);

            try {
                final IQ response = conOne.sendIqRequestAndWaitForResponse(requestConfigurationFormStanza);

                // Verify result.
                final DataForm configurationForm = DataForm.from(response);
                for (final Map.Entry<String, FormField.Value> entry : optionValues.entrySet()) {
                    final String fieldName = entry.getKey();
                    final FormField.Value expectedValue = entry.getValue();
                    assertTrue(configurationForm.hasField(fieldName), "Expected the room configuration form requested by '" + conOne.getUser() + "' from '" + mucAddress + "' to contain a field named '" + fieldName + "' that was set to a non-default value when creating the room (but the configuration form does not contain that field).");
                    assertEquals(expectedValue.getValue(), configurationForm.getField(fieldName).getFirstValue(), "Expected the default value for field '" + fieldName + "' in the room configuration form requested by '" + conOne.getUser() + "' from '" + mucAddress + "' to equal the value that was used when creating the room (but it does not).");
                }
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to request a configuration form for '" + mucAddress + "' while being in the room (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a cancelled room configuration form does not affect the room configuration.
     */
    @SmackIntegrationTest(section = "10.2", quote = "If the room owner cancels the subsequent configuration, the service MUST leave the configuration of the room as it was before the room owner initiated the subsequent configuration process.")
    public void testRoomConfigCancel() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-config-cancel");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);

        try {
            createMuc(mucAsSeenByOwner, nicknameOwner);
            final Form originalConfigForm = mucAsSeenByOwner.getConfigurationForm();
            final List<FormField> originalConfig = originalConfigForm.getDataForm().getFields();

            try {
                // Execute system under test.
                final MUCOwner cancelRequest = new MUCOwner();
                cancelRequest.setTo(mucAddress);
                cancelRequest.setType(IQ.Type.set);
                cancelRequest.addExtension(DataForm.builder(DataForm.Type.cancel).build());

                conOne.sendIqRequestAndWaitForResponse(cancelRequest);

                // Verify result.
                final List<FormField> laterConfig = mucAsSeenByOwner.getConfigurationForm().getDataForm().getFields();
                assertArrayEquals(originalConfig.toArray(), laterConfig.toArray(), "Expected the room configuration for '" + mucAddress + "' to remain unchanged after cancelling a configuration form, but the list of form field taken before and after the form cancellation differs.");
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to submit and/or cancel a configuration form for '" + mucAddress + "' while being in the room (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that when as a result of a change in the room configuration a room admin loses admin status while in the room, the room sends updated presence.
     */
    @SmackIntegrationTest(section = "10.2", quote = "If as a result of a change in the room configuration a room admin loses admin status while in the room, the room MUST send updated presence for that individual to all occupants, denoting the change in status [...]")
    public void testRoomConfigDropAdmin() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-drop-admin");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerThree.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);
        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);

        try {
            mucAsSeenByOwner.create(nicknameOwner).getConfigFormManager()
                .setRoomAdmins(Set.of(conTwo.getUser().asBareJid()))
                .submitConfigurationForm();

            mucAsSeenByParticipant.join(nicknameParticipant);

            final SimpleResultSyncPoint ownerSeesTarget = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint participantSeesTarget = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        ownerSeesTarget.signal();
                    }
                }
            });
            mucAsSeenByParticipant.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        participantSeesTarget.signal();
                    }
                }
            });
            mucAsSeenByTarget.join(nicknameTarget);

            ownerSeesTarget.waitForResult(timeout);
            participantSeesTarget.waitForResult(timeout);

            final SimpleResultSyncPoint ownerSeesRevoke = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint targetSeesRevoke = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint participantSeesRevoke = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void adminRevoked(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        ownerSeesRevoke.signal();
                    }
                }
            });
            mucAsSeenByTarget.addUserStatusListener(new UserStatusListener() {
                @Override
                public void adminRevoked() {
                    targetSeesRevoke.signal();
                }
            });
            mucAsSeenByParticipant.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void adminRevoked(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        participantSeesRevoke.signal();
                    }
                }
            });

            try {
                // Execute system under test.
                mucAsSeenByOwner.getConfigFormManager()
                    .setRoomAdmins(Set.of()) // Remove the admin.
                    .submitConfigurationForm();

                // Verify result.
                assertResult(ownerSeesRevoke, "Expected '" + conOne.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the revokation of admin status from '" + targetMucAddress + "', after '" + conOne.getUser() + " updated the room configuration to remove '" + conTwo.getUser() + "' as a room admin (but no such stanza was received).");
                assertResult(targetSeesRevoke, "Expected '" + conTwo.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the revokation of admin status from '" + targetMucAddress + "', after '" + conOne.getUser() + " updated the room configuration to remove '" + conTwo.getUser() + "' as a room admin (but no such stanza was received).");
                assertResult(participantSeesRevoke, "Expected '" + conThree.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the revokation of admin status from '" + targetMucAddress + "', after '" + conOne.getUser() + " updated the room configuration to remove '" + conTwo.getUser() + "' as a room admin (but no such stanza was received).");
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to apply a change to a room using its configuration form for '" + mucAddress + "' while being in the room (but the server returned an error).", e);
            }
        } catch (MultiUserChatException.MucConfigurationNotSupportedException e) {
            throw new TestNotPossibleException(e);
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that when as a result of a change in the room configuration a user gains admin status while in the room, the room sends updated presence.
     */
    @SmackIntegrationTest(section = "10.2", quote = "If as a result of a change in the room configuration a user gains admin status while in the room, the room MUST send updated presence for that individual to all occupants, denoting the change in status [...]")
    public void testRoomConfigAddAdmin() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-add-admin");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerThree.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);
        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);

        try {
            mucAsSeenByOwner.create(nicknameOwner).makeInstant();

            mucAsSeenByParticipant.join(nicknameParticipant);

            final SimpleResultSyncPoint ownerSeesTarget = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint participantSeesTarget = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        ownerSeesTarget.signal();
                    }
                }
            });
            mucAsSeenByParticipant.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        participantSeesTarget.signal();
                    }
                }
            });
            mucAsSeenByTarget.join(nicknameTarget);

            ownerSeesTarget.waitForResult(timeout);
            participantSeesTarget.waitForResult(timeout);

            final SimpleResultSyncPoint ownerSeesGrant = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint targetSeesGrant = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint participantSeesGrant = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void adminGranted(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        ownerSeesGrant.signal();
                    }
                }
            });
            mucAsSeenByTarget.addUserStatusListener(new UserStatusListener() {
                @Override
                public void adminGranted() {
                    targetSeesGrant.signal();
                }
            });
            mucAsSeenByParticipant.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void adminGranted(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        participantSeesGrant.signal();
                    }
                }
            });

            try {
                // Execute system under test.
                mucAsSeenByOwner.getConfigFormManager()
                    .setRoomAdmins(Set.of( conTwo.getUser().asBareJid()))
                    .submitConfigurationForm();

                // Verify result.
                assertResult(ownerSeesGrant, "Expected '" + conOne.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the granting of admin status to '" + targetMucAddress + "', after '" + conOne.getUser() + " updated the room configuration to add '" + conTwo.getUser() + "' as a room admin (but no such stanza was received).");
                assertResult(targetSeesGrant, "Expected '" + conTwo.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the granting of admin status to '" + targetMucAddress + "', after '" + conOne.getUser() + " updated the room configuration to add '" + conTwo.getUser() + "' as a room admin (but no such stanza was received).");
                assertResult(participantSeesGrant, "Expected '" + conThree.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the granting of admin status to '" + targetMucAddress + "', after '" + conOne.getUser() + " updated the room configuration to add '" + conTwo.getUser() + "' as a room admin (but no such stanza was received).");
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to apply a change to a room using its configuration form for '" + mucAddress + "' while being in the room (but the server returned an error).", e);
            }
        } catch (MultiUserChatException.MucConfigurationNotSupportedException e) {
            throw new TestNotPossibleException(e);
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that when as a result of a change in the room configuration a room owner loses owner status while in the room, the room sends updated presence.
     */
    @SmackIntegrationTest(section = "10.2", quote = "If as a result of a change in the room configuration a room owner loses owner status while that owner is in the room, the room MUST send updated presence for that individual to all occupants, denoting the change in status [...]")
    public void testRoomConfigDropOwner() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-drop-owner");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerThree.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);
        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);

        try {
            mucAsSeenByOwner.create(nicknameOwner).getConfigFormManager()
                .setRoomOwners(Set.of(conOne.getUser().asBareJid(), conTwo.getUser().asBareJid()))
                .submitConfigurationForm();

            mucAsSeenByParticipant.join(nicknameParticipant);

            final SimpleResultSyncPoint ownerSeesTarget = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint participantSeesTarget = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        ownerSeesTarget.signal();
                    }
                }
            });
            mucAsSeenByParticipant.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        participantSeesTarget.signal();
                    }
                }
            });
            mucAsSeenByTarget.join(nicknameTarget);

            ownerSeesTarget.waitForResult(timeout);
            participantSeesTarget.waitForResult(timeout);

            final SimpleResultSyncPoint ownerSeesRevoke = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint targetSeesRevoke = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint participantSeesRevoke = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void ownershipRevoked(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        ownerSeesRevoke.signal();
                    }
                }
            });
            mucAsSeenByTarget.addUserStatusListener(new UserStatusListener() {
                @Override
                public void ownershipRevoked() {
                    targetSeesRevoke.signal();
                }
            });
            mucAsSeenByParticipant.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void ownershipRevoked(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        participantSeesRevoke.signal();
                    }
                }
            });

            try {
                // Execute system under test.
                mucAsSeenByOwner.getConfigFormManager()
                    .setRoomOwners(Set.of(conOne.getUser().asBareJid()))
                    .submitConfigurationForm();

                // Verify result.
                assertResult(ownerSeesRevoke, "Expected '" + conOne.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the revokation of ownership from '" + targetMucAddress + "', after '" + conOne.getUser() + " updated the room configuration to remove '" + conTwo.getUser() + "' as a room owner (but no such stanza was received).");
                assertResult(targetSeesRevoke, "Expected '" + conTwo.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the revokation of ownership from '" + targetMucAddress + "', after '" + conOne.getUser() + " updated the room configuration to remove '" + conTwo.getUser() + "' as a room owner (but no such stanza was received).");
                assertResult(participantSeesRevoke, "Expected '" + conThree.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the revokation of ownership from '" + targetMucAddress + "', after '" + conOne.getUser() + " updated the room configuration to remove '" + conTwo.getUser() + "' as a room owner (but no such stanza was received).");
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to apply a change to a room using its configuration form for '" + mucAddress + "' while being in the room (but the server returned an error).", e);
            }
        } catch (MultiUserChatException.MucConfigurationNotSupportedException e) {
            throw new TestNotPossibleException(e);
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that a room configuration form cannot be used to remove the only owner of a room.
     */
    @SmackIntegrationTest(section = "10.2", quote = "A service MUST NOT allow an owner to revoke his or her own owner status if there are no other owners; if an owner attempts to do this, the service MUST return a <conflict/> error to the owner.")
    public void testRoomConfigRemoveLastOwner() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-remove-last");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);

        try {
            createMuc(mucAsSeenByOwner, nicknameOwner);

            // Execute system under test & Verify result.
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
                mucAsSeenByOwner.getConfigFormManager()
                    .setRoomOwners(new HashSet<>())
                    .submitConfigurationForm();
            }, "Expected an error after '" + conOne.getUser() + "' (that is the only room owner) tried to update the configuration of room '" + mucAddress + "' to remove itself as an owner (but none occurred).");
            assertEquals(StanzaError.Condition.conflict, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conOne.getUser() + "' after it tried to remove itself as owner of room '" + mucAddress + "' using the room configuration form.");
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that when as a result of a change in the room configuration a user gains owner status while in the room, the room sends updated presence.
     */
    @SmackIntegrationTest(section = "10.2", quote = "If as a result of a change in the room configuration a user gains owner status while in the room, the room MUST send updated presence for that individual to all occupants, denoting the change in status [...]")
    public void testRoomConfigAddOwner() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-add-owner");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerThree.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);
        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);

        try {
            mucAsSeenByOwner.create(nicknameOwner).makeInstant();

            mucAsSeenByParticipant.join(nicknameParticipant);

            final SimpleResultSyncPoint ownerSeesTarget = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint participantSeesTarget = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        ownerSeesTarget.signal();
                    }
                }
            });
            mucAsSeenByParticipant.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        participantSeesTarget.signal();
                    }
                }
            });
            mucAsSeenByTarget.join(nicknameTarget);

            ownerSeesTarget.waitForResult(timeout);
            participantSeesTarget.waitForResult(timeout);

            final SimpleResultSyncPoint ownerSeesGrant = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint targetSeesGrant = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint participantSeesGrant = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void ownershipGranted(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        ownerSeesGrant.signal();
                    }
                }
            });
            mucAsSeenByTarget.addUserStatusListener(new UserStatusListener() {
                @Override
                public void ownershipGranted() {
                    targetSeesGrant.signal();
                }
            });
            mucAsSeenByParticipant.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void ownershipGranted(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        participantSeesGrant.signal();
                    }
                }
            });

            try {
                // Execute system under test.
                mucAsSeenByOwner.getConfigFormManager()
                    .setRoomOwners(Set.of(conOne.getUser().asBareJid(), conTwo.getUser().asBareJid()))
                    .submitConfigurationForm();

                // Verify result.
                assertResult(ownerSeesGrant, "Expected '" + conOne.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the granting of ownership to '" + targetMucAddress + "', after '" + conOne.getUser() + " updated the room configuration to add '" + conTwo.getUser() + "' as a room owner (but no such stanza was received).");
                assertResult(targetSeesGrant, "Expected '" + conTwo.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the granting of ownership to '" + targetMucAddress + "', after '" + conOne.getUser() + " updated the room configuration to add '" + conTwo.getUser() + "' as a room owner (but no such stanza was received).");
                assertResult(participantSeesGrant, "Expected '" + conThree.getUser() + "' to receive a presence stanza from '" + targetMucAddress + "' indicating the granting of ownership to '" + targetMucAddress + "', after '" + conOne.getUser() + " updated the room configuration to add '" + conTwo.getUser() + "' as a room owner (but no such stanza was received).");
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to apply a change to a room using its configuration form for '" + mucAddress + "' while being in the room (but the server returned an error).", e);
            }
        } catch (MultiUserChatException.MucConfigurationNotSupportedException e) {
            throw new TestNotPossibleException(e);
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that when as a result of a change in the room configuration a room becomes members-only, all non-members get removed from the room.
     */
    @SmackIntegrationTest(section = "10.2", quote = "If as a result of a change in the room configuration the room type is changed to members-only but there are non-members in the room, the service MUST remove any non-members from the room and include a status code of 322 in the presence unavailable stanzas sent to those users as well as any remaining occupants.")
    public void testRoomConfigToMemberOnly() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-make-memberonly");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerTwo.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByMember = mucManagerThree.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameTarget = Resourcepart.from("target-" + randomString);
        final Resourcepart nicknameMember = Resourcepart.from("member-" + randomString);
        final EntityFullJid targetMucAddress = JidCreate.entityFullFrom(mucAddress, nicknameTarget);

        final ResultSyncPoint<Presence, Exception> ownerSeesRemoval = new ResultSyncPoint<>();
        final ResultSyncPoint<Presence, Exception> targetSeesRemoval = new ResultSyncPoint<>();
        final ResultSyncPoint<Presence, Exception> memberSeesRemoval = new ResultSyncPoint<>();
        final StanzaListener ownerListener = (stanza) -> ownerSeesRemoval.signal((Presence) stanza);
        final StanzaListener targetListener = (stanza) -> targetSeesRemoval.signal((Presence) stanza);
        final StanzaListener memberListener = (stanza) -> memberSeesRemoval.signal((Presence) stanza);
        try {
            mucAsSeenByOwner.create(nicknameOwner).getConfigFormManager()
                .setMembersOnly(false)
                .submitConfigurationForm();

            mucAsSeenByOwner.grantMembership(conThree.getUser().asBareJid());

            mucAsSeenByMember.join(nicknameMember);

            final SimpleResultSyncPoint ownerSeesTarget = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint memberSeesTarget = new SimpleResultSyncPoint();
            mucAsSeenByOwner.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        ownerSeesTarget.signal();
                    }
                }
            });
            mucAsSeenByMember.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(EntityFullJid participant) {
                    if (participant.equals(targetMucAddress)) {
                        memberSeesTarget.signal();
                    }
                }
            });
            mucAsSeenByTarget.join(nicknameTarget);

            ownerSeesTarget.waitForResult(timeout);
            memberSeesTarget.waitForResult(timeout);

            final StanzaFilter removalDetectionFilter = new AndFilter(
                FromMatchesFilter.create(mucAddress),
                PresenceTypeFilter.UNAVAILABLE
            );
            conOne.addStanzaListener(ownerListener, removalDetectionFilter);
            conTwo.addStanzaListener(targetListener, removalDetectionFilter);
            conThree.addStanzaListener(memberListener, removalDetectionFilter);

            try {
                // Execute system under test.
                mucAsSeenByOwner.getConfigFormManager()
                    .setMembersOnly(true)
                    .submitConfigurationForm();

                // Verify result.
                final Presence ownerKick = assertResult(ownerSeesRemoval, "Expected '" + conOne.getUser() + "' to receive an 'unavailable' presence stanza from '" + targetMucAddress + "' indicating that non-member '" + targetMucAddress + "' was removed from the room, after '" + conOne.getUser() + " updated the room configuration to be members-only (but no such stanza was received).");
                assertTrue(ownerKick.getExtension(MUCUser.class).getStatus().stream().anyMatch(status -> status.getCode() == 322), "Expected to find status code '322' in the 'unavailable' presence stanza that '" + conOne.getUser() + "' received from '" + targetMucAddress + "' indicating that non-member '" + targetMucAddress + "' was removed from the room, after '" + conOne.getUser() + " updated the room configuration to be members-only (but that status code was not found).");
                final Stanza targetKick = assertResult(targetSeesRemoval, "Expected '" + conTwo.getUser() + "' to receive an 'unavailable' presence stanza from '" + targetMucAddress + "' indicating that non-member '" + targetMucAddress + "' was removed from the room, after '" + conOne.getUser() + " updated the room configuration to be members-only (but no such stanza was received).");
                assertTrue(targetKick.getExtension(MUCUser.class).getStatus().stream().anyMatch(status -> status.getCode() == 322), "Expected to find status code '322' in the 'unavailable' presence stanza that '" + conTwo.getUser() + "' received from '" + targetMucAddress + "' indicating that non-member '" + targetMucAddress + "' was removed from the room, after '" + conOne.getUser() + " updated the room configuration to be members-only (but that status code was not found).");
                final Stanza memberKick = assertResult(memberSeesRemoval, "Expected '" + conThree.getUser() + "' to receive an 'unavailable' presence stanza from '" + targetMucAddress + "' indicating that non-member '" + targetMucAddress + "' was removed from the room, after '" + conOne.getUser() + " updated the room configuration to be members-only (but no such stanza was received).");
                assertTrue(memberKick.getExtension(MUCUser.class).getStatus().stream().anyMatch(status -> status.getCode() == 322), "Expected to find status code '322' in the 'unavailable' presence stanza that '" + conThree.getUser() + "' received from '" + targetMucAddress + "' indicating that non-member '" + targetMucAddress + "' was removed from the room, after '" + conOne.getUser() + " updated the room configuration to be members-only (but that status code was not found).");
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to apply a change to a room using its configuration form for '" + mucAddress + "' while being in the room (but the server returned an error).", e);
            }
        } catch (MultiUserChatException.MucConfigurationNotSupportedException e) {
            throw new TestNotPossibleException(e);
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
            conOne.removeStanzaListener(ownerListener);
            conTwo.removeStanzaListener(targetListener);
            conThree.removeStanzaListener(memberListener);
        }
    }

    /**
     * Verifies that a 170 notification is broadcast after room logging is enabled.
     */
    @SmackIntegrationTest(section = "10.2.1", quote = "A room MUST send notification to all occupants when the room configuration changes in a way that has an impact on the privacy or security profile of the room. [...] which shall contain only a <status/> element with an appropriate value for the 'code' attribute. [...] If room logging is now enabled, status code 170.")
    public void testRoomConfigNotification170() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-notify-170");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);

        final SimpleResultSyncPoint ownerSeesNotification = new SimpleResultSyncPoint();
        final SimpleResultSyncPoint participantSeesNotification = new SimpleResultSyncPoint();
        final StanzaListener ownerListener = stanza -> ownerSeesNotification.signal();
        final StanzaListener participantListener = stanza -> participantSeesNotification.signal();
        final int statusCode = 170;
        try {
            mucAsSeenByOwner.create(nicknameOwner).getConfigFormManager()
                .disablPublicLogging()
                .submitConfigurationForm();

            mucAsSeenByParticipant.join(nicknameParticipant);

            final StanzaFilter notificationDetectionFilter = new AndFilter(
                FromMatchesFilter.create(mucAddress),
                StanzaTypeFilter.MESSAGE,
                new MUCUserStatusCodeFilter(MUCUser.Status.create(statusCode))
            );
            conOne.addStanzaListener(ownerListener, notificationDetectionFilter);
            conTwo.addStanzaListener(participantListener, notificationDetectionFilter);

            try {
                // Execute system under test.
                mucAsSeenByOwner.getConfigFormManager()
                    .enablePublicLogging()
                    .submitConfigurationForm();

                // Verify result.
                assertResult(ownerSeesNotification, "Expected '" + conOne.getUser() + "' to receive an a message stanza notification with status code " + statusCode + " from '" + mucAddress + "'  after '" + conOne.getUser() + " updated the room configuration to enable room logging (but no such stanza was received).");
                assertResult(participantSeesNotification, "Expected '" + conTwo.getUser() + "' to receive an a message stanza notification with status code " + statusCode + " from '" + mucAddress + "'  after '" + conOne.getUser() + " updated the room configuration to enable room logging (but no such stanza was received).");
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to apply a change to a room using its configuration form for '" + mucAddress + "' while being in the room (but the server returned an error).", e);
            }
        } catch (MultiUserChatException.MucConfigurationNotSupportedException e) {
            throw new TestNotPossibleException(e);
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
            conOne.removeStanzaListener(ownerListener);
            conTwo.removeStanzaListener(participantListener);
        }
    }

    /**
     * Verifies that a 171 notification is broadcast after room logging is disabled.
     */
    @SmackIntegrationTest(section = "10.2.1", quote = "A room MUST send notification to all occupants when the room configuration changes in a way that has an impact on the privacy or security profile of the room. [...] which shall contain only a <status/> element with an appropriate value for the 'code' attribute. [...] If room logging is now disabled, status code 171.")
    public void testRoomConfigNotification171() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-notify-171");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);

        final SimpleResultSyncPoint ownerSeesNotification = new SimpleResultSyncPoint();
        final SimpleResultSyncPoint participantSeesNotification = new SimpleResultSyncPoint();
        final StanzaListener ownerListener = stanza -> ownerSeesNotification.signal();
        final StanzaListener participantListener = stanza -> participantSeesNotification.signal();
        final int statusCode = 171;
        try {
            mucAsSeenByOwner.create(nicknameOwner).getConfigFormManager()
                .enablePublicLogging()
                .submitConfigurationForm();

            mucAsSeenByParticipant.join(nicknameParticipant);

            final StanzaFilter notificationDetectionFilter = new AndFilter(
                FromMatchesFilter.create(mucAddress),
                StanzaTypeFilter.MESSAGE,
                new MUCUserStatusCodeFilter(MUCUser.Status.create(statusCode))
            );
            conOne.addStanzaListener(ownerListener, notificationDetectionFilter);
            conTwo.addStanzaListener(participantListener, notificationDetectionFilter);

            try {
                // Execute system under test.
                mucAsSeenByOwner.getConfigFormManager()
                    .disablPublicLogging()
                    .submitConfigurationForm();

                // Verify result.
                assertResult(ownerSeesNotification, "Expected '" + conOne.getUser() + "' to receive an a message stanza notification with status code " + statusCode + " from '" + mucAddress + "'  after '" + conOne.getUser() + " updated the room configuration to disable room logging (but no such stanza was received).");
                assertResult(participantSeesNotification, "Expected '" + conTwo.getUser() + "' to receive an a message stanza notification with status code " + statusCode + " from '" + mucAddress + "'  after '" + conOne.getUser() + " updated the room configuration to disable room logging (but no such stanza was received).");
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to apply a change to a room using its configuration form for '" + mucAddress + "' while being in the room (but the server returned an error).", e);
            }
        } catch (MultiUserChatException.MucConfigurationNotSupportedException e) {
            throw new TestNotPossibleException(e);
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
            conOne.removeStanzaListener(ownerListener);
            conTwo.removeStanzaListener(participantListener);
        }
    }

    /**
     * Verifies that a 172 notification is broadcast after the room is switched to be non-anonymous.
     */
    @SmackIntegrationTest(section = "10.2.1", quote = "A room MUST send notification to all occupants when the room configuration changes in a way that has an impact on the privacy or security profile of the room. [...] which shall contain only a <status/> element with an appropriate value for the 'code' attribute. [...] If the room is now non-anonymous, status code 172.")
    public void testRoomConfigNotification172() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-notify-172");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);

        final SimpleResultSyncPoint ownerSeesNotification = new SimpleResultSyncPoint();
        final SimpleResultSyncPoint participantSeesNotification = new SimpleResultSyncPoint();
        final StanzaListener ownerListener = stanza -> ownerSeesNotification.signal();
        final StanzaListener participantListener = stanza -> participantSeesNotification.signal();
        final int statusCode = 172;
        try {
            createSemiAnonymousMuc(mucAsSeenByOwner, nicknameOwner);

            mucAsSeenByParticipant.join(nicknameParticipant);

            final StanzaFilter notificationDetectionFilter = new AndFilter(
                FromMatchesFilter.create(mucAddress),
                StanzaTypeFilter.MESSAGE,
                new MUCUserStatusCodeFilter(MUCUser.Status.create(statusCode))
            );
            conOne.addStanzaListener(ownerListener, notificationDetectionFilter);
            conTwo.addStanzaListener(participantListener, notificationDetectionFilter);

            try {
                // Execute system under test.
                final Form configForm = mucAsSeenByOwner.getConfigurationForm();
                final FillableForm answerForm = configForm.getFillableForm();
                answerForm.setAnswer("muc#roomconfig_whois", "anyone");
                mucAsSeenByOwner.sendConfigurationForm(answerForm);

                // Verify result.
                assertResult(ownerSeesNotification, "Expected '" + conOne.getUser() + "' to receive an a message stanza notification with status code " + statusCode + " from '" + mucAddress + "'  after '" + conOne.getUser() + " updated the room configuration to be non-anonymous (but no such stanza was received).");
                assertResult(participantSeesNotification, "Expected '" + conTwo.getUser() + "' to receive an a message stanza notification with status code " + statusCode + " from '" + mucAddress + "'  after '" + conOne.getUser() + " updated the room configuration to be non-anonymous (but no such stanza was received).");
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to apply a change to a room using its configuration form for '" + mucAddress + "' while being in the room (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
            conOne.removeStanzaListener(ownerListener);
            conTwo.removeStanzaListener(participantListener);
        }
    }

    /**
     * Verifies that a 173 notification is broadcast after the room is switched to be semi-anonymous.
     */
    @SmackIntegrationTest(section = "10.2.1", quote = "A room MUST send notification to all occupants when the room configuration changes in a way that has an impact on the privacy or security profile of the room. [...] which shall contain only a <status/> element with an appropriate value for the 'code' attribute. [...] f the room is now semi-anonymous, status code 173.")
    public void testRoomConfigNotification173() throws Exception
    {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-owner-notify-173");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOwner = Resourcepart.from("owner-" + randomString);
        final Resourcepart nicknameParticipant = Resourcepart.from("participant-" + randomString);

        final SimpleResultSyncPoint ownerSeesNotification = new SimpleResultSyncPoint();
        final SimpleResultSyncPoint participantSeesNotification = new SimpleResultSyncPoint();
        final StanzaListener ownerListener = stanza -> ownerSeesNotification.signal();
        final StanzaListener participantListener = stanza -> participantSeesNotification.signal();
        final int statusCode = 173;
        try {
            createNonAnonymousMuc(mucAsSeenByOwner, nicknameOwner);

            mucAsSeenByParticipant.join(nicknameParticipant);

            final StanzaFilter notificationDetectionFilter = new AndFilter(
                FromMatchesFilter.create(mucAddress),
                StanzaTypeFilter.MESSAGE,
                new MUCUserStatusCodeFilter(MUCUser.Status.create(statusCode))
            );
            conOne.addStanzaListener(ownerListener, notificationDetectionFilter);
            conTwo.addStanzaListener(participantListener, notificationDetectionFilter);

            try {
                // Execute system under test.
                final Form configForm = mucAsSeenByOwner.getConfigurationForm();
                final FillableForm answerForm = configForm.getFillableForm();
                answerForm.setAnswer("muc#roomconfig_whois", "moderators");
                mucAsSeenByOwner.sendConfigurationForm(answerForm);

                // Verify result.
                assertResult(ownerSeesNotification, "Expected '" + conOne.getUser() + "' to receive an a message stanza notification with status code " + statusCode + " from '" + mucAddress + "'  after '" + conOne.getUser() + " updated the room configuration to be semi-anonymous (but no such stanza was received).");
                assertResult(participantSeesNotification, "Expected '" + conTwo.getUser() + "' to receive an a message stanza notification with status code " + statusCode + " from '" + mucAddress + "'  after '" + conOne.getUser() + " updated the room configuration to be semi-anonymous (but no such stanza was received).");
            } catch (XMPPException.XMPPErrorException e) {
                fail("Expected owner '" + conOne.getUser() + "' to be able to apply a change to a room using its configuration form for '" + mucAddress + "' while being in the room (but the server returned an error).", e);
            }
        } finally {
            // Tear down test fixture.
            tryDestroy(mucAsSeenByOwner);
            conOne.removeStanzaListener(ownerListener);
            conTwo.removeStanzaListener(participantListener);
        }
    }

}
