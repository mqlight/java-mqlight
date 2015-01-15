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
package com.ibm.mqlight.api;

import java.nio.ByteBuffer;

/**
 * A sub-type of delivery that is used to represent binary data being received
 * by the client.
 */
public interface BytesDelivery extends Delivery {
    /**
     * @return a byte buffer containing a message pay-load data.  Logically, this buffer 
     *         <em>belongs</em> to application at the point this object is supplied to the
     *         <code>DeliveryListener</code>.  That is to say that once passed to the
     *         <code>DeliveryListener</code> the client will never modify the data held
     *         in this buffer.
     */
    ByteBuffer getData();
}
