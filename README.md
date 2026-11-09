# Zairoq Protections

Server-side Fabric land-claim mod for Minecraft 26.3.

## Claim creation

Place one of the following mineral blocks, then place a vanilla Lodestone directly on top:

| Base block | Claim footprint |
| --- | ---: |
| Coal Block | 10 x 10 |
| Copper Block | 20 x 20 |
| Diamond Block | 50 x 50 |
| Netherite Block | 100 x 100 |

The footprint is centered on the Lodestone column and protects the entire build height of that dimension. Claims cannot overlap in the same dimension.

The Lodestone and mineral base form the claim core. Only the owner can remove the core by breaking either core block. Breaking the base also removes the Lodestone. Abandoning a claim through the command removes protection data and leaves the blocks in place.

## Commands

- `/protection help`
- `/protection info` — information about the claim at your position.
- `/protection list` — claims you own, co-own, or are trusted in.
- `/protection trust <player>` — allow a player to interact with protected blocks and entities.
- `/protection untrust <player>`
- `/protection coowner add <player>` — grant build and interaction permissions.
- `/protection coowner remove <player>`
- `/protection abandon` — remove your claim and leave the core blocks in place.
- `/protection admin remove <id>` — operator command to remove claim data without changing blocks.

Owners and co-owners can build. Trusted players can interact with blocks and entities but cannot place or break blocks. Only owners can appoint or remove co-owners, abandon claims, or dismantle the core.

## Persistence

Claims are saved in `<world>/data/zairoq_protections.json` and are flushed on changes and server saves. Keep this file with the world backup.

## Installation

Install the mod on the dedicated server. It does not add client-side content or require a client installation.

## Build

Requires Java 25 and Gradle 9.8.2:

```sh
gradle build
```
