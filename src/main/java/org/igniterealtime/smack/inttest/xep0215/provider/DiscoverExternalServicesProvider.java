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
package org.igniterealtime.smack.inttest.xep0215.provider;

import org.igniterealtime.smack.inttest.xep0215.packet.DiscoverExternalServices;
import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import java.io.IOException;
import java.text.ParseException;

/**
 * A provider for DiscoverExternalServices stanzas.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 * @see <a href="https://xmpp.org/extensions/xep-0215.html">XEP-0215: External Service Discovery</a>
 */
public class DiscoverExternalServicesProvider extends IqProvider<DiscoverExternalServices>
{
    public DiscoverExternalServices parse(XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment)
        throws XmlPullParserException, IOException
    {
        final DiscoverExternalServices result = new DiscoverExternalServices();
        boolean done = false;
        DiscoverExternalServices.Service service;
        DiscoverExternalServices.Service.Action action = null;
        String expiresString = null; // Leaving this as a string rather than a date, as we want to test the raw String in an integration test.
        String host = null;
        String name = null;
        String password = null;
        Integer port = null;
        Boolean restricted = null;
        String transport = null;
        String type = null;
        String username = null;

        result.setServiceType(parser.getAttributeValue("type"));
        while (!done)
        {
            final XmlPullParser.Event eventType = parser.next();

            if (eventType == XmlPullParser.Event.START_ELEMENT && DiscoverExternalServices.Service.ELEMENT.equals(parser.getName()))
            {
                // Initialize the variables from the parsed XML
                final String actionVal = parser.getAttributeValue("action");
                action = actionVal == null ? null : DiscoverExternalServices.Service.Action.valueOf(actionVal);
                expiresString = parser.getAttributeValue("expires");
                host = ParserUtils.getRequiredAttribute(parser,"host");
                name = parser.getAttributeValue("name");
                password = parser.getAttributeValue("password");
                port = ParserUtils.getIntegerAttribute(parser, "port");
                restricted = ParserUtils.getBooleanAttribute(parser,"restricted");
                transport = parser.getAttributeValue("transport");
                type = ParserUtils.getRequiredAttribute(parser,"type");
                username = parser.getAttributeValue("username");
            }
            else if (eventType == XmlPullParser.Event.END_ELEMENT && DiscoverExternalServices.Service.ELEMENT.equals(parser.getName()))
            {
                // Create a new Service and add it to the result.
                service = new DiscoverExternalServices.Service(host, type);
                service.setAction(action);
                service.setExpires(expiresString);
                service.setName(name);
                service.setPassword(password);
                service.setPort(port);
                service.setRestricted(restricted);
                service.setTransport(transport);
                service.setUsername(username);

                result.addService(service);
            }
            else if (eventType == XmlPullParser.Event.END_ELEMENT && DiscoverExternalServices.ELEMENT.equals(parser.getName()))
            {
                done = true;
            }
        }

        return result;
    }
}
