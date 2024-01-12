package org.jivesoftware.smackx.commands;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.form.FillableForm;
import org.jivesoftware.smackx.xdata.form.SubmitForm;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.Jid;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AdHocCommandIntegrationTest extends AbstractSmackIntegrationTest {

    private final AdHocCommandManager adHocCommandManagerForAdmin;
    private final AdHocCommandManager adHocCommandManagerForConOne;
    private final AbstractXMPPConnection adminConnection;

    private final List<FormField.Type> NON_STRING_FORM_FIELD_TYPES = Arrays.asList(
        FormField.Type.jid_multi,
        FormField.Type.list_multi
    );

    private static final String ADD_MEMBERS_OR_ADMINS_TO_A_GROUP = "http://jabber.org/protocol/admin#add-group-members";
    private static final String CREATE_NEW_GROUP = "http://jabber.org/protocol/admin#add-group";
    private static final String ADD_A_USER = "http://jabber.org/protocol/admin#add-user";
    private static final String SEND_ANNOUNCEMENT_TO_ONLINE_USERS = "http://jabber.org/protocol/admin#announce";
    private static final String AUTHENTICATE_USER = "http://jabber.org/protocol/admin#authenticate-user";
    private static final String CHANGE_USER_PASSWORD = "http://jabber.org/protocol/admin#change-user-password";
    private static final String DELETE_MEMBERS_OR_ADMINS_FROM_A_GROUP = "http://jabber.org/protocol/admin#delete-group-members";
    private static final String DELETE_GROUP = "http://jabber.org/protocol/admin#delete-group";
    private static final String DELETE_A_USER = "http://jabber.org/protocol/admin#delete-user";
    private static final String DISABLE_A_USER = "http://jabber.org/protocol/admin#disable-user";
    private static final String EDIT_ADMIN_LIST = "http://jabber.org/protocol/admin#edit-admin";
    private static final String EDIT_BLOCKED_LIST = "http://jabber.org/protocol/admin#edit-blacklist";
    private static final String EDIT_ALLOWED_LIST = "http://jabber.org/protocol/admin#edit-whitelist";
    private static final String END_USER_SESSION = "http://jabber.org/protocol/admin#end-user-session";
    private static final String GET_PRESENCE_OF_ACTIVE_USERS = "http://jabber.org/protocol/admin#get-active-presences";
    private static final String GET_NUMBER_OF_ACTIVE_USERS = "http://jabber.org/protocol/admin#get-active-users-num";
    private static final String GET_LIST_OF_ACTIVE_USERS = "http://jabber.org/protocol/admin#get-active-users";
    private static final String GET_ADMIN_CONSOLE_INFO = "http://jabber.org/protocol/admin#get-console-info";
    private static final String GET_LIST_OF_DISABLED_USERS = "http://jabber.org/protocol/admin#get-disabled-users-list";
    private static final String GET_NUMBER_OF_DISABLED_USERS = "http://jabber.org/protocol/admin#get-disabled-users-num";
    private static final String GET_LIST_OF_GROUP_MEMBERS = "http://jabber.org/protocol/admin#get-group-members";
    private static final String GET_LIST_OF_EXISTING_GROUPS = "http://jabber.org/protocol/admin#get-groups";
    private static final String GET_NUMBER_OF_IDLE_USERS = "http://jabber.org/protocol/admin#get-idle-users-num";
    private static final String GET_LIST_OF_ONLINE_USERS = "http://jabber.org/protocol/admin#get-online-users-list";
    private static final String GET_NUMBER_OF_ONLINE_USERS = "http://jabber.org/protocol/admin#get-online-users-num";
    private static final String GET_LIST_OF_REGISTERED_USERS = "http://jabber.org/protocol/admin#get-registered-users-list";
    private static final String GET_NUMBER_OF_REGISTERED_USERS = "http://jabber.org/protocol/admin#get-registered-users-num";
    private static final String GET_BASIC_STATISTICS_OF_THE_SERVER = "http://jabber.org/protocol/admin#get-server-stats";
    private static final String GET_NUMBER_OF_CONNECTED_USER_SESSIONS = "http://jabber.org/protocol/admin#get-sessions-num";
    private static final String GET_USER_PROPERTIES = "http://jabber.org/protocol/admin#get-user-properties";
    private static final String GET_USER_ROSTER = "http://jabber.org/protocol/admin#get-user-roster";
    private static final String REENABLE_A_USER = "http://jabber.org/protocol/admin#reenable-user";
    private static final String CURRENT_HTTP_BIND_STATUS = "http://jabber.org/protocol/admin#status-http-bind";
    private static final String UPDATE_GROUP_CONFIGURATION = "http://jabber.org/protocol/admin#update-group";
    private static final String GROUP_ADMIN_ADDED = "http://jabber.org/protocol/event#group-admin-added";
    private static final String GROUP_ADMIN_REMOVED = "http://jabber.org/protocol/event#group-admin-removed";
    private static final String GROUP_DELETING = "http://jabber.org/protocol/event#group-created";
    private static final String GROUP_MEMBER_ADDED = "http://jabber.org/protocol/event#group-member-added";
    private static final String GROUP_MEMBER_REMOVED = "http://jabber.org/protocol/event#group-member-removed";
    private static final String GROUP_MODIFIED = "http://jabber.org/protocol/event#group-modified";
    private static final String USER_CREATED = "http://jabber.org/protocol/event#user-created";
    private static final String DELETING_A_USER = "http://jabber.org/protocol/event#user-deleting";
    private static final String USER_MODIFIED = "http://jabber.org/protocol/event#user-modified";
    private static final String VCARD_CREATED = "http://jabber.org/protocol/event#vcard-created";
    private static final String DELETING_A_VCARD = "http://jabber.org/protocol/event#vcard-deleting";
    private static final String VCARD_MODIFIED = "http://jabber.org/protocol/event#vcard-modified";
    private static final String REQUEST_PONG_FROM_SERVER = "ping";

    public AdHocCommandIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws InvocationTargetException, InstantiationException, IllegalAccessException, SmackException, IOException, XMPPException, InterruptedException {
        super(environment);

        adminConnection = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
        adminConnection.connect();
        adminConnection.login(sinttestConfiguration.adminAccountUsername,
            sinttestConfiguration.adminAccountPassword);

        adHocCommandManagerForConOne = AdHocCommandManager.getAddHocCommandsManager(conOne);
        adHocCommandManagerForAdmin = AdHocCommandManager.getAddHocCommandsManager(adminConnection);
    }


    private AdHocCommandData executeCommandSimple(String commandNode, Jid jid) throws Exception {
        AdHocCommand command = adHocCommandManagerForAdmin.getRemoteCommand(jid, commandNode);
        return command.execute().getResponse();
    }
    private AdHocCommandData executeCommandWithArgs(String commandNode, Jid jid, String... args) throws Exception {
        AdHocCommand command = adHocCommandManagerForAdmin.getRemoteCommand(jid, commandNode);
        AdHocCommandResult.StatusExecuting result = command.execute().asExecutingOrThrow();
        FillableForm form = result.getFillableForm();

        for (int i = 0; i < args.length; i += 2) {
            FormField field = form.getField(args[i]);
            if (field == null) {
                throw new IllegalStateException("Field " + args[i] + " not found in form");
            }
            if (NON_STRING_FORM_FIELD_TYPES.contains(field.getType())){
                form.setAnswer(args[i], Collections.singletonList(args[i + 1]));
            } else {
                form.setAnswer(args[i], args[i + 1]);
            }
        }

        SubmitForm submitForm = form.getSubmitForm();

        return command.
            complete(submitForm).getResponse();
    }

    private AdHocCommandData createUser(String jid) throws Exception {
        return executeCommandWithArgs(ADD_A_USER, adminConnection.getUser().asEntityBareJid(),
            "accountjid", jid,
            "password", "password",
            "password-verify", "password"
        );
    }

    private AdHocCommandData deleteUser(String jid) throws Exception {
        return executeCommandWithArgs(DELETE_A_USER, adminConnection.getUser().asEntityBareJid(),
            "accountjids", jid
        );
    }

    @SmackIntegrationTest
    public void testGetCommandsForUser() throws Exception {
        DiscoverItems result = adHocCommandManagerForConOne.discoverCommands(conOne.getUser().asEntityBareJid());
        List<DiscoverItems.Item> items = result.getItems();
        List<DiscoverItems.Item> ping = items.stream().filter(item -> item.getNode().equals(REQUEST_PONG_FROM_SERVER)).collect(Collectors.toList());
        List<DiscoverItems.Item> addUser = items.stream().filter(item -> item.getNode().equals(ADD_A_USER)).collect(Collectors.toList());

        assertEquals(1, ping.size());
        assertEquals(0, addUser.size());
    }

    @SmackIntegrationTest
    public void testGetCommandsForAdmin() throws Exception {
        DiscoverItems result = adHocCommandManagerForAdmin.discoverCommands(adminConnection.getUser().asEntityBareJid());
        List<DiscoverItems.Item> items = result.getItems();
        List<DiscoverItems.Item> ping = items.stream().filter(item -> item.getNode().equals(REQUEST_PONG_FROM_SERVER)).collect(Collectors.toList());
        List<DiscoverItems.Item> addUser = items.stream().filter(item -> item.getNode().equals(ADD_A_USER)).collect(Collectors.toList());

        assertEquals(1, ping.size());
        assertEquals(1, addUser.size());
        assertTrue(items.size() > 10);
    }

    //node="http://jabber.org/protocol/admin#add-group-members" name="Add members or admins to a group"
    //node="http://jabber.org/protocol/admin#add-group" name="Create new group"
    @SmackIntegrationTest
    public void testCreateNewGroup() throws Exception {
        AdHocCommandData result = executeCommandWithArgs(CREATE_NEW_GROUP, adminConnection.getUser().asEntityBareJid(),
            "group", "testGroupName",
            "desc", "testGroup Description",
            "members", "admin@example.org",
            "showInRoster", "nobody",
            "displayName", "testGroup Display Name"
        );

        AdHocCommandNote note = result.getNotes().get(0);
        assertEquals(note.getType(), AdHocCommandNote.Type.info);
        assertTrue(note.getValue().contains("Operation finished successfully"));

        //Clean-up
        executeCommandWithArgs(DELETE_GROUP, adminConnection.getUser().asEntityBareJid(),
            "group", "testGroupName"
        );
    }

    //node="http://jabber.org/protocol/admin#add-user" name="Add a User"
    @SmackIntegrationTest
    public void testAddUser() throws Exception {
        final String ADDED_USER_JID = "addusertest" + testRunId + "@example.org";
        AdHocCommandData result = executeCommandWithArgs(ADD_A_USER, adminConnection.getUser().asEntityBareJid(),
            "accountjid", ADDED_USER_JID,
            "password", "password",
            "password-verify", "password"
        );

        AdHocCommandNote note = result.getNotes().get(0);
        assertEquals(note.getType(), AdHocCommandNote.Type.info);
        assertTrue(note.getValue().contains("Operation finished successfully"));

        //Clean-up
        deleteUser(ADDED_USER_JID);
    }

    @SmackIntegrationTest
    public void testAddUserWithoutJid() throws Exception {
        Exception e = assertThrows(IllegalStateException.class, () -> {
            executeCommandWithArgs(ADD_A_USER, adminConnection.getUser().asEntityBareJid(),
                "password", "password",
                "password-verify", "password"
            );
        });
        assertEquals("Not all required fields filled. Missing: [accountjid]", e.getMessage());
    }

    //node="http://jabber.org/protocol/admin#announce" name="Send Announcement to Online Users"
    //node="http://jabber.org/protocol/admin#authenticate-user" name="Authenticate User"
    //node="http://jabber.org/protocol/admin#change-user-password" name="Change User Password"
    //node="http://jabber.org/protocol/admin#delete-group-members" name="Delete members or admins from a group"
    //node="http://jabber.org/protocol/admin#delete-group" name="Delete group"
    //node="http://jabber.org/protocol/admin#delete-user" name="Delete a User"
    @SmackIntegrationTest
    public void testDeleteUser() throws Exception {
        final String DELETED_USER_JID = "deleteusertest" + testRunId + "@example.org";
        createUser(DELETED_USER_JID);
        AdHocCommandData result = executeCommandWithArgs(DELETE_A_USER, adminConnection.getUser().asEntityBareJid(),
            "accountjids", DELETED_USER_JID
        );

        AdHocCommandNote note = result.getNotes().get(0);
        assertEquals(note.getType(), AdHocCommandNote.Type.info);
        assertTrue(note.getValue().contains("Operation finished successfully"));
    }

    //node="http://jabber.org/protocol/admin#disable-user" name="Disable a User"
    @SmackIntegrationTest
    public void testDisableUser() throws Exception {
        final String DISABLED_USER_JID = "disableusertest" + testRunId + "@example.org";
        createUser(DISABLED_USER_JID);
        AdHocCommandData result = executeCommandWithArgs(DISABLE_A_USER, adminConnection.getUser().asEntityBareJid(),
            "accountjids", DISABLED_USER_JID
        );

        AdHocCommandNote note = result.getNotes().get(0);
        assertEquals(note.getType(), AdHocCommandNote.Type.info);
        assertTrue(note.getValue().contains("Operation finished successfully"));

        //Clean-up
        deleteUser(DISABLED_USER_JID);
    }
    //node="http://jabber.org/protocol/admin#edit-admin" name="Edit Admin List"
    //node="http://jabber.org/protocol/admin#edit-blacklist" name="Edit Blocked List"
    //node="http://jabber.org/protocol/admin#edit-whitelist" name="Edit Allowed List"
    //node="http://jabber.org/protocol/admin#end-user-session" name="End User Session"
    //node="http://jabber.org/protocol/admin#get-active-presences" name="Get Presence of Active Users"

    //node="http://jabber.org/protocol/admin#get-active-users-num" name="Get Number of Active Users"
    //@see <a href="https://xmpp.org/extensions/xep-0133.html#get-active-users-num">XEP-0133 Service Administration: Get Number of Active Users</a>
    @SmackIntegrationTest
    public void testGetOnlineUsersNumber() throws Exception {
        final String EXPECTED_ACTIVE_USERS_NUMBER = "4"; // Three defaults, plus this test's extra admin user
        DataForm form = executeCommandSimple(GET_NUMBER_OF_ACTIVE_USERS, adminConnection.getUser().asEntityBareJid()).getForm();
        assertEquals(EXPECTED_ACTIVE_USERS_NUMBER, form.getField("activeusersnum").getFirstValue());
    }

    //node="http://jabber.org/protocol/admin#get-active-users" name="Get List of Active Users"
    @SmackIntegrationTest
    public void testGetOnlineUsersListSimple() throws Exception {
        final List<String> EXPECTED_ACTIVE_USERS = new ArrayList<>(Arrays.asList(
            conOne.getUser().asEntityBareJidString(),
            conTwo.getUser().asEntityBareJidString(),
            conThree.getUser().asEntityBareJidString(),
            adminConnection.getUser().asEntityBareJidString()
        ));

        DataForm form = executeCommandWithArgs(GET_LIST_OF_ACTIVE_USERS, adminConnection.getUser().asEntityBareJid())
            .getForm();
        FormField jids = form.getField("activeuserjids");
        final List<String> values = jids.getValues().stream().map(CharSequence::toString).collect(Collectors.toList());

        assertEquals(EXPECTED_ACTIVE_USERS.size(), values.size());
        assertTrue(values.containsAll(EXPECTED_ACTIVE_USERS));
    }
    @SmackIntegrationTest
    public void testGetOnlineUsersListWithMaxUsers() throws Exception {
        final List<String> EXPECTED_ACTIVE_USERS = new ArrayList<>(Arrays.asList(
                conOne.getUser().asEntityBareJidString(),
                conTwo.getUser().asEntityBareJidString(),
                conThree.getUser().asEntityBareJidString(),
                adminConnection.getUser().asEntityBareJidString()
        ));

        DataForm form = executeCommandWithArgs(GET_LIST_OF_ACTIVE_USERS, adminConnection.getUser().asEntityBareJid(),
            "max_items", "25")
            .getForm();

        FormField jids = form.getField("activeuserjids");
        final List<String> values = jids.getValues().stream().map(CharSequence::toString).collect(Collectors.toList());

        assertEquals(EXPECTED_ACTIVE_USERS.size(), values.size());
        assertTrue(values.containsAll(EXPECTED_ACTIVE_USERS));
    }

    //node="http://jabber.org/protocol/admin#get-console-info" name="Get admin console info."
    @SmackIntegrationTest
    public void testAdminConsoleInfo() throws Exception {
        final String EXPECTED_ADMIN_PORT = "9090";
        final String EXPECTED_ADMIN_SECURE_PORT = "9091";
        DataForm form = executeCommandSimple(GET_ADMIN_CONSOLE_INFO, adminConnection.getUser().asEntityBareJid()).getForm();

        assertTrue(form.getField("bindInterface").getFirstValue().length() >= 7); //Present, and long enough to be an IP Address
        assertEquals(EXPECTED_ADMIN_PORT, form.getField("adminPort").getFirstValue());
        assertEquals(EXPECTED_ADMIN_SECURE_PORT, form.getField("adminSecurePort").getFirstValue());
    }

    //node="http://jabber.org/protocol/admin#get-disabled-users-list" name="Get List of Disabled Users"
    @SmackIntegrationTest
    public void testDisabledUsersListEmpty() throws Exception {
        DataForm form = executeCommandWithArgs(GET_LIST_OF_DISABLED_USERS, adminConnection.getUser().asEntityBareJid(),
            "max_items", "25")
            .getForm();
        assertEquals(0, form.getField("disableduserjids").getValues().size());
    }
    //node="http://jabber.org/protocol/admin#get-disabled-users-num" name="Get Number of Disabled Users"
    //node="http://jabber.org/protocol/admin#get-group-members" name="Get List of Group Members"
    //node="http://jabber.org/protocol/admin#get-groups" name="Get List of Existing Groups"
    //node="http://jabber.org/protocol/admin#get-idle-users-num" name="Get Number of Idle Users"
    //node="http://jabber.org/protocol/admin#get-online-users-list" name="Get List of Online Users"
    //node="http://jabber.org/protocol/admin#get-online-users-num" name="Get Number of Online Users"
    //node="http://jabber.org/protocol/admin#get-registered-users-list" name="Get List of Registered Users"
    //node="http://jabber.org/protocol/admin#get-registered-users-num" name="Get Number of Registered Users"
    //node="http://jabber.org/protocol/admin#get-server-stats" name="Get basic statistics of the server."
    //node="http://jabber.org/protocol/admin#get-sessions-num" name="Get Number of Connected User Sessions"
    //node="http://jabber.org/protocol/admin#get-user-properties" name="Get User Properties"
    //node="http://jabber.org/protocol/admin#get-user-roster" name="Get User Roster"
    //node="http://jabber.org/protocol/admin#reenable-user" name="Re-Enable a User"
    //node="http://jabber.org/protocol/admin#status-http-bind" name="Current Http Bind Status"
    //node="http://jabber.org/protocol/admin#update-group" name="Update group configuration"
    //node="http://jabber.org/protocol/event#group-admin-added" name="Group admin added"
    //node="http://jabber.org/protocol/event#group-admin-removed" name="Group admin removed"
    //node="http://jabber.org/protocol/event#group-created" name="Group deleting"
    //node="http://jabber.org/protocol/event#group-member-added" name="Group member added"
    //node="http://jabber.org/protocol/event#group-member-removed" name="Group member removed"
    //node="http://jabber.org/protocol/event#group-modified" name="Group modified"
    //node="http://jabber.org/protocol/event#user-created" name="User created"
    //node="http://jabber.org/protocol/event#user-deleting" name="Deleting a User"
    //node="http://jabber.org/protocol/event#user-modified" name="User modified"
    //node="http://jabber.org/protocol/event#vcard-created" name="VCard created"
    //node="http://jabber.org/protocol/event#vcard-deleting" name="Deleting a VCard"
    //node="http://jabber.org/protocol/event#vcard-modified" name="VCard modified"
    //node="ping" name="Request pong from server"
}
