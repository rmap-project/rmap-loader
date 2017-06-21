/*******************************************************************************
 * Copyright 2017 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This software was produced as part of the RMap Project (http://rmap-project.info),
 * The RMap Project was funded by the Alfred P. Sloan Foundation and is a 
 * collaboration between Data Conservancy, Portico, and IEEE.
 *******************************************************************************/
package info.rmapproject.loader;

import java.util.Date;

/**
 * Interface to support management of a registry to record run times of harvesting processes.
 * 
 * @author karen.hanson@jhu.edu
 *
 */

public interface HarvestRunRegistry {

	/**
	 * Add a new run date for the loader name specified
	 * @param loaderName lookup name for loader
	 * @param date date run
	 */
    public void addRunDate(String loaderName, Date date);

    /**
     * Retrieve most recent run date for loader name specified
     * @param loaderName lookup name for loader
     * @return
     */
    public Date getLastRunDate(String loaderName);
	
}
