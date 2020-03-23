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
     * Get strategy name from environment variables
     * @return MergeStrategy
     */
    public static MergeStrategy getMergeStrategy() {
        String strategyName = getDefaultStrategy();

        return MergeStrategyFactory.getMergeStrategy(
            StringUtils.isEmpty(strategyName) ? MergeStrategy.DEFAULT_STRATEGY : strategyName);
    }

    private static String getDefaultStrategy() {
        String strategyEnv = System.getenv("CASC_MERGE_STRATEGY");
        if (StringUtils.isEmpty(strategyEnv)) {
            strategyEnv = System.getProperty("casc.merge.strategy", MergeStrategy.DEFAULT_STRATEGY);
        }
        return strategyEnv;
    }
}
