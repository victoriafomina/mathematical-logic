package org.core.expr

class OrExpr(val elements: List<Expr>) : Expr() {
    init {
        require(elements.size > 1) {
            "OrExpr elements size must be > 1 but was [${elements.size}]"
        }
    }

    override fun anyInSubExpression(predicate: (e: Expr) -> Boolean): Boolean {
        return predicate(this) || elements.any { predicate(it) }
    }
}
