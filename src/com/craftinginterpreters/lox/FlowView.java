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

    private final Canvas canvas = new Canvas(1200, 320);

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
        for (WaterFlow.Dam dam : system.getDams()) {
            if (r.name.equals(dam.outflowTarget)) d = Math.max(d, depth(dam) + 1);
        }
        return d;
    }

    private int depth(WaterFlow.Dam dam) {
        WaterFlow.River in = system.findRiver(dam.inflow);
        return in == null ? 0 : depth(in) + 1;
    }

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        Map<WaterFlow.River, double[]> pos = new HashMap<>();
        Map<WaterFlow.Dam, double[]> damPos = new HashMap<>();
        Map<Integer, Integer> perCol = new HashMap<>();
        for (WaterFlow.River r : system.getRivers()) {
            int d = depth(r);
            int row = perCol.merge(d, 1, Integer::sum) - 1;
            pos.put(r, new double[]{60 + d * 170, 80 + row * 160});
        }
        for (WaterFlow.Dam dam : system.getDams()) {
            int d = depth(dam);
            int row = perCol.merge(d, 1, Integer::sum) - 1;
            damPos.put(dam, new double[]{60 + d * 170, 80 + row * 160});
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

        for (WaterFlow.Dam dam : system.getDams()) {
            double[] p = damPos.get(dam);
            WaterFlow.River in = system.findRiver(dam.inflow);
            WaterFlow.River out = system.findRiver(dam.outflowTarget == null ? "" : dam.outflowTarget);
            if (in != null) {
                g.setLineWidth(1 + Math.min(20, in.getCurrentFlow() / 10));
                g.strokeLine(pos.get(in)[0], pos.get(in)[1], p[0], p[1]);
            }
            if (out != null) {
                g.setLineWidth(1 + Math.min(20, dam.getOutflow() / 10));
                g.strokeLine(p[0], p[1], pos.get(out)[0], pos.get(out)[1]);
            }
        }

        for (WaterFlow.River r : system.getRivers()) {
            double[] p = pos.get(r);
            g.setFill(Color.LIGHTBLUE);
            g.fillOval(p[0] - 18, p[1] - 18, 36, 36);
            g.setFill(Color.BLACK);
            g.fillText(r.name, p[0] - 18, p[1] - 26);
            g.fillText(String.format("%.1f", r.getCurrentFlow()), p[0] - 18, p[1] + 34);
        }

        for (WaterFlow.Dam dam : system.getDams()) {
            double[] p = damPos.get(dam);
            double x = p[0] - 25, y = p[1] - 15, w = 50, h = 30;
            double fill = dam.capacity > 0 ? dam.storage / dam.capacity : 0;
            g.setFill(Color.WHITE);
            g.fillRect(x, y, w, h);
            g.setFill(Color.SADDLEBROWN);
            g.fillRect(x, y + h * (1 - fill), w, h * fill);
            g.setStroke(Color.BLACK);
            g.setLineWidth(1);
            g.strokeRect(x, y, w, h);
            g.setFill(Color.BLACK);
            g.fillText(dam.name, x, y - 8);
        }
    }
}