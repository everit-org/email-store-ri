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
 * QAttachment is a Querydsl query type for QAttachment
 */
@Generated("com.mysema.query.sql.codegen.MetaDataSerializer")
public class QAttachment extends com.mysema.query.sql.RelationalPathBase<QAttachment> {

    private static final long serialVersionUID = 70745474;

    public static final QAttachment attachment = new QAttachment("EMSTR_ATTACHMENT");

    public class PrimaryKeys {

        public final com.mysema.query.sql.PrimaryKey<QAttachment> emstrAttachmentPk = createPrimaryKey(attachmentId);

    }

    public class ForeignKeys {

        public final com.mysema.query.sql.ForeignKey<QBinaryContent> emstrAttachmentBnryContFk = createForeignKey(binaryContentId, "BINARY_CONTENT_ID");

        public final com.mysema.query.sql.ForeignKey<QEmail> emstrAttachmentEmailFk = createForeignKey(storedEmailId, "STORED_EMAIL_ID");

    }

    public final NumberPath<Long> attachmentId = createNumber("attachmentId", Long.class);

    public final NumberPath<Long> binaryContentId = createNumber("binaryContentId", Long.class);

    public final NumberPath<Integer> index_ = createNumber("index_", Integer.class);

    public final NumberPath<Long> storedEmailId = createNumber("storedEmailId", Long.class);

    public final PrimaryKeys pk = new PrimaryKeys();

    public final ForeignKeys fk = new ForeignKeys();

    public QAttachment(String variable) {
        super(QAttachment.class, forVariable(variable), "org.everit.email.store.ri", "EMSTR_ATTACHMENT");
        addMetadata();
    }

    public QAttachment(String variable, String schema, String table) {
        super(QAttachment.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QAttachment(Path<? extends QAttachment> path) {
        super(path.getType(), path.getMetadata(), "org.everit.email.store.ri", "EMSTR_ATTACHMENT");
        addMetadata();
    }

    public QAttachment(PathMetadata<?> metadata) {
        super(QAttachment.class, metadata, "org.everit.email.store.ri", "EMSTR_ATTACHMENT");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(attachmentId, ColumnMetadata.named("ATTACHMENT_ID").ofType(-5).withSize(19).notNull());
        addMetadata(binaryContentId, ColumnMetadata.named("BINARY_CONTENT_ID").ofType(-5).withSize(19));
        addMetadata(index_, ColumnMetadata.named("INDEX_").ofType(4).withSize(10).notNull());
        addMetadata(storedEmailId, ColumnMetadata.named("STORED_EMAIL_ID").ofType(-5).withSize(19).notNull());
    }

}

