/**
 * Copyright (c) 2008, http://www.snakeyaml.org
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
package io.jenkins.plugins.casc.snakeyaml;

public class LoaderOptions {

    private boolean allowDuplicateKeys = true;

    public boolean isAllowDuplicateKeys() {
        return allowDuplicateKeys;
    }

    /**
     * Allow/Reject duplicate map keys in the YAML file.
     *
     * Default is to allow.
     *
     * YAML 1.1 is slightly vague around duplicate entries in the YAML file. The
     * best reference is <a href="http://www.yaml.org/spec/1.1/#id862121">
     * 3.2.1.3. Nodes Comparison</a> where it hints that a duplicate map key is
     * an error.
     *
     * For future reference, YAML spec 1.2 is clear. The keys MUST be unique.
     * <a href="http://www.yaml.org/spec/1.2/spec.html#id2759572">1.3. Relation
     * to JSON</a>
     * @param allowDuplicateKeys false to reject duplicate mapping keys
     */
    public void setAllowDuplicateKeys(boolean allowDuplicateKeys) {
        this.allowDuplicateKeys = allowDuplicateKeys;
    }

}
