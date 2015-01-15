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

package com.ibm.mqlight.api.impl.endpoint;

import org.junit.Test;

public class TestEndpointImpl {

    @Test(expected=IllegalArgumentException.class)
    public void badUriProtocol() {
        new EndpointImpl("mailto://example.org", null, null);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void badUriHost() {
        new EndpointImpl("amqp://", null, null);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void badUriMissingPassword1() {
        new EndpointImpl("amqp://user@example.org", null, null);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void badUriMissingPassword2() {
        new EndpointImpl("amqp://example.org", null, "password");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void badUriInvalidCredentials() {
        new EndpointImpl("amqp://user:password:whats_this@example.org", null, "password");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void badUriMissingUser() {
        new EndpointImpl("amqp://:password@example.org", null, null);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void badUriDuplicateUser() {
        new EndpointImpl("amqp://user:password:example.org", "user", null);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void badUriDuplicatePassword() {
        new EndpointImpl("amqp://user:password:example.org", null, "password");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void badUriDuplicateCredientials() {
        new EndpointImpl("amqp://user:password:example.org", "user", "password");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void badUriItsNotAUri() {
        new EndpointImpl("Hello, My name is Inigo Montoya", null, null);
    }
}
