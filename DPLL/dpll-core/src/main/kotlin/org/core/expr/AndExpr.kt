package org.core.expr

class AndExpr(val elements: List<Expr>) : Expr() {
    init {
        require(elements.size > 1) {
            "AndExpr elements size must be > 1 but was [${elements.size}]"
        }
    }

    override fun anyInSubExpression(predicate: (e: Expr) -> Boolean): Boolean {
        return predicate(this) || elements.any { predicate(it) }
    }
}
