package com.metroos.launcher.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metroos.launcher.ui.theme.MetroOrange
import com.metroos.launcher.ui.theme.MetroTeal

/**
 * Modern typographic calculator. Expression engine is a shunting-yard
 * parser with correct precedence, unary minus and parentheses.
 */
@Composable
fun CalculatorApp() {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070E))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                expression.ifEmpty { "0" },
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 34.sp,
                fontWeight = FontWeight.Light
            )
            Text(
                result,
                color = MetroTeal,
                fontSize = 46.sp,
                fontWeight = FontWeight.Light
            )
        }

        Spacer(Modifier.height(18.dp))

        val rows = listOf(
            listOf("C", "(", ")", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("%", "0", ".", "=")
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { key ->
                        CalcKey(
                            label = key,
                            accent = key == "=",
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            onClick = {
                                when (key) {
                                    "C" -> { expression = ""; result = "" }
                                    "=" -> {
                                        result = runCatching {
                                            val v = CalcEngine.evaluate(expression)
                                            formatResult(v)
                                        }.getOrElse { "Error" }
                                    }
                                    else -> expression += key
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalcKey(label: String, accent: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(if (accent) MetroOrange else Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Light
        )
    }
}

private fun formatResult(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else "%.6f".format(v).trimEnd('0').trimEnd('.')

/**
 * Shunting-yard expression evaluator supporting + - × ÷ % ( ) and unary minus.
 */
object CalcEngine {

    fun evaluate(expr: String): Double {
        val tokens = tokenize(expr)
        val rpn = toPostfix(tokens)
        return evalRpn(rpn)
    }

    private sealed interface Tok
    private data class Num(val value: Double) : Tok
    private data class Op(val symbol: String) : Tok
    private data class Paren(val open: Boolean) : Tok

    private fun precedence(op: String): Int = when (op) {
        "+", "−" -> 1
        "×", "÷", "%" -> 2
        "u-" -> 3
        else -> 0
    }

    private fun tokenize(expr: String): List<Tok> {
        val tokens = mutableListOf<Tok>()
        var i = 0
        var prev: Tok? = null
        while (i < expr.length) {
            val ch = expr[i]
            when {
                ch.isDigit() || ch == '.' -> {
                    val sb = StringBuilder()
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        sb.append(expr[i]); i++
                    }
                    val num = Num(sb.toString().toDoubleOrNull() ?: 0.0)
                    tokens.add(num); prev = num
                    continue
                }
                ch in "+-×÷%" -> {
                    val symbol = when (ch) {
                        '-' -> if (prev == null || prev is Op || (prev is Paren && prev.open)) "u-" else "−"
                        '*' -> "×"
                        '/' -> "÷"
                        else -> ch.toString()
                    }
                    val op = Op(symbol)
                    tokens.add(op); prev = op
                }
                ch == '(' -> { val p = Paren(true); tokens.add(p); prev = p }
                ch == ')' -> { val p = Paren(false); tokens.add(p); prev = p }
            }
            i++
        }
        return tokens
    }

    private fun toPostfix(tokens: List<Tok>): List<Tok> {
        val out = mutableListOf<Tok>()
        val stack = ArrayDeque<String>()
        tokens.forEach { tok ->
            when (tok) {
                is Num -> out.add(tok)
                is Paren -> {
                    if (tok.open) stack.addLast("(")
                    else {
                        while (stack.isNotEmpty() && stack.last() != "(") out.add(Op(stack.removeLast()))
                        stack.removeLastOrNull()
                    }
                }
                is Op -> {
                    while (stack.isNotEmpty() && stack.last() != "(" &&
                        precedence(stack.last()) >= precedence(tok.symbol)
                    ) {
                        out.add(Op(stack.removeLast()))
                    }
                    stack.addLast(tok.symbol)
                }
            }
        }
        while (stack.isNotEmpty()) {
            if (stack.last() != "(") out.add(Op(stack.removeLast())) else stack.removeLast()
        }
        return out
    }

    private fun evalRpn(rpn: List<Tok>): Double {
        val stack = ArrayDeque<Double>()
        rpn.forEach { tok ->
            when (tok) {
                is Num -> stack.addLast(tok.value)
                is Op -> {
                    if (tok.symbol == "u-") {
                        val a = stack.removeLastOrNull() ?: 0.0
                        stack.addLast(-a)
                    } else {
                        val b = stack.removeLastOrNull() ?: 0.0
                        val a = stack.removeLastOrNull() ?: 0.0
                        stack.addLast(
                            when (tok.symbol) {
                                "+" -> a + b
                                "−" -> a - b
                                "×" -> a * b
                                "÷" -> if (b == 0.0) throw ArithmeticException("division by zero") else a / b
                                "%" -> if (b == 0.0) throw ArithmeticException("mod by zero") else a % b
                                else -> throw IllegalStateException("bad op")
                            }
                        )
                    }
                }
                is Paren -> throw IllegalStateException("unbalanced parentheses")
            }
        }
        return stack.removeLastOrNull() ?: 0.0
    }
}
