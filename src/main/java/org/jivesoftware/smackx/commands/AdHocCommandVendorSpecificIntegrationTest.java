package org.jivesoftware.smackx.commands;

import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.SmackXmlParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData;
import org.jivesoftware.smackx.iqversion.VersionManager;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class AdHocCommandVendorSpecificIntegrationTest extends AbstractAdHocCommandIntegrationTest {
    private static final String ADD_MEMBERS_OR_ADMINS_TO_A_GROUP = "http://jabber.org/protocol/admin#add-group-members";
    private static final String CREATE_NEW_GROUP = "http://jabber.org/protocol/admin#add-group";
    private static final String AUTHENTICATE_USER = "http://jabber.org/protocol/admin#authenticate-user";
    private static final String DELETE_MEMBERS_OR_ADMINS_FROM_A_GROUP = "http://jabber.org/protocol/admin#delete-group-members";
    private static final String DELETE_GROUP = "http://jabber.org/protocol/admin#delete-group";
    private static final String GET_PRESENCE_OF_ACTIVE_USERS = "http://jabber.org/protocol/admin#get-active-presences";
    private static final String GET_ADMIN_CONSOLE_INFO = "http://jabber.org/protocol/admin#get-console-info";
    private static final String GET_LIST_OF_GROUP_MEMBERS = "http://jabber.org/protocol/admin#get-group-members";
    private static final String GET_LIST_OF_EXISTING_GROUPS = "http://jabber.org/protocol/admin#get-groups";
    private static final String GET_BASIC_STATISTICS_OF_THE_SERVER = "http://jabber.org/protocol/admin#get-server-stats";
    private static final String GET_NUMBER_OF_CONNECTED_USER_SESSIONS = "http://jabber.org/protocol/admin#get-sessions-num";
    private static final String GET_USER_PROPERTIES = "http://jabber.org/protocol/admin#get-user-properties";
    private static final String CURRENT_HTTP_BIND_STATUS = "http://jabber.org/protocol/admin#status-http-bind";
    private static final String UPDATE_GROUP_CONFIGURATION = "http://jabber.org/protocol/admin#update-group";
    private static final String REQUEST_PONG_FROM_SERVER = "ping";

    public AdHocCommandVendorSpecificIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws InvocationTargetException, InstantiationException, IllegalAccessException, SmackException, IOException, XMPPException, InterruptedException, TestNotPossibleException {
        super(environment);

        VersionManager versionManagerOne = VersionManager.getInstanceFor(conOne);
        final boolean supported = (versionManagerOne.isSupported(conOne.getXMPPServiceDomain())) && (versionManagerOne.getVersion(conOne.getXMPPServiceDomain()).getName().equals("Openfire"));
        if (!supported) {
            throw new TestNotPossibleException("This test can only be run on Openfire");
        }
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

    //node="http://jabber.org/protocol/admin#add-group-members" name="Add members or admins to a group"
    @SmackIntegrationTest
    public void testAddGroupMembersNonAdmins() throws Exception {
        checkServerSupportCommand(ADD_MEMBERS_OR_ADMINS_TO_A_GROUP);
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
        checkServerSupportCommand(CREATE_NEW_GROUP);
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

    //node="http://jabber.org/protocol/admin#authenticate-user" name="Authenticate User"
    @SmackIntegrationTest
    public void testAuthenticateUser() throws Exception {
        checkServerSupportCommand(AUTHENTICATE_USER);

        // Setup test fixture.
        final String userToAuthenticate = conOne.getUser().asEntityBareJid().toString();
        final String password = sinttestConfiguration.accountOnePassword;

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(AUTHENTICATE_USER, adminConnection.getUser().asEntityBareJid(),
            "accountjid", userToAuthenticate,
            "password", password
        );

        // Verify results.
        assertNoteType(AdHocCommandNote.Type.info, result);
        assertNoteContains("Operation finished successfully", result);

    }

    @SmackIntegrationTest
    public void testAuthenticateUserWrongPassword() throws Exception {
        checkServerSupportCommand(AUTHENTICATE_USER);

        // Setup test fixture.
        final String userToAuthenticate = conOne.getUser().asEntityBareJid().toString();
        final String password = "incorrect-password";


        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(AUTHENTICATE_USER, adminConnection.getUser().asEntityBareJid(),
            "accountjid", userToAuthenticate,
            "password", password
        );

        // Verify results.
        assertNoteType(AdHocCommandNote.Type.error, result);
        assertNoteContains("Authentication failed", result);
    }

    @SmackIntegrationTest
    public void testAuthenticateUserNonExistentUser() throws Exception {
        checkServerSupportCommand(AUTHENTICATE_USER);

        // Setup test fixture.
        final String userToAuthenticate = JidCreate.bareFrom("authenticateusertestnonexistentuser-" + testRunId + "@example.org").toString();

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(AUTHENTICATE_USER, adminConnection.getUser().asEntityBareJid(),
            "accountjid", userToAuthenticate,
            "password", "password"
        );

        // Verify results.
        assertNoteType(AdHocCommandNote.Type.error, result);
        assertNoteContains("User does not exist", result);
    }

    @SmackIntegrationTest
    public void testAuthenticateUserWithRemoteJid() throws Exception {
        checkServerSupportCommand(AUTHENTICATE_USER);
        // Setup test fixture.
        final String userToAuthenticate = JidCreate.bareFrom("authenticateusertestremotejid-" + testRunId + "@somewhereelse.org").toString();

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(AUTHENTICATE_USER, adminConnection.getUser().asEntityBareJid(),
            "accountjid", userToAuthenticate,
            "password", "password"
        );

        // Verify results.
        assertNoteType(AdHocCommandNote.Type.error, result);
        assertNoteContains("Cannot authenticate remote user", result);
    }

    //node="http://jabber.org/protocol/admin#delete-group-members" name="Delete members or admins from a group"
    @SmackIntegrationTest
    public void testDeleteGroupMembers() throws Exception {
        checkServerSupportCommand(DELETE_MEMBERS_OR_ADMINS_FROM_A_GROUP);
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
        checkServerSupportCommand(DELETE_GROUP);
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

    //node="http://jabber.org/protocol/admin#get-active-presences" name="Get Presence of Active Users"
    @SmackIntegrationTest
    public void testGetPresenceOfActiveUsers() throws Exception {
        checkServerSupportCommand(GET_PRESENCE_OF_ACTIVE_USERS);

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

    //node="http://jabber.org/protocol/admin#get-console-info" name="Get admin console info."
    @SmackIntegrationTest
    public void testAdminConsoleInfo() throws Exception {
        checkServerSupportCommand(GET_ADMIN_CONSOLE_INFO);
        // Execute system under test.
        AdHocCommandData result = executeCommandSimple(GET_ADMIN_CONSOLE_INFO, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        final int expectedAdminPort = 9090;
        final int expectedAdminSecurePort = 9091;
        assertFormFieldEquals("adminPort", expectedAdminPort, result);
        assertFormFieldEquals("adminSecurePort", expectedAdminSecurePort, result);
        assertFormFieldExists("bindInterface", result);
    }

    //node="http://jabber.org/protocol/admin#get-group-members" name="Get List of Group Members"
    @SmackIntegrationTest
    public void testGetGroupMembers() throws Exception {
        checkServerSupportCommand(GET_LIST_OF_GROUP_MEMBERS);

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
        checkServerSupportCommand(GET_LIST_OF_EXISTING_GROUPS);

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
                .map(item -> item.getFields().stream().filter(field -> field.getFieldName().equals("name")).collect(Collectors.toList()))
                .map(fields -> fields.get(0).getValues().get(0))
                .map(CharSequence::toString)
                .collect(Collectors.toList());

            Map<String, String> group1Props = result.getForm().getItems().get(0).getFields().stream()
                .collect(Collectors.toMap(
                    FormField::getFieldName,
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

    //node="http://jabber.org/protocol/admin#get-server-stats" name="Get basic statistics of the server."
    @SmackIntegrationTest
    public void testGetServerStats() throws Exception {
        checkServerSupportCommand(GET_BASIC_STATISTICS_OF_THE_SERVER);

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
        checkServerSupportCommand(GET_NUMBER_OF_CONNECTED_USER_SESSIONS);

        // Execute system under test.
        AdHocCommandData result = executeCommandSimple(GET_NUMBER_OF_CONNECTED_USER_SESSIONS, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        final int expectedMinimumCount = 3; // Each test runs with at least three test accounts (but more users might be active!)
        assertTrue(Integer.parseInt(result.getForm().getField("onlineuserssessionsnum").getFirstValue()) >= expectedMinimumCount);
    }

    //node="http://jabber.org/protocol/admin#get-user-properties" name="Get User Properties"
    @SmackIntegrationTest
    public void testUserProperties() throws Exception {
        checkServerSupportCommand(GET_USER_PROPERTIES);

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
        checkServerSupportCommand(GET_USER_PROPERTIES);

        // Execute system under test.
        AdHocCommandData result = executeCommandWithArgs(GET_USER_PROPERTIES, adminConnection.getUser().asEntityBareJid(),
            "accountjids", adminConnection.getUser().asEntityBareJidString() + "," + conOne.getUser().asEntityBareJidString()
        );

        // Verify results.
        // TODO: Find a way to not depend on hard-coded values.
        assertFormFieldEquals("name", new ArrayList<>(Arrays.asList("Administrator", "")), result); // Because SINT users have no name
        assertFormFieldEquals("email", new ArrayList<>(Arrays.asList("admin@example.com", "")), result); // Because SINT users have no email
    }

    //node="http://jabber.org/protocol/admin#status-http-bind" name="Current Http Bind Status"
    @SmackIntegrationTest
    public void testHttpBindStatus() throws Exception {
        checkServerSupportCommand(CURRENT_HTTP_BIND_STATUS);

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
        checkServerSupportCommand(UPDATE_GROUP_CONFIGURATION);

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
                .map(item -> item.getFields().stream().filter(field -> field.getFieldName().equals("name")).collect(Collectors.toList()))
                .map(fields -> fields.get(0).getValues().get(0))
                .map(CharSequence::toString)
                .collect(Collectors.toList());

            Map<String, String> group1Props = result.getForm().getItems().get(0).getFields().stream()
                .collect(Collectors.toMap(
                    FormField::getFieldName,
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
        checkServerSupportCommand(REQUEST_PONG_FROM_SERVER);

        // Execute System Under test.
        AdHocCommandData result = executeCommandSimple(REQUEST_PONG_FROM_SERVER, adminConnection.getUser().asEntityBareJid());

        // Verify results.
        assertFormFieldExists("timestamp", result);
        String timestampString = result.getForm().getField("timestamp").getFirstValue();
        ZonedDateTime timestamp = ZonedDateTime.parse(timestampString, DateTimeFormatter.ISO_DATE_TIME);
        assertTrue(timestamp.isAfter(ZonedDateTime.now().minusMinutes(2)));
        assertTrue(timestamp.isBefore(ZonedDateTime.now().plusMinutes(2)));
    }
}
