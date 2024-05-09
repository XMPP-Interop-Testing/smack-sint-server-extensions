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
package org.igniterealtime.smack.inttest.util;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests that verify the implementation of {@link JUnitXmlTestRunResultProcessor}
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class JUnitXmlTestRunResultProcessorTest
{
    @Test
    public void testNoSpecification() throws Exception
    {
        // Setup test fixture.
        final String specification = null;
        final String specificationSection = null;

        // Execute system under test.
        final URI result = JUnitXmlTestRunResultProcessor.generateLink(specification, specificationSection);

        // Verify results.
        assertNull(result);
    }

    @Test
    public void testXepWithSection() throws Exception
    {
        // Setup test fixture.
        final String specification = JUnitXmlTestRunResultProcessor.normalizeSpecification("xep-0030");
        final String specificationSection = "8";

        // Execute system under test.
        final URI result = JUnitXmlTestRunResultProcessor.generateLink(specification, specificationSection);

        // Verify results.
        assertNotNull(result);
        assertTrue(result.toString().startsWith("https://xmpp.org/extensions/xep-0030.html"));
    }

    @Test
    public void testXepWithoutSection() throws Exception
    {
        // Setup test fixture.
        final String specification = JUnitXmlTestRunResultProcessor.normalizeSpecification("xep-0030");
        final String specificationSection = null;

        // Execute system under test.
        final URI result = JUnitXmlTestRunResultProcessor.generateLink(specification, specificationSection);

        // Verify results.
        assertNotNull(result);
        assertEquals("https://xmpp.org/extensions/xep-0030.html", result.toString());
    }

    @Test
    public void testXepWithSpace() throws Exception
    {
        // Setup test fixture.
        final String specification = JUnitXmlTestRunResultProcessor.normalizeSpecification("xep 0030");
        final String specificationSection = null;

        // Execute system under test.
        final URI result = JUnitXmlTestRunResultProcessor.generateLink(specification, specificationSection);

        // Verify results.
        assertNotNull(result);
        assertEquals("https://xmpp.org/extensions/xep-0030.html", result.toString());
    }

    @Test
    public void testXepWithoutSpace() throws Exception
    {
        // Setup test fixture.
        final String specification = JUnitXmlTestRunResultProcessor.normalizeSpecification("xep0030");
        final String specificationSection = null;

        // Execute system under test.
        final URI result = JUnitXmlTestRunResultProcessor.generateLink(specification, specificationSection);

        // Verify results.
        assertEquals("https://xmpp.org/extensions/xep-0030.html", result.toString());
    }

    @Test
    public void testXepWithDashSpace() throws Exception
    {
        // Setup test fixture.
        final String specification = JUnitXmlTestRunResultProcessor.normalizeSpecification("xep-0030");
        final String specificationSection = null;

        // Execute system under test.
        final URI result = JUnitXmlTestRunResultProcessor.generateLink(specification, specificationSection);

        // Verify results.
        assertNotNull(result);
        assertEquals("https://xmpp.org/extensions/xep-0030.html", result.toString());
    }

    /**
     * @see <a href="https://github.com/XMPP-Interop-Testing/smack-sint-server-extensions/issues/21">Improper characters being used to construct URL in JUnitXmlTestRunResultProcessor</a>
     */
    @Test
    public void testXepWithFreeFormSection() throws Exception
    {
        // Setup test fixture.
        final String specification = JUnitXmlTestRunResultProcessor.normalizeSpecification("xep-0030");
        final String specificationSection = "7.1 & 7.2.2";

        // Execute system under test.
        final URI result = JUnitXmlTestRunResultProcessor.generateLink(specification, specificationSection);

        // Verify results.
        assertNotNull(result);
        assertTrue(result.toString().startsWith("https://xmpp.org/extensions/xep-0030.html"));
    }

    @Test
    public void testRfcWithSection() throws Exception
    {
        // Setup test fixture.
        final String specification = JUnitXmlTestRunResultProcessor.normalizeSpecification("rfc6121");
        final String specificationSection = "8";

        // Execute system under test.
        final URI result = JUnitXmlTestRunResultProcessor.generateLink(specification, specificationSection);

        // Verify results.
        assertNotNull(result);
        assertTrue(result.toString().startsWith("https://www.rfc-editor.org/rfc/rfc6121.html"));
    }

    @Test
    public void testRfcWithoutSection() throws Exception
    {
        // Setup test fixture.
        final String specification = JUnitXmlTestRunResultProcessor.normalizeSpecification("rfc6121");
        final String specificationSection = null;

        // Execute system under test.
        final URI result = JUnitXmlTestRunResultProcessor.generateLink(specification, specificationSection);

        // Verify results.
        assertNotNull(result);
        assertEquals("https://www.rfc-editor.org/rfc/rfc6121.html", result.toString());
    }

    @Test
    public void testRfcWithSpace() throws Exception
    {
        // Setup test fixture.
        final String specification = JUnitXmlTestRunResultProcessor.normalizeSpecification("rfc 6121");
        final String specificationSection = null;

        // Execute system under test.
        final URI result = JUnitXmlTestRunResultProcessor.generateLink(specification, specificationSection);

        // Verify results.
        assertNotNull(result);
        assertEquals("https://www.rfc-editor.org/rfc/rfc6121.html", result.toString());
    }

    @Test
    public void testRfcWithoutSpace() throws Exception
    {
        // Setup test fixture.
        final String specification = JUnitXmlTestRunResultProcessor.normalizeSpecification("rfc6121");
        final String specificationSection = null;

        // Execute system under test.
        final URI result = JUnitXmlTestRunResultProcessor.generateLink(specification, specificationSection);

        // Verify results.
        assertNotNull(result);
        assertEquals("https://www.rfc-editor.org/rfc/rfc6121.html", result.toString());
    }

    @Test
    public void testRfcWithDashSpace() throws Exception
    {
        // Setup test fixture.
        final String specification = JUnitXmlTestRunResultProcessor.normalizeSpecification("rfc-6121");
        final String specificationSection = null;

        // Execute system under test.
        final URI result = JUnitXmlTestRunResultProcessor.generateLink(specification, specificationSection);

        // Verify results.
        assertNotNull(result);
        assertEquals("https://www.rfc-editor.org/rfc/rfc6121.html", result.toString());
    }
}
