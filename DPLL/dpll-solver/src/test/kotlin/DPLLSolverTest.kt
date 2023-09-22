import org.core.expr.AndExpr
import org.core.expr.LitExpr
import org.core.expr.NotExpr
import org.core.expr.OrExpr
import org.core.status.SolverStatus.SAT
import org.core.status.SolverStatus.UNSAT
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.solver.dpll.DPLLSolver

class DPLLSolverTest {
    private lateinit var solver: DPLLSolver

    @BeforeEach
    fun initTests() {
        solver = DPLLSolver()
    }

    @Test
    fun smokeTest() {
        val lit = LitExpr("a")
        solver.assert(lit)

        val (status, model) = solver.check()

        assertTrue(status == SAT)
        assertTrue(model!!.assignments.any { it.first == "a" && it.second })
    }

    @Test
    fun twoLiteralsTest() {
        val lit1 = LitExpr("a")
        val lit2 = LitExpr("b")
        solver.assert(lit1)
        solver.assert(lit2)

        val (status, model) = solver.check()

        assertTrue(status == SAT)
        assertTrue(model!!.literalIsEqualTrue(lit1.name))
        assertTrue(model.literalIsEqualTrue(lit2.name))
    }

    @Test
    fun literalAndNotLiteralTest() {
        val lit1 = LitExpr("a")
        val lit2 = NotExpr(LitExpr("b"))
        solver.assert(lit1)
        solver.assert(lit2)

        val (status, model) = solver.check()

        assertTrue(status == SAT)
        assertTrue(model!!.literalIsEqualTrue(lit1.name))
        assertTrue(!model.literalIsEqualTrue((lit2.element as LitExpr).name))
    }

    @Test
    fun notLiteralAndLiteralTest() {
        val lit1 = LitExpr("a")
        val lit2 = NotExpr(LitExpr("b"))
        solver.assert(lit2)
        solver.assert(lit1)

        val (status, model) = solver.check()

        assertTrue(status == SAT)
        assertTrue(model!!.literalIsEqualTrue(lit1.name))
        assertFalse(model.literalIsEqualTrue((lit2.element as LitExpr).name))
    }

    @Test
    fun orExprTest() {
        val litA = LitExpr("a")
        val notLitB = NotExpr(LitExpr("b"))
        val orExpr = OrExpr(listOf(litA, notLitB))
        solver.assert(orExpr)

        val (status, model) = solver.check()

        assertTrue(status == SAT)
        assertTrue(model!!.literalIsEqualTrue("a"))
        assertFalse(model.literalIsEqualTrue("b"))
    }

    @Test
    fun simpleUnsatFormulaTest() {
        val litA = LitExpr("a")
        val litB = LitExpr("b")
        val notLitA = NotExpr(litA)
        val notLitB = NotExpr(litB)
        solver.assert(OrExpr(listOf(litA, litB)))
        solver.assert(OrExpr(listOf(litA, notLitB)))
        solver.assert(OrExpr(listOf(notLitA, litB)))
        solver.assert(OrExpr(listOf(notLitA, notLitB)))

        val (status, _) = solver.check()

        assertTrue(status == UNSAT)
    }

    @Test
    fun unsatFormulaTest() {
        val first = OrExpr(listOf(AndExpr(listOf(NotExpr(LitExpr("a")), LitExpr("b"))), NotExpr(LitExpr("c"))))
        val second = OrExpr(listOf(AndExpr(listOf(NotExpr(LitExpr("d")), LitExpr("e"))), NotExpr(LitExpr("f"))))
        solver.assert(OrExpr(listOf(first, second)))
        solver.assert(OrExpr(listOf(first, NotExpr(second))))
        solver.assert(OrExpr(listOf(NotExpr(first), second)))
        solver.assert(OrExpr(listOf(NotExpr(first), NotExpr(second))))

        val (status, model) = solver.check()

        assertTrue(status == UNSAT)
    }
}
