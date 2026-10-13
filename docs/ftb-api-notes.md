# FTB API Notes

Signatures recorded here have been verified against the actual dependency jars on the classpath.
Do not add an entry from memory or from online docs alone.

Entry format:

```
### <Class>#<method>
- Signature: `<return type> <method>(<params>)`
- Artifact/version: <maven coordinate>
- Verified: <how / where>
- Notes: <side, threading, nullability>
```

## FTB Library

### ChunkDimPos#<init>
- Signature: `ChunkDimPos(ResourceKey<Level> dimension, int x, int z)`, `ChunkDimPos(ResourceKey<Level> dimension, ChunkPos pos)`, `ChunkDimPos(Level level, BlockPos pos)`, `ChunkDimPos(Entity entity)`
- Class: `dev.ftb.mods.ftblibrary.math.ChunkDimPos`
- Artifact/version: `curse.maven:ftb-library-forge-404465:8226927` (ftblibrary 2001.2.13)
- Verified: `javap` against the ModDevGradle-transformed jar in `~/.gradle/caches/8.14.5/transforms/*/transformed/`
- Notes: position type taken by every FTB Chunks claim/unclaim call. Accessors: `ChunkPos getChunkPos()`, `int x()`, `int z()`, `ResourceKey<Level> dimension()`, `ChunkDimPos offset(int, int)`.

## FTB Teams

Artifact/version for every entry below: `curse.maven:ftb-teams-forge-404468:7499810` (ftbteams 2001.3.2).

### FTBTeamsAPI#api
- Signature: `static FTBTeamsAPI.API api()`
- Class: `dev.ftb.mods.ftbteams.api.FTBTeamsAPI`
- Verified: `javap` against the ModDevGradle-transformed jar in `~/.gradle/caches/8.14.5/transforms/*/transformed/`
- Notes: entry point. Static `MOD_ID` / `MOD_NAME` string constants also exist.

### FTBTeamsAPI.API#isManagerLoaded / #getManager
- Signature: `boolean isManagerLoaded()`, `TeamManager getManager()`
- Class: `dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API`
- Verified: `javap` against the ModDevGradle-transformed jar in `~/.gradle/caches/8.14.5/transforms/*/transformed/`
- Notes: server-side manager. Check `isManagerLoaded()` before `getManager()`. Client-side counterparts are `isClientManagerLoaded()` / `ClientTeamManager getClientManager()` (client package only).

### TeamManager#getTeamForPlayer
- Signature: `Optional<Team> getTeamForPlayer(ServerPlayer player)`
- Class: `dev.ftb.mods.ftbteams.api.TeamManager`
- Verified: `javap` against the ModDevGradle-transformed jar in `~/.gradle/caches/8.14.5/transforms/*/transformed/`
- Notes: returns `Optional`, not a nullable `Team`. Full lookup path: `FTBTeamsAPI.api().getManager().getTeamForPlayer(player)`.

### TeamManager#getTeamForPlayerID / #getPlayerTeamForPlayerID / #getTeamByID
- Signature: `Optional<Team> getTeamForPlayerID(UUID playerId)`, `Optional<Team> getPlayerTeamForPlayerID(UUID playerId)`, `Optional<Team> getTeamByID(UUID teamId)`
- Class: `dev.ftb.mods.ftbteams.api.TeamManager`
- Verified: `javap` against the ModDevGradle-transformed jar in `~/.gradle/caches/8.14.5/transforms/*/transformed/`
- Notes: UUID-based lookups, usable for offline players. `getPlayerTeamForPlayerID` returns the player's personal (player-type) team, `getTeamForPlayerID` the team they are currently in. Also present: `boolean arePlayersInSameTeam(UUID, UUID)`, `Collection<Team> getTeams()`.

### Team (selected methods)
- Signature: `UUID getId()`, `UUID getTeamId()`, `UUID getOwner()`, `Set<UUID> getMembers()`, `Collection<ServerPlayer> getOnlineMembers()`, `boolean isPlayerTeam()`, `boolean isPartyTeam()`, `boolean isServerTeam()`, `boolean isValid()`, `Component getName()`, `CompoundTag getExtraData()`, `void markDirty()`
- Class: `dev.ftb.mods.ftbteams.api.Team`
- Verified: `javap` against the ModDevGradle-transformed jar in `~/.gradle/caches/8.14.5/transforms/*/transformed/`
- Notes: `getExtraData()` + `markDirty()` is the API-level way to persist custom per-team NBT. `isPlayerTeam` / `isPartyTeam` / `isServerTeam` are default methods.

### Team#getRankForPlayer
- Signature: `TeamRank getRankForPlayer(UUID playerId)`
- Class: `dev.ftb.mods.ftbteams.api.Team`
- Verified: `javap` against the ModDevGradle-transformed jar in `~/.gradle/caches/8.14.5/transforms/*/transformed/`
- Notes: also present: `Map<UUID, TeamRank> getPlayersByRank(TeamRank)`.

### TeamRank
- Signature: enum constants `ENEMY`, `NONE`, `ALLY`, `INVITED`, `MEMBER`, `OFFICER`, `OWNER`; `boolean isAtLeast(TeamRank)`, `boolean isMemberOrBetter()`, `boolean isOfficerOrBetter()`, `boolean isOwner()`, `int getPower()`
- Class: `dev.ftb.mods.ftbteams.api.TeamRank`
- Verified: `javap -c` against the ModDevGradle-transformed jar in `~/.gradle/caches/8.14.5/transforms/*/transformed/`
- Notes: `isXOrBetter()` / `isOwner()` all delegate to `isAtLeast(X)`, which compares `power` and returns false when the argument's power is negative.

## FTB Chunks

Artifact/version for every entry below: `curse.maven:ftb-chunks-forge-314906:8216874` (ftbchunks 2001.3.8).

### FTBChunksAPI#api
- Signature: `static FTBChunksAPI.API api()`
- Class: `dev.ftb.mods.ftbchunks.api.FTBChunksAPI`
- Verified: `javap` against the ModDevGradle-transformed jar in `~/.gradle/caches/8.14.5/transforms/*/transformed/`
- Notes: entry point. `clientApi()` also exists but returns a client type; do not call it from common code.

### FTBChunksAPI.API#isManagerLoaded / #getManager
- Signature: `boolean isManagerLoaded()`, `ClaimedChunkManager getManager()`
- Class: `dev.ftb.mods.ftbchunks.api.FTBChunksAPI$API`
- Verified: `javap` against the ModDevGradle-transformed jar in `~/.gradle/caches/8.14.5/transforms/*/transformed/`
- Notes: check `isManagerLoaded()` first.

### FTBChunksAPI.API#claimAsPlayer
- Signature: `ClaimResult claimAsPlayer(ServerPlayer player, ResourceKey<Level> dimension, ChunkPos pos, boolean checkOnly)`
- Class: `dev.ftb.mods.ftbchunks.api.FTBChunksAPI$API`
- Verified: `javap` against the ModDevGradle-transformed jar in `~/.gradle/caches/8.14.5/transforms/*/transformed/`
- Notes: convenience claim for a player's own team. Last parameter name is not in the bytecode; meaning inferred from the matching `ChunkTeamData#claim` flow (see below).

### ClaimedChunkManager#getOrCreateData / #getPersonalData / #getChunk
- Signature: `ChunkTeamData getOrCreateData(Team team)`, `ChunkTeamData getOrCreateData(ServerPlayer player)`, `ChunkTeamData getPersonalData(UUID playerId)`, `ClaimedChunk getChunk(ChunkDimPos pos)`
- Class: `dev.ftb.mods.ftbchunks.api.ClaimedChunkManager`
- Verified: `javap` against the ModDevGradle-transformed jar in `~/.gradle/caches/8.14.5/transforms/*/transformed/`
- Notes: `getChunk` returns null when the chunk is unclaimed (impl checks `getChunk(...) == null` for `NOT_CLAIMED`). Also present: `Collection<? extends ClaimedChunk> getAllClaimedChunks()`, `Map<UUID, Collection<ClaimedChunk>> getClaimedChunksByTeam(Predicate<ClaimedChunk>)`.

### ChunkTeamData#getExtraClaimChunks / #setExtraClaimChunks
- Signature: `int getExtraClaimChunks()`, `void setExtraClaimChunks(int extra)`
- Class: `dev.ftb.mods.ftbchunks.api.ChunkTeamData`
- Verified: `javap` against the ModDevGradle-transformed jar in `~/.gradle/caches/8.14.5/transforms/*/transformed/` (impl `dev.ftb.mods.ftbchunks.data.ChunkTeamDataImpl` disassembled with `javap -c`)
- Notes:
  - `setExtraClaimChunks` only assigns the field. It does not call `markDirty()` or recompute `getMaxClaimChunks()`.
  - `getExtraClaimChunks` on a party team with a non-zero value moves that value to the owner's personal data, zeroes it on the party, and marks both dirty (logged at info by FTB).
  - FTB's own `/ftbchunks extra_claim_chunks` command writes to `getPersonalData(player.getUUID())`, then calls `ChunkTeamDataImpl#markDirty()`, `getOrCreateData(player).updateLimits()`, and `SendGeneralDataPacket.send(...)`. None of those three are on the `api` interfaces. See open questions.
  - Related: `int getMaxClaimChunks()` (cached limit, read-only), `int getExtraForceLoadChunks()` / `void setExtraForceLoadChunks(int)`, `int getMaxForceLoadChunks()`.

### ChunkTeamData#claim / #unclaim
- Signature: `ClaimResult claim(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly)`, `ClaimResult unclaim(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly)`
- Class: `dev.ftb.mods.ftbchunks.api.ChunkTeamData`
- Verified: `javap` against the ModDevGradle-transformed jar in `~/.gradle/caches/8.14.5/transforms/*/transformed/` (impl disassembled with `javap -c`)
- Notes:
  - `claim` fails with `ALREADY_CLAIMED`, `DIMENSION_FORBIDDEN`, or `NOT_ENOUGH_POWER` (non-server team with `getClaimedChunks().size() >= getMaxClaimChunks()`), then fires `ClaimedChunkEvent.BEFORE_CLAIM`, registers the claim, fires `AFTER_CLAIM`, and calls `markDirty()`.
  - `unclaim` fails with `NOT_CLAIMED` or `NOT_OWNER` (different team and source lacks permission level 2), then fires `BEFORE_UNCLAIM` and unclaims.
  - Third parameter name is not in the bytecode. In `claim`, when it is `true` the method returns the would-be result right after `BEFORE_CLAIM` without registering the claim, i.e. a check-only flag. On success `claim` returns the new `ClaimedChunk` (which implements `ClaimResult`).
  - Also present: `forceLoad(...)` / `unForceLoad(...)` with the same parameter list.

### ClaimedChunk (selected methods)
- Signature: `ChunkTeamData getTeamData()`, `ChunkDimPos getPos()`, `long getTimeClaimed()`, `void unclaim(CommandSourceStack source, boolean sync)`
- Class: `dev.ftb.mods.ftbchunks.api.ClaimedChunk` (extends `ClaimResult`)
- Verified: `javap` against the ModDevGradle-transformed jar in `~/.gradle/caches/8.14.5/transforms/*/transformed/`
- Notes: `ChunkTeamData#unclaim` calls `ClaimedChunkImpl.unclaim(source, true)`; second parameter name not in the bytecode.

### ClaimResult
- Signature: `String getResultId()`, `default boolean isSuccess()`, `MutableComponent getMessage()`, `static ClaimResult success()`, `static ClaimResult customProblem(String id)`
- Class: `dev.ftb.mods.ftbchunks.api.ClaimResult`
- Verified: `javap` against the ModDevGradle-transformed jar in `~/.gradle/caches/8.14.5/transforms/*/transformed/`
- Notes: built-in failures are enum `ClaimResult.StandardProblem`: `NOT_OWNER`, `NOT_ENOUGH_POWER`, `ALREADY_CLAIMED`, `DIMENSION_FORBIDDEN`, `NOT_CLAIMED`, `ALREADY_LOADED`, `NOT_LOADED`.

### ClaimedChunkEvent
- Signature: `static final Event<ClaimedChunkEvent.Before> BEFORE_CLAIM, BEFORE_LOAD, BEFORE_UNCLAIM, BEFORE_UNLOAD`; `static final Event<ClaimedChunkEvent.After> AFTER_CLAIM, AFTER_LOAD, AFTER_UNCLAIM, AFTER_UNLOAD`
- Callbacks: `CompoundEventResult<ClaimResult> Before#before(CommandSourceStack source, ClaimedChunk chunk)`, `void After#after(CommandSourceStack source, ClaimedChunk chunk)`
- Class: `dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent`
- Verified: `javap` against the ModDevGradle-transformed jar in `~/.gradle/caches/8.14.5/transforms/*/transformed/`
- Notes: `Event` / `CompoundEventResult` are Architectury types (`dev.architectury.event`). Their registration signatures are not recorded here yet; verify against the Architectury jar before use. In `claim`, a non-null result object from `BEFORE_CLAIM` that is not `isSuccess()` aborts the claim and is returned.

## Open questions

- Persisting a changed extra-claim value: `ChunkTeamData` exposes no `markDirty()` or `updateLimits()`. FTB's command uses impl-only methods (`ChunkTeamDataImpl#markDirty`, `#updateLimits`, `SendGeneralDataPacket.send`). Phase 1 needs a decision on whether to depend on the `data` impl package or find another route.
