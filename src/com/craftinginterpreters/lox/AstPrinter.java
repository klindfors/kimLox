package com.craftinginterpreters.lox;

class AstPrinter implements Expr.Visitor<String> {
    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return parenthesize("assign " + expr.name.lexeme, expr.value);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.lexeme;
    }

    @Override
    public String visitArrayExpr(Expr.Array expr) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < expr.elements.size(); i++) {
            if (i > 0) builder.append(", ");
            builder.append(expr.elements.get(i).accept(this));
        }
        builder.append("]");
        return builder.toString();
    }

    @Override
    public String visitFunctionCallExpr(Expr.FunctionCall expr) {
        StringBuilder builder = new StringBuilder();
        builder.append(expr.name.lexeme).append("(");
        for (int i = 0; i < expr.arguments.size(); i++) {
            if (i > 0) builder.append(", ");
            builder.append(expr.arguments.get(i).accept(this));
        }
        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visitFlowExprExpr(Expr.FlowExpr expr) {
        if (expr.operand != null) {
            return parenthesize(expr.operator.lexeme, expr.operand);
        }
        return expr.operator.lexeme;
    }

    @Override
    public String visitRiverJoinExpr(Expr.riverJoin expr) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < expr.rivers.size(); i++) {
            if (i > 0) builder.append(" & ");
            builder.append(expr.rivers.get(i).accept(this));
        }
        return builder.toString();
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);
        for (Expr expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");

        return builder.toString();
    }

    public static void main(String[] args) {
        // Test water flow language constructs
        Expr expression = new Expr.Binary(
                new Expr.Unary(
                        new Token(TokenType.MINUS, "-", null, 1),
                        new Expr.Literal(123)),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Grouping(
                        new Expr.Literal(45.67)));

        System.out.println(new AstPrinter().print(expression));

        // Test array expression
        java.util.List<Expr> arrayElements = java.util.Arrays.asList(
                new Expr.Literal(11.4),
                new Expr.Literal(11.5),
                new Expr.Literal(0.0)
        );
        Expr arrayExpr = new Expr.Array(arrayElements);
        System.out.println("Array: " + new AstPrinter().print(arrayExpr));

        // Test function call
        java.util.List<Expr> funcArgs = java.util.Arrays.asList(new Expr.Literal(0.56));
        Expr funcCall = new Expr.FunctionCall(new Token(TokenType.T, "T", null, 1), funcArgs);
        System.out.println("Function: " + new AstPrinter().print(funcCall));
    }
}