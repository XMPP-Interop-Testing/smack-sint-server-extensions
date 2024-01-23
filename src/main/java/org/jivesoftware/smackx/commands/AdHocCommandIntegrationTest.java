package org.jivesoftware.smackx.commands;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.SmackXmlParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.form.FillableForm;
import org.jivesoftware.smackx.xdata.form.SubmitForm;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AdHocCommandIntegrationTest extends AbstractSmackIntegrationTest {

    private final AdHocCommandManager adHocCommandManagerForAdmin;
    private final AdHocCommandManager adHocCommandManagerForConOne;
    private final AbstractXMPPConnection adminConnection;
    SmackIntegrationTestEnvironment environment;

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
    private static final String REQUEST_PONG_FROM_SERVER = "ping";

    public AdHocCommandIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws InvocationTargetException, InstantiationException, IllegalAccessException, SmackException, IOException, XMPPException, InterruptedException {
        super(environment);
        this.environment = environment;

        adminConnection = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
        adminConnection.connect();
        adminConnection.login(sinttestConfiguration.adminAccountUsername,
            sinttestConfiguration.adminAccountPassword);

        adHocCommandManagerForConOne = AdHocCommandManager.getInstance(conOne);
        adHocCommandManagerForAdmin = AdHocCommandManager.getInstance(adminConnection);
    }


    private AdHocCommandData executeCommandSimple(String commandNode, Jid jid) throws Exception {
        AdHocCommand command = adHocCommandManagerForAdmin.getRemoteCommand(jid, commandNode);
        return command.execute().getResponse();
    }

    private void fillForm(FillableForm form, String[] args){
        for (int i = 0; i < args.length; i += 2) {
            FormField field = form.getField(args[i]);
            if (field == null) {
                throw new IllegalStateException("Field " + args[i] + " not found in form");
            }
            if (NON_STRING_FORM_FIELD_TYPES.contains(field.getType())){
                form.setAnswer(args[i], Stream.of(args[i + 1]
                        .split(",", -1))
                    .map(String::trim)
                    .collect(Collectors.toList()));
            } else {
                form.setAnswer(args[i], args[i + 1]);
            }
        }
    }

    private AdHocCommandData executeCommandWithArgs(String commandNode, Jid jid, String... args) throws Exception {
        AdHocCommand command = adHocCommandManagerForAdmin.getRemoteCommand(jid, commandNode);
        AdHocCommandResult.StatusExecuting result = command.execute().asExecutingOrThrow();
        FillableForm form = result.getFillableForm();
        fillForm(form, args);

        SubmitForm submitForm = form.getSubmitForm();

        return command.
            complete(submitForm).getResponse();
    }

    private AdHocCommandData executeMultistageCommandWithArgs(String commandNode, Jid jid, String[] args1, String[] args2) throws Exception {
        AdHocCommand command = adHocCommandManagerForAdmin.getRemoteCommand(jid, commandNode);

        AdHocCommandResult.StatusExecuting result = command.execute().asExecutingOrThrow();
        FillableForm form = result.getFillableForm();
        fillForm(form, args1);
        SubmitForm submitForm = form.getSubmitForm();

        result = command.next(submitForm).asExecutingOrThrow();
        form = result.getFillableForm();
        fillForm(form, args2);
        submitForm = form.getSubmitForm();

        return command.
            complete(submitForm).getResponse();
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

    private Set<Jid> getGroupMembers(String groupName) throws Exception {
        DataForm form = executeCommandWithArgs(GET_LIST_OF_GROUP_MEMBERS, adminConnection.getUser().asEntityBareJid(),
            "group", groupName
        ).getForm();

        return form.getItems().stream()
            .map(DataForm.Item::getFields)
            .flatMap(List::stream)
            .filter(field -> field.getFieldName().equals("jid"))
            .map(FormField::getFirstValue)
            .map(JidCreate::fromOrThrowUnchecked)
            .collect(Collectors.toSet());
    }

    private void assertFormFieldEquals(String fieldName, Jid expectedValue, AdHocCommandData data) throws XmppStringprepException {
        FormField field = data.getForm().getField(fieldName);
        assertEquals(expectedValue, JidCreate.from(field.getFirstValue()));
    }

    private void assertFormFieldEquals(String fieldName, String expectedValue, AdHocCommandData data) {
        FormField field = data.getForm().getField(fieldName);
        assertEquals(expectedValue, field.getFirstValue());
    }

    private void assertFormFieldEquals(String fieldName, int expectedValue, AdHocCommandData data) {
        FormField field = data.getForm().getField(fieldName);
        assertEquals(expectedValue, Integer.parseInt(field.getFirstValue()));
    }

    private void assertFormFieldContainsAll(String fieldName, Collection<Jid> expectedValues, AdHocCommandData data) {
        FormField field = data.getForm().getField(fieldName);
        List<String> fieldValues = field.getValues().stream().map(CharSequence::toString).collect(Collectors.toList());
        Set<Jid> reportedValues = fieldValues.stream().map(JidCreate::fromOrThrowUnchecked).collect(Collectors.toSet());
        assertTrue(reportedValues.containsAll(expectedValues));
    }

    private void assertFormFieldJidEquals(String fieldName, Set<Jid> expectedValues, AdHocCommandData data) {
        FormField field = data.getForm().getField(fieldName);
        List<String> fieldValues = field.getValues().stream().map(CharSequence::toString).collect(Collectors.toList());
        assertEquals(expectedValues, fieldValues.stream().map(JidCreate::fromOrThrowUnchecked).collect(Collectors.toSet()));
    }

    private void assertFormFieldEquals(String fieldName, Collection<String> expectedValues, AdHocCommandData data) {
        FormField field = data.getForm().getField(fieldName);
        List<String> fieldValues = field.getValues().stream().map(CharSequence::toString).collect(Collectors.toList());
        assertEquals(expectedValues, fieldValues);
    }

    private void assertFormFieldExists(String fieldName, AdHocCommandData data) {
        FormField field = data.getForm().getField(fieldName);
        assertNotNull(field);
    }

    private void assertFormFieldHasValues(String fieldName, int valueCount, AdHocCommandData data) {
        FormField field = data.getForm().getField(fieldName);
        assertEquals(valueCount, field.getValues().size());
    }

    private void assertNoteType(AdHocCommandNote.Type expectedType, AdHocCommandData data) {
        AdHocCommandNote note = data.getNotes().get(0);
        assertEquals(expectedType, note.getType());
    }

    private void assertNoteContains(String expectedValue, AdHocCommandData data) {
        AdHocCommandNote note = data.getNotes().get(0);
        assertTrue(note.getValue().contains(expectedValue));
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
    @SmackIntegrationTest
    public void testAddGroupMembersNonAdmins() throws Exception {
        final String groupName = "testGroupMembers" + testRunId;
        final List<String> newMembers = Arrays.asList(
            conOne.getUser().asEntityBareJidString(),
            conTwo.getUser().asEntityBareJidString()
        );
        try {
            // Setup test fixture.
            executeCommandWithArgs(CREATE_NEW_GROUP, adminConnection.getUser().asEntityBareJid(),
                "group", groupName,
                "desc", groupName + " Description",
                "showInRoster", "nobody"
            );

            // Execute system under test.
            AdHocCommandData result = executeCommandWithArgs(ADD_MEMBERS_OR_ADMINS_TO_A_GROUP, adminConnection.getUser().asEntityBareJid(),
                "group", groupName,
                "admin", "false",
                "users", String.join(",", newMembers)
            );

            // Verify results.
            assertNoteType(AdHocCommandNote.Type.info, result);
            assertNoteContains("Operation finished successfully", result);
        } finally {
            // Tear down test fixture.
            executeCommandWithArgs(DELETE_GROUP, adminConnection.getUser().asEntityBareJid(),
                "group", groupName
            );
        }
    }

    //node="http://jabber.org/protocol/admin#add-group" name="Create new group"
    @SmackIntegrationTest
    public void testCreateNewGroup() throws Exception {
        // Setup test fixture.
        final String newGroupName = "testGroup" + testRunId;
        try {
            // Execute system under test.
            AdHocCommandData result = executeCommandWithArgs(CREATE_NEW_GROUP, adminConnection.getUser().asEntityBareJid(),
                "group", newGroupName,
                "desc", "testGroup Description",
                "members", "admin@example.org",
                "showInRoster", "nobody",
                "displayName", "testGroup Display Name"
            );

            // Verify results.
            assertNoteType(AdHocCommandNote.Type.info, result);
            assertNoteContains("Operation finished successfully", result);
        } finally {
            // Tear down test fixture.
            executeCommandWithArgs(DELETE_GROUP, adminConnection.getUser().asEntityBareJid(),
                "group", newGroupName
            );
        }
    }

    //node="http://jabber.org/protocol/admin#add-user" name="Add a User"
    @SmackIntegrationTest
    public void testAddUser() throws Exception {
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
        } finally {
            // Tear down test fixture.
            deleteUser(addedUser);
        }
    }

    @SmackIntegrationTest
    public void testAddUserWithoutJid() throws Exception {
        Exception e = assertThrows(IllegalStateException.class, () ->
            executeCommandWithArgs(ADD_A_USER, adminConnection.getUser().asEntityBareJid(),
                "password", "password",
                "password-verify", "password"
        ));
        assertEquals("Not all required fields filled. Missing: [accountjid]", e.getMessage());
    }

    @SmackIntegrationTest
    public void testAddUserWithMismatchedPassword() throws Exception {
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

    @SmackIntegrationTest
    public void testAddUserWithRemoteJid() throws Exception {
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

    @SmackIntegrationTest
    public void testAddUserWithInvalidJid() throws Exception {
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

    //node="http://jabber.org/protocol/admin#announce" name="Send Announcement to Online Users"
    @SmackIntegrationTest
    public void testSendAnnouncementToOnlineUsers() throws Exception {
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

    //node="http://jabber.org/protocol/admin#authenticate-user" name="Authenticate User"
    @SmackIntegrationTest
    public void testAuthenticateUser() throws Exception {
        // Setup test fixture.
        final Jid userToAuthenticate = JidCreate.bareFrom("authenticateusertest-" + testRunId + "@example.org");
        final String password = "password";
        try {
            createUser(userToAuthenticate, password);

            // Execute system under test.
            AdHocCommandData result = executeCommandWithArgs(AUTHENTICATE_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjid", userToAuthenticate.toString(),
                password, password
            );

            // Verify results.
            assertNoteType(AdHocCommandNote.Type.info, result);
            assertNoteContains("Operation finished successfully", result);
        } finally {
            // Tear down test fixture.
            deleteUser(userToAuthenticate);
        }
    }

    @SmackIntegrationTest
    public void testAuthenticateUserWrongPassword() throws Exception {
        // Setup test fixture.
        final Jid userToAuthenticate = JidCreate.bareFrom("authenticateusertestwrongpassword-" + testRunId + "@example.org");
        final String password = "password";
        try {
            createUser(userToAuthenticate, password);

            // Execute system under test.
            AdHocCommandData result = executeCommandWithArgs(AUTHENTICATE_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjid", userToAuthenticate.toString(),
                password, password+"2"
            );

            // Verify results.
            assertNoteType(AdHocCommandNote.Type.error, result);
            assertNoteContains("Authentication failed", result);
        } finally {
            // Tear down test fixture.
            deleteUser(userToAuthenticate);
        }
    }

    @SmackIntegrationTest
    public void testAuthenticateUserNonExistentUser() throws Exception {
        // Setup test fixture.
        final Jid userToAuthenticate = JidCreate.bareFrom("authenticateusertestnonexistentuser-" + testRunId + "@example.org");

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(AUTHENTICATE_USER, adminConnection.getUser().asEntityBareJid(),
            "accountjid", userToAuthenticate.toString(),
            "password", "password"
        );

        // Verify results.
        assertNoteType(AdHocCommandNote.Type.error, result);
        assertNoteContains("User does not exist", result);
    }

    @SmackIntegrationTest
    public void testAuthenticateUserWithRemoteJid() throws Exception {
        // Setup test fixture.
        final Jid userToAuthenticate = JidCreate.bareFrom("authenticateusertestremotejid-" + testRunId + "@somewhereelse.org");

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(AUTHENTICATE_USER, adminConnection.getUser().asEntityBareJid(),
            "accountjid", userToAuthenticate.toString(),
            "password", "password"
        );

        // Verify results.
        assertNoteType(AdHocCommandNote.Type.error, result);
        assertNoteContains("Cannot authenticate remote user", result);
    }

    //node="http://jabber.org/protocol/admin#change-user-password" name="Change User Password"
    @SmackIntegrationTest
    public void testChangePassword() throws Exception {
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
            result = executeCommandWithArgs(AUTHENTICATE_USER, adminConnection.getUser().asEntityBareJid(),
                "accountjid", userToChangePassword.toString(),
                "password", "password2"
            );

            // Verify results.
            assertNoteType(AdHocCommandNote.Type.info, result);
            assertNoteContains("Operation finished successfully", result);
        } finally {
            // Tear down test fixture.
            deleteUser(userToChangePassword);
        }
    }

    //node="http://jabber.org/protocol/admin#delete-group-members" name="Delete members or admins from a group"
    @SmackIntegrationTest
    public void testDeleteGroupMembers() throws Exception {
        // Setup test fixture.
        final String groupName = "testGroupMemberRemoval" + testRunId;
        final List<String> groupMembers = Arrays.asList(
            conOne.getUser().asEntityBareJidString(),
            conTwo.getUser().asEntityBareJidString()
        );
        try {
            executeCommandWithArgs(CREATE_NEW_GROUP, adminConnection.getUser().asEntityBareJid(),
                "group", groupName,
                "desc", groupName + " Description",
                "showInRoster", "nobody"
            );

            executeCommandWithArgs(ADD_MEMBERS_OR_ADMINS_TO_A_GROUP, adminConnection.getUser().asEntityBareJid(),
                "group", groupName,
                "admin", "false",
                "users", String.join(",", groupMembers)
            );

            // Execute system under test.
            AdHocCommandData result = executeCommandWithArgs(DELETE_MEMBERS_OR_ADMINS_FROM_A_GROUP, adminConnection.getUser().asEntityBareJid(),
                "group", groupName,
                "users", conOne.getUser().asEntityBareJidString()
            );

            // Verify results.
            assertNoteType(AdHocCommandNote.Type.info, result);
            assertNoteContains("Operation finished successfully", result);
            Set<Jid> members = getGroupMembers(groupName);
            assertEquals(1, members.size());
            assertTrue(members.contains(conTwo.getUser().asEntityBareJid()));
        } finally {
            // Tear down test fixture.
            executeCommandWithArgs(DELETE_GROUP, adminConnection.getUser().asEntityBareJid(),
                "group", groupName
            );
        }
    }

    //node="http://jabber.org/protocol/admin#delete-group" name="Delete group"
    @SmackIntegrationTest
    public void testDeleteGroup() throws Exception {
        // Setup test fixture.
        final String newGroupName = "testGroup" + testRunId;
        executeCommandWithArgs(CREATE_NEW_GROUP, adminConnection.getUser().asEntityBareJid(),
            "group", newGroupName,
            "desc", "testGroup Description",
            "members", "admin@example.org",
            "showInRoster", "nobody",
            "displayName", "testGroup Display Name"
        );

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(DELETE_GROUP, adminConnection.getUser().asEntityBareJid(),
            "group", newGroupName
        );

        // Verify results.
        assertNoteType(AdHocCommandNote.Type.info, result);
        assertNoteContains("Operation finished successfully", result);
    }

    //node="http://jabber.org/protocol/admin#delete-user" name="Delete a User"
    @SmackIntegrationTest
    public void testDeleteUser() throws Exception {
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

    //node="http://jabber.org/protocol/admin#disable-user" name="Disable a User"
    @SmackIntegrationTest
    public void testDisableUser() throws Exception {
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

    //node="http://jabber.org/protocol/admin#edit-admin" name="Edit Admin List"
    @SmackIntegrationTest
    public void testEditAdminList() throws Exception {
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

    //node="http://jabber.org/protocol/admin#edit-blacklist" name="Edit Blocked List"
    //@SmackIntegrationTest
    // Disabled whilst we can't tidy up after ourselves.
    public void testEditBlackList() throws Exception {
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
            //TODO: FIND A WAY TO RETURN THE BLACKLIST TO AN EMPTY STATE
            //executeCommandWithArgs(EDIT_BLOCKED_LIST, adminConnection.getUser().asEntityBareJid(),
            //    "blacklistjids", null
            //);
        }
    }

    //node="http://jabber.org/protocol/admin#edit-whitelist" name="Edit Allowed List"
    //TODO: Once we know how to fix the blacklist cleanup...

    //node="http://jabber.org/protocol/admin#end-user-session" name="End User Session"
    @SmackIntegrationTest
    public void testEndUserSession() throws Exception {
        final Jid userToEndSession = JidCreate.bareFrom("endsessiontest" + testRunId + "@example.org");
        try {
            createUser(userToEndSession);

            // Fetch user details to get the user loaded
            AdHocCommandData result = executeCommandWithArgs(GET_USER_PROPERTIES, adminConnection.getUser().asEntityBareJid(),
                "accountjids", userToEndSession.toString()
            );

            assertFormFieldExists("accountjids", result);

            // Login as the user to be able to end their session
            AbstractXMPPConnection userConnection = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
            userConnection.connect();
            userConnection.login(userToEndSession.getLocalpartOrThrow().toString(), "password");

            result = executeCommandWithArgs(GET_LIST_OF_ACTIVE_USERS, adminConnection.getUser().asEntityBareJid(),
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

    //node="http://jabber.org/protocol/admin#get-active-presences" name="Get Presence of Active Users"
    @SmackIntegrationTest
    public void testGetPresenceOfActiveUsers() throws Exception {
        // Setup test fixture.
        final List<Jid> expectedPresences = Arrays.asList(
            conOne.getUser().asEntityBareJid(),
            conTwo.getUser().asEntityBareJid(),
            conThree.getUser().asEntityBareJid(),
            adminConnection.getUser().asEntityBareJid()
        );

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(GET_PRESENCE_OF_ACTIVE_USERS, adminConnection.getUser().asEntityBareJid(),
            "max_items", "25"
        );

        // Verify results.
        assertFormFieldHasValues("activeuserpresences", 5, result); //3 SINT users, plus 2 from the admin

        List<Presence> presences = result.getForm().getField("activeuserpresences").getValues().stream()
            .map(CharSequence::toString)
            .map(s -> {
                try {
                    return SmackXmlParser.newXmlParser(new StringReader(s));
                } catch (XmlPullParserException e) {
                    throw new RuntimeException(e);
                }
            })
            .map(parser -> {
                try {
                    parser.next();
                    return PacketParserUtils.parsePresence(parser);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .collect(Collectors.toList());

        assertTrue(presences.stream().allMatch(presence -> presence.getType() == Presence.Type.available));
        assertTrue(presences.stream().allMatch(presence -> expectedPresences.contains(presence.getFrom().asEntityBareJidOrThrow())));
    }

    //node="http://jabber.org/protocol/admin#get-active-users-num" name="Get Number of Active Users"
    //@see <a href="https://xmpp.org/extensions/xep-0133.html#get-active-users-num">XEP-0133 Service Administration: Get Number of Active Users</a>
    @SmackIntegrationTest
    public void testGetActiveUsersNumber() throws Exception {
        // Execute system under test.
        DataForm form = executeCommandSimple(GET_NUMBER_OF_ACTIVE_USERS, adminConnection.getUser().asEntityBareJid()).getForm();

        // Verify results.
        final int expectedMinimumCount = 3; // Each test runs with at least three test accounts (but more users might be active!)
        assertTrue(Integer.parseInt(form.getField("activeusersnum").getFirstValue()) >= expectedMinimumCount);
    }

    //node="http://jabber.org/protocol/admin#get-active-users" name="Get List of Active Users"
    @SmackIntegrationTest
    public void testGetActiveUsersListSimple() throws Exception {
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
    @SmackIntegrationTest
    public void testGetOnlineUsersListWithMaxUsers() throws Exception {
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

    //node="http://jabber.org/protocol/admin#get-console-info" name="Get admin console info."
    @SmackIntegrationTest
    public void testAdminConsoleInfo() throws Exception {
        // Execute system under test.
        AdHocCommandData result = executeCommandSimple(GET_ADMIN_CONSOLE_INFO, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        final int expectedAdminPort = 9090;
        final int expectedAdminSecurePort = 9091;
        assertFormFieldEquals("adminPort", expectedAdminPort, result);
        assertFormFieldEquals("adminSecurePort", expectedAdminSecurePort, result);
        assertFormFieldExists("bindInterface", result);
    }

    //node="http://jabber.org/protocol/admin#get-disabled-users-list" name="Get List of Disabled Users"
    @SmackIntegrationTest
    public void testDisabledUsersListEmpty() throws Exception {
        // Setup test fixture.
        // TODO clear the list

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(GET_LIST_OF_DISABLED_USERS, adminConnection.getUser().asEntityBareJid(),
            "max_items", "25");

        // Verify results.
        assertFormFieldEquals("disableduserjids", new ArrayList<>(), result);
    }

    @SmackIntegrationTest
    public void testDisabledUsersList() throws Exception {
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

    //node="http://jabber.org/protocol/admin#get-disabled-users-num" name="Get Number of Disabled Users"
    @SmackIntegrationTest
    public void testDisabledUsersNumber() throws Exception {
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

    //node="http://jabber.org/protocol/admin#get-group-members" name="Get List of Group Members"
    @SmackIntegrationTest
    public void testGetGroupMembers() throws Exception {
        final String groupName = "testGroupMembers" + testRunId;
        try {
            // Setup test fixture.
            final Set<Jid> groupMembers = new HashSet<>(Arrays.asList(
                conOne.getUser().asEntityBareJid(),
                conTwo.getUser().asEntityBareJid()
            ));
            executeCommandWithArgs(CREATE_NEW_GROUP, adminConnection.getUser().asEntityBareJid(),
                "group", groupName,
                "desc", groupName + " Description",
                "showInRoster", "nobody"
            );
            executeCommandWithArgs(ADD_MEMBERS_OR_ADMINS_TO_A_GROUP, adminConnection.getUser().asEntityBareJid(),
                "group", groupName,
                "admin", "false",
                "users", String.join(",", groupMembers)
            );

            // Execute system under test.
            Set<Jid> members = getGroupMembers(groupName);

            // Verify results.
            assertEquals(groupMembers, members);
        } finally {
            // Tear down test fixture.
            executeCommandWithArgs(DELETE_GROUP, adminConnection.getUser().asEntityBareJid(),
                "group", groupName
            );
        }
    }

    //node="http://jabber.org/protocol/admin#get-groups" name="Get List of Existing Groups"
    @SmackIntegrationTest
    public void testGetGroups() throws Exception {
        // Setup test fixture.
        final String groupName = "testGetGroups" + testRunId;
        final String groupDescription = "testGetGroups Description";
        final String groupDisplayName = "testGetGroups Display Name";
        final String groupShowInRoster = "nobody";
        executeCommandWithArgs(CREATE_NEW_GROUP, adminConnection.getUser().asEntityBareJid(),
            "group", groupName,
            "desc", groupDescription,
            "showInRoster", groupShowInRoster,
            "displayName", groupDisplayName
        );
        try {
            // Execute system under test.
            AdHocCommandData result = executeCommandWithArgs(GET_LIST_OF_EXISTING_GROUPS, adminConnection.getUser().asEntityBareJid());

            // Verify results.
            List<String> groupNames = result.getForm().getItems().stream()
                .map(item -> item.getFields().stream().filter(field -> field.getVariable().equals("name")).collect(Collectors.toList()))
                .map(fields -> fields.get(0).getValues().get(0))
                .map(CharSequence::toString)
                .collect(Collectors.toList());

            Map<String, String> group1Props = result.getForm().getItems().get(0).getFields().stream()
                .collect(Collectors.toMap(
                    field -> field.getVariable(),
                    field -> field.getValues().get(0).toString()
                ));

            assertEquals(1, groupNames.size());
            assertTrue(groupNames.contains(groupName));
            assertEquals(groupName, group1Props.get("name"));
            assertEquals(groupDescription, group1Props.get("desc"));
            assertEquals("false", group1Props.get("shared"));
            assertEquals("0", group1Props.get("count"));
        } finally {
            // Tear down test fixture.
            executeCommandWithArgs(DELETE_GROUP, adminConnection.getUser().asEntityBareJid(),
                "group", groupName
            );
        }
    }

    //node="http://jabber.org/protocol/admin#get-idle-users-num" name="Get Number of Idle Users"
    @SmackIntegrationTest
    public void testGetIdleUsersNumber() throws Exception {
        // Execute system under test.
        AdHocCommandData result = executeCommandSimple(GET_NUMBER_OF_IDLE_USERS, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        assertTrue(Integer.parseInt(result.getForm().getField("idleusersnum").getFirstValue()) >= 0);
    }

    //node="http://jabber.org/protocol/admin#get-online-users-list" name="Get List of Online Users"
    @SmackIntegrationTest
    public void testGetOnlineUsersListSimple() throws Exception {
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

    //node="http://jabber.org/protocol/admin#get-online-users-num" name="Get Number of Online Users"
    @SmackIntegrationTest
    public void testGetOnlineUsersNumber() throws Exception {
        // Execute system under test.
        DataForm form = executeCommandSimple(GET_NUMBER_OF_ONLINE_USERS, adminConnection.getUser().asEntityBareJid()).getForm();

        // Verify results.
        final int expectedMinimumCount = 3; // Each test runs with at least three test accounts (but more users might be active!)
        assertTrue(Integer.parseInt(form.getField("onlineusersnum").getFirstValue()) >= expectedMinimumCount);
    }

    //node="http://jabber.org/protocol/admin#get-registered-users-list" name="Get List of Registered Users"
    @SmackIntegrationTest
    public void testGetRegisteredUsersList() throws Exception {
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

    //node="http://jabber.org/protocol/admin#get-registered-users-num" name="Get Number of Registered Users"
    @SmackIntegrationTest
    public void testGetRegisteredUsersNumber() throws Exception {
        // Execute system under test.
        AdHocCommandData result = executeCommandSimple(GET_NUMBER_OF_REGISTERED_USERS, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        final int expectedMinimumCount = 3; // Each test runs with at least three registered test accounts (but more users might be active!)
        assertTrue(Integer.parseInt(result.getForm().getField("registeredusersnum").getFirstValue()) >= expectedMinimumCount);
    }

    //node="http://jabber.org/protocol/admin#get-server-stats" name="Get basic statistics of the server."
    @SmackIntegrationTest
    public void testGetServerStats() throws Exception {
        // Execute System under test.
        AdHocCommandData result = executeCommandSimple(GET_BASIC_STATISTICS_OF_THE_SERVER, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        assertFormFieldEquals("name", "Openfire", result);
        assertFormFieldExists("version", result);
        assertFormFieldExists("domain", result);
        assertFormFieldExists("os", result);
        assertFormFieldExists("uptime", result);
        assertTrue(Integer.parseInt(result.getForm().getField("activeusersnum").getFirstValue()) >= 3); // At _least_ 3 test users
        assertTrue(Integer.parseInt(result.getForm().getField("sessionsnum").getFirstValue()) >= 3);  // At _least_ 3 test users
    }

    //node="http://jabber.org/protocol/admin#get-sessions-num" name="Get Number of Connected User Sessions"
    @SmackIntegrationTest
    public void testGetSessionsNumber() throws Exception {
        // Execute system under test.
        AdHocCommandData result = executeCommandSimple(GET_NUMBER_OF_CONNECTED_USER_SESSIONS, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        final int expectedMinimumCount = 3; // Each test runs with at least three test accounts (but more users might be active!)
        assertTrue(Integer.parseInt(result.getForm().getField("onlineuserssessionsnum").getFirstValue()) >= expectedMinimumCount);
    }

    //node="http://jabber.org/protocol/admin#get-user-properties" name="Get User Properties"
    @SmackIntegrationTest
    public void testUserProperties() throws Exception {
        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(GET_USER_PROPERTIES, adminConnection.getUser().asEntityBareJid(),
            "accountjids", adminConnection.getUser().asEntityBareJidString()
        );

        // Verify results.
        // TODO: Find a way to not depend on hard-coded values.
        assertFormFieldEquals("name", "Administrator", result);
        assertFormFieldEquals("email", "admin@example.com", result);
    }

    @SmackIntegrationTest
    public void testUserPropertiesWithMultipleUsers() throws Exception {
        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(GET_USER_PROPERTIES, adminConnection.getUser().asEntityBareJid(),
            "accountjids", adminConnection.getUser().asEntityBareJidString() + "," + conOne.getUser().asEntityBareJidString()
        );

        // Verify results.
        // TODO: Find a way to not depend on hard-coded values.
        assertFormFieldEquals("name", new ArrayList<>(Arrays.asList("Administrator", "")), result); // Because SINT users have no name
        assertFormFieldEquals("email", new ArrayList<>(Arrays.asList("admin@example.com", "")), result); // Because SINT users have no email
    }

    //node="http://jabber.org/protocol/admin#get-user-roster" name="Get User Roster"
    @SmackIntegrationTest
    public void testUserRoster() throws Exception {
        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(GET_USER_ROSTER, adminConnection.getUser().asEntityBareJid(),
            "accountjids", adminConnection.getUser().asEntityBareJidString()
        );

        // Verify results.
        // TODO: Actually populate a roster of one of the test accounts, instead of depending on an assumed state of the roster of the admin user.
        assertFormFieldJidEquals("accountjids", Collections.singleton(adminConnection.getUser().asEntityBareJid()), result);
    }

    //node="http://jabber.org/protocol/admin#reenable-user" name="Re-Enable a User"
    @SmackIntegrationTest
    public void testReenableUser() throws Exception {
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

    @SmackIntegrationTest
    public void testReenableNonDisabledUser() throws Exception {
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

    @SmackIntegrationTest
    public void testReenableNonExistingUser() throws Exception {
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

    @SmackIntegrationTest
    public void testReenableRemoteUser() throws Exception {
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

    //node="http://jabber.org/protocol/admin#status-http-bind" name="Current Http Bind Status"
    @SmackIntegrationTest
    public void testHttpBindStatus() throws Exception {
        // Execute system under test.
        AdHocCommandData result = executeCommandSimple(CURRENT_HTTP_BIND_STATUS, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        assertFormFieldEquals("httpbindenabled", "true", result);
        assertFormFieldEquals("httpbindaddress", "http://example.org:7070/http-bind/", result);
        assertFormFieldEquals("httpbindsecureaddress", "https://example.org:7443/http-bind/", result);
        assertFormFieldEquals("javascriptaddress", "http://example.org:7070/scripts/", result);
        assertFormFieldEquals("websocketaddress", "ws://example.org:7070/ws/", result);
        assertFormFieldEquals("websocketsecureaddress", "wss://example.org:7443/ws/", result);
    }

    //node="http://jabber.org/protocol/admin#update-group" name="Update group configuration"
    @SmackIntegrationTest
    public void testUpdateGroupConfiguration() throws Exception {
        final String groupName = "testUpdateGroupConfiguration" + testRunId;
        final String groupDescription = "testUpdateGroupConfiguration Description";
        final String groupShowInRoster = "nobody";
        final String updatedGroupName = "testUpdateGroupConfigurationUpdated" + testRunId;
        final String updatedGroupDescription = "testUpdateGroupConfigurationUpdated Description";

        try {
            // Setup test fixture.
            executeCommandWithArgs(CREATE_NEW_GROUP, adminConnection.getUser().asEntityBareJid(),
                "group", groupName,
                "desc", groupDescription,
                "showInRoster", groupShowInRoster
            );

            // Execute system under test.
            AdHocCommandData result = executeMultistageCommandWithArgs(UPDATE_GROUP_CONFIGURATION, adminConnection.getUser().asEntityBareJid(),
                new String[]{
                    "group", groupName
                },
                new String[]{
                    //"group", UPDATED_GROUP_NAME,
                    "desc", updatedGroupDescription,
                    "showInRoster", groupShowInRoster
                }
            );

            // Verify results.
            assertNoteType(AdHocCommandNote.Type.info, result);
            assertNoteContains("Operation finished successfully", result);
            result = executeCommandWithArgs(GET_LIST_OF_EXISTING_GROUPS, adminConnection.getUser().asEntityBareJid());

            List<String> groupNames = result.getForm().getItems().stream()
                .map(item -> item.getFields().stream().filter(field -> field.getVariable().equals("name")).collect(Collectors.toList()))
                .map(fields -> fields.get(0).getValues().get(0))
                .map(CharSequence::toString)
                .collect(Collectors.toList());

            Map<String, String> group1Props = result.getForm().getItems().get(0).getFields().stream()
                .collect(Collectors.toMap(
                    field -> field.getVariable(),
                    field -> field.getValues().get(0).toString()
                ));

            assertEquals(1, groupNames.size());
            //assertTrue(groupNames.contains(updatedGroupName));
            //assertEquals(updatedGroupName, group1Props.get("name"));
            assertEquals(updatedGroupDescription, group1Props.get("desc"));
            assertEquals("false", group1Props.get("shared"));
        } finally {
            // Tear down test fixture.
            executeCommandWithArgs(DELETE_GROUP, adminConnection.getUser().asEntityBareJid(),
                "group", groupName
                //"group", UPDATED_GROUP_NAME
            );
        }
    }

    //node="ping" name="Request pong from server"
    @SmackIntegrationTest
    public void testPing() throws Exception {
        // Execute System Under test.
        AdHocCommandData result = executeCommandSimple(REQUEST_PONG_FROM_SERVER, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        assertFormFieldExists("timestamp", result);
        String timestampString = result.getForm().getField("timestamp").getFirstValue();
        LocalDateTime timestamp = LocalDateTime.parse(timestampString, DateTimeFormatter.ISO_DATE_TIME);
        assertTrue(timestamp.isAfter(LocalDateTime.now().minusMinutes(2)));
        assertTrue(timestamp.isBefore(LocalDateTime.now().plusMinutes(2)));
    }
}
