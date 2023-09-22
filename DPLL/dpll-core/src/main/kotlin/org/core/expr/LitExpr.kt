package org.core.expr

class LitExpr(val name: String) : Expr() {
    override fun anyInSubExpression(predicate: (e: Expr) -> Boolean): Boolean {
        return predicate(this)
    }
}
