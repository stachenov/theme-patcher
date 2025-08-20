# Theme Patcher Plugin for IntelliJ IDEs

## How to Use

Because it's not ready yet, there's no distribution. You can only use it if you compile it manually.

The plugin adds a settings page that consists of three components:

- the list of rulesets;
- the list of themes that the selected ruleset is applied to;
- the list of rules in the selected ruleset.

Add a ruleset to the first list, select it, add the themes you need to patch to the second list and then add rules to the third list.
The rules in a ruleset will only be applied when the currently active theme is one of the listed in the second list.
If there are rules in several rulesets that change the same value, the last one wins.

Each rule specifies a key and a value that the key will be assigned.
The list of keys is taken from the currently active theme,
so at the moment it's not possible to customize a key that the current theme doesn't define explicitly.
It _is_ possible to customize fallback keys, though (the ones that start with a `*.`).
For example, if you need to customize the tool window background, there's the `ToolWindow.background` key for that.
But if it's not in the current theme, then `*.background` will be used instead, and you can customize it.

Sometimes there's a key that a theme doesn't define, and there's not even a fallback, just a hardcoded default.
The current version of the plugin doesn't "officially" support modifying such keys, but it's still possible in two ways.

First, because the set of keys is taken from the current theme, you can try switching to another theme
and see if the key is there. There's nothing that prevents having one theme active but setting up a ruleset
to patch some other theme.

Second, and it's actually a bug in the UI, but the one that turned into a useful feature,
you can add _some_ key of the needed type to the table,
and then just... rename it by selecting the table cell containing the key and double-clicking it or pressing F2.
The table was supposed to be only modified through the edit dialog,
but it turns out that it's possible to just rename a key (but not to edit its value) like that.
Try, for example, patching `MainToolbar.Icon.insets.compact` and renaming it to `MainToolbar.Icon.insets`.
Be careful, though, to make sure the value type of the key you _intend_ to modify is the same
as the type of the key you initially added. So don't rename `MainToolbar.Icon.insets` to `Tree.rowHeight`.

It is possible that a future version will allow customizing any keys without these tricks,
but the benefit of restricting to the present keys is that the type of the key can be detected
from its actual value. Customizing random keys will need a UI to select the value type,
and worse, one would have to guess the type correctly for it to work. So it wouldn't be a good UX either way,
which is why it isn't implemented.

## Where to get keys

For colors the easiest way is to switch the IDE to the internal mode by putting `idea.is.internal=true`
into `Help | Edit Custom Properties...`
The internal mode will activate a lot of internal undocumented features, including the UI Inspector
that can be invoked by Ctrl+Alt+clicking almost any component.
There you can see its properties, including various colors, borders, etc.
In the latest versions there's even a color picker tool.

For other values it's harder, and the only definitive way to get them all is the source code
of the IJ Platform and even Swing.
In particular, the [`JBUI.CurrentTheme`](https://github.com/JetBrains/intellij-community/blob/master/platform/util/ui/src/com/intellij/util/ui/JBUI.java) class contains a lot of functions that just retrieve some value
from the theme by some key.
So if you see, say, `JBUI.CurrentTheme.ToolWindow.borderColor` getting `"ToolWindow.Stripe.borderColor"`,
that's the key you're looking for to change the color of the border between the tool window and the tool window stripe.

Another way is to browse the files [`IntelliJPlatform.themeMetadata.json`](https://github.com/JetBrains/intellij-community/blob/master/platform/platform-resources/src/themes/metadata/IntelliJPlatform.themeMetadata.json)
and [`JDK.themeMetadata.json`](https://github.com/JetBrains/intellij-community/blob/master/platform/platform-resources/src/themes/metadata/JDK.themeMetadata.json).
They contain a lot of keys and their descriptions.

For the Compact Mode, there are keys ending with ".compact".
They will be used instead of their regular counterparts when the Compact Mode is on.

Try customizing `Tree.rowHeight` and `*.background` to get started.

## Limitations

The following types are supported at the moments:
- integers;
- colors;
- sizes (width x height);
- insets (top,left,bottom,right);
- font sizes.

It's not possible to specify whether an integer value is scaled or not.
Instead, there's an auto-detection that should work most of the time.

## Licensing

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
