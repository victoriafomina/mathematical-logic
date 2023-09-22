package org.solver.dpll.util

import org.core.expr.AndExpr
import org.core.expr.Expr
import org.core.expr.LitExpr
import org.core.expr.NotExpr
import org.core.expr.OrExpr

internal class TseytinTransformation {
    private var nextIndexForVar = 0
    private var newVariablesReification = mutableListOf<Expr>()

    fun toCNF(expression: Expr): List<Expr> {
        nextIndexForVar = 0
        newVariablesReification.clear()
        return toCNFRec(expression, emptyList()).second
    }

    private fun toCNFRec(expression: Expr, disjunctions: List<Expr>): Pair<Expr, List<Expr>> {
        if (expression is LitExpr) {
            return Pair(expression, disjunctions)
        }

        if (expression is NotExpr) {
            val subExprResult = toCNFRec(expression.element, disjunctions)
            return Pair(getSimplifiedNotExpr(subExprResult.first), subExprResult.second)
        }

        if (expression is AndExpr) {
            val left = expression.elements[0]

            val rightElements = expression.elements.subList(1, expression.elements.size)
            val right = if (rightElements.size == 1) rightElements.first() else AndExpr(rightElements)

            val leftResult = toCNFRec(left, disjunctions)
            val rightResult = toCNFRec(right, leftResult.second)

            val newVar = LitExpr("*$nextIndexForVar")
            ++nextIndexForVar

            val newElements = listOf(
                OrExpr(listOf(NotExpr(newVar), leftResult.first)),
                OrExpr(listOf(NotExpr(newVar), rightResult.first)),
                OrExpr(listOf(getSimplifiedNotExpr(leftResult.first), getSimplifiedNotExpr(rightResult.first), newVar)),
            )

            return Pair(newVar, rightResult.second + newElements)
        }

        if (expression is OrExpr) {
            val left = expression.elements[0]

            val rightElements = expression.elements.subList(1, expression.elements.size)
            val right = if (rightElements.size == 1) rightElements.first() else OrExpr(rightElements)

            val leftResult = toCNFRec(left, disjunctions)
            val rightResult = toCNFRec(right, leftResult.second)

            val newVar = LitExpr("*$nextIndexForVar")
            ++nextIndexForVar

            val newElements = listOf(
                OrExpr(listOf(NotExpr(newVar), leftResult.first, rightResult.first)),
                OrExpr(listOf(getSimplifiedNotExpr(leftResult.first), newVar)),
                OrExpr(listOf(getSimplifiedNotExpr(rightResult.first), newVar)),
            )

            return Pair(newVar, rightResult.second + newElements)
        }

        error("Unexpected type")
    }

    companion object {
        private fun getSimplifiedNotExpr(expr: Expr): Expr {
            return if (expr is NotExpr) {
                expr.element
            } else {
                NotExpr(expr)
            }
        }
    }
}
