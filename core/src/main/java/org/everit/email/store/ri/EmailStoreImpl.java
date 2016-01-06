package org.everit.email.store.ri;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;

import org.everit.blobstore.BlobAccessor;
import org.everit.blobstore.Blobstore;
import org.everit.email.Email;
import org.everit.email.EmailAddress;
import org.everit.email.Recipients;
import org.everit.email.store.EmailStore;
import org.everit.email.store.ri.schema.qdsl.QAddress;
import org.everit.email.store.ri.schema.qdsl.QEmail;
import org.everit.email.store.ri.schema.qdsl.QEmailRecipient;
import org.everit.persistence.querydsl.support.QuerydslSupport;
import org.everit.transaction.propagator.TransactionPropagator;

import com.mysema.query.sql.dml.SQLInsertClause;

/**
 * Reference implementation of {@link EmailStore}.
 */
public class EmailStoreImpl implements EmailStore {

  private enum RecipientType {
    BCC, CC, FROM, TO
  }

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

  private long createAddress(final EmailAddress emailAddress, final long storedEmailId) {
    return querydslSupport.execute((connection, configuration) -> {
      QAddress qAddress = QAddress.address1;
      return new SQLInsertClause(connection, configuration, qAddress)
          .set(qAddress.address, emailAddress.address)
          .set(qAddress.personal, emailAddress.personal)
          .set(qAddress.storedEmailId, storedEmailId)
          .executeWithKey(qAddress.emailAddressId);
    });
  }

  private long createBlob(final byte[] contentBytes) {
    try (BlobAccessor blobAccessor = blobstore.createBlob()) {
      // Write the data
      blobAccessor.write(contentBytes, 0, contentBytes.length);
      return blobAccessor.getBlobId();
    }
  }

  private long createEmail(final String subject, final String htmlContent,
      final String textContent) {
    // FIXME check parameters?
    long htmlContentBlobId = createBlob(getBytes(htmlContent));
    long textContentBlobId = createBlob(getBytes(textContent));
    QEmail qEmail = QEmail.email;
    return querydslSupport.execute((connection, configuration) -> {
      return new SQLInsertClause(connection, configuration, qEmail)
          .set(qEmail.subject_, subject)
          .set(qEmail.htmlContentBlobId, htmlContentBlobId)
          .set(qEmail.textContentBlobId, textContentBlobId)
          .executeWithKey(qEmail.storedEmailId);
    });
  }

  private void createEmailRecipient(final EmailAddress from, final Recipients recipients,
      final long storedEmailId) {
    if (from != null) {
      saveRecipient(from, RecipientType.FROM, 0, storedEmailId);
    }
    if (recipients != null) {
      saveRecipients(recipients.to, RecipientType.TO, storedEmailId);
      saveRecipients(recipients.cc, RecipientType.CC, storedEmailId);
      saveRecipients(recipients.bcc, RecipientType.BCC, storedEmailId);
    }
  }

  @Override
  public void delete(final long storedEmailId) {
    // TODO Auto-generated method stub

  }

  private byte[] getBytes(final String content) {
    return content.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public Email read(final long storedEmailId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public long save(final Email email) {
    if (email == null) {
      // FIXME add java doc the parameter cannot be null, Create exception?
      throw new RuntimeException("cannot be null!");
    }

    transactionPropagator.required(() -> {
      long storedEmailId =
          createEmail(email.subject, email.htmlContent != null ? email.htmlContent.html : null,
              email.textContent);
      createEmailRecipient(email.from, email.recipients, storedEmailId);
    });
    return 0;
  }

  private void saveRecipient(final EmailAddress emailAddress, final RecipientType recipientType,
      final int index, final long storedEmailId) {
    long emailAddressId = createAddress(emailAddress, storedEmailId);

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

  private void saveRecipients(final Collection<EmailAddress> emailAddresses,
      final RecipientType recipientType, final long storedEmailId) {
    if (emailAddresses == null) {
      return;
    }

    int index = 0;
    for (EmailAddress emailAddress : emailAddresses) {
      saveRecipient(emailAddress, recipientType, index++, storedEmailId);
    }
  }

}
