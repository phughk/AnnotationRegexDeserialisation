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

package com.kaznowski.ard.io;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * This is the same code as {@link StringReader} except that the {@link StringReader#ready()} isn't stupid.
 * The original {@link StringReader#ready()} returns true if the next read is non blocking. This is apparently
 * always true since reading a string will never block. This implementation makes ready return false if it has reached
 * the end of the line
 */
public class NotStupidStringReader extends Reader {
  private String str;
  private int length;
  private int next = 0;
  private int mark = 0;

  public NotStupidStringReader( String var1 ) {
    this.str = var1;
    this.length = var1.length();
  }

  private void ensureOpen() throws IOException {
    if ( this.str == null )
    {
      throw new IOException( "Stream closed" );
    }
  }

  public int read() throws IOException {
    Object var1 = this.lock;
    synchronized ( this.lock )
    {
      this.ensureOpen();
      return !ready() ? -1 : this.str.charAt( this.next++ );
    }
  }

  public int read( char[] var1, int var2, int var3 ) throws IOException {
    Object var4 = this.lock;
    synchronized ( this.lock )
    {
      this.ensureOpen();
      if ( var2 >= 0 && var2 <= var1.length && var3 >= 0 && var2 + var3 <= var1.length && var2 + var3 >= 0 )
      {
        if ( var3 == 0 )
        {
          return 0;
        }
        else if ( this.next >= this.length )
        {
          return -1;
        }
        else
        {
          int var5 = Math.min( this.length - this.next, var3 );
          this.str.getChars( this.next, this.next + var5, var1, var2 );
          this.next += var5;
          return var5;
        }
      }
      else
      {
        throw new IndexOutOfBoundsException();
      }
    }
  }

  public long skip( long var1 ) throws IOException {
    Object var3 = this.lock;
    synchronized ( this.lock )
    {
      this.ensureOpen();
      if ( this.next >= this.length )
      {
        return 0L;
      }
      else
      {
        long var4 = Math.min( (long) (this.length - this.next), var1 );
        var4 = Math.max( (long) (-this.next), var4 );
        this.next = (int) ((long) this.next + var4);
        return var4;
      }
    }
  }

  public boolean ready() throws IOException {
    Object var1 = this.lock;
    synchronized ( this.lock )
    {
      this.ensureOpen();
      return this.next < this.length;
    }
  }

  public boolean markSupported() {
    return true;
  }

  public void mark( int var1 ) throws IOException {
    if ( var1 < 0 )
    {
      throw new IllegalArgumentException( "Read-ahead limit < 0" );
    }
    else
    {
      Object var2 = this.lock;
      synchronized ( this.lock )
      {
        this.ensureOpen();
        this.mark = this.next;
      }
    }
  }

  public void reset() throws IOException {
    Object var1 = this.lock;
    synchronized ( this.lock )
    {
      this.ensureOpen();
      this.next = this.mark;
    }
  }

  public void close() {
    this.str = null;
  }
}

