package org.core.expr

abstract class Expr {
    abstract fun anyInSubExpression(predicate: (e: Expr) -> Boolean): Boolean
}
