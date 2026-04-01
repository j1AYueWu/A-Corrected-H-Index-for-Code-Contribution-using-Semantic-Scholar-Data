package org.example.model;
public class AuthorResult {

    public String name;

    public int hOrig;
    public int hA;
    public int hB;

    public int hBonusK1;
    public int hBonusK2;
    public int hBonusK3;

    public AuthorResult(String name,
                        int hOrig,
                        int hA,
                        int hB,
                        int hBonusK1,
                        int hBonusK2,
                        int hBonusK3) {

        this.name = name;
        this.hOrig = hOrig;
        this.hA = hA;
        this.hB = hB;
        this.hBonusK1 = hBonusK1;
        this.hBonusK2 = hBonusK2;
        this.hBonusK3 = hBonusK3;
    }
}