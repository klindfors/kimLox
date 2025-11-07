package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    final Environment globals = new Environment();
    private Environment environment = globals;
    private final WaterFlow.RiverSystem riverSystem = new WaterFlow.RiverSystem();
    Interpreter() {
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
    }


    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LoxFunction function = new LoxFunction(stmt, environment);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);

        throw new Return(value);
    }
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }



    // NEW: Handle river definitions
    @Override
    public Void visitRiverDefStmt(Stmt.RiverDef stmt) {
        String riverName = stmt.name.lexeme;

        // Determine flow type
        WaterFlow.FlowType flowType = WaterFlow.FlowType.ROOT; // default
        if (stmt.flowType instanceof Expr.FlowExpr) {
            Expr.FlowExpr flowExpr = (Expr.FlowExpr) stmt.flowType;
            switch (flowExpr.operator.type) {
                case ROOT: flowType = WaterFlow.FlowType.ROOT; break;
                case LESS: flowType = WaterFlow.FlowType.UPSTREAM; break;
                case GREATER: flowType = WaterFlow.FlowType.DOWNSTREAM; break;
            }
        }

        // Parse properties
        List<Double> waterArray = new ArrayList<>();
        double terrainType = 0.5; // default
        double catchmentArea = 1.0; // default

        for (Expr property : stmt.properties) {
            if (property instanceof Expr.Array) {
                // rW[...] water array
                Expr.Array arrayExpr = (Expr.Array) property;
                for (Expr element : arrayExpr.elements) {
                    Object value = evaluate(element);
                    if (value instanceof Double) {
                        waterArray.add((Double) value);
                    }
                }
            } else if (property instanceof Expr.FunctionCall) {
                // T(...) or A(...) function calls
                Expr.FunctionCall funcCall = (Expr.FunctionCall) property;
                String funcName = funcCall.name.lexeme;

                if (!funcCall.arguments.isEmpty()) {
                    Object argValue = evaluate(funcCall.arguments.get(0));
                    if (argValue instanceof Double) {
                        double value = (Double) argValue;
                        if ("T".equals(funcName)) {
                            terrainType = value;
                        } else if ("A".equals(funcName)) {
                            catchmentArea = value;
                        }
                    }
                }
            }
        }

        // Ensure water array has 10 elements
        while (waterArray.size() < 10) {
            waterArray.add(0.0);
        }
        if (waterArray.size() > 10) {
            waterArray = waterArray.subList(0, 10);
        }

        // Create and add river to system
        WaterFlow.River river = new WaterFlow.River(riverName, flowType,
                waterArray, terrainType, catchmentArea);
        riverSystem.addRiver(river);

        // Handle inflows if this is an upstream river
        if (flowType == WaterFlow.FlowType.UPSTREAM && stmt.flowType instanceof Expr.FlowExpr) {
            Expr.FlowExpr flowExpr = (Expr.FlowExpr) stmt.flowType;
            if (flowExpr.operand != null) {
                handleRiverConnections(river, flowExpr.operand);
            }
        }

        System.out.println("Created river: " + river);
        return null;
    }

    @Override
    public Void visitDamDefStmt(Stmt.DamDef stmt) {
        return null;
    }

    // Handle river connections (& combinations)
    private void handleRiverConnections(WaterFlow.River downstreamRiver, Expr connectionExpr) {
        if (connectionExpr instanceof Expr.RiverJoin) {
            Expr.RiverJoin joinExpr = (Expr.RiverJoin) connectionExpr;
            for (Expr riverExpr : joinExpr.rivers) {
                if (riverExpr instanceof Expr.Variable) {
                    String upstreamName = ((Expr.Variable) riverExpr).name.lexeme;
                    WaterFlow.River upstreamRiver = riverSystem.findRiver(upstreamName);
                    if (upstreamRiver != null) {
                        downstreamRiver.addInflow(upstreamRiver);
                    }
                }
            }
        } else if (connectionExpr instanceof Expr.Grouping) {
            handleRiverConnections(downstreamRiver, ((Expr.Grouping) connectionExpr).expression);
        } else if (connectionExpr instanceof Expr.Variable) {
            String upstreamName = ((Expr.Variable) connectionExpr).name.lexeme;
            WaterFlow.River upstreamRiver = riverSystem.findRiver(upstreamName);
            if (upstreamRiver != null) {
                downstreamRiver.addInflow(upstreamRiver);
            }
        }
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                throw new RuntimeError(expr.operator,
                        "Operands must be two numbers or two strings.");
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);
            //add the if  case again
        }
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));

        }

        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren,
                    "Can only call functions and classes.");
        }

        LoxCallable function = (LoxCallable)callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " +
                    function.arity() + " arguments but got " +
                    arguments.size() + ".");
        }

        return function.call(this, arguments);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
        }
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    // NEW: Handle array expressions
    @Override
    public Object visitArrayExpr(Expr.Array expr) {
        List<Object> elements = new ArrayList<>();
        for (Expr element : expr.elements) {
            elements.add(evaluate(element));
        }
        return elements;
    }

    // NEW: Handle function calls (T, A, etc.)
    @Override
    public Object visitFunctionCallExpr(Expr.FunctionCall expr) {
        String functionName = expr.name.lexeme;
        List<Object> arguments = new ArrayList<>();

        for (Expr arg : expr.arguments) {
            arguments.add(evaluate(arg));
        }

        // Handle built-in water flow functions
        switch (functionName) {
            case "T":
                if (!arguments.isEmpty() && arguments.get(0) instanceof Double) {
                    return arguments.get(0); // Return terrain type value
                }
                break;
            case "A":
                if (!arguments.isEmpty() && arguments.get(0) instanceof Double) {
                    return arguments.get(0); // Return area value
                }
                break;
            case "simulate":
                // Built-in function to simulate the river system
                riverSystem.simulateDay(new ArrayList<>());
                return null;
            case "status":
                // Built-in function to print system status
                riverSystem.printSystemStatus();
                return null;
            case "rain":
                // Built-in function to add rainfall: rain("river_name", amount)
                if (arguments.size() >= 2 && arguments.get(0) instanceof String && arguments.get(1) instanceof Double) {
                    String riverName = (String) arguments.get(0);
                    double amount = (Double) arguments.get(1);
                    WaterFlow.River river = riverSystem.findRiver(riverName);
                    if (river != null) {
                        river.addRainfall(amount);
                        System.out.println("Added " + amount + "mm rainfall to " + riverName);
                    }
                }
                return null;
        }

        throw new RuntimeError(expr.name, "Unknown function '" + functionName + "'.");
    }

    // NEW: Handle flow expressions (^, <, >)
    @Override
    public Object visitFlowExprExpr(Expr.FlowExpr expr) {
        // This is mainly used during river definition parsing
        return expr.operator.lexeme + (expr.operand != null ? " " + stringify(evaluate(expr.operand)) : "");
    }

    // NEW: Handle river joins (&)
    @Override
    public Object visitRiverJoinExpr(Expr.RiverJoin expr) {
        List<String> riverNames = new ArrayList<>();
        for (Expr riverExpr : expr.rivers) {
            if (riverExpr instanceof Expr.Variable) {
                riverNames.add(((Expr.Variable) riverExpr).name.lexeme);
            }
        }
        return String.join(" & ", riverNames);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            kimLox.runtimeError(error);
        }
    }

    // Public method to access the river system for interactive use
    public WaterFlow.RiverSystem getRiverSystem() {
        return riverSystem;
    }
}