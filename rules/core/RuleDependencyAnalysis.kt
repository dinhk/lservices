package com.qut.webservices.igalogicservices.rules.core

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Helper class used to determine the interdependencies between rules
 */
class RuleDependencyAnalysis<T, C>(rules: Array<Rule<T, C>>, source: T, calculations: C)
        where T : Any, C : Any {

    private val logger: Logger = LoggerFactory.getLogger(RuleDependencyAnalysis::class.java)
    private val numberedRules = mutableMapOf<Int, Rule<T, C>?>()

    init {
        numberedRules[-1] = null
//      Assign a number to each rule. This is required by the TopographicalSort algorithm
        for ((i, rule) in rules.withIndex()) {
            rule.setMetaDataCollectionMode(source, calculations)
            numberedRules[i] = rule
        }
    }

    /**
     * Find the set of rules directly triggered by a particular attribute
     */
    private fun findTriggeredRules(attribute: Attribute): List<Int> {
        val triggered = mutableListOf<Int>()
        for (rule in numberedRules.filter { it.value != null }) {
            if (attribute in rule.value!!.getMetadata().triggers) {
                triggered.add(rule.key)
            }
        }
        return triggered
    }

    /**
     * Given a set of changed attributes:
     * -determine which rules directly reference the changed attributes
     * -discover the rule interdependencies
     * -order the rules using a Topographical Sort
     */
    fun resolveExecutionOrder(attributes: List<Attribute>): Pair<List<Rule<T, C>>, Set<Attribute>> {

        val requiredAttributes = mutableSetOf<Attribute>()
        val triggeredRules = resolvedRuleDependencies(attributes)
        for (rule in triggeredRules) {
            for (pair in rule.filter { it >= 0 }) {
                val metadata = numberedRules[pair]!!.getMetadata()
                val allRequirements = metadata.triggers.union(metadata.requirements)
                requiredAttributes.addAll(allRequirements)
            }
        }
        val allRules = triggeredRules.flatten().distinct()

//        we need to re-index the rules - start by creating a map of the original index to the new unique id
        val lookup = allRules.withIndex().associateBy({ it.value }, { it.index })
        val reverseLookup = allRules.withIndex().associateBy({ it.index }, { it.value })

        logger.trace("allRules={}", allRules)

        val reIndexedRules = mutableListOf<IntArray>()
        for (triggeredRule in triggeredRules) {
            reIndexedRules.add(intArrayOf(lookup[triggeredRule[0]]!!, lookup[triggeredRule[1]]!!))
        }

        val orderedRules = TopographicalSort.findOrder(allRules.size, reIndexedRules.toTypedArray())
        return Pair(orderedRules.map { reverseLookup[it] }.filter { it!! >= 0 }.map { numberedRules[it]!! }, requiredAttributes)
    }

    /**
     * Given a set of changed attributes:
     * -determine which rules directly reference the changed attributes
     * -discover the rule interdependencies
     * -return the full set of attributes required to execute the in-scope rules
     */
    fun resolveEntityRequirements(attributes: List<Attribute>): Set<Attribute> {
        val requiredAttributes = mutableSetOf<Attribute>()
        val triggeredRules = resolvedRuleDependencies(attributes)

        val forDiagramsDotNet = mutableListOf<String>()
        for (rule in triggeredRules) {
            for (pair in rule.filter { it >= 0 }) {
                val metadata = numberedRules[pair]!!.getMetadata()
                val allRequirements = metadata.triggers.union(metadata.requirements)
                requiredAttributes.addAll(allRequirements)
                for (trigger in allRequirements) {
                    if (trigger.parent != "Calculated") {
                        forDiagramsDotNet.add("${numberedRules[pair]}->$trigger")
                    }
                }
            }
        }
        logger.trace(forDiagramsDotNet.distinct().joinToString("\r\n"))
        return requiredAttributes
    }

    /**
     * Given a set of changed attributes:
     * -determine which rules directly reference the changed attributes
     * -discover the rule interdependencies, returning them a sets of pairs
     */
    private fun resolvedRuleDependencies(attributes: List<Attribute>): List<List<Int>> {
        val pairs = mutableListOf<Pair<Int, Int>>()
        for (attribute in attributes) {
            pairs.addAll(findAttributeDependencies(attribute))
        }

        val listOfArrays = mutableListOf<List<Int>>()
        val forDiagramsDotNet = StringBuilder()
        for (pair in pairs.distinctBy { it }) {
            logger.trace("{} depends on {}", numberedRules[pair.first], numberedRules[pair.second])
            forDiagramsDotNet.appendLine("${numberedRules[pair.first]}->${numberedRules[pair.second]}")
            listOfArrays.add(pair.toList())
        }
        logger.trace("{}", forDiagramsDotNet)
        return listOfArrays
    }

    /**
     * Find all rules which directly reference the given attribute.
     * Then find all rules which have a parent-child or child-parent
     * relationship to those rules
     */
    private fun findAttributeDependencies(attribute: Attribute): List<Pair<Int, Int>> {
        val triggeredRules = findTriggeredRules(attribute)
        logger.trace("triggeredRule Ids = {}", triggeredRules)
        logger.trace("triggeredRules = {}", triggeredRules.map { numberedRules[it]!! })

        val pairs = mutableListOf<Pair<Int, Int>>()
        val traversed = mutableListOf<Pair<Int, Int>>()
        for (triggeredRule in triggeredRules) {
            findRuleDependencies(pairs, traversed, triggeredRule)
//            add a default dependency
            pairs.add(Pair(triggeredRule, -1))
        }
        return pairs
    }

    /**
     * Find all rules which have a parent-child or child-parent to the given numbered rule
     */
    private fun findRuleDependencies(
        pairs: MutableList<Pair<Int, Int>>,
        traversed: MutableList<Pair<Int, Int>>,
        ruleId: Int,
    ) {
        val rule = numberedRules[ruleId] ?: throw IllegalArgumentException("Invalid ruleId")

//        find any rules that populate the inputs to the rule
        for (requiredInput in rule.getMetadata().triggers)
        {
            for (candidateRule in numberedRules.filter { it.value != null }) {
                if (candidateRule.value!!.getMetadata().output == requiredInput) {
                    val pair = Pair(ruleId, candidateRule.key)
                    pairs.add(pair)
                    if (pair !in traversed) {
                        traversed.add(pair)
                        findRuleDependencies(pairs, traversed, pair.second)
                    }
                }
            }
        }

//         find any rules that have a dependency on the output of the rule
        for (candidateRule in numberedRules.filter { it.value != null }) {
            for (requiredInput in candidateRule.value!!.getMetadata().triggers)
//           filter { it != ignore })
            {
                if (rule.getMetadata().output == requiredInput) {
                    val pair = Pair(candidateRule.key, ruleId)
                    pairs.add(pair)
                    if (pair !in traversed) {
                        traversed.add(pair)
                        findRuleDependencies(pairs, traversed, pair.first)
                    }
                }
            }
        }
    }

}