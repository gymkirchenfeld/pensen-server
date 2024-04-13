/*
 * Copyright (C) 2023 - 2024 by Stefan Rothe
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

import ch.kinet.TimeUtil;
import ch.kinet.Util;
import ch.kinet.pdf.Alignment;
import ch.kinet.pdf.Border;
import ch.kinet.pdf.Document;
import ch.kinet.pdf.VerticalAlignment;
import ch.kinet.pensen.calculation.Postings;
import ch.kinet.pensen.calculation.Workload;
import ch.kinet.pensen.data.Division;
import ch.kinet.pensen.data.Employment;
import java.time.LocalDate;

public class PostingsPDFGenerator {

    private final Document pdf;
    private final Workload workload;

    public static void createPDF(Document pdf, Workload workload) {
        PostingsPDFGenerator instance = new PostingsPDFGenerator(pdf, workload);
        instance.create();
    }

    private PostingsPDFGenerator(Document pdf, Workload workload) {
        this.workload = workload;
        this.pdf = pdf;
    }

    private void create() {
        if (workload.postings().isEmpty()) {
            return;
        }

        pdf.addPage(2f, 1.5f);
        header();
        personalBlock();
        postingsBlock();
    }

    private void header() {
        Division division = workload.getEmployment().getDivision();
        pdf.beginTable(20, 60, 20);
        pdf.addCell(division.getLogo(), 200, 18);
        pdf.setVerticalAlignment(VerticalAlignment.Bottom);
        pdf.setFontSize(12);
        StringBuilder title = new StringBuilder();
        title.append("Ein- und Ausbuchungen SJ ");
        title.append(workload.getSchoolYear().getDescription());
        pdf.addCell(title.toString(), Alignment.Center);

        pdf.setFontSize(8);
        pdf.addCell(TimeUtil.formatDMY(LocalDate.now()), Alignment.Right);
        pdf.endTable();
    }

    private void personalBlock() {
        Employment e = workload.getEmployment();
        pdf.beginTable(25, 25, 25, 25);
        pdf.addCell("Name:", Alignment.Left, Border.Top);
        pdf.setBold();
        pdf.addCell(Format.name(e.getTeacher()), Alignment.Left, Border.Top);
        pdf.setNormal();
        pdf.addCell("Altersentlastung 1. Semester:", Alignment.Left, Border.Top);
        pdf.addCell(Format.percent(workload.ageReliefFactor1(), false), Alignment.Right, Border.Top);

        pdf.addCell("Kürzel:", Alignment.Left);
        pdf.addCell(e.getTeacher().getCode(), Alignment.Left);
        pdf.addCell("Altersentlastung 2. Semester:", Alignment.Left);
        pdf.addCell(Format.percent(workload.ageReliefFactor2(), false), Alignment.Right);

        pdf.addCell("Personalnummer:", Alignment.Left, Border.Bottom);
        pdf.addCell(e.getTeacher().getEmployeeNumber(), Alignment.Left, Border.Bottom);
        pdf.addCell("Anstellungsart:", Alignment.Left, Border.Bottom);
        pdf.addCell(Format.employmentType(e), Alignment.Right, Border.Bottom);
        pdf.endTable();
        pdf.addParagraph(workload.getEmployment().getComments(), Alignment.Left);
    }

    private void postingsBlock() {
        Postings postings = workload.postings();
        pdf.beginTable(50, 10, 10, 10, 10, 10);
        pdf.setBold();
        pdf.addCell("Bezeichnung", Alignment.Left, Border.Bottom);
        pdf.addCell("Lektionen", Alignment.Right, Border.Bottom);
        pdf.addCell("Vollpensum", Alignment.Right, Border.Bottom);
        pdf.addCell("Prozent", Alignment.Right, Border.Bottom);
        pdf.addCell("AE", Alignment.Right, Border.Bottom);
        pdf.addCell("Total", Alignment.Right, Border.Bottom);
        pdf.setNormal();
        postings.items().forEachOrdered(posting -> postingBlock(posting));
        // footer
        pdf.setBold();
        pdf.addCell("Total", Alignment.Left, Border.Top);
        pdf.addCell("", Alignment.Right, Border.Top); // Lektionen
        pdf.addCell("", Alignment.Right, Border.Top); // Vollpensum
        pdf.addCell("", Alignment.Right, Border.Top); // Prozent
        pdf.addCell("", Alignment.Right, Border.Top); // AE
        pdf.addCell(Format.percent(postings.totalPercent(), true), Alignment.Right, Border.Top); // Total
        pdf.endTable();
    }

    private void postingBlock(Postings.Item posting) {
        pdf.setBold();
        pdf.addCell(title(posting), Alignment.Left, Border.Top);
        pdf.setNormal();
        pdf.addCell("", Alignment.Right, Border.Top); // Lektionen
        pdf.addCell("", Alignment.Right, Border.Top); // Vollpensum
        pdf.addCell("", Alignment.Right, Border.Top); // Prozent
        pdf.addCell("", Alignment.Right, Border.Top); // AE
        pdf.addCell("", Alignment.Right, Border.Top); // Total
        posting.streamDetails().forEachOrdered(entry -> {
            pdf.addCell(entry.payrollType().getDescription(), Alignment.Left);
            pdf.addCell(Format.lessons(entry.lessons()), Alignment.Right);
            pdf.addCell(Format.lessons(entry.weeklyLessons()), Alignment.Right);
            pdf.addCell(Format.percent(entry.percentWithoutAgeRelief(), false), Alignment.Right); // Prozent
            pdf.addCell(Format.percent(entry.ageRelief(), false), Alignment.Right); // AE
            pdf.addCell(Format.percent(entry.percentWithAgeRelief(), false), Alignment.Right); // Total
        });
    }

    private static String title(Postings.Item entry) {
        StringBuilder result = new StringBuilder();
        result.append(entry.description());
        result.append(" (");
        String start = TimeUtil.formatDMY(entry.startDate());
        String end = TimeUtil.formatDMY(entry.endDate());
        result.append(start);
        if (end != null && !Util.equal(start, end)) {
            result.append(" - ");
            result.append(end);
        }

        result.append(")");
        return result.toString();
    }
}
