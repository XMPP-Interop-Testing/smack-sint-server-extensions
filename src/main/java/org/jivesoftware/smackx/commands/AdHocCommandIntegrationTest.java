package org.jivesoftware.smackx.commands;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AdHocCommandIntegrationTest extends AbstractSmackIntegrationTest {

    private final AdHocCommandManager adHocCommandManagerForAdmin;
    private final AdHocCommandManager adHocCommandManagerForConOne;
    private final AbstractXMPPConnection adminConnection;

    private static final String ADD_USER_COMMAND_NODE = "http://jabber.org/protocol/admin#add-user";
    private static final String GET_ONLINE_USERS_COMMAND_NODE = "http://jabber.org/protocol/admin#get-active-users-num";

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

    @SmackIntegrationTest
    public void testGetCommandsForUser() throws Exception {
        DiscoverItems result = adHocCommandManagerForConOne.discoverCommands(conOne.getUser().asEntityBareJid());
        List<DiscoverItems.Item> items = result.getItems();
        List<DiscoverItems.Item> ping = items.stream().filter(item -> item.getNode().equals("ping")).collect(Collectors.toList());
        List<DiscoverItems.Item> addUser = items.stream().filter(item -> item.getNode().equals(ADD_USER_COMMAND_NODE)).collect(Collectors.toList());

        assertEquals(1, ping.size());
        assertEquals(0, addUser.size());
    }

    @SmackIntegrationTest
    public void testGetCommandsForAdmin() throws Exception {
        DiscoverItems result = adHocCommandManagerForAdmin.discoverCommands(adminConnection.getUser().asEntityBareJid());
        List<DiscoverItems.Item> items = result.getItems();
        List<DiscoverItems.Item> ping = items.stream().filter(item -> item.getNode().equals("ping")).collect(Collectors.toList());
        List<DiscoverItems.Item> addUser = items.stream().filter(item -> item.getNode().equals(ADD_USER_COMMAND_NODE)).collect(Collectors.toList());

        assertEquals(1, ping.size());
        assertEquals(1, addUser.size());
        assertTrue(items.size() > 10);
    }

    @SmackIntegrationTest
    public void testGetOnlineUsersNumber() throws Exception {
        RemoteCommand command = adHocCommandManagerForAdmin.getRemoteCommand(adminConnection.getUser().asEntityBareJid(), GET_ONLINE_USERS_COMMAND_NODE);
        command.execute();
        DataForm form = command.getForm();
        assertEquals("4", form.getField("activeusersnum").getFirstValue());
    }
}
