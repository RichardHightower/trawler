package trawler.core.config

import trawler.core.model.Action
import trawler.core.model.Effect


data class RoleDefinition(val moduleName: String, val name: String, val activeTags: List<TagDefinition>,
                          val ruleSets: Set<String>, val properties: Map<String, String>,
                          val description: String?)


data class AccessRuleDefinition(val moduleName: String, val name: String, val resources: Set<String>,
                                val actions: Set<Action>,
                                val effect: Effect,
                                val conditions:List<String>,
                                val description: String?)


