package com.brocla.rpn_calc.ui.calculator

import javax.inject.Inject

interface ClipboardParser {
    sealed interface Result {
        data class Success(val value: Double) : Result
        data object Invalid : Result
    }
    fun parse(raw: String): Result
}

class ClipboardParserImpl @Inject constructor() : ClipboardParser {
    override fun parse(raw: String): ClipboardParser.Result {
        // 1. Strip chars not in [0-9 . , + - E e]
        val stripped = raw.filter { it in "0123456789.,+-Ee" }
        // 2. Remove commas
        val noCommas = stripped.replace(",", "")
        if (noCommas.isEmpty()) return ClipboardParser.Result.Invalid
        // 3. Attempt toDoubleOrNull()
        val value = noCommas.toDoubleOrNull() ?: return ClipboardParser.Result.Invalid
        return ClipboardParser.Result.Success(value)
    }
}
