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

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Tests for the XEP-0433: Extended Channel Search, with focus on the 'all' search option.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 * @see <a href="https://xmpp.org/extensions/xep-0433.html">XEP-0433: Extended Channel Search</a>
 */
@SpecificationReference(document = "XEP-0433", version = "0.1.0")
public class ExtendedChannelSearchSortIntegrationTest extends AbstractSmackIntegrationTest
{
    protected static final int ROOMS_AMOUNT = 15;
    protected static final String ROOM_NAME_PREFIX = "smack-inttest-xep0433-sort";

    final DomainBareJid searchService;

    public ExtendedChannelSearchSortIntegrationTest(SmackIntegrationTestEnvironment environment) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
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
        final MultiUserChatManager mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        final MultiUserChatManager mucManagerTwo = MultiUserChatManager.getInstanceFor(conTwo);
        final MultiUserChatManager mucManagerThree = MultiUserChatManager.getInstanceFor(conThree);
        final DomainBareJid mucDomain = mucManagerOne.getMucServiceDomains().stream().findFirst().orElseThrow(() -> new TestNotPossibleException("Unable to find a MUC service domain"));

        try {
            for (int i = 11; i <= ROOMS_AMOUNT + 10; i++) {
                String roomNameLocal = String.join("-", ROOM_NAME_PREFIX, testRunId, Integer.toString(i));
                EntityBareJid mucAddress = JidCreate.entityBareFrom(Localpart.from(roomNameLocal), mucDomain);
                mucManagerOne.getMultiUserChat(mucAddress).create(Resourcepart.from("test-user-one")).getConfigFormManager().setRoomName("Test Room " + i).submitConfigurationForm();
                if (i % 2 == 0) {
                    mucManagerTwo.getMultiUserChat(mucAddress).join(Resourcepart.from("test-user-two"));
                }
                if (i % 3 == 0) {
                    mucManagerThree.getMultiUserChat(mucAddress).join(Resourcepart.from("test-user-three"));
                }
            }
        } catch (Exception e) {
            throw new TestNotPossibleException("Unable to create or populate MUC room.", e);
        }
    }

    @AfterClass
    public void tearDown() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XmppStringprepException
    {
        // Destroy the rooms that were used as search results.
        final MultiUserChatManager mucManager = MultiUserChatManager.getInstanceFor(conOne);
        final DomainBareJid mucDomain = mucManager.getMucServiceDomains().stream().findFirst().orElseThrow(() -> new IllegalStateException("Unable to find a MUC service domain"));
        for (int i = 1; i <= ROOMS_AMOUNT; i++) {
            String roomNameLocal = String.join("-", ROOM_NAME_PREFIX, testRunId, Integer.toString(i));
            EntityBareJid mucAddress = JidCreate.entityBareFrom(Localpart.from(roomNameLocal), mucDomain);
            mucManager.getMultiUserChat(mucAddress).destroy();
        }
    }

    @SmackIntegrationTest(section = "6.1", quote = "Order the results by the address of the channel.")
    public void testRequestSearchSortByAddress() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
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

        final FormField qField = form.getField("q");
        final FormField allField = form.getField("all");
        if (qField == null && allField == null) {
            throw new TestNotPossibleException("The service does not support the 'all' nor 'q' search form field.");
        }

        final FillableForm fillableForm = form.getFillableForm();
        for (final FormField field : fillableForm.getDataForm().getFields()) {
            if (allField != null) {
                if (field.getFieldName().equalsIgnoreCase("all")) {
                    fillableForm.setAnswer(field.getFieldName(), true);
                } else if (field.isRequired()) {
                    throw new TestNotPossibleException("Server requires form field that this test implementation does not support: " + field.getFieldName());
                }
            } else {
                if (field.getFieldName().equalsIgnoreCase("q")) {
                    fillableForm.setAnswer(field.getFieldName(), "test");
                } else if (List.of("sinname", "sinaddress", "sindescription").contains(field.getFieldName().toLowerCase()) && field.getType().equals(FormField.Type.bool)) {
                    fillableForm.setAnswer(field.getFieldName(), true);
                } else if (field.isRequired()) {
                    throw new TestNotPossibleException("Server requires form field that this test implementation does not support: " + field.getFieldName());
                }
            }
        }

        // Set the 'sort key' field (which is a required field) to a value that doesn't exist.
        fillableForm.setAnswer("key", "{urn:xmpp:channel-search:0:order}address");

        searchRequest.addExtension(fillableForm.getDataFormToSubmit());

        try {
            // Execute system under test.
            final ExtendedChannelResult result = conOne.sendIqRequestAndWaitForResponse(searchRequest);

            // Verify result.
            if (result.getItems().size() < 3) {
                throw new TestNotPossibleException("The service did not return enough results for the implementation to be able to assert that they're consistently ordered.");
            }
            final List<String> addresses = result.getItems().stream().map(item -> item.address).collect(Collectors.toList());
            assertTrue(isSorted(addresses, true) || isSorted(addresses, false), "Expected the items in the response to the search query to be ordered by address (but they were not).");
        } catch (XMPPException.XMPPErrorException e) {
            final StanzaError stanzaError = e.getStanzaError();
            if (stanzaError.getCondition().equals(StanzaError.Condition.feature_not_implemented) && stanzaError.getExtension("invalid-sort-key", "urn:xmpp:channel-search:0:error") != null) {
                throw new TestNotPossibleException("The service does not support the '{urn:xmpp:channel-search:0:order}address' sort key.", e);
            }
            if (stanzaError.getCondition().equals(StanzaError.Condition.resource_constraint)) {
                throw new TestNotPossibleException("Unable to execute search, as the service is rejecting the request due to rate limiting", e);
            }
            throw e;
        }
    }

    @SmackIntegrationTest(section = "6.1", quote = "Order the results descendingly by the number of users.")
    public void testRequestSearchSortByNumberOfUsers() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
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

        final FormField qField = form.getField("q");
        final FormField allField = form.getField("all");
        if (qField == null && allField == null) {
            throw new TestNotPossibleException("The service does not support the 'all' nor 'q' search form field.");
        }

        final FillableForm fillableForm = form.getFillableForm();
        for (final FormField field : fillableForm.getDataForm().getFields()) {
            if (allField != null) {
                if (field.getFieldName().equalsIgnoreCase("all")) {
                    fillableForm.setAnswer(field.getFieldName(), true);
                } else if (field.isRequired()) {
                    throw new TestNotPossibleException("Server requires form field that this test implementation does not support: " + field.getFieldName());
                }
            } else {
                if (field.getFieldName().equalsIgnoreCase("q")) {
                    fillableForm.setAnswer(field.getFieldName(), "test");
                } else if (List.of("sinname", "sinaddress", "sindescription").contains(field.getFieldName().toLowerCase()) && field.getType().equals(FormField.Type.bool)) {
                    fillableForm.setAnswer(field.getFieldName(), true);
                } else if (field.isRequired()) {
                    throw new TestNotPossibleException("Server requires form field that this test implementation does not support: " + field.getFieldName());
                }
            }
        }

        // Set the 'sort key' field (which is a required field) to a value that doesn't exist.
        fillableForm.setAnswer("key", "{urn:xmpp:channel-search:0:order}nusers");

        searchRequest.addExtension(fillableForm.getDataFormToSubmit());

        try {
            // Execute system under test.
            final ExtendedChannelResult result = conOne.sendIqRequestAndWaitForResponse(searchRequest);

            // Verify result.
            if (result.getItems().size() < 3) {
                throw new TestNotPossibleException("The service did not return enough results for the implementation to be able to assert that they're consistently ordered.");
            }
            final List<Integer> users = result.getItems().stream().map(item -> item.nusers).collect(Collectors.toList());
            assertTrue(isSorted(users, true), "Expected the items in the response to the search query to be ordered descendingly by number of users (but they were not).");
        } catch (XMPPException.XMPPErrorException e) {
            final StanzaError stanzaError = e.getStanzaError();
            if (stanzaError.getCondition().equals(StanzaError.Condition.feature_not_implemented) && stanzaError.getExtension("invalid-sort-key", "urn:xmpp:channel-search:0:error") != null) {
                throw new TestNotPossibleException("The service does not support the '{urn:xmpp:channel-search:0:order}nusers' sort key.", e);
            }
            if (stanzaError.getCondition().equals(StanzaError.Condition.resource_constraint)) {
                throw new TestNotPossibleException("Unable to execute search, as the service is rejecting the request due to rate limiting", e);
            }
            throw e;
        }
    }

    public static <T extends Comparable<T>> boolean isSorted(List<T> list, boolean isDescending) {
        if (list.size() <= 1) {
            return true;
        }

        Iterator<T> iter = list.iterator();
        T current, previous = iter.next();
        while (iter.hasNext()) {
            current = iter.next();
            if (isDescending) {
                if (previous.compareTo(current) < 0) {
                    return false;
                }
            } else {
                if (previous.compareTo(current) > 0) {
                    return false;
                }
            }
            previous = current;
        }
        return true;
    }
}
