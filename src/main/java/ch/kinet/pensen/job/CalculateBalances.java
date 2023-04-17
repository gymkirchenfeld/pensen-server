/*
 * Copyright (C) 2022 by Stefan Rothe
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
import ch.kinet.pensen.data.Authorisation;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.SchoolYear;
import java.util.List;
import java.util.stream.Collectors;

public final class CalculateBalances extends JobImplementation {

    private PensenData pensenData;
    private List<SchoolYear> schoolYears;

    public CalculateBalances() {
        super("IPB-Saldi aktualisieren");
    }

    @Override
    public void initialize(DataManager dataManager) {
        pensenData = dataManager.getData(PensenData.class);
        schoolYears = pensenData.streamSchoolYears().filter(
            schoolYear -> !schoolYear.isArchived()
        ).collect(Collectors.toList());
    }

    @Override
    public boolean isAllowed(Authorisation authorisation) {
        return authorisation != null && authorisation.isAdmin();
    }

    @Override
    public boolean parseData(JsonObject data) {
        return true;
    }

    @Override
    public long getStepCount() {
        return schoolYears.size();
    }

    @Override
    public void run(Authorisation creator, JobCallback callback) {
        callback.info("Aktualisierte alle IPB-Saldi.");
        for (SchoolYear schoolYear : schoolYears) {
            callback.info("Aktualisierte IPB-Saldi im Schuljahr {0}.", schoolYear.getCode());
            pensenData.recalculateBalance(schoolYear);
            callback.step();
        }

        callback.info("Aktualisierung ist beendet worden.");
    }
}
