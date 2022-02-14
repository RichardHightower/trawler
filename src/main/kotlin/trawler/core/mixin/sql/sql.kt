package trawler.core.mixin.sql

import trawler.core.config.RelationshipDefinition
import trawler.core.model.Association
import trawler.core.model.AssociationModel
import trawler.core.model.Field
import trawler.core.model.ModelObjectMeta


enum class SqlColumnType {
    VARCHAR,
    BOOLEAN,
    INT,
    BIGINT,
    DECIMAL,
    FLOAT,
    DOUBLE,
    DATE,
    DATETIME
}


data class DatabaseCatalog(
    val name: String
)
data class Column(
    val name: String,
    val isPrimaryKey: Boolean,
    val isKey: Boolean,
    val isFK: Boolean,
    val isGenerated: Boolean,
    val size: Int,
    val sqlColumnType: SqlColumnType,

)

data class Table(
    val name: String, val database: DatabaseCatalog,
    val columns: List<Column>,
    val relationships: List<Relationship>
)

interface Relationship
data class OneToManyTableRelationship(
    val oneTable : Table, val manyTable: Table,
    val oneTableFKColumnName : String, val manyTableKeyColumnName: String
) : Relationship

data class ManyToManyRelationship(
    val tableA : Table, val tableB: Table, val joinTable: Table,
    val aTableKeyColumnName : String, val bTableKeyColumnName: String,
    val joinTableFKA : String, val joinTableFKB: String
) : Relationship

data class  SqlTableMapping (val model:ModelObjectMeta, val table:Table,
                             val fieldMappings : Map<Field, Column>,
                             val associationMappings: Map<AssociationModel, Relationship>)

object SqlTableMapper {
    fun createMapping( model:ModelObjectMeta, table:Table, relationshipDef: RelationshipDefinition?):SqlTableMapping {
        return SqlTableMapping(model, table, mapOf(), mapOf())
    }
}

