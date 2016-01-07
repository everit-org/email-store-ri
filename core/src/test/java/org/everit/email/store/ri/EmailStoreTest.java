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
import org.everit.persistence.querydsl.support.ri.QuerydslSupportImpl;
import org.everit.transaction.propagator.TransactionPropagator;
import org.everit.transaction.propagator.jta.JTATransactionPropagator;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.mysema.query.sql.Configuration;
import com.mysema.query.sql.H2Templates;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

public class EmailStoreTest {

  private static class DummyInputStreamSupplier implements InputStreamSupplier {

    private final ClassLoader classLoader;

    private final String fileName;

    DummyInputStreamSupplier(final ClassLoader classLoader, final String fileName) {
      this.fileName = fileName;
      this.classLoader = classLoader;
    }

    @Override
    public InputStream getStream() {
      return classLoader.getResourceAsStream(fileName);
    }
  }

  private static final String TEST_CID = "test-cid";

  private static final String TEST_SUBJECT = "test-subject";

  private Blobstore blobstore;

  private EmailStoreImpl emailStore;

  private BasicManagedDataSource managedDataSource = null;

  private TransactionPropagator transactionPropagator;

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

    QuerydslSupportImpl querydslSupport =
        new QuerydslSupportImpl(new Configuration(H2Templates.DEFAULT), managedDataSource);
    emailStore = new EmailStoreImpl(querydslSupport, transactionPropagator, blobstore);
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
    xaDatasource.setUser("sa");
    xaDatasource.setPassword("sa");
    return xaDatasource;
  }

  private Email getDefaultFullEmail() {
    HashMap<String, Attachment> inlineImageByCidMap = new HashMap<String, Attachment>();
    Attachment sampleImgAttachment = new Attachment()
        .withContentType("image")
        .withName("test-sample-img-name")
        .withInputStreamSupplier(
            new DummyInputStreamSupplier(getClass().getClassLoader(), "sample.png"));
    inlineImageByCidMap.put(TEST_CID, sampleImgAttachment);

    Attachment sampleTxtAttachment = new Attachment()
        .withContentType("txt")
        .withName("test-sample-txt-name")
        .withInputStreamSupplier(
            new DummyInputStreamSupplier(getClass().getClassLoader(), "sample.txt"));
    Collection<Attachment> attachments = new ArrayList<>();
    attachments.add(sampleTxtAttachment);

    EmailAddress to = createEmailAddress("test-address-to", "test-person-to");
    Collection<EmailAddress> collectionTo = new ArrayList<>();
    collectionTo.add(to);

    EmailAddress cc = createEmailAddress("test-address-cc", "test-person-cc");
    Collection<EmailAddress> collectionCc = new ArrayList<>();
    collectionCc.add(cc);

    EmailAddress bcc = createEmailAddress("test-address-bcc", "test-person-bcc");
    Collection<EmailAddress> collectionBcc = new ArrayList<>();
    collectionBcc.add(bcc);

    return new Email()
        .withSubject(TEST_SUBJECT)
        .withTextContent("test-text-content")
        .withFrom(createEmailAddress("test-address-from", "test-person-from"))
        .withHtmlContent(new HtmlContent()
            .withHtml("test-html")
            .withInlineImageByCidMap(inlineImageByCidMap))
        .withAttachments(attachments)
        .withRecipients(new Recipients()
            .withTo(collectionTo)
            .withCc(collectionCc)
            .withBcc(collectionBcc));
  }

  @Test
  public void testReadEmail() {
    long storedEmailId = emailStore.save(getDefaultFullEmail());
    Email read = emailStore.read(storedEmailId);
    Assert.assertEquals(TEST_SUBJECT, read.subject);
  }

  private void testSaveAttachments() {
    emailStore.save(getDefaultFullEmail().withAttachments(null));

    Email email = getDefaultFullEmail();
    List<Attachment> attachments = (ArrayList<Attachment>) email.attachments;
    attachments.get(0).withContentType(null);
    emailStore.save(email);

    email = getDefaultFullEmail();
    attachments = (ArrayList<Attachment>) email.attachments;
    attachments.get(0).withInputStreamSupplier(null);
    emailStore.save(email);

    email = getDefaultFullEmail();
    attachments = (ArrayList<Attachment>) email.attachments;
    attachments.get(0).withName(null);
    emailStore.save(email);
  }

  @Test
  public void testSaveEmail() {
    try {
      emailStore.save(null);
      Assert.fail("Expect exception. The email parameter is null!");
    } catch (RuntimeException e) {
      Assert.assertNotNull(e);
    }

    emailStore.save(getDefaultFullEmail());

    testSaveAttachments();

    testSaveFrom();

    testSaveHtmlContent();

    testSaveRecipients();

    emailStore.save(getDefaultFullEmail().withSubject(null));

    emailStore.save(getDefaultFullEmail().withTextContent(null));

  }

  private void testSaveFrom() {
    emailStore.save(getDefaultFullEmail().withFrom(null));

    Email email = getDefaultFullEmail();
    email.from.withAddress(null);
    emailStore.save(email);

    email = getDefaultFullEmail();
    email.from.withPersonal(null);
    emailStore.save(email);
  }

  private void testSaveHtmlContent() {
    emailStore.save(getDefaultFullEmail().withHtmlContent(null));

    Email email = getDefaultFullEmail();
    email.htmlContent.withHtml(null);
    emailStore.save(email);

    email = getDefaultFullEmail();
    email.htmlContent.withInlineImageByCidMap(null);
    emailStore.save(email);

    email = getDefaultFullEmail();
    email.htmlContent.inlineImageByCidMap.put(TEST_CID, null);
    emailStore.save(email);
  }

  private void testSaveRecipients() {
    emailStore.save(getDefaultFullEmail().withRecipients(null));

    Email email = getDefaultFullEmail();
    email.recipients.withTo(null);
    emailStore.save(email);

    email = getDefaultFullEmail();
    email.recipients.withCc(null);
    emailStore.save(email);

    email = getDefaultFullEmail();
    email.recipients.withBcc(null);
    emailStore.save(email);
  }
}
