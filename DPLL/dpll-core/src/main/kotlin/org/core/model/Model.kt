package org.core.model

class Model(val assignments: List<Pair<String, Boolean>>) {
    fun literalIsEqualTrue(literal: String): Boolean {
        require(assignments.any { it.first == literal }) {
            "Literal with the name [$literal] does not exist in model"
        }

        return assignments.first { it.first == literal }.second
    }
}
