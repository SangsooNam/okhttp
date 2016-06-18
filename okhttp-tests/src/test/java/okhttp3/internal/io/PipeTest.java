/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package okhttp3.internal.io;

import java.io.InterruptedIOException;
import java.util.concurrent.TimeUnit;
import okio.Buffer;
import okio.Source;
import org.junit.Test;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

public final class PipeTest {
  @Test public void test() throws Exception {
    Pipe pipe = new Pipe(6);
    pipe.sink.write(new Buffer().writeUtf8("abc"), 3L);

    Source source = pipe.source;
    Buffer readBuffer = new Buffer();
    assertEquals(3L, source.read(readBuffer, 6L));
    assertEquals("abc", readBuffer.readUtf8());

    pipe.sink.close();
    assertEquals(-1L, source.read(readBuffer, 6L));

    source.close();
  }

  @Test public void writeTimeout() throws Exception {
    Pipe pipe = new Pipe(3);
    pipe.sink.timeout().timeout(250, TimeUnit.MILLISECONDS);
    pipe.sink.write(new Buffer().writeUtf8("abc"), 3L);
    long startNanos = System.nanoTime();
    try {
      pipe.sink.write(new Buffer().writeUtf8("def"), 3L);
      fail();
    } catch (InterruptedIOException expected) {
      assertEquals("timeout", expected.getMessage());
    }
    long timeoutNanos = System.nanoTime();
    assertEquals(250.0, TimeUnit.NANOSECONDS.toMillis(timeoutNanos - startNanos), 100.0);

    Buffer readBuffer = new Buffer();
    assertEquals(3L, pipe.source.read(readBuffer, 6L));
    assertEquals("abc", readBuffer.readUtf8());
  }

  @Test public void readTimeout() throws Exception {
    Pipe pipe = new Pipe(3L);
    pipe.source.timeout().timeout(250, TimeUnit.MILLISECONDS);
    long startNanos = System.nanoTime();
    Buffer readBuffer = new Buffer();
    try {
      pipe.source.read(readBuffer, 6L);
      fail();
    } catch (InterruptedIOException expected) {
      assertEquals("timeout", expected.getMessage());
    }
    long timeoutNanos = System.nanoTime();
    assertEquals(250.0, TimeUnit.NANOSECONDS.toMillis(timeoutNanos - startNanos), 100.0);
    assertEquals(0, readBuffer.size());
  }
}
