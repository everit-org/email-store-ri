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
 * QHtmlContent is a Querydsl query type for QHtmlContent
 */
@Generated("com.mysema.query.sql.codegen.MetaDataSerializer")
public class QHtmlContent extends com.mysema.query.sql.RelationalPathBase<QHtmlContent> {

    private static final long serialVersionUID = 1829127631;

    public static final QHtmlContent htmlContent = new QHtmlContent("EMSTR_HTML_CONTENT");

    public class PrimaryKeys {

        public final com.mysema.query.sql.PrimaryKey<QHtmlContent> emstrHtmlContentPk = createPrimaryKey(htmlContentId);

    }

    public class ForeignKeys {

        public final com.mysema.query.sql.ForeignKey<QEmail> emstrHtmlContentEmailFk = createForeignKey(storedEmailId, "STORED_EMAIL_ID");

        public final com.mysema.query.sql.ForeignKey<QInlineImage> _emstrInlineImgHtmlContFk = createInvForeignKey(htmlContentId, "HTML_CONTENT_ID");

    }

    public final NumberPath<Long> blobId = createNumber("blobId", Long.class);

    public final NumberPath<Long> htmlContentId = createNumber("htmlContentId", Long.class);

    public final NumberPath<Long> storedEmailId = createNumber("storedEmailId", Long.class);

    public final PrimaryKeys pk = new PrimaryKeys();

    public final ForeignKeys fk = new ForeignKeys();

    public QHtmlContent(String variable) {
        super(QHtmlContent.class, forVariable(variable), "org.everit.email.store.ri", "EMSTR_HTML_CONTENT");
        addMetadata();
    }

    public QHtmlContent(String variable, String schema, String table) {
        super(QHtmlContent.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QHtmlContent(Path<? extends QHtmlContent> path) {
        super(path.getType(), path.getMetadata(), "org.everit.email.store.ri", "EMSTR_HTML_CONTENT");
        addMetadata();
    }

    public QHtmlContent(PathMetadata<?> metadata) {
        super(QHtmlContent.class, metadata, "org.everit.email.store.ri", "EMSTR_HTML_CONTENT");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(blobId, ColumnMetadata.named("BLOB_ID").ofType(-5).withSize(19));
        addMetadata(htmlContentId, ColumnMetadata.named("HTML_CONTENT_ID").ofType(-5).withSize(19).notNull());
        addMetadata(storedEmailId, ColumnMetadata.named("STORED_EMAIL_ID").ofType(-5).withSize(19).notNull());
    }

}

