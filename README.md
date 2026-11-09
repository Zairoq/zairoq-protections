# Zairoq Protections

Server-side Fabric land-claim mod for Minecraft 26.3.

## Crafting Protection Stones

Craft one of the following combinations in the crafting table:

| Ingredients | Result | Claim footprint |
| --- | --- | ---: |
| Lodestone + Coal Block | Coal Protection Stone | 10 x 10 |
| Lodestone + Copper Block (unoxidized) | Copper Protection Stone | 20 x 20 |
| Lodestone + Diamond Block | Diamond Protection Stone | 50 x 50 |
| Lodestone + Netherite Block | Netherite Protection Stone | 100 x 100 |

Place the crafted Protection Stone to create a claim centered on that block, protecting the entire build height of that dimension. Claims cannot overlap in the same dimension. Each tier is identified by its registered item ID, not by its display name or lore.

The Protection Stone is the claim core. Only its owner can break it; doing so removes the claim and drops the same Protection Stone item. Existing claims created by the former mineral-block + Lodestone method remain readable and retain their original core coordinates.

## Commands

- `/protection help`
- `/protection info` — information about the claim at your position.
- `/protection list` — claims you own, co-own, or are trusted in.
- `/protection trust <player>` — allow a player to interact with protected blocks and entities.
- `/protection untrust <player>`
- `/protection coowner add <player>` — grant build and interaction permissions.
- `/protection coowner remove <player>`
- `/protection abandon` — remove your claim and leave the core block in place.
- `/protection admin remove <id>` — operator command to remove claim data without changing blocks.

Owners and co-owners can build. Trusted players can interact with blocks and entities but cannot place or break blocks. Only owners can appoint or remove co-owners, abandon claims, or dismantle the core.

## Persistence

Claims are saved in `<world>/data/zairoq_protections.json` and are flushed on changes and server saves. Keep this file with the world backup. The new core uses the existing claim data format; no migration is required.

## Installation

Install the mod on the dedicated server. It does not add client-side content or require a client installation.

## Build

Requires Java 25 and Gradle 9.8.2:

```sh
gradle build
```
