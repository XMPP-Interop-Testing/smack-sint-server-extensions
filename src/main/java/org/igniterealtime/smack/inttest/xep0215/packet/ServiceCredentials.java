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
package org.igniterealtime.smack.inttest.xep0215.packet;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import java.util.*;

/**
 * Ab 'external service discovery' stanza implementation, as defined by XEP-0215, which represents a list of
 * discovered services.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 * @see <a href="https://xmpp.org/extensions/xep-0215.html">XEP-0215: External Service Discovery</a>
 */
public class ServiceCredentials extends IQ
{
    public static final String ELEMENT = "credentials";
    public static final String NAMESPACE = "urn:xmpp:extdisco:2";

    private final List<Service> services = new LinkedList<>();

    public ServiceCredentials(final String host, final String type) {
        this(host, type, null);
    }

    public ServiceCredentials(final String host, final String type, final Integer port) {
        super(ELEMENT, NAMESPACE);
        final Service service = new Service(host, type);
        service.setPort(port);
        services.add(service);
    }

    public void addService(final Service service) {
        services.add(service);
    }

    public void addServices(final Collection<Service> servicesToAdd) {
        if (servicesToAdd != null) {
            this.services.addAll(servicesToAdd);
        }
    }

    /**
     * Returns an unmodifiable list of all services that are part of the discovered information.
     *
     * @return A list of services (possibly empty, never null).
     */
    public List<Service> getServices() {
        return Collections.unmodifiableList(services);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        services.forEach(service -> xml.append(service.toXML()));
        return xml;
    }

    /**
     * Representation of a discovered 'service' as defined by XEP-0215.
     */
    public static class Service
    {
        public static final String ELEMENT = "service";

        private final String host;

        private final String type;

        private Integer port;

        private String password;

        private String username;

        private DataForm dataForm;

        public Service(String host, String type)
        {
            this.host = host;
            this.type = type;
        }

        public String getHost()
        {
            return host;
        }

        public String getType()
        {
            return type;
        }

        public Integer getPort()
        {
            return port;
        }

        public void setPort(Integer port)
        {
            this.port = port;
        }

        public String getPassword()
        {
            return password;
        }

        public void setPassword(String password)
        {
            this.password = password;
        }

        public String getUsername()
        {
            return username;
        }

        public void setUsername(String username)
        {
            this.username = username;
        }

        public DataForm getDataForm()
        {
            return dataForm;
        }

        public void setDataForm(DataForm dataForm)
        {
            this.dataForm = dataForm;
        }

        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement("service");
            xml.attribute("host", host);
            xml.attribute("type", type);
            xml.optAttribute("port", port);
            xml.optAttribute("password", password);
            xml.optAttribute("username", username);
            xml.optAppend(dataForm);

            xml.closeEmptyElement();
            return xml;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Service service = (Service) o;
            return Objects.equals(host, service.host) && Objects.equals(type, service.type) && Objects.equals(port, service.port) && Objects.equals(password, service.password) && Objects.equals(username, service.username) && Objects.equals(dataForm, service.dataForm);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(host, type, port, password, username, dataForm);
        }

        @Override
        public String toString() {
            return toXML().toString();
        }
    }
}
