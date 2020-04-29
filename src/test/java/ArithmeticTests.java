import org.junit.Test;

import static org.junit.Assert.*;

public class ArithmeticTests {

    @Test
    public void canDoAllArithmeticOperations() {
        EvaluateArithmeticAction action = new EvaluateArithmeticAction();
        assertEquals("3", action.evaluate("1 + 2"));
        assertEquals("-1", action.evaluate("1 - 2"));
        assertEquals("6", action.evaluate("3 * 2"));
        assertEquals("2", action.evaluate("4 / 2"));
    }

    @Test
    public void canOmitWhitespace() {
        EvaluateArithmeticAction action = new EvaluateArithmeticAction();
        assertEquals("3", action.evaluate("1+2"));
        assertEquals("-1", action.evaluate("1-2"));
        assertEquals("6", action.evaluate("3*2"));
        assertEquals("2", action.evaluate("4/2"));
    }

    @Test
    public void canHaveLeadingAndTrailingWhitespace() {
        EvaluateArithmeticAction action = new EvaluateArithmeticAction();
        assertEquals("3", action.evaluate(" 1+2 "));
        assertEquals("3", action.evaluate("      \n      1+2  \n  \t"));
    }

    @Test
    public void canHaveInternalNewlines() {
        EvaluateArithmeticAction action = new EvaluateArithmeticAction();
        assertEquals("3", action.evaluate("1+\n2"));
    }

    @Test
    public void canUseDecimalsInInput() {
        EvaluateArithmeticAction action = new EvaluateArithmeticAction();
        assertEquals("3", action.evaluate("1.5 * 2"));
    }

    @Test
    public void canGetDecimalAnswers() {
        EvaluateArithmeticAction action = new EvaluateArithmeticAction();
        // We do not guarantee any particular precision, just check that it has at least, say,  5 places
        assertTrue(action.evaluate("2 / 3").startsWith("0.66666"));
    }

    @Test
    public void precendenceWorks() {
        EvaluateArithmeticAction action = new EvaluateArithmeticAction();
        assertEquals("7", action.evaluate("1 + 2 * 3"));
        assertEquals("9", action.evaluate("(1 + 2) * 3"));
    }

    @Test
    public void canUsePreviousResult() {
        EvaluateArithmeticAction action = new EvaluateArithmeticAction();
        assertEquals("14", action.evaluate("5+9"));
        assertEquals("14", action.evaluate("$"));
        assertEquals("15", action.evaluate("$+1"));
    }

    @Test
    public void canEnumerateSelection() {
        EvaluateArithmeticAction action = new EvaluateArithmeticAction();
        assertEquals("0", action.evaluate("# * 2", 0));
        assertEquals("2", action.evaluate("# * 2", 1));
        assertEquals("4", action.evaluate("# * 2", 2));
    }


    @Test
    public void canAppendResultToExpression() {
        EvaluateArithmeticAction action = new EvaluateArithmeticAction();
        assertEquals("1 + 1 =2", action.evaluate("1 + 1 ="));
        // add whitespace after "="
        assertEquals("5 * 5 = 25", action.evaluate("5 * 5 = "));
        assertEquals("5\t*\t5\t=\n25", action.evaluate("5\t*\t5\t=\n"));
    }

    @Test
    public void doesNotEvaluateWrongExpression() {
        EvaluateArithmeticAction action = new EvaluateArithmeticAction();
        assertEquals("1 + 1 = X", action.evaluate("1 + 1 = X"));
        assertEquals("2 * 2 X", action.evaluate("2 * 2 X"));
    }


    @Test
    public void doesNotEvaluateMultipleExpressions() {
        EvaluateArithmeticAction action = new EvaluateArithmeticAction();
        assertEquals("1 + 2 3 + 4", action.evaluate("1 + 2 3 + 4"));
        assertEquals("1 + 2\n3 + 4", action.evaluate("1 + 2\n3 + 4"));
        assertEquals("1 + 2 = 3 + 4", action.evaluate("1 + 2 = 3 + 4"));
    }
}