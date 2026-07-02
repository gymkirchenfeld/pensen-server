package ch.kinet.pensen.calculation;

import ch.kinet.pensen.data.Employment;
import ch.kinet.pensen.data.PayrollType;
import ch.kinet.pensen.data.SemesterEnum;
import java.util.stream.Stream;

public final class CalculationLessonsAgeReliefIncluded2 extends CalculationLessons2 {

    CalculationLessonsAgeReliefIncluded2(Employment employment, Stream<PayrollType> payrollTypes) {
        super(employment, payrollTypes, AgeReliefIncludedSupport.poolTitle(employment));
    }

    @Override
    double postingDetailInputPercent(SemesterEnum semester, double percent) {
        return AgeReliefIncludedSupport.postingDetailPercent(employment, semester, percent);
    }

    @Override
    double poolPercent(SemesterEnum semester, double percent) {
        return AgeReliefIncludedSupport.poolPercent(employment, semester, percent);
    }
}