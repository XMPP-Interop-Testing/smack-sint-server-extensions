/**
 * Copyright 2025 Dan Caseley
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
package org.igniterealtime.smack.inttest.xep0133;

import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.FlexibleStanzaTypeFilter;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.commands.AdHocCommandNote;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the XEP-0133: Service Administration
 *
 * @see <a href="https://xmpp.org/extensions/xep-0133.html">XEP-0133: Service Administration</a>
 */
@SpecificationReference(document = "XEP-0133", version = "1.3.1")
public class AdHocCommandIntegrationTest extends AbstractAdHocCommandIntegrationTest {

    private static final String ADD_A_USER = "http://jabber.org/protocol/admin#add-user"; //4.1
    private static final String SEND_ANNOUNCEMENT_TO_ONLINE_USERS = "http://jabber.org/protocol/admin#announce"; //4.23
    private static final String SET_MOTD = "http://jabber.org/protocol/admin#set-motd"; //4.24
    private static final String EDIT_MOTD = "http://jabber.org/protocol/admin#edit-motd"; //4.25
    private static final String DELETE_MOTD = "http://jabber.org/protocol/admin#delete-motd"; //4.26
    private static final String SET_WELCOME_MESSAGE = "http://jabber.org/protocol/admin#set-welcome"; //4.27
    private static final String DELETE_WELCOME_MESSAGE = "http://jabber.org/protocol/admin#set-welcome"; //4.28
    private static final String CHANGE_USER_PASSWORD = "http://jabber.org/protocol/admin#change-user-password"; //4.7
    private static final String DELETE_A_USER = "http://jabber.org/protocol/admin#delete-user"; //4.2
    private static final String DISABLE_A_USER = "http://jabber.org/protocol/admin#disable-user"; //4.3
    private static final String EDIT_ADMIN_LIST = "http://jabber.org/protocol/admin#edit-admin"; //4.29
    private static final String EDIT_BLOCKED_LIST = "http://jabber.org/protocol/admin#edit-blacklist"; //4.11
    private static final String EDIT_ALLOWED_LIST = "http://jabber.org/protocol/admin#edit-whitelist"; //4.12
    private static final String END_USER_SESSION = "http://jabber.org/protocol/admin#end-user-session"; //4.5
    private static final String GET_NUMBER_OF_ACTIVE_USERS = "http://jabber.org/protocol/admin#get-active-users-num"; //4.16
    private static final String GET_LIST_OF_ACTIVE_USERS = "http://jabber.org/protocol/admin#get-active-users"; //4.21
    private static final String GET_LIST_OF_DISABLED_USERS = "http://jabber.org/protocol/admin#get-disabled-users-list"; //4.19
    private static final String GET_NUMBER_OF_DISABLED_USERS = "http://jabber.org/protocol/admin#get-disabled-users-num"; //4.14
    private static final String GET_NUMBER_OF_IDLE_USERS = "http://jabber.org/protocol/admin#get-idle-users-num"; //4.17
    private static final String GET_LIST_OF_IDLE_USERS = "http://jabber.org/protocol/admin#get-idle-users"; //4.22
    private static final String GET_LIST_OF_ONLINE_USERS = "http://jabber.org/protocol/admin#get-online-users-list"; //4.20
    private static final String GET_NUMBER_OF_ONLINE_USERS = "http://jabber.org/protocol/admin#get-online-users-num"; //4.15
    private static final String GET_LIST_OF_REGISTERED_USERS = "http://jabber.org/protocol/admin#get-registered-users-list"; //4.18
    private static final String GET_NUMBER_OF_REGISTERED_USERS = "http://jabber.org/protocol/admin#get-registered-users-num"; //4.13
    private static final String GET_USER_ROSTER = "http://jabber.org/protocol/admin#get-user-roster"; //4.8
    private static final String REENABLE_A_USER = "http://jabber.org/protocol/admin#reenable-user"; //4.4
    private static final String GET_USER_LAST_LOGIN_TIME = "http://jabber.org/protocol/admin#get-user-lastlogin"; //4.9
    private static final String GET_USER_STATISTICS = "http://jabber.org/protocol/admin#user-stats"; //4.10
    private static final String RESTART_SERVICE = "http://jabber.org/protocol/admin#restart"; //4.30
    private static final String SHUTDOWN_SERVICE = "http://jabber.org/protocol/admin#shutdown"; //4.31

    public AdHocCommandIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws InvocationTargetException, InstantiationException, IllegalAccessException, SmackException, IOException, XMPPException, InterruptedException {
        super(environment);
    }

    private void createUser(Jid jid) throws Exception {
        createUser(jid, "password");
    }

    private void createUser(Jid jid, String password) throws Exception {
        executeCommandWithArgs(ADD_A_USER, adminConnection.getUser().asEntityBareJid(),
            "accountjid", jid.toString(),
            "password", password,
            "password-verify", password
        );
    }

    private void deleteUser(String jid) throws Exception {
        executeCommandWithArgs(DELETE_A_USER, adminConnection.getUser().asEntityBareJid(),
            "accountjids", jid
        );
    }
    private void deleteUser(Jid jid) throws Exception {
        executeCommandWithArgs(DELETE_A_USER, adminConnection.getUser().asEntityBareJid(),
            "accountjids", jid.toString()
        );
    }

    @SmackIntegrationTest(section = "3", quote =
        "A server or component MUST advertise any administrative commands it supports via Service Discovery (XEP-0030) " +
        "(as described in XEP-0050: Ad-Hoc Commands); such commands exist as well-defined discovery nodes associated " +
        "with the service in question.")
    public void testGetCommandsForUser() throws Exception {
        // Setup test fixture.

        // Execute system under test.
        DiscoverItems result = adHocCommandManagerForConOne.discoverCommands(conOne.getUser().asEntityBareJid());

        // Verify results.
        List<DiscoverItems.Item> items = result.getItems();
        assertFalse(items.isEmpty());
    }

    @SmackIntegrationTest(section = "3", quote =
        "A server or component MUST advertise any administrative commands it supports via Service Discovery (XEP-0030) " +
            "(as described in XEP-0050: Ad-Hoc Commands); such commands exist as well-defined discovery nodes associated " +
            "with the service in question.")
    public void testGetCommandsForAdmin() throws Exception {
        // Setup test fixture.

        // Execute system under test.
        DiscoverItems result = adHocCommandManagerForAdmin.discoverCommands(adminConnection.getUser().asEntityBareJid());

        // Verify results.
        List<DiscoverItems.Item> items = result.getItems();
        assertTrue(items.size() > 10);
    }

    //node="http://jabber.org/protocol/admin#add-user" name="Add a User"
    @SmackIntegrationTest(section = "4.1")
    public void testAddUser() throws Exception {
        checkServerSupportCommand(ADD_A_USER);
        // Setup test fixture.
        final Jid addedUser = JidCreate.bareFrom("addusertest" + testRunId + "@example.org");
        try {
            // Execute system under test.
            AdHocCommandData result = executeCommandWithArgs(ADD_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjid", addedUser.toString(),
                "password", "password",
                "password-verify", "password"
            );

            // Verify results.
            assertNoteType(AdHocCommandNote.Type.info, result);
            assertNoteContains("Operation finished successfully", result);

            try {
                AbstractXMPPConnection userConnection = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
                userConnection.connect();
                userConnection.login(addedUser.getLocalpartOrThrow().toString(), "password");
                assertTrue(userConnection.isAuthenticated());
            } catch (Exception e) {
                fail("Failed to login as the newly created user: " + addedUser, e);
            }
        } finally {
            // Tear down test fixture.
            deleteUser(addedUser);
        }
    }

    @SmackIntegrationTest(section = "4.1")
    public void testAddUserWithoutJid() throws Exception {
        checkServerSupportCommand(ADD_A_USER);
        Exception e = assertThrows(IllegalStateException.class, () ->
            executeCommandWithArgs(ADD_A_USER, adminConnection.getUser().asEntityBareJid(),
                "password", "password",
                "password-verify", "password"
        ));
        assertEquals("Not all required fields filled. Missing: [accountjid]", e.getMessage());
    }

    @SmackIntegrationTest(section = "4.1")
    public void testAddUserWithMismatchedPassword() throws Exception {
        checkServerSupportCommand(ADD_A_USER);
        // Setup test fixture.
        final Jid newUser = JidCreate.bareFrom("addusermismatchedpasswordtest" + testRunId + "@example.org");
        try {
            // Execute system under test.
            AdHocCommandData result = executeCommandWithArgs(ADD_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjid", newUser.toString(),
                "password", "password",
                "password-verify", "password2"
            );

            // Verify results.
            assertNoteType(AdHocCommandNote.Type.error, result);
            assertNoteContains("Passwords do not match", result);
        } finally {
            // Tear down test fixture.
            deleteUser(newUser);
        }
    }

    @SmackIntegrationTest(section = "4.1")
    public void testAddUserWithRemoteJid() throws Exception {
        checkServerSupportCommand(ADD_A_USER);
        // Setup test fixture.
        final Jid newUser = JidCreate.bareFrom("adduserinvalidjidtest" + testRunId + "@somewhereelse.org");
        try {
            // Execute system under test.
            AdHocCommandData result = executeCommandWithArgs(ADD_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjid", newUser.toString(),
                "password", "password",
                "password-verify", "password2"
            );

            // Verify results.
            assertNoteType(AdHocCommandNote.Type.error, result);
            assertNoteContains("Cannot create remote user", result);
        } finally {
            // Tear down test fixture.
            deleteUser(newUser);
        }
    }

    @SmackIntegrationTest(section = "4.1")
    public void testAddUserWithInvalidJid() throws Exception {
        checkServerSupportCommand(ADD_A_USER);
        // Setup test fixture.
        final String newUserInvalidJid = "adduserinvalidjidtest" + testRunId + "@invalid@domain";
        try {
            // Execute system under test.
            AdHocCommandData result = executeCommandWithArgs(ADD_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjid", newUserInvalidJid,
                "password", "password",
                "password-verify", "password2"
            );

            // Verify results.
            assertNoteType(AdHocCommandNote.Type.error, result);
            assertNoteContains("Please provide a valid JID", result);
        } finally {
            // Tear down test fixture.
            deleteUser(newUserInvalidJid); // Should not exist, but just in case this somehow made it through, delete it.
        }
    }

    //node="http://jabber.org/protocol/admin#delete-user" name="Delete a User"
    @SmackIntegrationTest(section = "4.2")
    public void testDeleteUser() throws Exception {
        checkServerSupportCommand(DELETE_A_USER);
        // Setup test fixture.
        final Jid deletedUser = JidCreate.bareFrom("deleteusertest" + testRunId + "@example.org");
        createUser(deletedUser);

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(DELETE_A_USER, adminConnection.getUser().asEntityBareJid(),
            "accountjids", deletedUser.toString()
        );

        // Verify results.
        assertNoteType(AdHocCommandNote.Type.info, result);
        assertNoteContains("Operation finished successfully", result);
    }

    @SmackIntegrationTest(section = "4.2")
    public void testDeleteUserWithFullJid() throws Exception {
        checkServerSupportCommand(DELETE_A_USER);
        // Setup test fixture.
        final Jid deletedUser = JidCreate.bareFrom("deleteusertest2" + testRunId + "@example.org");
        createUser(deletedUser);

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(DELETE_A_USER, adminConnection.getUser().asEntityBareJid(),
            "accountjids", deletedUser.toString() + "/resource"
        );

        // Verify results.
        assertNoteType(AdHocCommandNote.Type.info, result);
        assertNoteContains("Operation finished successfully", result);
        // Although https://xmpp.org/extensions/xep-0133.html#delete-user specifies that the client should send the bare
        // JID, there's no error handling specified for the case where the full JID is sent, and so it's expected that
        // the server should handle it gracefully.
    }

    //node="http://jabber.org/protocol/admin#disable-user" name="Disable a User"
    @SmackIntegrationTest(section = "4.3")
    public void testDisableUser() throws Exception {
        checkServerSupportCommand(DISABLE_A_USER);
        // Setup test fixture.
        final Jid disabledUser = JidCreate.bareFrom("disableusertest" + testRunId + "@example.org");
        try {
            createUser(disabledUser);

            // Execute system under test.
            AdHocCommandData result = executeCommandWithArgs(DISABLE_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjids", disabledUser.toString()
            );

            // Verify results.
            assertNoteType(AdHocCommandNote.Type.info, result);
            assertNoteContains("Operation finished successfully", result);
        } finally {
            // Tear down test fixture.
            deleteUser(disabledUser);
        }
    }

    //node="http://jabber.org/protocol/admin#reenable-user" name="Re-Enable a User"
    @SmackIntegrationTest(section = "4.4")
    public void testReenableUser() throws Exception {
        checkServerSupportCommand(REENABLE_A_USER);
        checkServerSupportCommand(DISABLE_A_USER);

        final Jid disabledUser = JidCreate.entityBareFrom("reenableusertest" + testRunId + "@example.org");
        try {
            // Setup test fixture.
            createUser(disabledUser);
            executeCommandWithArgs(DISABLE_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjids", disabledUser.toString()
            );

            // Execute system under test.
            AdHocCommandData result = executeCommandWithArgs(REENABLE_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjids", disabledUser.toString()
            );

            // Verify results.
            assertNoteType(AdHocCommandNote.Type.info, result);
            assertNoteContains("Operation finished successfully", result);
        } finally {
            // Tear down test fixture.
            deleteUser(disabledUser);
        }
    }

    @SmackIntegrationTest(section = "4.4")
    public void testReenableNonDisabledUser() throws Exception {
        checkServerSupportCommand(REENABLE_A_USER);
        final Jid disabledUser = JidCreate.entityBareFrom("reenableusernondisabledtest" + testRunId + "@example.org");
        try {
            // Setup test fixture.
            createUser(disabledUser);

            // Execute system under test.
            AdHocCommandData result = executeCommandWithArgs(REENABLE_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjids", disabledUser.toString()
            );

            // Verify results.
            assertNoteType(AdHocCommandNote.Type.info, result);
            assertNoteContains("Operation finished successfully", result);
        } finally {
            // Tear down test fixture.
            deleteUser(disabledUser);
        }
    }

    @SmackIntegrationTest(section = "4.4")
    public void testReenableNonExistingUser() throws Exception {
        checkServerSupportCommand(REENABLE_A_USER);

        // Setup test fixture.
        final Jid disabledUser = JidCreate.entityBareFrom("reenablenonexistingusertest" + testRunId + "@example.org");

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(REENABLE_A_USER, adminConnection.getUser().asEntityBareJid(),
            "accountjids", disabledUser.toString()
        );

        // Verify results.
        assertNoteType(AdHocCommandNote.Type.error, result);
        assertNoteContains("User does not exist: " + disabledUser, result);
    }

    @SmackIntegrationTest(section = "4.4")
    public void testReenableRemoteUser() throws Exception {
        checkServerSupportCommand(REENABLE_A_USER);

        // Setup test fixture.
        final Jid disabledUser = JidCreate.entityBareFrom("reenableremoteusertest" + testRunId + "@elsewhere.org");

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(REENABLE_A_USER, adminConnection.getUser().asEntityBareJid(),
            "accountjids", disabledUser.toString()
        );

        // Verify results.
        assertNoteType(AdHocCommandNote.Type.error, result);
        assertNoteContains("Cannot re-enable remote user: " + disabledUser, result);
    }

    //node="http://jabber.org/protocol/admin#end-user-session" name="End User Session"
    @SmackIntegrationTest(section = "4.5", quote = "An administrator may need to terminate one [...] of the user's current sessions [...] if the JID is of the form <user@host/resource>, the service MUST end only the session associated with that resource.")
    public void testEndUserSessionFullJid() throws Exception {
        checkServerSupportCommand(END_USER_SESSION);
        checkServerSupportCommand(GET_LIST_OF_ACTIVE_USERS);

        final Jid testUser = JidCreate.bareFrom("endsessiontest-full-" + testRunId + "@example.org");
        AbstractXMPPConnection userConnectionOne = null;
        AbstractXMPPConnection userConnectionTwo = null;
        try {
            createUser(testUser);

            // Login as the user to be able to end their session
            userConnectionOne = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
            userConnectionTwo = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
            userConnectionOne.connect();
            userConnectionTwo.connect();
            userConnectionOne.login(testUser.getLocalpartOrThrow().toString(), "password", Resourcepart.from("resource-one-" + StringUtils.randomString(5)));
            userConnectionTwo.login(testUser.getLocalpartOrThrow().toString(), "password", Resourcepart.from("resource-two-" + StringUtils.randomString(5)));

            final SimpleResultSyncPoint isDisconnected = new SimpleResultSyncPoint();
            userConnectionOne.addConnectionListener(new ConnectionListener() {
                @Override
                public void connectionClosed() {
                    isDisconnected.signal();
                }

                @Override
                public void connectionClosedOnError(Exception e) {
                    isDisconnected.signal();
                }
            });

            final String needle = "wait for me " + StringUtils.randomString(13);
            final SimpleResultSyncPoint receivedMessage = new SimpleResultSyncPoint();
            userConnectionTwo.addSyncStanzaListener((stanza) -> receivedMessage.signal(), new FlexibleStanzaTypeFilter<Message>() {
                protected boolean acceptSpecific(Message message) {
                    return message.getFrom().equals(adminConnection.getUser()) && needle.equals(message.getBody());
            }});

            // End the user's session
            AdHocCommandData result = executeCommandWithArgs(END_USER_SESSION, adminConnection.getUser().asEntityBareJid(),
                "accountjids",  userConnectionOne.getUser().toString() // _full_ JID. Should close only this session.
            );

            assertEquals(AdHocCommandData.Status.completed, result.getStatus(), "Expected the status of the " + END_USER_SESSION + "command that was executed by '" + adminConnection.getUser() + " to represent that the command is done executing (but it does not).");
            assertResult(isDisconnected, "Expected the connection of '" + userConnectionOne.getUser() + "' to be disconnected after '" + adminConnection.getUser() + "' invoked the " + END_USER_SESSION + " ad-hoc command using the target's full JID (but the connection remains connected).");

            // Send a message to the _other_ resource. As the server must process the stanzas sent by admin in order, the message would likely not be received when that other resource also got disconnected.
            adminConnection.sendStanza(MessageBuilder.buildMessage().setBody(needle).to(userConnectionTwo.getUser()).build());
            receivedMessage.waitForResult(timeout);
            assertTrue(userConnectionTwo.isConnected(), "Did not expected the connection of '" + userConnectionTwo.getUser() + "' to be disconnected after '" + adminConnection.getUser() + "' invoked the " + END_USER_SESSION + " ad-hoc command using the full JID of a different resource of that user ('" + userConnectionOne.getUser() + "').");
        } finally {
            if (userConnectionOne != null && userConnectionOne.isConnected()) {
                userConnectionOne.disconnect();
            }
            if (userConnectionTwo != null && userConnectionTwo.isConnected()) {
                userConnectionTwo.disconnect();
            }
            deleteUser(testUser);
        }
    }

    @SmackIntegrationTest(section = "4.5", quote = "An administrator may need to terminate [...] all of the user's current sessions [...] If the JID is of the form <user@host>, the service MUST end all of the user's sessions")
    public void testEndUserSessionBareJid() throws Exception {
        checkServerSupportCommand(END_USER_SESSION);
        checkServerSupportCommand(GET_LIST_OF_ACTIVE_USERS);

        final Jid testUser = JidCreate.bareFrom("endsessiontest-bare-" + testRunId + "@example.org");
        AbstractXMPPConnection userConnectionOne = null;
        AbstractXMPPConnection userConnectionTwo = null;
        try {
            createUser(testUser);

            // Login as the user to be able to end their session
            userConnectionOne = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
            userConnectionTwo = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
            userConnectionOne.connect();
            userConnectionTwo.connect();
            userConnectionOne.login(testUser.getLocalpartOrThrow().toString(), "password", Resourcepart.from("resource-one-" + StringUtils.randomString(5)));
            userConnectionTwo.login(testUser.getLocalpartOrThrow().toString(), "password", Resourcepart.from("resource-two-" + StringUtils.randomString(5)));

            final SimpleResultSyncPoint isOneDisconnected = new SimpleResultSyncPoint();
            userConnectionOne.addConnectionListener(new ConnectionListener() {
                @Override
                public void connectionClosed() {
                    isOneDisconnected.signal();
                }

                @Override
                public void connectionClosedOnError(Exception e) {
                    isOneDisconnected.signal();
                }
            });

            final SimpleResultSyncPoint isTwoDisconnected = new SimpleResultSyncPoint();
            userConnectionTwo.addConnectionListener(new ConnectionListener() {
                @Override
                public void connectionClosed() {
                    isTwoDisconnected.signal();
                }

                @Override
                public void connectionClosedOnError(Exception e) {
                    isTwoDisconnected.signal();
                }
            });

            // End the user's sessions
            AdHocCommandData result = executeCommandWithArgs(END_USER_SESSION, adminConnection.getUser().asEntityBareJid(),
                "accountjids",  userConnectionOne.getUser().asBareJid().toString() // bare_ JID. Should close all sessions.
            );

            assertEquals(AdHocCommandData.Status.completed, result.getStatus(), "Expected the status of the " + END_USER_SESSION + "command that was executed by '" + adminConnection.getUser() + " to represent that the command is done executing (but it does not).");
            assertResult(isOneDisconnected, "Expected the connection of '" + userConnectionOne.getUser() + "' to be disconnected after '" + adminConnection.getUser() + "' invoked the " + END_USER_SESSION + " ad-hoc command using the target's bare JID (but the connection remains connected).");
            assertResult(isTwoDisconnected, "Expected the connection of '" + userConnectionTwo.getUser() + "' to be disconnected after '" + adminConnection.getUser() + "' invoked the " + END_USER_SESSION + " ad-hoc command using the target's bare JID (but the connection remains connected).");
        } finally {
            if (userConnectionOne != null && userConnectionOne.isConnected()) {
                userConnectionOne.disconnect();
            }
            if (userConnectionTwo != null && userConnectionTwo.isConnected()) {
                userConnectionTwo.disconnect();
            }
            deleteUser(testUser);
        }
    }

    @SmackIntegrationTest(section = "4.5", quote = "An administrator may need to terminate [...] all of the user's current sessions")
    public void testEndUserSessionTwoUsers() throws Exception {
        checkServerSupportCommand(END_USER_SESSION);
        checkServerSupportCommand(GET_LIST_OF_ACTIVE_USERS);

        final Jid testUserOne = JidCreate.bareFrom("endsessiontest-one-" + testRunId + "@example.org");
        final Jid testUserTwo = JidCreate.bareFrom("endsessiontest-two-" + testRunId + "@example.org");
        AbstractXMPPConnection userConnectionOne = null;
        AbstractXMPPConnection userConnectionTwo = null;
        try {
            createUser(testUserOne);
            createUser(testUserTwo);

            // Login as the user to be able to end their session
            userConnectionOne = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
            userConnectionTwo = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
            userConnectionOne.connect();
            userConnectionTwo.connect();
            userConnectionOne.login(testUserOne.getLocalpartOrThrow().toString(), "password");
            userConnectionTwo.login(testUserTwo.getLocalpartOrThrow().toString(), "password");

            final SimpleResultSyncPoint isOneDisconnected = new SimpleResultSyncPoint();
            userConnectionOne.addConnectionListener(new ConnectionListener() {
                @Override
                public void connectionClosed() {
                    isOneDisconnected.signal();
                }

                @Override
                public void connectionClosedOnError(Exception e) {
                    isOneDisconnected.signal();
                }
            });

            final SimpleResultSyncPoint isTwoDisconnected = new SimpleResultSyncPoint();
            userConnectionTwo.addConnectionListener(new ConnectionListener() {
                @Override
                public void connectionClosed() {
                    isTwoDisconnected.signal();
                }

                @Override
                public void connectionClosedOnError(Exception e) {
                    isTwoDisconnected.signal();
                }
            });

            // End the user's sessions
            AdHocCommandData result = executeCommandWithArgs(END_USER_SESSION, adminConnection.getUser().asEntityBareJid(),
                "accountjids", userConnectionOne.getUser().asBareJid().toString() + "," + userConnectionTwo.getUser().asBareJid().toString()
            );

            assertEquals(AdHocCommandData.Status.completed, result.getStatus(), "Expected the status of the " + END_USER_SESSION + "command that was executed by '" + adminConnection.getUser() + " to represent that the command is done executing (but it does not).");
            assertResult(isOneDisconnected, "Expected the connection of '" + userConnectionOne.getUser() + "' to be disconnected after '" + adminConnection.getUser() + "' invoked the " + END_USER_SESSION + " ad-hoc command using the a list of targets that included this one (but the connection remains connected).");
            assertResult(isTwoDisconnected, "Expected the connection of '" + userConnectionTwo.getUser() + "' to be disconnected after '" + adminConnection.getUser() + "' invoked the " + END_USER_SESSION + " ad-hoc command using the a list of targets that included this one (but the connection remains connected).");
        } finally {
            if (userConnectionOne != null && userConnectionOne.isConnected()) {
                userConnectionOne.disconnect();
            }
            if (userConnectionTwo != null && userConnectionTwo.isConnected()) {
                userConnectionTwo.disconnect();
            }
            deleteUser(testUserOne);
            deleteUser(testUserTwo);
        }
    }

    // No s4.6 test - retracted

    //node="http://jabber.org/protocol/admin#change-user-password" name="Change User Password"
    @SmackIntegrationTest(section = "4.7")
    public void testChangePassword() throws Exception {
        checkServerSupportCommand(CHANGE_USER_PASSWORD);
        // Setup test fixture.
        final Jid userToChangePassword = JidCreate.bareFrom("changepasswordtest" + testRunId + "@example.org");
        try {
            createUser(userToChangePassword);
            AdHocCommandData result = executeCommandWithArgs(CHANGE_USER_PASSWORD, adminConnection.getUser().asEntityBareJid(),
                "accountjid", userToChangePassword.toString(),
                "password", "password2"
            );

            if (result.getNotes().get(0).getType() != AdHocCommandNote.Type.info) {
                throw new IllegalStateException("Bug in test implementation: problem while provisioning test user.");
            }

            // Execute system under test.
            AbstractXMPPConnection userConnection = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
            userConnection.connect();

            // Verify results.
            assertDoesNotThrow(() -> userConnection.login(userToChangePassword.getLocalpartOrThrow().toString(), "password2"));
        } finally {
            // Tear down test fixture.
            deleteUser(userToChangePassword);
        }
    }

    //node="http://jabber.org/protocol/admin#get-user-roster" name="Get User Roster"
    @SmackIntegrationTest(section = "4.8")
    public void testUserRoster() throws Exception {
        checkServerSupportCommand(GET_USER_ROSTER);

        // Setup test fixture.
        IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(conOne, conTwo, 10000);

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(GET_USER_ROSTER, adminConnection.getUser().asEntityBareJid(),
            "accountjids", conOne.getUser().asEntityBareJidString()
        );

        // Verify results.
        assertFormFieldJidEquals("accountjids", Collections.singleton(conOne.getUser().asEntityBareJid()), result);
        List<Element> elements = result.getForm().getExtensionElements();
        assertEquals(1, elements.size());
        assertTrue(elements.get(0) instanceof RosterPacket);
        RosterPacket roster = (RosterPacket) elements.get(0);
        assertTrue(roster.getRosterItems().stream().anyMatch(item -> item.getJid().equals(conTwo.getUser().asEntityBareJid())));

        // Tear down test fixture.
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);
    }

    @SmackIntegrationTest(section = "4.9")
    public void testGetUserLastLoginTime() throws Exception {
        checkServerSupportCommand(GET_USER_LAST_LOGIN_TIME);

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(GET_USER_LAST_LOGIN_TIME, adminConnection.getUser().asEntityBareJid(),
            "accountjids", conOne.getUser().asEntityBareJidString()
        );

        // Verify results.
        assertFormFieldExists("lastlogin", result);
        assertFormFieldHasValues("lastlogin", 1, result);
        FormField field = result.getForm().getField("lastlogin");
        try {
            Date lastLogin = field.getFirstValueAsDate();
            ZonedDateTime lastLoginTime = ZonedDateTime.ofInstant(lastLogin.toInstant(), ZoneId.systemDefault());
            assertTrue(lastLoginTime.isAfter(ZonedDateTime.now().minusMinutes(10)));
        } catch (ParseException e) {
            // Do nothing here, since the field only SHOULD be in the format specified by XEP-0082
            // Let a non-parsing exception bubble up.
        }
    }

    @SmackIntegrationTest(section = "4.10")
    public void testGetUserStatistics() throws Exception {
        checkServerSupportCommand(GET_USER_STATISTICS);

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(GET_USER_STATISTICS, adminConnection.getUser().asEntityBareJid(),
            "accountjids", conOne.getUser().asEntityBareJidString()
        );

        // Verify results.
        assertFormFieldCountAtLeast(1, result);
        // Which stats a server should support isn't defined, so we can't check for specific fields or values.
        // Instead, we assume that supporting the command means that the server will return at least one field.
    }

    //node="http://jabber.org/protocol/admin#edit-blacklist" name="Edit Blocked List"
    @SmackIntegrationTest(section = "4.11")
    public void testEditBlackList() throws Exception {
        checkServerSupportCommand(EDIT_BLOCKED_LIST);
        final String blacklistDomain = "xmpp.someotherdomain.org";
        try {
            // Setup test fixture.

            // Execute system under test: Pretend it's a 1-stage command initially, so that we can check that the current list of Blocked Users is populated
            AdHocCommandData result = executeCommandSimple(EDIT_BLOCKED_LIST, adminConnection.getUser().asEntityBareJid());

            // Verify results.
            assertFormFieldHasValues("blacklistjids", 0, result);

            // Execute system under test: Run the full 2-stage command to alter the Blocklist.
            result = executeCommandWithArgs(EDIT_BLOCKED_LIST, adminConnection.getUser().asEntityBareJid(),
                "blacklistjids", blacklistDomain
            );

            // Verify Results.
            assertNoteType(AdHocCommandNote.Type.info, result);
            assertNoteContains("Operation finished successfully", result);

            // Pretend it's a 1-stage command again, so that we can check that the new list of Blocked Users is correct.
            result = executeCommandSimple(EDIT_BLOCKED_LIST, adminConnection.getUser().asEntityBareJid());
            assertFormFieldEquals("blacklistjids", blacklistDomain, result);

        } finally {
            // Tear down test fixture.
            executeCommandWithArgs(EDIT_BLOCKED_LIST, adminConnection.getUser().asEntityBareJid(),
                "blacklistjids", ""
            );
        }
    }

    //node="http://jabber.org/protocol/admin#edit-whitelist" name="Edit Allowed List"
    @SmackIntegrationTest(section = "4.12")
    public void testEditWhiteList() throws Exception {
        checkServerSupportCommand(EDIT_ALLOWED_LIST);
        final String whitelistDomain = "xmpp.someotherdomain.org";
        try {
            // Setup test fixture.

            // Execute system under test: Pretend it's a 1-stage command initially, so that we can check that the current list of Allowed Users is populated
            AdHocCommandData result = executeCommandSimple(EDIT_ALLOWED_LIST, adminConnection.getUser().asEntityBareJid());

            // Verify results.
            assertFormFieldHasValues("whitelistjids", 0, result);

            // Execute system under test: Run the full 2-stage command to alter the Whitelist.
            result = executeCommandWithArgs(EDIT_ALLOWED_LIST, adminConnection.getUser().asEntityBareJid(),
                "whitelistjids", whitelistDomain
            );

            // Verify Results.
            assertNoteType(AdHocCommandNote.Type.info, result);
            assertNoteContains("Operation finished successfully", result);

            // Pretend it's a 1-stage command again, so that we can check that the new list of Allowed Users is correct.
            result = executeCommandSimple(EDIT_ALLOWED_LIST, adminConnection.getUser().asEntityBareJid());
            assertFormFieldEquals("whitelistjids", whitelistDomain, result);

        } finally {
            // Tear down test fixture.
            executeCommandWithArgs(EDIT_ALLOWED_LIST, adminConnection.getUser().asEntityBareJid(),
                "whitelistjids", ""
            );
        }
    }

    //node="http://jabber.org/protocol/admin#get-registered-users-num" name="Get Number of Registered Users"
    @SmackIntegrationTest(section = "4.13")
    public void testGetRegisteredUsersNumber() throws Exception {
        checkServerSupportCommand(GET_NUMBER_OF_REGISTERED_USERS);

        // Execute system under test.
        AdHocCommandData result = executeCommandSimple(GET_NUMBER_OF_REGISTERED_USERS, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        final int expectedMinimumCount = 3; // Each test runs with at least three registered test accounts (but more users might be active!)
        assertTrue(Integer.parseInt(result.getForm().getField("registeredusersnum").getFirstValue()) >= expectedMinimumCount);
    }

    //node="http://jabber.org/protocol/admin#get-disabled-users-num" name="Get Number of Disabled Users"
    @SmackIntegrationTest(section = "4.14")
    public void testDisabledUsersNumber() throws Exception {
        checkServerSupportCommand(GET_NUMBER_OF_DISABLED_USERS);
        checkServerSupportCommand(REENABLE_A_USER);
        checkServerSupportCommand(DISABLE_A_USER);

        // Setup test fixture.
        final Jid disabledUser = JidCreate.bareFrom("disableusernumtest" + testRunId + "@example.org");
        try {
            // Create and disable a user
            createUser(disabledUser);
            executeCommandWithArgs(DISABLE_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjids", disabledUser.toString()
            );

            // Execute system under test.
            AdHocCommandData result = executeCommandSimple(GET_NUMBER_OF_DISABLED_USERS, adminConnection.getUser().asEntityBareJid());

            // Verify results.
            assertTrue(Integer.parseInt(result.getForm().getField("disabledusersnum").getFirstValue()) >= 1);
        } finally {
            // Tear down test fixture.
            executeCommandWithArgs(REENABLE_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjids", disabledUser.toString()
            );
            deleteUser(disabledUser);
        }
    }

    //node="http://jabber.org/protocol/admin#get-online-users-num" name="Get Number of Online Users"
    @SmackIntegrationTest(section = "4.15")
    public void testGetOnlineUsersNumber() throws Exception {
        checkServerSupportCommand(GET_NUMBER_OF_ONLINE_USERS);

        // Execute system under test.
        DataForm form = executeCommandSimple(GET_NUMBER_OF_ONLINE_USERS, adminConnection.getUser().asEntityBareJid()).getForm();

        // Verify results.
        final int expectedMinimumCount = 3; // Each test runs with at least three test accounts (but more users might be active!)
        assertTrue(Integer.parseInt(form.getField("onlineusersnum").getFirstValue()) >= expectedMinimumCount);
    }

    //node="http://jabber.org/protocol/admin#get-active-users-num" name="Get Number of Active Users"
    @SmackIntegrationTest(section = "4.16")
    public void testGetActiveUsersNumber() throws Exception {
        checkServerSupportCommand(GET_NUMBER_OF_ACTIVE_USERS);

        // Execute system under test.
        DataForm form = executeCommandSimple(GET_NUMBER_OF_ACTIVE_USERS, adminConnection.getUser().asEntityBareJid()).getForm();

        // Verify results.
        final int expectedMinimumCount = 3; // Each test runs with at least three test accounts (but more users might be active!)
        assertTrue(Integer.parseInt(form.getField("activeusersnum").getFirstValue()) >= expectedMinimumCount);
    }

    //node="http://jabber.org/protocol/admin#get-idle-users-num" name="Get Number of Idle Users"
    @SmackIntegrationTest(section = "4.17")
    public void testGetIdleUsersNumber() throws Exception {
        checkServerSupportCommand(GET_NUMBER_OF_IDLE_USERS);

        // Execute system under test.
        AdHocCommandData result = executeCommandSimple(GET_NUMBER_OF_IDLE_USERS, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        assertTrue(Integer.parseInt(result.getForm().getField("idleusersnum").getFirstValue()) >= 0);
    }

    //node="http://jabber.org/protocol/admin#get-registered-users-list" name="Get List of Registered Users"
    @SmackIntegrationTest(section = "4.18")
    public void testGetRegisteredUsersList() throws Exception {
        checkServerSupportCommand(GET_LIST_OF_REGISTERED_USERS);

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(GET_LIST_OF_REGISTERED_USERS, adminConnection.getUser().asEntityBareJid(),
            "max_items", "25");

        // Verify results.
        final Collection<Jid> expectedRegisteredUsers = Arrays.asList(
            conOne.getUser().asEntityBareJid(),
            conTwo.getUser().asEntityBareJid(),
            conThree.getUser().asEntityBareJid()
        );
        assertFormFieldContainsAll("registereduserjids", expectedRegisteredUsers, result);
    }

    //node="http://jabber.org/protocol/admin#get-disabled-users-list" name="Get List of Disabled Users"
    @SmackIntegrationTest(section = "4.19")
    public void testDisabledUsersListEmpty() throws Exception {
        checkServerSupportCommand(GET_LIST_OF_DISABLED_USERS);

        // Setup test fixture.
        // Nothing to do. Assumes no users are disabled by default (and that other tests tidy up after themselves).

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(GET_LIST_OF_DISABLED_USERS, adminConnection.getUser().asEntityBareJid(),
            "max_items", "25");

        // Verify results.
        assertFormFieldEquals("disableduserjids", new ArrayList<>(), result);
    }

    @SmackIntegrationTest(section = "4.19")
    public void testDisabledUsersList() throws Exception {
        checkServerSupportCommand(GET_LIST_OF_DISABLED_USERS);
        checkServerSupportCommand(DISABLE_A_USER);

        final Jid disabledUser = JidCreate.bareFrom("disableuserlisttest" + testRunId + "@example.org");
        createUser(disabledUser);

        executeCommandWithArgs(DISABLE_A_USER, adminConnection.getUser().asEntityBareJid(),
            "accountjids", disabledUser.toString()
        );

        AdHocCommandData result = executeCommandWithArgs(GET_LIST_OF_DISABLED_USERS, adminConnection.getUser().asEntityBareJid(),
            "max_items", "25");

        assertFormFieldJidEquals("disableduserjids", Collections.singleton(disabledUser), result);

        //Clean-up
        deleteUser(disabledUser);
    }

    //node="http://jabber.org/protocol/admin#get-online-users-list" name="Get List of Online Users"
    @SmackIntegrationTest(section = "4.20")
    public void testGetOnlineUsersListSimple() throws Exception {
        checkServerSupportCommand(GET_LIST_OF_ONLINE_USERS);

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(GET_LIST_OF_ONLINE_USERS, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        final Collection<Jid> expectedOnlineUsers = Arrays.asList(
            conOne.getUser().asEntityBareJid(),
            conTwo.getUser().asEntityBareJid(),
            conThree.getUser().asEntityBareJid()
        );
        assertFormFieldContainsAll("onlineuserjids", expectedOnlineUsers, result);
    }

    //node="http://jabber.org/protocol/admin#get-active-users" name="Get List of Active Users"
    @SmackIntegrationTest(section = "4.21")
    public void testGetActiveUsersListSimple() throws Exception {
        checkServerSupportCommand(GET_LIST_OF_ACTIVE_USERS);

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(GET_LIST_OF_ACTIVE_USERS, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        final Collection<Jid> expectedActiveUsers = Arrays.asList(
            conOne.getUser().asEntityBareJid(),
            conTwo.getUser().asEntityBareJid(),
            conThree.getUser().asEntityBareJid()
        );
        assertFormFieldContainsAll("activeuserjids", expectedActiveUsers, result);
    }

    @SmackIntegrationTest(section = "4.21")
    public void testGetOnlineUsersListWithMaxUsers() throws Exception {
        checkServerSupportCommand(GET_LIST_OF_ACTIVE_USERS);

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(GET_LIST_OF_ACTIVE_USERS, adminConnection.getUser().asEntityBareJid(),
            "max_items", "25");

        // Verify results.
        final Collection<Jid> expectedActiveUsers = Arrays.asList(
            conOne.getUser().asEntityBareJid(),
            conTwo.getUser().asEntityBareJid(),
            conThree.getUser().asEntityBareJid()
        );
        assertFormFieldContainsAll("activeuserjids", expectedActiveUsers, result);
    }

    //node="http://jabber.org/protocol/admin#get-idle-users" name="Get List of Idle Users"
    @SmackIntegrationTest(section = "4.22")
    public void testGetIdleUsersList() throws Exception {
        checkServerSupportCommand(GET_LIST_OF_IDLE_USERS);
        conOne.sendStanza(PresenceBuilder.buildPresence().ofType(Presence.Type.unavailable).setMode(Presence.Mode.away).build());

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(GET_LIST_OF_IDLE_USERS, adminConnection.getUser().asEntityBareJid());
        System.out.println(result);

        // Verify results.
        final Collection<String> expectedIdleUsers = Collections.singletonList(
            conOne.getUser().asEntityBareJid().toString()
        );

        assertFormFieldEquals("activeuserjids", expectedIdleUsers, result);
    }

    //node="http://jabber.org/protocol/admin#announce" name="Send Announcement to Online Users"
    @SmackIntegrationTest(section = "4.23")
    public void testSendAnnouncementToOnlineUsers() throws Exception {
        checkServerSupportCommand(SEND_ANNOUNCEMENT_TO_ONLINE_USERS);
        // Setup test fixture.
        final String announcement = "testAnnouncement" + testRunId;
        final SimpleResultSyncPoint syncPoint = new SimpleResultSyncPoint();

        StanzaListener stanzaListener = stanza -> {
            if (stanza instanceof Message) {
                Message message = (Message) stanza;
                if (message.getBody().contains(announcement)) {
                    syncPoint.signal();
                }
            }
        };

        adminConnection.addSyncStanzaListener(stanzaListener, stanza -> true);

        try {
            // Execute system under test.
            AdHocCommandData result = executeCommandWithArgs(SEND_ANNOUNCEMENT_TO_ONLINE_USERS, adminConnection.getUser().asEntityBareJid(),
                "announcement", announcement
            );
            syncPoint.waitForResult(timeout);

            // Verify results.
            assertNoteType(AdHocCommandNote.Type.info, result);
            assertNoteContains("Operation finished successfully", result);
        }
        finally {
            // Tear down test fixture.
            adminConnection.removeSyncStanzaListener(stanzaListener);
        }
    }

    //node="http://jabber.org/protocol/admin#set-motd" name="Set Message of the Day"
    @SmackIntegrationTest(section = "4.24")
    public void testSetMOTD() throws Exception {
        checkServerSupportCommand(SET_MOTD);
        checkServerSupportCommand(EDIT_MOTD); // Used in validation

        final Collection<String> newMOTD = Arrays.asList(
            "This is MOTD 1",
            "This is MOTD 2"
        );

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(
            SET_MOTD,
            adminConnection.getUser().asEntityBareJid(),
            "motd",
            String.join(",", newMOTD)
        );

        // Verify results.
        assertSame(AdHocCommandData.Status.completed, result.getStatus());

        // Check value using the edit form
        result = executeCommandSimple(EDIT_MOTD, adminConnection.getUser().asEntityBareJid());
        assertFormFieldEquals("motd", newMOTD, result);
    }

    //node="http://jabber.org/protocol/admin#edit-motd" name="Edit Message of the Day"
    @SmackIntegrationTest(section = "4.25")
    public void testEditMOTD() throws Exception {
        checkServerSupportCommand(EDIT_MOTD);

        final Collection<String> newMOTD = Arrays.asList(
            "This is MOTD A",
            "This is MOTD B"
        );

        // Execute system under test: Pretend it's a 1-stage command initially, so that we can check the current MOTD form
        AdHocCommandData result = executeCommandSimple(EDIT_MOTD, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        assertFormFieldExists("motd", result);

        // Execute system under test: Now run the full thing
        result = executeCommandWithArgs(
            EDIT_MOTD,
            adminConnection.getUser().asEntityBareJid(),
            "motd",
            String.join(",", newMOTD)
        );

        // Verify results.
        assertSame(AdHocCommandData.Status.completed, result.getStatus());

        // Pretend it's a 1-stage command again, so that we can check that the new MOTD is correct.
        result = executeCommandSimple(EDIT_MOTD, adminConnection.getUser().asEntityBareJid());
        assertFormFieldEquals("motd", newMOTD, result);
    }

    //node="http://jabber.org/protocol/admin#delete-motd" name="Delete Message of the Day"
    @SmackIntegrationTest(section = "4.26")
    public void testDeleteMOTD() throws Exception {
        checkServerSupportCommand(DELETE_MOTD);
        checkServerSupportCommand(EDIT_MOTD); // Used in validation

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(
            DELETE_MOTD,
            adminConnection.getUser().asEntityBareJid());

        // Verify results.
        assertSame(AdHocCommandData.Status.completed, result.getStatus());

        // Check value using the edit form
        result = executeCommandSimple(EDIT_MOTD, adminConnection.getUser().asEntityBareJid());
        assertFormFieldEquals("motd", List.of(), result);
    }

    //node="http://jabber.org/protocol/admin#set-welcome" name="Set Welcome Message"
    @SmackIntegrationTest(section = "4.27")
    public void testSetWelcome() throws Exception {
        checkServerSupportCommand(SET_WELCOME_MESSAGE);

        final Collection<String> newWelcomeMessage = Arrays.asList(
            "Line 1 of welcome message",
            "Line 2 of welcome message"
        );

        // Execute system under test: Pretend it's a 1-stage command initially, so that we can check the current Welcome Message form
        AdHocCommandData result = executeCommandSimple(SET_WELCOME_MESSAGE, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        assertFormFieldExists("welcome", result);

        // Execute system under test: Now run the full thing
        result = executeCommandWithArgs(
            SET_WELCOME_MESSAGE,
            adminConnection.getUser().asEntityBareJid(),
            "welcome",
            String.join(",", newWelcomeMessage)
        );

        // Verify results.
        assertSame(AdHocCommandData.Status.completed, result.getStatus());

        // Pretend it's a 1-stage command again, so that we can check that the new welcome message is correct.
        result = executeCommandSimple(SET_WELCOME_MESSAGE, adminConnection.getUser().asEntityBareJid());
        assertFormFieldEquals("welcome", newWelcomeMessage, result);
    }

    //node="http://jabber.org/protocol/admin#delete-welcome" name="Delete Welcome Message"
    @SmackIntegrationTest(section = "4.28")
    public void testDeleteWelcome() throws Exception {
        checkServerSupportCommand(DELETE_WELCOME_MESSAGE);
        checkServerSupportCommand(SET_WELCOME_MESSAGE); // Used for validation

        // Execute system under test.
        AdHocCommandData result = executeCommandSimple(DELETE_WELCOME_MESSAGE, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        assertSame(AdHocCommandData.Status.completed, result.getStatus());

        // Use Set Welcome Message form to check the value
        result = executeCommandSimple(SET_WELCOME_MESSAGE, adminConnection.getUser().asEntityBareJid());
        assertFormFieldEquals("welcome", List.of(), result);
    }

    //node="http://jabber.org/protocol/admin#edit-admin" name="Edit Admin List"
    @SmackIntegrationTest(section = "4.29")
    public void testEditAdminList() throws Exception {
        checkServerSupportCommand(EDIT_ADMIN_LIST);
        final Jid adminToAdd = JidCreate.bareFrom("editadminlisttest" + testRunId + "@example.org");
        try {
            // Setup test fixture.
            createUser(adminToAdd);

            // Execute system under test: Pretend it's a 1-stage command initially, so that we can check that the current list of Admins is populated
            AdHocCommandData result = executeCommandSimple(EDIT_ADMIN_LIST, adminConnection.getUser().asEntityBareJid());

            // Verify results.
            assertFormFieldEquals("adminjids", adminConnection.getUser().asEntityBareJid(), result);

            // Execute system under test: Run the full 2-stage command to alter the list of Admins
            result = executeCommandWithArgs(EDIT_ADMIN_LIST, adminConnection.getUser().asEntityBareJid(),
                "adminjids", adminConnection.getUser().asEntityBareJidString() + "," + adminToAdd
            );

            // Verify results.
            assertNoteType(AdHocCommandNote.Type.info, result);
            assertNoteContains("Operation finished successfully", result);

            // Execute system under test: Pretend it's a 1-stage command again, so that we can check that the new list of Admins is correct
            result = executeCommandSimple(EDIT_ADMIN_LIST, adminConnection.getUser().asEntityBareJid());

            // Verify results.
            assertFormFieldJidEquals("adminjids", new HashSet<>(Arrays.asList(
                adminConnection.getUser().asEntityBareJid(),
                adminToAdd
            )), result);
        } finally {
            // Tear down test fixture.
            deleteUser(adminToAdd);
            executeCommandWithArgs(EDIT_ADMIN_LIST, adminConnection.getUser().asEntityBareJid(),
                "adminjids", adminConnection.getUser().asEntityBareJidString()
            );
        }
    }

    //node="http://jabber.org/protocol/admin#restart" name="Restart Service"
    @SmackIntegrationTest(section = "4.30")
    public void testRestartServiceNoParams() throws Exception {
        checkServerSupportCommand(RESTART_SERVICE);

        // Execute system under test: Pretend it's a 1-stage command initially, so that we can check the current Welcome Message form
        AdHocCommandData result = executeCommandSimple(RESTART_SERVICE, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        assertFormFieldExists("delay", result);
        assertFormFieldExists("announcement", result);

        // No actual execution of the command, as that would be rather disruptive to other tests...
    }

    //node="http://jabber.org/protocol/admin#shutdown" name="Shut Down Service"
    @SmackIntegrationTest(section = "4.31")
    public void testShutdownServiceNoParams() throws Exception {
        checkServerSupportCommand(SHUTDOWN_SERVICE);

        // Execute system under test: Pretend it's a 1-stage command initially, so that we can check the current Welcome Message form
        AdHocCommandData result = executeCommandSimple(SHUTDOWN_SERVICE, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        assertFormFieldExists("delay", result);
        assertFormFieldExists("announcement", result);

        // No actual execution of the command, as that would be rather disruptive to other tests...
    }
}
