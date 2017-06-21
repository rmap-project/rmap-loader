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

import info.rmapproject.loader.model.RecordInfo;

/**
 * @author apb@jhu.edu
 */
public interface HarvestRecordRegistry {

    public void register(RecordInfo info, URI discoURI);

    public HarvestRecordStatus getStatus(RecordInfo info);

}