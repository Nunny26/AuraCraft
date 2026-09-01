# AuraCraft
Passively pulls ingredients from nearby Chests and Barrels to fulfill your crafting recipes.
Pulls items directly from your Ender Chest, Shulker Boxes in your inventory, and even Shulker Boxes *inside* your Ender Chest!
Passively synchronizes nearby items to your client. Recipes in the Vanilla Recipe Book will automatically light up and lose their "Red Squares" if the items are available in your radius.
Fully supports Shift-Click "Craft All" mechanics. It mathematically calculates exact sets of missing ingredients and pulls them transactionally. 
## Configuration (Mod Menu)
AuraCraft is fully configurable in-game using **Mod Menu** and **Cloth Config**. 
Set how far the mod searches for chests (Configurable up to 100 blocks, Default: 8).
Toggle Ender Chest pulling.
Toggle inventory Shulker Box pulling.

Dependencies
Minecraft 26.2 | Java 25 | Fabric Loader >= 0.19.3
This mod requires the following dependencies to be installed in your `mods` folder:
* [Fabric API](https://modrinth.com/mod/fabric-api)
* [Cloth Config API](https://modrinth.com/mod/cloth-config)
## Branches
* `main`: The stable release featuring the core auto-pull mechanics.
* `beta`: An experimental branch developing an interactable 3rd-window UI to visually manage the connected radius storage. 
