/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.email.store.ri.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.everit.blobstore.BlobReader;

/**
 * A BlobInputStream contains a {@link BlobReader} that may be read from the stream.
 */
public class BlobInputStream extends InputStream {

  private final BlobReader blobReader;

  /**
   * Simple constructor.
   *
   * @param blobReader
   *          the {@link BlobReader} object. Cannot be <code>null</code>.
   *
   * @throws {@link
   *           NullPointerException} if blobReader parameter is null.
   */
  public BlobInputStream(final BlobReader blobReader) {
    this.blobReader =
        Objects.requireNonNull(blobReader, "The blobReader parameter cannot be null!");
  }

  @Override
  public void close() throws IOException {
    blobReader.close();
  }

  @Override
  public int read() throws IOException {
    byte[] buffer = new byte[1];
    int nRead = blobReader.read(buffer, 0, 1);
    return nRead != -1 ? buffer[0] : -1;
  }

  @Override
  public int read(final byte[] b, final int off, final int len) throws IOException {
    return blobReader.read(b, off, len);
  }

}
