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

public final class JobThread extends Thread {

    private final Job job;
    private final JobImplementation implementation;

    public JobThread(Job job, JobImplementation implementation) {
        this.job = job;
        this.implementation = implementation;
    }

    @Override
    public void run() {
        try {
            System.out.println("Starting job " + implementation.getTitle());
            implementation.run(job.getCreator(), job);
            job.succeeded(implementation.getProduct());
        }
        catch (RuntimeException ex) {
            job.failed(ex);
        }
    }
}
