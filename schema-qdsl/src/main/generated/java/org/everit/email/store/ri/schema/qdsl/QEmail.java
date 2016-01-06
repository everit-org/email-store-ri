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
 * QEmail is a Querydsl query type for QEmail
 */
@Generated("com.mysema.query.sql.codegen.MetaDataSerializer")
public class QEmail extends com.mysema.query.sql.RelationalPathBase<QEmail> {

    private static final long serialVersionUID = 466003933;

    public static final QEmail email = new QEmail("EMSTR_EMAIL");

    public class PrimaryKeys {

        public final com.mysema.query.sql.PrimaryKey<QEmail> emstrEmailPk = createPrimaryKey(storedEmailId);

    }

    public class ForeignKeys {

        public final com.mysema.query.sql.ForeignKey<QAttachment> _emstrAttachmentEmailFk = createInvForeignKey(storedEmailId, "STORED_EMAIL_ID");

        public final com.mysema.query.sql.ForeignKey<QEmailRecipient> _emstrEmailRcpntEmailFk = createInvForeignKey(storedEmailId, "STORED_EMAIL_ID");

        public final com.mysema.query.sql.ForeignKey<QInlineImage> _emstrInlineImageEmailFk = createInvForeignKey(storedEmailId, "STORED_EMAIL_ID");

    }

    public final NumberPath<Long> htmlContentBlobId = createNumber("htmlContentBlobId", Long.class);

    public final NumberPath<Long> storedEmailId = createNumber("storedEmailId", Long.class);

    public final StringPath subject_ = createString("subject_");

    public final NumberPath<Long> textContentBlobId = createNumber("textContentBlobId", Long.class);

    public final PrimaryKeys pk = new PrimaryKeys();

    public final ForeignKeys fk = new ForeignKeys();

    public QEmail(String variable) {
        super(QEmail.class, forVariable(variable), "org.everit.email.store.ri", "EMSTR_EMAIL");
        addMetadata();
    }

    public QEmail(String variable, String schema, String table) {
        super(QEmail.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QEmail(Path<? extends QEmail> path) {
        super(path.getType(), path.getMetadata(), "org.everit.email.store.ri", "EMSTR_EMAIL");
        addMetadata();
    }

    public QEmail(PathMetadata<?> metadata) {
        super(QEmail.class, metadata, "org.everit.email.store.ri", "EMSTR_EMAIL");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(htmlContentBlobId, ColumnMetadata.named("HTML_CONTENT_BLOB_ID").ofType(-5).withSize(19).notNull());
        addMetadata(storedEmailId, ColumnMetadata.named("STORED_EMAIL_ID").ofType(-5).withSize(19).notNull());
        addMetadata(subject_, ColumnMetadata.named("SUBJECT_").ofType(12).withSize(255).notNull());
        addMetadata(textContentBlobId, ColumnMetadata.named("TEXT_CONTENT_BLOB_ID").ofType(-5).withSize(19).notNull());
    }

}

