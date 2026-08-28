package io.github.afsm.ide

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

internal object AfsmIcons {
    @JvmField
    val Graph: Icon = IconLoader.getIcon("/icons/afsmGraph.svg", AfsmIcons::class.java)
}
