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

import ch.kinet.DataManager;
import ch.kinet.JsonObject;
import ch.kinet.Mail;
import ch.kinet.Mailer;
import ch.kinet.http.Data;
import ch.kinet.pdf.Document;
import ch.kinet.pensen.data.Authorisation;
import ch.kinet.pensen.data.Division;
import ch.kinet.pensen.data.Employment;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.Teacher;
import ch.kinet.pensen.calculation.Workloads;
import ch.kinet.pensen.calculation.Workload;
import ch.kinet.pensen.server.Configuration;
import javax.mail.MessagingException;

public final class WorkloadMail extends JobImplementation {

    private PensenData pensenData;
    private SchoolYear schoolYear;
    private Workload workload;
    private Workloads workloads;
    private String mailBody;
    private String mailFrom;
    private String mailSubject;
    private static final String BODY =
        "Guten Tag\n\n" +
        "Beiliegend ist das aktuelle Pensenblatt.\n\n" +
        "Freundliche Grüsse\n" +
        "Die Schulleitung\n";

    public WorkloadMail() {
        super("Pensenblatt versenden");
    }

    @Override
    public void initialize(DataManager dataManager) {
        pensenData = dataManager.getData(PensenData.class);
    }

    @Override
    public boolean isAllowed(Authorisation authorisation) {
        return authorisation != null;
    }

    @Override
    public boolean parseData(JsonObject data) {
        schoolYear = pensenData.getSchoolYearById(data.getObjectId("schoolYear", -1));
        Teacher teacher = pensenData.getTeacherById(data.getObjectId("teacher", -1));
        Division division = pensenData.getDivisionById(data.getObjectId("division", -1));
        mailBody = data.getString("mailBody");
        mailFrom = data.getString("mailFrom");
        mailSubject = data.getString("mailSubject");
        if (schoolYear == null) {
            setErrorMessage("Ein Schuljahr muss ausgewählt werden.");
            return false;
        }

        if (teacher == null) {
            workloads = pensenData.loadWorkloads(schoolYear, division);
        }
        else {
            Employment employment = pensenData.loadEmployment(schoolYear, teacher);
            if (employment == null) {
                setErrorMessage("Eine Lehrperson mit Anstellung muss ausgewählt werden.");
                return false;
            }

            workload = pensenData.loadWorkload(employment);
        }

        return true;
    }

    @Override
    public long getStepCount() {
        return workloads == null ? 1 : workloads.size();
    }

    @Override
    public void run(Authorisation creator, JobCallback callback) {
        Mailer mailer = Configuration.getInstance().createMailer(mailFrom);
        if (workloads == null) {
            sendMail(mailer, workload);
            callback.step();
        }
        else {
            workloads.teachers().forEachOrdered(teacher -> {
                if (!hasError()) {
                    sendMail(mailer, workloads.getWorkload(teacher));
                }

                callback.step();
            });
        }
    }

    private void sendMail(Mailer mailer, Workload workload) {
        String email = workload.getTeacher().getEmail();
        if (email != null && email.contains("@")) {
            Mail mail = Mail.create();
            Document pdf = Document.createPortrait(fileName(workload.getTeacher()));
            WorkloadPDFGenerator.createPDF(pdf, workload);
            PostingsPDFGenerator.createPDF(pdf, workload);
            Data product = pdf.toData();
            mail.setSubject(mailSubject);
            mail.setBody(mailBody);
            mail.addTo(workload.getTeacher().getEmail());
            mail.addAttachment(product);
            try {
                mailer.sendMail(mail);
            }
            catch (MessagingException ex) {
                if (ex.getMessage().startsWith("Could not connect to SMTP host")) {
                    setErrorMessage("Kann keine Verbindung zum SMTP-Server herstellen.");
                }
                else {
                    setErrorMessage("Fehler beim Versenden der E-Mail: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }
    }

    private String fileName(Teacher teacher) {
        StringBuilder result = new StringBuilder();
        result.append("Pensenblatt_");
        result.append(teacher.getCode());
        result.append('_');
        result.append(schoolYear.getCode());
        result.append(".pdf");
        return result.toString();
    }
}
