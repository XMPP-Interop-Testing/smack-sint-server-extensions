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
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.FlexibleStanzaTypeFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.rsm.packet.RSMSet;
import org.jivesoftware.smackx.xdata.FormField;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

    private void tryDeleteUser(String jid) throws Exception {
        try {
            executeCommandWithArgs(DELETE_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjids", jid
            );
        } catch (XMPPException e) {
            // Ignore
        }
    }

    private void tryDeleteUser(Jid jid) throws Exception {
        tryDeleteUser(jid.toString());
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
        assertFalse(items.isEmpty(), "Expected '" + adminConnection.getUser() +"' (an administrator) to be able to retrieve a non-empty 'command list' using service discovery (but the command list was empty).");
    }

    //node="http://jabber.org/protocol/admin#add-user" name="Add a User"
    @SmackIntegrationTest(section = "4.1", quote = "Adding a user MUST result in the creation of an account [...] The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#add-user\".")
    public void testAddUser() throws Exception {
        checkServerSupportCommand(ADD_A_USER);
        // Setup test fixture.
        final Jid addedUser = JidCreate.bareFrom(Localpart.from("addusertest-" + StringUtils.randomString(5)), connection.getXMPPServiceDomain());
        try {
            // Execute system under test.
            AdHocCommandData result = executeCommandWithArgs(ADD_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjid", addedUser.toString(),
                "password", "password",
                "password-verify", "password"
            );

            // Verify results.
            assertCommandCompletedSuccessfully(result, "Expected response to the " + ADD_A_USER + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");

            try {
                AbstractXMPPConnection userConnection = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
                userConnection.connect();
                userConnection.login(addedUser.getLocalpartOrThrow().toString(), "password");
                assertTrue(userConnection.isAuthenticated(), "Expected to be able to connect and login with user '" + addedUser + "', that was created by '" + adminConnection.getUser() + "' using the " + ADD_A_USER + " command. However, authentication failed.");
            } catch (Exception e) {
                fail("Expected to be able to connect and login with user '" + addedUser + "', that was created by '" + adminConnection.getUser() + "' using the " + ADD_A_USER + " command. However, authentication failed.", e);
            }
        } finally {
            // Tear down test fixture.
            tryDeleteUser(addedUser);
        }
    }

    @SmackIntegrationTest(section = "4.1", quote = "Adding a user MUST result in the creation of an account [...] The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#add-user\".")
    public void testAddUserWithMismatchedPassword() throws Exception {
        checkServerSupportCommand(ADD_A_USER);
        // Setup test fixture.
        final Jid newUser = JidCreate.bareFrom(Localpart.from("addusermismatchedpasswordtest-" + StringUtils.randomString(5)), connection.getXMPPServiceDomain());
        try {
            // Execute system under test.
            AdHocCommandData result = executeCommandWithArgs(ADD_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjid", newUser.toString(),
                "password", "password",
                "password-verify", "password2"
            );

            // Verify results.
            assertCommandFailed(result, "Expected response to the " + ADD_A_USER + " command that was executed by '" + adminConnection.getUser() + "' to represent failure (but it does not), as the two provided password values were not equal to each-other.");
        } finally {
            // Tear down test fixture.
            tryDeleteUser(newUser);
        }
    }

    @SmackIntegrationTest(section = "4.1", quote = "Adding a user MUST result in the creation of an account [...] The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#add-user\".")
    public void testAddUserWithRemoteJid() throws Exception {
        checkServerSupportCommand(ADD_A_USER);
        // Setup test fixture.
        final Jid newUser = JidCreate.bareFrom("adduserinvalidjidtest-" + StringUtils.randomString(5) + "@somewhereelse.org");
        try {
            // Execute system under test.
            AdHocCommandData result = executeCommandWithArgs(ADD_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjid", newUser.toString(),
                "password", "password",
                "password-verify", "password2"
            );

            // Verify results.
            assertCommandFailed(result, "Expected response to the " + ADD_A_USER + " command that was executed by '" + adminConnection.getUser() + "' to represent failure (but it does not) as the provided 'accountjid' value is not an address of the local XMPP domain.");
        } finally {
            // Tear down test fixture.
            tryDeleteUser(newUser);
        }
    }

    @SmackIntegrationTest(section = "4.1", quote = "Adding a user MUST result in the creation of an account [...] The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#add-user\".")
    public void testAddUserWithInvalidJid() throws Exception {
        checkServerSupportCommand(ADD_A_USER);
        // Setup test fixture.
        final String newUserInvalidJid = "adduserinvalidjidtest-" + StringUtils.randomString(5) + "@invalid@domain";
        try {
            // Execute system under test.
            AdHocCommandData result = executeCommandWithArgs(ADD_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjid", newUserInvalidJid,
                "password", "password",
                "password-verify", "password2"
            );

            // Verify results.
            assertCommandFailed(result, "Expected response to the " + ADD_A_USER + " command that was executed by '" + adminConnection.getUser() + "' to represent failure (but it does not) as the provided 'accountjid' value is not a valid JID.");
        } finally {
            // Tear down test fixture.
            tryDeleteUser(newUserInvalidJid);
        }
    }

    //node="http://jabber.org/protocol/admin#delete-user" name="Delete a User"
    @SmackIntegrationTest(section = "4.2", quote = "An administrator may need to permanently delete a user account. [...] The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#delete-user\".")
    public void testDeleteUser() throws Exception {
        checkServerSupportCommand(DELETE_A_USER);
        // Setup test fixture.
        final Jid deletedUser = JidCreate.bareFrom(Localpart.from("deleteusertest-" + StringUtils.randomString(5)), connection.getXMPPServiceDomain());
        createUser(deletedUser);

        try {
            // Execute system under test.
            AdHocCommandData result = executeCommandWithArgs(DELETE_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjids", deletedUser.toString()
            );

            // Verify results.
            assertCommandCompletedSuccessfully(result, "Expected response to the " + DELETE_A_USER + " command (targeting a user by bare JID) that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");
        } finally {
            // Tear down test fixture.
            tryDeleteUser(deletedUser);
        }
    }

    @SmackIntegrationTest(section = "4.2", quote = "An administrator may need to permanently delete a user account. [...] The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#delete-user\".")
    public void testDeleteUserWithFullJid() throws Exception {
        checkServerSupportCommand(DELETE_A_USER);
        // Setup test fixture.
        final Jid deletedUser = JidCreate.bareFrom(Localpart.from("deleteusertest2-" + StringUtils.randomString(5)), connection.getXMPPServiceDomain());
        createUser(deletedUser);

        try {
            // Execute system under test.
            AdHocCommandData result = executeCommandWithArgs(DELETE_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjids", deletedUser.toString() + "/resource"
            );

            // Verify results.
            assertCommandCompletedSuccessfully(result, "Expected response to the " + DELETE_A_USER + " command (targeting a user by full JID) that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");
            // Although https://xmpp.org/extensions/xep-0133.html#delete-user specifies that the client should send the bare
            // JID, there's no error handling specified for the case where the full JID is sent, and so it's expected that
            // the server should handle it gracefully.
        } finally {
            // Tear down test fixture.
            tryDeleteUser(deletedUser);
        }
    }

    //node="http://jabber.org/protocol/admin#disable-user" name="Disable a User"
    @SmackIntegrationTest(section = "4.3", quote = "An administrator may need to temporarily disable a user account. Disabling a user MUST result in the termination of any active sessions for the user and in the prevention of further user logins until the account is re-enabled")
    public void testDisableUser() throws Exception {
        checkServerSupportCommand(DISABLE_A_USER);
        // Setup test fixture.
        final Jid disabledUser = JidCreate.bareFrom(Localpart.from("disableusertest-" + StringUtils.randomString(5)), connection.getXMPPServiceDomain());
        try {
            createUser(disabledUser);

            // Execute system under test.
            AdHocCommandData result = executeCommandWithArgs(DISABLE_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjids", disabledUser.toString()
            );

            // Verify results.
            assertCommandCompletedSuccessfully(result, "Expected response to the " + DISABLE_A_USER + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");
        } finally {
            // Tear down test fixture.
            tryDeleteUser(disabledUser);
        }
    }

    @SmackIntegrationTest(section = "4.3", quote = "An administrator may need to temporarily disable a user account. Disabling a user MUST result in the termination of any active sessions for the user and in the prevention of further user logins until the account is re-enabled")
    public void testDisableUserGetsSessionsTerminated() throws Exception {
        checkServerSupportCommand(DISABLE_A_USER);
        // Setup test fixture.
        final Jid disabledUser = JidCreate.bareFrom(Localpart.from("disableusertest-" + StringUtils.randomString(5)), connection.getXMPPServiceDomain());
        AbstractXMPPConnection userConnectionOne = null;
        AbstractXMPPConnection userConnectionTwo = null;
        try {
            createUser(disabledUser);

            // Login as the user to be able to see their sessions being ended
            userConnectionOne = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
            userConnectionTwo = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
            userConnectionOne.connect();
            userConnectionTwo.connect();
            userConnectionOne.login(disabledUser.getLocalpartOrThrow().toString(), "password", Resourcepart.from("resource-one-" + StringUtils.randomString(5)));
            userConnectionTwo.login(disabledUser.getLocalpartOrThrow().toString(), "password", Resourcepart.from("resource-two-" + StringUtils.randomString(5)));

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

            // Execute system under test.
            executeCommandWithArgs(DISABLE_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjids", disabledUser.toString()
            );

            // Verify results.
            assertResult(isOneDisconnected, "Expected the connection of '" + userConnectionOne.getUser() + "' to be disconnected after '" + adminConnection.getUser() + "' invoked the " + DISABLE_A_USER + " ad-hoc command using the target's bare JID (but the connection remains connected).");
            assertResult(isTwoDisconnected, "Expected the connection of '" + userConnectionTwo.getUser() + "' to be disconnected after '" + adminConnection.getUser() + "' invoked the " + DISABLE_A_USER + " ad-hoc command using the target's bare JID (but the connection remains connected).");
        } finally {
            // Tear down test fixture.
            if (userConnectionOne != null && userConnectionOne.isConnected()) {
                userConnectionOne.disconnect();
            }
            if (userConnectionTwo != null && userConnectionTwo.isConnected()) {
                userConnectionTwo.disconnect();
            }
            tryDeleteUser(disabledUser);
        }
    }

    @SmackIntegrationTest(section = "4.3", quote = "An administrator may need to temporarily disable a user account. Disabling a user MUST result in [...] the prevention of further user logins until the account is re-enabled")
    public void testDisableUserCantLogin() throws Exception {
        checkServerSupportCommand(DISABLE_A_USER);

        // Setup test fixture.
        AbstractXMPPConnection userConnectionOne = null;
        final Jid disabledUser = JidCreate.bareFrom(Localpart.from("disableusertest-" + StringUtils.randomString(5)), connection.getXMPPServiceDomain());
        try {
            createUser(disabledUser);

            // Execute system under test.
            executeCommandWithArgs(DISABLE_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjids", disabledUser.toString()
            );

            // Verify results.
            userConnectionOne = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
            userConnectionOne.connect();
            AbstractXMPPConnection finalUserConnectionOne = userConnectionOne;
            assertThrows(SASLErrorException.class, () -> finalUserConnectionOne.login(disabledUser.getLocalpartOrThrow().toString(), "password", Resourcepart.from("resource-one-" + StringUtils.randomString(5))), "Expected '" + disabledUser + "' to not be able to login after their account was disabled by '" + adminConnection.getUser() + "' using the '" + DISABLE_A_USER + "' command (but the user was able to login).");
        } finally {
            // Tear down test fixture.
            if (userConnectionOne != null && userConnectionOne.isConnected()) {
                userConnectionOne.disconnect();
            }

            tryDeleteUser(disabledUser);
        }
    }

    @SmackIntegrationTest(section = "4.3", quote = "An administrator may need to temporarily disable a user account. Disabling a user [...] MUST NOT result in the destruction of any implementation-specific data for the account (e.g., database entries or a roster file)")
    public void testDisableUserDoesntAffectRoster() throws Exception {
        checkServerSupportCommand(DISABLE_A_USER);
        checkServerSupportCommand(GET_USER_ROSTER); // Used in assertion.

        // Setup test fixture.
        AbstractXMPPConnection userConnectionOne = null;
        final Jid disabledUser = JidCreate.bareFrom(Localpart.from("disableusertest-" + StringUtils.randomString(5)), connection.getXMPPServiceDomain());
        try {
            createUser(disabledUser);
            userConnectionOne = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
            userConnectionOne.connect();
            userConnectionOne.login(disabledUser.getLocalpartOrThrow().toString(), "password", Resourcepart.from("resource-one-" + StringUtils.randomString(5)));
            final EntityBareJid contactJid = JidCreate.entityBareFrom("foo@bar.example.org");
            final String contactName = "test user";
            Roster.getInstanceFor(userConnectionOne).createItem(contactJid, contactName, null);

            // Execute system under test.
            executeCommandWithArgs(DISABLE_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjids", disabledUser.toString()
            );

            // Verify results.
            AdHocCommandData rosterData = executeCommandWithArgs(GET_USER_ROSTER, adminConnection.getUser().asEntityBareJid(),
                "accountjids", disabledUser.toString()
            );
            List<Element> elements = rosterData.getForm().getExtensionElements();
            RosterPacket roster = (RosterPacket) elements.get(0);
            assertTrue(roster.getRosterItems().stream().anyMatch(item -> item.getJid().equals(contactJid) && item.getName().equals(contactName)), "Expected the roster of the disabled user '" + disabledUser + "' to remain unaffected, but an entry (jid: '" + contactJid + "', name: '" + contactName + "') that was added before the user was disabled does not appear on the roster anymore.");
        } finally {
            // Tear down test fixture.
            if (userConnectionOne != null && userConnectionOne.isConnected()) {
                userConnectionOne.disconnect();
            }

            tryDeleteUser(disabledUser);
        }
    }

    //node="http://jabber.org/protocol/admin#reenable-user" name="Re-Enable a User"
    @SmackIntegrationTest(section = "4.4", quote = "An administrator may need to re-enable a user account that had been temporarily disabled. [...] The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#reenable-user\".")
    public void testReenableUser() throws Exception {
        checkServerSupportCommand(REENABLE_A_USER);
        checkServerSupportCommand(DISABLE_A_USER);

        final Jid disabledUser = JidCreate.entityBareFrom(Localpart.from("reenableusertest-" + StringUtils.randomString(5)), connection.getXMPPServiceDomain());
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
            assertCommandCompletedSuccessfully(result, "Expected response to the " + REENABLE_A_USER + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");
        } finally {
            // Tear down test fixture.
            tryDeleteUser(disabledUser);
        }
    }

    @SmackIntegrationTest(section = "4.4", quote = "An administrator may need to re-enable a user account that had been temporarily disabled. Re-enabling a user MUST result in granting the user the ability to access the service again.")
    public void testReenableUserCanLogin() throws Exception {
        checkServerSupportCommand(REENABLE_A_USER);
        checkServerSupportCommand(DISABLE_A_USER);

        final Jid disabledUser = JidCreate.entityBareFrom(Localpart.from("reenableusertest-" + StringUtils.randomString(5)), connection.getXMPPServiceDomain());
        AbstractXMPPConnection userConnectionOne = null;
        try {
            // Setup test fixture.
            createUser(disabledUser);
            executeCommandWithArgs(DISABLE_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjids", disabledUser.toString()
            );

            // Execute system under test.
            executeCommandWithArgs(REENABLE_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjids", disabledUser.toString()
            );

            // Verify results.
            userConnectionOne = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
            userConnectionOne.connect();
            AbstractXMPPConnection finalUserConnectionOne = userConnectionOne;
            assertDoesNotThrow(() -> finalUserConnectionOne.login(disabledUser.getLocalpartOrThrow().toString(), "password", Resourcepart.from("resource-one-" + StringUtils.randomString(5))), "Expected '" + disabledUser + "' to be able to login after their account was disabled and re-enabled by '" + adminConnection.getUser() + "' using the '" + REENABLE_A_USER + "' command (but the user was not able to login).");
        } finally {
            // Tear down test fixture.
            if (userConnectionOne != null && userConnectionOne.isConnected()) {
                userConnectionOne.disconnect();
            }
            tryDeleteUser(disabledUser);
        }
    }

    @SmackIntegrationTest(section = "4.4", quote = "An administrator may need to re-enable a user account that had been temporarily disabled.")
    public void testReenableNonExistingUser() throws Exception {
        checkServerSupportCommand(REENABLE_A_USER);

        // Setup test fixture.
        final Jid disabledUser = JidCreate.entityBareFrom(Localpart.from("reenablenonexistingusertest-" + StringUtils.randomString(5)), connection.getXMPPServiceDomain());

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(REENABLE_A_USER, adminConnection.getUser().asEntityBareJid(),
            "accountjids", disabledUser.toString()
        );

        // Verify results.
        assertCommandFailed(result, "Expected response to the " + REENABLE_A_USER + " command that was executed by '" + adminConnection.getUser() + "' to represent failure (but it does not) as the targeted user does not exist.");
    }

    @SmackIntegrationTest(section = "4.4", quote = "An administrator may need to re-enable a user account that had been temporarily disabled.")
    public void testReenableRemoteUser() throws Exception {
        checkServerSupportCommand(REENABLE_A_USER);

        // Setup test fixture.
        final Jid disabledUser = JidCreate.entityBareFrom("reenableremoteusertest-" + StringUtils.randomString(5) + "@elsewhere.org");

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(REENABLE_A_USER, adminConnection.getUser().asEntityBareJid(),
            "accountjids", disabledUser.toString()
        );

        // Verify results.
        assertCommandFailed(result, "Expected response to the " + REENABLE_A_USER + " command that was executed by '" + adminConnection.getUser() + "' to represent failure (but it does not) as the targeted user is not a user of the local domain.");
    }

    //node="http://jabber.org/protocol/admin#end-user-session" name="End User Session"
    @SmackIntegrationTest(section = "4.5", quote = "An administrator may need to terminate one [...] of the user's current sessions [...] if the JID is of the form <user@host/resource>, the service MUST end only the session associated with that resource.")
    public void testEndUserSessionFullJid() throws Exception {
        checkServerSupportCommand(END_USER_SESSION);
        checkServerSupportCommand(GET_LIST_OF_ACTIVE_USERS);

        final Jid testUser = JidCreate.bareFrom(Localpart.from("endsessiontest-full-" + StringUtils.randomString(5)), connection.getXMPPServiceDomain());
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

            assertCommandCompletedSuccessfully(result, "Expected response to the " + END_USER_SESSION + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");

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
            tryDeleteUser(testUser);
        }
    }

    @SmackIntegrationTest(section = "4.5", quote = "An administrator may need to terminate [...] all of the user's current sessions [...] If the JID is of the form <user@host>, the service MUST end all of the user's sessions")
    public void testEndUserSessionBareJid() throws Exception {
        checkServerSupportCommand(END_USER_SESSION);
        checkServerSupportCommand(GET_LIST_OF_ACTIVE_USERS);

        final Jid testUser = JidCreate.bareFrom(Localpart.from("endsessiontest-bare-" + StringUtils.randomString(5)), connection.getXMPPServiceDomain());
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

            assertCommandCompletedSuccessfully(result, "Expected response to the " + END_USER_SESSION + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");
            assertResult(isOneDisconnected, "Expected the connection of '" + userConnectionOne.getUser() + "' to be disconnected after '" + adminConnection.getUser() + "' invoked the " + END_USER_SESSION + " ad-hoc command using the target's bare JID (but the connection remains connected).");
            assertResult(isTwoDisconnected, "Expected the connection of '" + userConnectionTwo.getUser() + "' to be disconnected after '" + adminConnection.getUser() + "' invoked the " + END_USER_SESSION + " ad-hoc command using the target's bare JID (but the connection remains connected).");
        } finally {
            if (userConnectionOne != null && userConnectionOne.isConnected()) {
                userConnectionOne.disconnect();
            }
            if (userConnectionTwo != null && userConnectionTwo.isConnected()) {
                userConnectionTwo.disconnect();
            }
            tryDeleteUser(testUser);
        }
    }

    @SmackIntegrationTest(section = "4.5", quote = "An administrator may need to terminate [...] all of the user's current sessions")
    public void testEndUserSessionTwoUsers() throws Exception {
        checkServerSupportCommand(END_USER_SESSION);
        checkServerSupportCommand(GET_LIST_OF_ACTIVE_USERS);

        final Jid testUserOne = JidCreate.bareFrom(Localpart.from("endsessiontest-one-" + StringUtils.randomString(5)), connection.getXMPPServiceDomain());
        final Jid testUserTwo = JidCreate.bareFrom(Localpart.from("endsessiontest-two-" + StringUtils.randomString(5)), connection.getXMPPServiceDomain());
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

            assertCommandCompletedSuccessfully(result, "Expected response to the " + END_USER_SESSION + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");
            assertResult(isOneDisconnected, "Expected the connection of '" + userConnectionOne.getUser() + "' to be disconnected after '" + adminConnection.getUser() + "' invoked the " + END_USER_SESSION + " ad-hoc command using the a list of targets that included this one (but the connection remains connected).");
            assertResult(isTwoDisconnected, "Expected the connection of '" + userConnectionTwo.getUser() + "' to be disconnected after '" + adminConnection.getUser() + "' invoked the " + END_USER_SESSION + " ad-hoc command using the a list of targets that included this one (but the connection remains connected).");
        } finally {
            if (userConnectionOne != null && userConnectionOne.isConnected()) {
                userConnectionOne.disconnect();
            }
            if (userConnectionTwo != null && userConnectionTwo.isConnected()) {
                userConnectionTwo.disconnect();
            }
            tryDeleteUser(testUserOne);
            tryDeleteUser(testUserTwo);
        }
    }

    // No s4.6 test - retracted

    //node="http://jabber.org/protocol/admin#change-user-password" name="Change User Password"
    @SmackIntegrationTest(section = "4.7", quote = "An administrator may need to change a user's password. The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#change-user-password\".")
    public void testChangePassword() throws Exception {
        checkServerSupportCommand(CHANGE_USER_PASSWORD);
        // Setup test fixture.
        final Jid userToChangePassword = JidCreate.bareFrom(Localpart.from("changepasswordtest-" + StringUtils.randomString(5)), connection.getXMPPServiceDomain());
        try {
            createUser(userToChangePassword);

            // Execute system under test.
            AdHocCommandData result = executeCommandWithArgs(CHANGE_USER_PASSWORD, adminConnection.getUser().asEntityBareJid(),
                "accountjid", userToChangePassword.toString(),
                "password", "password2"
            );

            // Verify results.
            assertCommandCompletedSuccessfully(result, "Expected response to the " + CHANGE_USER_PASSWORD + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");

            AbstractXMPPConnection userConnection = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
            userConnection.connect();

            assertDoesNotThrow(() -> userConnection.login(userToChangePassword.getLocalpartOrThrow().toString(), "password2"), "Expected user '" + userToChangePassword + "' to be able to authenticate using the new credentials, after '" + adminConnection.getUser() + "' changed their password using the '" + CHANGE_USER_PASSWORD + "' command (but the user was not able to authenticate).");
        } finally {
            // Tear down test fixture.
            tryDeleteUser(userToChangePassword);
        }
    }

    //node="http://jabber.org/protocol/admin#get-user-roster" name="Get User Roster"
    @SmackIntegrationTest(section = "4.8", quote = "An administrator may need to retrieve a user's roster [...] The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#get-user-roster\".")
    public void testUserRoster() throws Exception {
        checkServerSupportCommand(GET_USER_ROSTER);

        // Setup test fixture.
        IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(conOne, conTwo, 10000);

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(GET_USER_ROSTER, adminConnection.getUser().asEntityBareJid(),
            "accountjids", conOne.getUser().asEntityBareJidString()
        );

        // Verify results.
        assertCommandCompletedSuccessfully(result, "Expected response to the " + GET_USER_ROSTER + " command that was executed by '" + adminConnection.getUser() + "' to retrieve the roster of '" + conOne.getUser() + "' to represent success (but it does not).");
        List<Element> elements = result.getForm().getExtensionElements();
        final Optional<RosterPacket> roster = elements.stream().filter(element -> element instanceof RosterPacket).map(element -> (RosterPacket) element).findAny();
        assertTrue(roster.isPresent(), "Expected response to the " + GET_USER_ROSTER + " command that was executed by '" + adminConnection.getUser() + "' to retrieve the roster of '" + conOne.getUser() + "' to contain a roster (but it does not).");
        assertTrue(roster.get().getRosterItems().stream().anyMatch(item -> item.getJid().equals(conTwo.getUser().asEntityBareJid())), "Expected response to the " + GET_USER_ROSTER + " command that was executed by '" + adminConnection.getUser() + "' to retrieve the roster of '" + conOne.getUser() + "' to contain their roster. A roster was returned, but it does not contain an expected item for '" + conTwo.getUser() + "'.");

        // Tear down test fixture.
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);
    }

    @SmackIntegrationTest(section = "4.9", quote = "An administrator may need to retrieve a user's last login time [...] The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#get-user-lastlogin\".")
    public void testGetUserLastLoginTime() throws Exception {
        checkServerSupportCommand(GET_USER_LAST_LOGIN_TIME);

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(GET_USER_LAST_LOGIN_TIME, adminConnection.getUser().asEntityBareJid(),
            "accountjids", conOne.getUser().asEntityBareJidString()
        );

        // Verify results.
        assertCommandCompletedSuccessfully(result, "Expected response to the " + GET_USER_LAST_LOGIN_TIME + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");
        if (result.getForm().getField("lastlogin") == null) {
            // The implementation uses a different field than 'lastlogin' maybe? Can't perform the rest of the assertions.
            assertFormFieldHasValues("lastlogin", 1, result, "Expected the last login time for '" + adminConnection.getUser() + "' as returned in a response to '" + GET_USER_LAST_LOGIN_TIME + "' command that was executed by '" + adminConnection.getUser() + "' to contain a value (as this user logged in to issuing this command!) but it does not contain a value.");
            FormField field = result.getForm().getField("lastlogin");
            try {
                Date lastLogin = field.getFirstValueAsDate();
                ZonedDateTime lastLoginTime = ZonedDateTime.ofInstant(lastLogin.toInstant(), ZoneId.systemDefault());
                assertTrue(lastLoginTime.isAfter(ZonedDateTime.now().minusHours(2)), "Expected the last login time for '" + adminConnection.getUser() + "' as returned in a response to '" + GET_USER_LAST_LOGIN_TIME + "' command that was executed by '" + adminConnection.getUser() + "' to contain a somewhat recent time (as this user logged in to issuing this command!) The last login time was unexpectedly long ago: " + Duration.between(lastLoginTime, Instant.now()));
            } catch (ParseException e) {
                // Do nothing here, since the field only SHOULD be in the format specified by XEP-0082
                // Let a non-parsing exception bubble up.
            }
        }
    }

    @SmackIntegrationTest(section = "4.10", quote = "An administrator may want to gather statistics about a particular user's interaction with the service [...] The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#user-stats\".")
    public void testGetUserStatistics() throws Exception {
        checkServerSupportCommand(GET_USER_STATISTICS);

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(GET_USER_STATISTICS, adminConnection.getUser().asEntityBareJid(),
            "accountjids", conOne.getUser().asEntityBareJidString()
        );

        // Verify results.
        assertCommandCompletedSuccessfully(result, "Expected response to the " + GET_USER_STATISTICS + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");
        assertFormFieldCountAtLeast(1, result, "Expected response to the " + GET_USER_STATISTICS + " command that was executed by '" + adminConnection.getUser() + "' to contain at least one field (but it does not).");
        // Which stats a server should support isn't defined, so we can't check for specific fields or values.
        // Instead, we assume that supporting the command means that the server will return at least one field.
    }

    //node="http://jabber.org/protocol/admin#edit-blacklist" name="Edit Blocked List"
    @SmackIntegrationTest(section = "4.11", quote = "The service may enable an administrator to define one or more service-wide blacklists [...] The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#edit-blacklist\"")
    public void testEditBlackList() throws Exception {
        checkServerSupportCommand(EDIT_BLOCKED_LIST);
        final String blacklistDomain = "xmpp.someotherdomain.org";
        try {
            // Setup test fixture.

            // Execute system under test: Pretend it's a 1-stage command initially, so that we can check that the current list of Blocked Users is populated
            AdHocCommandData result = executeCommandSimple(EDIT_BLOCKED_LIST, adminConnection.getUser().asEntityBareJid());

            if (result.getForm().getField("blacklistjids").hasValueSet()) {
                throw new TestNotPossibleException("The implementation of this test expects the preexisting blacklist to be empty (but it was not).");
            }

            // Execute system under test: Run the full 2-stage command to alter the Blocklist.
            result = executeCommandWithArgs(EDIT_BLOCKED_LIST, adminConnection.getUser().asEntityBareJid(),
                "blacklistjids", blacklistDomain
            );

            // Verify Results.
            assertCommandCompletedSuccessfully(result, "Expected response to the " + EDIT_BLOCKED_LIST + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");

            // Pretend it's a 1-stage command again, so that we can check that the new list of Blocked Users is correct.
            result = executeCommandSimple(EDIT_BLOCKED_LIST, adminConnection.getUser().asEntityBareJid());
            assertFormFieldEquals("blacklistjids", blacklistDomain, result, "Expected the blacklist to contain exactly one entry: '" + blacklistDomain + "' after being edited by '" + adminConnection.getUser() + "' (but that wasn't the case).");

        } finally {
            // Tear down test fixture.
            executeCommandWithArgs(EDIT_BLOCKED_LIST, adminConnection.getUser().asEntityBareJid(),
                "blacklistjids", ""
            );
        }
    }

    //node="http://jabber.org/protocol/admin#edit-whitelist" name="Edit Allowed List"
    @SmackIntegrationTest(section = "4.12", quote = "The service may enable an administrator to define one or more service-wide whitelists (lists of entities that are allowed to communicate the service). [...] The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#add-to-whitelist\"")
    public void testEditWhiteList() throws Exception {
        checkServerSupportCommand(EDIT_ALLOWED_LIST);
        final String whitelistDomain = "xmpp.someotherdomain.org";
        try {
            // Setup test fixture.

            // Execute system under test: Pretend it's a 1-stage command initially, so that we can check that the current list of Allowed Users is populated
            AdHocCommandData result = executeCommandSimple(EDIT_ALLOWED_LIST, adminConnection.getUser().asEntityBareJid());

            if (result.getForm().getField("whitelistjids").hasValueSet()) {
                throw new TestNotPossibleException("The implementation of this test expects the preexisting whitelist to be empty (but it was not).");
            }

            // Execute system under test: Run the full 2-stage command to alter the Whitelist.
            result = executeCommandWithArgs(EDIT_ALLOWED_LIST, adminConnection.getUser().asEntityBareJid(),
                "whitelistjids", whitelistDomain
            );

            // Verify Results.
            assertCommandCompletedSuccessfully(result, "Expected response to the " + EDIT_ALLOWED_LIST + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");

            // Pretend it's a 1-stage command again, so that we can check that the new list of Allowed Users is correct.
            result = executeCommandSimple(EDIT_ALLOWED_LIST, adminConnection.getUser().asEntityBareJid());
            assertFormFieldEquals("whitelistjids", whitelistDomain, result, "Expected the whitelist to contain exactly one entry: '" + whitelistDomain + "' after being edited by '" + adminConnection.getUser() + "' (but that wasn't the case).");

        } finally {
            // Tear down test fixture.
            executeCommandWithArgs(EDIT_ALLOWED_LIST, adminConnection.getUser().asEntityBareJid(),
                "whitelistjids", ""
            );
        }
    }

    //node="http://jabber.org/protocol/admin#get-registered-users-num" name="Get Number of Registered Users"
    @SmackIntegrationTest(section = "4.13", quote = "It may be helpful to enable an administrator to retrieve the number of registered users. The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#get-registered-users-num\".")
    public void testGetRegisteredUsersNumber() throws Exception {
        checkServerSupportCommand(GET_NUMBER_OF_REGISTERED_USERS);

        // Execute system under test.
        AdHocCommandData result = executeCommandSimple(GET_NUMBER_OF_REGISTERED_USERS, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        assertCommandCompletedSuccessfully(result, "Expected response to the " + GET_NUMBER_OF_REGISTERED_USERS + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");
        final int expectedMinimumCount = 3; // Each test runs with at least three registered test accounts (but more users might be active!)
        final int reportedCount = Integer.parseInt(result.getForm().getField("registeredusersnum").getFirstValue());
        assertTrue(reportedCount >= expectedMinimumCount, "Expected the response to the command " + GET_NUMBER_OF_REGISTERED_USERS + " that was executed by '" + adminConnection.getUser() + "' to be at least " + expectedMinimumCount + " (as that's how many test accounts are registered by this test suite), but it was " + reportedCount + " instead.");
    }

    //node="http://jabber.org/protocol/admin#get-disabled-users-num" name="Get Number of Disabled Users"
    @SmackIntegrationTest(section = "4.14", quote = "Given that admins may be able to disable user accounts, it may be helpful to enable an administrator to retrieve the number of disabled users. The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#get-disabled-users-num\".")
    public void testDisabledUsersNumber() throws Exception {
        checkServerSupportCommand(GET_NUMBER_OF_DISABLED_USERS);
        checkServerSupportCommand(REENABLE_A_USER);
        checkServerSupportCommand(DISABLE_A_USER);

        // Setup test fixture.
        final Jid disabledUser = JidCreate.bareFrom(Localpart.from("disableusernumtest-" + StringUtils.randomString(5)), connection.getXMPPServiceDomain());
        try {
            // Create and disable a user
            createUser(disabledUser);
            executeCommandWithArgs(DISABLE_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjids", disabledUser.toString()
            );

            // Execute system under test.
            AdHocCommandData result = executeCommandSimple(GET_NUMBER_OF_DISABLED_USERS, adminConnection.getUser().asEntityBareJid());

            // Verify results.
            assertCommandCompletedSuccessfully(result, "Expected response to the " + GET_NUMBER_OF_DISABLED_USERS + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");
            final int expectedMinimumCount = 1; // This test disabled one user.
            final int reportedCount = Integer.parseInt(result.getForm().getField("disabledusersnum").getFirstValue());
            assertTrue(reportedCount >= expectedMinimumCount, "Expected the response to the command " + GET_NUMBER_OF_DISABLED_USERS + " that was executed by '" + adminConnection.getUser() + "' to be at least " + expectedMinimumCount + " (as that's how many test accounts were disabled by this test), but it was '" + reportedCount + "' instead.");
        } finally {
            // Tear down test fixture.
            executeCommandWithArgs(REENABLE_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjids", disabledUser.toString()
            );
            tryDeleteUser(disabledUser);
        }
    }

    //node="http://jabber.org/protocol/admin#get-online-users-num" name="Get Number of Online Users"
    @SmackIntegrationTest(section = "4.15", quote = "It may be helpful to enable an administrator to retrieve the number of registered users who are online at any one moment. [...] The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#get-online-users-num\".")
    public void testGetOnlineUsersNumber() throws Exception {
        checkServerSupportCommand(GET_NUMBER_OF_ONLINE_USERS);

        // Execute system under test.
        AdHocCommandData result = executeCommandSimple(GET_NUMBER_OF_ONLINE_USERS, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        assertCommandCompletedSuccessfully(result, "Expected response to the " + GET_NUMBER_OF_ONLINE_USERS + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");
        final int expectedMinimumCount = 3; // Each test runs with at least three registered test accounts (but more users might be active!)
        final int reportedCount = Integer.parseInt(result.getForm().getField("onlineusersnum").getFirstValue());
        assertTrue(reportedCount >= expectedMinimumCount, "Expected the response to the command " + GET_NUMBER_OF_ONLINE_USERS + " that was executed by '" + adminConnection.getUser() + "' to be at least " + expectedMinimumCount + " (as that's how many test accounts are connected by this test suite), but it was " + reportedCount + " instead.");
    }

    //node="http://jabber.org/protocol/admin#get-active-users-num" name="Get Number of Active Users"
    @SmackIntegrationTest(section = "4.16", quote = "Some services may distinguish users who are online and actively using the service from users who are online but idle. [...] The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#get-active-users-num\".")
    public void testGetActiveUsersNumber() throws Exception {
        checkServerSupportCommand(GET_NUMBER_OF_ACTIVE_USERS);

        // Execute system under test.
        AdHocCommandData result = executeCommandSimple(GET_NUMBER_OF_ACTIVE_USERS, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        assertCommandCompletedSuccessfully(result, "Expected response to the " + GET_NUMBER_OF_ACTIVE_USERS + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");
        final int expectedMinimumCount = 3; // Each test runs with at least three test accounts (but more users might be active!)
        final int reportedCount = Integer.parseInt(result.getForm().getField("activeusersnum").getFirstValue());
        assertTrue(reportedCount >= expectedMinimumCount, "Expected the response to the command " + GET_NUMBER_OF_ACTIVE_USERS + " that was executed by '" + adminConnection.getUser() + "' to be at least " + expectedMinimumCount + " (as that's how many test accounts are started for each test), but it was " + reportedCount + " instead.");
    }

    //node="http://jabber.org/protocol/admin#get-idle-users-num" name="Get Number of Idle Users"
    @SmackIntegrationTest(section = "4.17", quote = "Some services may distinguish users who are online and actively using the service from users who are online but idle. [...] The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#get-idle-users-num\".")
    public void testGetIdleUsersNumber() throws Exception {
        checkServerSupportCommand(GET_NUMBER_OF_IDLE_USERS);

        // Execute system under test.
        AdHocCommandData result = executeCommandSimple(GET_NUMBER_OF_IDLE_USERS, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        assertCommandCompletedSuccessfully(result, "Expected response to the " + GET_NUMBER_OF_IDLE_USERS + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");
        final int reportedCount = Integer.parseInt(result.getForm().getField("idleusersnum").getFirstValue());
        assertTrue(reportedCount >= 0, "Expected the response to the command " + GET_NUMBER_OF_IDLE_USERS + " that was executed by '" + adminConnection.getUser() + "' to be any non-negative number, but it was " + reportedCount + " instead.");
    }

    //node="http://jabber.org/protocol/admin#get-registered-users-list" name="Get List of Registered Users"
    @SmackIntegrationTest(section = "4.18", quote = " it may be helpful to enable an administrator to retrieve a list of all registered users [...] The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#get-registered-users-list\".")
    public void testGetRegisteredUsersList() throws Exception {
        checkServerSupportCommand(GET_LIST_OF_REGISTERED_USERS);

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(GET_LIST_OF_REGISTERED_USERS, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        assertCommandCompletedSuccessfully(result, "Expected response to the " + GET_LIST_OF_REGISTERED_USERS + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");

        final RSMSet rsm = result.getExtension(RSMSet.class);
        final int reportedJidCount = result.getForm().getField("registereduserjids").getValues().size();
        final boolean responseContainsEverything = rsm != null && rsm.getCount() == reportedJidCount;
        final boolean responseProbablyContainsEverything = rsm == null && reportedJidCount < 10; // smaller than what can reasonably be expected to be the smallest page size.

        if (responseContainsEverything || responseProbablyContainsEverything) {
            // Only check for known accounts if we can be reasonably sure that we received the complete result set (as opposed to just a page of a larger result set).
            final Collection<Jid> expectedJids = List.of(
                conOne.getUser().asEntityBareJid(),
                conTwo.getUser().asEntityBareJid(),
                conThree.getUser().asEntityBareJid()
            );
            assertFormFieldContainsAll("registereduserjids", expectedJids, result, "Expected the (non paginated) response to the command " + GET_LIST_OF_REGISTERED_USERS + " that was executed by '" + adminConnection.getUser() + "' to contain all of the users (" + expectedJids.stream().map(Jid::toString).collect(Collectors.joining(", ")) + ") that are used by this test framework for testing (but they were not).");
        }
    }

    @SmackIntegrationTest(section = "4.19", quote = "It may be helpful to enable an administrator to retrieve a list of all disabled users. [...] The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#get-disabled-users-list\".")
    public void testDisabledUsersList() throws Exception {
        checkServerSupportCommand(GET_LIST_OF_DISABLED_USERS);
        checkServerSupportCommand(DISABLE_A_USER);
        checkServerSupportCommand(REENABLE_A_USER);

        final Jid disabledUser = JidCreate.bareFrom(Localpart.from("disableuserlisttest-" + StringUtils.randomString(5)), connection.getXMPPServiceDomain());
        createUser(disabledUser);

        try {
            executeCommandWithArgs(DISABLE_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjids", disabledUser.toString()
            );

            AdHocCommandData result = executeCommandWithArgs(GET_LIST_OF_DISABLED_USERS, adminConnection.getUser().asEntityBareJid());

            assertCommandCompletedSuccessfully(result, "Expected response to the " + GET_LIST_OF_DISABLED_USERS + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");

            final RSMSet rsm = result.getExtension(RSMSet.class);
            final int reportedJidCount = result.getForm().getField("disableduserjids").getValues().size();
            final boolean responseContainsEverything = rsm != null && rsm.getCount() == reportedJidCount;
            final boolean responseProbablyContainsEverything = rsm == null && reportedJidCount < 10; // smaller than what can reasonably be expected to be the smallest page size.

            if (responseContainsEverything || responseProbablyContainsEverything) {
                // Only check for known accounts if we can be reasonably sure that we received the complete result set (as opposed to just a page of a larger result set).
                final Collection<Jid> expectedJids = List.of(
                    disabledUser
                );
                assertFormFieldContainsAll("disableduserjids", expectedJids, result, "Expected the (non paginated) response to the command " + GET_LIST_OF_DISABLED_USERS + " that was executed by '" + adminConnection.getUser() + "' to contain all of the users (" + expectedJids.stream().map(Jid::toString).collect(Collectors.joining(", ")) + ") that were disabled during this test (but they were not in the response).");
            }
        } finally {
            // Tear down test fixture.
            executeCommandWithArgs(REENABLE_A_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjids", disabledUser.toString()
            );
            tryDeleteUser(disabledUser);
        }
    }

    //node="http://jabber.org/protocol/admin#get-online-users-list" name="Get List of Online Users"
    @SmackIntegrationTest(section = "4.20", quote = "It may be helpful to enable an administrator to retrieve a list of all online users. [..] The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#get-online-users-list\".")
    public void testGetOnlineUsersListSimple() throws Exception {
        checkServerSupportCommand(GET_LIST_OF_ONLINE_USERS);

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(GET_LIST_OF_ONLINE_USERS, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        assertCommandCompletedSuccessfully(result, "Expected response to the " + GET_LIST_OF_ONLINE_USERS + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");

        final RSMSet rsm = result.getExtension(RSMSet.class);
        final int reportedJidCount = result.getForm().getField("onlineuserjids").getValues().size();
        final boolean responseContainsEverything = rsm != null && rsm.getCount() == reportedJidCount;
        final boolean responseProbablyContainsEverything = rsm == null && reportedJidCount < 10; // smaller than what can reasonably be expected to be the smallest page size.

        if (responseContainsEverything || responseProbablyContainsEverything) {
            // Only check for known accounts if we can be reasonably sure that we received the complete result set (as opposed to just a page of a larger result set).
            final Collection<Jid> expectedJids = List.of(
                conOne.getUser().asEntityBareJid(),
                conTwo.getUser().asEntityBareJid(),
                conThree.getUser().asEntityBareJid()
            );
            assertFormFieldContainsAll("onlineuserjids", expectedJids, result, "Expected the (non paginated) response to the command " + GET_LIST_OF_ONLINE_USERS + " that was executed by '" + adminConnection.getUser() + "' to contain all of the users (" + expectedJids.stream().map(Jid::toString).collect(Collectors.joining(", ")) + ") as those are test accounts that are connected by this test suite (but they were not).");
        }
    }

    //node="http://jabber.org/protocol/admin#get-active-users" name="Get List of Active Users"
    @SmackIntegrationTest(section = "4.21", quote = "It may be helpful to enable an administrator to retrieve a list of all active users. [...] The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#get-active-users\".")
    public void testGetActiveUsersListSimple() throws Exception {
        checkServerSupportCommand(GET_LIST_OF_ACTIVE_USERS);

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(GET_LIST_OF_ACTIVE_USERS, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        assertCommandCompletedSuccessfully(result, "Expected response to the " + GET_LIST_OF_ACTIVE_USERS + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");

        final RSMSet rsm = result.getExtension(RSMSet.class);
        final int reportedJidCount = result.getForm().getField("activeuserjids").getValues().size();
        final boolean responseContainsEverything = rsm != null && rsm.getCount() == reportedJidCount;
        final boolean responseProbablyContainsEverything = rsm == null && reportedJidCount < 10; // smaller than what can reasonably be expected to be the smallest page size.

        if (responseContainsEverything || responseProbablyContainsEverything) {
            // Only check for known accounts if we can be reasonably sure that we received the complete result set (as opposed to just a page of a larger result set).
            final Collection<Jid> expectedJids = List.of(
                conOne.getUser().asEntityBareJid(),
                conTwo.getUser().asEntityBareJid(),
                conThree.getUser().asEntityBareJid()
            );
            assertFormFieldContainsAll("activeuserjids", expectedJids, result, "Expected the (non paginated) response to the command " + GET_LIST_OF_ONLINE_USERS + " that was executed by '" + adminConnection.getUser() + "' to contain all of the users (" + expectedJids.stream().map(Jid::toString).collect(Collectors.joining(", ")) + ") as those are test accounts that are connected by this test suite (but they were not).");
        }
    }

    //node="http://jabber.org/protocol/admin#get-idle-users" name="Get List of Idle Users"
    @SmackIntegrationTest(section = "4.22", quote = "It may be helpful to enable an administrator to retrieve a list of all idle users. [...] The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#get-idle-users\".")
    public void testGetIdleUsersList() throws Exception {
        checkServerSupportCommand(GET_LIST_OF_IDLE_USERS);

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(GET_LIST_OF_IDLE_USERS, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        assertCommandCompletedSuccessfully(result, "Expected response to the " + GET_LIST_OF_IDLE_USERS + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");

        // We can't be sure how 'idle' is defined in servers. This could be a period of inactivity, a presence status, or something else.
    }

    //node="http://jabber.org/protocol/admin#announce" name="Send Announcement to Online Users"
    @SmackIntegrationTest(section = "4.23", quote = "Administrators of some existing Jabber servers have found it useful to be able to send an announcement to all online users of the server [...] The message shall be sent only to users who currently have a \"session\" with the service. [...] The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#announce\".")
    public void testSendAnnouncementToOnlineUsers() throws Exception {
        checkServerSupportCommand(SEND_ANNOUNCEMENT_TO_ONLINE_USERS);
        // Setup test fixture.
        final String announcement = "testAnnouncement-" + StringUtils.randomString(5);
        final SimpleResultSyncPoint syncPoint = new SimpleResultSyncPoint();

        StanzaListener stanzaListener = stanza -> {
            Message message = (Message) stanza;
            if (message.getBody() != null && message.getBody().contains(announcement)) {
                syncPoint.signal();
            }
        };

        conOne.addSyncStanzaListener(stanzaListener, StanzaTypeFilter.MESSAGE);

        try {
            // Execute system under test.
            AdHocCommandData result = executeCommandWithArgs(SEND_ANNOUNCEMENT_TO_ONLINE_USERS, adminConnection.getUser().asEntityBareJid(),
                "announcement", announcement
            );

            // Verify results.
            assertCommandCompletedSuccessfully(result, "Expected response to the " + SEND_ANNOUNCEMENT_TO_ONLINE_USERS + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");
            assertResult(syncPoint, "Expected '" + conOne.getUser() + "' to receive the announcement that was sent by '" + adminConnection.getUser() + "' (but the announcement was not received).");
        }
        finally {
            // Tear down test fixture.
            adminConnection.removeSyncStanzaListener(stanzaListener);
        }
    }

    //node="http://jabber.org/protocol/admin#set-motd" name="Set Message of the Day"
    @SmackIntegrationTest(section = "4.24", quote = "Administrators of some existing Jabber servers have found it useful to be able to send a \"message of the day\" that is delivered to any user who logs in to the server that day [...] The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#set-motd\".")
    public void testSetMOTD() throws Exception {
        checkServerSupportCommand(SET_MOTD);
        checkServerSupportCommand(DELETE_MOTD); // Used in setup and teardown

        final Collection<String> newMOTD = Arrays.asList(
            "This is MOTD 1",
            "This is MOTD 2"
        );

        final SimpleResultSyncPoint syncPoint = new SimpleResultSyncPoint();

        StanzaListener stanzaListener = stanza -> {
            Message message = (Message) stanza;
            if (newMOTD.stream().allMatch(s -> message.getBody() != null && message.getBody().contains(s))) {
                syncPoint.signal();
            }
        };
        final Jid testUser = JidCreate.bareFrom(Localpart.from("motdsettest-" + StringUtils.randomString(5)), connection.getXMPPServiceDomain());
        AbstractXMPPConnection userConnection = null;
        try {
            // Setup test fixture.
            createUser(testUser);
            userConnection = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
            userConnection.addSyncStanzaListener(stanzaListener, StanzaTypeFilter.MESSAGE);

            executeCommandSimple(DELETE_MOTD, adminConnection.getUser().asEntityBareJid()); // Ensure that no MOTD pre-exists.

            // Execute system under test.
            AdHocCommandData result = executeCommandWithArgs(
                SET_MOTD,
                adminConnection.getUser().asEntityBareJid(),
                "motd",
                String.join(",", newMOTD)
            );

            // Verify results.
            assertCommandCompletedSuccessfully(result, "Expected response to the " + SET_MOTD + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");
            userConnection.connect();
            userConnection.login(testUser.getLocalpartOrThrow().toString(), "password");
            assertResult(syncPoint, "Expected user '" + userConnection.getUser() + "' to receive the Message of the Day, that was set by '" + adminConnection.getUser() + "' using the " + SET_MOTD + " command, before the intended recipient connected to the server (but they did not receive it).");
        } finally {
            // Tear down test fixture.
            try {
                executeCommandSimple(DELETE_MOTD, adminConnection.getUser().asEntityBareJid());
            } catch (XMPPException e) {
                // Ignore
            }
            if (userConnection != null) {
                userConnection.removeSyncStanzaListener(stanzaListener);
                userConnection.disconnect();
            }
            tryDeleteUser(testUser);
        }
    }

    //node="http://jabber.org/protocol/admin#edit-motd" name="Edit Message of the Day"
    @SmackIntegrationTest(section = "4.25", quote = "After setting a message of the day, an administrator may want to edit that message [...]. The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#edit-motd\".")
    public void testEditMOTD() throws Exception {
        checkServerSupportCommand(SET_MOTD); // Used in setup
        checkServerSupportCommand(EDIT_MOTD);
        checkServerSupportCommand(DELETE_MOTD); // Used in teardown

        final Collection<String> newMOTD = Arrays.asList(
            "This is MOTD 1",
            "This is MOTD 2"
        );

        final SimpleResultSyncPoint syncPoint = new SimpleResultSyncPoint();

        StanzaListener stanzaListener = stanza -> {
            Message message = (Message) stanza;
            if (newMOTD.stream().allMatch(s -> message.getBody() != null && message.getBody().contains(s))) {
                syncPoint.signal();
            }
        };
        final Jid testUser = JidCreate.bareFrom(Localpart.from("motdedittest-" + StringUtils.randomString(5)), connection.getXMPPServiceDomain());
        AbstractXMPPConnection userConnection = null;

        // Setup test fixture.
        try {
            executeCommandWithArgs(
                SET_MOTD,
                adminConnection.getUser().asEntityBareJid(),
                "motd",
                String.join(",", "This is the old MOTD that should be replaced.")
            );

            createUser(testUser);
            userConnection = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
            userConnection.addSyncStanzaListener(stanzaListener, StanzaTypeFilter.MESSAGE);

            // Execute system under test: Now run the full thing
            AdHocCommandData result = executeCommandWithArgs(
                EDIT_MOTD,
                adminConnection.getUser().asEntityBareJid(),
                "motd",
                String.join(",", newMOTD)
            );

            // Verify results.
            assertCommandCompletedSuccessfully(result, "Expected response to the " + EDIT_MOTD + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");
            userConnection.connect();
            userConnection.login(testUser.getLocalpartOrThrow().toString(), "password");
            assertResult(syncPoint, "Expected user '" + userConnection.getUser() + "' to receive the Message of the Day, that was edited by '" + adminConnection.getUser() + "' using the " + EDIT_MOTD + " command, before the intended recipient connected to the server (but they did not receive it).");
        } finally {
            // Tear down test fixture.
            try {
                executeCommandSimple(DELETE_MOTD, adminConnection.getUser().asEntityBareJid());
            } catch (XMPPException e) {
                // Ignore
            }
            if (userConnection != null) {
                userConnection.removeSyncStanzaListener(stanzaListener);
                userConnection.disconnect();
            }
            tryDeleteUser(testUser);
        }
    }

    //node="http://jabber.org/protocol/admin#delete-motd" name="Delete Message of the Day"
    @SmackIntegrationTest(section = "4.26", quote = "Sometimes a previously-set \"message of the day\" is no longer appropriate and needs to be deleted. The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#delete-motd\".")
    public void testDeleteMOTD() throws Exception {
        checkServerSupportCommand(SET_MOTD); // Used in setup
        checkServerSupportCommand(EDIT_MOTD); // Used in validation
        checkServerSupportCommand(DELETE_MOTD);

        try {
            // Setup test fixture.
            executeCommandWithArgs(
                SET_MOTD,
                adminConnection.getUser().asEntityBareJid(),
                "motd",
                String.join(",", "This test message should have been removed.")
            );

            // Execute system under test.
            AdHocCommandData result = executeCommandSimple(
                DELETE_MOTD,
                adminConnection.getUser().asEntityBareJid());

            // Verify results.
            assertCommandCompletedSuccessfully(result, "Expected response to the " + DELETE_MOTD + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");

            // Check value using the edit form
            result = executeCommandSimple(EDIT_MOTD, adminConnection.getUser().asEntityBareJid());
            FormField motdField = result.getForm().getField("motd");
            assertFalse(motdField.hasValueSet(), "Expected the pre-filled Message of the Day value of the 'edit' form returned as a response to the " + EDIT_MOTD + " command to be empty, as '" + adminConnection + "' earlier removed the MotD using the " + DELETE_MOTD + " command (but the edit for did have a pre-filled motd: '" + String.join(" ", motdField.getValues()) + "').");
        } finally {
            // Tear down test fixture.
            try {
                executeCommandSimple(DELETE_MOTD, adminConnection.getUser().asEntityBareJid());
            } catch (XMPPException e) {
                // Ignore
            }
        }
    }

    //node="http://jabber.org/protocol/admin#set-welcome" name="Set Welcome Message"
    @SmackIntegrationTest(section = "4.27", quote = "Some existing Jabber servers send an informative \"welcome message\" to newly registered users of the server when they first log in. [...] The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#set-welcome\".")
    public void testSetWelcome() throws Exception {
        checkServerSupportCommand(SET_WELCOME_MESSAGE);
        checkServerSupportCommand(DELETE_WELCOME_MESSAGE); // Used in teardown.

        final Collection<String> newWelcomeMessage = Arrays.asList(
            "Line 1 of welcome message",
            "Line 2 of welcome message"
        );

        final SimpleResultSyncPoint syncPoint = new SimpleResultSyncPoint();

        StanzaListener stanzaListener = stanza -> {
            if (stanza instanceof Message) {
                Message message = (Message) stanza;
                if (newWelcomeMessage.stream().allMatch(s -> message.getBody() != null && message.getBody().contains(s))) {
                    syncPoint.signal();
                }
            }
        };
        final Jid testUser = JidCreate.bareFrom(Localpart.from("welcomesettest-" + StringUtils.randomString(5)), connection.getXMPPServiceDomain());
        AbstractXMPPConnection userConnection = null;

        try {
            // Execute system under test: Now run the full thing
            AdHocCommandData result = executeCommandWithArgs(
                SET_WELCOME_MESSAGE,
                adminConnection.getUser().asEntityBareJid(),
                "welcome",
                String.join(",", newWelcomeMessage)
            );

            // Verify results.
            assertCommandCompletedSuccessfully(result, "Expected response to the " + SET_WELCOME_MESSAGE + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");

            createUser(testUser); // Create user _after_ setting the welcome message!
            userConnection = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
            userConnection.addSyncStanzaListener(stanzaListener, StanzaTypeFilter.MESSAGE);
            assertResult(syncPoint, "Expected the newly created user '" + userConnection.getUser() + "' to receive the Welcome Message that was set by '" + adminConnection.getUser() + "' using the " + SET_WELCOME_MESSAGE + " command before the intended recipient was created (but they did not receive it).");
        } finally {
            // Tear down test fixture.
            if (userConnection != null) {
                userConnection.removeSyncStanzaListener(stanzaListener);
                userConnection.disconnect();
            }
            tryDeleteUser(testUser);
            try {
                executeCommandSimple(DELETE_WELCOME_MESSAGE, adminConnection.getUser().asEntityBareJid());
            } catch (XMPPException e) {
                // Ignore.
            }
        }
    }

    //node="http://jabber.org/protocol/admin#delete-welcome" name="Delete Welcome Message"
    @SmackIntegrationTest(section = "4.28", quote = "Sometimes a previously-set \"message of the day\" is no longer appropriate and needs to be deleted. The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#delete-motd\".")
    public void testDeleteWelcome() throws Exception {
        checkServerSupportCommand(DELETE_WELCOME_MESSAGE);
        checkServerSupportCommand(SET_WELCOME_MESSAGE); // Used for setup and validation

        // Setup test fixture.
        executeCommandWithArgs(
            SET_WELCOME_MESSAGE,
            adminConnection.getUser().asEntityBareJid(),
            "welcome",
            String.join(",", "This test message should have been deleted.")
        );

        // Execute system under test.
        AdHocCommandData result = executeCommandSimple(DELETE_WELCOME_MESSAGE, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        assertCommandCompletedSuccessfully(result, "Expected response to the " + DELETE_WELCOME_MESSAGE + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");

        // Use Set Welcome Message form to check the value
        result = executeCommandSimple(SET_WELCOME_MESSAGE, adminConnection.getUser().asEntityBareJid());
        FormField welcomeField = result.getForm().getField("welcome");
        assertFalse(welcomeField.hasValueSet(), "Expected the pre-filled Welcome Message value of the 'set' form returned as a response to the " + SET_WELCOME_MESSAGE + " command to be empty, as '" + adminConnection + "' earlier removed the Welcome Message using the " + DELETE_WELCOME_MESSAGE + " command (but the edit for did have a pre-filled welcome message: '" + String.join(" ", welcomeField.getValues()) + "').");
    }

    //node="http://jabber.org/protocol/admin#edit-admin" name="Edit Admin List"
    @SmackIntegrationTest(section = "4.29", quote = "An administrator may want to directly edit the list of users who have administrative privileges [...] The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#edit-admin\".")
    public void testEditAdminList() throws Exception {
        checkServerSupportCommand(EDIT_ADMIN_LIST);
        final Jid adminToAdd = JidCreate.bareFrom(Localpart.from("editadminlisttest-" + StringUtils.randomString(5)), connection.getXMPPServiceDomain());
        List<? extends CharSequence> preexistingValues = null;
        try {
            // Setup test fixture.
            createUser(adminToAdd);

            // Execute system under test: Pretend it's a 1-stage command initially, so that we can check that the current list of Admins is populated
            AdHocCommandData result = executeCommandSimple(EDIT_ADMIN_LIST, adminConnection.getUser().asEntityBareJid());

            preexistingValues = result.getForm().getField("adminjids").getValues();
            if (preexistingValues == null || preexistingValues.isEmpty()) {
                throw new TestNotPossibleException("Unable to read the admin list. Changing the admin list might lock existing admins out, so this test is skipped");
            }

            // Execute system under test: Run the full 2-stage command to alter the list of Admins
            result = executeCommandWithArgs(EDIT_ADMIN_LIST, adminConnection.getUser().asEntityBareJid(),
                "adminjids", String.join(",", preexistingValues) + "," + adminToAdd
            );

            // Verify results.
            assertCommandCompletedSuccessfully(result, "Expected response to the " + EDIT_ADMIN_LIST + " command that was executed by '" + adminConnection.getUser() + "' to represent success (but it does not).");

            // Execute system under test: Pretend it's a 1-stage command again, so that we can check that the new list of Admins is correct
            result = executeCommandSimple(EDIT_ADMIN_LIST, adminConnection.getUser().asEntityBareJid());
            assertFormFieldJidEquals("adminjids", new HashSet<>(Arrays.asList(
                adminConnection.getUser().asEntityBareJid(),
                adminToAdd
            )), result, "Expected the pre-filled 'adminjids'' value of the 'edit-admin' form returned as a response to the " + EDIT_ADMIN_LIST + " command to be contain '" + adminToAdd + "', as '" + adminConnection + "' added them using the " + EDIT_ADMIN_LIST + " command (but they were not on the list).");
        } finally {
            // Tear down test fixture.
            tryDeleteUser(adminToAdd);
            if (preexistingValues != null && !preexistingValues.isEmpty()) {
                executeCommandWithArgs(EDIT_ADMIN_LIST, adminConnection.getUser().asEntityBareJid(),
                    "adminjids", String.join(",", preexistingValues));
            }
        }
    }

    //node="http://jabber.org/protocol/admin#restart" name="Restart Service"
    @SmackIntegrationTest(section = "4.30", quote = "A service may allow an administrator to restart the service. The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#restart\".")
    public void testRestartServiceNoParams() throws Exception {
        checkServerSupportCommand(RESTART_SERVICE);

        // Execute system under test: Pretend it's a 1-stage command initially, so that we can check the current Welcome Message form
        AdHocCommandData result = executeCommandSimple(RESTART_SERVICE, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        assertFormFieldExists("delay", result, "Expected the form for the " + RESTART_SERVICE + " command to contain a 'delay' field (but it does not).");
        assertFormFieldExists("announcement", result, "Expected the form for the " + RESTART_SERVICE + " command to contain a 'announcement' field (but it does not).");

        // No actual execution of the command, as that would be rather disruptive to other tests...
    }

    //node="http://jabber.org/protocol/admin#shutdown" name="Shut Down Service"
    @SmackIntegrationTest(section = "4.31", quote = "A service may allow an administrator to shut down the service. The command node for this use case SHOULD be \"http://jabber.org/protocol/admin#shutdown\".")
    public void testShutdownServiceNoParams() throws Exception {
        checkServerSupportCommand(SHUTDOWN_SERVICE);

        // Execute system under test: Pretend it's a 1-stage command initially, so that we can check the current Welcome Message form
        AdHocCommandData result = executeCommandSimple(SHUTDOWN_SERVICE, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        assertFormFieldExists("delay", result, "Expected the form for the " + SHUTDOWN_SERVICE + " command to contain a 'delay' field (but it does not).");
        assertFormFieldExists("announcement", result, "Expected the form for the " + SHUTDOWN_SERVICE + " command to contain a 'announcement' field (but it does not).");

        // No actual execution of the command, as that would be rather disruptive to other tests...
    }
}
