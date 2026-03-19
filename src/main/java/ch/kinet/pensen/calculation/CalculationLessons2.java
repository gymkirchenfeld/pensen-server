package ch.kinet.pensen.calculation;

import ch.kinet.pensen.data.Employment;
import ch.kinet.pensen.data.PayrollType;

import java.util.stream.Stream;

public class CalculationLessons2 extends CalculationLessons {
    CalculationLessons2(Employment employment, Stream<PayrollType> payrollTypes) {
        super(employment, payrollTypes);
    }
}
