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

import org.igniterealtime.smack.inttest.xep0215.packet.ServiceCredentials;
import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import java.io.IOException;

/**
 * A provider for ServiceCredentials stanzas.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 * @see <a href="https://xmpp.org/extensions/xep-0215.html">XEP-0215: External Service Discovery</a>
 */
public class ServiceCredentialsProvider extends IqProvider<ServiceCredentials>
{
    public ServiceCredentials parse(XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment)
        throws XmlPullParserException, IOException
    {
        boolean done = false;
        ServiceCredentials.Service service;
        String host = null;
        String password = null;
        Integer port = null;
        String type = null;
        String username = null;
        final ServiceCredentials result = new ServiceCredentials(host, type);

        while (!done)
        {
            final XmlPullParser.Event eventType = parser.next();

            if (eventType == XmlPullParser.Event.START_ELEMENT && ServiceCredentials.Service.ELEMENT.equals(parser.getName()))
            {
                // Initialize the variables from the parsed XML
                host = ParserUtils.getRequiredAttribute(parser,"host");
                password = parser.getAttributeValue("password");
                port = ParserUtils.getIntegerAttribute(parser, "port");
                type = ParserUtils.getRequiredAttribute(parser,"type");
                username = parser.getAttributeValue("username");
            }
            else if (eventType == XmlPullParser.Event.END_ELEMENT && ServiceCredentials.Service.ELEMENT.equals(parser.getName()))
            {
                // Create a new Service and add it to the result.
                service = new ServiceCredentials.Service(host, type);
                service.setPassword(password);
                service.setPort(port);
                service.setUsername(username);

                result.addService(service);
            }
            else if (eventType == XmlPullParser.Event.END_ELEMENT && ServiceCredentials.ELEMENT.equals(parser.getName()))
            {
                done = true;
            }
        }

        return result;
    }
}
