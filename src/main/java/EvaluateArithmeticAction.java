import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.project.Project;
import groovy.lang.GroovyRuntimeException;
import groovy.util.Eval;
import org.jetbrains.annotations.NotNull;


public class EvaluateArithmeticAction extends AnAction {
    private String lastExpressionResult = "0";

    /**
     * Evaluates the selection at each caret as a mathematical expression
     * and replaces the content of the selection with the answer.
     * @param event  Event related to this action
     */
    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        final Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
        final Project project = event.getRequiredData(CommonDataKeys.PROJECT);
        final Document document = editor.getDocument();

        WriteCommandAction.runWriteCommandAction(project, () -> {
            lastExpressionResult = "0";
            int enumeration = 0;
            for (Caret caret : editor.getCaretModel().getAllCarets()) {
                String new_text = caret.getSelectedText();
                if (new_text != null) {
                    document.replaceString(
                            caret.getSelectionStart(),
                            caret.getSelectionEnd(),
                            evaluate(new_text, enumeration)
                    );
                    enumeration++;
                }
                caret.removeSelection();
            }
        });
    }

    /**
     * Evaluate a given string as an arithmetic expression and return the result as a string
     * @param expression_string The string to evaluate as an arithmetic expression
     * @return The result of the evaluation if possible or the unchanged string if not
     */
    String evaluate(@NotNull String expression_string) {
        return evaluate(expression_string, 0);
    }

     String evaluate(@NotNull String expression_string, int numeration) {
        // Only allow arithmetic characters and whitespace
        String expressionToEvaluate = expression_string.replaceAll("\\$", lastExpressionResult);
        expressionToEvaluate = expressionToEvaluate.replaceAll("#", String.valueOf(numeration));
        if (expressionToEvaluate.matches("^[0-9+\\-/*^().\\s]*(=\\s*)?")) {
            try {
                final boolean appendResultToExpression = expressionToEvaluate.contains("=");
                // Normalise all whitespace to spaces, remove the optional equal sign (=).
                // This means groovy expects a single expression
                String new_string = expressionToEvaluate.replaceAll("\\s|=", " ");

                String answer = String.valueOf(Eval.me(new_string));
                if (answer.contains(".")) {
                    // Strip all trailing zeroes after decimal point
                    answer = answer.replaceAll("0*$","");

                    // Remove the decimal point if there's nothing left after it
                    answer = answer.replaceAll("\\.$","");

                }
                lastExpressionResult = answer;
                return appendResultToExpression ? (expressionToEvaluate + answer) : answer;
            } catch (GroovyRuntimeException e) {
                // The expression was not a valid arithmetic expression
                return expressionToEvaluate;
            }
        }
        return expression_string;
    }

    @Override
    public void update(@NotNull final AnActionEvent event) {
        final Project project = event.getProject();
        final Editor editor = event.getData(CommonDataKeys.EDITOR);
        event.getPresentation().setEnabledAndVisible(
            project != null &&
            editor != null &&
            editor.getSelectionModel().hasSelection()
        );
    }
}