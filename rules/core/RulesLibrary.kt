package com.qut.webservices.igalogicservices.rules.core
import com.qut.webservices.igalogicservices.utils.Hashing
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

/**
 * Abstract class, providing the logic for implementing versioned rule-sets
 */
abstract class RulesLibrary<T, C> constructor(allRules: Array<Rule<T, C>>, protected val ruleLogger: RuleLogger)
        where T : Any, C: Any

{
    private val logger: Logger = LoggerFactory.getLogger(RulesLibrary::class.java)
    private val analysis : RuleDependencyAnalysis<T,C>
    private val calculations = createCalculationsObject()
    init {
        analysis = RuleDependencyAnalysis(allRules, createSourceObject(), calculations)
    }

    /**
     * Returns a set of in-scope rules for the incoming set of changed attributes.
     * The rules are ordered to ensure that respective dependencies are met
     */
    open fun findInScopeRules(changedAttributes: List<Attribute>) : Pair<List<Rule<T, C>>, Set<Attribute>>
    {
        return analysis.resolveExecutionOrder(changedAttributes)
    }

    /**
     * Checks to determine if a rule will be triggered by an attribute
     *
     * Returns:
     *  True - A rule exists for the changed attributes
     *  False - A rule does not exist for the changed attributes
     */
    open fun isRuleTriggeredByAttributes(changedAttributes: List<Attribute>): Boolean {
        val (ruleList, attributes) = findInScopeRules(changedAttributes)
        logger.trace("Rules that will be triggered ${ruleList.toString()} by attributes ${attributes.toString()}")
        return ruleList.isNotEmpty()
    }

    /**
     * Returns the set of data entities required to execute the rules which are triggered
     * in response to the provided set of changed attributes
     */
    open fun resolveRuleDataRequirements(changedAttributes: List<Attribute>) : List<Attribute>
    {
        return analysis.resolveEntityRequirements(changedAttributes).toList()
    }

//    ridiculously hard/impossible to create a generic class in Kotlin/Java unless you already have an instance of C !
//    see https://stackoverflow.com/questions/75175/create-instance-of-generic-type-in-java/25195050#25195050
    abstract fun createCalculationsObject() : C
    abstract fun createSourceObject() : T

    /**
     * Based on the provided source data and the set of entities for which there exists a hash, return the
     * minimum set of entities required by downstream consumers, such as the IGA
     */
    abstract fun protect(source: T, initialisedEntities: List<Attribute>) : List<String>

    /**
     * Returns the set of data entities required to execute the rules which are triggered
     * in response to the provided set of changed attributes
     */
    open fun resolveDataRequirements(changedAttributes: List<Attribute>, source: T, initialisedEntities: List<Attribute>) : List<String> {
        val required = mutableListOf<String>()
        required.addAll(this.protect(source, initialisedEntities))
        logger.trace("protection requirements = {}", required)
        val ruleRequirements = this.resolveRuleDataRequirements(changedAttributes)
        logger.trace("rule requirements = {}", ruleRequirements)
        val filteredEntities = ruleRequirements.filter { it.parent != calculations::class.simpleName && (it.isKotlinType == null || !it.isKotlinType) }
        required.addAll(filteredEntities.map { it.className })

        ruleLogger.flushRules()
        ruleLogger.flushLogToLogger()
        return required
    }

    open fun checkRequirementsFulfilled(changedAttributes: List<Attribute>, source: T, correlationId:String? = null) : List<Attribute>
    {
        val calculations = this.createCalculationsObject()
        val rulesAndDependencies = this.findInScopeRules(changedAttributes)
        val hashesAndNonNullObjects = Hashing().calculateHashes(source)
        val providedEntities = hashesAndNonNullObjects.first

        val filteredEntities = rulesAndDependencies.second.filter {
            it.parent != calculations::class.simpleName
                    && (it.isKotlinType == null || !it.isKotlinType) }

//        check that the source has the required entities
        for (entity in filteredEntities)
        {
            if (entity.className !in providedEntities.map { it.className})
            {
                throw IllegalArgumentException("$entity is required, but was not provided")
            }
        }
        return filteredEntities
    }


    /**
     * Execute the set of in-scope rules for the provided set of changed attributes
     */
    open fun executeRules(changedAttributes: List<Attribute>, source: T, correlationId:String? = null) : C
    {
        val calculations = this.createCalculationsObject()
        val rulesAndDependencies = this.findInScopeRules(changedAttributes)

        logger.trace("executing rules: {}", rulesAndDependencies.first)

        for (rule in rulesAndDependencies.first)
        {
            logger.trace("Executing {}", rule)
            val targetAttribute = calculations::class.memberProperties.first { it.name == rule.getMetadata().output.attribute } as KMutableProperty<*>
            ruleLogger.prepareRule(rule)
            val result = rule.execute(source, calculations)
            ruleLogger.addOutputValue(result)
            ruleLogger.flushRule()
            try {
                targetAttribute.setter.call(calculations, result)
            }
            catch (ex: Exception)
            {
                throw Exception("$rule does not return the expected type.", ex)
            }

        }
        return calculations
    }

}