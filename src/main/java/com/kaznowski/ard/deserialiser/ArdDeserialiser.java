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

import com.kaznowski.ard.annotation.FieldExpression;
import com.kaznowski.ard.io.NotStupidStringReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

public final class ArdDeserialiser<E> {
  private final Class<E> clazz;
  private final Logger LOG = Logger.getLogger( ArdDeserialiser.class.getName() );
  Handler handler = new ConsoleHandler();

  public ArdDeserialiser( final Class<E> clazz ) {
    this.clazz = clazz;
    LOG.addHandler( handler );
    handler.setLevel( Level.ALL );
    handler.setFormatter( new SimpleFormatter() );
  }

  public List<E> match( String text ) {
    List<E> results = new ArrayList<E>();
    BufferedReader bufferedReader = new BufferedReader( new NotStupidStringReader( text ) );
    while ( feedNotFinished( bufferedReader ) )
    {
      results.add( matchSingle( bufferedReader ) );
    }
    return results;
  }

  private boolean feedNotFinished( Reader reader ) {
    try
    {
      return reader.ready();
    }
    catch ( IOException e )
    {
      throw new RuntimeException( "TODO" );
    }
  }

  private E matchSingle( BufferedReader reader ) {
    String result = consumeAndReturnNextSearchSpace( reader );
    Supplier<E> constructor = createConstructor( clazz );
    E instance = constructor.get();
    Field field = regexMatchers().stream().findFirst().get();
    setField( instance, field, result );
    return instance;
  }

  private void setField( E instance, Field field, Object value ) {
    try
    {
      field.set( instance, value );
    }
    catch ( IllegalAccessException e )
    {
      logError( e, LOG::severe );
    }
  }

  private String consumeAndReturnNextSearchSpace( BufferedReader bufferedReader ) {
    StringBuilder stringBuilder = new StringBuilder();
    try
    {
      while ( bufferedReader.ready() )
      {
        stringBuilder.append( (char) bufferedReader.read() );
      }
    }
    catch ( IOException e )
    {
      logError( e, LOG::info ); // TODO
    }
    return stringBuilder.toString();
  }

  private static void logError( Throwable throwable, Consumer<String> logger ) {
    Writer writer = new StringWriter();
    PrintWriter printWriter = new PrintWriter( writer );
    throwable.printStackTrace( printWriter );
    logger.accept( writer.toString() );
  }

  private static <E> Supplier<E> createConstructor( Class<E> clazz ) {
    Constructor<E> classConstructor = (Constructor<E>) clazz.getDeclaredConstructors()[0];
    return () ->
    {
      try
      {
        return classConstructor.newInstance();
      }
      catch ( InstantiationException | IllegalAccessException | InvocationTargetException e )
      {
        throw new RuntimeException( "TODO" );
      }
    };
  }

  private Collection<Field> regexMatchers() {
    return Arrays.stream( clazz.getDeclaredFields() ).filter( isRegexField() ).collect( Collectors.toList() );
  }

  private static Predicate<Field> isRegexField() {
    return field -> field.getAnnotation( FieldExpression.class ) == null;
  }
}