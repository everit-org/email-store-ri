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
 * QInlineImage is a Querydsl query type for QInlineImage
 */
@Generated("com.mysema.query.sql.codegen.MetaDataSerializer")
public class QInlineImage extends com.mysema.query.sql.RelationalPathBase<QInlineImage> {

    private static final long serialVersionUID = 1272526275;

    public static final QInlineImage inlineImage = new QInlineImage("EMSTR_INLINE_IMAGE");

    public class PrimaryKeys {

        public final com.mysema.query.sql.PrimaryKey<QInlineImage> emstrInlineImagePk = createPrimaryKey(inlineImageId);

    }

    public class ForeignKeys {

        public final com.mysema.query.sql.ForeignKey<QBinaryContent> emstrInlineImgBnryContFk = createForeignKey(binaryContentId, "BINARY_CONTENT_ID");

        public final com.mysema.query.sql.ForeignKey<QEmail> emstrInlineImageEmailFk = createForeignKey(storedEmailId, "STORED_EMAIL_ID");

    }

    public final NumberPath<Long> binaryContentId = createNumber("binaryContentId", Long.class);

    public final StringPath cid_ = createString("cid_");

    public final NumberPath<Integer> index_ = createNumber("index_", Integer.class);

    public final NumberPath<Long> inlineImageId = createNumber("inlineImageId", Long.class);

    public final NumberPath<Long> storedEmailId = createNumber("storedEmailId", Long.class);

    public final PrimaryKeys pk = new PrimaryKeys();

    public final ForeignKeys fk = new ForeignKeys();

    public QInlineImage(String variable) {
        super(QInlineImage.class, forVariable(variable), "org.everit.email.store.ri", "EMSTR_INLINE_IMAGE");
        addMetadata();
    }

    public QInlineImage(String variable, String schema, String table) {
        super(QInlineImage.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QInlineImage(Path<? extends QInlineImage> path) {
        super(path.getType(), path.getMetadata(), "org.everit.email.store.ri", "EMSTR_INLINE_IMAGE");
        addMetadata();
    }

    public QInlineImage(PathMetadata<?> metadata) {
        super(QInlineImage.class, metadata, "org.everit.email.store.ri", "EMSTR_INLINE_IMAGE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(binaryContentId, ColumnMetadata.named("BINARY_CONTENT_ID").ofType(-5).withSize(19).notNull());
        addMetadata(cid_, ColumnMetadata.named("CID_").ofType(12).withSize(255));
        addMetadata(index_, ColumnMetadata.named("INDEX_").ofType(4).withSize(10).notNull());
        addMetadata(inlineImageId, ColumnMetadata.named("INLINE_IMAGE_ID").ofType(-5).withSize(19).notNull());
        addMetadata(storedEmailId, ColumnMetadata.named("STORED_EMAIL_ID").ofType(-5).withSize(19).notNull());
    }

}

