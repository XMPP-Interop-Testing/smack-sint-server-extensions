/**
 * Copyright 2025 Guus der Kinderen
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
package org.igniterealtime.smack.inttest.util;

import org.igniterealtime.smack.inttest.*;
import org.igniterealtime.smack.inttest.debugger.SinttestDebugger;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smackx.admin.ServiceAdministrationManager;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods to manage test accounts on the domain.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 */
public class AccountUtilities
{
    protected static final Logger LOGGER = Logger.getLogger(AccountUtilities.class.getName());

    /**
     * Create a new account on the domain. This helps ensure that the target of the stanzas sent by these tests is an
     * existing account that does not have any resources available or connected.
     */
    public static void createNonConnectedLocalUser(final SmackIntegrationTestEnvironment environment, final String username, final String password) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        // TODO Although it is desirable to use the provisioning API in Smack's Integration Testing Framework (as we do here), find a better way than this reflection-based one.
        final Method method = environment.connectionManager.getClass().getDeclaredMethod("registerAccount", String.class, String.class);
        try {
            method.setAccessible(true);
            method.invoke(environment.connectionManager, username, password);
        } finally {
            method.setAccessible(false);
        }
    }

    /**
     * Removes an account from the domain.
     */
    public static void removeNonConnectedLocalUser(final SmackIntegrationTestEnvironment environment, final String username, final String password) throws InvocationTargetException, InstantiationException, IllegalAccessException
    {
        // TODO Much of this is copied directly from the XmppConnectionManager class. As it is highly desirable to use the provisioning API in Smack's Integration Testing Framework (which is basically copied hiere), find a way to make direct use of that API (rather than copying code).
        if (environment.configuration.accountRegistration == Configuration.AccountRegistration.inBandRegistration) {
            // Note that we use the account manager from the to-be-deleted connection.
            final AbstractXMPPConnection cleanUpConnection = environment.connectionManager.getDefaultConnectionDescriptor().construct(environment.configuration);
            try {
                cleanUpConnection.connect();
                cleanUpConnection.login(username, password);

                AccountManager accountManager = AccountManager.getInstance(cleanUpConnection);
                accountManager.deleteAccount();
            } catch (Throwable e) {
                LOGGER.log(Level.WARNING, "Could not delete the dynamically registered additional account named '" + username + "'", e);
            } finally {
                cleanUpConnection.disconnect();
            }
        }

        if (environment.configuration.accountRegistration == Configuration.AccountRegistration.serviceAdministration) {
            Localpart usernameAsLocalpart;
            try {
                usernameAsLocalpart = Localpart.from(username);
            } catch (XmppStringprepException e) {
                throw new AssertionError(e);
            }

            EntityBareJid connectionAddress = JidCreate.entityBareFrom(usernameAsLocalpart, environment.configuration.service);

            try {
                final Field field = environment.connectionManager.getClass().getDeclaredField("adminManager");
                try {
                    field.setAccessible(true);
                    final ServiceAdministrationManager adminManager = (ServiceAdministrationManager) field.get(environment.connectionManager);
                    adminManager.deleteUser(connectionAddress);
                } finally {
                    field.setAccessible(false);
                }
            } catch (Throwable e) {
                LOGGER.log(Level.WARNING, "Could not delete the dynamically registered additional account named '" + username + "'", e);
            }
        }
    }

    /**
     * Creates a new (unconnected) XMPP connection.
     */
    // FIXME A method like this aught to be provided by SINT's XmppConnectionManager class.
    public static AbstractXMPPConnection spawnNewConnection(final SmackIntegrationTestEnvironment environment, final Configuration sinttestConfiguration) throws InvocationTargetException, InstantiationException, IllegalAccessException
    {
        List<ConnectionConfigurationBuilderApplier> connectionConfigurationAppliers = new ArrayList<>();

        final XmppConnectionManager connectionManager = environment.connectionManager;

        // Nasty reflection to get the configured debugger.
        Field field;
        try {
            field = connectionManager.getClass().getDeclaredField("sinttestFramework");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        boolean wasAccessible = field.isAccessible();
        field.setAccessible(true);
        final SmackIntegrationTestFramework sinttestFramework = (SmackIntegrationTestFramework) field.get(connectionManager);
        field.setAccessible(wasAccessible);

        try {
            field = sinttestFramework.getClass().getDeclaredField("sinttestDebugger");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        wasAccessible = field.isAccessible();
        field.setAccessible(true);
        final SinttestDebugger sinttestDebugger = (SinttestDebugger) field.get(sinttestFramework);
        field.setAccessible(wasAccessible);

        if (sinttestDebugger != null) {
            var applier = sinttestDebugger.getConnectionConfigurationBuilderApplier();
            connectionConfigurationAppliers.add(applier);
        }

        return connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration, connectionConfigurationAppliers);
    }
}
