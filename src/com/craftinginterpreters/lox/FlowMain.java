package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FlowMain {
    private static final Interpreter interpreter = new Interpreter();
    private static WaterFlow.RiverSystem riverSystem;
    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    public static void main(String[] args) throws IOException {
        System.out.println("Waterflow Interpreter");
        System.out.println();

        if (args.length > 1) {
            System.out.println("Usage: java FlowMain [exampleprogram]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runInteractiveMode();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));



        // Indicate an error in the exit code.
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);

        //after running file, enter interactive mode
        if (!hadError && !hadRuntimeError) {
            riverSystem = interpreter.getRiverSystem();
            runInteractiveSimulation();
        }
    }

    private static void runInteractiveMode() throws IOException {
        System.out.println("Enter river system definitions (type 'done' when finished):");
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        StringBuilder systemDefinition = new StringBuilder();
        String line;

        while (!(line = reader.readLine()).equals("done")) {
            if (line == null) break;
            systemDefinition.append(line).append("\n");
        }
        if (systemDefinition.length() > 0) {
            run(systemDefinition.toString());
            riverSystem = interpreter.getRiverSystem();
        }
        if (!hadError && !hadRuntimeError) {
            runInteractiveSimulation();
        }
    }
    private static void runInteractiveSimulation() throws IOException {
        System.out.println("\n~*~*~ Interactive River Flow Simulator ~*~*~");
        System.out.println("Available commands:");
        System.out.println("  status                    - Show current system status");
        System.out.println("  rain <river> <amount>     - Add rainfall to a river");
        System.out.println("  simulate                  - Run one day of simulation");
        System.out.println("  simulate <days>           - Run multiple days");
        System.out.println("  flow <river>              - Show flow for specific river");
        System.out.println("  help                      - Show this help");
        System.out.println("  exit                      - Exit simulation");
        System.out.println();

        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        while (true) {
            System.out.print("water> ");
            String line = reader.readLine();
            if (line == null || line.equals("exit")) break;

            processInteractiveCommand(line.trim());
        }
    }

    private static void processInteractiveCommand(String command) {
        String[] parts = command.split("\\s+");
        if (parts.length == 0) return;

        switch (parts[0].toLowerCase()) {
            case "status":
                if (riverSystem != null) {
                    riverSystem.printSystemStatus();
                } else {
                    System.out.println("No river system defined.");
                }
                break;

            case "rain":
                if (parts.length >= 3) {
                    String riverName = parts[1];
                    try {
                        double amount = Double.parseDouble(parts[2]);
                        WaterFlow.River river = riverSystem.findRiver(riverName);
                        if (river != null) {
                            river.addRainfall(amount);
                            System.out.println("Added " + amount + "mm rainfall to " + riverName);
                        } else {
                            System.out.println("River '" + riverName + "' not found.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid rainfall amount: " + parts[2]);
                    }
                } else {
                    System.out.println("Usage: rain <river_name> <amount_mm>");
                }
                break;

            case "simulate":
                if (riverSystem != null) {
                    int days = 1;
                    if (parts.length >= 2) {
                        try {
                            days = Integer.parseInt(parts[1]);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid number of days: " + parts[1]);
                            return;
                        }
                    }

                    for (int i = 0; i < days; i++) {
                        System.out.println("~*~*~ Simulating Day " + (i + 1) + " ~*~*~");
                        riverSystem.simulateDay(new ArrayList<>());
                        // Status is now printed automatically by simulateDay()
                    }
                    riverSystem.printSystemStatus();

                } else {
                    System.out.println("No river system defined.");
                }
                break;

            case "flow":
                if (parts.length >= 2 && riverSystem != null) {
                    String riverName = parts[1];
                    WaterFlow.River river = riverSystem.findRiver(riverName);
                    if (river != null) {
                        System.out.println(riverName + " current flow: " +
                                String.format("%.2f", river.getCurrentFlow()) + " L/s");
                        System.out.println("Water array: " + river.waterArray);
                        System.out.println("Terrain factor: " + river.terrainType);
                        System.out.println("Catchment area: " + river.catchmentArea + " hectares");
                        if (!river.inflows.isEmpty()) {
                            System.out.println("Inflows from: " +
                                    river.inflows.stream()
                                            .map(r -> r.name)
                                            .reduce((a, b) -> a + ", " + b)
                                            .orElse("none"));
                        }
                    } else {
                        System.out.println("River '" + riverName + "' not found.");
                    }
                } else {
                    System.out.println("Usage: flow <river_name>");
                }
                break;

            case "help":
                System.out.println("Available commands:");
                System.out.println("  status                    - Show current system status");
                System.out.println("  rain <river> <amount>     - Add rainfall to a river");
                System.out.println("  simulate                  - Run one day of simulation");
                System.out.println("  simulate <days>           - Run multiple days");
                System.out.println("  flow <river>              - Show flow for specific river");
                System.out.println("  help                      - Show this help");
                System.out.println("  exit                      - Exit simulation");
                break;

            case "":
                break; // Empty command, do nothing

            default:
                System.out.println("Unknown command: " + parts[0] + ". Type 'help' for available commands.");
                break;
        }
    }


    private static void printDailyFlows(int day) {
        System.out.println("~*~*~*~*~*~*~*~*~*~*~*~*~");
        System.out.println("Day " + day + " results:");
        System.out.println("~*~*~*~*~*~*~*~*~*~*~*~*~");

        if (riverSystem == null) {
            System.out.println("No river system defined.");
            System.out.println("~*~*~*~*~*~*~*~*~*~*~*~*~");
            return;
        }

        // Print all river flows
        System.out.println("Rivers:");
        for (WaterFlow.River river : riverSystem.getRivers()) {
            System.out.printf("  %-12s : %.2f L/s%n", river.name, river.getCurrentFlow());
        }

        // Print all dam flows
        if (riverSystem.getDams() != null && !riverSystem.getDams().isEmpty()) {
            System.out.println("\nDams:");
            for (WaterFlow.Dam dam : riverSystem.getDams()) {
                System.out.printf("  %-12s : %.2f L/s (storage %.2f / %.2f)%n",
                        dam.name, dam.getOutflow());
            }
        }

        System.out.println("~*~*~*~*~*~*~*~*~*~*~*~*~");
        System.out.println();
    }


    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();



        System.out.println("=== DEBUG: SCANNED TOKENS ==="); //good grief
        for (Token t : tokens) {
            System.out.println(t.type + " -> '" + t.lexeme + "'");
        }
        System.out.println("==============================");


        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();
        // Stop if there was a syntax error.
        if (hadError) return;
        interpreter.interpret(statements);
    } //kolla om det finns några errors, annars kör i vind.

    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }
}