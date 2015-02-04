package com.ibm.mqlight.api;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

public interface JsonDelivery {

    /**
     *
     * @param classOfT
     * @return
     * @throws JsonSyntaxException
     */
    <T> T getData(Class<T> classOfT) throws JsonSyntaxException;
    /**
     *
     * @param typeOfT
     * @return
     * @throws JsonSyntaxException
     */
    <T> T getData(Type typeOfT) throws JsonSyntaxException;

    /**
     *
     * @return
     * @throws JsonSyntaxException
     */
    JsonElement getData() throws JsonSyntaxException;

    /**
     *
     * @return
     */
    String getRawData();
}
