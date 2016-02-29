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
 * QTextContent is a Querydsl query type for QTextContent
 */
@Generated("com.mysema.query.sql.codegen.MetaDataSerializer")
public class QTextContent extends com.mysema.query.sql.RelationalPathBase<QTextContent> {

    private static final long serialVersionUID = -1251266035;

    public static final QTextContent textContent = new QTextContent("EMSTR_TEXT_CONTENT");

    public class PrimaryKeys {

        public final com.mysema.query.sql.PrimaryKey<QTextContent> emstrTextContentPk = createPrimaryKey(textContentId);

    }

    public class ForeignKeys {

        public final com.mysema.query.sql.ForeignKey<QEmail> emstrTextContentEmailFk = createForeignKey(storedEmailId, "STORED_EMAIL_ID");

    }

    public final NumberPath<Long> blobId = createNumber("blobId", Long.class);

    public final NumberPath<Long> storedEmailId = createNumber("storedEmailId", Long.class);

    public final NumberPath<Long> textContentId = createNumber("textContentId", Long.class);

    public final PrimaryKeys pk = new PrimaryKeys();

    public final ForeignKeys fk = new ForeignKeys();

    public QTextContent(String variable) {
        super(QTextContent.class, forVariable(variable), "org.everit.email.store.ri", "EMSTR_TEXT_CONTENT");
        addMetadata();
    }

    public QTextContent(String variable, String schema, String table) {
        super(QTextContent.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QTextContent(Path<? extends QTextContent> path) {
        super(path.getType(), path.getMetadata(), "org.everit.email.store.ri", "EMSTR_TEXT_CONTENT");
        addMetadata();
    }

    public QTextContent(PathMetadata<?> metadata) {
        super(QTextContent.class, metadata, "org.everit.email.store.ri", "EMSTR_TEXT_CONTENT");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(blobId, ColumnMetadata.named("BLOB_ID").ofType(-5).withSize(19));
        addMetadata(storedEmailId, ColumnMetadata.named("STORED_EMAIL_ID").ofType(-5).withSize(19).notNull());
        addMetadata(textContentId, ColumnMetadata.named("TEXT_CONTENT_ID").ofType(-5).withSize(19).notNull());
    }

}

