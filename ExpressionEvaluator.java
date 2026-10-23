package phase4;

/**
 * Evaluates arithmetic expressions using manually implemented operator and value stacks.
 */
public class ExpressionEvaluator {
    private ExpressionEvaluator() {
    }

    /**
     * Evaluates an infix expression with parentheses and the +, -, *, and / operators.
     *
     * @param expression arithmetic expression to evaluate
     * @return the calculated value
     * @throws IllegalArgumentException if the expression is invalid or divides by zero
     */
    public static double evaluate(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Enter an arithmetic expression.");
        }

        ValueStack values = new ValueStack(expression.length() + 1);
        OperatorStack operators = new OperatorStack(expression.length() + 1);
        boolean expectingOperand = true;
        int index = 0;

        while (index < expression.length()) {
            char current = expression.charAt(index);
            if (Character.isWhitespace(current)) {
                index++;
                continue;
            }

            if (Character.isDigit(current) || current == '.') {
                if (!expectingOperand) {
                    throw new IllegalArgumentException("Place an operator between numbers.");
                }
                int start = index;
                boolean decimalPointSeen = false;
                while (index < expression.length()) {
                    char digit = expression.charAt(index);
                    if (Character.isDigit(digit)) {
                        index++;
                    } else if (digit == '.' && !decimalPointSeen) {
                        decimalPointSeen = true;
                        index++;
                    } else {
                        break;
                    }
                }
                String number = expression.substring(start, index);
                if (".".equals(number)) {
                    throw new IllegalArgumentException("A decimal point must have digits.");
                }
                values.push(Double.parseDouble(number));
                expectingOperand = false;
                continue;
            }

            if (current == '(') {
                if (!expectingOperand) {
                    throw new IllegalArgumentException("Place an operator before the parenthesis.");
                }
                operators.push(current);
                index++;
                continue;
            }

            if (current == ')') {
                if (expectingOperand) {
                    throw new IllegalArgumentException("A closing parenthesis needs an operand.");
                }
                while (!operators.isEmpty() && operators.peek() != '(') {
                    applyTopOperator(values, operators);
                }
                if (operators.isEmpty()) {
                    throw new IllegalArgumentException("There is no matching opening parenthesis.");
                }
                operators.pop();
                while (!operators.isEmpty() && isUnary(operators.peek())) {
                    applyTopOperator(values, operators);
                }
                expectingOperand = false;
                index++;
                continue;
            }

            if (isOperator(current)) {
                if (expectingOperand) {
                    if (current != '+' && current != '-') {
                        throw new IllegalArgumentException("An operand is missing before the operator.");
                    }
                    operators.push(current == '-' ? '~' : '#');
                    index++;
                    continue;
                }
                while (!operators.isEmpty() && operators.peek() != '('
                        && precedence(operators.peek()) >= precedence(current)) {
                    applyTopOperator(values, operators);
                }
                operators.push(current);
                expectingOperand = true;
                index++;
                continue;
            }

            throw new IllegalArgumentException("Unsupported character: " + current);
        }

        if (expectingOperand) {
            throw new IllegalArgumentException("The expression ends before an operand.");
        }
        while (!operators.isEmpty()) {
            if (operators.peek() == '(') {
                throw new IllegalArgumentException("There is no matching closing parenthesis.");
            }
            applyTopOperator(values, operators);
        }
        if (values.size() != 1) {
            throw new IllegalArgumentException("The expression is not valid.");
        }
        double result = values.pop();
        if (Double.isNaN(result) || Double.isInfinite(result)) {
            throw new IllegalArgumentException("The result is outside the supported range.");
        }
        return result;
    }

    private static void applyTopOperator(ValueStack values, OperatorStack operators) {
        char operator = operators.pop();
        if (isUnary(operator)) {
            if (values.isEmpty()) {
                throw new IllegalArgumentException("A unary operator needs an operand.");
            }
            double value = values.pop();
            values.push(operator == '~' ? -value : value);
            return;
        }
        if (values.size() < 2) {
            throw new IllegalArgumentException("An operator needs two operands.");
        }
        double right = values.pop();
        double left = values.pop();
        switch (operator) {
            case '+':
                values.push(left + right);
                break;
            case '-':
                values.push(left - right);
                break;
            case '*':
                values.push(left * right);
                break;
            case '/':
                if (right == 0.0) {
                    throw new IllegalArgumentException("Division by zero is not allowed.");
                }
                values.push(left / right);
                break;
            default:
                throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
    }

    private static boolean isOperator(char character) {
        return character == '+' || character == '-' || character == '*'
                || character == '/';
    }

    private static boolean isUnary(char operator) {
        return operator == '~' || operator == '#';
    }

    private static int precedence(char operator) {
        if (isUnary(operator)) {
            return 3;
        }
        if (operator == '*' || operator == '/') {
            return 2;
        }
        return 1;
    }

    private static final class ValueStack {
        private final double[] values;
        private int top = -1;

        private ValueStack(int capacity) {
            values = new double[capacity];
        }

        private void push(double value) {
            values[++top] = value;
        }

        private double pop() {
            return values[top--];
        }

        private int size() {
            return top + 1;
        }

        private boolean isEmpty() {
            return top < 0;
        }
    }

    private static final class OperatorStack {
        private final char[] operators;
        private int top = -1;

        private OperatorStack(int capacity) {
            operators = new char[capacity];
        }

        private void push(char operator) {
            operators[++top] = operator;
        }

        private char pop() {
            return operators[top--];
        }

        private char peek() {
            return operators[top];
        }

        private boolean isEmpty() {
            return top < 0;
        }
    }
}
