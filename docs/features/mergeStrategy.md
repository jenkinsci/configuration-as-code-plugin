# Merge Strategy

It's possible to load multiple config files. CasC can load YAML files from a directory.
And it's convenient to maintain if we split different parts of Jenkins into multiple files.

## Support strategies

* [IrreconcilableMergeStrategy]((../../plugin/src/main/java/io/jenkins/plugins/casc/yaml/IrreconcilableMergeStrategy.java)) (default)
    * The strategy name is `errorOnConflict`.
    * Throw an exception if there's a conflict exist in multiple YAML files.
* [OverrideMergeStrategy](../../plugin/src/main/java/io/jenkins/plugins/casc/yaml/OverrideMergeStrategy.java)
    * The strategy name is `override`
    * Override the config files according to the loading order.
    
## Use cases

You can provide two YAML config files. They can be system and user config files. Then allow users to change the users' part.
So, the users' config file can override the system one.

## How to

There are two ways to configure the strategy name.

* set the environment `CASC_MERGE_STRATEGY`
* set the system property `casc.merge.strategy`

The strategy name could be `errorOnConflict` or `override`.
