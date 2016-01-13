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

import static org.junit.Assert.*;

import java.util.ArrayList;

import junit.framework.AssertionFailedError;

import org.junit.Test;

public class TestArgumentParser {

    @Test
    public void shortStringArgument() {
        ArgumentParser parser = new ArgumentParser();
        parser.expect("-a", null, String.class, null);
        
        ArgumentParser.Results r1 = parser.parse(new String[]{"-a", "v1"});
        assertEquals("Expected one parsed argument", 1, r1.parsed.size());
        assertEquals("Expected no unparsed arguments", 0, r1.unparsed.length);
        assertEquals("Expected value missing from parsed arguments", "v1", r1.parsed.get("-a"));
    }
    
    @Test
    public void shortStringArgumentWithUnparsed() {
        ArgumentParser parser = new ArgumentParser();
        parser.expect("-a", null, String.class, null);
        
        ArgumentParser.Results r1 = parser.parse(new String[]{"-a", "v1", "v2"});
        assertEquals("Expected one parsed argument", 1, r1.parsed.size());
        assertEquals("Expected one unparsed arguments", 1, r1.unparsed.length);
        assertEquals("Expected value missing from parsed arguments", "v1", r1.parsed.get("-a"));
        assertEquals("Expected value missing from unparsed arguments", "v2", r1.unparsed[0]);
    }
    
    @Test
    public void shortSingleArgumentThatMatchesStringArgument() {
        ArgumentParser parser = new ArgumentParser();
        parser.expect("-a", null, String.class, null);
        
        ArgumentParser.Results r1 = parser.parse(new String[]{"-a"});
        assertEquals("Expected no parsed argument", 0, r1.parsed.size());
        assertEquals("Expected one unparsed arguments", 1, r1.unparsed.length);
        assertEquals("Expected value missing from unparsed arguments", "-a", r1.unparsed[0]);
    }
    
    @Test
    public void longStringArgument() {
        ArgumentParser parser = new ArgumentParser();
        parser.expect(null, "-aaaargh", String.class, null);
        
        ArgumentParser.Results r1 = parser.parse(new String[]{"-aaaargh=v1"});
        assertEquals("Expected one parsed argument", 1, r1.parsed.size());
        assertEquals("Expected no unparsed arguments", 0, r1.unparsed.length);
        assertEquals("Expected value missing from parsed arguments", "v1", r1.parsed.get("-aaaargh"));
    }
    
    @Test
    public void longStringArgumentWithUnparsed() {
        ArgumentParser parser = new ArgumentParser();
        parser.expect(null, "-aaaargh", String.class, null);
        
        ArgumentParser.Results r1 = parser.parse(new String[]{"-aaaargh=v1", "v2"});
        assertEquals("Expected one parsed argument", 1, r1.parsed.size());
        assertEquals("Expected one unparsed arguments", 1, r1.unparsed.length);
        assertEquals("Expected value missing from parsed arguments", "v1", r1.parsed.get("-aaaargh"));
        assertEquals("Expected value missing from unparsed arguments", "v2", r1.unparsed[0]);
    }
    
    @Test
    public void longSingleArgumentThatMatchesStringArgument() {
        ArgumentParser parser = new ArgumentParser();
        parser.expect(null, "-aaaargh", String.class, null);
        
        ArgumentParser.Results r1 = parser.parse(new String[]{"-aaaargh"});
        assertEquals("Expected no parsed argument", 0, r1.parsed.size());
        assertEquals("Expected one unparsed arguments", 1, r1.unparsed.length);
        assertEquals("Expected value missing from unparsed arguments", "-aaaargh", r1.unparsed[0]);
    }
    
    @Test
    public void longAndShortStringArguments() {
        ArgumentParser parser = new ArgumentParser();
        parser.expect("-a", "-aaaargh", String.class, null);
        
        ArgumentParser.Results r1 = parser.parse(new String[]{"-a", "v1"});
        assertEquals("Expected two parsed arguments", 2, r1.parsed.size());
        assertEquals("Expected no unparsed arguments", 0, r1.unparsed.length);
        assertEquals("Expected value missing from parsed arguments", "v1", r1.parsed.get("-a"));
        assertEquals("Expected value missing from parsed arguments", "v1", r1.parsed.get("-aaaargh"));
    }
    
    @Test
    public void longAndShortStringArgumenstWithUnparsed() {
        ArgumentParser parser = new ArgumentParser();
        parser.expect("-a", "-aaaargh", String.class, null);
        
        ArgumentParser.Results r1 = parser.parse(new String[]{"-aaaargh=v1", "v2"});
        assertEquals("Expected one parsed argument", 2, r1.parsed.size());
        assertEquals("Expected one unparsed arguments", 1, r1.unparsed.length);
        assertEquals("Expected value missing from parsed arguments", "v1", r1.parsed.get("-a"));
        assertEquals("Expected value missing from parsed arguments", "v1", r1.parsed.get("-aaaargh"));
        assertEquals("Expected value missing from unparsed arguments", "v2", r1.unparsed[0]);
    }
    
    @Test
    public void longAndShortSingleArgumentThatMatchesStringArguments() {
        ArgumentParser parser = new ArgumentParser();
        parser.expect("-a", "-aaaargh", String.class, null);
        
        ArgumentParser.Results r1 = parser.parse(new String[]{"-a"});
        ArgumentParser.Results r2 = parser.parse(new String[]{"-aaaargh"});
        assertEquals("Expected no parsed argument", 0, r1.parsed.size());
        assertEquals("Expected one unparsed arguments", 1, r1.unparsed.length);
        assertEquals("Expected value missing from unparsed arguments", "-a", r1.unparsed[0]);
        assertEquals("Expected no parsed argument", 0, r2.parsed.size());
        assertEquals("Expected one unparsed arguments", 1, r2.unparsed.length);
        assertEquals("Expected value missing from unparsed arguments", "-aaaargh", r2.unparsed[0]);
    }
    
    @Test
    public void integerArgument() {
        ArgumentParser parser = new ArgumentParser();
        parser.expect("-b", "-burglh", Integer.class, null);
        
        ArgumentParser.Results[] results = new ArgumentParser.Results[] {
            parser.parse(new String[]{"-b1"}),
            parser.parse(new String[]{"-b", "1"}),
            parser.parse(new String[]{"-burglh=1"}),
        };
        for (int i=0; i < results.length; ++i) {
            assertEquals("Expected two parsed arguments [i="+i+"]", 2, results[i].parsed.size());
            assertEquals("Expected no unparsed arguments [i="+i+"]", 0, results[i].unparsed.length);
            assertEquals("Expected value for short argument to match [i="+i+"]", 1, results[i].parsed.get("-b"));
            assertEquals("Expected value for long argument to match [i="+i+"]", 1, results[i].parsed.get("-burglh"));
        }
    }
    
    @Test
    public void integerArgumentWithUnparsed() {
        ArgumentParser parser = new ArgumentParser();
        parser.expect("-b", "-burglh", Integer.class, null);
        
        ArgumentParser.Results[] results = new ArgumentParser.Results[] {
            parser.parse(new String[]{"-b1", "v"}),
            parser.parse(new String[]{"-b", "1", "v"}),
            parser.parse(new String[]{"-burglh=1", "v"}),
        };
        for (int i=0; i < results.length; ++i) {
            assertEquals("Expected two parsed arguments [i="+i+"]", 2, results[i].parsed.size());
            assertEquals("Expected one unparsed arguments [i="+i+"]", 1, results[i].unparsed.length);
            assertEquals("Expected value for short argument to match [i="+i+"]", 1, results[i].parsed.get("-b"));
            assertEquals("Expected value for long argument to match [i="+i+"]", 1, results[i].parsed.get("-burglh"));
            assertEquals("Expected value for unparsed argument to match [i="+i+"]", "v", results[i].unparsed[0]);
        }
    }

    @Test
    public void doubleArgument() {
        ArgumentParser parser = new ArgumentParser();
        parser.expect("-c", "-cuuurgh", Double.class, null);
        
        ArgumentParser.Results[] results = new ArgumentParser.Results[] {
            parser.parse(new String[]{"-c-1.5"}),
            parser.parse(new String[]{"-c", "-1.5"}),
            parser.parse(new String[]{"-cuuurgh=-1.5"}),
        };
        for (int i=0; i < results.length; ++i) {
            assertEquals("Expected two parsed arguments [i="+i+"]", 2, results[i].parsed.size());
            assertEquals("Expected no unparsed arguments [i="+i+"]", 0, results[i].unparsed.length);
            assertEquals("Expected value for short argument to match [i="+i+"]", -1.5, results[i].parsed.get("-c"));
            assertEquals("Expected value for long argument to match [i="+i+"]", -1.5, results[i].parsed.get("-cuuurgh"));
        }
    }

    @Test
    public void SingleArgument() {
        ArgumentParser parser = new ArgumentParser();
        parser.expect("-d", "-durrh", null, null);
        
        ArgumentParser.Results[] results = new ArgumentParser.Results[] {
            parser.parse(new String[]{"-d"}),
            parser.parse(new String[]{"-durrh"})
        };
        for (int i=0; i < results.length; ++i) {
            assertEquals("Expected two parsed argument [i="+i+"]", 2, results[i].parsed.size());
            assertEquals("Expected no unparsed arguments [i="+i+"]", 0, results[i].unparsed.length);
            assertEquals("Expected value for short argument to match [i="+i+"]", true, results[i].parsed.get("-d"));
            assertEquals("Expected value for long argument to match [i="+i+"]", true, results[i].parsed.get("-durrh"));
        }
        
        results = new ArgumentParser.Results[] {
            parser.parse(new String[]{"-x"}),
            parser.parse(new String[]{"-yyyy"})
        };
        for (int i=0; i < results.length; ++i) {
            assertEquals("Expected no parsed arguments [i="+i+"]", 2, results[i].parsed.size());
            assertEquals("Expected one unparsed arguments [i="+i+"]", 1, results[i].unparsed.length);
            assertEquals("Expected value for short argument to match [i="+i+"]", false, results[i].parsed.get("-d"));
            assertEquals("Expected value for long argument to match [i="+i+"]", false, results[i].parsed.get("-durrh"));
        }
    }
    
    @Test
    public void booleanArgument() {
        ArgumentParser parser = new ArgumentParser();
        parser.expect("-b", "-burglh", Boolean.class, null);
        
        ArgumentParser.Results[] results = new ArgumentParser.Results[] {
            parser.parse(new String[]{"-b", "true"}),
            parser.parse(new String[]{"-burglh=TRUE"}),
            parser.parse(new String[]{"-burglh=tRuE"}),
        };
        for (int i=0; i < results.length; ++i) {
            assertEquals("Expected two parsed arguments [i="+i+"]", 2, results[i].parsed.size());
            assertEquals("Expected no unparsed arguments [i="+i+"] (unparsed is: "+ results[i].unparsed, 0, results[i].unparsed.length);
            assertEquals("Expected value for short argument to match [i="+i+"]", true, results[i].parsed.get("-b"));
            assertEquals("Expected value for long argument to match [i="+i+"]", true, results[i].parsed.get("-burglh"));
        }
    }
    
    @Test
    public void booleanArgumentWithUnparsed() {
        ArgumentParser parser = new ArgumentParser();
        parser.expect("-b", "-burglh", Boolean.class, null);
        
        ArgumentParser.Results[] results = new ArgumentParser.Results[] {
            parser.parse(new String[]{"-b", "FALSE", "v"}),
            parser.parse(new String[]{"-burglh=false", "v"}),
            parser.parse(new String[]{"-burglh=FaLsE", "v"}),
        };
        for (int i=0; i < results.length; ++i) {
            assertEquals("Expected two parsed arguments [i="+i+"]", 2, results[i].parsed.size());
            assertEquals("Expected one unparsed arguments [i="+i+"]", 1, results[i].unparsed.length);
            assertEquals("Expected value for short argument to match [i="+i+"]", false, results[i].parsed.get("-b"));
            assertEquals("Expected value for long argument to match [i="+i+"]", false, results[i].parsed.get("-burglh"));
            assertEquals("Expected value for unparsed argument to match [i="+i+"]", "v", results[i].unparsed[0]);
        }
    }
    
    @Test
    public void badness() {
        ArgumentParser parser = new ArgumentParser();
        parser.expect("-e", "-eeek", Integer.class, null);
        
        ArgumentParser.Results r1 = parser.parse(new String[]{"-ea"});
        assertEquals("Expected no parsed arguments", 0, r1.parsed.size());
        assertEquals("Expected one unparsed argument", 1, r1.unparsed.length);
        assertEquals("Expected value for unparsed arguments to match", "-ea", r1.unparsed[0]);
        
        ArrayList<String[]>failingCases  = new ArrayList<>();
        failingCases.add(new String[]{"-e", "X"});
        failingCases.add(new String[]{"-eeek=X"});
        failingCases.add(new String[]{"-eeek="});
        
        for (int i = 0; i < failingCases.size(); ++i) {
            try {
                parser.parse(failingCases.get(i));
                throw new AssertionFailedError("Expected parsing to fail on: "+i);
            } catch(IllegalArgumentException e) {
                // Expected
            }
        }
        
        parser = new ArgumentParser();
        try {
            parser.expect("-a", "-aa", Void.class, null);
            throw new AssertionFailedError("Expected setup of parser to fail");
        } catch(IllegalArgumentException e) {
            // Expected
        }
    }
    
    @Test
    public void booleanBadness() {
        ArgumentParser parser = new ArgumentParser();
        parser.expect("-b", "-bk", Boolean.class, null);
        
        ArrayList<String[]> failingCases  = new ArrayList<>();
        failingCases.add(new String[]{"-ba"});
        failingCases.add(new String[]{"-bka"});
        failingCases.add(new String[]{"-b"});
        for (int i = 0; i < failingCases.size(); ++i) {
            ArgumentParser.Results r1 = parser.parse(new String[]{"-ba"});
            assertEquals("Expected no parsed arguments", 0, r1.parsed.size());
            assertEquals("Expected one unparsed argument", 1, r1.unparsed.length);
            assertEquals("Expected value for unparsed arguments to match", "-ba", r1.unparsed[0]);
        }
        
        failingCases = new ArrayList<>();
        failingCases.add(new String[]{"-bk=X"});
        failingCases.add(new String[]{"-bk="});
        failingCases.add(new String[]{"-bk=t"});
        failingCases.add(new String[]{"-bk=truee"});
        failingCases.add(new String[]{"-bk=f"});
        failingCases.add(new String[]{"-bk=falsee"});
        for (int i = 0; i < failingCases.size(); ++i) {
            try {
                parser.parse(failingCases.get(i));
                throw new AssertionFailedError("Expected parsing to fail on: "+i);
            } catch(IllegalArgumentException e) {
                // Expected
            }
        }
    }
}
