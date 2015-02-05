/*
 *   <copyright
 *   notice="oco-source"
 *   pids="5725-P60"
 *   years="2015"
 *   crc="1438874957" >
 *   IBM Confidential
 *
 *   OCO Source Materials
 *
 *   5724-H72
 *
 *   (C) Copyright IBM Corp. 2015
 *
 *   The source code for the program is not published
 *   or otherwise divested of its trade secrets,
 *   irrespective of what has been deposited with the
 *   U.S. Copyright Office.
 *   </copyright>
 */

package com.ibm.mqlight.api.impl;

import java.util.HashMap;
import java.util.Map;

import junit.framework.AssertionFailedError;
import static org.junit.Assert.*;

import org.junit.Test;

import com.google.gson.GsonBuilder;
import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.StateException;
import com.ibm.mqlight.api.endpoint.Endpoint;
import com.ibm.mqlight.api.endpoint.EndpointPromise;
import com.ibm.mqlight.api.endpoint.EndpointService;
import com.ibm.mqlight.api.impl.callback.SameThreadCallbackService;
import com.ibm.mqlight.api.impl.engine.DeliveryRequest;
import com.ibm.mqlight.api.timer.TimerService;

public class TestDeliveryImpl {

    private class StubEndpointService implements EndpointService {
        @Override public void lookup(EndpointPromise promise) {}
        @Override public void onSuccess(Endpoint endpoint) {}
    }

    private class StubTimerService implements TimerService {
        @Override public void schedule(long delay, Promise<Void> promise) {}
        @Override public void cancel(Promise<Void> promise) {}
    }

    private class MockClient extends NonBlockingClientImpl {
        final boolean retvalFromDoDelivery;
        public MockClient(boolean retvalFromDoDelivery) {
            super(new StubEndpointService(),
                  new SameThreadCallbackService(),
                  new MockComponent(),
                  new StubTimerService(),
                  new GsonBuilder(),
                  null, null, null);
            this.retvalFromDoDelivery = retvalFromDoDelivery;
        }

        @Override
        protected boolean doDelivery(DeliveryRequest request) {
            return retvalFromDoDelivery;
        }
    }

    private class MockDelivery extends DeliveryImpl {

        protected MockDelivery(NonBlockingClientImpl client, QOS qos,
                               String share, String topic, String topicPattern, long ttl,
                               Map<String, Object> properties, DeliveryRequest deliveryRequest) {
            super(client, qos, share, topic, topicPattern, ttl, properties, deliveryRequest);
        }

        @Override
        public Type getType() {
            throw new AssertionFailedError("Should not have been called!");
        }

    }

    @Test
    public void getters() {
        MockClient client = new MockClient(true);
        QOS expectedQos = QOS.AT_LEAST_ONCE;
        String expectedShare = "share";
        String expectedTopic = "topic";
        String expectedTopicPattern = "topicPattern";
        long expectedTtl = 5;
        HashMap<String, Object> expectedProperties = new HashMap<>();
        DeliveryRequest deliveryRequest = new DeliveryRequest(null, null, null, null, null);

        MockDelivery delivery =
                new MockDelivery(client, expectedQos, expectedShare, expectedTopic, expectedTopicPattern, expectedTtl, expectedProperties, deliveryRequest);

        assertEquals("properties", expectedProperties, delivery.getProperties());
        assertEquals("qos", expectedQos, delivery.getQOS());
        assertEquals("share", expectedShare, delivery.getShare());
        assertEquals("topic", expectedTopic, delivery.getTopic());
        assertEquals("topicPattern", expectedTopicPattern, delivery.getTopicPattern());
        assertEquals("ttl", expectedTtl, delivery.getTtl());
    }

    @Test
    public void confirmWhenAutoConfirm() {
        MockClient client = new MockClient(true);
        MockDelivery delivery =
                new MockDelivery(client, QOS.AT_LEAST_ONCE, null, "topic", "topic", 0, null, null);
        try {
            delivery.confirm();
            throw new AssertionFailedError("Expected StateException to be thrown");
        } catch(StateException e) {
            // Expected: delivery was auto-confirm...
        }
    }

    @Test
    public void confirmSuccessful() {
        MockClient client = new MockClient(true);
        DeliveryRequest deliveryRequest = new DeliveryRequest(null, null, null, null, null);
        MockDelivery delivery =
                new MockDelivery(client, QOS.AT_LEAST_ONCE, null, "topic", "topic", 0, null, deliveryRequest);
        delivery.confirm();
    }

    @Test
    public void confirmNetworkLost() {
        MockClient client = new MockClient(false);
        DeliveryRequest deliveryRequest = new DeliveryRequest(null, null, null, null, null);
        MockDelivery delivery =
                new MockDelivery(client, QOS.AT_LEAST_ONCE, null, "topic", "topic", 0, null, deliveryRequest);
        try {
            delivery.confirm();
            throw new AssertionFailedError("Expected StateException to be thrown");
        } catch(StateException e) {
            // Expected: network was lost since delivery was made
        }
    }

    @Test
    public void duplicateConform() {
        MockClient client = new MockClient(true);
        DeliveryRequest deliveryRequest = new DeliveryRequest(null, null, null, null, null);
        MockDelivery delivery =
                new MockDelivery(client, QOS.AT_LEAST_ONCE, null, "topic", "topic", 0, null, deliveryRequest);
        delivery.confirm();
        try {
            delivery.confirm();
            throw new AssertionFailedError("Expected StateException to be thrown");
        } catch(StateException e) {
            // Expected: duplicate attempt to confirm.
        }
    }
}
