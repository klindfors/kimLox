package com.craftinginterpreters.lox;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class FlowView extends Application {

    static WaterFlow.RiverSystem system;

    private final Canvas canvas = new Canvas(900, 650);

    @Override
    public void start(Stage stage) {
        Button step = new Button("Simulate day");
        step.setOnAction(e -> {
            system.simulateDay();
            draw();
        });
        stage.setScene(new Scene(new BorderPane(canvas, null, null, step, null)));
        stage.setTitle("kimlox river");
        stage.show();
        draw();
    }

    private int depth(WaterFlow.River r) {
        int d = 0;
        for (WaterFlow.River in : r.inflows) d = Math.max(d, depth(in) + 1);
        return d;
    }

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        Map<WaterFlow.River, double[]> pos = new HashMap<>();
        Map<Integer, Integer> perRow = new HashMap<>();
        for (WaterFlow.River r : system.getRivers()) {
            int d = depth(r);
            int col = perRow.merge(d, 1, Integer::sum) - 1;
            pos.put(r, new double[]{120 + col * 220, 60 + d * 120});
        }

        g.setStroke(Color.STEELBLUE);
        for (WaterFlow.River r : system.getRivers()) {
            double[] to = pos.get(r);
            for (WaterFlow.River in : r.inflows) {
                double[] from = pos.get(in);
                g.setLineWidth(1 + Math.min(20, in.getCurrentFlow() / 10));
                g.strokeLine(from[0], from[1], to[0], to[1]);
            }
        }

        for (WaterFlow.River r : system.getRivers()) {
            double[] p = pos.get(r);
            g.setFill(Color.LIGHTBLUE);
            g.fillOval(p[0] - 18, p[1] - 18, 36, 36);
            g.setFill(Color.BLACK);
            g.fillText(String.format("%s  %.1f", r.name, r.getCurrentFlow()), p[0] + 24, p[1] + 4);
        }

        for (WaterFlow.Dam dam : system.getDams()) {
            WaterFlow.River in = system.findRiver(dam.inflow);
            if (in == null) continue;
            double[] p = pos.get(in);
            double x = p[0] - 25, y = p[1] + 40, w = 50, h = 30;
            double fill = dam.capacity > 0 ? dam.storage / dam.capacity : 0;
            g.setFill(Color.SADDLEBROWN);
            g.fillRect(x, y + h * (1 - fill), w, h * fill);
            g.setStroke(Color.BLACK);
            g.setLineWidth(1);
            g.strokeRect(x, y, w, h);
            g.setFill(Color.BLACK);
            g.fillText(dam.name, x + w + 6, y + 18);
        }
    }
}