/**
Copyright 2025 Sergei Tachenov

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package name.tachenov.plugins.themePatcher.ui

import name.tachenov.plugins.themePatcher.app.ThemeConfig
import javax.swing.AbstractListModel

internal class ThemeListModel(private val allThemes: List<ThemeConfig>) : AbstractListModel<ThemeConfig>() {
    private val order = allThemes.withIndex().associate { it.value to it.index }
    private val list = mutableListOf<ThemeConfig>()

    fun getAvailableThemes(): List<ThemeConfig> = allThemes.filter { it !in list }

    override fun getSize(): Int = list.size

    override fun getElementAt(index: Int): ThemeConfig = list[index]

    fun addTheme(theme: ThemeConfig) {
        val insertionIndex = -list.binarySearchBy(order[theme]) { order[it] } - 1
        list.add(insertionIndex, theme)
        fireIntervalAdded(this, insertionIndex, insertionIndex)
    }

    fun removeTheme(theme: ThemeConfig) {
        val removalIndex = list.indexOf(theme)
        list.removeAt(removalIndex)
        fireIntervalRemoved(this, removalIndex, removalIndex)
    }

    fun setThemes(themes: List<ThemeConfig>) {
        clear()
        list.addAll(themes)
        list.sortBy { order[it] }
        fireIntervalAdded(this, 0, themes.lastIndex)
    }

    private fun clear() {
        if (size == 0) return
        val lastIndex = list.lastIndex
        list.clear()
        fireIntervalRemoved(this, 0, lastIndex)
    }
}
