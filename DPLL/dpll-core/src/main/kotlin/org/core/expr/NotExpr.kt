package org.core.expr

class NotExpr(val element: Expr) : Expr() {
    override fun anyInSubExpression(predicate: (e: Expr) -> Boolean): Boolean {
        return predicate(this) || predicate(element)
    }
}
