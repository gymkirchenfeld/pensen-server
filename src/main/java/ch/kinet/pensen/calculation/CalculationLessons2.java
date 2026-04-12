package ch.kinet.pensen.calculation;

import ch.kinet.pensen.data.Employment;
import ch.kinet.pensen.data.PayrollType;
import ch.kinet.pensen.data.SemesterEnum;
import ch.kinet.pensen.data.SemesterValue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CalculationLessons2 extends CalculationLessons {
    CalculationLessons2(Employment employment, Stream<PayrollType> payrollTypes) {
        super(employment, payrollTypes);
    }

    double calculateLessonDifference(double diffPercent, PayrollType type) {
        // Die Formel im Excel ist: =WENN(H19=WAHR;WENN($F$29 - $E$14 <= 0; AUFRUNDEN(($F$29 - $E$14) * C19; 2);ABRUNDEN(($F$29 - $E$14) * C19; 2));"")
        // Um Rundungsartefakte möglichst zu vermeiden, wird zuerst auf 8 Kommastellen gerundet.
        // Beispiel:
        //    Bei 14 Lektionen GYM2-4 und 50% Anstellung würde die exakte IPB-Korrektur 3519 / 1150 Lektionen betragen.
        //    In Excel wird diese rationale Zahl als 3.06000000000 dargestellt und somit auf 3.06 gerundet.
        //    In Java wird diese rationale Zahl aber als 3.05999999999999 dargestellt und somit auf 3.05 gerundet.
        //    Runden auf 8 Kommastellen löst dieses Problem (zumindest in diesem Fall)
        // N.B.:
        //    BigDecimal.setScale(2, RoundingMode.UP) entspricht AUFRUNDEN(Wert, 2) in Excel
        //    BigDecimal.setScale(2, RoundingMode.DOWN) entspricht ABRUNDEN(Wert, 2) in Excel
        BigDecimal lessons = BigDecimal.valueOf(percentToLessons(type, diffPercent))
                .setScale(8, RoundingMode.HALF_UP);
        if (diffPercent <= 0) {
            return lessons.setScale(2, RoundingMode.UP).doubleValue();
        } else {
            return lessons.setScale(2, RoundingMode.DOWN).doubleValue();
        }
    }

    private PayrollType typeWithMostLessons(SemesterEnum semester) {
        return payrollMap.types()
                .filter(PayrollType::isIpbCorrectionAllowed)
                .max(Comparator.comparingDouble(type ->
                        percentToLessons(type, employment.withoutAgeRelief(semester, payrollMap.get(semester, type)))
                ))
                .orElse(payrollMap.defaultType());
    }

    private double excelRound(double value, int places) {
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }


    @Override
    void calculatePayroll() {

        final SemesterValue diff = employment.paymentTarget().map(
                (s, payment) -> payment - totalPercent.get(s)
        );

        final Map<SemesterEnum, PayrollType> typeForIpbCorrectionPerSemester = Arrays.stream(SemesterEnum.values())
                .collect(Collectors.toMap(s -> s, this::typeWithMostLessons));

        final Map<SemesterEnum, Payroll.IpbCorrectionData> ipbCorrectionDataPerSemester = new HashMap<>();

        payrollMap.types().forEachOrdered(type -> {
            final SemesterValue percentWithoutCorrection = payrollMap.get(type);

            final SemesterValue correctionLessons = diff.map((s, d) -> {
                if (typeForIpbCorrectionPerSemester.get(s).equals(type)) {

                    double cL = calculateLessonDifference(-d, type);
                    ipbCorrectionDataPerSemester.put(s, new Payroll.IpbCorrectionData(
                            type,
                            cL,
                            lessonsToPercent(type, cL),
                            excelRound(percentToLessons(type, employment.withoutAgeRelief(s, percentWithoutCorrection.get(s))), 2),
                            percentWithoutCorrection.get(s)
                    ));
                    return cL;
                } else {
                    return 0.0;
                }
            });

            final SemesterValue correctionPercent = correctionLessons.map((s, corrL) ->
                    lessonsToPercent(type, corrL)
            );

            SemesterValue percent = payrollMap.get(type).map((s, p) -> p - correctionPercent.get(s));

            SemesterValue lessons;
            if (type.isLessonBased()) {
                lessons = percent.map((s, p) -> percentToLessons(type, employment.withoutAgeRelief(s, p)));
            } else {
                lessons = SemesterValue.create();
            }

            // Runde nur, wenn die Zahl relevant für die Eingabe in SAP ist
            if (type.isLessonBased()) {
                lessons = lessons.map((s, l) -> {
                    if (type.equals(typeForIpbCorrectionPerSemester.get(s))) {
                        return l;
                    } else {
                        return excelRound(l, 2);
                    }
                });
            } else {
                percent = percent.map((s, p) -> excelRound(p, 2));
            }

            payroll.add(
                    type, lessons, percent);
        });

        payroll.setIpbCorrectionDataPerSemester(ipbCorrectionDataPerSemester);
    }
}
