package ch.kinet.pensen.calculation;

import ch.kinet.pensen.data.Employment;
import ch.kinet.pensen.data.SemesterEnum;

final class AgeReliefIncludedSupport {

    private AgeReliefIncludedSupport() {
    }

    static String poolTitle(Employment employment) {
        return employment.ageReliefFactor(SemesterEnum.First) > 0 ? "Pensum: Pool (inkl. AE)" : "Pensum: Pool";
    }

    static double poolPercent(Employment employment, SemesterEnum semester, double percent) {
        // Am Kirchenfeld werden Pooleinträge inkl. AE erfasst
        return percent / (1.0 + employment.ageReliefFactor(semester) / 100);
    }

    static double postingDetailPercent(Employment employment, SemesterEnum semester, double percent) {
        // Am Kirchenfeld werden Einzelbuchungen in Prozent inkl. AE erfasst
        return employment.withoutAgeRelief(semester, percent);
    }
}