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
import ch.kinet.pensen.calculation.Workload;
import ch.kinet.pensen.data.Division;
import ch.kinet.pensen.data.Employment;

public final class TeacherCoursePDFGenerator {

    private final Document pdf;
    private final Workload workload;

    public static void writePDF(Document pdf, Workload workload) {
        TeacherCoursePDFGenerator instance = new TeacherCoursePDFGenerator(pdf, workload);
        instance.write();
    }

    private TeacherCoursePDFGenerator(Document pdf, Workload workload) {
        this.workload = workload;
        this.pdf = pdf;
    }

    private void write() {
        pdf.addPage(2f, 1.5f);
        header();
        personalBlock();
        courseBlock();
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
        pdf.addCell(Date.formatDMY(Date.today()), Alignment.Right);
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
        pdf.addCell("Auszahlung:", Alignment.Left);
        pdf.addCell(Format.percentSemester(e.getPayment1(), e.getPayment2()), Alignment.Right);

        pdf.addCell("Personalnummer:", Alignment.Left, Border.Bottom);
        pdf.addCell(e.getTeacher().getEmployeeNumber(), Alignment.Left, Border.Bottom);
        pdf.addCell("Anstellungsart:", Alignment.Left, Border.Bottom);
        pdf.addCell(Format.employmentType(e), Alignment.Right, Border.Bottom);
        pdf.endTable();
        pdf.addParagraph(workload.getEmployment().getComments(), Alignment.Left);
    }

    private void courseBlock() {
        courseHeader();
        workload.courses().items().forEachOrdered(item -> {
            pdf.addCell(item.subject().getCode(), Alignment.Left);
            pdf.addCell(item.subject().getDescription(), Alignment.Left);
            pdf.addCell(item.grade().getDescription(), Alignment.Left);
            pdf.addCell(Util.concat(item.schoolClasses().map(sc -> sc.getCode()), ", "), Alignment.Left);
            pdf.addCell(Format.lessons(item.lessons1()), Alignment.Right);
            pdf.addCell(Format.percent(item.percent1(), false), Alignment.Right);
            pdf.addCell(Format.lessons(item.lessons2()), Alignment.Right);
            pdf.addCell(Format.percent(item.percent2(), false), Alignment.Right);
        });

        courseFooter();
    }

    private void courseHeader() {
        pdf.beginTable(10, 25, 10, 15, 10, 10, 10, 10);
        pdf.setBold();
        pdf.addCell("Fach", Alignment.Left, Border.Bottom);
        pdf.addCell("", Alignment.Left, Border.Bottom);
        pdf.addCell("Stufe", Alignment.Left, Border.Bottom);
        pdf.addCell("Klassen", Alignment.Left, Border.Bottom);
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
        pdf.addCell("", Alignment.Left, Border.Top);
        pdf.addCell(Format.lessons(courses.lessons1()), Alignment.Right, Border.Top);
        pdf.addCell(Format.percent(courses.percent1(), true), Alignment.Right, Border.Top);
        pdf.addCell(Format.lessons(courses.lessons2()), Alignment.Right, Border.Top);
        pdf.addCell(Format.percent(courses.percent2(), true), Alignment.Right, Border.Top);
        pdf.setNormal();
        pdf.endTable();
    }
}
