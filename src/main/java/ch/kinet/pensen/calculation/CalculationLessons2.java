package ch.kinet.pensen.calculation;

import ch.kinet.pensen.data.Employment;
import ch.kinet.pensen.data.PayrollType;
import ch.kinet.pensen.data.SemesterValue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Stream;


public class CalculationLessons2 extends CalculationLessons {
    // Das Excel ePM_Berechnung_Korrektur_IPB_MASA.xlsx definiert, wie die in SAP einzutragenden Zahlen aus dem Pensum berechnet werden
    // Hier wird diese Berechnungslogik implementert

    private final int maxPayrollTypeId;
    private final int maxSaldoResolvingOrder;

    CalculationLessons2(Employment employment, Stream<PayrollType> payrollTypes) {
        super(employment, payrollTypes);
        maxPayrollTypeId = payrollMap.types()
                .mapToInt(PayrollType::getId)
                .max()
                .orElse(0);
        maxSaldoResolvingOrder = payrollMap.types()
                .mapToInt(PayrollType::getSaldoResolvingOrder)
                .max()
                .orElse(0);
    }

    double calculateLessonDifference(double diffPercent, PayrollType type) {
        // Die Formel im Excel ist: =WENN(H19=WAHR;WENN($F$29 - $E$14 <= 0; AUFRUNDEN(($F$29 - $E$14) * C19; 2);ABRUNDEN(($F$29 - $E$14) * C19; 2));"")
        // Um Rundungsartefakte möglichst zu vermeiden, wird zuerst auf 8 Kommastellen gerundet
        // Beispiel:
        //    Bei 14 Lektionen GYM2-4 und 50% Anstellung würde die exakte IPB-Korrektur 3519 / 1150 Lektionen betragen
        //    In Excel wird diese rationale Zahl als 3.06000000000 dargestellt und somit auf 3.06 gerundet
        //    In Java wird diese rationale Zahl aber als 3.05999999999999 dargestellt und somit auf 3.05 abgerundet
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

    PayrollType createIPBCorrectionPayrollType(PayrollType type) {
        return new PayrollType("IPBKorr_" + type.getCode(),
                "IPB Korrektur " + type.getDescription(),
                maxPayrollTypeId + type.getId() + 1, type.isLessonBased(),
                maxSaldoResolvingOrder + type.getSaldoResolvingOrder() + 1);
    }

    @Override
    void calculatePayroll() {
        SemesterValue diff = employment.paymentTarget().map(
                (s, payment) -> totalPercent.get(s) - payment
        );
        payrollMap.types().forEachOrdered(type -> {
            SemesterValue percent = payrollMap.get(type);
            SemesterValue lessons = percent.map((s, p) -> percentToLessons(type, employment.withoutAgeRelief(s, p)));
            payroll.add(type, lessons, percent);

            SemesterValue corrLessons = percent.map((s, p) -> {
                if (p > 0) {
                    double d = diff.get(s);
                    diff.set(s, 0);
                    return -calculateLessonDifference(d, type);
                } else {
                    return 0.0;
                }
            });
            if (corrLessons.semester1() != 0 || corrLessons.semester2() != 0) {
                SemesterValue corrPercent = corrLessons.map((s, l) -> lessonsToPercent(type, l));
                payroll.add(createIPBCorrectionPayrollType(type), corrLessons, corrPercent);
            }
        });
    }
}
