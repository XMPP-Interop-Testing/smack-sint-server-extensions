/*
 * Copyright 2024 Guus der Kinderen
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
package org.igniterealtime.smack.inttest.xep0215;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.xep0215.packet.DiscoverExternalServices;
import org.igniterealtime.smack.inttest.xep0215.packet.ServiceCredentials;
import org.igniterealtime.smack.inttest.xep0215.provider.DiscoverExternalServicesProvider;
import org.igniterealtime.smack.inttest.xep0215.provider.ServiceCredentialsProvider;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.stringprep.XmppStringprepException;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpecificationReference(document = "XEP-0215", version = "1.0.0")
public class ExternalServiceDiscoveryIntegrationTest extends AbstractSmackIntegrationTest
{
    public static final String NAMESPACE = "urn:xmpp:extdisco:2";

    private final DomainBareJid service;

    public ExternalServiceDiscoveryIntegrationTest(SmackIntegrationTestEnvironment environment) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        super(environment);

        service = ServiceDiscoveryManager.getInstanceFor(environment.conOne).findService(NAMESPACE, true);
        if (service == null) {
            throw new TestNotPossibleException("Unable to find any service on domain that supports XEP-0215 External Service Discovery.");
        }

        ProviderManager.addIQProvider(DiscoverExternalServices.ELEMENT, DiscoverExternalServices.NAMESPACE, new DiscoverExternalServicesProvider());
        ProviderManager.addIQProvider(ServiceCredentials.ELEMENT, ServiceCredentials.NAMESPACE, new ServiceCredentialsProvider());
    }

    @SmackIntegrationTest(section = "2", quote = "expires - A timestamp indicating when the provided username and password credentials will expire. The format MUST adhere to the dateTime format specified in XMPP Date and Time Profiles (XEP-0082) [...] .")
    public void requestAllExpiryTimestampsValidTest() throws SmackException.NotConnectedException, InterruptedException, XMPPException.XMPPErrorException, SmackException.NoResponseException, TestNotPossibleException
    {
        final DiscoverExternalServices request = new DiscoverExternalServices();
        request.setTo(service);

        final DiscoverExternalServices response = conOne.sendIqRequestAndWaitForResponse(request);
        final Collection<String> discoveredExpiryDates = response.getServices().stream().map(DiscoverExternalServices.Service::getExpires).filter(Objects::nonNull).collect(Collectors.toSet());
        if (discoveredExpiryDates.isEmpty()) {
            throw new TestNotPossibleException("The server under test does not provide any data that has en 'expires' value.");
        }

        final Set<String> invalidFormat = new HashSet<>();
        for (final String discoveredExpiryDate : discoveredExpiryDates) {
            try {
                ParserUtils.getDateFromXep82String(discoveredExpiryDate);
            } catch (ParseException e) {
                invalidFormat.add(discoveredExpiryDate);
            }
        }

        assertTrue(invalidFormat.isEmpty(), "Expected all 'expires' timestamps received by '" + conOne + "' from '" + service + "' to be in the dateTime format specified in XMPP Date and Time Profiles (XEP-0082), but these values were not: " + String.join(", ", invalidFormat));
    }

    @SmackIntegrationTest(section = "2", quote = "expires - A timestamp indicating when the provided username and password credentials will expire. The format [...] MUST be expressed in UTC.")
    public void requestAllExpiryTimestampsZuluTest() throws SmackException.NotConnectedException, InterruptedException, XMPPException.XMPPErrorException, SmackException.NoResponseException, TestNotPossibleException
    {
        final DiscoverExternalServices request = new DiscoverExternalServices();
        request.setTo(service);

        final DiscoverExternalServices response = conOne.sendIqRequestAndWaitForResponse(request);
        final Collection<String> discoveredExpiryDates = response.getServices().stream().map(DiscoverExternalServices.Service::getExpires).filter(Objects::nonNull).collect(Collectors.toSet());
        if (discoveredExpiryDates.isEmpty()) {
            throw new TestNotPossibleException("The server under test does not provide any data that has en 'expires' value.");
        }

        final String[] zuluEndings = new String[] { "z", "Z", "+00:00", "-00:00"};
        final Set<String> notUTC = new HashSet<>();
        for (final String discoveredExpiryDate : discoveredExpiryDates) {
            if (Arrays.stream(zuluEndings).noneMatch(discoveredExpiryDate::endsWith)) {
                notUTC.add(discoveredExpiryDate);
            }
        }

        assertTrue(notUTC.isEmpty(), "Expected all 'expires' timestamps received by '" + conOne + "' from '" + service + "' to be expressed in UTC, but these values were not: " + String.join(", ", notUTC));
    }

    @SmackIntegrationTest(section = "3.1", quote = "A requesting entity requests all services by sending a <services/> element to its server or a discovery service.")
    public void requestAllServicesTest() throws SmackException.NotConnectedException, InterruptedException, XMPPException.XMPPErrorException, SmackException.NoResponseException
    {
        final DiscoverExternalServices request = new DiscoverExternalServices();
        request.setTo(service);

        final IQ response = conOne.sendIqRequestAndWaitForResponse(request);
        assertTrue(response instanceof DiscoverExternalServices, "Expected a request for all services to return a valid response (but it did not).");
    }

    @SmackIntegrationTest(section = "3.2", quote = "A requesting entity requests services of a particular type by sending a <services/> element including a 'type' attribute specifying the service type of interest.")
    public void requestSelectedServicesTest() throws SmackException.NotConnectedException, InterruptedException, XMPPException.XMPPErrorException, SmackException.NoResponseException, TestNotPossibleException
    {
        final DiscoverExternalServices request = new DiscoverExternalServices();
        request.setTo(service);

        final DiscoverExternalServices response = conOne.sendIqRequestAndWaitForResponse(request);
        final List<DiscoverExternalServices.Service> discoveredServices = response.getServices();
        if (discoveredServices.isEmpty()) {
            throw new TestNotPossibleException("The server under test does not provide any data. Cannot request selected services (while expecting a non-empty response).");
        }

        final Map<String, List<DiscoverExternalServices.Service>> servicesByTypes = discoveredServices.stream().collect(Collectors.groupingBy(DiscoverExternalServices.Service::getType));
        for (final Map.Entry<String, List<DiscoverExternalServices.Service>> servicesByType : servicesByTypes.entrySet()) {
            final String type = servicesByType.getKey();
            final List<DiscoverExternalServices.Service> expectedServices = servicesByType.getValue();

            final DiscoverExternalServices requestSelected = new DiscoverExternalServices();
            requestSelected.setTo(service);
            requestSelected.setServiceType(type);

            final DiscoverExternalServices selectedResponse = conOne.sendIqRequestAndWaitForResponse(requestSelected);
            final List<DiscoverExternalServices.Service> actualServices = selectedResponse.getServices();
            assertEquals(type, selectedResponse.getServiceType(), "Expected the response to a request for services of a particular type to echo back that type in the child element of the response (but it did not).");

            assertTrue(expectedServices.size() == actualServices.size() && expectedServices.containsAll(actualServices) && actualServices.containsAll(expectedServices),
                "When querying for services of type '" + type + "', the response is expected to be equal to all services from the scope-less response of that type. Expected: " + expectedServices + " Actual: " + actualServices);
        }
    }

    @SmackIntegrationTest(section = "3.3", quote = "The entity can request credentials by sending a special request to the server composed of a <credentials/> element qualified by the 'urn:xmpp:extdisco:2' namespace and contains a <service/> element.")
    public void requestServiceCredentialsTest() throws SmackException.NotConnectedException, InterruptedException, XMPPException.XMPPErrorException, SmackException.NoResponseException, TestNotPossibleException
    {
        final DiscoverExternalServices request = new DiscoverExternalServices();
        request.setTo(service);

        final DiscoverExternalServices response = conOne.sendIqRequestAndWaitForResponse(request);
        final List<DiscoverExternalServices.Service> discoveredServices = response.getServices();
        if (discoveredServices.isEmpty()) {
            throw new TestNotPossibleException("The server under test does not provide any data. Cannot request credentials for a service (as there do not appear to be any in existence).");
        }

        // We can't predict if the response that we get is one that includes an error or not. We'll only assert that any response can be parsed and thus seems syntactically correct.
        final DiscoverExternalServices.Service firstDiscoveredService = discoveredServices.get(0);
        final ServiceCredentials credentialsRequest = new ServiceCredentials(firstDiscoveredService.getHost(), firstDiscoveredService.getType(), firstDiscoveredService.getPort());
        credentialsRequest.setTo(service);
        final IQ credentialsResponse = conOne.sendIqRequestAndWaitForResponse(credentialsRequest);
        assertTrue(credentialsResponse instanceof ServiceCredentials, "Expected a request for credentials to return a response (but it did not).");
    }

    @SmackIntegrationTest(section = "3.3", quote = "If the server cannot obtain credentials at the service, it returns an appropriate stanza error [...]")
    public void requestServiceCredentialsNonExistingServiceTest() throws SmackException.NotConnectedException, InterruptedException, XMPPException.XMPPErrorException, SmackException.NoResponseException, TestNotPossibleException, XmppStringprepException
    {
        final String server = "doesntexist"+StringUtils.randomString(4);
        final ServiceCredentials request = new ServiceCredentials(server, "test");
        request.setTo(service);

        try {
            conOne.sendIqRequestAndWaitForResponse(request);
            fail("Expected a request made by '" + conOne + "' to '" + service + "' for credentials of a non-exising service ('" + server + "') to be responded to with an IQ error response (but it did not).");
        } catch (XMPPException.XMPPErrorException e) {
            // Expected to catch this Exception. We cannot be certain what error condition is returned, so there's no explicit assertion for that.
        }
    }
}
