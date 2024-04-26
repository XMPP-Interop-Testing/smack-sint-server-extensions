/**
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
package org.igniterealtime.smack.sint.xep0030;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@SpecificationReference(document = "XEP-0030")
public class ServiceDiscoveryIntegrationTest extends AbstractSmackIntegrationTest
{
    public ServiceDiscoveryIntegrationTest(SmackIntegrationTestEnvironment environment) throws SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        super(environment);

        try {
            final DiscoverInfo discoInfoRequest = DiscoverInfo.builder(connection)
                .to(environment.configuration.service)
                .build();
            connection.sendIqRequestAndWaitForResponse(discoInfoRequest);
        } catch (XMPPException.XMPPErrorException e) {
            throw new TestNotPossibleException("XEP-0030: Service Discovery is not supported by service " + environment.configuration.service);
        }
    }

    /**
     * Asserts that the domain under test returns a service discover 'info' response that contains at least one identity.
     */
    @SmackIntegrationTest(section = "3.1", quote = "The target entity then MUST either return an IQ result, or return an error [...]. The result MUST contain a <query/> element [...], which in turn contains one or more <identity/> elements [...]")
    public void testDiscoInfoResponseContainsIdentity() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final DiscoverInfo request = DiscoverInfo.builder(conOne)
            .to(conOne.getXMPPServiceDomain())
            .build();

        // Execute system-under-test.
        final DiscoverInfo response = connection.sendIqRequestAndWaitForResponse(request);

        // Verify result.
        assertFalse(response.getIdentities().isEmpty(), "Expected the disco#info response from '" + conOne.getXMPPServiceDomain() + "' to contain at least one identity (but it did not).");
    }

    /**
     * Asserts that the domain under test returns a service discover 'info' response that contains at least one feature.
     */
    @SmackIntegrationTest(section = "3.1", quote = "The target entity then MUST either return an IQ result, or return an error [...]. The result MUST contain a <query/> element [...], which in turn contains [...] one or more <feature/> elements.")
    public void testDiscoInfoResponseContainsFeature() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final DiscoverInfo request = DiscoverInfo.builder(conOne)
            .to(conOne.getXMPPServiceDomain())
            .build();

        // Execute system-under-test.
        final DiscoverInfo response = connection.sendIqRequestAndWaitForResponse(request);

        // Verify result.
        assertFalse(response.getFeatures().isEmpty(), "Expected the disco#info response from '" + conOne.getXMPPServiceDomain() + "' to contain at least one feature (but it did not).");
    }

    /**
     * Asserts that the domain under test returns a service discover 'info' response that contains at least the feature
     * identified by the 'http://jabber.org/protocol/disco#info' namespace.
     */
    @SmackIntegrationTest(section = "3.1", quote = "[...] every entity MUST support at least the 'http://jabber.org/protocol/disco#info' feature;")
    public void testDiscoInfoResponseContainsDiscoInfoFeature() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final DiscoverInfo request = DiscoverInfo.builder(conOne)
            .to(conOne.getXMPPServiceDomain())
            .build();

        // Execute system-under-test.
        final DiscoverInfo response = connection.sendIqRequestAndWaitForResponse(request);

        // Verify result.
        final String needle = "http://jabber.org/protocol/disco#info";
        assertTrue(response.getFeatures().stream().anyMatch(feature -> feature.getVar().equals(needle)), "Expected the disco#info response from '" + conOne.getXMPPServiceDomain() + "' to contain the '" + needle + "' feature (but it did not).");
    }

    /**
     * Asserts that each identity returned in a service discover 'info' response by the domain under possesses a 'category'.
     */
    @SmackIntegrationTest(section = "3.1", quote = "Each <identity/> element MUST possess the 'category' [...] attribute[s] specifying the category [...] for the entity")
    public void testIdentitiesPossessCategory() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final DiscoverInfo request = DiscoverInfo.builder(conOne)
            .to(conOne.getXMPPServiceDomain())
            .build();

        // Execute system-under-test.
        try {
            connection.sendIqRequestAndWaitForResponse(request);
        } catch (IllegalArgumentException e) {
            // Verify result.
            // Smack's XMPP parser will catch a missing category, and will throw an exception with this particular message.
            if ("category cannot be null".equals(e.getMessage())) { // TODO: Instead of hard-coding the error message used by Smack, detect it programmatically.
                fail("Expected all 'identity' elements returned by '" + conOne.getXMPPServiceDomain() + "' to contain a category (but at least one did not).");
                return;
            }
            throw e;
        }
    }

    /**
     * Asserts that each identity returned in a service discover 'info' response by the domain under possesses a 'type'.
     */
    @SmackIntegrationTest(section = "3.1", quote = "Each <identity/> element MUST possess the [...] 'type' attribute[s] specifying the [...] type for the entity")
    public void testIdentitiesPossessType() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final DiscoverInfo request = DiscoverInfo.builder(conOne)
            .to(conOne.getXMPPServiceDomain())
            .build();

        // Execute system-under-test.
        try {
            connection.sendIqRequestAndWaitForResponse(request);
        } catch (IllegalArgumentException e) {
            // Verify result.
            // Smack's XMPP parser will catch a missing category, and will throw an exception with this particular message.
            if ("type cannot be null".equals(e.getMessage())) { // TODO: Instead of hard-coding the error message used by Smack, detect it programmatically.
                fail("Expected all 'identity' elements returned by '" + conOne.getXMPPServiceDomain() + "' to contain a type (but at least one did not).");
                return;
            }
            throw e;
        }
    }

    /**
     * Asserts that the 'info' response returned by the domain under test contains no identities that have the same
     * category, type and xml:lang, but a different name.
     */
    @SmackIntegrationTest(section = "3.1", quote = "the <query/> element MUST NOT include multiple <identity/> elements with the same category+type+xml:lang but with different 'name' values")
    public void testDiscoInfoResponseNoDifferentNameForCatTypeAndLang() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final DiscoverInfo request = DiscoverInfo.builder(conOne)
            .to(conOne.getXMPPServiceDomain())
            .build();

        // Execute system-under-test.
        final DiscoverInfo response = connection.sendIqRequestAndWaitForResponse(request);

        // Verify result.
        final Function<DiscoverInfo.Identity, String> keyFunction = identity -> identity.getCategory() + "|" + identity.getType() + "|" + identity.getLanguage();
        final ConcurrentMap<String, HashSet<String>> namesByKey = new ConcurrentHashMap<>();
        for (final DiscoverInfo.Identity identity : response.getIdentities()) {
            namesByKey.computeIfAbsent(keyFunction.apply(identity), k -> new HashSet<>()).add(identity.getName());
        }
        assertTrue(namesByKey.values().stream().noneMatch(s -> s.size() >= 2), "Expected the disco#info response from '" + conOne.getXMPPServiceDomain() + "' to contain only equally named identities for identities that share the same category, type and language (but for at least one such combination, multiple names were provided).");
    }

    /**
     * Asserts that each identity returned in a service discover 'info' response by the domain under possesses a 'type'.
     */
    @SmackIntegrationTest(section = "3.1", quote = "Each <feature/> element MUST possess a 'var' attribute [...]")
    public void testFeaturesPossessVar() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final DiscoverInfo request = DiscoverInfo.builder(conOne)
            .to(conOne.getXMPPServiceDomain())
            .build();

        // Execute system-under-test.
        try {
            connection.sendIqRequestAndWaitForResponse(request);
        } catch (IllegalArgumentException e) {
            // Verify result.
            // Smack's XMPP parser will catch a missing category, and will throw an exception with this particular message.
            if ("variable cannot be null".equals(e.getMessage())) { // TODO: Instead of hard-coding the error message used by Smack, detect it programmatically.
                fail("Expected all 'feature' elements returned by '" + conOne.getXMPPServiceDomain() + "' to contain a var (but at least one did not).");
                return;
            }
            throw e;
        }
    }
}
