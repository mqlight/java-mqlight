/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.ibm.mqlight.api.samples;

import java.util.HashMap;
import java.util.Map;

public class ArgumentParser {

    public static class Results {
        public final Map<String, Object> parsed;
        public final String[] unparsed;
        private Results(Map<String, Object> parsed, String[] unparsed) {
            this.parsed = parsed;
            this.unparsed = unparsed;
        }
    }

    private static class ExpectedArgument {
        private final String shortName;
        private final String longName;
        private final Class<?> type;
        private ExpectedArgument(String shortName, String longName, Class<?> type) {
            this.shortName = shortName;
            this.longName = longName;
            this.type = type;
        }
    }

    private final HashMap<String, ExpectedArgument> expected = new HashMap<>();
    private final HashMap<String, Object> defaults = new HashMap<>();

    public ArgumentParser() {}

    public ArgumentParser expect(String shortName, String longName, Class<?> type, Object defaultValue) {
        if (!(type == null || type == Double.class || type == String.class || type == Boolean.class || type == Integer.class)) {
            throw new IllegalArgumentException("Unsupported type: " + (type == null ? null : type.getClass()));
        }
        if ((defaultValue != null) && (type == null || !type.isAssignableFrom(defaultValue.getClass()))) {
            throw new IllegalArgumentException("Invalid default: " + (defaultValue == null ? null : defaultValue.getClass()));
        }
        ExpectedArgument arg = new ExpectedArgument(shortName, longName, type);
        if (shortName != null) {
            expected.put(shortName, arg);
            if (defaultValue == null) {
                if (type == null) {
                    defaults.put(shortName, false);
                }
            } else {
                defaults.put(shortName, defaultValue);
            }
        }
        if (longName != null) {
            expected.put(longName, arg);
            if (defaultValue == null) {
                if (type == null) {
                    defaults.put(longName, false);
                }
            } else {
                defaults.put(longName, defaultValue);
            }
        }
        return this;
    }

    public Results parse(String[] args) {
        HashMap<String, Object> parsed = new HashMap<>(defaults);

        int i=0;
        while(i < args.length) {
            ExpectedArgument ea = null;
            String arg = args[i];
            String value = null;
            for (ExpectedArgument a : expected.values()) {
                if (a.type == null) {
                    if ((args[i].equals(a.shortName) || args[i].equals(a.longName))) {
                        ea = a;
                        value = Boolean.TRUE.toString();
                        break;
                    }
                } else {
                    if (a.longName != null && args[i].startsWith(a.longName+"=")) {
                        ea = a;
                        value = args[i].substring(a.longName.length()+1);
                        if ("".equals(value)) throw new IllegalArgumentException("No value supplied for argument '" + arg + "'");
                        break;
                    } else if ((a.type == Double.class) || (a.type == Integer.class)) {
                        if (a.shortName != null && args[i].startsWith(a.shortName) && (a.shortName.length() < args[i].length())) {
                            String tmp = args[i].substring(a.shortName.length());
                            try {
                                Double.parseDouble(tmp);
                                ea = a;
                                value = tmp;
                                break;
                            } catch (NumberFormatException e) {
                                // Ignore - argument might start with a string that matches but the rest of
                                // the argument isn't a valid numeric value.
                            }
                        }
                    }
                    if ((i+1) < args.length) {
                        if (a.shortName != null && args[i].equals(a.shortName)) {
                            ea = a;
                            value = args[++i];
                            break;
                        }
                    }
                }
            }

            if (ea == null) {
                break;
            } else {
                Object v = value;   // Assume ea.type == String unless changed below...
                try {
                    if (ea.type == null || ea.type == Boolean.class) {
                        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                            v = Boolean.valueOf(value);
                        } else {
                            throw new IllegalArgumentException("Value for argument '" + arg + "' must be TRUE or FALSE");
                        }
                    } else if (ea.type == Double.class) {
                        v = Double.valueOf(value);
                    } else if (ea.type == Integer.class) {
                        v = Integer.valueOf(value);
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Value for argument '" + arg + "' must be a numberic value");
                }
                if (ea.shortName != null) parsed.put(ea.shortName, v);
                if (ea.longName != null) parsed.put(ea.longName, v);
            }
            ++i;
        }

        String[] unparsed = new String[args.length - i];
        System.arraycopy(args, i, unparsed, 0, unparsed.length);
        return new Results(parsed, unparsed);
    }
}
