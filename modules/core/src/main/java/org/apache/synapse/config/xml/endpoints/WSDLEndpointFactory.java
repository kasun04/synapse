/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.config.xml.endpoints;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMNode;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.config.xml.endpoints.utils.WSDL11EndpointBuilder;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.WSDLEndpoint;
import org.apache.synapse.endpoints.utils.EndpointDefinition;

import javax.xml.namespace.QName;
import java.io.File;
import java.net.URL;

/**
 * Creates an {@link WSDLEndpoint} based endpoint from a XML configuration.
 *
 * &lt;wsdl [uri="wsdl-uri"] service="qname" port/endpoint="qname"&gt;
 *   &lt;wsdl:definition&gt;...&lt;/wsdl:definition&gt;?
 *   &lt;wsdl20:description&gt;...&lt;/wsdl20:description&gt;?
 *   &lt;enableRM [policy="key"]/&gt;?
 *   &lt;enableSec [policy="key"]/&gt;?
 *   &lt;enableAddressing [version=("final" | "submission")]/&gt;?
 *   &lt;suspendDurationOnFailure&gt;suspend-duration&lt;/suspendDurationOnFailure&gt;?
 *   &lt;timeout&gt;
 *     &lt;duration&gt;timeout-duration&lt;/duration&gt;
 *     &lt;action&gt;discard|fault&lt;/action&gt;
 *   &lt;/timeout&gt;?
 * &lt;/wsdl&gt;
 */
public class WSDLEndpointFactory extends EndpointFactory {

    private static WSDLEndpointFactory instance = new WSDLEndpointFactory();

    private WSDLEndpointFactory() {}

    public static WSDLEndpointFactory getInstance() {
        return instance;
    }

    protected Endpoint createEndpoint(OMElement epConfig, boolean anonymousEndpoint) {

        WSDLEndpoint wsdlEndpoint = new WSDLEndpoint();
        OMAttribute name = epConfig.getAttribute(new QName(
                org.apache.synapse.config.xml.XMLConfigConstants.NULL_NAMESPACE, "name"));

        if (name != null) {
            wsdlEndpoint.setName(name.getAttributeValue());
        }

        OMElement wsdlElement = epConfig.getFirstChildWithName
                (new QName(SynapseConstants.SYNAPSE_NAMESPACE, "wsdl"));
        if (wsdlElement != null) {

            EndpointDefinition endpoint = null;

            // get the service name and port name. at this point we should not worry about
            // the presence of those parameters. they are handled by corresponding WSDL builders.
            String serviceName = wsdlElement.getAttributeValue(new QName("service"));
            String portName = wsdlElement.getAttributeValue(new QName("port"));
            // check if wsdl is supplied as a URI
            String wsdlURI = wsdlElement.getAttributeValue(new QName("uri"));

            // set serviceName and portName in the endpoint. it does not matter if these are
            // null at this point. we are setting them only for serialization purpose.
            wsdlEndpoint.setServiceName(serviceName);
            wsdlEndpoint.setPortName(portName);

            if (wsdlURI != null) {

                wsdlEndpoint.setWsdlURI(wsdlURI.trim());
                try {
                    OMNode wsdlOM = SynapseConfigUtils.getOMElementFromURL(
                            new URL(wsdlURI).toString());
                    if (wsdlOM != null && wsdlOM instanceof OMElement) {
                        OMElement omElement = (OMElement) wsdlOM;
                        OMNamespace ns = omElement.getNamespace();
                        if (ns != null) {
                            String nsUri = omElement.getNamespace().getNamespaceURI();
                            if (org.apache.axis2.namespace.Constants.NS_URI_WSDL11.equals(nsUri)) {

                                endpoint = new WSDL11EndpointBuilder().
                                        createEndpointDefinitionFromWSDL(
                                                wsdlURI.trim(), omElement, serviceName, portName);

                            } else if (WSDL2Constants.WSDL_NAMESPACE.equals(nsUri)) {
                                //endpoint = new WSDL20EndpointBuilder().
                                // createEndpointDefinitionFromWSDL(wsdlURI, serviceName, portName);

                                handleException("WSDL 2.0 Endpoints are currently not supported");
                            }
                        }
                    }
                } catch (Exception e) {
                    handleException("Couldn't create endpoint from the given WSDL URI : "
                            + e.getMessage(), e);
                }
            }

            // check if the wsdl 1.1 document is suppled inline
            OMElement definitionElement = wsdlElement.getFirstChildWithName
                    (new QName(org.apache.axis2.namespace.Constants.NS_URI_WSDL11, "definitions"));
            if (endpoint == null && definitionElement != null) {
                wsdlEndpoint.setWsdlDoc(definitionElement);
                String resolveRoot = System.getProperty(SynapseConstants.RESOLVE_ROOT);
                String baseUri = "file:./";
                if (resolveRoot != null) {
                    baseUri = resolveRoot.trim();
                }
                if(!baseUri.endsWith(File.separator)){
                    baseUri = baseUri + File.separator;
                }
                endpoint = new WSDL11EndpointBuilder().createEndpointDefinitionFromWSDL(
                        baseUri, definitionElement, serviceName, portName);
            }

            // check if a wsdl 2.0 document is supplied inline
            OMElement descriptionElement = wsdlElement.getFirstChildWithName
                    (new QName(org.apache.axis2.namespace.Constants.NS_URI_WSDL11, "description"));
            if (endpoint == null && descriptionElement != null) {
                wsdlEndpoint.setWsdlDoc(descriptionElement);
                handleException("WSDL 2.0 Endpoints are currently not supported.");
            }
            
            if (endpoint != null) {
                // for now, QOS information has to be provided explicitly.
                extractEndpointProperties(endpoint, wsdlElement);
                wsdlEndpoint.setEndpoint(endpoint);
            } else {
                handleException("WSDL is not specified for WSDL endpoint.");
            }
        }

        return wsdlEndpoint;
    }

}
