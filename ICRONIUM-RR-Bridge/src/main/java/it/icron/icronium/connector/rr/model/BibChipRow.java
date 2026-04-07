package it.icron.icronium.connector.rr.model;

public class BibChipRow {

    private String chip;
    private int bib;
    private String lastName;
    private String firstName;
    private String team;

    public BibChipRow() {
    }

    public BibChipRow(String chip, int bib) {
        this.chip = chip;
        this.bib = bib;
    }

    public BibChipRow(String chip, int bib, String lastName, String firstName) {
        this.chip = chip;
        this.bib = bib;
        this.lastName = lastName;
        this.firstName = firstName;
    }

    public BibChipRow(String chip, int bib, String lastName, String firstName, String team) {
        this.chip = chip;
        this.bib = bib;
        this.lastName = lastName;
        this.firstName = firstName;
        this.team = team;
    }

    public String getChip() {
        return chip;
    }

    public void setChip(String chip) {
        this.chip = chip;
    }

    public int getBib() {
        return bib;
    }

    public void setBib(int bib) {
        this.bib = bib;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }
}
