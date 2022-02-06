package trawler.core.model

import trawler.core.config.TagDefinition

enum class Effect {
    ALLOW,
    DENY
}

interface Action

enum class ResourceType : Action {
    URI,
    MODEL
}

enum class HttpActions : Action {
    GET,
    POST,
    PUT,
    PATCH,
    DELETE,
    HEAD,
    OPTIONS,
    TRACE,
    CONNECT
}

data class Resource(val type:ResourceType, val path:String)

/* More conditions to come */
enum class ConditionOperator  {
    EQUAL,
}

data class  Condition (val principalProperty: String, val operator: ConditionOperator, val model: ModelObjectMeta, val fieldPath: List<String>)

data class AccessRule(val name: String, val resources: Set<Resource>,
                                val actions: Set<Action>,
                                val effect: Effect,
                                val conditions:List<Condition>)

data class Role(val name: String, val activeTags: List<TagDefinition>,
                val ruleSets: Set<AccessRule>, val properties: Map<String, String>)

