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

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.commands.AdHocCommand;
import org.jivesoftware.smackx.commands.AdHocCommandManager;
import org.jivesoftware.smackx.commands.AdHocCommandNote;
import org.jivesoftware.smackx.commands.AdHocCommandResult;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.form.FillableForm;
import org.jivesoftware.smackx.xdata.form.SubmitForm;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractAdHocCommandIntegrationTest extends AbstractSmackIntegrationTest {

    public final AdHocCommandManager adHocCommandManagerForAdmin;
    public final AdHocCommandManager adHocCommandManagerForConOne;
    public final AbstractXMPPConnection adminConnection;
    SmackIntegrationTestEnvironment environment;

    public AbstractAdHocCommandIntegrationTest(SmackIntegrationTestEnvironment environment) throws SmackException, IOException, XMPPException, InterruptedException, InvocationTargetException, InstantiationException, IllegalAccessException {
        super(environment);
        this.environment = environment;

        adminConnection = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
        adminConnection.connect();
        adminConnection.login(sinttestConfiguration.adminAccountUsername,
            sinttestConfiguration.adminAccountPassword);

        adHocCommandManagerForConOne = AdHocCommandManager.getInstance(conOne);
        adHocCommandManagerForAdmin = AdHocCommandManager.getInstance(adminConnection);
    }

    public static final List<FormField.Type> MULTI_VALUE_FORM_TYPES = Arrays.asList(
        FormField.Type.jid_multi,
        FormField.Type.list_multi,
        FormField.Type.text_multi
    );

    void fillForm(FillableForm form, String[] args){
        for (int i = 0; i < args.length; i += 2) {
            FormField field = form.getField(args[i]);
            if (field == null) {
                throw new IllegalStateException("Field " + args[i] + " not found in form");
            }
            if (MULTI_VALUE_FORM_TYPES.contains(field.getType())) {
                if(args[i + 1].isEmpty()){
                    form.setAnswer(args[i], Collections.emptyList());
                } else {
                    form.setAnswer(args[i], Stream.of(args[i + 1]
                            .split(",", -1))
                        .map(String::trim)
                        .collect(Collectors.toList()));
                }
            } else {
                form.setAnswer(args[i], args[i + 1]);
            }
        }
    }

    AdHocCommandData executeCommandSimple(String commandNode, Jid jid) throws Exception {
        AdHocCommand command = adHocCommandManagerForAdmin.getRemoteCommand(jid, commandNode);
        return command.execute().getResponse();
    }

    AdHocCommandData executeCommandWithArgs(String commandNode, Jid jid, String... args) throws Exception {
        AdHocCommand command = adHocCommandManagerForAdmin.getRemoteCommand(jid, commandNode);
        AdHocCommandResult.StatusExecuting result = command.execute().asExecutingOrThrow();
        FillableForm form = result.getFillableForm();
        fillForm(form, args);

        SubmitForm submitForm = form.getSubmitForm();

        return command.
            complete(submitForm).getResponse();
    }

    AdHocCommandData executeMultistageCommandWithArgs(String commandNode, Jid jid, String[] args1, String[] args2) throws Exception {
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

    void assertFormFieldEquals(String fieldName, Jid expectedValue, AdHocCommandData data) throws XmppStringprepException {
        FormField field = data.getForm().getField(fieldName);
        assertEquals(expectedValue, JidCreate.from(field.getFirstValue()));
    }

    void assertFormFieldEquals(String fieldName, String expectedValue, AdHocCommandData data) {
        FormField field = data.getForm().getField(fieldName);
        assertEquals(expectedValue, field.getFirstValue());
    }

    void assertFormFieldEquals(String fieldName, int expectedValue, AdHocCommandData data) {
        FormField field = data.getForm().getField(fieldName);
        assertEquals(expectedValue, Integer.parseInt(field.getFirstValue()));
    }

    void assertFormFieldContainsAll(String fieldName, Collection<Jid> expectedValues, AdHocCommandData data) {
        FormField field = data.getForm().getField(fieldName);
        List<String> fieldValues = field.getValues().stream().map(CharSequence::toString).collect(Collectors.toList());
        Set<Jid> reportedValues = fieldValues.stream().map(JidCreate::fromOrThrowUnchecked).collect(Collectors.toSet());
        assertTrue(reportedValues.containsAll(expectedValues));
    }

    void assertFormFieldJidEquals(String fieldName, Set<Jid> expectedValues, AdHocCommandData data) {
        FormField field = data.getForm().getField(fieldName);
        List<String> fieldValues = field.getValues().stream().map(CharSequence::toString).collect(Collectors.toList());
        assertEquals(expectedValues, fieldValues.stream().map(JidCreate::fromOrThrowUnchecked).collect(Collectors.toSet()));
    }

    void assertFormFieldEquals(String fieldName, Collection<String> expectedValues, AdHocCommandData data) {
        FormField field = data.getForm().getField(fieldName);
        List<String> fieldValues = field.getValues().stream().map(CharSequence::toString).collect(Collectors.toList());
        assertEquals(expectedValues, fieldValues);
    }

    void assertFormFieldExists(String fieldName, AdHocCommandData data) {
        FormField field = data.getForm().getField(fieldName);
        assertNotNull(field);
    }

    void assertFormFieldHasValues(String fieldName, int valueCount, AdHocCommandData data) {
        FormField field = data.getForm().getField(fieldName);
        assertEquals(valueCount, field.getValues().size());
    }

    void assertFormFieldCountEquals(int expectedCount, AdHocCommandData data) {
        assertEquals(expectedCount, data.getForm().getFields().size());
    }

    void assertFormFieldCountAtLeast(int expectedCount, AdHocCommandData data) {
        assertTrue(data.getForm().getFields().size() >= expectedCount);
    }

    void assertNoteType(AdHocCommandNote.Type expectedType, AdHocCommandData data) {
        AdHocCommandNote note = data.getNotes().get(0);
        assertEquals(expectedType, note.getType());
    }

    void assertNoteContains(String expectedValue, AdHocCommandData data) {
        AdHocCommandNote note = data.getNotes().get(0);
        assertTrue(note.getValue().contains(expectedValue));
    }

    boolean serverSupportsCommand(String commandNode) throws Exception {
        DiscoverItems result = adHocCommandManagerForAdmin.discoverCommands(adminConnection.getUser().asEntityBareJid());
        return result.getItems().stream().anyMatch(item -> item.getNode().equals(commandNode));
    }

    void checkServerSupportCommand(String commandNode) throws Exception {
        if(!serverSupportsCommand(commandNode)){
            throw new TestNotPossibleException("Server does not support command " + commandNode);
        }
    }

    void assertCommandCompletedSuccessfully(final AdHocCommandData result, final String message) throws SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        assertEquals(IQ.Type.result, result.getType(), message + " Unexpected value of the 'type' attribute of the IQ response.");
        assertEquals(AdHocCommandData.Status.completed, result.getStatus(), message + " Unexpected value of the 'status' attribute of the 'command' child element of the IQ response.");
        assertTrue(result.getNotes().stream().noneMatch(note -> note.getType().equals(AdHocCommandNote.Type.error)), message + " Unexpected 'error' note in the 'command' child element of the IQ response.");
    }

    void assertCommandFailed(final AdHocCommandData result, final String message)
    {
        // There is no unique representation of a failed command, but either there's an IQ level error, or there's an error-note in the command status.
        final boolean isError = result.getType().equals(AdHocCommandData.Type.error);
        final boolean hasErrorNote = result.getNotes().stream().anyMatch(note -> note.getType().equals(AdHocCommandNote.Type.error));
        assertTrue(isError || hasErrorNote, message + " Expected the value of the 'type' attribute of the IQ response to be 'error' and/or the 'command' child element of the IQ response to contain an 'error' note (but neither was true).");
    }
}
