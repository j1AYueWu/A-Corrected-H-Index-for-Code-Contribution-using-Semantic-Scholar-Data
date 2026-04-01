package org.example.io;

import org.example.model.AuthorResult;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;

public class ResultExporter {

    public static void saveResults(
            String fileName,
            List<AuthorResult> results) throws Exception {

        BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));

        bw.write("Author,h_Orig,h_ModelA,h_ModelB,h_Bonus_rk1,h_Bonus_rk2,h_Bonus_rk3");
        bw.newLine();

        for (AuthorResult r : results) {

            bw.write(
                    escape(r.name) + "," +
                            r.hOrig + "," +
                            r.hA + "," +
                            r.hB + "," +
                            r.hBonusK1 + "," +
                            r.hBonusK2 + "," +
                            r.hBonusK3
            );

            bw.newLine();
        }

        bw.close();

        System.out.println("[完成] Experimental results exported.");
    }

    private static String escape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

}