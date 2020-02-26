Configuration as Code can load multiple YAML files. The conflicts might exists among these config files.
So, a YAML file merge strategy is necessary. There're two build-in strategies:

| Name | Description |
|---|---|
| `irreconcilable` | Throw an exception if there's a conflict (default **behaviour**) |
| `order` | Take the first value if there're more than one same key exists |

If you want to switch to a different strategy, please set the environment value by the strategy name. 
For example: `-DCASC_MERGE_STRATEGY=order`
