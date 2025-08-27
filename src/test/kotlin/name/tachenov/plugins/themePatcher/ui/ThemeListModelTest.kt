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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

internal class ThemeListModelTest {
    private lateinit var sut: ThemeListModel
    private lateinit var events: List<EventData>

    @BeforeEach
    fun setUp() {
        sut = ThemeListModel(TEST_THEMES)
        events = mutableListOf()
        sut.addListDataListener(object : ListDataListener {
            override fun intervalAdded(e: ListDataEvent) {
                recordEvent(e)
            }

            override fun intervalRemoved(e: ListDataEvent) {
                recordEvent(e)
            }

            override fun contentsChanged(e: ListDataEvent) {
                recordEvent(e)
            }

            private fun recordEvent(e: ListDataEvent) {
                assertThat(e.source).isEqualTo(sut)
                events += EventData(e.type, e.index0, e.index1)
            }
        })
    }

    @Test
    fun `all available themes`() {
        assertThat(sut.getAvailableThemes()).containsExactlyElementsOf(TEST_THEMES)
        assertThat(sut.size).isZero()
    }

    @Test
    fun `some themes used`() {
        sut.addTheme(TEST_THEMES[0])
        assertThat(sut.getAvailableThemes()).containsExactly(TEST_THEMES[1], TEST_THEMES[2])
        assertThat(sut.size).isEqualTo(1)
        assertThat(sut.getElementAt(0)).isEqualTo(TEST_THEMES[0])
        assertThat(events).containsExactly(
            EventData(ListDataEvent.INTERVAL_ADDED, 0, 0),
        )
    }

    @Test
    fun `add themes in order`() {
        sut.addTheme(TEST_THEMES[0])
        sut.addTheme(TEST_THEMES[1])
        assertThat(sut.getAvailableThemes()).containsExactly(TEST_THEMES[2])
        assertThat(sut.size).isEqualTo(2)
        assertThat(sut.getElementAt(0)).isEqualTo(TEST_THEMES[0])
        assertThat(sut.getElementAt(1)).isEqualTo(TEST_THEMES[1])
        assertThat(events).containsExactly(
            EventData(ListDataEvent.INTERVAL_ADDED, 0, 0),
            EventData(ListDataEvent.INTERVAL_ADDED, 1, 1),
        )
    }

    @Test
    fun `add themes not in order`() {
        sut.addTheme(TEST_THEMES[1])
        sut.addTheme(TEST_THEMES[0])
        assertThat(sut.getAvailableThemes()).containsExactly(TEST_THEMES[2])
        assertThat(sut.size).isEqualTo(2)
        assertThat(sut.getElementAt(0)).isEqualTo(TEST_THEMES[0])
        assertThat(sut.getElementAt(1)).isEqualTo(TEST_THEMES[1])
        assertThat(events).containsExactly(
            EventData(ListDataEvent.INTERVAL_ADDED, 0, 0),
            EventData(ListDataEvent.INTERVAL_ADDED, 0, 0),
        )
    }

    @Test
    fun `add all themes`() {
        sut.addTheme(TEST_THEMES[1])
        sut.addTheme(TEST_THEMES[0])
        sut.addTheme(TEST_THEMES[2])
        assertThat(sut.getAvailableThemes()).isEmpty()
        assertThat(sut.size).isEqualTo(3)
        assertThat(sut.getElementAt(0)).isEqualTo(TEST_THEMES[0])
        assertThat(sut.getElementAt(1)).isEqualTo(TEST_THEMES[1])
        assertThat(sut.getElementAt(2)).isEqualTo(TEST_THEMES[2])
        assertThat(events).containsExactly(
            EventData(ListDataEvent.INTERVAL_ADDED, 0, 0),
            EventData(ListDataEvent.INTERVAL_ADDED, 0, 0),
            EventData(ListDataEvent.INTERVAL_ADDED, 2, 2),
        )
    }

    @Test
    fun `set themes to empty list in order`() {
        sut.setThemes(listOf(TEST_THEMES[0], TEST_THEMES[1]))
        assertThat(sut.getAvailableThemes()).containsExactly(TEST_THEMES[2])
        assertThat(sut.size).isEqualTo(2)
        assertThat(sut.getElementAt(0)).isEqualTo(TEST_THEMES[0])
        assertThat(sut.getElementAt(1)).isEqualTo(TEST_THEMES[1])
        assertThat(events).containsExactly(
            EventData(ListDataEvent.INTERVAL_ADDED, 0, 1),
        )
    }

    @Test
    fun `set themes to empty list not in order`() {
        sut.setThemes(listOf(TEST_THEMES[1], TEST_THEMES[0]))
        assertThat(sut.getAvailableThemes()).containsExactly(TEST_THEMES[2])
        assertThat(sut.size).isEqualTo(2)
        assertThat(sut.getElementAt(0)).isEqualTo(TEST_THEMES[0])
        assertThat(sut.getElementAt(1)).isEqualTo(TEST_THEMES[1])
        assertThat(events).containsExactly(
            EventData(ListDataEvent.INTERVAL_ADDED, 0, 1),
        )
    }

    @Test
    fun `set themes to non empty list not in order`() {
        sut.addTheme(TEST_THEMES[2])
        sut.setThemes(listOf(TEST_THEMES[1], TEST_THEMES[0]))
        assertThat(sut.getAvailableThemes()).containsExactly(TEST_THEMES[2])
        assertThat(sut.size).isEqualTo(2)
        assertThat(sut.getElementAt(0)).isEqualTo(TEST_THEMES[0])
        assertThat(sut.getElementAt(1)).isEqualTo(TEST_THEMES[1])
        assertThat(events).containsExactly(
            EventData(ListDataEvent.INTERVAL_ADDED, 0, 0),
            EventData(ListDataEvent.INTERVAL_REMOVED, 0, 0),
            EventData(ListDataEvent.INTERVAL_ADDED, 0, 1),
        )
    }

    @Test
    fun `set themes to bigger non empty list not in order`() {
        sut.addTheme(TEST_THEMES[2])
        sut.addTheme(TEST_THEMES[1])
        sut.setThemes(listOf(TEST_THEMES[0]))
        assertThat(sut.getAvailableThemes()).containsExactly(TEST_THEMES[1], TEST_THEMES[2])
        assertThat(sut.size).isEqualTo(1)
        assertThat(sut.getElementAt(0)).isEqualTo(TEST_THEMES[0])
        assertThat(events).containsExactly(
            EventData(ListDataEvent.INTERVAL_ADDED, 0, 0),
            EventData(ListDataEvent.INTERVAL_ADDED, 0, 0),
            EventData(ListDataEvent.INTERVAL_REMOVED, 0, 1),
            EventData(ListDataEvent.INTERVAL_ADDED, 0, 0),
        )
    }

    @Test
    fun `remove themes - the only theme`() {
        sut.addTheme(TEST_THEMES[0])
        sut.removeTheme(TEST_THEMES[0])
        assertThat(sut.size).isZero()
        assertThat(events).containsExactly(
            EventData(ListDataEvent.INTERVAL_ADDED, 0, 0),
            EventData(ListDataEvent.INTERVAL_REMOVED, 0, 0),
        )
    }

    @Test
    fun `remove themes - the last theme`() {
        sut.addTheme(TEST_THEMES[0])
        sut.addTheme(TEST_THEMES[1])
        sut.removeTheme(TEST_THEMES[1])
        assertThat(sut.size).isEqualTo(1)
        assertThat(sut.getElementAt(0)).isEqualTo(TEST_THEMES[0])
        assertThat(events).containsExactly(
            EventData(ListDataEvent.INTERVAL_ADDED, 0, 0),
            EventData(ListDataEvent.INTERVAL_ADDED, 1, 1),
            EventData(ListDataEvent.INTERVAL_REMOVED, 1, 1),
        )
    }

    @Test
    fun `remove themes - the first theme`() {
        sut.addTheme(TEST_THEMES[0])
        sut.addTheme(TEST_THEMES[1])
        sut.removeTheme(TEST_THEMES[0])
        assertThat(sut.size).isEqualTo(1)
        assertThat(sut.getElementAt(0)).isEqualTo(TEST_THEMES[1])
        assertThat(events).containsExactly(
            EventData(ListDataEvent.INTERVAL_ADDED, 0, 0),
            EventData(ListDataEvent.INTERVAL_ADDED, 1, 1),
            EventData(ListDataEvent.INTERVAL_REMOVED, 0, 0),
        )
    }
}

private val TEST_THEMES = listOf(
    ThemeConfig("Theme 1", "id1"),
    ThemeConfig("Theme 2", "id2"),
    ThemeConfig("Theme 1.1", "id1.1"),
)

private data class EventData(
    val id: Int,
    val index0: Int,
    val index1: Int,
)
