package io.jenkins.plugins.casc.yaml;

import hudson.ExtensionList;
import java.util.Optional;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

public class MergeStrategyFactory {
    private static final Logger LOGGER = Logger.getLogger(MergeStrategyFactory.class.getName());

    private static final String strategyName;

    static {
        String strategyEnv = System.getenv("CASC_MERGE_STRATEGY");
        if (StringUtils.isEmpty(strategyEnv)) {
            strategyEnv = System.getProperty("casc.merge.strategy", "default");
        }
        strategyName = strategyEnv;

        LOGGER.info("Get merge strategy: " + strategyName);
    }

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
        return MergeStrategyFactory.getMergeStrategy(
            StringUtils.isEmpty(strategyName) ? "default" : strategyName);
    }
}
