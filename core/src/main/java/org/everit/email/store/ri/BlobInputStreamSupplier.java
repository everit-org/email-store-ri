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
package org.everit.email.store.ri;

import java.io.InputStream;
import java.util.Objects;

import org.everit.blobstore.BlobReader;
import org.everit.blobstore.Blobstore;
import org.everit.email.InputStreamSupplier;

/**
 * Implementation of {@link InputStreamSupplier} that use Blob to create stream.
 */
public class BlobInputStreamSupplier implements InputStreamSupplier {

  private long blobId;

  private Blobstore blobstore;

  /**
   * Simple constructor.
   *
   * @param blobstore
   *          the {@link Blobstore} instance. Cannot be <code>null</code>.
   * @param blobId
   *          the id of the blob.
   *
   * @throws NullPointerException
   *           if blobStore paramter is null.
   */
  public BlobInputStreamSupplier(final Blobstore blobstore, final long blobId) {
    this.blobstore = Objects.requireNonNull(blobstore, "The blobStore parameter cannot be null!");
    this.blobId = blobId;
  }

  @Override
  public InputStream getStream() {
    BlobReader readBlob = blobstore.readBlob(blobId);
    return new BlobInputStream(readBlob);
  }

}
