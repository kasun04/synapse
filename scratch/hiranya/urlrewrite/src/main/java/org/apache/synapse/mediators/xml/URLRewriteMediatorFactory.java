/*
 *  Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.apache.synapse.mediators.xml;

import org.apache.synapse.config.xml.AbstractMediatorFactory;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.apache.synapse.commons.evaluators.config.EvaluatorFactoryFinder;
import org.apache.synapse.commons.evaluators.Evaluator;
import org.apache.synapse.commons.evaluators.EvaluatorException;
import org.apache.synapse.mediators.URLRewriteMediator;
import org.apache.synapse.mediators.RewriteRule;
import org.apache.synapse.mediators.RewriteAction;
import org.apache.axiom.om.OMElement;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import java.util.Iterator;

public class URLRewriteMediatorFactory extends AbstractMediatorFactory {

    private static final QName REWRITE_Q    = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "rewrite");

    public Mediator createMediator(OMElement element) {
        Iterator rules = element.getChildrenWithName(new QName("rule"));
        URLRewriteMediator mediator = new URLRewriteMediator();
        while (rules.hasNext()) {
            RewriteRule rule = parseRule((OMElement) rules.next());
            mediator.addRule(rule);
        }

        return mediator;
    }

    private RewriteRule parseRule(OMElement ruleElement) {
        OMElement conditionElt = ruleElement.getFirstChildWithName(new QName(
                SynapseConstants.SYNAPSE_NAMESPACE, "condition"));
        Iterator actions = ruleElement.getChildrenWithName(new QName(
                SynapseConstants.SYNAPSE_NAMESPACE, "action"));

        if (actions == null) {
            handleException("At least one rewrite action is required");
        }

        RewriteRule rule = new RewriteRule();
        while (actions.hasNext()) {
            rule.addRewriteAction(parseAction((OMElement) actions.next()));
        }

        if (conditionElt != null) {
            OMElement child = conditionElt.getFirstElement();
            if (child != null) {
                try {
                    Evaluator eval = EvaluatorFactoryFinder.getInstance().getEvaluator(child);
                    rule.setCondition(eval);
                } catch (EvaluatorException e) {
                    handleException("Error while parsing the evaluator configuration", e);
                }
            }
        }

        return rule;
    }

    private RewriteAction parseAction(OMElement actionElt) {
        String value = actionElt.getAttributeValue(new QName("value"));
        String xpath = actionElt.getAttributeValue(new QName("xpath"));

        if (value == null && xpath == null) {
            handleException("value or xpath attribute is required on the action element");
        }

        RewriteAction action = new RewriteAction();
        if (xpath != null) {
            try {
                action.setXpath(new SynapseXPath(xpath));
            } catch (JaxenException e) {
                handleException("Error while parsign the XPath expression: " + xpath, e);
            }
        } else if (value != null) {
            action.setValue(value);
        }

        String fragment = actionElt.getAttributeValue(new QName("fragment"));
        if (fragment != null) {
            if ("protocol".equals(fragment)) {
                action.setFragmentIndex(URLRewriteMediator.PROTOCOL);
            } else if ("user".equals(fragment)) {
                action.setFragmentIndex(URLRewriteMediator.USER_INFO);
            } else if ("host".equals(fragment)) {
                action.setFragmentIndex(URLRewriteMediator.HOST);
            } else if ("port".equals(fragment)) {
                action.setFragmentIndex(URLRewriteMediator.PORT);
            } else if ("path".equals(fragment)) {
                action.setFragmentIndex(URLRewriteMediator.PATH);
            } else if ("query".equals(fragment)) {
                action.setFragmentIndex(URLRewriteMediator.QUERY);
            } else if ("ref".equals(fragment)) {
                action.setFragmentIndex(URLRewriteMediator.REF);
            } else if ("full".equals(fragment)) {
                action.setFragmentIndex(URLRewriteMediator.FULL_URI);
            } else {
                handleException("Unknown URL fragment name: " + fragment);
            }
        } else {
            action.setFragmentIndex(URLRewriteMediator.FULL_URI);
        }
        return action;
    }

    public QName getTagQName() {
        return REWRITE_Q;
    }
}
