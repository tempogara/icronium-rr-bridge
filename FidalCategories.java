package it.tempogara.batch;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public class FidalCategories {

    public static final Set<String> ALLOWED_CATEGORIES = Set.of(
        "EM5", "EF5",
        "EM8", "EF8",
        "EM10", "EF10",
        "RM", "RF",
        "CM", "CF",
        "AM", "AF",
        "JM", "JF",
        "PM", "PF",
        "SM", "SF",
        "SM35", "SF35",
        "SM40", "SF40",
        "SM45", "SF45",
        "SM50", "SF50",
        "SM55", "SF55",
        "SM60", "SF60",
        "SM65", "SF65",
        "SM70", "SF70",
        "SM75", "SF75",
        "SM80", "SF80",
        "SM85", "SF85",
        "SM90", "SF90",
        "SM95", "SF95"
    );

    private static final List<CategoryRule> CATEGORY_RULES = List.of(
        new CategoryRule("EM5", "M", 5, 7),
        new CategoryRule("EM8", "M", 8, 9),
        new CategoryRule("EM10", "M", 10, 11),
        new CategoryRule("RM", "M", 12, 13),
        new CategoryRule("CM", "M", 14, 15),
        new CategoryRule("AM", "M", 16, 17),
        new CategoryRule("JM", "M", 18, 19),
        new CategoryRule("PM", "M", 20, 22),
        new CategoryRule("EF5", "F", 5, 7),
        new CategoryRule("EF8", "F", 8, 9),
        new CategoryRule("EF10", "F", 10, 11),
        new CategoryRule("RF", "F", 12, 13),
        new CategoryRule("CF", "F", 14, 15),
        new CategoryRule("AF", "F", 16, 17),
        new CategoryRule("JF", "F", 18, 19),
        new CategoryRule("PF", "F", 20, 22),
        new CategoryRule("SM", "M", 23, 34),
        new CategoryRule("SF", "F", 23, 34),
        new CategoryRule("SM35", "M", 35, 39),
        new CategoryRule("SF35", "F", 35, 39),
        new CategoryRule("SM40", "M", 40, 44),
        new CategoryRule("SF40", "F", 40, 44),
        new CategoryRule("SM45", "M", 45, 49),
        new CategoryRule("SF45", "F", 45, 49),
        new CategoryRule("SM50", "M", 50, 54),
        new CategoryRule("SF50", "F", 50, 54),
        new CategoryRule("SM55", "M", 55, 59),
        new CategoryRule("SF55", "F", 55, 59),
        new CategoryRule("SM60", "M", 60, 64),
        new CategoryRule("SF60", "F", 60, 64),
        new CategoryRule("SM65", "M", 65, 69),
        new CategoryRule("SF65", "F", 65, 69),
        new CategoryRule("SM70", "M", 70, 74),
        new CategoryRule("SF70", "F", 70, 74),
        new CategoryRule("SM75", "M", 75, 79),
        new CategoryRule("SF75", "F", 75, 79),
        new CategoryRule("SM80", "M", 80, 84),
        new CategoryRule("SF80", "F", 80, 84),
        new CategoryRule("SM85", "M", 85, 89),
        new CategoryRule("SF85", "F", 85, 89),
        new CategoryRule("SM90", "M", 90, 94),
        new CategoryRule("SF90", "F", 90, 94),
        new CategoryRule("SM95", "M", 95, 100),
        new CategoryRule("SF95", "F", 95, 100)
    );

    public static boolean isAllowedCategory(String categoryCode) {
        if (categoryCode == null) {
            return false;
        }
        String normalized = categoryCode.trim().toUpperCase();
        return ALLOWED_CATEGORIES.contains(normalized);
    }

    public static String getCategory(String sesso, int annoNascita) {
        return getCategory(sesso, annoNascita, LocalDate.now().getYear());
    }

    public static String getCategory(String sesso, int annoNascita, int annoRiferimento) {
        String normalizedSex = normalizeSex(sesso);
        if (normalizedSex == null || annoNascita <= 0 || annoNascita > annoRiferimento) {
            return null;
        }

        int eta = annoRiferimento - annoNascita;
        for (CategoryRule rule : CATEGORY_RULES) {
            if (rule.matches(normalizedSex, eta)) {
                return rule.code;
            }
        }
        return null;
    }

    private static String normalizeSex(String sesso) {
        if (sesso == null) {
            return null;
        }

        String normalized = sesso.trim().toUpperCase();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.equals("M") || normalized.equals("MASCHIO") || normalized.equals("MALE")) {
            return "M";
        }
        if (normalized.equals("F") || normalized.equals("FEMMINA") || normalized.equals("FEMALE")) {
            return "F";
        }
        return null;
    }

    private static class CategoryRule {
        private final String code;
        private final String sex;
        private final int minAge;
        private final int maxAge;

        private CategoryRule(String code, String sex, int minAge, int maxAge) {
            this.code = code;
            this.sex = sex;
            this.minAge = minAge;
            this.maxAge = maxAge;
        }

        private boolean matches(String requestedSex, int age) {
            return sex.equals(requestedSex) && age >= minAge && age <= maxAge;
        }
    }
}
