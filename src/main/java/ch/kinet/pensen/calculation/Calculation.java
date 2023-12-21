/*
 * Copyright (C) 2023 by Stefan Rothe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY); without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.kinet.pensen.calculation;

import ch.kinet.Util;
import ch.kinet.pensen.data.Course;
import ch.kinet.pensen.data.Employment;
import ch.kinet.pensen.data.PayrollType;
import ch.kinet.pensen.data.PoolEntry;
import ch.kinet.pensen.data.Posting;
import ch.kinet.pensen.data.PostingDetail;
import ch.kinet.pensen.data.SemesterEnum;
import ch.kinet.pensen.data.SemesterValue;
import ch.kinet.pensen.data.Teacher;
import ch.kinet.pensen.data.ThesisEntry;
import java.util.Comparator;

public abstract class Calculation {

    static final Comparator<PayrollType> SALDO_RESOLVING_ORDER =
        (PayrollType o1, PayrollType o2) -> Util.compare(o1.getSaldoResolvingOrder(), o2.getSaldoResolvingOrder());

    public static Calculation create(Employment employment, PayrollType defaultType) {
        switch (employment.getSchoolYear().calculationModeEnum()) {
            case Lessons:
                return new CalculationLessons(employment);
            case LessonsAgeReliefIncluded:
                return new CalculationLessonsAgeReliefIncluded(employment);
            case PercentAgeReliefIncluded:
                return new CalculationPercentAgeReliefIncluded(employment, defaultType);
            case Percent:
                return new CalculationPercent(employment);
            default:
                throw new IllegalArgumentException();
        }
    }

    final Employment employment;
    final Payroll payroll;
    final Postings postings;
    private final Courses courses;
    private final Pool pool;
    private final Theses theses;

    Calculation(Employment employment, String poolTitle) {
        this.employment = employment;
        courses = Courses.create();
        payroll = Payroll.create();
        pool = Pool.create(poolTitle);
        theses = Theses.create();
        postings = Postings.create();
    }

    public void addCourse(Course entry) {
        Teacher teacher = employment.getTeacher();
        double lessons1 = entry.lessonsFor(teacher, SemesterEnum.First);
        double lessons2 = entry.lessonsFor(teacher, SemesterEnum.Second);
        double percent1 = entry.percentFor(teacher, SemesterEnum.First);
        double percent2 = entry.percentFor(teacher, SemesterEnum.Second);
        PayrollType payrollType = entry.payrollType();

        courses.addItem(entry, lessons1, percent1, lessons2, percent2);
        sumPayrollLessons(payrollType, SemesterEnum.First, lessons1);
        sumPayrollLessons(payrollType, SemesterEnum.Second, lessons2);
    }

    public void addPoolEntry(PoolEntry entry) {
        SemesterValue percent = entry.percent();
        PayrollType payrollType = entry.getType().getPayrollType();

        pool.addItem(entry.getDescription(), entry.getType(), percent);

        // calculate percent without age relief for Kirchenfeld historic
        percent = percent.map((s, p) -> poolPercent(s, p));
        sumPayrollPercent(payrollType, SemesterEnum.First, percent.semester1());
        sumPayrollPercent(payrollType, SemesterEnum.Second, percent.semester2());
    }

    public void addThesisEntry(ThesisEntry entry) {
        PayrollType payrollType = entry.getType().getPayrollType();
        double percent = entry.getType().getPercent() * entry.getCount();

        theses.addItem(entry.getType(), entry.getCount(), percent);
        sumPayrollPercent(payrollType, SemesterEnum.First, percent);
        sumPayrollPercent(payrollType, SemesterEnum.Second, percent);
    }

    public void addPosting(Posting posting) {
        postings.addItem(posting);
    }

    public void addPostingDetail(PostingDetail postingDetail) {
        PayrollType type = postingDetail.getType().getPayrollType();
        if (postingDetail.getType().isPercent()) {
            handlePostingDetailPercent(postingDetail.getPosting(), type, postingDetail.getValue());
        }
        else {
            handlePostingDetailLessons(postingDetail.getPosting(), type, postingDetail.getValue());
        }
    }

    public Workload createWorkload() {
        Summary summary = Summary.create(
            employment.ageReliefFactor(SemesterEnum.First),
            employment.ageReliefFactor(SemesterEnum.Second)
        );

        summary.add("Unterricht", courses.percent1(), courses.percent2());
        summary.add("Abschlussarbeiten", theses.percent(), theses.percent());
        // calculate percent without age relief for Kirchenfeld historic
        SemesterValue percent = pool.percent().map((s, p) -> poolPercent(s, p));
        summary.add("Pool", percent.semester1(), percent.semester2());

        calculatePayroll();
        double payment = calculatePayment();
        return new Workload(employment, courses, pool, theses, postings, summary, payroll, payment);
    }

    final boolean lessonBased(PayrollType type) {
        return employment.getSchoolYear().lessonBased(type);
    }

    final double lessonsToPercent(PayrollType type, double lessons) {
        return employment.getSchoolYear().lessonsToPercent(type, lessons);
    }

    final double percentToLessons(PayrollType type, double lessons) {
        return employment.getSchoolYear().percentToLessons(type, lessons);
    }

    void sumPayrollLessons(PayrollType type, SemesterEnum semester, double lessons) {
        if (!lessonBased(type)) {
            throw new IllegalArgumentException("Cannot add lessons to percent-based payroll.");
        }

        addToPayroll(type, semester, lessons);
    }

    void sumPayrollPercent(PayrollType type, SemesterEnum semester, double percent) {
        if (lessonBased(type)) {
            // calculate lessons from percent
            addToPayroll(type, semester, percentToLessons(type, percent));
        }
        else {
            addToPayroll(type, semester, percent);
        }
    }

    abstract void addToPayroll(PayrollType type, SemesterEnum semester, double value);

    abstract double calculatePayment();

    abstract void calculatePayroll();

    abstract void handlePostingDetailLessons(Posting posting, PayrollType payrollType, double lessons);

    abstract void handlePostingDetailPercent(Posting posting, PayrollType payrollType, double percent);

    abstract double poolPercent(SemesterEnum semester, double percent);
}
