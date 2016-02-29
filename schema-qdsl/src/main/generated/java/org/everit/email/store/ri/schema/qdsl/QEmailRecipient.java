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
package org.everit.email.store.ri.schema.qdsl;

import static com.mysema.query.types.PathMetadataFactory.*;

import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;

import com.mysema.query.sql.ColumnMetadata;




/**
 * QEmailRecipient is a Querydsl query type for QEmailRecipient
 */
@Generated("com.mysema.query.sql.codegen.MetaDataSerializer")
public class QEmailRecipient extends com.mysema.query.sql.RelationalPathBase<QEmailRecipient> {

    private static final long serialVersionUID = 931099324;

    public static final QEmailRecipient emailRecipient = new QEmailRecipient("EMSTR_EMAIL_RECIPIENT");

    public class PrimaryKeys {

        public final com.mysema.query.sql.PrimaryKey<QEmailRecipient> emstrEmailRecipientPk = createPrimaryKey(emailRecipientId);

    }

    public class ForeignKeys {

        public final com.mysema.query.sql.ForeignKey<QEmail> emstrEmailRcpntEmailFk = createForeignKey(storedEmailId, "STORED_EMAIL_ID");

        public final com.mysema.query.sql.ForeignKey<QAddress> emstrEmailRcpntAddressFk = createForeignKey(emailAddressId, "EMAIL_ADDRESS_ID");

    }

    public final NumberPath<Long> emailAddressId = createNumber("emailAddressId", Long.class);

    public final NumberPath<Long> emailRecipientId = createNumber("emailRecipientId", Long.class);

    public final NumberPath<Integer> index_ = createNumber("index_", Integer.class);

    public final StringPath recipientType = createString("recipientType");

    public final NumberPath<Long> storedEmailId = createNumber("storedEmailId", Long.class);

    public final PrimaryKeys pk = new PrimaryKeys();

    public final ForeignKeys fk = new ForeignKeys();

    public QEmailRecipient(String variable) {
        super(QEmailRecipient.class, forVariable(variable), "org.everit.email.store.ri", "EMSTR_EMAIL_RECIPIENT");
        addMetadata();
    }

    public QEmailRecipient(String variable, String schema, String table) {
        super(QEmailRecipient.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QEmailRecipient(Path<? extends QEmailRecipient> path) {
        super(path.getType(), path.getMetadata(), "org.everit.email.store.ri", "EMSTR_EMAIL_RECIPIENT");
        addMetadata();
    }

    public QEmailRecipient(PathMetadata<?> metadata) {
        super(QEmailRecipient.class, metadata, "org.everit.email.store.ri", "EMSTR_EMAIL_RECIPIENT");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(emailAddressId, ColumnMetadata.named("EMAIL_ADDRESS_ID").ofType(-5).withSize(19).notNull());
        addMetadata(emailRecipientId, ColumnMetadata.named("EMAIL_RECIPIENT_ID").ofType(-5).withSize(19).notNull());
        addMetadata(index_, ColumnMetadata.named("INDEX_").ofType(4).withSize(10).notNull());
        addMetadata(recipientType, ColumnMetadata.named("RECIPIENT_TYPE").ofType(12).withSize(255).notNull());
        addMetadata(storedEmailId, ColumnMetadata.named("STORED_EMAIL_ID").ofType(-5).withSize(19).notNull());
    }

}

