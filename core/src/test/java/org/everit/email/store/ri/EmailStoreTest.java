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
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.sql.XADataSource;
import javax.transaction.xa.XAException;

import org.apache.commons.dbcp2.managed.BasicManagedDataSource;
import org.apache.geronimo.transaction.manager.GeronimoTransactionManager;
import org.everit.blobstore.Blobstore;
import org.everit.blobstore.mem.MemBlobstore;
import org.everit.email.Attachment;
import org.everit.email.Email;
import org.everit.email.EmailAddress;
import org.everit.email.HtmlContent;
import org.everit.email.InputStreamSupplier;
import org.everit.email.Recipients;
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
import org.everit.persistence.querydsl.support.ri.QuerydslSupportImpl;
import org.everit.transaction.propagator.TransactionPropagator;
import org.everit.transaction.propagator.jta.JTATransactionPropagator;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.mysema.query.sql.Configuration;
import com.mysema.query.sql.H2Templates;
import com.mysema.query.sql.RelationalPathBase;
import com.mysema.query.sql.SQLQuery;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

public class EmailStoreTest {

  private static class DummyInputStreamSupplier implements InputStreamSupplier {

    private final ClassLoader classLoader;

    private final String resourceName;

    DummyInputStreamSupplier(final ClassLoader classLoader, final String resourceName) {
      this.resourceName = resourceName;
      this.classLoader = classLoader;
    }

    @Override
    public InputStream getStream() {
      if (resourceName == null) {
        return null;
      }
      return classLoader.getResourceAsStream(resourceName);
    }
  }

  private static final String DEFAULT_BCC_ADDRESS = "test-address-bcc";

  private static final String DEFAULT_BCC_PERSON = "test-person-bcc";

  private static final String DEFAULT_CC_ADDRESS = "test-address-cc";

  private static final String DEFAULT_CC_PERSON = "test-person-cc";

  private static final String DEFAULT_CID = "test-cid";

  private static final String DEFAULT_CONTENT_TYPE_IMAGE = "image";

  private static final String DEFAULT_CONTENT_TYPE_TXT = "txt";

  private static final String DEFAULT_FROM_ADDRESS = "test-address-from";

  private static final String DEFAULT_FROM_PERSON = "test-person-from";

  private static final String DEFAULT_HTML = "test-html";

  private static final String DEFAULT_NAME_SAMPLE_IMG_NAME = "test-sample-img-name";

  private static final String DEFAULT_NAME_SAMPLE_TXT_NAME = "test-sample-txt-name";

  private static final String DEFAULT_SUBJECT = "test-subject";

  private static final String DEFAULT_TEXT_CONTENT = "test-text-content";

  private static final String DEFAULT_TO_ADDRESS = "test-address-to";

  private static final String DEFAULT_TO_PERSON = "test-person-to";

  private Blobstore blobstore;

  private EmailStoreImpl emailStore;

  private BasicManagedDataSource managedDataSource = null;

  private QuerydslSupport querydslSupport;

  private TransactionPropagator transactionPropagator;

  @After
  public void after() {
    if (managedDataSource != null) {
      try {
        managedDataSource.close();
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void assertTableRows(final long expectedEmailCount, final long expectedAttachmentCount,
      final long expectedTextContentCount, final long expectedHTMLContentCount,
      final long expectedInlineImageCount, final long expectedEmailRecipientCount,
      final long expectedAddressCount, final long expectedBinaryContentCount) {
    long emailCount = selectTableCount(QEmail.email);
    Assert.assertEquals(expectedEmailCount, emailCount);

    long attachmentCount = selectTableCount(QAttachment.attachment);
    Assert.assertEquals(expectedAttachmentCount, attachmentCount);

    long textContentCount = selectTableCount(QTextContent.textContent);
    Assert.assertEquals(expectedTextContentCount, textContentCount);

    long htmlContentCount = selectTableCount(QHtmlContent.htmlContent);
    Assert.assertEquals(expectedHTMLContentCount, htmlContentCount);

    long inlineImageCount = selectTableCount(QInlineImage.inlineImage);
    Assert.assertEquals(expectedInlineImageCount, inlineImageCount);

    long emailRecipientCount = selectTableCount(QEmailRecipient.emailRecipient);
    Assert.assertEquals(expectedEmailRecipientCount, emailRecipientCount);

    long addressCount = selectTableCount(QAddress.address1);
    Assert.assertEquals(expectedAddressCount, addressCount);

    long binaryContentCount = selectTableCount(QBinaryContent.binaryContent);
    Assert.assertEquals(expectedBinaryContentCount, binaryContentCount);
  }

  @Before
  public void before() {
    GeronimoTransactionManager transactionManager = null;
    try {
      transactionManager = new GeronimoTransactionManager(6000);
    } catch (XAException e) {
      throw new RuntimeException(e);
    }
    managedDataSource = createManagedDataSource(transactionManager, createXADatasource());

    try (Connection connection = managedDataSource.getConnection()) {
      DatabaseConnection databaseConnection = new JdbcConnection(connection);

      Liquibase liquibase = new Liquibase("META-INF/liquibase/email.store.ri.liquibase.xml",
          new ClassLoaderResourceAccessor(), databaseConnection);

      liquibase.update((Contexts) null);
    } catch (LiquibaseException | SQLException e) {
      try {
        managedDataSource.close();
      } catch (SQLException e1) {
        e.addSuppressed(e1);
      }
      throw new RuntimeException(e);
    }

    blobstore = new MemBlobstore(transactionManager);

    transactionPropagator = new JTATransactionPropagator(transactionManager);

    querydslSupport =
        new QuerydslSupportImpl(new Configuration(H2Templates.DEFAULT), managedDataSource);
    emailStore = new EmailStoreImpl(querydslSupport, transactionPropagator, blobstore);
  }

  private Attachment createAttachment(final String contentType, final String name,
      final String resourceName) {
    return new Attachment()
        .withContentType(contentType)
        .withName(name)
        .withInputStreamSupplier(
            new DummyInputStreamSupplier(getClass().getClassLoader(), resourceName));
  }

  private EmailAddress createEmailAddress(final String address, final String person) {
    return new EmailAddress()
        .withAddress(address)
        .withPersonal(person);
  }

  protected BasicManagedDataSource createManagedDataSource(
      final GeronimoTransactionManager transactionManager, final XADataSource xaDataSource) {
    BasicManagedDataSource lManagedDataSource = new BasicManagedDataSource();
    lManagedDataSource.setTransactionManager(transactionManager);
    lManagedDataSource.setXaDataSourceInstance(xaDataSource);
    return lManagedDataSource;
  }

  private XADataSource createXADatasource() {
    JdbcDataSource xaDatasource = new JdbcDataSource();
    xaDatasource.setURL("jdbc:h2:mem:test");
    // xaDatasource.setURL("jdbc:h2:tcp://localhost:9092/~/test");
    xaDatasource.setUser("test");
    xaDatasource.setPassword("test");
    return xaDatasource;
  }

  /**
   * assertTableRows(1, 1, 1, 1, 1, 4, 4, 2);
   */
  private Email getDefaultFullEmail() {
    HashMap<String, Attachment> inlineImageByCidMap = new HashMap<String, Attachment>();
    inlineImageByCidMap.put(DEFAULT_CID,
        createAttachment(DEFAULT_CONTENT_TYPE_IMAGE, DEFAULT_NAME_SAMPLE_IMG_NAME, "sample.png"));

    Collection<Attachment> attachments = new ArrayList<>();
    attachments.add(
        createAttachment(DEFAULT_CONTENT_TYPE_TXT, DEFAULT_NAME_SAMPLE_TXT_NAME, "sample.txt"));

    EmailAddress to = createEmailAddress(DEFAULT_TO_ADDRESS, DEFAULT_TO_PERSON);
    Collection<EmailAddress> collectionTo = new ArrayList<>();
    collectionTo.add(to);

    EmailAddress cc = createEmailAddress(DEFAULT_CC_ADDRESS, DEFAULT_CC_PERSON);
    Collection<EmailAddress> collectionCc = new ArrayList<>();
    collectionCc.add(cc);

    EmailAddress bcc = createEmailAddress(DEFAULT_BCC_ADDRESS, DEFAULT_BCC_PERSON);
    Collection<EmailAddress> collectionBcc = new ArrayList<>();
    collectionBcc.add(bcc);

    return new Email()
        .withSubject(DEFAULT_SUBJECT)
        .withTextContent(DEFAULT_TEXT_CONTENT)
        .withFrom(createEmailAddress(DEFAULT_FROM_ADDRESS, DEFAULT_FROM_PERSON))
        .withHtmlContent(new HtmlContent()
            .withHtml(DEFAULT_HTML)
            .withInlineImageByCidMap(inlineImageByCidMap))
        .withAttachments(attachments)
        .withRecipients(new Recipients()
            .withTo(collectionTo)
            .withCc(collectionCc)
            .withBcc(collectionBcc));
  }

  private long selectTableCount(final RelationalPathBase<?> table) {
    return querydslSupport.execute((connection, configuration) -> {
      return new SQLQuery(connection, configuration)
          .from(table)
          .count();
    });
  }

  @Test
  public void testDeleteEmail() {
    Email firstEmail = getDefaultFullEmail();
    long firstEmailId = emailStore.save(firstEmail);
    assertTableRows(1, 1, 1, 1, 1, 4, 4, 2);

    Email secondEmail = getDefaultFullEmail();
    secondEmail.recipients.to.add((createEmailAddress("test0", "test0")));
    secondEmail.recipients.to.add((createEmailAddress("test1", "test1")));
    secondEmail.recipients.cc.add((createEmailAddress("test2", "test2")));
    Attachment txtAttachment = createAttachment("test", "test", "sample.txt");
    secondEmail.attachments.add(txtAttachment);
    secondEmail.htmlContent.inlineImageByCidMap.put("test", txtAttachment);
    long secondEmailId = emailStore.save(secondEmail);
    assertTableRows(2, 3, 2, 2, 3, 11, 11, 6);

    emailStore.delete(firstEmailId);
    Email readEmail = emailStore.read(firstEmailId);
    Assert.assertNull(readEmail);
    assertTableRows(1, 2, 1, 1, 2, 7, 7, 4);

    try {
      emailStore.delete(firstEmailId);
      Assert.fail("Not exists email. Expect NonExistentEmailException.");
    } catch (NonExistentEmailException e) {
      Assert.assertNotNull(e);
    }
    assertTableRows(1, 2, 1, 1, 2, 7, 7, 4);

    emailStore.delete(secondEmailId);
    readEmail = emailStore.read(secondEmailId);
    Assert.assertNull(readEmail);
    assertTableRows(0, 0, 0, 0, 0, 0, 0, 0);

    Email thirdEmail = getDefaultFullEmail();
    thirdEmail.withTextContent(null);
    thirdEmail.withHtmlContent(null);
    thirdEmail.withAttachments(Collections.emptyList());
    thirdEmail.withRecipients(new Recipients());
    thirdEmail.withFrom(null);
    long thirdEmailId = emailStore.save(thirdEmail);
    assertTableRows(1, 0, 0, 0, 0, 0, 0, 0);
    emailStore.delete(thirdEmailId);
    assertTableRows(0, 0, 0, 0, 0, 0, 0, 0);

    Email fourthEmail = getDefaultFullEmail();
    fourthEmail.htmlContent.withHtml(null);
    long fourthEmailId = emailStore.save(fourthEmail);
    assertTableRows(1, 1, 1, 1, 1, 4, 4, 2);
    emailStore.delete(fourthEmailId);
    assertTableRows(0, 0, 0, 0, 0, 0, 0, 0);
  }

  private void testReadComplexEmail() {
    Email saveEmail = getDefaultFullEmail();
    saveEmail.attachments.add(createAttachment("second", "second", "sample.txt"));
    saveEmail.recipients.to.add(createEmailAddress("second-to", "second-to"));
    saveEmail.recipients.to.add(createEmailAddress("third-to", "third-to"));
    saveEmail.recipients.cc.add(createEmailAddress("second-cc", "second-cc"));
    saveEmail.recipients.bcc.clear();
    saveEmail.htmlContent.inlineImageByCidMap.put("second-cid",
        createAttachment("second", "second", "sample.png"));
    saveEmail.withFrom(null);
    long storedEmailId = emailStore.save(saveEmail);
    Email readEmail = emailStore.read(storedEmailId);
    Assert.assertEquals(2, readEmail.attachments.size());
    List<Attachment> attachments = (List<Attachment>) readEmail.attachments;
    Attachment attachment = attachments.get(1);
    Assert.assertEquals("second", attachment.contentType);
    Assert.assertEquals("second", attachment.name);

    Assert.assertNull(DEFAULT_FROM_ADDRESS, readEmail.from);

    Assert.assertEquals(2, readEmail.htmlContent.inlineImageByCidMap.size());

    Assert.assertEquals(3, readEmail.recipients.to.size());
    List<EmailAddress> toAddresses = (List<EmailAddress>) readEmail.recipients.to;
    EmailAddress to2 = toAddresses.get(1);
    Assert.assertEquals("second-to", to2.address);
    Assert.assertEquals("second-to", to2.personal);
    EmailAddress to3 = toAddresses.get(2);
    Assert.assertEquals("third-to", to3.address);
    Assert.assertEquals("third-to", to3.personal);

    Assert.assertEquals(2, readEmail.recipients.cc.size());
    List<EmailAddress> ccAddresses = (List<EmailAddress>) readEmail.recipients.cc;
    EmailAddress cc = ccAddresses.get(1);
    Assert.assertEquals("second-cc", cc.address);
    Assert.assertEquals("second-cc", cc.personal);

    Assert.assertEquals(0, readEmail.recipients.bcc.size());
  }

  private void testReadDefaultEmail() {
    long storedEmailId = emailStore.save(getDefaultFullEmail());
    Email email = emailStore.read(storedEmailId);

    Assert.assertEquals(1, email.attachments.size());
    List<Attachment> attachments = (List<Attachment>) email.attachments;
    Attachment attachment = attachments.get(0);
    Assert.assertEquals(DEFAULT_CONTENT_TYPE_TXT, attachment.contentType);
    Assert.assertEquals(DEFAULT_NAME_SAMPLE_TXT_NAME, attachment.name);
    Assert.assertNotNull(attachment.inputStreamSupplier);

    Assert.assertEquals(DEFAULT_FROM_ADDRESS, email.from.address);
    Assert.assertEquals(DEFAULT_FROM_PERSON, email.from.personal);

    Assert.assertEquals(DEFAULT_HTML, email.htmlContent.html);
    Assert.assertEquals(1, email.htmlContent.inlineImageByCidMap.size());
    Attachment inlineImageAttachment = email.htmlContent.inlineImageByCidMap.get(DEFAULT_CID);
    Assert.assertEquals(DEFAULT_CONTENT_TYPE_IMAGE, inlineImageAttachment.contentType);
    Assert.assertEquals(DEFAULT_NAME_SAMPLE_IMG_NAME, inlineImageAttachment.name);
    Assert.assertNotNull(inlineImageAttachment.inputStreamSupplier);

    Assert.assertEquals(1, email.recipients.to.size());
    List<EmailAddress> toAddresses = (List<EmailAddress>) email.recipients.to;
    EmailAddress to = toAddresses.get(0);
    Assert.assertEquals(DEFAULT_TO_ADDRESS, to.address);
    Assert.assertEquals(DEFAULT_TO_PERSON, to.personal);

    Assert.assertEquals(1, email.recipients.cc.size());
    List<EmailAddress> ccAddresses = (List<EmailAddress>) email.recipients.cc;
    EmailAddress cc = ccAddresses.get(0);
    Assert.assertEquals(DEFAULT_CC_ADDRESS, cc.address);
    Assert.assertEquals(DEFAULT_CC_PERSON, cc.personal);

    Assert.assertEquals(1, email.recipients.bcc.size());
    List<EmailAddress> bccAddresses = (List<EmailAddress>) email.recipients.bcc;
    EmailAddress bcc = bccAddresses.get(0);
    Assert.assertEquals(DEFAULT_BCC_ADDRESS, bcc.address);
    Assert.assertEquals(DEFAULT_BCC_PERSON, bcc.personal);

    Assert.assertEquals(DEFAULT_SUBJECT, email.subject);

    Assert.assertEquals(DEFAULT_TEXT_CONTENT, email.textContent);
  }

  @Test
  public void testReadEmail() {
    testReadDefaultEmail();
    testReadComplexEmail();
    testReadEmptyInlineImageByCidMap();
  }

  private void testReadEmptyInlineImageByCidMap() {
    Email email = getDefaultFullEmail();
    email.htmlContent.inlineImageByCidMap = Collections.emptyMap();
    long storedEmailId = emailStore.save(email);
    Email readEmail = emailStore.read(storedEmailId);
    Assert.assertEquals(0, readEmail.htmlContent.inlineImageByCidMap.size());
  }

  private void testSaveAttachments() {
    try {
      emailStore.save(getDefaultFullEmail().withAttachments(null));
      Assert.fail("Attachments is null. Expect NullPointerException.");
    } catch (NullPointerException e) {
      Assert.assertNotNull(e);
    }

    Email email = getDefaultFullEmail();
    List<Attachment> attachments = (ArrayList<Attachment>) email.attachments;
    attachments.get(0).withContentType(null);
    long storedEmailId = emailStore.save(email);
    Email readEmail = emailStore.read(storedEmailId);
    attachments = (ArrayList<Attachment>) readEmail.attachments;
    Assert.assertNull(attachments.get(0).contentType);

    email = getDefaultFullEmail();
    attachments = (ArrayList<Attachment>) email.attachments;
    attachments.get(0).withInputStreamSupplier(null);
    storedEmailId = emailStore.save(email);
    readEmail = emailStore.read(storedEmailId);
    attachments = (ArrayList<Attachment>) readEmail.attachments;
    Assert.assertNull(attachments.get(0).inputStreamSupplier);

    email = getDefaultFullEmail();
    attachments = (ArrayList<Attachment>) email.attachments;
    attachments.get(0).withName(null);
    storedEmailId = emailStore.save(email);
    readEmail = emailStore.read(storedEmailId);
    attachments = (ArrayList<Attachment>) readEmail.attachments;
    Assert.assertNull(attachments.get(0).name);

    try {
      email = getDefaultFullEmail();
      attachments = (ArrayList<Attachment>) email.attachments;
      attachments.clear();
      attachments.add(null);
      emailStore.save(email);
      Assert.fail("Attachment is null. Expect NullPointerException.");
    } catch (NullPointerException e) {
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void testSaveEmail() {
    try {
      emailStore.save(null);
      Assert.fail("Expect NullPointerException. The email parameter is null!");
    } catch (NullPointerException e) {
      Assert.assertNotNull(e);
    }

    testSaveAttachments();

    testSaveFrom();

    testSaveHtmlContent();

    testSaveRecipients();

    long storedEmailId = emailStore.save(getDefaultFullEmail().withSubject(null));
    Email readEmail = emailStore.read(storedEmailId);
    Assert.assertNull(readEmail.subject);

    storedEmailId = emailStore.save(getDefaultFullEmail().withTextContent(null));
    readEmail = emailStore.read(storedEmailId);
    Assert.assertNull(readEmail.textContent);

  }

  private void testSaveFrom() {
    long storedEmailId = emailStore.save(getDefaultFullEmail().withFrom(null));
    Email readEmail = emailStore.read(storedEmailId);
    Assert.assertNull(readEmail.from);

    Email email = getDefaultFullEmail();
    email.from.withAddress(null);
    storedEmailId = emailStore.save(email);
    readEmail = emailStore.read(storedEmailId);
    Assert.assertNull(readEmail.from.address);

    email = getDefaultFullEmail();
    email.from.withPersonal(null);
    storedEmailId = emailStore.save(email);
    readEmail = emailStore.read(storedEmailId);
    Assert.assertNull(readEmail.from.personal);
  }

  private void testSaveHtmlContent() {
    long storedEmailId = emailStore.save(getDefaultFullEmail().withHtmlContent(null));
    Email readEmail = emailStore.read(storedEmailId);
    Assert.assertNull(readEmail.htmlContent);

    Email email = getDefaultFullEmail();
    email.htmlContent.withHtml(null);
    storedEmailId = emailStore.save(email);
    readEmail = emailStore.read(storedEmailId);
    Assert.assertNull(readEmail.htmlContent.html);

    try {
      email = getDefaultFullEmail();
      email.htmlContent.withInlineImageByCidMap(null);
      emailStore.save(email);
      Assert.fail("InlineImageByCidMap collection is null. "
          + "Expect NullPointerException.");
    } catch (NullPointerException e) {
      Assert.assertNotNull(e);
    }

    try {
      email = getDefaultFullEmail();
      email.htmlContent.inlineImageByCidMap.put(DEFAULT_CID, null);
      emailStore.save(email);
      Assert.fail("Attachment is null. Expect NullPointerException.");
    } catch (NullPointerException e) {
      Assert.assertNotNull(e);
    }
  }

  private void testSaveRecipients() {
    long storedEmailId = emailStore.save(getDefaultFullEmail().withRecipients(null));
    Email readEmail = emailStore.read(storedEmailId);
    Assert.assertTrue(readEmail.recipients.to.isEmpty());
    Assert.assertTrue(readEmail.recipients.cc.isEmpty());
    Assert.assertTrue(readEmail.recipients.bcc.isEmpty());

    Email email = getDefaultFullEmail();
    try {
      email.recipients.withTo(null);
      emailStore.save(email);
      Assert.fail("Recipient.TO collection is null. Expect NullPointerException.");
    } catch (NullPointerException e) {
      Assert.assertNotNull(e);
    }

    try {
      email = getDefaultFullEmail();
      email.recipients.withCc(null);
      emailStore.save(email);
      Assert.fail("Recipient.CC collection is null. Expect NullPointerException.");
    } catch (NullPointerException e) {
      Assert.assertNotNull(e);
    }

    try {
      email = getDefaultFullEmail();
      email.recipients.withBcc(null);
      emailStore.save(email);
      Assert.fail("Recipient.BCC collection is null. Expect NullPointerException.");
    } catch (NullPointerException e) {
      Assert.assertNotNull(e);
    }
  }
}
