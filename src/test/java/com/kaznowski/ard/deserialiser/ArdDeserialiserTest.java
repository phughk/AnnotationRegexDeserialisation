/**
 * MIT License
 *
 * Copyright (c) 2018-2018 Przemyslaw Hugh Kaznowski
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.kaznowski.ard.deserialiser;

import com.kaznowski.ard.samples.SingleFieldNoLimitPojo;
import com.kaznowski.ard.samples.SingleFieldPatternPojo;
import com.kaznowski.ard.samples.SinglePropertyPatternPojo;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ArdDeserialiserTest {

  @Test
  public void deserialisingNoTextProvidesEmptyList() {
    // given any matcher
    ArdDeserialiser<SingleFieldNoLimitPojo> subject = new ArdDeserialiser<>( SingleFieldNoLimitPojo.class );

    // when matching against an empty string
    List<SingleFieldNoLimitPojo> result = subject.match( "" );

    // then the result is an empty list
    assertEquals( 0, result.size() );
  }

  @Test
  public void fieldMatchesEntireTextWhenValueIsEmpty() {
    // given a matcher that will match the entire text
    ArdDeserialiser<SingleFieldNoLimitPojo> subject = new ArdDeserialiser<>( SingleFieldNoLimitPojo.class );

    // when we deserialise a chunk of text
    String source = "Full chunk of text";
    List<SingleFieldNoLimitPojo> result = subject.match( source );

    // then the entire text has been deserialised into a single value
    assertEquals( 1, result.size() );
    assertEquals( source, result.get( 0 ).value );
  }

  @Test
  public void regexPatternMatchesOnField() {
    // given a matcher that will read only a subset of the text
    ArdDeserialiser<SingleFieldPatternPojo> subject = new ArdDeserialiser<>( SingleFieldPatternPojo.class );

    // when we deserialise a chunk of text
    List<SingleFieldPatternPojo> result = subject.match( "zzzabccbazzz" );

    // then we get a single match with the expected value matching pattern
    assertEquals( 1, result.size() );
    assertEquals( "abccba", result.get( 0 ).value );
  }

  @Test
  public void regexWorksOnSetter() {
    // given a matcher for a private property with a public setter
    ArdDeserialiser<SinglePropertyPatternPojo> subject = new ArdDeserialiser<>( SinglePropertyPatternPojo.class );

    // when we deserialise a chunk of text
    List<SinglePropertyPatternPojo> result = subject.match( "some text" );

    // then we get the expected value
    assertEquals( 1, result.size() );
    assertEquals( "some text", result.get( 0 ).getValue() );
  }
}
