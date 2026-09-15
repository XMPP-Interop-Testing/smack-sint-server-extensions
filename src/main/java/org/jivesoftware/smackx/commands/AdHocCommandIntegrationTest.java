package org.jivesoftware.smackx.commands;

import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.PresenceBuilder;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

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

            // TODO Assert that the newly created user is now available.
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
    @SmackIntegrationTest(section = "4.5")
    public void testEndUserSession() throws Exception {
        checkServerSupportCommand(END_USER_SESSION);
        final Jid userToEndSession = JidCreate.bareFrom("endsessiontest" + testRunId + "@example.org");
        try {
            createUser(userToEndSession);

            // Fetch user details to get the user loaded
            //AdHocCommandData result = executeCommandWithArgs(GET_USER_PROPERTIES, adminConnection.getUser().asEntityBareJid(),
            //    "accountjids", userToEndSession.toString()
            //);

            //assertFormFieldExists("accountjids", result);

            // Login as the user to be able to end their session
            AbstractXMPPConnection userConnection = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
            userConnection.connect();
            userConnection.login(userToEndSession.getLocalpartOrThrow().toString(), "password");

            AdHocCommandData result = executeCommandWithArgs(GET_LIST_OF_ACTIVE_USERS, adminConnection.getUser().asEntityBareJid(),
                "max_items", "25"
            );
            List<String> jids = result.getForm().getField("activeuserjids").getValues().stream().map(CharSequence::toString).collect(Collectors.toList());
            assertTrue(jids.contains(userConnection.getUser().asEntityBareJidString()));

            // End the user's session
            result = executeCommandWithArgs(END_USER_SESSION, adminConnection.getUser().asEntityBareJid(),
                "accountjids", userToEndSession.toString()
            );

            assertNoteType(AdHocCommandNote.Type.info, result);
            assertNoteContains("Operation finished successfully", result);

            result = executeCommandWithArgs(GET_LIST_OF_ACTIVE_USERS, adminConnection.getUser().asEntityBareJid(),
                "max_items", "25"
            );
            jids = result.getForm().getField("activeuserjids").getValues().stream().map(CharSequence::toString).collect(Collectors.toList());
            assertFalse(jids.contains(userConnection.getUser().asEntityBareJidString()));
        } finally {
            deleteUser(userToEndSession);
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

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(GET_USER_ROSTER, adminConnection.getUser().asEntityBareJid(),
            "accountjids", adminConnection.getUser().asEntityBareJidString()
        );

        // Verify results.
        // TODO: Actually populate a roster of one of the test accounts, instead of depending on an assumed state of the roster of the admin user.
        assertFormFieldJidEquals("accountjids", Collections.singleton(adminConnection.getUser().asEntityBareJid()), result);
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
        assertFormFieldExists("rostersize", result);
        assertFormFieldExists("onlineresources", result);
        // TODO: Examples are non-normative, so we can't really check the values, and I'm not 100% happy with these assertions.
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
            deleteUser(disabledUser);
            // TODO consider unmarking the user as being disabled, as deleting the user might not propagate.
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
        // TODO clear the list

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(GET_LIST_OF_DISABLED_USERS, adminConnection.getUser().asEntityBareJid(),
            "max_items", "25");

        // Verify results.
        assertFormFieldEquals("disableduserjids", new ArrayList<>(), result);
    }

    @SmackIntegrationTest(section = "4.19")
    public void testDisabledUsersList() throws Exception {
        checkServerSupportCommand(GET_LIST_OF_DISABLED_USERS);

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
