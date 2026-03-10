package io.jenkins.plugins.casc.yaml;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.ExtensionList;
import java.util.Optional;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.StringUtils;

public class MergeStrategyFactory {
    private static MergeStrategy getMergeStrategy(@NonNull String name) {
        ExtensionList<MergeStrategy> mergeStrategyList = Jenkins.get().getExtensionList(MergeStrategy.class);
        Optional<MergeStrategy> opt = mergeStrategyList.stream()
                .filter(strategy -> strategy.getName().equals(name))
                .findFirst();
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
