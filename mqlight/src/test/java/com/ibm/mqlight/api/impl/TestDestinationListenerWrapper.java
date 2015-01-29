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

import java.nio.BufferOverflowException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.AssertionFailedError;
import static org.junit.Assert.*;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.amqp.messaging.DeliveryAnnotations;
import org.apache.qpid.proton.amqp.messaging.MessageAnnotations;
import org.junit.Test;

import com.ibm.mqlight.api.BytesDelivery;
import com.ibm.mqlight.api.ClientOptions;
import com.ibm.mqlight.api.Delivery;
import com.ibm.mqlight.api.DestinationListener;
import com.ibm.mqlight.api.MalformedDelivery;
import com.ibm.mqlight.api.NonBlockingClient;
import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.StringDelivery;
import com.ibm.mqlight.api.callback.CallbackService;
import com.ibm.mqlight.api.endpoint.Endpoint;
import com.ibm.mqlight.api.endpoint.EndpointPromise;
import com.ibm.mqlight.api.endpoint.EndpointService;
import com.ibm.mqlight.api.impl.engine.DeliveryRequest;
import com.ibm.mqlight.api.network.NetworkChannel;
import com.ibm.mqlight.api.network.NetworkListener;
import com.ibm.mqlight.api.network.NetworkService;
import com.ibm.mqlight.api.timer.TimerService;

public class TestDestinationListenerWrapper {

    private class StubClient extends NonBlockingClientImpl {
        public StubClient() {
            super(new EndpointService() {
                      @Override public void onSuccess(Endpoint endpoint) {}
                      @Override public void lookup(EndpointPromise promise) {}
                  }, new CallbackService() {
                      @Override public void run(Runnable runnable, Object orderingCtx, Promise<Void> promise) {}
                  }, new NetworkService() {
                      @Override public void connect(Endpoint endpoint, NetworkListener listener, Promise<NetworkChannel> promise) {}
                  }, new TimerService() {
                      @Override public void schedule(long delay, Promise<Void> promise) {}
                      @Override public void cancel(Promise<Void> promise) {}
                  }, ClientOptions.builder().build(), null, null);
        }
    }

    private static class MockListener implements DestinationListener<Object> {
        private enum Method {
            ON_MESSAGE, ON_MALFORMED, ON_UNSUBSCRIBED, NONE
        }
        private final Method expectedMethod;
        private NonBlockingClient actualClient = null;
        private Object actualContext = null;
        private String actualTopicPattern = null;
        private String actualShare = null;
        private Delivery actualDelivery = null;
        private MockListener(Method exceptedMethod) {
            this.expectedMethod = exceptedMethod;
        }
        @Override public void onMessage(NonBlockingClient client, Object context, Delivery delivery) {
            if (expectedMethod != Method.ON_MESSAGE) {
                throw new AssertionFailedError("onMessage should not have been called");
            }
            this.actualClient = client;
            this.actualContext = context;
            this.actualDelivery = delivery;
        }
        @Override public void onMalformed(NonBlockingClient client, Object context, MalformedDelivery delivery) {
            if (expectedMethod != Method.ON_MALFORMED) {
                throw new AssertionFailedError("onMalformed should not have been called");
            }
            this.actualClient = client;
            this.actualContext = context;
            this.actualDelivery = delivery;
        }
        @Override public void onUnsubscribed(NonBlockingClient client, Object context, String topicPattern, String share) {
            if (expectedMethod != Method.ON_UNSUBSCRIBED) {
                throw new AssertionFailedError("onUnsubscribed should not have been called");
            }
            this.actualClient = client;
            this.actualContext = context;
            this.actualTopicPattern = topicPattern;
            this.actualShare = share;
        }
        
        private void testClientAndContextMatch(NonBlockingClient expectedClient, Object expectedContext) {
            assertSame("Expected client passed into listener to match", expectedClient, actualClient);
            assertSame("Expected context passed into listener to match", expectedContext, actualContext);
        }
    }
    
    private class MockCallbackService implements CallbackService {
        @Override public void run(Runnable runnable, Object orderingCtx, Promise<Void> promise) {
            runnable.run();
            promise.setSuccess(null);
        }
    }

    @Test
    public void onUnsubscribed() {
        StubClient expectedClient = new StubClient();
        MockListener listener = new MockListener(MockListener.Method.ON_UNSUBSCRIBED);
        Object expectedContext = new Object();
        String expectedPattern = "/kittens";
        String expectedShare = "share";
        MockCallbackService callbackService = new MockCallbackService();
        
        DestinationListenerWrapper<Object> wrapper = new DestinationListenerWrapper<Object>(expectedClient, listener, expectedContext);
        wrapper.onUnsubscribed(callbackService, expectedPattern, expectedShare);
        listener.testClientAndContextMatch(expectedClient, expectedContext);
        assertSame("Expected same topic pattern", expectedPattern, listener.actualTopicPattern);
        assertSame("Expected same share", expectedShare, listener.actualShare);
    }
    
    @Test
    public void onUnsubscribedNullListener() {
        DestinationListenerWrapper<Object> wrapper = new DestinationListenerWrapper<Object>(new StubClient(), null, null);
        wrapper.onUnsubscribed(new MockCallbackService(), "", "");
    }
    
    private byte[] createSerializedProtonMessage(AmqpValue body, String topic, long ttl, Map<String, String> properties, Map<Symbol, Object> annotations) {
        org.apache.qpid.proton.message.Message protonMsg = Proton.message();
        protonMsg.setBody(body);
        protonMsg.setAddress("amqp:///" + topic); 
        protonMsg.setTtl(ttl);
        if ((properties != null) && !properties.isEmpty()) {
            protonMsg.setApplicationProperties(new ApplicationProperties(properties));
        }
        if (annotations != null) {
            protonMsg.setDeliveryAnnotations(new DeliveryAnnotations(annotations));
        }
        
        byte data[] = new byte[2 * 1024];
        int length;
        while (true) {
            try {
                length = protonMsg.encode(data,  0,  data.length);
                break;
            } catch(BufferOverflowException boe) {
                data = new byte[data.length * 2];
            }
        }
        byte[] result = new byte[length];
        System.arraycopy(data, 0, result, 0, length);
        return result;
    }
    
    @Test
    public void onDeliveryBytes() {
        StubClient expectedClient = new StubClient();
        MockListener listener = new MockListener(MockListener.Method.ON_MESSAGE);
        Object expectedContext = new Object();
        MockCallbackService callbackService = new MockCallbackService();
        
        byte[] expectedData = new byte[] {3, 1, 4, 1, 5, 9};
        String expectedTopic = "/topic1";
        String expectedTopicPattern = "/#";
        long expectedTtl = 12345;
        QOS expectedQos = QOS.AT_LEAST_ONCE;
        byte[] msgData = createSerializedProtonMessage(new AmqpValue(new Binary(expectedData)), expectedTopic, expectedTtl, null, null);
        
        DeliveryRequest request = new DeliveryRequest(msgData, expectedQos, "private:" + expectedTopicPattern, null, null);
        DestinationListenerWrapper<Object> wrapper = new DestinationListenerWrapper<Object>(expectedClient, listener, expectedContext);
        wrapper.onDelivery(callbackService, request, expectedQos, false);
        
        assertNotNull("Properties should not have been null!", listener.actualDelivery.getProperties());
        assertEquals("Expected no message properties", 0, listener.actualDelivery.getProperties().size());
        assertEquals("Expected QOS to match", expectedQos, listener.actualDelivery.getQOS());
        assertEquals("Expected share to match", null, listener.actualDelivery.getShare());
        assertEquals("Expected topic to match", expectedTopic, listener.actualDelivery.getTopic());
        assertEquals("Expected topic pattern to match", expectedTopicPattern, listener.actualDelivery.getTopicPattern());
        assertEquals("Expected ttl to match", expectedTtl, listener.actualDelivery.getTtl());
        assertEquals("Expected delivery to be of type bytes", Delivery.Type.BYTES, listener.actualDelivery.getType());
        BytesDelivery byteDelivery = (BytesDelivery)listener.actualDelivery;
        assertEquals("Expected same amount of delivery data", expectedData.length, byteDelivery.getData().remaining());
        byte[] actualData = new byte[expectedData.length];
        byteDelivery.getData().get(actualData);
        assertArrayEquals("Expected delivery data to match", expectedData, actualData);
    }
    
    @Test
    public void onDeliveryString() {
        StubClient expectedClient = new StubClient();
        MockListener listener = new MockListener(MockListener.Method.ON_MESSAGE);
        Object expectedContext = new Object();
        MockCallbackService callbackService = new MockCallbackService();
        
        String expectedData = "message data";
        String expectedTopic = "/topic1";
        String expectedTopicPattern = "/#";
        long expectedTtl = 12345;
        QOS expectedQos = QOS.AT_LEAST_ONCE;
        byte[] msgData = createSerializedProtonMessage(new AmqpValue(expectedData), expectedTopic, expectedTtl, null, null);
        
        DeliveryRequest request = new DeliveryRequest(msgData, expectedQos, "private:" + expectedTopicPattern, null, null);
        DestinationListenerWrapper<Object> wrapper = new DestinationListenerWrapper<Object>(expectedClient, listener, expectedContext);
        wrapper.onDelivery(callbackService, request, expectedQos, false);
        
        assertNotNull("Properties should not have been null!", listener.actualDelivery.getProperties());
        assertEquals("Expected no message properties", 0, listener.actualDelivery.getProperties().size());
        assertEquals("Expected QOS to match", expectedQos, listener.actualDelivery.getQOS());
        assertEquals("Expected share to match", null, listener.actualDelivery.getShare());
        assertEquals("Expected topic to match", expectedTopic, listener.actualDelivery.getTopic());
        assertEquals("Expected topic pattern to match", expectedTopicPattern, listener.actualDelivery.getTopicPattern());
        assertEquals("Expected ttl to match", expectedTtl, listener.actualDelivery.getTtl());
        assertEquals("Expected delivery to be of type string", Delivery.Type.STRING, listener.actualDelivery.getType());
        StringDelivery stringDelivery = (StringDelivery)listener.actualDelivery;
        assertEquals("Expected data to match", expectedData, stringDelivery.getData());
    }
    
    @Test
    public void malformedBadAMQPMessageData() {
        StubClient expectedClient = new StubClient();
        MockListener listener = new MockListener(MockListener.Method.ON_MALFORMED);
        Object expectedContext = new Object();
        MockCallbackService callbackService = new MockCallbackService();
        
        QOS expectedQos = QOS.AT_LEAST_ONCE;
        byte[] msgData = "I bet this isn't a valid AMQP message".getBytes();
        
        DeliveryRequest request = new DeliveryRequest(msgData, expectedQos, "private:/malformed", null, null);
        DestinationListenerWrapper<Object> wrapper = new DestinationListenerWrapper<Object>(expectedClient, listener, expectedContext);
        wrapper.onDelivery(callbackService, request, expectedQos, false);
        
        assertEquals("Expected delivery to be of type string", Delivery.Type.MALFORMED, listener.actualDelivery.getType());
        assertEquals("Expected malformed reason to be PAYLOADNOTAMQP", MalformedDelivery.MalformedReason.PAYLOADNOTAMQP, ((MalformedDelivery)listener.actualDelivery).getReason());
        
    }
    
    public void malformedUnhandledAMQPBody() {
        StubClient expectedClient = new StubClient();
        MockListener listener = new MockListener(MockListener.Method.ON_MESSAGE);
        Object expectedContext = new Object();
        MockCallbackService callbackService = new MockCallbackService();
        
        String expectedTopic = "/topic1";
        String expectedTopicPattern = "/#";
        long expectedTtl = 12345;
        QOS expectedQos = QOS.AT_LEAST_ONCE;
        byte[] msgData = createSerializedProtonMessage(new AmqpValue(new Integer(7)), expectedTopic, expectedTtl, null, null);
        
        DeliveryRequest request = new DeliveryRequest(msgData, expectedQos, "private:" + expectedTopicPattern, null, null);
        DestinationListenerWrapper<Object> wrapper = new DestinationListenerWrapper<Object>(expectedClient, listener, expectedContext);
        wrapper.onDelivery(callbackService, request, expectedQos, false);
        
        assertEquals("Expected delivery to be of type string", Delivery.Type.MALFORMED, listener.actualDelivery.getType());
        assertEquals("Expected malformed reason to be PAYLOADNOTAMQP", MalformedDelivery.MalformedReason.FORMATNOMAPPING, ((MalformedDelivery)listener.actualDelivery).getReason());
        
    }
    
    private void testMalformedAnnotation(String stringCondition, MalformedDelivery.MalformedReason reason) {
        StubClient expectedClient = new StubClient();
        MockListener listener = new MockListener(MockListener.Method.ON_MALFORMED);
        Object expectedContext = new Object();
        MockCallbackService callbackService = new MockCallbackService();
        
        String expectedData = "message data";
        String expectedTopic = "/topic1";
        String expectedTopicPattern = "/#";
        long expectedTtl = 12345;
        QOS expectedQos = QOS.AT_LEAST_ONCE;
        String expectedMalformedDescription = "Apathy.  Probably.";
        String expectedMQMDFormat = "abcd";
        int expectedMQMDCCSID = 1234;
        HashMap<Symbol, Object> annotations = new HashMap<>();
        annotations.put(Symbol.getSymbol("x-opt-message-malformed-condition"), Symbol.getSymbol(stringCondition));
        annotations.put(Symbol.getSymbol("x-opt-message-malformed-description"), expectedMalformedDescription);
        annotations.put(Symbol.getSymbol("x-opt-message-malformed-MQMD.Format"), expectedMQMDFormat);
        annotations.put(Symbol.getSymbol("x-opt-message-malformed-MQMD.CodedCharSetId"), expectedMQMDCCSID);
        byte[] msgData = createSerializedProtonMessage(new AmqpValue(expectedData), expectedTopic, expectedTtl, null, annotations);
        
        DeliveryRequest request = new DeliveryRequest(msgData, expectedQos, "private:" + expectedTopicPattern, null, null);
        DestinationListenerWrapper<Object> wrapper = new DestinationListenerWrapper<Object>(expectedClient, listener, expectedContext);
        wrapper.onDelivery(callbackService, request, expectedQos, false);
        
        assertNotNull("Properties should not have been null!", listener.actualDelivery.getProperties());
        assertEquals("Expected no message properties", 0, listener.actualDelivery.getProperties().size());
        assertEquals("Expected QOS to match", expectedQos, listener.actualDelivery.getQOS());
        assertEquals("Expected share to match", null, listener.actualDelivery.getShare());
        assertEquals("Expected topic to match", expectedTopic, listener.actualDelivery.getTopic());
        assertEquals("Expected topic pattern to match", expectedTopicPattern, listener.actualDelivery.getTopicPattern());
        assertEquals("Expected ttl to match", expectedTtl, listener.actualDelivery.getTtl());
        assertEquals("Expected delivery to be of type string", Delivery.Type.MALFORMED, listener.actualDelivery.getType());
        
        MalformedDelivery malformedDelivery = (MalformedDelivery)listener.actualDelivery;
        assertEquals("Malformed reason doesn't match expected", reason, malformedDelivery.getReason());
        assertEquals("Malformed description doesn't match expected", expectedMalformedDescription, malformedDelivery.getDescription());
        assertEquals("Malformed MQMD.Format doesn't match expected", expectedMQMDFormat, malformedDelivery.getMQMDFormat());
        assertEquals("Malformed MQMD.CCSID doesn't match expected", expectedMQMDCCSID, malformedDelivery.getMQMDCodedCharSetId());
    }

    @Test
    public void malformedBasedOnAnnotation1() {
        testMalformedAnnotation("FORMATNOMAPPING", MalformedDelivery.MalformedReason.FORMATNOMAPPING);
    }
    
    @Test
    public void malformedBasedOnAnnotation2() {
        testMalformedAnnotation("JMSNOMAPPING", MalformedDelivery.MalformedReason.JMSNOMAPPING);
    }
    
    @Test
    public void malformedBasedOnAnnotation3() {
        testMalformedAnnotation("PAYLOADENCODING", MalformedDelivery.MalformedReason.PAYLOADENCODING);
    }
    
    @Test
    public void malformedBasedOnAnnotation4() {
        testMalformedAnnotation("PAYLOADNOTAMQP", MalformedDelivery.MalformedReason.PAYLOADNOTAMQP);
    }
}
