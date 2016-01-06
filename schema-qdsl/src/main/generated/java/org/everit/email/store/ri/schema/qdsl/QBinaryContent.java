package org.everit.email.store.ri.schema.qdsl;

import static com.mysema.query.types.PathMetadataFactory.*;

import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;

import com.mysema.query.sql.ColumnMetadata;




/**
 * QBinaryContent is a Querydsl query type for QBinaryContent
 */
@Generated("com.mysema.query.sql.codegen.MetaDataSerializer")
public class QBinaryContent extends com.mysema.query.sql.RelationalPathBase<QBinaryContent> {

    private static final long serialVersionUID = -885131143;

    public static final QBinaryContent binaryContent = new QBinaryContent("EMSTR_BINARY_CONTENT");

    public class PrimaryKeys {

        public final com.mysema.query.sql.PrimaryKey<QBinaryContent> emstrBinaryContentPk = createPrimaryKey(binaryContentId);

    }

    public class ForeignKeys {

        public final com.mysema.query.sql.ForeignKey<QAttachment> _emstrAttachmentBnryContFk = createInvForeignKey(binaryContentId, "BINARY_CONTENT_ID");

        public final com.mysema.query.sql.ForeignKey<QInlineImage> _emstrInlineImgBnryContFk = createInvForeignKey(binaryContentId, "BINARY_CONTENT_ID");

    }

    public final NumberPath<Long> binaryContentId = createNumber("binaryContentId", Long.class);

    public final NumberPath<Long> blobId = createNumber("blobId", Long.class);

    public final StringPath name_ = createString("name_");

    public final PrimaryKeys pk = new PrimaryKeys();

    public final ForeignKeys fk = new ForeignKeys();

    public QBinaryContent(String variable) {
        super(QBinaryContent.class, forVariable(variable), "org.everit.email.store.ri", "EMSTR_BINARY_CONTENT");
        addMetadata();
    }

    public QBinaryContent(String variable, String schema, String table) {
        super(QBinaryContent.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QBinaryContent(Path<? extends QBinaryContent> path) {
        super(path.getType(), path.getMetadata(), "org.everit.email.store.ri", "EMSTR_BINARY_CONTENT");
        addMetadata();
    }

    public QBinaryContent(PathMetadata<?> metadata) {
        super(QBinaryContent.class, metadata, "org.everit.email.store.ri", "EMSTR_BINARY_CONTENT");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(binaryContentId, ColumnMetadata.named("BINARY_CONTENT_ID").ofType(-5).withSize(19).notNull());
        addMetadata(blobId, ColumnMetadata.named("BLOB_ID").ofType(-5).withSize(19).notNull());
        addMetadata(name_, ColumnMetadata.named("NAME_").ofType(12).withSize(255).notNull());
    }

}

