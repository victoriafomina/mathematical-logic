package org.solver.dpll.util

import org.core.expr.Expr
import org.core.expr.LitExpr
import org.core.expr.NotExpr
import org.core.expr.OrExpr

object SolverUtil {
    fun isLiteral(expr: Expr): Boolean =
        expr is LitExpr || expr is NotExpr && expr.element is LitExpr

    fun disjunctContaintsLiteral(literal: Expr, expr: Expr): Boolean {
        when (expr) {
            is LitExpr -> return literal is LitExpr && literal.name == expr.name

            is NotExpr ->
                return literal is NotExpr &&
                    literal.element is LitExpr &&
                    (literal.element as LitExpr).name == (expr.element as LitExpr).name

            is OrExpr -> return expr.elements.any { areEqualLiterals(it, literal) }
        }

        return false
    }

    fun areEqualLiterals(expr1: Expr, expr2: Expr): Boolean {
        if (expr1 is LitExpr && expr2 is LitExpr) {
            return expr1.name == expr2.name
        } else if (expr1 is NotExpr && expr1.element is LitExpr &&
            expr2 is NotExpr && expr2.element is LitExpr
        ) {
            return (expr1.element as LitExpr).name == (expr2.element as LitExpr).name
        }

        return false
    }

    fun hasComplementaryLiteral(literal: Expr, disjunct: Expr): Boolean {
        require(literal is LitExpr || literal is NotExpr && (literal.element is LitExpr)) {
            "Literal is expected"
        }

        when (disjunct) {
            is LitExpr ->
                return literal is NotExpr && (literal.element as LitExpr).name == disjunct.name

            is NotExpr -> return literal is LitExpr && literal.name == (disjunct.element as LitExpr).name

            is OrExpr -> return disjunct.elements.any { hasComplementaryLiteral(literal, it) }
        }

        return false
    }

    fun getAllUniqueLiterals(disjuncts: List<Expr>): List<Expr> {
        val uniqueLiterals = mutableListOf<Expr>()

        disjuncts.forEach { expr ->
            when (expr) {
                is LitExpr -> addLiteralIfNotContains(expr, uniqueLiterals)

                is NotExpr -> addLiteralIfNotContains(expr, uniqueLiterals)

                is OrExpr -> {
                    expr.elements.forEach { addLiteralIfNotContains(it, uniqueLiterals) }
                }
            }
        }

        return uniqueLiterals
    }

    fun getPureLiteralOrNull(disjuncts: List<Expr>): Expr? {
        val literals = getAllUniqueLiterals(disjuncts)
        var pureLiteral: Expr? = null

        for (i in 0..literals.lastIndex) {
            val lit = literals[i]
            if (disjuncts.all { !hasComplementaryLiteral(lit, it) }) {
                pureLiteral = lit
                break
            }
        }

        return pureLiteral
    }

    fun isCNFFormula(disjuncts: List<Expr>): Boolean = disjuncts.all {
        when (it) {
            is LitExpr -> true

            is NotExpr -> it.element is LitExpr

            is OrExpr -> it.elements.all { e -> isLiteral(e) }

            else -> false
        }
    }

    private fun addLiteralIfNotContains(literal: Expr, literals: MutableList<Expr>) {
        require(literal is LitExpr || literal is NotExpr && (literal.element is LitExpr)) {
            "Literal is expected"
        }

        if (!literals.any { areEqualLiterals(it, literal) }) {
            literals.add(literal)
        }
    }
}
