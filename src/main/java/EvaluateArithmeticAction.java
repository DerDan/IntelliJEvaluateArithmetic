import com.fathzer.soft.javaluator.DoubleEvaluator;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class EvaluateArithmeticAction extends AnAction {
    private static final DoubleEvaluator evaluator = new DoubleEvaluator() {
        @Override
        protected Double toValue(String literal, Object evaluationContext) {
            if (literal.matches("0[xX][0-9a-fA-F]+")) {
                return (double) Integer.parseInt(literal.substring(2), 16);
            } else {
                return super.toValue(literal, evaluationContext);
            }
        }
    };

    private String previous_expression_result = "0";

    /**
     * Evaluates the selection at each caret as a mathematical expression
     * and replaces the content of the selection with the answer.
     *
     * @param event Event related to this action
     */
    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        final Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
        final Project project = event.getRequiredData(CommonDataKeys.PROJECT);
        final Document document = editor.getDocument();

        WriteCommandAction.runWriteCommandAction(project, () -> {
            List<Caret> carets = editor.getCaretModel().getAllCarets();
            List<String> selectedTexts = carets.stream()
                    .map(Caret::getSelectedText)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            List<String> results = processSelections(selectedTexts);
            for (int i = 0; i < carets.size(); i++) {
                Caret caret = carets.get(i);
                if (caret.getSelectedText() != null) {
                    document.replaceString(caret.getSelectionStart(), caret.getSelectionEnd(), results.get(i));
                }
                caret.removeSelection();
            }
        });
    }

    /**
     * Evaluate a given string as an arithmetic expression and return the result as a string
     *
     * @param expression_string The string to evaluate as an arithmetic expression
     * @return The result of the evaluation if possible or the unchanged string if not
     */
    String evaluate(@NotNull String expression_string) {
        return evaluate(expression_string, 1);
    }

    /**
     * Evaluate a given string as an arithmetic expression and return the result as a string
     *
     * @param expression_string  The string to evaluate as an arithmetic expression
     * @param index_of_selection An index of current selection to process
     * @return The result of the evaluation if possible or the unchanged string if not
     */

    String evaluate(@NotNull String expression_string, int index_of_selection) {
        // replace $ with the previous expression result
        String expression_to_evaluate = expression_string.replaceAll("\\$", previous_expression_result);
        // replace # with the index of selection
        expression_to_evaluate = expression_to_evaluate.replaceAll("#", String.valueOf(index_of_selection));
        // Only allow arithmetic characters and whitespace
        if (expression_to_evaluate.matches("^[xX0-9+\\-/*^().\\s]*(=\\s*)?")) {
            try {
                final boolean append_result_to_expression = expression_to_evaluate.contains("=");
                // Normalise all whitespace to spaces, remove the optional equal sign (=).
                // This means groovy expects a single expression
                String new_string = expression_to_evaluate.replaceAll("\\s|=", " ");

                Double value_double = evaluator.evaluate(new_string);
                long value_long = value_double.longValue();
                String answer;
                if (value_long == value_double) {
                    // avoid scientific notation
                    answer = String.valueOf(value_long);
                } else {
                    answer = String.valueOf(value_double);
                    if (answer.contains(".")) {
                        // Strip all trailing zeroes after decimal point
                        answer = answer.replaceAll("0*$", "");

                        // Remove the decimal point if there's nothing left after it
                        answer = answer.replaceAll("\\.$", "");

                    }
                }
                previous_expression_result = answer;
                return append_result_to_expression ? (expression_to_evaluate + answer) : answer;
            } catch (IllegalArgumentException e) {
                // The expression was not a valid arithmetic expression
                return expression_string;
            }
        }
        return expression_string;
    }

    List<String> processSelections(List<String> selections) {
        if (selections.size() > 1 && selections.stream().allMatch(this::isNumber)) {
            return applySumToSelections(selections);
        }
        previous_expression_result = "0";
        List<String> results = new java.util.ArrayList<>();
        int index = 1;
        for (String sel : selections) {
            results.add(evaluate(sel, index));
            index++;
        }
        return results;
    }

    protected List<String> applySumToSelections(List<String> selections) {
        double sum = selections.stream().mapToDouble(this::parseNumber).sum();
        String sumStr = formatNumber(sum);
        List<String> result = new java.util.ArrayList<>(selections);
        String last = selections.get(selections.size() - 1).trim();
        result.set(result.size() - 1, (last.isEmpty() ? "" : last + " ") + "= " + sumStr);
        return result;
    }

    boolean isNumber(String text) {
        if (text == null) return false;
        String t = text.trim();
        return t.isEmpty() || t.matches("0[xX][0-9a-fA-F]+") || t.matches("-?\\d+(\\.\\d+)?");
    }

    double parseNumber(String text) {
        String t = text.trim();
        if (t.isEmpty()) return 0;
        if (t.matches("0[xX][0-9a-fA-F]+")) {
            return (double) Long.parseLong(t.substring(2), 16);
        }
        return Double.parseDouble(t);
    }

    String formatNumber(double value) {
        long valueLong = (long) value;
        if (valueLong == value) {
            return String.valueOf(valueLong);
        }
        String result = String.valueOf(value);
        if (result.contains(".")) {
            result = result.replaceAll("0*$", "").replaceAll("\\.$", "");
        }
        return result;
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