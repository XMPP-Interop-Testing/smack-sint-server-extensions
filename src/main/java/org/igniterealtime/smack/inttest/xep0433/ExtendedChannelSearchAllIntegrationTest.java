/*
 * Copyright 2024-2025 Guus der Kinderen
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
package org.igniterealtime.smack.inttest.xep0433;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.AfterClass;
import org.igniterealtime.smack.inttest.annotations.BeforeClass;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.form.FillableForm;
import org.jivesoftware.smackx.xdata.form.Form;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Tests for the XEP-0433: Extended Channel Search, with focus on the 'all' search option.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 * @see <a href="https://xmpp.org/extensions/xep-0433.html">XEP-0433: Extended Channel Search</a>
 */
@SpecificationReference(document = "XEP-0433", version = "0.1.0")
public class ExtendedChannelSearchAllIntegrationTest extends AbstractSmackIntegrationTest
{
    protected static final int ROOMS_AMOUNT = 15;
    protected static final String ROOM_NAME_PREFIX = "smack-inttest-xep0433-all";

    final DomainBareJid searchService;

    public ExtendedChannelSearchAllIntegrationTest(SmackIntegrationTestEnvironment environment) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        super(environment);

        searchService = ServiceDiscoveryManager.getInstanceFor(connection).findService("urn:xmpp:channel-search:0:search", false);
        if (searchService == null) {
            throw new TestNotPossibleException("Unable to find any service on domain that supports XEP-0433: Extended Channel Search.");
        }

        ProviderManager.addIQProvider(ExtendedChannelSearchForm.ELEMENT, ExtendedChannelSearchForm.NAMESPACE, new ExtendedChannelSearchForm.Provider());
        ProviderManager.addIQProvider(ExtendedChannelResult.ELEMENT, ExtendedChannelResult.NAMESPACE, new ExtendedChannelResult.Provider());

        final ExtendedChannelSearchForm request = new ExtendedChannelSearchForm();
        request.setType(IQ.Type.get);
        request.setTo(searchService);
        final IQ response = conOne.sendIqRequestAndWaitForResponse(request);
        final DataForm form = DataForm.from(response);
        final FormField field = form.getField("all");
        if (field == null) {
            throw new TestNotPossibleException("The service does not support the 'all' search form field.");
        }
    }

    @BeforeClass
    public void setUp() throws TestNotPossibleException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Create a number of rooms that will act as search results.
        final MultiUserChatManager mucManager = MultiUserChatManager.getInstanceFor(connection);
        final DomainBareJid mucDomain = mucManager.getMucServiceDomains().stream().findFirst().orElseThrow(() -> new TestNotPossibleException("Unable to find a MUC service domain"));

        try {
            for (int i = 1; i <= ROOMS_AMOUNT; i++) {
                String roomNameLocal = String.join("-", ROOM_NAME_PREFIX, testRunId, Integer.toString(i));
                EntityBareJid mucAddress = JidCreate.entityBareFrom(Localpart.from(roomNameLocal), mucDomain);
                mucManager.getMultiUserChat(mucAddress).create(Resourcepart.from("test-user")).getConfigFormManager().setRoomName("Test Room " + i).submitConfigurationForm();
            }
        } catch (Exception e) {
            throw new TestNotPossibleException("Unable to create MUC room.", e);
        }
    }

    @AfterClass
    public void tearDown() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XmppStringprepException
    {
        // Destroy the rooms that were used as search results.
        final MultiUserChatManager mucManager = MultiUserChatManager.getInstanceFor(connection);
        final DomainBareJid mucDomain = mucManager.getMucServiceDomains().stream().findFirst().orElseThrow(() -> new IllegalStateException("Unable to find a MUC service domain"));
        for (int i = 1; i <= ROOMS_AMOUNT; i++) {
            String roomNameLocal = String.join("-", ROOM_NAME_PREFIX, testRunId, Integer.toString(i));
            EntityBareJid mucAddress = JidCreate.entityBareFrom(Localpart.from(roomNameLocal), mucDomain);
            mucManager.getMultiUserChat(mucAddress).destroy();
        }
    }

    @SmackIntegrationTest(section = "6.1", quote = "The following fields are specified: [var] all [type] boolean [support level] OPTIONAL")
    public void testRequestSearchFormVarAll() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        // Setup test fixture.
        final ExtendedChannelSearchForm request = new ExtendedChannelSearchForm();
        request.setType(IQ.Type.get);
        request.setTo(searchService);

        // Execute system under test.
        final IQ response = conOne.sendIqRequestAndWaitForResponse(request);
        final DataForm form = DataForm.from(response);
        final FormField field = form.getField("all");
        if (field == null) {
            throw new TestNotPossibleException("The service does not support the 'all' search form field.");
        }

        // Verify result.
        assertEquals(FormField.Type.bool, field.getType(), "Unexpected type of the field 'all' in the search form received from search service '" + searchService + "' by '" + conOne.getUser() + "')");
    }

    @SmackIntegrationTest(section = "4.2.2.6", quote = "If the Searcher provides form fields which are conflicting, the Search Service MUST reply with a <bad-request/> error of type modify. In addition, the <conflicting-fields/> application specific condition MUST be included.")
    public void testSubmitConflictingQandAll() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        // Setup test fixture.
        final ExtendedChannelSearchForm formRequest = new ExtendedChannelSearchForm();
        formRequest.setType(IQ.Type.get);
        formRequest.setTo(searchService);
        final IQ response = conOne.sendIqRequestAndWaitForResponse(formRequest);
        final Form form = new Form(DataForm.from(response));

        final ExtendedChannelSearchForm searchRequest = new ExtendedChannelSearchForm();
        searchRequest.setType(IQ.Type.get);
        searchRequest.setTo(searchService);

        // Attempt to set the 'all' field
        final FillableForm fillableForm = form.getFillableForm();
        for (final FormField field : fillableForm.getDataForm().getFields()) {
            if (field.getFieldName().equalsIgnoreCase("all")) {
                fillableForm.setAnswer(field.getFieldName(), true);
                continue;
            }
            if (field.getFieldName().equalsIgnoreCase("q")) {
                fillableForm.setAnswer(field.getFieldName(), "test");
                continue;
            }

            if (field.isRequired()) {
                throw new TestNotPossibleException("Server requires form field that this test implementation does not support: " + field.getFieldName());
            }
        }

        searchRequest.addExtension(fillableForm.getDataFormToSubmit());

        try {
            // Execute system under test.
            conOne.sendIqRequestAndWaitForResponse(searchRequest);

            // Verify result.
            fail( "The search request issued by '" + conOne.getUser() + "' that contained conflicting fields 'q' and 'all' did not result in an error response from '" + searchService + "'." );
        } catch (XMPPException.XMPPErrorException e) {
            final StanzaError stanzaError = e.getStanzaError();
            if (stanzaError.getCondition().equals(StanzaError.Condition.resource_constraint)) {
                throw new TestNotPossibleException("Unable to execute search, as the service is rejecting the request due to rate limiting", e);
            }
            if (stanzaError.getExtension("full-set-retrieval-rejected", "urn:xmpp:channel-search:0:error") != null) {
                if (stanzaError.getType().equals(StanzaError.Type.CANCEL) && stanzaError.getCondition().equals(StanzaError.Condition.not_allowed)) {
                    throw new TestNotPossibleException("Unable to execute search, as the service has generally disabled the queries for the full result set.", e);
                }
                if (stanzaError.getType().equals(StanzaError.Type.AUTH) && stanzaError.getCondition().equals(StanzaError.Condition.forbidden)) {
                    throw new TestNotPossibleException("Unable to execute search, as the service has does not allow this test-user to perform queries for the full result set.", e);
                }
            }
            assertEquals(StanzaError.Condition.bad_request, stanzaError.getCondition(), "Unexpected 'condition' in the (expected) error that was returned in response to the search request issued by '" + conOne.getUser() + "' that contained conflicting fields 'q' and 'all'.");
            assertEquals(StanzaError.Type.MODIFY, stanzaError.getType(), "Unexpected 'type' in the (expected) error that was returned in response to the search request issued by '" + conOne.getUser() + "' that contained conflicting fields 'q' and 'all'.");
            assertNotNull(stanzaError.getExtension("conflicting-fields", "urn:xmpp:channel-search:0:error"), "Missing application specific condition in the (expected) error that was returned in response to the search request issued by '" + conOne.getUser() + "' that contained conflicting fields 'q' and 'all'.");
        }
    }

    @SmackIntegrationTest(section = "4.2", quote = "To request the result list for a given search query, a Searcher submits a form with the urn:xmpp:channel-search:0:search-params FORM_TYPE.")
    public void testSubmitSearchForm() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        // Setup test fixture.
        final ExtendedChannelSearchForm formRequest = new ExtendedChannelSearchForm();
        formRequest.setType(IQ.Type.get);
        formRequest.setTo(searchService);
        final IQ response = conOne.sendIqRequestAndWaitForResponse(formRequest);
        final Form form = new Form(DataForm.from(response));

        final ExtendedChannelSearchForm searchRequest = new ExtendedChannelSearchForm();
        searchRequest.setType(IQ.Type.get);
        searchRequest.setTo(searchService);

        // Attempt to set the 'all' field
        final FillableForm fillableForm = form.getFillableForm();
        for (final FormField field : fillableForm.getDataForm().getFields()) {
            if (field.getFieldName().equalsIgnoreCase("all")) {
                fillableForm.setAnswer(field.getFieldName(), true);
                continue;
            }

            if (field.isRequired()) {
                throw new TestNotPossibleException("Server requires form field that this test implementation does not support: " + field.getFieldName());
            }
        }

        searchRequest.addExtension(fillableForm.getDataFormToSubmit());

        // Execute system under test.
        final IQ searchResponse;
        try {
            searchResponse = conOne.sendIqRequestAndWaitForResponse(searchRequest);
        } catch (XMPPException.XMPPErrorException e) {
            final StanzaError stanzaError = e.getStanzaError();
            if (stanzaError.getCondition().equals(StanzaError.Condition.resource_constraint)) {
                throw new TestNotPossibleException("Unable to execute search, as the service is rejecting the request due to rate limiting", e);
            }
            if (stanzaError.getExtension("full-set-retrieval-rejected", "urn:xmpp:channel-search:0:error") != null) {
                if (stanzaError.getType().equals(StanzaError.Type.CANCEL) && stanzaError.getCondition().equals(StanzaError.Condition.not_allowed)) {
                    throw new TestNotPossibleException("Unable to execute search, as the service has generally disabled the queries for the full result set.", e);
                }
                if (stanzaError.getType().equals(StanzaError.Type.AUTH) && stanzaError.getCondition().equals(StanzaError.Condition.forbidden)) {
                    throw new TestNotPossibleException("Unable to execute search, as the service has does not allow this test-user to perform queries for the full result set.", e);
                }
            }
            throw e;
        }

        // Verify result.
        assertNotNull(searchResponse, "The search request issued by '" + conOne.getUser() + "' did not result in a response from '" + searchService + "'." );
    }
}
