package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;



class WaterFlow {

    static class River {
        final String name;
        final FlowType flowType;
        final List<Double> waterArray;  //rW[...] - rainwater from last 10 days!
        final double terrainType;       //T(...) - terrain type, affects drain rate of water in cathcment area
        final double catchmentArea;     //A(...) - size of the area, affects drain rate
        final List<River> inflows;
        River outflow;

        River(String name, FlowType flowType, List<Double> waterArray,
              double terrainType, double catchmentArea) {
            this.name = name;
            this.flowType = flowType;
            this.waterArray = new ArrayList<>(waterArray);
            this.terrainType = terrainType;
            this.catchmentArea = catchmentArea;
            this.inflows = new ArrayList<>();
            this.outflow = null;
        }

        void addInflow(River inflow) {
            inflows.add(inflow);
            inflow.outflow = this;
        }

        double getCurrentFlow() {
            double totalFlow = 0;

            for (int i = 0; i < waterArray.size(); i++) {
                double waterAmount = waterArray.get(i);

                // Recent water contributes more to flow (index 0 = most recent)
                double recencyWeight = (10.0 - i) / 10.0; // 1.0 to 0.1

                totalFlow += waterAmount * recencyWeight;
            }

            // Apply area effect: larger catchment can channel more flow
            double areaFlowMultiplier = Math.log(1 + catchmentArea / 10.0);

            // Terrain affects flow speed but not the drainage calculation
            double terrainFlowMultiplier = 0.8 + (terrainType * 0.4); // 0.8 to 1.2

            return totalFlow * areaFlowMultiplier * terrainFlowMultiplier;
        }


        // Add rainfall to the catchment
        void addRainfall(double rainfall) {
            // Remove oldest day, add new rainfall
            if (waterArray.size() >= 10) {
                waterArray.remove(0);
            }
            // Just add rainfall amount - no area multiplication
            waterArray.add(rainfall);
        }



        void drainTerrain() {
            for (int i = 0; i < waterArray.size(); i++) {
                double currentWater = waterArray.get(i);
                if (currentWater <= 0) continue;

                double drainageRate = calculateDrainageRate(i);
                double remainingWater = currentWater * (1.0 - drainageRate);

                waterArray.set(i, Math.max(0, remainingWater));
            }
        }

        // In the River class, ADD this new method:
        private double calculateDrainageRate(int dayIndex) {
            // dayIndex: 0 = most recent, 9 = oldest (10th day)

            // Force complete drainage on day 10 (index 9)
            if (dayIndex == 9) {
                return 1.0; // Drain 100%
            }

            // Calculate base drainage that ensures 10-day rule
            // We need a formula where cumulative drainage over 10 days = 100%
            // Using exponential decay: each day drains a percentage

            // Base rate ensures water is mostly gone by day 9
            double baseRate = 0.15; // 15% per day base

            // Age factor: older water drains faster
            double ageFactor = 1.0 + (dayIndex * 0.1); // 1.0 to 1.9 multiplier

            // Area factor: larger area = slower drainage (inverse relationship)
            double areaFactor = 50.0 / (50.0 + catchmentArea); // Normalize around 50 hectares

            // Terrain factor: more stony (higher value) = faster drainage
            double terrainFactor = 1.0 + terrainType; // 1.0 to 2.0 multiplier

            double finalRate = baseRate * ageFactor * areaFactor * terrainFactor;

            // Cap at 95% to prevent complete drainage before day 10
            return Math.min(0.95, finalRate);
        }




        @Override
        public String toString() {
            return String.format("River{%s, flow=%.2f, terrain=%.2f, area=%.2f}",
                    name, getCurrentFlow(), terrainType, catchmentArea);
        }
    }

    /**
     * Types of flow operators
     */
    enum FlowType {
        ROOT,        // ^ - root river (no inflows, only rain)
        UPSTREAM,    // < - receives from upstream
        DOWNSTREAM   // > - flows to downstream (less common)
    }

    /**
     * Represents the entire river system
     */
    static class RiverSystem {
        private final List<River> rivers;
        private int currentDay;

        RiverSystem() {
            this.rivers = new ArrayList<>();
            this.currentDay = 0;
        }

        void addRiver(River river) {
            rivers.add(river);
        }

        River findRiver(String name) {
            for (River river : rivers) {
                if (river.name.equals(name)) {
                    return river;
                }
            }
            return null;
        }

        // simulate one day of the whole river system
        void simulateDay(List<RainfallEvent> rainfallEvents) {
            currentDay++;

            // 1. rain event happens
            for (RainfallEvent event : rainfallEvents) {
                River river = findRiver(event.riverName);
                if (river != null) {
                    river.addRainfall(event.amount);
                }
            }

            // 2. drain terrain water into the river
            for (River river : rivers) {
                river.drainTerrain();
            }

            // 3. water flows downstream
            for (River river : rivers) {
                if (river.outflow != null) {
                    double flow = river.getCurrentFlow();

                }
            }

            printSystemStatus();


        }

        void printSystemStatus() {
            System.out.println("~*~*~ Day " + currentDay + " River System Status ~*~*~");
            for (River river : rivers) {
                System.out.println(river);
            }
        }

        List<River> getRivers() {
            return new ArrayList<>(rivers);
        }
    }

    //rainfall event on a specific river/catchment area

    static class RainfallEvent {
        final String riverName;
        final double amount;

        RainfallEvent(String riverName, double amount) {
            this.riverName = riverName;
            this.amount = amount;
        }

        @Override
        public String toString() {
            return String.format("Rain: %.1fmm on %s", amount, riverName);
        }
    }
}