package io.jenkins.plugins.casc.impl.configurators;

import static java.util.Collections.singletonList;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Descriptor;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.RootElementConfigurator;
import io.jenkins.plugins.casc.model.Mapping;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Define a Configurator for a Descriptor
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Restricted(NoExternalUse.class)
public class DescriptorConfigurator extends BaseConfigurator<Descriptor>
        implements RootElementConfigurator<Descriptor> {

    private final List<String> names;
    private final Descriptor descriptor;
    private final Class target;

    public DescriptorConfigurator(Descriptor descriptor) {
        this.descriptor = descriptor;
        this.target = descriptor.getClass();
        this.names = resolvePossibleNames(descriptor);
    }

    @NonNull
    @Override
    public String getName() {
        return names.get(0);
    }

    @NonNull
    @Override
    public List<String> getNames() {
        return names;
    }

    @Override
    public Class<Descriptor> getTarget() {
        return target;
    }

    @Override
    public Descriptor getTargetComponent(ConfigurationContext context) {
        return descriptor;
    }

    @Override
    protected Descriptor instance(Mapping mapping, ConfigurationContext context) {
        return descriptor;
    }

    private List<String> resolvePossibleNames(Descriptor descriptor) {
        return Optional.ofNullable(descriptor.getClass().getAnnotation(Symbol.class))
                .map(s -> Arrays.asList(s.value()))
                .orElseGet(() -> {
                    Class<?> typeParam = extractDescriptorTypeParameter(descriptor.getClass());

                    Class<?> targetClass = typeParam != null
                            ? typeParam
                            : descriptor.getKlass().toJavaClass();

                    while (targetClass.isAnonymousClass()) {
                        targetClass = targetClass.getSuperclass();
                    }

                    return singletonList(fromPascalCaseToCamelCase(targetClass.getSimpleName()));
                });
    }

    private Class<?> extractDescriptorTypeParameter(Class<?> clazz) {
        while (clazz != null && clazz != Object.class) {
            Type genericSuperclass = clazz.getGenericSuperclass();

            if (genericSuperclass instanceof ParameterizedType pt) {
                Type rawType = pt.getRawType();

                if (rawType instanceof Class && Descriptor.class.isAssignableFrom((Class<?>) rawType)) {
                    Type[] args = pt.getActualTypeArguments();

                    if (args.length > 0) {
                        Type typeArg = args[0];

                        if (typeArg instanceof Class) {
                            return (Class<?>) typeArg;
                        } else if (typeArg instanceof ParameterizedType) {
                            Type nestedRawType = ((ParameterizedType) typeArg).getRawType();
                            if (nestedRawType instanceof Class) {
                                return (Class<?>) nestedRawType;
                            }
                        }
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private static String fromPascalCaseToCamelCase(String s) {
        if (s == null || s.isEmpty()) {
            return s != null ? s : "";
        }
        StringBuilder sb = new StringBuilder(s);
        sb.setCharAt(0, Character.toLowerCase(s.charAt(0)));
        return sb.toString();
    }
}
