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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.everit.blobstore.BlobAccessor;
import org.everit.blobstore.BlobReader;
import org.everit.blobstore.Blobstore;
import org.everit.email.Attachment;
import org.everit.email.Email;
import org.everit.email.EmailAddress;
import org.everit.email.HtmlContent;
import org.everit.email.Recipients;
import org.everit.email.store.EmailStore;
import org.everit.email.store.NonExistentEmailException;
import org.everit.email.store.ri.schema.qdsl.QAddress;
import org.everit.email.store.ri.schema.qdsl.QAttachment;
import org.everit.email.store.ri.schema.qdsl.QBinaryContent;
import org.everit.email.store.ri.schema.qdsl.QEmail;
import org.everit.email.store.ri.schema.qdsl.QEmailRecipient;
import org.everit.email.store.ri.schema.qdsl.QHtmlContent;
import org.everit.email.store.ri.schema.qdsl.QInlineImage;
import org.everit.email.store.ri.schema.qdsl.QTextContent;
import org.everit.persistence.querydsl.support.QuerydslSupport;
import org.everit.transaction.propagator.TransactionPropagator;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.dml.SQLDeleteClause;
import com.mysema.query.sql.dml.SQLInsertClause;
import com.mysema.query.types.Projections;

/**
 * Reference implementation of {@link EmailStore}.
 */
public class EmailStoreImpl implements EmailStore {

  /**
   * Types of recipient.
   */
  private enum RecipientType {
    BCC, CC, FROM, TO
  }

  private static final int BUFFER_SIZE = 1024;

  private static final int START_INDEX = 0;

  private final Blobstore blobstore;

  private final QuerydslSupport querydslSupport;

  private final TransactionPropagator transactionPropagator;

  /**
   * Simple constructor.
   *
   * @param querydslSupport
   *          a {@link QuerydslSupport} instance.
   * @param transactionPropagator
   *          a {@link TransactionPropagator} instance.
   * @param blobstore
   *          a {@link Blobstore} instance.
   */
  public EmailStoreImpl(final QuerydslSupport querydslSupport,
      final TransactionPropagator transactionPropagator, final Blobstore blobstore) {
    Objects.requireNonNull(querydslSupport, "The querydslSupport cannot be null!");
    Objects.requireNonNull(transactionPropagator, "The transactionPropagator cannot be null!");
    Objects.requireNonNull(blobstore, "The blobstore cannot be null!");

    this.querydslSupport = querydslSupport;
    this.transactionPropagator = transactionPropagator;
    this.blobstore = blobstore;
  }

  private Attachment createAttachment(final Long blobId, final String contentType,
      final String name) {
    BlobInputStreamSupplier inputStreamSupplier = null;
    if (blobId != null) {
      inputStreamSupplier = new BlobInputStreamSupplier(blobstore, blobId);
    }
    Attachment attachment = new Attachment()
        .withContentType(contentType)
        .withName(name)
        .withInputStreamSupplier(inputStreamSupplier);
    return attachment;
  }

  private Long createBlob(final byte[] contentBytes) {
    if (contentBytes.length < 1) {
      return null;
    }
    return createBlob(new ByteArrayInputStream(contentBytes));
  }

  private Long createBlob(final InputStream inputStream) {
    Objects.requireNonNull(inputStream, "inputStream cannot be null");
    try (BlobAccessor blobAccessor = blobstore.createBlob()) {
      int nRead;
      byte[] data = new byte[BUFFER_SIZE];
      while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
        blobAccessor.write(data, 0, nRead);
      }
      return blobAccessor.getBlobId();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void delete(final long storedEmailId) {
    transactionPropagator.required(() -> {
      lockEmailForUpdate(storedEmailId);

      deleteRecipients(storedEmailId);
      deleteAttachments(storedEmailId);
      deleteHtmlContent(storedEmailId);
      deleteTextContent(storedEmailId);
      deleteEmail(storedEmailId);
    });
  }

  private void deleteAttachments(final long storedEmailId) {
    QAttachment qAttachment = QAttachment.attachment;
    List<Long> binaryContentIds = querydslSupport.execute((connection, configuration) -> {
      return new SQLQuery(connection, configuration)
          .from(qAttachment)
          .where(qAttachment.storedEmailId.eq(storedEmailId))
          .list(qAttachment.binaryContentId);
    });

    querydslSupport.execute((connection, configuration) -> {
      return new SQLDeleteClause(connection, configuration, qAttachment)
          .where(qAttachment.storedEmailId.eq(storedEmailId))
          .execute();
    });

    deleteBinaryContents(binaryContentIds);
  }

  private void deleteBinaryContents(final List<Long> binaryContentIds) {
    if (binaryContentIds.isEmpty()) {
      return;
    }

    QBinaryContent qBinaryContent = QBinaryContent.binaryContent;
    List<Long> blobIds = querydslSupport.execute((connection, configuration) -> {
      return new SQLQuery(connection, configuration)
          .from(qBinaryContent)
          .where(qBinaryContent.binaryContentId.in(binaryContentIds))
          .list(qBinaryContent.blobId);
    });

    querydslSupport.execute((connection, configuration) -> {
      return new SQLDeleteClause(connection, configuration, qBinaryContent)
          .where(qBinaryContent.binaryContentId.in(binaryContentIds))
          .execute();
    });

    deleteBlobs(blobIds);
  }

  private void deleteBlob(final Long blobId) {
    if (blobId == null) {
      return;
    }

    blobstore.deleteBlob(blobId);
  }

  private void deleteBlobs(final List<Long> blobIds) {
    for (Long blobId : blobIds) {
      deleteBlob(blobId);
    }
  }

  private void deleteEmail(final long storedEmailId) {
    querydslSupport.execute((connection, configuration) -> {
      QEmail qEmail = QEmail.email;
      return new SQLDeleteClause(connection, configuration, qEmail)
          .where(qEmail.storedEmailId.eq(storedEmailId))
          .execute();
    });
  }

  private void deleteEmailAddresses(final List<Long> emailAddressIds) {
    querydslSupport.execute((connection, configuration) -> {
      QAddress qAddress = QAddress.address1;
      return new SQLDeleteClause(connection, configuration, qAddress)
          .where(qAddress.emailAddressId.in(emailAddressIds))
          .execute();
    });
  }

  private void deleteHtmlContent(final long storedEmailId) {
    QHtmlContent qHtmlContent = QHtmlContent.htmlContent;
    Tuple tuple = querydslSupport.execute((connection, configuration) -> {
      return new SQLQuery(connection, configuration)
          .from(qHtmlContent)
          .where(qHtmlContent.storedEmailId.eq(storedEmailId))
          .uniqueResult(qHtmlContent.htmlContentId,
              qHtmlContent.blobId);
    });

    if (tuple == null) {
      return;
    }

    Long htmlContentId = tuple.get(qHtmlContent.htmlContentId);
    deleteInlineImages(htmlContentId);

    querydslSupport.execute((connection, configuration) -> {
      return new SQLDeleteClause(connection, configuration, qHtmlContent)
          .where(qHtmlContent.htmlContentId.eq(htmlContentId))
          .execute();
    });

    deleteBlob(tuple.get(qHtmlContent.blobId));
  }

  private void deleteInlineImages(final long htmlContentId) {
    QInlineImage qInlineImage = QInlineImage.inlineImage;
    List<Long> binaryContentIds = querydslSupport.execute((connection, configuration) -> {
      return new SQLQuery(connection, configuration)
          .from(qInlineImage)
          .where(qInlineImage.htmlContentId.eq(htmlContentId))
          .list(qInlineImage.binaryContentId);
    });

    querydslSupport.execute((connection, configuration) -> {
      return new SQLDeleteClause(connection, configuration, qInlineImage)
          .where(qInlineImage.htmlContentId.eq(htmlContentId))
          .execute();
    });

    deleteBinaryContents(binaryContentIds);
  }

  private void deleteRecipients(final long storedEmailId) {
    QEmailRecipient qEmailRecipient = QEmailRecipient.emailRecipient;
    List<Long> emailAddressIds = querydslSupport.execute((connection, configuration) -> {
      return new SQLQuery(connection, configuration)
          .from(qEmailRecipient)
          .where(qEmailRecipient.storedEmailId.eq(storedEmailId))
          .list(qEmailRecipient.emailAddressId);
    });

    if (emailAddressIds.isEmpty()) {
      return;
    }

    querydslSupport.execute((connection, configuration) -> {
      return new SQLDeleteClause(connection, configuration, qEmailRecipient)
          .where(qEmailRecipient.storedEmailId.eq(storedEmailId))
          .execute();
    });

    deleteEmailAddresses(emailAddressIds);
  }

  private void deleteTextContent(final long storedEmailId) {
    QTextContent qTextContent = QTextContent.textContent;
    Tuple tuple = querydslSupport.execute((connection, configuration) -> {
      return new SQLQuery(connection, configuration)
          .from(qTextContent)
          .where(qTextContent.storedEmailId.eq(storedEmailId))
          .uniqueResult(qTextContent.textContentId,
              qTextContent.blobId);
    });

    if (tuple == null) {
      return;
    }

    Long textContentId = tuple.get(qTextContent.textContentId);
    querydslSupport.execute((connection, configuration) -> {
      return new SQLDeleteClause(connection, configuration, qTextContent)
          .where(qTextContent.textContentId.eq(textContentId))
          .execute();
    });

    deleteBlob(tuple.get(qTextContent.blobId));
  }

  private byte[] getBytes(final String content) {
    if (content == null) {
      return new byte[0];
    }
    return content.getBytes(StandardCharsets.UTF_8);
  }

  private long insertAddress(final EmailAddress emailAddress) {
    return querydslSupport.execute((connection, configuration) -> {
      QAddress qAddress = QAddress.address1;
      return new SQLInsertClause(connection, configuration, qAddress)
          .set(qAddress.address, emailAddress.address)
          .set(qAddress.personal, emailAddress.personal)
          .executeWithKey(qAddress.emailAddressId);
    });
  }

  private void insertAttachments(final Collection<Attachment> attachments,
      final long storedEmailId) {
    if (attachments == null) {
      throw new IllegalArgumentException("attachments collection cannot be null");
    }

    querydslSupport.execute((connection, configuration) -> {
      int index = START_INDEX;
      for (Attachment attachment : attachments) {
        Long binaryContentId = insertBinaryContent(attachment);
        QAttachment qAttachment = QAttachment.attachment;
        new SQLInsertClause(connection, configuration, qAttachment)
            .set(qAttachment.binaryContentId, binaryContentId)
            .set(qAttachment.index_, index++)
            .set(qAttachment.storedEmailId, storedEmailId)
            .executeWithKey(qAttachment.attachmentId);
      }
      return null;
    });
  }

  private Long insertBinaryContent(final Attachment attachment) {
    if (attachment == null) {
      return null;
    }

    return querydslSupport.execute((connection, configuration) -> {
      Long attachmentBlobId = null;
      if (attachment.inputStreamSupplier != null) {
        attachmentBlobId = createBlob(attachment.inputStreamSupplier.getStream());
      }

      QBinaryContent qBinaryContent = QBinaryContent.binaryContent;
      return new SQLInsertClause(connection, configuration, qBinaryContent)
          .set(qBinaryContent.name_, attachment.name)
          .set(qBinaryContent.contentType_, attachment.contentType)
          .set(qBinaryContent.blobId, attachmentBlobId)
          .executeWithKey(qBinaryContent.binaryContentId);
    });
  }

  private long insertEmail(final String subject) {
    return querydslSupport.execute((connection, configuration) -> {
      QEmail qEmail = QEmail.email;
      return new SQLInsertClause(connection, configuration, qEmail)
          .set(qEmail.subject_, subject)
          .executeWithKey(qEmail.storedEmailId);
    });
  }

  private void insertHtmlContent(final HtmlContent htmlContent, final long storedEmailId) {
    if (htmlContent == null) {
      return;
    }

    Long htmlContentBlobId = createBlob(getBytes(htmlContent.html));
    Long htmlContentId = querydslSupport.execute((connection, configuration) -> {
      QHtmlContent qHtmlContent = QHtmlContent.htmlContent;
      return new SQLInsertClause(connection, configuration, qHtmlContent)
          .set(qHtmlContent.blobId, htmlContentBlobId)
          .set(qHtmlContent.storedEmailId, storedEmailId)
          .executeWithKey(qHtmlContent.htmlContentId);
    });
    insertInlineImages(htmlContent.inlineImageByCidMap, htmlContentId);
  }

  private void insertInlineImages(final Map<String, Attachment> inlineImageByCidMap,
      final long htmlContentId) {
    if (inlineImageByCidMap == null) {
      throw new IllegalArgumentException("inlineImageByCidMap cannot be null");
    }

    querydslSupport.execute((connection, configuration) -> {
      int index = START_INDEX;
      for (Map.Entry<String, Attachment> entry : inlineImageByCidMap.entrySet()) {
        Long binaryContentId = insertBinaryContent(entry.getValue());
        QInlineImage qInlineImage = QInlineImage.inlineImage;
        new SQLInsertClause(connection, configuration, qInlineImage)
            .set(qInlineImage.cid_, entry.getKey())
            .set(qInlineImage.binaryContentId, binaryContentId)
            .set(qInlineImage.index_, index++)
            .set(qInlineImage.htmlContentId, htmlContentId)
            .executeWithKey(qInlineImage.inlineImageId);
      }
      return null;
    });
  }

  private void insertRecipient(final EmailAddress emailAddress, final RecipientType recipientType,
      final int index, final long storedEmailId) {
    long emailAddressId = insertAddress(emailAddress);

    querydslSupport.execute((connection, configuration) -> {
      QEmailRecipient qEmailRecipient = QEmailRecipient.emailRecipient;
      return new SQLInsertClause(connection, configuration, qEmailRecipient)
          .set(qEmailRecipient.emailAddressId, emailAddressId)
          .set(qEmailRecipient.index_, index)
          .set(qEmailRecipient.storedEmailId, storedEmailId)
          .set(qEmailRecipient.recipientType, recipientType.name())
          .executeWithKey(qEmailRecipient.emailRecipientId);
    });
  }

  private void insertRecipients(final Collection<EmailAddress> emailAddresses,
      final RecipientType recipientType, final long storedEmailId) {
    if (emailAddresses == null) {
      throw new IllegalArgumentException(
          "Recipient." + recipientType + " collection cannot be null");
    }

    int index = START_INDEX;
    for (EmailAddress emailAddress : emailAddresses) {
      insertRecipient(emailAddress, recipientType, index++, storedEmailId);
    }
  }

  private void insertTextContent(final String textContent, final long storedEmailId) {
    if (textContent == null) {
      return;
    }

    Long textContentBlobId = createBlob(getBytes(textContent));
    querydslSupport.execute((connection, configuration) -> {
      QTextContent qTextContent = QTextContent.textContent;
      return new SQLInsertClause(connection, configuration, qTextContent)
          .set(qTextContent.storedEmailId, storedEmailId)
          .set(qTextContent.blobId, textContentBlobId)
          .executeWithKey(qTextContent.textContentId);
    });
  }

  private void lockEmailForUpdate(final long storedEmailId) {
    Boolean exists = querydslSupport.execute((connection, configuration) -> {
      QEmail qEmail = QEmail.email;
      return new SQLQuery(connection, configuration)
          .from(qEmail)
          .where(qEmail.storedEmailId.eq(storedEmailId))
          .forUpdate()
          .exists();
    });
    if (!exists) {
      throw new NonExistentEmailException(
          "Stored email not exists [storeEmailId: " + storedEmailId + "]");
    }
  }

  @Override
  public Email read(final long storedEmailId) {
    return transactionPropagator.required(() -> {
      QEmail qEmail = QEmail.email;
      Tuple emailTuple = querydslSupport.execute((connection, configuration) -> {
        return new SQLQuery(connection, configuration)
            .from(qEmail)
            .where(qEmail.storedEmailId.eq(storedEmailId))
            .uniqueResult(qEmail.subject_,
                qEmail.storedEmailId);
      });

      if (emailTuple == null) {
        return null;
      }

      Email email = new Email()
          .withSubject(emailTuple.get(qEmail.subject_));

      email.withTextContent(readTextContent(storedEmailId));

      HtmlContent htmlContent = readHtmlContent(storedEmailId);
      email = email.withHtmlContent(htmlContent);

      Collection<Attachment> attachments = readAttachments(storedEmailId);
      email = email.withAttachments(attachments);

      EmailAddress from = readFrom(storedEmailId);
      email = email.withFrom(from);

      Recipients recipients = readRecipients(storedEmailId);
      email.withRecipients(recipients);

      return email;
    });
  }

  private Collection<Attachment> readAttachments(final long storedEmailId) {
    QAttachment qAttachment = QAttachment.attachment;
    QBinaryContent qBinaryContent = QBinaryContent.binaryContent;
    List<Tuple> attachmentTuples = querydslSupport.execute((connection, configuration) -> {
      return new SQLQuery(connection, configuration)
          .from(qAttachment)
          .join(qBinaryContent).on(qAttachment.binaryContentId.eq(qBinaryContent.binaryContentId))
          .where(qAttachment.storedEmailId.eq(storedEmailId))
          .orderBy(qAttachment.index_.asc())
          .list(qBinaryContent.blobId,
              qBinaryContent.contentType_,
              qBinaryContent.name_);
    });

    Collection<Attachment> attachments = new ArrayList<>();
    for (Tuple attachmentTuple : attachmentTuples) {
      String contentType = attachmentTuple.get(qBinaryContent.contentType_);
      String name = attachmentTuple.get(qBinaryContent.name_);
      Long blobId = attachmentTuple.get(qBinaryContent.blobId);
      Attachment attachment = createAttachment(blobId, contentType, name);

      attachments.add(attachment);
    }
    return attachments;
  }

  private byte[] readBlob(final Long blobId) {
    if (blobId == null) {
      return new byte[0];
    }

    try (BlobReader readBlob = blobstore.readBlob(blobId)) {
      int blobSize = (int) readBlob.getSize();
      byte[] result = new byte[blobSize];
      readBlob.read(result, 0, blobSize);
      return result;
    }
  }

  private List<EmailAddress> readEmailAddress(final long storedEmailId,
      final RecipientType recipientType) {
    return querydslSupport.execute((connection, configuration) -> {
      QAddress qAddress = QAddress.address1;
      QEmailRecipient qEmailRecipient = QEmailRecipient.emailRecipient;
      return new SQLQuery(connection, configuration)
          .from(qAddress)
          .join(qEmailRecipient).on(qEmailRecipient.emailAddressId.eq(qAddress.emailAddressId))
          .where(qEmailRecipient.storedEmailId.eq(storedEmailId)
              .and(qEmailRecipient.recipientType.eq(recipientType.name())))
          .orderBy(qEmailRecipient.index_.asc())
          .list(Projections.fields(EmailAddress.class,
              qAddress.personal,
              qAddress.address));
    });
  }

  private EmailAddress readFrom(final long storedEmailId) {
    List<EmailAddress> fromAddress = readEmailAddress(storedEmailId, RecipientType.FROM);
    return fromAddress.isEmpty() ? null : fromAddress.get(0);
  }

  private HtmlContent readHtmlContent(final long storedEmailId) {
    QHtmlContent qHtmlContent = QHtmlContent.htmlContent;
    Tuple htmlContentTuple = querydslSupport.execute((connection, configuration) -> {
      return new SQLQuery(connection, configuration)
          .from(qHtmlContent)
          .where(qHtmlContent.storedEmailId.eq(storedEmailId))
          .uniqueResult(qHtmlContent.blobId,
              qHtmlContent.htmlContentId);
    });

    if (htmlContentTuple == null) {
      return null;
    }

    HtmlContent htmlContent = new HtmlContent();
    byte[] htmlContentBytes = readBlob(htmlContentTuple.get(qHtmlContent.blobId));
    if (htmlContentBytes.length > 0) {
      htmlContent.withHtml(new String(htmlContentBytes, StandardCharsets.UTF_8));
    }
    Map<String, Attachment> inlineImageByCidMap =
        readInlineImages(htmlContentTuple.get(qHtmlContent.htmlContentId));
    if (!inlineImageByCidMap.isEmpty()) {
      htmlContent.withInlineImageByCidMap(inlineImageByCidMap);
    }
    return htmlContent;
  }

  private Map<String, Attachment> readInlineImages(final long htmlContentId) {
    QBinaryContent qBinaryContent = QBinaryContent.binaryContent;
    QInlineImage qInlineImage = QInlineImage.inlineImage;
    List<Tuple> inlineImageTuples = querydslSupport.execute((connection, configuration) -> {
      return new SQLQuery(connection, configuration)
          .from(qInlineImage)
          .leftJoin(qBinaryContent)
          .on(qBinaryContent.binaryContentId.eq(qInlineImage.binaryContentId))
          .where(qInlineImage.htmlContentId.eq(htmlContentId))
          .orderBy(qInlineImage.index_.asc())
          .list(qBinaryContent.blobId,
              qBinaryContent.contentType_,
              qBinaryContent.name_,
              qInlineImage.cid_);
    });

    Map<String, Attachment> inlineImageByCidMap = new LinkedHashMap<>();
    for (Tuple inlineImageTuple : inlineImageTuples) {
      String contentType = inlineImageTuple.get(qBinaryContent.contentType_);
      String name = inlineImageTuple.get(qBinaryContent.name_);
      Long blobId = inlineImageTuple.get(qBinaryContent.blobId);
      Attachment attachment = createAttachment(blobId, contentType, name);

      inlineImageByCidMap.put(inlineImageTuple.get(qInlineImage.cid_), attachment);
    }
    return inlineImageByCidMap;
  }

  private Recipients readRecipients(final long storedEmailId) {
    Recipients recipients = new Recipients();
    List<EmailAddress> to = readEmailAddress(storedEmailId, RecipientType.TO);
    if (!to.isEmpty()) {
      recipients = recipients.withTo(to);
    }

    List<EmailAddress> cc = readEmailAddress(storedEmailId, RecipientType.CC);
    if (!cc.isEmpty()) {
      recipients = recipients.withCc(cc);
    }

    List<EmailAddress> bcc = readEmailAddress(storedEmailId, RecipientType.BCC);
    if (!bcc.isEmpty()) {
      recipients = recipients.withBcc(bcc);
    }

    return recipients;
  }

  private String readTextContent(final long storedEmailId) {
    Long textContentBlobId = querydslSupport.execute((connection, configuration) -> {
      QTextContent qTextContent = QTextContent.textContent;
      return new SQLQuery(connection, configuration)
          .from(qTextContent)
          .where(qTextContent.storedEmailId.eq(storedEmailId))
          .uniqueResult(qTextContent.blobId);
    });

    byte[] textContentBytes = readBlob(textContentBlobId);
    if (textContentBytes.length > 0) {
      return new String(textContentBytes, StandardCharsets.UTF_8);
    }
    return null;
  }

  @Override
  public long save(final Email email) {
    if (email == null) {
      throw new NullPointerException("The email cannot be null!");
    }

    return transactionPropagator.required(() -> {
      long storedEmailId = insertEmail(email.subject);
      insertTextContent(email.textContent, storedEmailId);
      insertHtmlContent(email.htmlContent, storedEmailId);
      insertAttachments(email.attachments, storedEmailId);
      saveEmailRecipients(email.from, email.recipients, storedEmailId);
      return storedEmailId;
    });
  }

  private void saveEmailRecipients(final EmailAddress from, final Recipients recipients,
      final long storedEmailId) {
    if (from != null) {
      insertRecipient(from, RecipientType.FROM, START_INDEX, storedEmailId);
    }
    if (recipients != null) {
      insertRecipients(recipients.to, RecipientType.TO, storedEmailId);
      insertRecipients(recipients.cc, RecipientType.CC, storedEmailId);
      insertRecipients(recipients.bcc, RecipientType.BCC, storedEmailId);
    }
  }

}
