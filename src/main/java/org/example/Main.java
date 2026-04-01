package org.example;

public class Main {

    public static void main(String[] args) {

        System.out.println("======= Phase 1: Baseline Validation =======");

        try {
            BaselineStudySystem baseline = new BaselineStudySystem();
            baseline.runBaseline();

            System.out.println("[Completed] Baseline_Validation.csv has been generated");

        } catch (Exception e) {
            System.err.println("[Error] Baseline phase failed:");
            e.printStackTrace();
            return;
        }

        System.out.println("\n======= Phase Two: The h-index experiment =======");

        try {
            ModifiedHIndexSystem system = new ModifiedHIndexSystem();
            system.process();

            System.out.println("[Completed] The experimental results have been generated:");
            System.out.println(" - Experimental_Results.csv");

        } catch (Exception e) {
            System.err.println("[Error] Experimental calculation failed:");
            e.printStackTrace();
        }

        System.out.println("\n======= All experiments have been completed =======");
    }
}