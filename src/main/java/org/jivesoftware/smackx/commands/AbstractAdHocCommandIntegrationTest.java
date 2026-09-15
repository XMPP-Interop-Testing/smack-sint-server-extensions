package org.jivesoftware.smackx.commands;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
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

    public static final List<FormField.Type> NON_STRING_FORM_FIELD_TYPES = Arrays.asList(
        FormField.Type.jid_multi,
        FormField.Type.list_multi
    );

    void fillForm(FillableForm form, String[] args){
        for (int i = 0; i < args.length; i += 2) {
            FormField field = form.getField(args[i]);
            if (field == null) {
                throw new IllegalStateException("Field " + args[i] + " not found in form");
            }
            if (NON_STRING_FORM_FIELD_TYPES.contains(field.getType())){
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

}
