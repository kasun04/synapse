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
package org.apache.synapse.samples.framework.tests.endpoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.samples.framework.SampleClientResult;
import org.apache.synapse.samples.framework.SynapseTestCase;
import org.apache.synapse.samples.framework.clients.StockQuoteSampleClient;

import java.util.concurrent.CountDownLatch;

public class Sample54 extends SynapseTestCase {

    private static final Log log = LogFactory.getLog(Sample54.class);
    SampleClientResult result;
    StockQuoteSampleClient client;
    CountDownLatch latch;

    public Sample54() {
        super(54);
        client = getStockQuoteClient();
        latch = new CountDownLatch(1);
    }


    public void testSessionFullLB() {
        String addUrl = "http://localhost:8280/services/LBService1";

        log.info("Running test: Session affinity load balancing between 3 endpoints");
        result = client.statefulClient(addUrl,null, 100);
        assertTrue("Client did not run successfully ", result.gotResponse());
    }


}
