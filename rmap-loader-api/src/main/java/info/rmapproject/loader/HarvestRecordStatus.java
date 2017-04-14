/*
 * Copyright 2017 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.rmapproject.loader;

import java.net.URI;

/**
 * Status of a harvest record.
 *
 * @author apb@jhu.edu
 */
public class HarvestRecordStatus {

    boolean recordExists = false;

    boolean isLatest = false;

    URI latest;

    public boolean recordExists() {
        return recordExists;
    }

    public void setRecordExists(boolean exists) {
        this.recordExists = exists;
    }

    public boolean isLatest() {
        return isLatest;
    }

    public void setIsLatest(boolean isLatest) {
        this.isLatest = isLatest;
    }

    public URI latest() {
        return latest;
    }

    public void setLatest(URI latest) {
        this.latest = latest;
    }
}
