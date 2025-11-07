package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;



class WaterFlow {

    static class River {
        final String name;
        final FlowType flowType;
        final List<Double> waterArray;  //rW[...] - rainwater from last 10 days
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

            double areaFlowMultiplier = Math.log(1 + catchmentArea / 10.0);

            double terrainFlowMultiplier = 0.8 + (terrainType * 0.4); // 0.8 to 1.2

            return totalFlow * areaFlowMultiplier * terrainFlowMultiplier;
        }


        // Add rainfall to the catchment
        void addRainfall(double rainfall) {
            // Remove oldest day, add new rainfall
            if (waterArray.size() >= 10) {
                waterArray.remove(0);
            }

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
        private double calculateDrainageRate(int dayIndex) {
            //dayIndex: 0 = most recent, 9 = oldest (10th day)

            //force complete drainage on day 10 (index 9)
            if (dayIndex == 9) {
                return 1.0;
            }

            double baseRate = 0.15;

            double ageFactor = 1.0 + (dayIndex * 0.1); // 1.0 to 1.9 multiplier

            //rea factor: larger area = slower drainage (inverse relationship)
            double areaFactor = 50.0 / (50.0 + catchmentArea); // Normalize around 50 hectares

            //terrain factor: more stony (higher value) = faster drainage
            double terrainFactor = 1.0 + terrainType; // 1.0 to 2.0 multiplier
            double finalRate = baseRate * ageFactor * areaFactor * terrainFactor;
            return Math.min(0.95, finalRate);
        }




        @Override
        public String toString() {
            return String.format("River{%s, flow=%.2f, terrain=%.2f, area=%.2f}",
                    name, getCurrentFlow(), terrainType, catchmentArea);
        }
    }

    public static class Dam {
        final String name;
        final String inflow;
        double threshold;
        double capacity;
        double opening;
        double storage = 0.0;
        double outflow;

        double getOutflow(){
            return this.outflow;
        }

        Dam(String name, String inflow, double threshold, double capacity, double opening) {
            this.name = name;
            this.inflow = inflow;
            this.threshold = threshold;
            this.capacity = capacity;
            this.opening = opening;

        }

        double computeOutflow(double inflowValue) {
            // Example logic: simple proportional outflow if above threshold
            if (inflowValue > threshold) {
                double excess = inflowValue - threshold;
                storage = Math.min(storage + excess, capacity);
            }
            // O is fraction between 0–1 (outflow percentage)
            double outflow = opening * storage;
            storage -= outflow;
            if (storage < 0) storage = 0;
            this.outflow=outflow;
            return outflow;
        }
    }

      enum FlowType {
        ROOT,        // ^ - root river (no inflows, only rain)
        UPSTREAM,    // < - receives from upstream
        DOWNSTREAM   // > - flows to downstream (less common)
    }

    static class RiverSystem {
        private final List<River> rivers;
        private final List<Dam> dams;
        private int currentDay;

        RiverSystem() {
            this.rivers = new ArrayList<>();
            this.dams = new ArrayList<>();
            this.currentDay = 0;
        }

        void addRiver(River river) {
            rivers.add(river);
        }

        void addDam(Dam dam) {
            dams.add(dam);
        }

        River findRiver(String name) {
            for (River river : rivers) {
                if (river.name.equals(name)) {
                    return river;
                }
            }
            return null;
        }

        Dam findDam(String name) {
            for (Dam dam : dams) {
                if (dam.name.equals(name)) {
                    return dam;
                }
            }
            return null;
        }

        void simulateDay(List<RainfallEvent> rainfallEvents) {
            currentDay++;

            // Apply rainfall to relevant rivers
            for (RainfallEvent event : rainfallEvents) {
                River river = findRiver(event.riverName);
                if (river != null) {
                    river.addRainfall(event.amount);
                }
            }

            // Drain terrain water for all rivers
            for (River river : rivers) {
                river.drainTerrain();
            }
            simulateDamFlows();
            printSystemStatus();
        }
        void simulateDamFlows() {
            for (Dam dam : dams) {
                // Find the river that feeds this dam
                River inflowRiver = findRiver(dam.inflow);

                if (inflowRiver == null) {
                    System.out.println("Warning: Inflow river '" + dam.inflow + "' for dam " + dam.name + " not found.");
                    continue;
                }

                double inflowValue = inflowRiver.getCurrentFlow();
                double outflowValue = dam.computeOutflow(inflowValue);

                System.out.printf(
                        "Dam %-10s | Inflow: %-8.2f | Outflow: %-8.2f | Storage: %-8.2f%n",
                        dam.name, inflowValue, outflowValue, dam.storage
                );
            }
        }

        public void printSystemStatus() {
            System.out.println("~*~*~*~*~*~*~*~*~*~*~*~*~");
            System.out.println("~*~*~ Day " + currentDay + " Water System Status ~*~*~");

            System.out.println("\nRivers:");
            for (River river : rivers) {
                System.out.println("  " + river);
            }

            System.out.println("\nDams:");
            if (dams.isEmpty()) {
                System.out.println("  (none)");
            } else {
                for (Dam dam : dams) {
                    System.out.printf("  Dam{%s, inflow=%s, storage=%.2f, capacity=%.2f, opening=%.2f}%n",
                            dam.name, dam.inflow, dam.storage, dam.capacity, dam.opening);
                }
            }

            System.out.println("~*~*~*~*~*~*~*~*~*~*~*~*~");
        }

        List<River> getRivers() {
            return new ArrayList<>(rivers);
        }

        List<Dam> getDams() {
            return new ArrayList<>(dams);
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