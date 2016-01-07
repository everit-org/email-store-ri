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
import java.io.ByteArrayOutputStream;
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
import org.everit.email.InputStreamSupplier;
import org.everit.email.Recipients;
import org.everit.email.store.EmailStore;
import org.everit.email.store.ri.schema.qdsl.QAddress;
import org.everit.email.store.ri.schema.qdsl.QAttachment;
import org.everit.email.store.ri.schema.qdsl.QBinaryContent;
import org.everit.email.store.ri.schema.qdsl.QEmail;
import org.everit.email.store.ri.schema.qdsl.QEmailRecipient;
import org.everit.email.store.ri.schema.qdsl.QInlineImage;
import org.everit.persistence.querydsl.support.QuerydslSupport;
import org.everit.transaction.propagator.TransactionPropagator;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.dml.SQLInsertClause;
import com.mysema.query.types.Projections;

/**
 * Reference implementation of {@link EmailStore}.
 */
public class EmailStoreImpl implements EmailStore {

  private static class InputStreamSupplierImpl implements InputStreamSupplier {

    private final byte[] content;

    InputStreamSupplierImpl(final byte[] content) {
      this.content = content;
    }

    @Override
    public InputStream getStream() {
      return new ByteArrayInputStream(content);
    }

  }

  private enum RecipientType {
    BCC, CC, FROM, TO
  }

  private static final int BUFFER_SIZE = 1024;

  private static final int START_INDEX = 0;

  private final Blobstore blobstore;

  private final QuerydslSupport querydslSupport;

  private final TransactionPropagator transactionPropagator;

  public EmailStoreImpl(final QuerydslSupport querydslSupport,
      final TransactionPropagator transactionPropagator, final Blobstore blobstore) {
    Objects.requireNonNull(querydslSupport, "The querydslSupport cannot be null!");
    Objects.requireNonNull(transactionPropagator, "The transactionPropagator cannot be null!");
    Objects.requireNonNull(blobstore, "The blobstore cannot be null!");

    this.querydslSupport = querydslSupport;
    this.transactionPropagator = transactionPropagator;
    this.blobstore = blobstore;
  }

  private long createBlob(final byte[] contentBytes) {
    try (BlobAccessor blobAccessor = blobstore.createBlob()) {
      // Write the data
      blobAccessor.write(contentBytes, 0, contentBytes.length);
      return blobAccessor.getBlobId();
    }
  }

  @Override
  public void delete(final long storedEmailId) {
    // TODO Auto-generated method stub

  }

  private byte[] getBytes(final String content) {
    return content.getBytes(StandardCharsets.UTF_8);
  }

  private long insertAddress(final EmailAddress emailAddress, final long storedEmailId) {
    return querydslSupport.execute((connection, configuration) -> {
      QAddress qAddress = QAddress.address1;
      return new SQLInsertClause(connection, configuration, qAddress)
          .set(qAddress.address, emailAddress.address)
          .set(qAddress.personal, emailAddress.personal)
          .set(qAddress.storedEmailId, storedEmailId)
          .executeWithKey(qAddress.emailAddressId);
    });
  }

  private void insertAttachments(final Collection<Attachment> attachments,
      final long storedEmailId) {
    if (attachments == null) {
      return;
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
        try (InputStream is = attachment.inputStreamSupplier.getStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
          int nRead;
          byte[] data = new byte[BUFFER_SIZE];
          while ((nRead = is.read(data, 0, data.length)) != -1) {
            baos.write(data, 0, nRead);
          }
          baos.flush();
          byte[] attachmentBytes = baos.toByteArray();
          attachmentBlobId = createBlob(attachmentBytes);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }

      QBinaryContent qBinaryContent = QBinaryContent.binaryContent;
      return new SQLInsertClause(connection, configuration, qBinaryContent)
          .set(qBinaryContent.name_, attachment.name)
          .set(qBinaryContent.contentType_, attachment.contentType)
          .set(qBinaryContent.blobId, attachmentBlobId)
          .executeWithKey(qBinaryContent.binaryContentId);
    });
  }

  private long insertEmail(final String subject, final String htmlContent,
      final String textContent) {
    return querydslSupport.execute((connection, configuration) -> {
      QEmail qEmail = QEmail.email;
      SQLInsertClause insertClause = new SQLInsertClause(connection, configuration, qEmail)
          .set(qEmail.subject_, subject);

      if (htmlContent != null) {
        long htmlContentBlobId = createBlob(getBytes(htmlContent));
        insertClause = insertClause.set(qEmail.htmlContentBlobId, htmlContentBlobId);
      }

      if (textContent != null) {
        long textContentBlobId = createBlob(getBytes(textContent));
        insertClause = insertClause.set(qEmail.textContentBlobId, textContentBlobId);
      }

      return insertClause.executeWithKey(qEmail.storedEmailId);
    });
  }

  private void insertInlineImages(final HtmlContent htmlContent, final long storedEmailId) {
    if ((htmlContent == null) || (htmlContent.inlineImageByCidMap == null)) {
      return;
    }

    querydslSupport.execute((connection, configuration) -> {
      int index = START_INDEX;
      for (Map.Entry<String, Attachment> entry : htmlContent.inlineImageByCidMap.entrySet()) {
        Long binaryContentId = insertBinaryContent(entry.getValue());
        QInlineImage qInlineImage = QInlineImage.inlineImage;
        new SQLInsertClause(connection, configuration, qInlineImage)
            .set(qInlineImage.cid_, entry.getKey())
            .set(qInlineImage.storedEmailId, storedEmailId)
            .set(qInlineImage.binaryContentId, binaryContentId)
            .set(qInlineImage.index_, index++);
      }
      return null;
    });
  }

  private void insertRecipient(final EmailAddress emailAddress, final RecipientType recipientType,
      final int index, final long storedEmailId) {
    long emailAddressId = insertAddress(emailAddress, storedEmailId);

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
      return;
    }

    int index = START_INDEX;
    for (EmailAddress emailAddress : emailAddresses) {
      insertRecipient(emailAddress, recipientType, index++, storedEmailId);
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
            .uniqueResult(qEmail.htmlContentBlobId,
                qEmail.subject_,
                qEmail.textContentBlobId);
      });

      if (emailTuple == null) {
        return null;
      }

      Email email = new Email()
          .withSubject(emailTuple.get(qEmail.subject_));

      byte[] textContentBytes = readBlob(emailTuple.get(qEmail.textContentBlobId));
      if (textContentBytes != null) {
        email = email.withTextContent(new String(textContentBytes, StandardCharsets.UTF_8));
      }
      HtmlContent htmlContent = new HtmlContent();
      byte[] htmlContentBytes = readBlob(emailTuple.get(qEmail.htmlContentBlobId));
      if (htmlContentBytes != null) {
        htmlContent.withHtml(new String(htmlContentBytes, StandardCharsets.UTF_8));
      }
      Map<String, Attachment> inlineImageByCidMap = readInlineImages(storedEmailId);
      htmlContent.withInlineImageByCidMap(inlineImageByCidMap);

      email = email.withHtmlContent(htmlContent);

      Collection<Attachment> attachments = readAttachments(storedEmailId);

      email = email.withAttachments(attachments);

      List<EmailAddress> fromAddress = readEmailAddress(storedEmailId, RecipientType.FROM);
      email = email.withFrom(fromAddress.isEmpty() ? null : fromAddress.get(0));

      Recipients recipients = new Recipients();
      List<EmailAddress> to = readEmailAddress(storedEmailId, RecipientType.TO);
      recipients = recipients.withTo(to);

      List<EmailAddress> cc = readEmailAddress(storedEmailId, RecipientType.CC);
      recipients = recipients.withCc(cc);

      List<EmailAddress> bcc = readEmailAddress(storedEmailId, RecipientType.BCC);
      recipients = recipients.withBcc(bcc);

      email = email.withRecipients(recipients);

      return email;
    });
    // return querydslSupport.execute((connection, configuration) -> {
    // QEmail qEmail = QEmail.email;
    // return new SQLQuery(connection, configuration)
    // .from(qEmail)
    // .where(qEmail.storedEmailId.eq(storedEmailId))
    // .uniqueResult(
    // Projections.fields(Email.class,
    // qEmail.subject_.as("subject"),
    // qEmail.));
    // });
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
      Long blobId = attachmentTuple.get(qBinaryContent.blobId);
      byte[] attachmentContentBytes = readBlob(blobId);
      Attachment attachment = new Attachment()
          .withContentType(attachmentTuple.get(qBinaryContent.contentType_))
          .withName(attachmentTuple.get(qBinaryContent.name_))
          .withInputStreamSupplier(new InputStreamSupplierImpl(attachmentContentBytes));

      attachments.add(attachment);
    }
    return attachments;
  }

  private byte[] readBlob(final Long blobId) {
    if (blobId == null) {
      return null;
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

  private Map<String, Attachment> readInlineImages(final long storedEmailId) {
    QInlineImage qInlineImage = QInlineImage.inlineImage;
    QBinaryContent qBinaryContent = QBinaryContent.binaryContent;
    List<Tuple> inlineImageTuples = querydslSupport.execute((connection, configuration) -> {
      return new SQLQuery(connection, configuration)
          .from(qInlineImage)
          .join(qBinaryContent).on(qInlineImage.binaryContentId.eq(qBinaryContent.binaryContentId))
          .where(qInlineImage.storedEmailId.eq(storedEmailId))
          .orderBy(qInlineImage.index_.asc())
          .list(qBinaryContent.blobId,
              qBinaryContent.contentType_,
              qBinaryContent.name_,
              qInlineImage.cid_);
    });

    Map<String, Attachment> inlineImageByCidMap = new LinkedHashMap<>();
    for (Tuple inlineImageTuple : inlineImageTuples) {
      Long blobId = inlineImageTuple.get(qBinaryContent.blobId);
      byte[] attachmentContentBytes = readBlob(blobId);
      Attachment attachment = new Attachment()
          .withContentType(inlineImageTuple.get(qBinaryContent.contentType_))
          .withName(inlineImageTuple.get(qBinaryContent.name_))
          .withInputStreamSupplier(new InputStreamSupplierImpl(attachmentContentBytes));

      inlineImageByCidMap.put(inlineImageTuple.get(qInlineImage.cid_), attachment);
    }
    return inlineImageByCidMap;
  }

  @Override
  public long save(final Email email) {
    if (email == null) {
      // FIXME add java doc the parameter cannot be null, Create exception?
      throw new RuntimeException("The email cannot be null!");
    }

    return transactionPropagator.required(() -> {
      long storedEmailId = insertEmail(email.subject,
          email.htmlContent != null ? email.htmlContent.html : null,
          email.textContent);
      saveEmailRecipients(email.from, email.recipients, storedEmailId);
      insertInlineImages(email.htmlContent, storedEmailId);
      insertAttachments(email.attachments, storedEmailId);
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
