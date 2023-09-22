package org.solver.dpll

import org.core.expr.AndExpr
import org.core.expr.Expr
import org.core.expr.LitExpr
import org.core.expr.NotExpr
import org.core.expr.OrExpr
import org.core.model.Model
import org.core.status.SolverStatus
import org.core.status.SolverStatus.SAT
import org.core.status.SolverStatus.UNSAT
import org.solver.dpll.util.SolverUtil
import org.solver.dpll.util.TseytinTransformation

class DPLLSolver {
    private val tseytinTransformation = TseytinTransformation()
    private val disjuncts: MutableList<Expr> = mutableListOf()

    fun assert(expr: Expr) {
        disjuncts.add(expr)
    }

    fun check(): Pair<SolverStatus, Model?> {
        val model = mutableListOf<Pair<String, Boolean>>()

        if (!SolverUtil.isCNFFormula(disjuncts)) {
            val expressionToCnf = if (disjuncts.size == 1) disjuncts[0] else AndExpr(disjuncts)
            val newDisjunctions = tseytinTransformation.toCNF(expressionToCnf)
            disjuncts.clear()
            disjuncts.addAll(newDisjunctions)
        }

        val uniqueLiterals = SolverUtil.getAllUniqueLiterals(disjuncts)

        uniqueLiterals.forEach {
            if (it is LitExpr) {
                if (!model.any { x -> x.first == it.name }) {
                    model.add(Pair(it.name, false))
                }
            } else if (it is NotExpr) {
                val lit = (it.element as LitExpr).name
                if (!model.any { x -> x.first == lit }) {
                    model.add(Pair(lit, false))
                }
            }
        }

        return processDPLL(disjuncts, model)
    }

    private fun processDPLL(
        disjuncts: List<Expr>,
        assignments: MutableList<Pair<String, Boolean>>,
    ): Pair<SolverStatus, Model?> {
        var disjunctsList: MutableList<Expr?> = disjuncts.toMutableList()
        var model = assignments.toMutableList()

        if (disjunctsList.isEmpty()) {
            return Pair(SAT, Model(model))
        }

        if (disjunctsList.any { it == null }) {
            return Pair(UNSAT, null)
        }

        while (disjunctsList.any { SolverUtil.isLiteral(it!!) }) {
            val literal = disjunctsList.first { SolverUtil.isLiteral(it!!) }

            disjunctsList = unitPropagate(disjunctsList as MutableList<Expr>, literal!!).toMutableList()

            updateModel(model, literal)

            if (disjunctsList.any { it == null }) {
                return Pair(UNSAT, null)
            }
        }

        while (true) {
            val literal = SolverUtil.getPureLiteralOrNull(disjunctsList as List<Expr>) ?: break

            disjunctsList = eliminatePureLiteral(disjunctsList, literal).toMutableList()
            disjunctsList.removeIf { it == null }
            model = updateModel(model, literal)
        }

        val chosenLiteral = chooseLiteralSymbolOrNull(disjunctsList as List<Expr>)

        return if (processDPLL(
                disjunctsList + LitExpr(chosenLiteral!!),
                updateModel(model, LitExpr(chosenLiteral)),
            ).first == SAT
        ) {
            Pair(SAT, Model(model))
        } else {
            processDPLL(
                disjuncts + NotExpr(LitExpr(chosenLiteral)),
                updateModel(model, NotExpr(LitExpr(chosenLiteral))),
            )
        }
    }

    /**
     * Eliminate pure literal.
     */
    private fun eliminatePureLiteral(disjuncts: List<Expr?>, literal: Expr): List<Expr?> {
        return disjuncts.map { eliminateLiteral(it, literal) }
    }

    private fun unitPropagate(disjuncts: MutableList<Expr>, literal: Expr): List<Expr?> {
        require(literal is LitExpr || literal is NotExpr && literal.element is LitExpr) {
            "Element must be literal"
        }

        val disjunctsWithoutLiteral = disjuncts.filter {
            !SolverUtil.disjunctContaintsLiteral(literal, it)
        }

        val negLiteral = if (literal is LitExpr) NotExpr(literal) else (literal as NotExpr).element

        return eliminatePureLiteral(disjunctsWithoutLiteral, negLiteral)
    }

    private fun updateModel(
        assignments: MutableList<Pair<String, Boolean>>,
        literal: Expr,
    ): MutableList<Pair<String, Boolean>> {
        val stringLiteral = if (literal is LitExpr) literal.name else ((literal as NotExpr).element as LitExpr).name

        assignments.removeIf { it.first == stringLiteral }
        val valueToAssign = literal is LitExpr

        assignments.add(Pair(stringLiteral, valueToAssign))

        return assignments
    }

    private fun clear() {
        disjuncts.clear()
    }

    companion object {
        /**
         * Eliminates literal or its negation.
         */
        private fun eliminateLiteral(disjunct: Expr?, literal: Expr): Expr? {
            require(literal is LitExpr || literal is NotExpr && literal.element is LitExpr) {
                "Element must be literal"
            }

            if (disjunct == null) {
                return null
            }

            return when (disjunct) {
                is LitExpr -> {
                    if (literal is LitExpr && disjunct.name == literal.name) null else disjunct
                }

                is NotExpr -> {
                    if (literal is NotExpr &&
                        (disjunct.element as LitExpr).name == (literal.element as LitExpr).name
                    ) {
                        null
                    } else {
                        disjunct
                    }
                }

                is OrExpr -> {
                    val newDisjuncts = disjunct.elements.mapNotNull { eliminateLiteral(it, literal) }
                    if (newDisjuncts.size > 1) {
                        OrExpr(newDisjuncts)
                    } else if (newDisjuncts.size == 1) {
                        newDisjuncts[0]
                    } else {
                        null
                    }
                }

                else -> error("Unexpected type")
            }
        }

        private fun chooseLiteralSymbolOrNull(disjuncts: List<Expr>): String? {
            val exprContainingLiteral =
                disjuncts.find { it is LitExpr || it is OrExpr && it.elements.any { x -> x is LitExpr } }
                    ?: return null

            return when (exprContainingLiteral) {
                is LitExpr -> exprContainingLiteral.name

                is OrExpr -> {
                    val literal = exprContainingLiteral.elements.first { it is LitExpr }
                    (literal as LitExpr).name
                }

                else -> error("Unexpected type")
            }
        }
    }
}
