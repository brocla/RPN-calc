package com.brocla.rpn_calc.logic.model

import kotlinx.serialization.Serializable

@Serializable
data class Stack(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val t: Double = 0.0
) {
    fun lift(): Stack = Stack(x = x, y = x, z = y, t = z)
    fun drop(): Stack = Stack(x = y, y = z, z = t, t = t)
    fun rollDown(): Stack = Stack(x = y, y = z, z = t, t = x)
    fun swap(): Stack = Stack(x = y, y = x, z = z, t = t)
    fun withX(v: Double): Stack = Stack(x = v, y = y, z = z, t = t)
    fun applyBinaryResult(result: Double): Stack = Stack(x = result, y = z, z = t, t = t)
    fun applyUnaryResult(result: Double): Stack = Stack(x = result, y = y, z = z, t = t)
}
