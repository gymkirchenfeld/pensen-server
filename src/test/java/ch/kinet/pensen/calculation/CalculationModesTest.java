package ch.kinet.pensen.calculation;

import ch.kinet.pensen.data.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class CalculationModesTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("getTestCases")
    void testCalculation(CalculationTestCase testCase) {
        testCase.verify(testCase.calculate().payroll());
    }


    static Stream<CalculationTestCase> getTestCases() {
        return Stream.of(
                teacherA(), teacherB(), teacherC(), teacherD(), teacherE()
        ).flatMap(cases -> cases);
    }


    private static Stream<CalculationTestCase> teacherA() {
        return CalculationScenario.create("2027, Lehrkraft A, 75%, ohne Altersentlastung, mit Pool")
                .graduationYear(2027)
                .weeks(38)
                .birthday(1978, 1, 18)
                .payment(75, 75)
                .course(GradeFixture.GYM4, SubjectFixture.M, 4, 4)
                .course(GradeFixture.GYM4, SubjectFixture.M, 4, 4)
                .course(GradeFixture.GYM4, SubjectFixture.KS, 1, 1)
                .course(GradeFixture.GYM2, SubjectFixture.M, 3, 3)
                .pool(PoolTypeFixture.S, 15, 15)
                .pool(PoolTypeFixture.S, 5, 5)
                .mode(CalculationMode.Enum.lessons,
                        CalculationMode.Enum.lessonsAgeReliefIncluded,
                        CalculationMode.Enum.percent,
                        CalculationMode.Enum.percentAgeReliefIncluded)
                .row(PayrollTypeFixture.G2, 11.5, 50.0, 11.5, 50.0)
                .row(PayrollTypeFixture.GK2, 1.0, 5.0, 1.0, 5.0)
                .row(PayrollTypeFixture.PS, 0.0, 20.0, 0.0, 20.0)
                .total(75.0, 75.0)
                .mode(CalculationMode.Enum.lessons2, CalculationMode.Enum.lessons2AgeReliefIncluded)
                .row(PayrollTypeFixture.G2, 11.0, 47.83, 11.0, 47.83)
                .ipbRow(PayrollTypeFixture.G2, 0.5, 2.17, 0.5, 2.17)
                .row(PayrollTypeFixture.GK2, 1.0, 5.0, 1.0, 5.0)
                .row(PayrollTypeFixture.PS, 0.0, 20.0, 0.0, 20.0)
                .total(75.0, 75.0)
                .cases();
    }


    private static Stream<CalculationTestCase> teacherB() {
        return CalculationScenario.create("2027, Lehrkraft B, 60%, mit Altersentlastung 4%, ohne Pool")
                .graduationYear(2027)
                .weeks(38)
                .birthday(1974, 6, 8)
                .payment(60, 60)
                .course(GradeFixture.FMSP, SubjectFixture.D, 3.2, 3.2)
                .course(GradeFixture.FMSP, SubjectFixture.KS, 1, 1)
                .course(GradeFixture.BME4, SubjectFixture.SFPP, 1.2, 1.2)
                .course(GradeFixture.BME3, SubjectFixture.SFPP, 1.2, 0.6)
                .course(GradeFixture.BME2, SubjectFixture.SFPP, 0, 0.5)
                .course(GradeFixture.FMS3, SubjectFixture.BFSAPY, 2, 2)
                .course(GradeFixture.FMS3, SubjectFixture.BFSAPE, 2, 2)
                .course(GradeFixture.GYM3, SubjectFixture.EFPP, 2, 2)
                .mode(CalculationMode.Enum.percent)
                .row(PayrollTypeFixture.G2, 2.82, 12.748, 2.92, 13.200)
                .row(PayrollTypeFixture.F, 7.2, 31.200, 7.2, 31.200)
                .row(PayrollTypeFixture.BM, 2.4, 10.852, 2.3, 10.400)
                .row(PayrollTypeFixture.FK, 1.00, 5.200, 1.00, 5.200)
                .total(60.000, 60.000)
                .mode(CalculationMode.Enum.lessons)
                .row(PayrollTypeFixture.G2, 2.82, 12.75, 2.92, 13.20)
                .row(PayrollTypeFixture.F, 7.2, 31.20, 7.20, 31.20)
                .row(PayrollTypeFixture.BM, 2.4, 10.85, 2.30, 10.40)
                .row(PayrollTypeFixture.FK, 1.00, 5.20, 1.00, 5.20)
                .total(60.00, 60.00)
                .mode(CalculationMode.Enum.lessons2, CalculationMode.Enum.lessons2AgeReliefIncluded)
                .row(PayrollTypeFixture.G2, 2.00, 9.04, 2.00, 9.04)
                .row(PayrollTypeFixture.F, 7.2, 31.20, 7.2, 31.20)
                .ipbRow(PayrollTypeFixture.F, 0.89, 3.71, 1.00, 4.17)
                .row(PayrollTypeFixture.BM, 2.40, 10.85, 2.30, 10.40)
                .row(PayrollTypeFixture.FK, 1.00, 5.20, 1.00, 5.20)
                .total(60.00, 60.01)
                .cases();
    }

    private static Stream<CalculationTestCase> teacherC() {
        return CalculationScenario.create("2026, Lehrkraft C, 90%, mit Altersentlastung 8%, mit Pool")
                .graduationYear(2026)
                .weeks(38)
                .birthday(1970, 7, 21)
                .payment(90, 90)
                .course(GradeFixture.GYM4, SubjectFixture.D, 12, 10)
                .course(GradeFixture.GYM1, SubjectFixture.D, 6, 6)
                .pool(PoolTypeFixture.S, 10, 10)
                .pool(PoolTypeFixture.L, 2.5, 2.5)
                .pool(PoolTypeFixture.Z, 2.25, 2.25)
                .thesis(ThesisTypeFixture.MA, 1)
                .mode(CalculationMode.Enum.percent)
                .row(PayrollTypeFixture.G1, 6.00, 23.143, 6.00, 23.143)
                .row(PayrollTypeFixture.G2, 10.85, 50.927, 10.85, 50.927)
                .row(PayrollTypeFixture.PS, 0, 10.80, 0, 10.80)
                .row(PayrollTypeFixture.PL, 0, 2.70, 0, 2.70)
                .row(PayrollTypeFixture.PX, 0, 2.43, 0, 2.43)
                .total(90.000, 90.000)
                .mode(CalculationMode.Enum.percentAgeReliefIncluded)
                .row(PayrollTypeFixture.G1, 5.56, 21.429, 5.56, 21.429)
                .row(PayrollTypeFixture.G2, 10.19, 47.853, 10.34, 48.549)
                .row(PayrollTypeFixture.PS, 0, 9.259, 0, 9.259)
                .row(PayrollTypeFixture.PL, 0, 2.315, 0, 2.315)
                .row(PayrollTypeFixture.PX, 0, 2.083, 0, 2.083)
                .total(82.939, 83.635)
                .mode(CalculationMode.Enum.lessons)
                .row(PayrollTypeFixture.G1, 6.00, 23.143, 6.00, 23.143)
                .row(PayrollTypeFixture.G2, 10.85, 50.95, 10.85, 50.95)
                .row(PayrollTypeFixture.PS, 0, 10.80, 0, 10.80)
                .row(PayrollTypeFixture.PL, 0, 2.70, 0, 2.70)
                .row(PayrollTypeFixture.PX, 0, 2.43, 0, 2.43)
                .total(90.02, 90.02)
                .mode(CalculationMode.Enum.lessonsAgeReliefIncluded)
                .row(PayrollTypeFixture.G1, 6.00, 23.14, 6.00, 23.14)
                .row(PayrollTypeFixture.G2, 11.10, 52.12, 11.10, 52.12)
                .row(PayrollTypeFixture.PS, 0, 10.00, 0, 10.00)
                .row(PayrollTypeFixture.PL, 0, 2.50, 0, 2.50)
                .row(PayrollTypeFixture.PX, 0, 2.25, 0, 2.25)
                .total(90.01, 90.01)
                .mode(CalculationMode.Enum.lessons2)
                .row(PayrollTypeFixture.G1, 6.00, 23.14, 6.00, 23.14)
                .row(PayrollTypeFixture.G2, 12.23, 57.43, 10.23, 48.04)
                .ipbRow(PayrollTypeFixture.G2, -1.49, -6.48, 0.67, 2.91)
                .row(PayrollTypeFixture.PS, 0, 10.80, 0, 10.80)
                .row(PayrollTypeFixture.PL, 0, 2.70, 0, 2.70)
                .row(PayrollTypeFixture.PX, 0, 2.43, 0, 2.43)
                .total(90.02, 90.02)
                .mode(CalculationMode.Enum.lessons2AgeReliefIncluded)
                .row(PayrollTypeFixture.G1, 6.00, 23.14, 6.00, 23.14)
                .row(PayrollTypeFixture.G2, 12.23, 57.43, 10.23, 48.04)
                .ipbRow(PayrollTypeFixture.G2, -1.22, -5.30, 0.94, 4.09)
                .row(PayrollTypeFixture.PS, 0, 10.00, 0, 10.00)
                .row(PayrollTypeFixture.PL, 0, 2.50, 0, 2.50)
                .row(PayrollTypeFixture.PX, 0, 2.25, 0, 2.25)
                .total(90.02, 90.02)
                .cases();
    }

    private static Stream<CalculationTestCase> teacherD() {
        return CalculationScenario.create("2027, Lehrkraft D, 65%, mit Altersentlastung 4%, mit Kleingruppenabzug, mit Pool")
                .graduationYear(2027)
                .weeks(38)
                .birthday(1973, 8, 18)
                .payment(65, 65)
                .course(GradeFixture.BME4, SubjectFixture.D, 1, 1)
                .smallGroup()
                .course(GradeFixture.BME3, SubjectFixture.D, 3, 5.5)
                .course(GradeFixture.BME2, SubjectFixture.D, 2.5, 2)
                .course(GradeFixture.BME2, SubjectFixture.KS, 0.5, 0.5)
                .pool(PoolTypeFixture.S, 17.5, 17.5)
                .mode(CalculationMode.Enum.lessons)
                .row(PayrollTypeFixture.G2, 3.43, 15.51, 1.43, 6.47)
                .row(PayrollTypeFixture.BM, 6.92, 31.29, 8.92, 40.33)
                .row(PayrollTypeFixture.PS, 0, 18.2, 0, 18.2)
                .total(65.00, 65.00)
                .mode(CalculationMode.Enum.lessons2)
                .row(PayrollTypeFixture.G2, 0, 0, 0, 0)
                .row(PayrollTypeFixture.BM, 6.92, 31.29, 8.92, 40.33)
                .ipbRow(PayrollTypeFixture.BM, 3.57, 15.52, 1.49, 6.48)
                .row(PayrollTypeFixture.PS, 0, 18.20, 0, 18.20)
                .total(65.01, 65.01)
                .mode(CalculationMode.Enum.lessons2AgeReliefIncluded)
                .row(PayrollTypeFixture.G2, 0, 0, 0, 0)
                .row(PayrollTypeFixture.BM, 6.92, 31.29, 8.92, 40.33)
                .ipbRow(PayrollTypeFixture.BM, 3.73, 16.22, 1.65, 7.17)
                .row(PayrollTypeFixture.PS, 0, 17.50, 0, 17.50)
                .total(65.01, 65.01)
                .cases();
    }

    private static Stream<CalculationTestCase> teacherE() {
        return CalculationScenario.create("2027, Lehrkraft E, 50%, ohne Altersentlastung, Teamteaching")
                .graduationYear(2027)
                .weeks(38)
                .birthday(1981, 9, 2)
                .payment(50, 50)
                .course(GradeFixture.GYM4, SubjectFixture.D, 2, 2)
                .teacherCount(2, 2)
                .course(GradeFixture.GYM3, SubjectFixture.D, 2, 2)
                .teacherCount(2, 2)
                .course(GradeFixture.GYM1, SubjectFixture.D, 12, 12)
                .mode(CalculationMode.Enum.lessons)
                .row(PayrollTypeFixture.G1, 12.00, 42.86, 12.00, 42.86)
                .row(PayrollTypeFixture.G2, 1.64, 7.13, 1.64, 7.13)
                .total(49.99, 49.99)
                .mode(CalculationMode.Enum.lessons2, CalculationMode.Enum.lessons2AgeReliefIncluded)
                .row(PayrollTypeFixture.G1, 12.00, 42.86, 12.00, 42.86)
                .row(PayrollTypeFixture.G2, 2.00, 8.70, 2.00, 8.70)
                .ipbRow(PayrollTypeFixture.G2, -0.35, -1.52, -0.35, -1.52)
                .total(50.03, 50.03)
                .cases();
    }
}