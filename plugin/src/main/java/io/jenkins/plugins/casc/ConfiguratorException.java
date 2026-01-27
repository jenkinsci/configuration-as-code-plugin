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
import io.jenkins.plugins.casc.model.Source;
import java.util.Collections;
import java.util.List;
import jenkins.model.Jenkins;

/**
 * Exception type for {@link Configurator} issues.
 * @author Oleg Nenashev
 * @since 1.0
 * @see Configurator#configure(CNode, ConfigurationContext)
 * @see Configurator
 */
public class ConfiguratorException extends RuntimeException {

    @CheckForNull
    private final Configurator configurator;

    private final List<String> validAttributes;

    @CheckForNull
    private final String invalidAttribute;

    @CheckForNull
    private final Source source;

    @CheckForNull
    private final String path;

    private ConfiguratorException(
            @CheckForNull Configurator configurator,
            @CheckForNull String message,
            @CheckForNull String invalidAttribute,
            List<String> validAttributes,
            @CheckForNull Throwable cause,
            @CheckForNull Source source,
            @CheckForNull String path) {
        super(message, cause);
        this.configurator = configurator;

        this.invalidAttribute = invalidAttribute;
        this.validAttributes = validAttributes == null ? Collections.emptyList() : validAttributes;
        this.source = source;
        this.path = path;
    }

    public static ConfiguratorException from(CNode node, String message) {
        return new ConfiguratorException(
                null, message, null, Collections.emptyList(), null, node != null ? node.getSource() : null, null);
    }

    public static ConfiguratorException from(CNode node, String message, Throwable cause) {
        return new ConfiguratorException(
                null, message, null, Collections.emptyList(), cause, node != null ? node.getSource() : null, null);
    }

    public static ConfiguratorException from(
            CNode node, Configurator configurator, String path, String message, Throwable cause) {
        Source source = node != null ? node.getSource() : null;
        if (source == null && cause instanceof ConfiguratorException) {
            source = ((ConfiguratorException) cause).getSource();
        }

        return new ConfiguratorException(configurator, message, null, Collections.emptyList(), cause, source, path);
    }

    public ConfiguratorException(@CheckForNull String message) {
        this(null, message, null, Collections.emptyList(), null);
    }

    public ConfiguratorException(@CheckForNull String message, @CheckForNull Throwable cause) {
        this(null, message, null, Collections.emptyList(), cause, null, null);
    }

    public ConfiguratorException(@CheckForNull Configurator configurator, @CheckForNull String message) {
        this(configurator, message, null, Collections.emptyList(), null, null, null);
    }

    public ConfiguratorException(
            @CheckForNull Configurator configurator, @CheckForNull String message, @CheckForNull Throwable cause) {
        this(configurator, message, null, Collections.emptyList(), cause, null, null);
    }

    public ConfiguratorException(
            @CheckForNull Configurator configurator,
            @CheckForNull String message,
            String invalidAttribute,
            List<String> validAttributes,
            @CheckForNull Throwable cause) {
        this(configurator, message, invalidAttribute, validAttributes, cause, null, null);
    }

    @CheckForNull
    public Configurator getConfigurator() {
        return configurator;
    }

    public List<String> getValidAttributes() {
        return validAttributes;
    }

    @CheckForNull
    public String getInvalidAttribute() {
        return invalidAttribute;
    }

    @CheckForNull
    public Source getSource() {
        return source;
    }

    @CheckForNull
    public String getPath() {
        return path;
    }

    public String getErrorMessage() {
        return super.getMessage();
    }

    @Override
    public String getMessage() {
        final String base = (configurator != null)
                ? String.format(
                        "%s: %s",
                        Jenkins.getInstanceOrNull() == null ? configurator.getClass() : configurator.getName(),
                        super.getMessage())
                : super.getMessage();

        if (source == null && path == null) {
            return base;
        }

        StringBuilder sb = new StringBuilder(base);
        sb.append(System.lineSeparator()).append("  at ");
        if (source != null) {
            sb.append(source);
        } else {
            sb.append("unknown source");
        }
        if (path != null) {
            sb.append(" (path: ").append(path).append(")");
        }
        return sb.toString();
    }
}
