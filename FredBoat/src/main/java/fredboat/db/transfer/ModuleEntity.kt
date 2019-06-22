package fredboat.db.transfer

import fredboat.definitions.Module

data class ModuleEntity(
        val module: Module,
        var enabled: Boolean = true
)