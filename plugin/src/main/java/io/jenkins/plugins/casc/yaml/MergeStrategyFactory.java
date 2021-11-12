package io.jenkins.plugins.casc.yaml;

import hudson.ExtensionList;
import java.util.Optional;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

public class MergeStrategyFactory {
    private static MergeStrategy getMergeStrategy(@Nonnull String name) {
        ExtensionList<MergeStrategy> mergeStrategyList = Jenkins.getInstance()
            .getExtensionList(MergeStrategy.class);
        Optional<MergeStrategy> opt = mergeStrategyList.stream().
            filter(strategy -> strategy.getName().equals(name)).findFirst();
        return opt.orElse(null);
    }

    /**
     * Get strategy from name
     * @param strategyName is the name of strategy
     * @return MergeStrategy
     */
    public static MergeStrategy getMergeStrategyOrDefault(String strategyName) {
        return MergeStrategyFactory.getMergeStrategy(
            StringUtils.isEmpty(strategyName) ? MergeStrategy.DEFAULT_STRATEGY : strategyName);
    }
}
