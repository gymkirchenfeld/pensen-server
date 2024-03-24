/*
 * Copyright (C) 2022 - 2024 by Stefan Rothe
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

import ch.kinet.pdf.Alignment;
import ch.kinet.pdf.Border;
import ch.kinet.pdf.Document;
import ch.kinet.pensen.data.Course;
import ch.kinet.pensen.data.PayrollType;
import ch.kinet.pensen.data.SemesterEnum;
import java.util.List;
import java.util.stream.Collectors;

public class SubjectCoursePDFGenerator {

    private final List<Course> courses;
    private final boolean openWorkload;
    private final Document pdf;
    private final String title;
    private double lessons1;
    private double lessons2;
    private double percent1;
    private double percent2;

    public static void writePDF(Document pdf, String title, List<Course> courses, boolean openWorkload) {
        SubjectCoursePDFGenerator instance = new SubjectCoursePDFGenerator(pdf, title, courses, openWorkload);
        instance.write();
    }

    private SubjectCoursePDFGenerator(Document pdf, String title, List<Course> courses, boolean openWorkload) {
        this.courses = courses;
        this.openWorkload = openWorkload;
        this.pdf = pdf;
        this.title = title;
    }

    private void write() {
        pdf.addPage(2f, 1.5f);
        header();
        courses.stream().sorted().forEachOrdered(entry -> {
            PayrollType et = entry.payrollType();
            double l1 = entry.lessons(SemesterEnum.First);
            double l2 = entry.lessons(SemesterEnum.Second);
            double p1 = entry.percent(SemesterEnum.First);
            double p2 = entry.percent(SemesterEnum.Second);

            pdf.addCell(entry.getSubject().getCode(), Alignment.Left);
            pdf.addCell(entry.getSubject().getDescription(), Alignment.Left);
            pdf.addCell(entry.getGrade().getDescription(), Alignment.Left);
            pdf.addCell(entry.schoolClasses().map(sc -> sc.getCode()).collect(Collectors.joining(", ")), Alignment.Left);
            pdf.addCell(Format.lessons(entry.getLessons1()), Alignment.Right);
            pdf.addCell("", Alignment.Left);
            if (openWorkload) {
                pdf.addCell(Format.percent(p1, false), Alignment.Right);
            }
            else {
                pdf.addCell(entry.teachers(SemesterEnum.First).map(t -> t.getCode()).collect(Collectors.joining(", ")), Alignment.Left);
            }

            pdf.addCell(Format.lessons(entry.getLessons2()), Alignment.Right);
            pdf.addCell("", Alignment.Left);
            if (openWorkload) {
                pdf.addCell(Format.percent(p2, false), Alignment.Right);
            }
            else {
                pdf.addCell(entry.teachers(SemesterEnum.Second).map(t -> t.getCode()).collect(Collectors.joining(", ")), Alignment.Left);
            }

            this.lessons1 += l1;
            this.lessons2 += l2;
            this.percent1 += p1;
            this.percent2 += p2;
        });

        pdf.setBold();
        pdf.addCell("Total", Alignment.Left, Border.Top);
        pdf.addCell("", Alignment.Left, Border.Top); // description
        pdf.addCell("", Alignment.Left, Border.Top); // grade
        pdf.addCell("", Alignment.Left, Border.Top); // school classes
        pdf.addCell(Format.lessons(lessons1), Alignment.Right, Border.Top); // lessons 1
        pdf.addCell("", Alignment.Left, Border.Top);
        if (openWorkload) {
            pdf.addCell(Format.percent(percent1, false), Alignment.Right, Border.Top);
        }
        else {
            pdf.addCell("", Alignment.Left, Border.Top); // teachers
        }

        pdf.addCell(Format.lessons(lessons2), Alignment.Right, Border.Top); // lessons 2
        pdf.addCell("", Alignment.Left, Border.Top);
        if (openWorkload) {
            pdf.addCell(Format.percent(percent2, false), Alignment.Right, Border.Top);
        }
        else {
            pdf.addCell("", Alignment.Left, Border.Top); // teachers
        }

        pdf.endTable();
    }

    private void header() {
        pdf.setFontSize(12);
        pdf.setBold();
        pdf.addParagraph(title, Alignment.Left);
        pdf.setFontSize(8);
        pdf.setNormal();
        pdf.beginTable(7, 20, 6, 17, 5, 2, 10, 5, 2, 10);
        pdf.addCell("Fach", Alignment.Left, Border.Bottom);
        pdf.addCell("", Alignment.Left, Border.Bottom); // subject description
        pdf.addCell("Stufe", Alignment.Left, Border.Bottom);
        pdf.addCell("Klassen", Alignment.Left, Border.Bottom);
        pdf.addCell("1. Sem.", Alignment.Left, Border.Bottom);
        pdf.addCell("", Alignment.Left, Border.Bottom);
        pdf.addCell("", Alignment.Left, Border.Bottom);
        pdf.addCell("2. Sem.", Alignment.Left, Border.Bottom);
        pdf.addCell("", Alignment.Left, Border.Bottom);
        pdf.addCell("", Alignment.Left, Border.Bottom);
    }
}
