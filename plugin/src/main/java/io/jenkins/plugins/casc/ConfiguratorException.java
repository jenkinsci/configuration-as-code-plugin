/*
 * Copyright (c) 2018 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.jenkins.plugins.casc;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import io.jenkins.plugins.casc.model.CNode;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Exception type for {@link Configurator} issues.
 * @author Oleg Nenashev
 * @since 1.0
 * @see Configurator#configure(CNode, ConfigurationContext)
 * @see Configurator
 */
public class ConfiguratorException extends IOException {

    @CheckForNull
    private final Configurator configurator;

    private final List<String> validAttributes;

    @CheckForNull
    private final String invalidAttribute;

    public ConfiguratorException(
            @CheckForNull Configurator configurator,
            @CheckForNull String message,
            String invalidAttribute,
            List<String> validAttributes,
            @CheckForNull Throwable cause) {
        super(message, cause);
        this.configurator = configurator;

        this.invalidAttribute = invalidAttribute;
        this.validAttributes = validAttributes;
    }

    public ConfiguratorException(
            @CheckForNull Configurator configurator, @CheckForNull String message, @CheckForNull Throwable cause) {
        super(message, cause);
        this.configurator = configurator;
        this.invalidAttribute = null;
        this.validAttributes = Collections.emptyList();
    }

    public ConfiguratorException(@CheckForNull String message, @CheckForNull Throwable cause) {
        this(null, message, null, Collections.emptyList(), cause);
    }

    public ConfiguratorException(@CheckForNull Configurator configurator, @CheckForNull String message) {
        this(configurator, message, null, Collections.emptyList(), null);
    }

    public ConfiguratorException(@CheckForNull String message) {
        this(null, message, null, Collections.emptyList(), null);
    }

    public ConfiguratorException(@CheckForNull Throwable cause) {
        this(null, null, null, Collections.emptyList(), cause);
    }

    @CheckForNull
    public Configurator getConfigurator() {
        return configurator;
    }

    public List<String> getValidAttributes() {
        return validAttributes;
    }

    public String getInvalidAttribute() {
        return invalidAttribute;
    }

    @Override
    public String getMessage() {
        if (configurator != null) {
            return String.format("%s: %s", configurator.getName(), super.getMessage());
        }
        return super.getMessage();
    }
}
