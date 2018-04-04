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

package org.jenkinsci.plugins.casc;

import javax.annotation.CheckForNull;
import java.io.IOException;

/**
 * Exception type for {@link Configurator} issues.
 * @author Oleg Nenashev
 * @since TODO
 * @see Configurator#configure(Object)
 * @see ElementConfigurator
 */
public class ConfiguratorException extends IOException {

    @CheckForNull
    private final ElementConfigurator configurator;

    public ConfiguratorException(@CheckForNull ElementConfigurator configurator, @CheckForNull String message, @CheckForNull Throwable cause) {
        super(message, cause);
        this.configurator = configurator;
    }

    public ConfiguratorException(@CheckForNull String message, @CheckForNull Throwable cause) {
        this(null, message, cause);
    }

    public ConfiguratorException(@CheckForNull ElementConfigurator configurator, @CheckForNull String message) {
        this(configurator, message, null);
    }

    public ConfiguratorException(@CheckForNull String message) {
        this(null, message, null);
    }

    public ConfiguratorException(@CheckForNull Throwable cause) {
        this(null, null, cause);
    }

    @CheckForNull
    public ElementConfigurator getConfigurator() {
        return configurator;
    }

    @Override
    public String getMessage() {
        if (configurator != null) {
            return String.format("%s: %s", configurator.getName(), super.getMessage());
        }
        return super.getMessage();
    }
}
