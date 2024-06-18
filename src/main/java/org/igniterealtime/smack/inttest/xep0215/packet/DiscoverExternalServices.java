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
public class DiscoverExternalServices extends IQ
{
    public static final String ELEMENT = "services";
    public static final String NAMESPACE = "urn:xmpp:extdisco:2";

    private final List<Service> services = new LinkedList<>();

    private String serviceType = null;

    public DiscoverExternalServices() {
        super(ELEMENT, NAMESPACE);
    }

    /**
     * Returns the type of the service to which this request pertains (eg: 'turn').
     *
     * This method returns null when the request is not scoped to a particular service type.
     *
     * @return the optional service type.
     */
    public String getServiceType()
    {
        return serviceType;
    }

    /**
     * Defines the type of the service to which this request pertains (eg: 'turn').
     *
     * @param serviceType A type of service
     */
    public void setServiceType(String serviceType)
    {
        this.serviceType = serviceType;
    }

    /**
     * Adds a new service to the discovered information.
     *
     * @param service the discovered service
     */
    public void addService(final Service service) {
        services.add(service);
    }

    /**
     * Adds a collection of services to the discovered information.
     *
     * This method does nothing if the provided argument is null.
     *
     * @param servicesToAdd the discovered services
     */
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
        xml.optAttribute("type", getServiceType());
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

        public enum Action {
            add, delete, modify
        }

        /**
         * When sending a push update, the action value indicates if the service is being added or deleted from the set
         * of known services (or simply being modified).
         *
         * The defined values are "add", "remove", and "modify", where "add" is the default.
         */
        private Action action;

        /**
         * A timestamp indicating when the provided username and password credentials will expire. The format MUST
         * adhere to the dateTime format specified in XMPP Date and Time Profiles (XEP-0082) and MUST be expressed in
         * UTC.
         */
        private String expires;

        /**
         * Either a fully qualified domain name (FQDN) or an IP address (IPv4 or IPv6).
         */
        private final String host;

        /**
         * A friendly (human-readable) name or label for the service.
         */
        private String name;

        /**
         * A service- or server-generated password for use at the service.
         */
        private String password;

        /**
         * The communications port to be used at the host.
         */
        private Integer port;

        /**
         * A boolean value indicating that username and password credentials are required and will need to be requested
         * if not already provided
         */
        private Boolean restricted;

        /**
         * The underlying transport protocol to be used when communicating with the service (typically either TCP or UDP).
         */
        private String transport;

        /**
         * The service type as registered with the XMPP Registrar.
         */
        private final String type;

        /**
         * A service- or server-generated username for use at the service.
         */
        private String username;

        /**
         * Extended information added by the server or service.
         */
        private DataForm dataForm;

        public Service(String host, String type)
        {
            this.host = host;
            this.type = type;
        }

        public Action getAction()
        {
            return action;
        }

        public void setAction(Action action)
        {
            this.action = action;
        }

        public String getExpires()
        {
            return expires;
        }

        public void setExpires(String expires)
        {
            this.expires = expires;
        }

        public String getHost()
        {
            return host;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getPassword()
        {
            return password;
        }

        public void setPassword(String password)
        {
            this.password = password;
        }

        public Integer getPort()
        {
            return port;
        }

        public void setPort(Integer port)
        {
            this.port = port;
        }

        public Boolean isRestricted()
        {
            return restricted;
        }

        public void setRestricted(Boolean restricted)
        {
            this.restricted = restricted;
        }

        public String getTransport()
        {
            return transport;
        }

        public void setTransport(String transport)
        {
            this.transport = transport;
        }

        public String getType()
        {
            return type;
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
            xml.optAttribute("action", action);
            xml.optAttribute("expires", expires);
            xml.attribute("host", host);
            xml.optAttribute("name", name);
            xml.optAttribute("password", password);
            xml.optAttribute("port", port);
            if (restricted != null) {
                xml.optAttribute("restricted", Boolean.toString(restricted));
            }
            xml.optAttribute("transport", transport);
            xml.attribute("type", type);
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
            return action == service.action && Objects.equals(expires, service.expires) && Objects.equals(host, service.host) && Objects.equals(name, service.name) && Objects.equals(password, service.password) && Objects.equals(port, service.port) && Objects.equals(restricted, service.restricted) && Objects.equals(transport, service.transport) && Objects.equals(type, service.type) && Objects.equals(username, service.username) && Objects.equals(dataForm, service.dataForm);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(action, expires, host, name, password, port, restricted, transport, type, username, dataForm);
        }

        @Override
        public String toString() {
            return toXML().toString();
        }
    }
}
