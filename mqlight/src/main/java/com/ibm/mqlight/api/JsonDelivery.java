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

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * A sub-type of delivery that is used to represent JSON data being received
 * by the client.
 */
public interface JsonDelivery {

    /**
     * Deserializes the JSON data into an object of the specified class.  Use the {@link JsonDelivery#getData(Type)}
     * method instead if the class is of a generic type.  This is equivalent to calling {@link Gson#fromJson(String, Type)}.
     *
     * @param classOfT the class for the desired object.
     * @return an object of type T (or <code>null</code> if the JSON is a representation of null).
     * @throws JsonSyntaxException if the JSON data cannot be deserialized into an object of type <code>classOfT</code>.
     */
    <T> T getData(Class<T> classOfT) throws JsonSyntaxException;

    /**
     * Deserializes the JSON data into an object of the specified type.  Use the {@link JsonDelivery#getData(Class)}
     * method instead if the class is not of a generic type.  This is equivalent to calling {@link Gson#fromJson(String, Type)}.
     *
     * @param typeOfT the type for the desired object.
     * @return an object of type T (or <code>null</code> if the JSON is a representation of null).
     * @throws JsonSyntaxException if the JSON data cannot be deserialized into an object of type <code>typeOfT</code>.
     */
    <T> T getData(Type typeOfT) throws JsonSyntaxException;

    /**
     * @return a {@link JsonElement} representing the deserialized JSON data.  This is equivalent to calling {@link JsonParser#parse(String)}.
     * @throws JsonParseException if the data is not valid JSON.
     * @throws JsonSyntaxException if the data contains malformed JSON elements.
     */
    JsonElement getData() throws JsonParseException, JsonSyntaxException;

    /**
     * @return a <code>String</code> representation of the JSON.  This allows other parsers to be used.
     */
    String getRawData();
}
