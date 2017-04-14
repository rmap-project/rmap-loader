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

package info.rmapproject.loader.validation;

import static info.rmapproject.loader.validation.DiscoValidator.validate;
import static info.rmapproject.loader.validation.DiscoValidator.Format.RDF_XML;
import static info.rmapproject.loader.validation.DiscoValidator.Format.TURTLE;

import org.junit.Test;

import info.rmapproject.loader.validation.DiscoValidator.ValidationException;

public class ValidatorTest {

    @Test
    public void successTest() throws Exception {
        validate(this.getClass().getResourceAsStream("/good.ttl"), TURTLE);
    }

    @Test(expected = ValidationException.class)
    public void illegalPredicateTest() {
        validate(this.getClass().getResourceAsStream("/illegal.ttl"), TURTLE);
    }

    @Test(expected = ValidationException.class)
    public void unconnectedTest() {
        validate(this.getClass().getResourceAsStream("/unconnected.ttl"), TURTLE);
    }

    @Test
    public void ieeeTest() throws Exception {
        validate(this.getClass().getResourceAsStream("/6722410.xml"), RDF_XML);
    }

}
