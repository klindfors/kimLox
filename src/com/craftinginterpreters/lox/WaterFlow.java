package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * WaterFlow.java - updated with:
 *  - per-river daily stats: lastRainReceived, lastDrained
 *  - printSystemStatus showing rain received and drained per river, and dams list

 */
class WaterFlow {

    static class River {
        final String name;
        final FlowType flowType;
        final List<Double> waterArray;  // rW[...] - rainwater from last 10 days
        final double terrainType;       // T(...) - terrain type
        final double catchmentArea;     // A(...) - catchment area
        final List<River> inflows;
        River outflow;
        final Random random = new Random();

        // NEW (non-invasive): daily bookkeeping (does not change existing logic)
        double lastRainReceived = 0.0; // mm added to the waterArray for today
        double lastDrained = 0.0;      // mm drained from the terrain today

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
            double totalFlow = 0.0;

            // flow from this river’s terrain (rainfall memory)
            for (int i = 0; i < waterArray.size(); i++) {
                double waterAmount = waterArray.get(i);
                double recencyWeight = (10.0 - i) / 10.0; // 1.0 to 0.1
                totalFlow += waterAmount * recencyWeight;
            }

            // adding inflows recursively
            for (River inflow : inflows) {
                totalFlow += 0.9 * inflow.getCurrentFlow();
                // The 0.9 is an attenuation factor (optional)
                // to simulate some loss (you can adjust or remove)
            }

            // apply area and terrain effects
            double areaFlowMultiplier = Math.log(1 + catchmentArea / 10.0);
            double terrainFlowMultiplier = 0.8 + (terrainType * 0.4);

            return totalFlow * areaFlowMultiplier * terrainFlowMultiplier;
        }

        // Add rainfall to the catchment, after rain event.
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
            // dayIndex: 0 = most recent, 9 = oldest (10th day)
            // force complete drainage on day 10 (index 9)
            if (dayIndex == 9) {
                return 1.0;
            }
            double baseRate = 0.15;
            double ageFactor = 1.0 + (dayIndex * 0.1); // 1.0 to 1.9 multiplier
            // area factor: larger area = slower drainage (inverse relationship)
            double areaFactor = 50.0 / (50.0 + catchmentArea); // Normalize around 50 hectares
            // terrain factor: more stony (higher value) = faster drainage
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
        final String inflow;   // name of inflow river/dam
        double threshold;
        double capacity;
        double opening;
        double storage = 0.0;
        double outflow;
        String outflowTarget = null;

        double getOutflow() {
            return this.outflow;
        }

        Dam(String name, String inflow, double threshold, double capacity, double opening) {
            this.name = name;
            this.inflow = inflow;
            this.threshold =threshold;
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
            this.outflow = outflow;
            return outflow;
        }
    }

    enum FlowType {
        ROOT,        // ^ root river (no inflows, only rain)
        UPSTREAM,    // < receives from upstream
        DOWNSTREAM   // >  flows to downstream (uncommon)
    }

    static class RiverSystem {
        private final List<River> rivers;
        private final List<Dam> dams;

        // Keeps today's scheduled rainfall events (these are applied at simulateDay start)
        private List<RainfallEvent> scheduledRainEvents = new ArrayList<>();
        private List<RainfallEvent> lastRainfallEvents = new ArrayList<>();

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

        void applyRandomRainToAllRivers(double minRain, double maxRain) {
            Random rand = new Random();
            for (River river : rivers) {
                double rainfall = minRain + rand.nextDouble() * (maxRain - minRain);
                river.addRainfall(rainfall);
                river.lastRainReceived = rainfall;
            }
        }

        Dam findDam(String name) {
            for (Dam dam : dams) {
                if (dam.name.equals(name)) {
                    return dam;
                }
            }
            return null;
        }



        // exact rain: *P(x) river
        void scheduleExactRain(String riverName, double amount) {
            scheduledRainEvents.add(new RainfallEvent(riverName, amount));
        }

        // Random rain: *Prand(a-b) river
        void scheduleRandomRain(String riverName, double min, double max) {
            // We'll store as a special RainfallEvent with negative amount semantics,
            // but to keep it simple, we immediately resolve randomness here and add to scheduled.
            double value = min + Math.random() * (max - min);
            scheduledRainEvents.add(new RainfallEvent(riverName, value));
        }

        // Clear scheduled events (call after sim)
        void clearScheduledRain() {
            scheduledRainEvents.clear();
        }

        /**
         * Simulate one day. applies scheduled rainfall events, drains terrain,
         * simulates dams and prints system status. It records per-river:
         *   - lastRainReceived (mm added to waterArray this day)
         *   - lastDrained (mm removed by drainTerrain this day)
         */
        void simulateDay() {

            simulateDay(new ArrayList<>(scheduledRainEvents));

            clearScheduledRain();
        }

        // Main simulate used internally (and by interpreter if it provides a list)
        void simulateDay(List<RainfallEvent> rainfallEvents) {
            applyRandomRainToAllRivers(0.0, 10.0);
            currentDay++;


            lastRainfallEvents = new ArrayList<>(rainfallEvents);

            // rainfall events to rivers -> call addRainfall (preserves 10-day array)
            for (RainfallEvent ev : rainfallEvents) {
                River river = findRiver(ev.riverName);
                if (river != null) {
                    // Record pre-add total (sum of waterArray)
                    double beforeAddTotal = sumWaterArray(river.waterArray);

                    // Add rainfall to array using your method
                    river.addRainfall(ev.amount);

                    // record lastRainReceived (exact mm added this day)
                    river.lastRainReceived = ev.amount;

                    // afterAddTotal (sum in terrain before draining)
                    double afterAddTotal = sumWaterArray(river.waterArray);


                } else {
                    System.out.printf("Warning: rainfall scheduled for unknown river '%s'%n", ev.riverName);
                }
            }


            for (River river : rivers) {
                double beforeDrain = sumWaterArray(river.waterArray);
                river.drainTerrain(); // original logic unchanged
                double afterDrain = sumWaterArray(river.waterArray);

                // Amount drained from terrain that day is (beforeDrain - afterDrain)
                double drained = Math.max(0.0, beforeDrain - afterDrain);
                river.lastDrained = drained;
            }


            simulateDamFlows();


            printSystemStatus();
        }

        // Utility: sum a river's waterArray
        private double sumWaterArray(List<Double> arr) {
            double s = 0.0;
            for (Double d : arr) s += (d == null ? 0.0 : d);
            return s;
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

                // Keep the existing debug print you had
                System.out.printf(
                        "Dam %-10s | Inflow: %-8.2f | Outflow: %-8.2f | Storage: %-8.2f%n",
                        dam.name, inflowValue, outflowValue, dam.storage
                );
            }
        }

        public void printSystemStatus() {
            System.out.println("\n~*~*~*~*~*~*~*~*~*~*~*~*~");
            System.out.println("~*~*~ Day " + currentDay + " Water System Status ~*~*~");

            System.out.println("\nRainfall Events:");
            if (lastRainfallEvents.isEmpty()) {
                System.out.println("  No rainfall recorded today.");
            } else {
                for (RainfallEvent e : lastRainfallEvents) {
                    System.out.printf("  %-15s received %.2f mm%n", e.riverName, e.amount);
                }
            }

            // River Info, print flow + terrain + area + exact stored and drained stats
            System.out.println("\nRivers:");
            for (River river : rivers) {
                double storedNow = sumWaterArray(river.waterArray); // total mm currently in terrain (sum of array)
                System.out.printf("  %-15s | Flow: %-8.2f | Terrain: %-4.2f | Area: %-5.2f%n",
                        river.name, river.getCurrentFlow(), river.terrainType, river.catchmentArea);
                System.out.printf("    Stored in terrain (total mm): %.2f | Rain today: %.2f | Drained today: %.2f%n",
                        storedNow, river.lastRainReceived, river.lastDrained);
            }

            // Dam Info, shows inflow name and current storage/outflow. Also show outflow target if configured.
            System.out.println("\nDams:");
            if (dams.isEmpty()) {
                System.out.println("  (none)");
            } else {
                for (Dam dam : dams) {
                    River inflowRiver = findRiver(dam.inflow);
                    double inflowValue = inflowRiver != null ? inflowRiver.getCurrentFlow() : 0.0;
                    String outflowTarget = dam.outflowTarget != null ? dam.outflowTarget : "(not configured)";
                    System.out.printf(
                            "  Dam %-12s | Inflow(from=%-10s): %-8.2f | Outflow: %-8.2f | Storage: %-8.2f | Cap: %-6.2f | Open: %-4.2f%n",
                            dam.name, dam.inflow, inflowValue, dam.getOutflow(), dam.storage, dam.capacity, dam.opening
                    );
                }
            }

            System.out.println("~*~*~*~*~*~*~*~*~*~*~*~*~\n");
        }

        List<River> getRivers() {
            return new ArrayList<>(rivers);
        }

        List<Dam> getDams() {
            return new ArrayList<>(dams);
        }
    }

    // rainfall event on a specific river/catchment area
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
