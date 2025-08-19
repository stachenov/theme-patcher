# Theme Patcher Plugin for IntelliJ IDEs

**Warning!** This is still a work in progress. It's full of bugs, not yet implemented features,
and it only supports a couple of types at the moment. The UI is barely there too. But it already sort of works.

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
It may change colors of other components as well, and currently the only way to avoid it is to select
a theme that explicitly defines the key you need. Then it'll be in the list, even if you're editing rules for some other theme.

## Where to get keys

For colors the easiest way is to switch the IDE to the internal mode by putting `idea.is.internal=true`
into `Help | Edit Custom Properties...`
The internal mode will activate a lot of internal undocumented features, including the UI Inspector
that can be invoked by Ctrl+Alt+clicking almost any component.
There you can see its properties, including various colors, borders, etc.
In the latest versions there's even a color picker tool.

For other values it's harder, and the only definitive way to get them all is the source code
of the IJ Platform and even Swing.
In particular, the `JBUI.CurrentTheme` class contains a lot of functions that just retrieve some value
from the theme by some key.
So if you see, say, `JBUI.CurrentTheme.ToolWindow.borderColor` getting `"ToolWindow.Stripe.borderColor"`,
that's the key you're looking for to change the color of the border between the tool window and the tool window stripe.

Another way is to browse the files `IntelliJPlatform.themeMetadata.json` and `JDK.themeMetadata.json`.
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
