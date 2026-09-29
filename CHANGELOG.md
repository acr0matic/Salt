### Unreleased
- Water evaporation and salt crystal growing are now data-driven:
  - `salt:evaporation` recipes define which cauldron evaporates over a heater, what it turns into and what it drops (per-level drop lists with chances).
  - `salt:crystal_growing` recipes define the dripping fluid, the base blocks and the ordered growth stages of a crystal chain.
  - Default behavior is shipped as regular recipes in `data/salt/recipe/` and can be overridden or removed by datapacks.
  - JEI categories now display real recipes instead of static dummies.
- `SaltClusterGrowingChance` config now actually applies to each crystal growth event (previously it was not wired).
- Removed `salt:cauldron_evaporation/*` gameplay loot tables — salt cauldron drops are controlled by evaporation recipes now.

### 2024-07-05 - 1.2.6
- Fixed Salt Block not dissolving in water and not melting ice.
- Fixed #salt:salt_cluster_growables not working for blocks that aren't salt.

### 2024-06-20 - 1.2.5
- Fixed salting recipe not reloading with `/reload` command. 
- Added french localization.

### 2023-12-28 - 1.2.4
- Tag changes from 1.2.3 are actually applied now.

### 2023-12-23 - 1.2.3
- Added Salt item to `forge:salt`, `forge:dusts` and `forge:dusts/salt` tags. 
- Fixed Salt Cluster dropping two clusters instead of Raw Rock Salt when broken with empty hand. 

### 2023-07-18 - 1.2.2
- Fixed Salt Cauldron not dropping items.

### 2023-07-10 - 1.2.1
- Added Simplified Chinese localization. (junshengxie)

### 2023-07-08 - 1.2.0
- Added many new foods to can_be_salted tag. Thanks Zap.
- Reduced the size of Rock Salt deposit but increased the chance of them generating. 
  - You may need to regenerate the config file for the reduced size to take effect. 

### 2023-05-31 - 1.1.0
- Changed how saturation modifier is applied: it will now modify total food nutrition (original + salted) instead 
of modifying only the additional salted nutrition 
- Added AppleSkin support for salted foods
- Advancements will be shown in chat when completed.

### 2023-04-21 - 1.0.4
- Adjusted salt item entities positions when extracted from Salt Cauldron.
- Fixed salt not forming in a heated Water Cauldron.
- Fixed salted food models not working as intended.

---