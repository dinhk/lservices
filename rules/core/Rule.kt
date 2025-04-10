package com.qut.webservices.igalogicservices.rules.core

import com.qut.webservices.igalogicservices.dataaccess.IReverseLookupDataAccess
import com.qut.webservices.igalogicservices.dataaccess.ReverseLookupDataAccessStub
import com.qut.webservices.igalogicservices.utils.getBasicTypeDefaultValue
import org.mockito.Mockito
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import kotlin.reflect.KProperty

/**
 * Abstract class and basis for all rules
 * Implementors have the choice of providing rule metadata by overriding the getMetadata method, or
 * by using the changedAttribute, requiredAttribute and returns methods in the execute method
 */
abstract class Rule<S, C>
        where S : Any {

    private val lock = Any()
    val logger: Logger = LoggerFactory.getLogger(Rule::class.java)
    @Autowired
    lateinit var ruleLogger: RuleLogger
    @Autowired
    lateinit var reversLookup: IReverseLookupDataAccess
    protected var metadataCollectionMode: Boolean = false
    private var source: S? = null
    private var calculations: C? = null
    val triggers = mutableListOf<Attribute>()
    val requirements = mutableListOf<Attribute>()
    var result: Attribute? = null
    private var ruleMetadata: RuleMetadata? = null

    fun setMetaDataCollectionMode(source: S, calculations: C) {
        metadataCollectionMode = true
        this.source = source
        this.calculations = calculations
    }

    private fun endMetaDataCollection() {
        metadataCollectionMode = false
    }

    fun inMetaDataCollectionMode(): Boolean {
        return metadataCollectionMode
    }

    /**
     * Extract the set of source attributes required and the target attribute of the Rule by executing the rule
     * and examining the resulting set of triggers and result.
     * This metadata is used to determine the execute order of the rules and to determine what, if any, extra
     * data is required to compute a business rule given a data change.
     *
     * Relies on the implementation of the execute method of the Rule being written in this style:
     *
     *
     *          val currentCourses = changedAttribute(calculations.currentCourses, calculations::currentCourses)
     *          val person = changedAttribute(source.person, source::person)
     *          val citizenship = changedAttribute(person.citizenship, person::citizenship)
     *          returns(calculations::offshoreInternationalStudent)
     *
     *         if (currentCourses.isNotEmpty())
     *         {
     *             if (citizenship == null)
     *             {
     *                 return false
     *             }
     *             return citizenship in listOf("5")
     *         }
     *         return false
     *
     * Alternatively, Rule implementors can override this method to provide the metadata in this form:
     *
     *    override fun getMetadata(): RuleMetadata {
     *         return RuleMetadata(
     *             listOf(
     *                 Attribute("Person.citizenship"),
     *                 CalculatedAttribute("Calculated.currentCourses")
     *             ),
     *             Attribute("Calculated.onshoreInternationalStudent")
     *         )
     *     }
     */
    open fun getMetadata(): RuleMetadata {
        synchronized(lock) {
            if (ruleMetadata == null) {
                triggers.clear()
                result = null
                ruleLogger.prepareRule(this)
                if (!metadataCollectionMode || source == null || calculations == null) {
                    throw Exception("The default getMetadata requires a call to setMetaDataCollectionMode first")
                }
                execute(source!!, calculations!!)

                if (triggers.isEmpty()) {
                    throw Exception("A rule needs at least one trigger")
                }

                if (result == null) {
                    throw Exception("A rule needs to set an attribute value")
                }

                ruleMetadata = RuleMetadata(triggers, requirements, result!!)
                endMetaDataCollection()
                ruleLogger.flushRule()
            }
        }
        return ruleMetadata as RuleMetadata
    }


    /**
     * The Rule must define business logic which uses one or more attributes from the source or calculations object AND must write at least one calculated attribute
     * If not directly overriding getMetadata, the rule should declare dependencies and outputs is this style:
     *
     *
     *          val currentCourses = changedAttribute(calculations.currentCourses, calculations::currentCourses)
     *          val person = changedAttribute(source.person, source::person)
     *          val citizenship = changedAttribute(person.citizenship, person::citizenship)
     *          returns(calculations::offshoreInternationalStudent)
     *
     *         if (currentCourses.isNotEmpty())
     *         {
     *             if (citizenship == null)
     *             {
     *                 return false
     *             }
     *             return citizenship in listOf("5")
     *         }
     *         return false
     */
    abstract fun execute(source: S, calculations: C): Any?

    /**
     * Used to declare the attribute set by the rule.
     * TIP: find all rules where this output is used as you would any variable or object property. Navigate to the definition of property and find usages.
     *
     */
    protected fun returns(property: KProperty<*>) {
        if (metadataCollectionMode) {
            if (result != null) {
                throw Exception("Only a single attribute can be set per rule.")
            }
            result = Attribute(property)
            ruleLogger.addOutput(result!!)
        } else {
            ruleLogger.addOutput(Attribute(property))
        }
    }

    /**
     * Used to declare the attribute set by the rule
     */
    protected inline fun <reified T> ruleResult(type: T?, property: KProperty<*>): T where T : Any {
        return if (metadataCollectionMode) {
            if (result != null) {
                throw Exception("Only a single attribute can be set per rule.")
            }
            result = Attribute(property)
            ruleLogger.addOutput(result!!)
            val classToMock = T::class.java
            createStub<T>(property, classToMock)
        } else {
            ruleLogger.addOutput(Attribute(property))
            type!!
        }
    }

    /**
     * Used to declare a collection is returned by the rule (at metadata collection time) and returns the provided instance during normal execution
     */
    protected inline fun <reified T> ruleResult(instance: Array<T>?, property: KProperty<*>): Array<T> where T : Any {

        if (metadataCollectionMode) {
            if (result != null) {
                throw Exception("Only a single attribute can be set per rule.")
            }
            result = Attribute(property)
            ruleLogger.addOutput(result!!)
            val list = mutableListOf<T>()
            list.add(com.qut.webservices.igalogicservices.utils.createStub<T>())
            return list.toTypedArray()
        } else {
            ruleLogger.addOutput(Attribute(property))
            return instance!!
        }

    }

    /**
     * Used to declare a dependency on an attribute. When a dependency is declared, the Rules Engine
     * ensures that:
     * - any rule which populates the dependency is executed before this rule AND
     * - the caller (Boomi) is notified that the attribute is required (if not already provided) to execute the in-scope rules.
     * For example: the rule IsOffShoreInternationStudent requires both Student Course data and
     * Student (header) data. In the case where a data change event is raised because the Student's courses
     * have changed, the ChangeRouter service should notify Boomi that the Student header data is required.
     * At metadata collection time, the property data is read and the rules engine generates stub data conforming to the
     * property's return type.
     * At execution time the provided instance value is logged and passed back to the caller.
     */
    protected inline fun <reified T> changedAttribute(instance: T?, property: KProperty<*>): T where T : Any {
        return if (metadataCollectionMode) {
            triggers.add(Attribute(property))
            ruleLogger.addTrigger(Attribute(property))
            val classToMock = T::class.java
            createStub<T>(property, classToMock)
        } else {
            ruleLogger.addTrigger(Attribute(property), instance)
            instance!!
        }
    }


    /**
     * Used to declare a trigger for the rule when the attribute is nullable.
     * When a trigger is declared, the Rules Engine
     * ensures that:
     * - any rule which populates the dependency is executed before this rule AND
     * - the caller (Boomi) is notified that the attribute is required (if not already provided) to execute the in-scope rules.
     * For example: the rule IsOffShoreInternationStudent requires both Student Course data and
     * Student Citizenship data. In the case where a data change event is raised because the Student's courses
     * have changed, the ChangeRouter service should notify Boomi Student Citizenship data is required.
     * At metadata collection time, the property data is read and the rules engine generates stub data conforming to the
     * property's return type.
     * At execution time the provided instance value is logged and passed back to the caller.
     */
    protected inline fun <reified T> changedNullableAttribute(instance: T?, property: KProperty<*>): T? where T : Any {

        return if (metadataCollectionMode) {
            triggers.add(Attribute(property))
            ruleLogger.addTrigger(Attribute(property))
            val classToMock = T::class.java
            createStub<T>(property, classToMock)
        } else {
            ruleLogger.addTrigger(Attribute(property), instance)
            instance
        }
    }


    /**
     * Used to declare a trigger for a rule when the attribute is a collection.
     * When a trigger is declared, the Rules Engine
     * ensures that:
     * - any rule which populates the dependency is executed before this rule AND
     * - the caller (Boomi) is notified that the attribute is required (if not already provided) to execute the in-scope rules.
     * For example: the rule IsOffShoreInternationStudent requires both Student Course data and
     * Student Citizenship data. In the case where a data change event is raised because the Student's courses
     * have changed, the ChangeRouter service should notify Boomi Student Citizenship data is required.
     * At metadata collection time, the property data is read and the rules engine generates stub data conforming to the
     * property's return type.
     * At execution time the provided instance value is logged and passed back to the caller.
     * */
    protected inline fun <reified T> changedAttribute(
        instance: Array<T>?,
        property: KProperty<*>
    ): Array<T> where T : Any {
        return if (metadataCollectionMode) {
            ruleLogger.addTrigger(Attribute(property))
            triggers.add(Attribute(property))
            val d = mutableListOf<T>()
            d.add(com.qut.webservices.igalogicservices.utils.createStub<T>())
            d.toTypedArray()
        } else {
            ruleLogger.addTrigger(Attribute(property), instance)
            //            Boomi should have delivered at least an empty collection
            instance!!
        }
    }

    /**
     * Used to declare a data dependency on an attribute. When a dependency is declared, the Rules Engine
     * ensures that the caller (Boomi) is notified at the resolveDataRequirements stage that this attribute is required.
     * This method differs from the changedAttribute methods in that the rule is not executed on the basis of a change to the attribute.
     * The dependency is only realised, if a changedAttribute dependency is first realised.
     * At metadata collection time, the property data is read and the rules engine generates stub data conforming to the
     * property's return type.
     * At execution time the provided instance value is logged and passed back to the caller.
     */
    protected inline fun <reified T> requiredAttribute(instance: T?, property: KProperty<*>): T where T : Any {
        return if (metadataCollectionMode) {
            requirements.add(Attribute(property))
            ruleLogger.addTrigger(Attribute(property))
            val classToMock = T::class.java
            createStub<T>(property, classToMock)
        } else {
            ruleLogger.addTrigger(Attribute(property), instance)
            instance!!
        }
    }


    /**
     * Used to declare a data dependency on a nullable attribute. When a dependency is declared, the Rules Engine
     * ensures that the caller (Boomi) is notified at the resolveDataRequirements stage that this attribute is required.
     * This method differs from the changedAttribute methods in that the rule is not executed on the basis of a change to the attribute.
     * The dependency is only realised, if a changedAttribute dependency is first realised.
     * At metadata collection time, the property data is read and the rules engine generates stub data conforming to the
     * property's return type.
     * At execution time the provided instance value is logged and passed back to the caller.
     */
    protected inline fun <reified T> requiredNullableAttribute(instance: T?, property: KProperty<*>): T? where T : Any {

        return if (metadataCollectionMode) {
            requirements.add(Attribute(property))
            ruleLogger.addTrigger(Attribute(property))
            val classToMock = T::class.java
            createStub<T>(property, classToMock)
        } else {
            ruleLogger.addTrigger(Attribute(property), instance)
            instance
        }
    }


    /**
     * Used to declare a data dependency on an attribute which is a collection. When a dependency is declared, the Rules Engine
     * ensures that the caller (Boomi) is notified at the resolveDataRequirements stage that this attribute is required.
     * This method differs from the changedAttribute methods in that the rule is not executed on the basis of a change to the attribute.
     * The dependency is only realised, if a changedAttribute dependency is first realised.
     * At metadata collection time, the property data is read and the rules engine generates stub data conforming to the
     * property's return type.
     * At execution time the provided instance value is logged and passed back to the caller.
     * */
    protected inline fun <reified T> requiredAttribute(
        instance: Array<T>?,
        property: KProperty<*>
    ): Array<T> where T : Any {
        return if (metadataCollectionMode) {
            ruleLogger.addTrigger(Attribute(property))
            requirements.add(Attribute(property))
            val d = mutableListOf<T>()
            d.add(com.qut.webservices.igalogicservices.utils.createStub<T>())
            d.toTypedArray()
        } else {
            ruleLogger.addTrigger(Attribute(property), instance)
            //            Boomi should have delivered at least an empty collection
            instance!!
        }
    }

    override fun toString(): String {
        return this::class.simpleName.toString()
    }

    /**
     * Populate an object with enough data to ensure that when the Rule is executed in Metadata collection mode
     * that null reference exceptions are not generated
     */
    protected inline fun <reified T> createStub(property: KProperty<*>, classToMock: Class<T>): T where T : Any {
        return if (property.returnType.toString().startsWith("kotlin")) {
            val basicValue = getBasicTypeDefaultValue(property.returnType)
            if (basicValue == null) {
                if (property.returnType.toString().contains("Array")) {
                    val componentType = T::class.java.componentType
                    java.lang.reflect.Array.newInstance(
                        componentType,
                        0
                    ) as T // Unable to use createStub as Arrays are not supported
                } else {
                    Mockito.mock(classToMock)
                }
            } else {
                basicValue as T
            }
        } else {
            com.qut.webservices.igalogicservices.utils.createStub<T>()
        }
    }

    protected fun reverseLookup(): IReverseLookupDataAccess {
        return if (metadataCollectionMode) {
            ReverseLookupDataAccessStub()
        } else {
            reversLookup
        }
    }
}

class RuleMetadata(val triggers: List<Attribute>, val requirements: List<Attribute>, val output: Attribute)

