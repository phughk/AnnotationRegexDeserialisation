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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ArdDeserialiser<E> {
  private final Class<E> clazz;
  private final Logger LOG = Logger.getLogger( ArdDeserialiser.class.getName() );
  private final Handler handler = new ConsoleHandler();

  /**
   * @param clazz Class of generic that is being deserialised to.
   */
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
    Field field = fieldsAnnotatedForDeserialisation().stream().findFirst().get();
    FieldExpression fieldExpression = field.getAnnotation( FieldExpression.class );
    String pattern = patternFromField( fieldExpression );
    String valueMatchingPattern = matchByPattern( pattern, result );
    setField( instance, field, valueMatchingPattern );
    return instance;
  }

  private String patternFromField( FieldExpression fieldExpression ) {
    String raw = fieldExpression.value();
    if ( "".equals( raw ) )
    {
      return ".*";
    }
    return raw;
  }

  private void setField( E instance, Field field, Object value ) {
    try
    {
      field.set( instance, value );
    }
    catch ( IllegalAccessException e )
    {
      // field not accessible, trying setter
      Optional<Method> setter = findFieldSetter( field );
      if ( setter.isPresent() )
      {
        setFieldViaSetter( instance, setter.get(), value );
      }
      else
      {
        throw new RuntimeException( String.format( "Unable to set %s TODO", field.getName() ) );
      }
    }
  }

  private void setFieldViaSetter( E instance, Method setter, Object value ) {
    try
    {
      setter.invoke( instance, value );
    }
    catch ( IllegalAccessException | InvocationTargetException e )
    {
      logError( e, LOG::severe );
    }
  }

  private static Optional<Method> findFieldSetter( Field field ) {
    String fieldName = field.getName();
    String firstLetter = fieldName.substring( 0, 1 );
    String remainingLetters = fieldName.substring( 1, fieldName.length() );
    String setterName = String.format( "set%s%s", firstLetter.toUpperCase(), remainingLetters );
    try
    {
      return Optional.of( field.getDeclaringClass().getDeclaredMethod( setterName, field.getType() ) );
    }
    catch ( NoSuchMethodException e )
    {
      return Optional.empty();
    }
  }

  private String matchByPattern( String regex, String source ) {
    Pattern pattern = Pattern.compile( regex );
    Matcher matcher = pattern.matcher( source );
    matcher.find();
    return matcher.group();
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

  @Deprecated
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

  private Collection<Field> fieldsAnnotatedForDeserialisation() {
    return Arrays.stream( clazz.getDeclaredFields() ).filter( isRegexField() ).collect( Collectors.toList() );
  }

  private static Predicate<Field> isRegexField() {
    return field -> field.getAnnotation( FieldExpression.class ) != null;
  }
}
