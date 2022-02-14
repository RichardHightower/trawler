package trawler.core.config


data class FieldColumnMappingDefinition(val moduleName: String, val fieldName: String, val columnName: String?,
                                        val description: String?, val isPrimaryKey : Boolean, val sqlColumnType: String)
data class OneToOneDefinition(val moduleName: String, val fieldName: String, val model: String, val fk: String?,
                              val key: String?, val tags : List<TagDefinition>?)
data class OneToManyDefinition(val moduleName: String, val fieldName: String, val model: String, val fk: String?,
                               val key: String?, val tags : List<TagDefinition>?)
data class ManyToManyDefinition(val moduleName: String, val fieldName: String, val model: String, val joinTable:String?,
                                val fk: String?, val key: String?, val tags : List<TagDefinition>?)
data class RelationshipDefinition(val moduleName: String, val modelName: String, val table:String?, val description: String?,
                                  val fields: List<FieldDefinition>)
