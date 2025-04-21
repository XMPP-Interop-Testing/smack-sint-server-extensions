/*
 * Copyright 2024-2025 Guus der Kinderen. All rights reserved.
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
import org.jivesoftware.smackx.rsm.packet.RSMSet;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Tests for the XEP-0433: Extended Channel Search
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 * @see <a href="https://xmpp.org/extensions/xep-0433.html">XEP-0433: Extended Channel Search</a>
 */
@SpecificationReference(document = "XEP-0433", version = "0.1.0")
public class ExtendedChannelSearchIntegrationTest extends AbstractSmackIntegrationTest
{
    protected static final int ROOMS_AMOUNT = 15;
    protected static final String ROOM_NAME_PREFIX = "smack-inttest-xep0433-q";

    final DomainBareJid searchService;

    public ExtendedChannelSearchIntegrationTest(SmackIntegrationTestEnvironment environment) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        super(environment);

        searchService = ServiceDiscoveryManager.getInstanceFor(connection).findService("urn:xmpp:channel-search:0:search", false);
        if (searchService == null) {
            throw new TestNotPossibleException("Unable to find any service on domain that supports XEP-0433: Extended Channel Search.");
        }

        ProviderManager.addIQProvider(ExtendedChannelSearchForm.ELEMENT, ExtendedChannelSearchForm.NAMESPACE, new ExtendedChannelSearchForm.Provider());
        ProviderManager.addIQProvider(ExtendedChannelResult.ELEMENT, ExtendedChannelResult.NAMESPACE, new ExtendedChannelResult.Provider());
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

    @SmackIntegrationTest(section = "4.2", quote = "the Searcher MAY [...] request the search form from the Search Service.")
    public void testRequestSearchForm() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final ExtendedChannelSearchForm request = new ExtendedChannelSearchForm();
        request.setType(IQ.Type.get);
        request.setTo(searchService);

        // Execute system under test.
        final IQ response = conOne.sendIqRequestAndWaitForResponse(request);
        final DataForm form = DataForm.from(response);

        // Verify result.
        assertNotNull(form, "Expected the response from search service '" + searchService + "' to a request from '" + conOne.getUser() + "' for a extended channel search form to contain a data form (but it did not).");
        assertEquals("urn:xmpp:channel-search:0:search-params", form.getFormType(), "Unexpected data form type in the search form received by '" + conOne.getUser() + "' from '" + searchService + "'." );
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

        final FillableForm fillableForm = form.getFillableForm();
        for (final FormField field : fillableForm.getDataForm().getFields()) {
            if (field.getFieldName().equalsIgnoreCase("q")) {
                fillableForm.setAnswer(field.getFieldName(), "test");
            } else if (List.of("sinname", "sinaddress", "sindescription").contains(field.getFieldName().toLowerCase()) && field.getType().equals(FormField.Type.bool)) {
                fillableForm.setAnswer(field.getFieldName(), true);
            } else if (field.isRequired()) {
                throw new TestNotPossibleException("Server requires form field that this test implementation does not support: " + field.getFieldName());
            }
        }

        searchRequest.addExtension(fillableForm.getDataFormToSubmit());

        // Execute system under test.
        final IQ searchResponse;
        try {
            searchResponse = conOne.sendIqRequestAndWaitForResponse(searchRequest);
        } catch (XMPPException.XMPPErrorException e) {
            if (e.getStanzaError().getCondition().equals(StanzaError.Condition.resource_constraint)) {
                throw new TestNotPossibleException("Unable to execute search, as the service is rejecting the request due to rate limiting", e);
            }
            throw e;
        }

        // Verify result.
        assertNotNull(searchResponse, "The search request issued by '" + conOne.getUser() + "' did not result in a response from '" + searchService + "'." );
    }

    @SmackIntegrationTest(section = "4.2.2", quote = "The Searcher MAY include a Result Set Management (XEP-0059) <set/> element inside the <search/> element.")
    public void testSubmitSearchFormRSM() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
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

        final FillableForm fillableForm = form.getFillableForm();
        for (final FormField field : fillableForm.getDataForm().getFields()) {
            if (field.getFieldName().equalsIgnoreCase("q")) {
                fillableForm.setAnswer(field.getFieldName(), "test");
            } else if (List.of("sinname", "sinaddress", "sindescription").contains(field.getFieldName().toLowerCase()) && field.getType().equals(FormField.Type.bool)) {
                fillableForm.setAnswer(field.getFieldName(), true);
            } else if (field.isRequired()) {
                throw new TestNotPossibleException("Server requires form field that this test implementation does not support: " + field.getFieldName());
            }
        }
        searchRequest.addExtension(fillableForm.getDataFormToSubmit());

        searchRequest.addExtension(new RSMSet(1));

        // Execute system under test.
        final IQ searchResponse;
        try {
            searchResponse = conOne.sendIqRequestAndWaitForResponse(searchRequest);
        } catch (XMPPException.XMPPErrorException e) {
            if (e.getStanzaError().getCondition().equals(StanzaError.Condition.resource_constraint)) {
                throw new TestNotPossibleException("Unable to execute search, as the service is rejecting the request due to rate limiting", e);
            }
            throw e;
        }

        // Verify result.
        assertNotNull(searchResponse, "The search request issued by '" + conOne.getUser() + "' did not result in a response from '" + searchService + "'." );
        // We cannot assert this, as the RMS XEP explicitly states that no RMS element is present when the total result set contains 0 items). assertTrue(searchResponse.hasExtension(RSMSet.class), "Expected the response from '" + searchService + "' to the search request issued by '" + conOne.getUser() + "' to contain a Result Set Management extension (but it did not).");
    }

    @SmackIntegrationTest(section = "4.2.2.2", quote = "If the sort key requested by the Searcher is not supported by the Search Service, the Search Service MUST reply with <feature-not-implemented/> and the <invalid-sort-key> application defined condition and a modify type")
    public void testUnsupportedKey() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
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

        final FillableForm fillableForm = form.getFillableForm();
        for (final FormField field : fillableForm.getDataForm().getFields()) {
            if (field.getFieldName().equalsIgnoreCase("q")) {
                fillableForm.setAnswer(field.getFieldName(), "test");
            } else if (List.of("sinname", "sinaddress", "sindescription").contains(field.getFieldName().toLowerCase()) && field.getType().equals(FormField.Type.bool)) {
                fillableForm.setAnswer(field.getFieldName(), true);
            } else if (field.isRequired()) {
                throw new TestNotPossibleException("Server requires form field that this test implementation does not support: " + field.getFieldName());
            }
        }

        // Set the 'sort key' field (which is a required field to implement) to a value that doesn't exist.
        fillableForm.setAnswer("key", "this-is-not-an-existing-key");

        searchRequest.addExtension(fillableForm.getDataFormToSubmit());

        // Execute system under test &Verify result.
        final XMPPException.XMPPErrorException xmppErrorException = assertThrows(XMPPException.XMPPErrorException.class, () -> conOne.sendIqRequestAndWaitForResponse(searchRequest),
            "Expected an error after '" + conOne.getUser() + "' issues a search query using a sort key that is not supported by the search service (but none occurred).");

        if (xmppErrorException.getStanzaError().getCondition().equals(StanzaError.Condition.resource_constraint)) {
            throw new TestNotPossibleException("Unable to execute search, as the service is rejecting the request due to rate limiting", xmppErrorException);
        }
        assertEquals(StanzaError.Condition.feature_not_implemented, xmppErrorException.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conOne.getUser() + "' after it issued a search query using a sort key that is not supported by the search service.");
        assertEquals(StanzaError.Type.MODIFY, xmppErrorException.getStanzaError().getType(), "Unexpected error type in the (expected) error that was returned to '" + conOne.getUser() + "' after it issued a search query using a sort key that is not supported by the search service.");
        assertNotNull(xmppErrorException.getStanzaError().getExtension("invalid-sort-key", "urn:xmpp:channel-search:0:error"), "Expected the error that was return to '" + conOne.getUser() + "' after it issued a search query using a sort key that is not supported by the search service to contain the the <invalid-search-terms/> application defined condition (but it did not).");
    }

    @SmackIntegrationTest(section = "4.2.2.7", quote = "If no field which would define a result set and which is understood by the Search Service is present, it MUST reply with a <bad-request/> error of type cancel.")
    public void testSubmitNoFields() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
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

        // Attempt to set the none of the fields
        final FillableForm fillableForm = form.getFillableForm();
        for (final FormField field : fillableForm.getDataForm().getFields()) {
            if (field.isRequired()) {
                throw new TestNotPossibleException("Server requires form field that this test implementation does not support: " + field.getFieldName());
            }
            if (field.hasValueSet() && !List.of("FORM_TYPE", "types", "key").contains(field.getFieldName())) {
                throw new TestNotPossibleException("Server pre-sets a form field, that makes it impossible to submit a request that has no form fields (as unsetting it implies using the server's default value): " + field.getFieldName());
            }
        }

        searchRequest.addExtension(fillableForm.getDataFormToSubmit());

        try {
            // Execute system under test.
            conOne.sendIqRequestAndWaitForResponse(searchRequest);

            // Verify result.
            fail( "The search request issued by '" + conOne.getUser() + "' that did not contain any fields did not result in an error response from '" + searchService + "'." );
        } catch (XMPPException.XMPPErrorException e) {
            final StanzaError stanzaError = e.getStanzaError();
            if (stanzaError.getCondition().equals(StanzaError.Condition.resource_constraint)) {
                throw new TestNotPossibleException("Unable to execute search, as the service is rejecting the request due to rate limiting", e);
            }
            assertEquals(StanzaError.Condition.bad_request, stanzaError.getCondition(), "Unexpected 'condition' in the (expected) error that was returned in response to the search request issued by '" + conOne.getUser() + "' that did not contain any fields'");
            assertEquals(StanzaError.Type.CANCEL, stanzaError.getType(), "Unexpected 'type' in the (expected) error that was returned in response to the search request issued by '" + conOne.getUser() + "' that did not contain any fields.");
            assertNotNull(stanzaError.getExtension("no-search-conditions", "urn:xmpp:channel-search:0:error"), "Missing application specific condition in the (expected) error that was returned in response to the search request issued by '" + conOne.getUser() + "' that did not contain any fields.");
        }
    }

    @SmackIntegrationTest(section = "5", quote = "When receiving a search form, the Search Service MUST ignore fields with a var value it does not understand.")
    public void testSubmitSearchFormWithUnknownVar() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
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

        final FillableForm fillableForm = form.getFillableForm();
        for (final FormField field : fillableForm.getDataForm().getFields()) {
            if (field.getFieldName().equalsIgnoreCase("q")) {
                fillableForm.setAnswer(field.getFieldName(), "test");
            } else if (List.of("sinname", "sinaddress", "sindescription").contains(field.getFieldName().toLowerCase()) && field.getType().equals(FormField.Type.bool)) {
                fillableForm.setAnswer(field.getFieldName(), true);
            } else if (field.isRequired()) {
                throw new TestNotPossibleException("Server requires form field that this test implementation does not support: " + field.getFieldName());
            }
        }
        searchRequest.addExtension(fillableForm.getDataFormToSubmit());

        fillableForm.getDataFormToSubmit().asBuilder().addField(FormField.textSingleBuilder("donotrecognizethisfield").setValue("unknown").build());
        searchRequest.addExtension(fillableForm.getDataFormToSubmit());

        // Execute system under test.
        final IQ searchResponse;
        try {
            searchResponse = conOne.sendIqRequestAndWaitForResponse(searchRequest);
        } catch (XMPPException.XMPPErrorException e) {
            if (e.getStanzaError().getCondition().equals(StanzaError.Condition.resource_constraint)) {
                throw new TestNotPossibleException("Unable to execute search, as the service is rejecting the request due to rate limiting", e);
            }

            // Verify result.
            fail("Expected the search service '" + searchService + "' to ignore the unknown form field in the request issued by '" + conOne.getUser() + "', but the service returned an error: " + e.getStanzaError());
            return;
        }

        assertNotNull(searchResponse, "The search request issued by '" + conOne.getUser() + "' (that contained an unknown form field) did not result in a response from '" + searchService + "'." );
    }

    @SmackIntegrationTest(section = "5", quote = "If a search request does not yield any results, the Search Service MUST reply with a <result/> without any <item/> children in a type='result' IQ. Specifically, it MUST NOT reply with an <item-not-found/> error.")
    public void testSubmitSearchWithNoResults() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        // Setup test fixture.
        final ExtendedChannelSearchForm formRequest = new ExtendedChannelSearchForm();
        formRequest.setType(IQ.Type.get);
        formRequest.setTo(searchService);
        final IQ response = conOne.sendIqRequestAndWaitForResponse(formRequest);
        final Form form = new Form(DataForm.from(response));

        final boolean searchHadQ = form.getField("q") != null;

        final ExtendedChannelSearchForm searchRequest = new ExtendedChannelSearchForm();
        searchRequest.setType(IQ.Type.get);
        searchRequest.setTo(searchService);

        final FillableForm fillableForm = form.getFillableForm();
        for (final FormField field : fillableForm.getDataForm().getFields()) {
            if (field.getFieldName().equalsIgnoreCase("q")) {
                fillableForm.setAnswer(field.getFieldName(), "veryunlikelythaththismatchesanything andthusshouldnotgenerateresults");
            } else if (List.of("sinname", "sinaddress", "sindescription").contains(field.getFieldName().toLowerCase()) && field.getType().equals(FormField.Type.bool)) {
                fillableForm.setAnswer(field.getFieldName(), false);
            } else if (field.isRequired()) {
                throw new TestNotPossibleException("Server requires form field that this test implementation does not support: " + field.getFieldName());
            }
        }

        searchRequest.addExtension(fillableForm.getDataFormToSubmit());

        // Execute system under test.
        final ExtendedChannelResult searchResponse;
        try {
            searchResponse = conOne.sendIqRequestAndWaitForResponse(searchRequest);
        } catch (XMPPException.XMPPErrorException e) {
            if (e.getStanzaError().getCondition().equals(StanzaError.Condition.resource_constraint)) {
                throw new TestNotPossibleException("Unable to execute search, as the service is rejecting the request due to rate limiting", e);
            }

            // Verify result.
            if (e.getStanzaError().getCondition().equals(StanzaError.Condition.item_not_found)) {
                fail("Expected the search service '" + searchService + "' to return an empty result to '" + conOne.getUser() + "' when the search does not yield results, but the service returned an 'item-not-found' error.");
            }
            return;
        }

        assertNotNull(searchResponse, "The search request issued by '" + conOne.getUser() + "' did not result in a response from '" + searchService + "'.");
        if (!searchResponse.getItems().isEmpty()) {
            if (!searchHadQ) {
                throw new TestNotPossibleException("A non-empty response was returned to a search request that was not intended to return any items in the response.");
            } else {
                fail("The search request issued by '" + conOne.getUser() + "' that contains a very specific search query unexpectedly resulted in a response that contains a non-zero amount of items from '" + searchService + "'.");
            }
        }
    }

    @SmackIntegrationTest(section = "6.1", quote = "The following fields are specified: [var] q [type] text-single [support level] RECOMMENDED")
    public void testRequestSearchFormVarQ() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        // Setup test fixture.
        final ExtendedChannelSearchForm request = new ExtendedChannelSearchForm();
        request.setType(IQ.Type.get);
        request.setTo(searchService);

        // Execute system under test.
        final IQ response = conOne.sendIqRequestAndWaitForResponse(request);
        final DataForm form = DataForm.from(response);
        final FormField field = form.getField("q");
        if (field == null) {
            throw new TestNotPossibleException("The service does not support the 'q' search form field.");
        }

        // Verify result.
        assertEquals(FormField.Type.text_single, field.getType(), "Unexpected type of the field 'q' in the search form received from search service '" + searchService + "' by '" + conOne.getUser() + "')");
    }

    @SmackIntegrationTest(section = "6.1", quote = "The following fields are specified: [var] sinaddress [type] boolean [support level] RECOMMENDED if q is supported")
    public void testRequestSearchFormVarSinaddress() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        // Setup test fixture.
        final ExtendedChannelSearchForm request = new ExtendedChannelSearchForm();
        request.setType(IQ.Type.get);
        request.setTo(searchService);

        // Execute system under test.
        final IQ response = conOne.sendIqRequestAndWaitForResponse(request);
        final DataForm form = DataForm.from(response);
        final FormField field = form.getField("sinaddress");
        if (field == null) {
            throw new TestNotPossibleException("The service does not support the 'sinaddress' search form field.");
        }

        // Verify result.
        assertEquals(FormField.Type.bool, field.getType(), "Unexpected type of the field 'sinaddress' in the search form received from search service '" + searchService + "' by '" + conOne.getUser() + "')");
    }

    @SmackIntegrationTest(section = "6.1", quote = "The following fields are specified: [var] sinname [type] boolean [support level] REQUIRED if q is supported")
    public void testRequestSearchFormVarSinname() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        // Setup test fixture.
        final ExtendedChannelSearchForm request = new ExtendedChannelSearchForm();
        request.setType(IQ.Type.get);
        request.setTo(searchService);

        // Execute system under test.
        final IQ response = conOne.sendIqRequestAndWaitForResponse(request);
        final DataForm form = DataForm.from(response);
        final FormField q = form.getField("q");
        if (q == null) {
            throw new TestNotPossibleException("The service does not support the 'q' search form field, and thus does not have to support the 'sinname' field.");
        }
        final FormField field = form.getField("sinname");

        // Verify result.
        assertNotNull(field, "Expected the search form received from search service '" + searchService + "' by '" + conOne.getUser() + "' to include the mandatory field 'sinname', as it also supports the 'q' field (but it did not).");
        assertEquals(FormField.Type.bool, field.getType(), "Unexpected type of the field 'sinname' in the search form received from search service '" + searchService + "' by '" + conOne.getUser() + "')");
    }

    @SmackIntegrationTest(section = "6.1", quote = "The following fields are specified: [var] sindescription [type] boolean [support level] REQUIRED if q is supported")
    public void testRequestSearchFormVarSindescription() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        // Setup test fixture.
        final ExtendedChannelSearchForm request = new ExtendedChannelSearchForm();
        request.setType(IQ.Type.get);
        request.setTo(searchService);

        // Execute system under test.
        final IQ response = conOne.sendIqRequestAndWaitForResponse(request);
        final DataForm form = DataForm.from(response);
        final FormField q = form.getField("q");
        if (q == null) {
            throw new TestNotPossibleException("The service does not support the 'q' search form field, and thus does not have to support the 'sindescription' field.");
        }
        final FormField field = form.getField("sindescription");

        // Verify result.
        assertNotNull(field, "Expected the search form received from search service '" + searchService + "' by '" + conOne.getUser() + "' to include the mandatory field 'sindescription', as it also supports the 'q' field (but it did not).");
        assertEquals(FormField.Type.bool, field.getType(), "Unexpected type of the field 'sindescription' in the search form received from search service '" + searchService + "' by '" + conOne.getUser() + "')");
    }

    @SmackIntegrationTest(section = "6.1", quote = "The following fields are specified: [var] types [type] list-multi [support level] RECOMMENDED")
    public void testRequestSearchFormVarTypes() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        // Setup test fixture.
        final ExtendedChannelSearchForm request = new ExtendedChannelSearchForm();
        request.setType(IQ.Type.get);
        request.setTo(searchService);

        // Execute system under test.
        final IQ response = conOne.sendIqRequestAndWaitForResponse(request);
        final DataForm form = DataForm.from(response);
        final FormField field = form.getField("types");
        if (field == null) {
            throw new TestNotPossibleException("The service does not allow the 'types' search form field to be defined (implying that it only supports MUC search).");
        }

        // Verify result.
        assertEquals(FormField.Type.list_multi, field.getType(), "Unexpected type of the field 'types' in the search form received from search service '" + searchService + "' by '" + conOne.getUser() + "')");
    }

    @SmackIntegrationTest(section = "6.1", quote = "The following fields are specified: [var] key [type] list-single [support level] REQUIRED")
    public void testRequestSearchFormVarKey() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final ExtendedChannelSearchForm request = new ExtendedChannelSearchForm();
        request.setType(IQ.Type.get);
        request.setTo(searchService);

        // Execute system under test.
        final IQ response = conOne.sendIqRequestAndWaitForResponse(request);
        final DataForm form = DataForm.from(response);
        final FormField field = form.getField("key");

        // Verify result.
        assertNotNull(field, "Expected the search form received from search service '" + searchService + "' by '" + conOne.getUser() + "' to include the mandatory field 'key' (but it did not).");
        assertEquals(FormField.Type.list_single, field.getType(), "Unexpected type of the field 'key' in the search form received from search service '" + searchService + "' by '" + conOne.getUser() + "')");
    }
}
