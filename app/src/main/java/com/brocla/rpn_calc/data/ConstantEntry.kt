package com.brocla.rpn_calc.data

enum class ConstantCategory(val displayName: String) {
    MATHEMATICS("Mathematics"),
    PHYSICS_UNIVERSAL("Physics — Universal"),
    PHYSICS_ELECTROMAGNETIC("Physics — Electromagnetic"),
    PHYSICS_ATOMIC("Physics — Atomic"),
    CHEMISTRY("Chemistry"),
    ASTRONOMY("Astronomy"),
    ENGINEERING("Engineering"),
    MATERIALS("Materials"),
}

sealed class ConstantEntry {
    abstract val category: ConstantCategory

    /** A single named constant — appears flat under its category. */
    data class Simple(
        val name: String,
        val symbol: String,   // may be empty
        val value: Double,
        val unit: String,     // may be empty
        override val category: ConstantCategory,
    ) : ConstantEntry()

    /**
     * One property of a named body or material — appears under a two-level
     * expand/collapse within its category (groupName → propertyName → value).
     *
     * Example: groupName="Mercury", propertyName="Mass", value=3.3011e23, unit="kg"
     * Example: groupName="Steel — 1020 HR", propertyName="Yield Strength", value=210.0, unit="MPa"
     */
    data class Grouped(
        val groupName: String,
        val propertyName: String,
        val value: Double,
        val unit: String,
        override val category: ConstantCategory,
    ) : ConstantEntry()
}
