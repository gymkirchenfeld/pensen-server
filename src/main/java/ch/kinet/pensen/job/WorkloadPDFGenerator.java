/*
 * Copyright (C) 2022 - 2023 by Stefan Rothe
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
package ch.kinet.pensen.job;

import ch.kinet.Date;
import ch.kinet.Util;
import ch.kinet.pdf.Alignment;
import ch.kinet.pdf.Border;
import ch.kinet.pdf.Document;
import ch.kinet.pdf.VerticalAlignment;
import ch.kinet.pensen.calculation.Courses;
import ch.kinet.pensen.calculation.Pool;
import ch.kinet.pensen.calculation.Summary;
import ch.kinet.pensen.calculation.Theses;
import ch.kinet.pensen.calculation.Workload;
import ch.kinet.pensen.data.Division;
import ch.kinet.pensen.data.Employment;
import ch.kinet.pensen.data.Teacher;

public final class WorkloadPDFGenerator {

    private final Document pdf;
    private final Workload workload;

    public static void createPDF(Document pdf, Workload workload) {
        WorkloadPDFGenerator instance = new WorkloadPDFGenerator(pdf, workload);
        instance.create();
    }

    private WorkloadPDFGenerator(Document pdf, Workload workload) {
        this.workload = workload;
        this.pdf = pdf;
    }

    private void create() {
        pdf.addPage(2f, 1.5f);
        header();
        personalBlock();
        courseBlock();
        poolBlock();
        thesisBlock();
        summaryBlock();
        payrollBlock();
//        postingsBlock();
        balanceBlock();
        signatureBlock();
    }

    private void header() {
        Division division = workload.getEmployment().getDivision();
        pdf.beginTable(20, 60, 20);
        pdf.addCell(division.getLogo(), 200, 18);
        pdf.setVerticalAlignment(VerticalAlignment.Bottom);
        pdf.setFontSize(12);
        StringBuilder title = new StringBuilder();
        if (!workload.getSchoolYear().isFinalised()) {
            title.append("provisorische ");
        }

        title.append("Lektionenzuteilung SJ ");
        title.append(workload.getSchoolYear().getDescription());
        pdf.addCell(title.toString(), Alignment.Center);

        pdf.setFontSize(8);
        pdf.addCell(Date.today().formatDMY(), Alignment.Right);
        pdf.endTable();
    }

    private void personalBlock() {
        Employment e = workload.getEmployment();
        pdf.beginTable(25, 25, 25, 25);
        pdf.addCell("Name:", Alignment.Left, Border.Top);
        pdf.setBold();
        pdf.addCell(Format.name(e.getTeacher()), Alignment.Left, Border.Top);
        pdf.setNormal();
        pdf.addCell("Anstellung gemäss Verfügung:", Alignment.Left, Border.Top);
        pdf.addCell(Format.percentRange(e.getEmploymentMin(), e.getEmploymentMax()), Alignment.Right, Border.Top);

        pdf.addCell("Kürzel:", Alignment.Left);
        pdf.addCell(e.getTeacher().getCode(), Alignment.Left);
        pdf.addCell("Auszahlungsziel:", Alignment.Left);
        pdf.addCell(Format.percentSemester(e.getPayment1(), e.getPayment2()), Alignment.Right);

        pdf.addCell("Personalnummer:", Alignment.Left, Border.Bottom);
        pdf.addCell(e.getTeacher().getEmployeeNumber(), Alignment.Left, Border.Bottom);
        pdf.addCell("Anstellungsart:", Alignment.Left, Border.Bottom);
        pdf.addCell(Format.employmentType(e), Alignment.Right, Border.Bottom);
        pdf.endTable();
        pdf.addParagraph(workload.getEmployment().getComments(), Alignment.Left);
    }

    private void courseBlock() {
        Teacher t = workload.getTeacher();
        courseHeader();
        workload.courses().items().forEachOrdered(item -> {
            pdf.addCell(item.subject().getDescription(), Alignment.Left);
            pdf.addCell(Util.concat(item.schoolClasses().map(sc -> sc.getCode()), ", "), Alignment.Left);
            pdf.addCell(item.grade().getDescription(), Alignment.Left);
            pdf.addCell(Format.lessons(item.lessons1()), Alignment.Right);
            pdf.addCell(Format.percent(item.percent1(), false), Alignment.Right);
            pdf.addCell(Format.lessons(item.lessons2()), Alignment.Right);
            pdf.addCell(Format.percent(item.percent2(), false), Alignment.Right);
        });

        courseFooter();
    }

    private void courseHeader() {
        pdf.beginTable(25, 25, 10, 10, 10, 10, 10);
        pdf.setBold();
        pdf.setFontSize(10);
        pdf.addCell("Pensum: Unterricht", Alignment.Left, Border.Bottom);
        pdf.setFontSize(8);
        pdf.addCell("Klassen", Alignment.Left, Border.Bottom);
        pdf.addCell("Stufe", Alignment.Left, Border.Bottom);
        pdf.addCell("1. Sem.", Alignment.Right, Border.Bottom);
        pdf.addCell("1. Sem.", Alignment.Right, Border.Bottom);
        pdf.addCell("2. Sem.", Alignment.Right, Border.Bottom);
        pdf.addCell("2. Sem.", Alignment.Right, Border.Bottom);
        pdf.setNormal();
    }

    private void courseFooter() {
        Courses courses = workload.courses();
        pdf.setBold();
        pdf.addCell("Total", Alignment.Left, Border.Top);
        pdf.addCell("", Alignment.Left, Border.Top);
        pdf.addCell("", Alignment.Left, Border.Top);
        pdf.addCell(Format.lessons(courses.lessons1()), Alignment.Right, Border.Top);
        pdf.addCell(Format.percent(courses.percent1(), true), Alignment.Right, Border.Top);
        pdf.addCell(Format.lessons(courses.lessons2()), Alignment.Right, Border.Top);
        pdf.addCell(Format.percent(courses.percent2(), true), Alignment.Right, Border.Top);
        pdf.setNormal();
        pdf.endTable();
    }

    private void poolBlock() {
        Pool pool = workload.pool();
        if (pool.isEmpty()) {
            return;
        }

        Teacher t = workload.getTeacher();
        poolHeader(pool);
        pool.items().forEachOrdered(item -> {
            pdf.addCell(item.description(), Alignment.Left);
            pdf.addCell(item.type().getDescription(), Alignment.Left);
            pdf.addCell(Format.percent(item.percent1(), false), Alignment.Right);
            pdf.addCell(Format.percent(item.percent2(), false), Alignment.Right);
        });

        poolFooter(pool);
    }

    private void poolHeader(Pool pool) {
        pdf.addParagraph("", Alignment.Left);
        pdf.beginTable(50, 10, 20, 20);
        pdf.setBold();
        pdf.setFontSize(10);
        pdf.addCell(pool.title(), Alignment.Left, Border.Bottom);
        pdf.setFontSize(8);
        pdf.addCell("Typ", Alignment.Left, Border.Bottom);
        pdf.addCell("1. Sem.", Alignment.Right, Border.Bottom);
        pdf.addCell("2. Sem.", Alignment.Right, Border.Bottom);
        pdf.setNormal();
    }

    private void poolFooter(Pool pool) {
        pdf.setBold();
        pdf.addCell("Total", Alignment.Left, Border.Top);
        pdf.addCell("", Alignment.Left, Border.Top);
        pdf.addCell(Format.percent(pool.percent().semester1(), true), Alignment.Right, Border.Top);
        pdf.addCell(Format.percent(pool.percent().semester2(), true), Alignment.Right, Border.Top);
        pdf.setNormal();
        pdf.endTable();
    }

    private void thesisBlock() {
        Theses thesisSummary = workload.theses();
        if (thesisSummary.isEmpty()) {
            return;
        }

        thesisHeader();
        thesisSummary.items().forEachOrdered(item -> {
            pdf.addCell(item.type().getDescription(), Alignment.Left);
            pdf.addCell(Format.number(item.count(), 1), Alignment.Right);
            pdf.addCell(Format.percent(item.percentEach(), true), Alignment.Right);
            pdf.addCell(Format.percent(item.percent(), true), Alignment.Right);
        });

        thesisFooter();
    }

    private void thesisHeader() {
        pdf.addParagraph("", Alignment.Left);
        pdf.beginTable(50, 20, 20, 10);
        pdf.setBold();
        pdf.setFontSize(10);
        String title = "Pensum: Abschlussarbeiten";
        pdf.addCell(title, Alignment.Left, Border.Bottom);
        pdf.setFontSize(8);
        pdf.addCell("Anzahl", Alignment.Right, Border.Bottom);
        pdf.addCell("Prozent", Alignment.Right, Border.Bottom);
        pdf.addCell("Total", Alignment.Right, Border.Bottom);
        pdf.setNormal();
    }

    private void thesisFooter() {
        Theses thesisSummary = workload.theses();
        pdf.setBold();
        pdf.addCell("Total", Alignment.Left, Border.Top);
        pdf.addCell("", Alignment.Left, Border.Top);
        pdf.addCell("", Alignment.Left, Border.Top);
        pdf.addCell(Format.percent(thesisSummary.percent(), false), Alignment.Right, Border.Top);
        pdf.setNormal();
        pdf.endTable();
    }

    private void summaryBlock() {
        Summary summary = workload.summary();
        summaryHeader();
        summary.items().forEachOrdered(part -> {
            pdf.addCell(part.description(), Alignment.Left);
            pdf.addCell(Format.percent(part.percent1(), false), Alignment.Right);
            pdf.addCell(Format.percent(part.ageRelief1(), false), Alignment.Right);
            pdf.addCell(Format.percent(part.percent2(), false), Alignment.Right);
            pdf.addCell(Format.percent(part.ageRelief2(), false), Alignment.Right);
            pdf.addCell(Format.percent(part.percentWithAgeRelief(), false), Alignment.Right);

        });

        summaryFooter(summary);
    }

    private void summaryHeader() {
        String ae1Header = "";
        if (workload.ageReliefFactor1() > 0) {
            ae1Header = "AE 1. Sem. (" + String.valueOf(workload.ageReliefFactor1()) + "%)";
        }

        String ae2Header = "";
        if (workload.ageReliefFactor2() > 0) {
            ae2Header = "AE 2. Sem. (" + String.valueOf(workload.ageReliefFactor2()) + "%)";
        }

        pdf.addParagraph("", Alignment.Left);
        pdf.beginTable(25, 10, 20, 10, 20, 15);
        pdf.setBold();
        pdf.setFontSize(10);
        pdf.addCell("Pensum: Übersicht", Alignment.Left, Border.Bottom);
        pdf.setFontSize(8);
        pdf.addCell("1. Sem.", Alignment.Right, Border.Bottom);
        pdf.addCell(ae1Header, Alignment.Right, Border.Bottom);
        pdf.addCell("2. Sem.", Alignment.Right, Border.Bottom);
        pdf.addCell(ae2Header, Alignment.Right, Border.Bottom);
        pdf.addCell("Total", Alignment.Right, Border.Bottom);
        pdf.setNormal();
    }

    private void summaryFooter(Summary summary) {
        Summary.Item total = summary.total();
        pdf.setBold();
        pdf.addCell("Total", Alignment.Left, Border.Top);
        pdf.addCell(Format.percent(total.percent1(), false), Alignment.Right, Border.Top);
        pdf.addCell(Format.percent(total.ageRelief1(), false), Alignment.Right, Border.Top);
        pdf.addCell(Format.percent(total.percent2(), false), Alignment.Right, Border.Top);
        pdf.addCell(Format.percent(total.ageRelief2(), false), Alignment.Right, Border.Top);
        pdf.addCell(Format.percent(total.percentWithAgeRelief(), false), Alignment.Right, Border.Top);
        pdf.setNormal();
        pdf.endTable();
    }

    private void payrollBlock() {
        payrollHeader();
        workload.payroll().items().forEachOrdered(item -> {
            pdf.addCell(item.description(), Alignment.Left);
            pdf.addCell(Format.lessons(item.lessons().semester1()), Alignment.Right);
            pdf.addCell(Format.percent(item.percent().semester1(), false), Alignment.Right);
            pdf.addCell(Format.lessons(item.lessons().semester2()), Alignment.Right);
            pdf.addCell(Format.percent(item.percent().semester2(), false), Alignment.Right);
        });
        payrollFooter();
    }

    private void payrollHeader() {
        pdf.addParagraph("", Alignment.Left);
        pdf.beginTable(60, 10, 10, 10, 10);
        pdf.setBold();
        pdf.setFontSize(10);
        pdf.addCell("Pensenmeldung", Alignment.Left, Border.Bottom);
        pdf.setFontSize(8);
        pdf.addCell("1. Sem.", Alignment.Right, Border.Bottom);
        pdf.addCell("1. Sem.", Alignment.Right, Border.Bottom);
        pdf.addCell("2. Sem.", Alignment.Right, Border.Bottom);
        pdf.addCell("2. Sem.", Alignment.Right, Border.Bottom);
        pdf.setNormal();
    }

    private void payrollFooter() {
        pdf.endTable();;
    }

    private void balanceBlock() {
        Employment e = workload.getEmployment();
        double payment = -(e.getPayment1() + e.getPayment2()) / 2.0;
        double openingBalance = e.getOpeningBalance();
        double closingBalance = workload.getClosingBalance();
        double change = closingBalance - openingBalance;

        pdf.addParagraph("", Alignment.Left);
        pdf.beginTable(30, 50, 20);

        pdf.addCell("", Alignment.Center);
        pdf.setFontSize(10);
        pdf.addCell("IPB: Übersicht", Alignment.Left, Border.Bottom);
        pdf.setFontSize(8);
        pdf.addCell("Saldo", Alignment.Right, Border.Bottom);

        // line 1
        pdf.addCell("", Alignment.Center);
        pdf.addCell("Anfangssaldo", Alignment.Left);
        pdf.addCell(Format.percent(openingBalance, true), Alignment.Right);

        // line 2
        pdf.addCell("", Alignment.Center);
        pdf.addCell("Pensum", Alignment.Left);
        pdf.addCell(Format.percent(workload.summary().total().percentWithAgeRelief(), true), Alignment.Right);

        // line 3
        pdf.addCell("", Alignment.Center);
        pdf.addCell("Ein- und Ausbuchungen (siehe nächste Seite)", Alignment.Left, Border.Bottom);
        pdf.addCell(Format.percent(workload.postings().totalPercent(), true), Alignment.Right, Border.Bottom);

        // line 4
        pdf.addCell("", Alignment.Center);
        pdf.addCell("Auszahlung", Alignment.Left);
        pdf.addCell(Format.percent(payment, true), Alignment.Right);

        // line 5
        pdf.addCell("", Alignment.Left);
        pdf.addCell("Schlussaldo", Alignment.Left);
        pdf.addCell(Format.percent(closingBalance, true), Alignment.Right);

        // line 6
        pdf.addCell("", Alignment.Left);
        pdf.addCell("Veränderung", Alignment.Left);
        pdf.addCell(Format.percent(change, true), Alignment.Right);
        pdf.endTable();
    }

    private void signatureBlock() {
        pdf.addParagraph("", Alignment.Left);
        pdf.addParagraph("", Alignment.Left);
        Division division = workload.getEmployment().getDivision();
        pdf.beginTable(30, 30, 40);
        pdf.addCell(division.getHeadTitle(), Alignment.Left);
        pdf.addCell("Zustimmend zur Kenntnis genommen", Alignment.Left);
        pdf.addCell("", Alignment.Left);

        pdf.addCell(division.getHeadSignature(), 200, 60);
        pdf.addCell("", Alignment.Left, Border.Bottom);
        pdf.addCell("", Alignment.Left, Border.Bottom);

        pdf.addCell(division.getHeadName(), Alignment.Left);
        pdf.addCell("Ort und Datum", Alignment.Left);
        pdf.addCell("Unterschrift", Alignment.Left);
        pdf.endTable();
    }
}
