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

### FTB Library UI (selected classes)
- Signatures:
  - `BaseScreen`: `BaseScreen()`, `final void openGui()`, `final void refreshWidgets()`, `boolean onInit()`, `final void openYesNo(Component title, Component desc, Runnable onYes)`; default size 176x166 set in the constructor
  - `Panel`: `Panel(Panel parent)`, `abstract void addWidgets()`, `abstract void alignWidgets()`, `void add(Widget)`, `List<Widget> getWidgets()`, `final int align(WidgetLayout)`
  - `Widget`: `final void setPos(int, int)`, `final void setSize(int, int)`, `final Widget setPosAndSize(int, int, int, int)`, `BaseScreen getGui()`, `void addMouseOverText(TooltipList)`, `void playClickSound()`, public fields `posX`, `posY`, `width`, `height`
  - `Button`: `abstract void onClicked(MouseButton)`; `SimpleTextButton(Panel parent, Component title, Icon icon)` (abstract)
  - `TextField(Panel parent)`, `TextField setText(Component)` (wraps and resizes via `getGui().getTheme()`)
  - `PanelScrollBar(Panel parent, Panel panel)`
  - `ScreenWrapper` (extends vanilla `Screen`): `BaseScreen getGui()`; `BaseScreen#openGui` sets `new ScreenWrapper(this)` as the current screen
  - `WidgetLayout.VERTICAL`, `Icon.empty()` (returns `Color4I`), `MouseButton#isLeft()`, `TooltipList#add(Component)`
- Packages: `dev.ftb.mods.ftblibrary.ui`, `.ui.input`, `.icon`, `.util`
- Artifact/version: `curse.maven:ftb-library-forge-404465:8226927` (ftblibrary 2001.2.13)
- Verified: `javap` / `javap -c` against the ModDevGradle-transformed jar in `~/.gradle/caches/8.14.5/transforms/*/transformed/`
- Notes: client only. Used from `integration.ftb.client` only.

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
- Notes: `Event` / `CompoundEventResult` are Architectury types (`dev.architectury.event`), see the Architectury section below. These are not Forge bus events; register a callback on the static field. In `claim`, `BEFORE_CLAIM`'s `CompoundEventResult#object()` is read: a non-null result that is not `isSuccess()` aborts the claim and is returned (also when check-only). The source is the player's `createCommandSourceStack()` for map-screen claims (`dev.ftb.mods.ftbchunks.net.RequestChunkChangePacket`).

### ChunkTeamDataImpl#markDirty / #updateLimits (internal)
- Signature: `void markDirty()`, `void updateLimits()`
- Class: `dev.ftb.mods.ftbchunks.data.ChunkTeamDataImpl` (implements `ChunkTeamData`; not an `api` type)
- Verified: `javap -c` against the ModDevGradle-transformed jar in `~/.gradle/caches/8.14.5/transforms/*/transformed/`
- Notes:
  - Internal class, used only because the `api` interfaces have no way to persist an extra-claim change. Pinned to ftbchunks 2001.3.8; re-verify on any FTB Chunks version bump.
  - `ClaimedChunkManager#getPersonalData` / `#getOrCreateData` return `ChunkTeamDataImpl` at runtime (`ClaimedChunkManagerImpl` declares the covariant return type). `getPersonalData` returns null when the player has no personal team.
  - `updateLimits` recomputes `maxClaimChunks` / `maxForceLoadChunks`. For a party team it aggregates member personal data according to `FTBChunksWorldConfig.PARTY_LIMIT_MODE` (`LARGEST`, `OWNER`, `SUM`, `AVERAGE`); the owner's personal data counts in every mode.

### SendGeneralDataPacket#send (internal)
- Signature: `static void send(ChunkTeamData data, ServerPlayer player)`, `static void send(ChunkTeamData data, Collection<ServerPlayer> players)`
- Class: `dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket` (not an `api` type)
- Verified: `javap` against the ModDevGradle-transformed jar in `~/.gradle/caches/8.14.5/transforms/*/transformed/`
- Notes: pushes claim/force-load limits to clients. FTB's `/ftbchunks extra_claim_chunks` flow (from `javap -c` of `FTBChunksCommands#setExtraClaimChunks`): `getPersonalData(uuid).setExtraClaimChunks(n)`, `.markDirty()`, `getOrCreateData(player).updateLimits()`, `SendGeneralDataPacket.send(thatData, player)`.

## Architectury

Artifact/version for every entry below: `curse.maven:architectury-api-419699:5137938` (architectury 9.2.14).

### Event#register / #unregister
- Signature: `void register(T listener)`, `void unregister(T listener)`, `boolean isRegistered(T listener)`, `T invoker()`
- Class: `dev.architectury.event.Event`
- Verified: `javap` against the ModDevGradle-transformed jar in `~/.gradle/caches/8.14.5/transforms/*/transformed/`
- Notes: used to subscribe to `ClaimedChunkEvent` fields.

### CompoundEventResult (selected methods)
- Signature: `static <T> CompoundEventResult<T> pass()`, `static <T> CompoundEventResult<T> interruptFalse(T)`, `static <T> CompoundEventResult<T> interruptTrue(T)`, `static <T> CompoundEventResult<T> interruptDefault(T)`, `T object()`
- Class: `dev.architectury.event.CompoundEventResult`
- Verified: `javap` against the ModDevGradle-transformed jar in `~/.gradle/caches/8.14.5/transforms/*/transformed/`
- Notes: `ClaimResult.customProblem(id)` uses `id` as a translation key for its message (`Component.translatable`).

### NetworkManager (networking)
- Signature: `static void registerReceiver(NetworkManager.Side side, ResourceLocation id, NetworkManager.NetworkReceiver receiver)`, `static void sendToPlayer(ServerPlayer, ResourceLocation, FriendlyByteBuf)`, `static void sendToPlayers(Iterable<ServerPlayer>, ResourceLocation, FriendlyByteBuf)`, `static void sendToServer(ResourceLocation, FriendlyByteBuf)`
- Callbacks: `void NetworkReceiver#receive(FriendlyByteBuf buf, NetworkManager.PacketContext context)`; `PacketContext`: `Player getPlayer()`, `void queue(Runnable)`, `Env getEnvironment()`
- Class: `dev.architectury.networking.NetworkManager` (`Side` enum: `S2C`, `C2S`)
- Verified: `javap` / `javap -c` against the ModDevGradle-transformed jar in `~/.gradle/caches/8.14.5/transforms/*/transformed/`
- Notes: on Forge (`dev.architectury.networking.forge.NetworkManagerImpl`) all ids share one Architectury event channel; registering an S2C receiver only stores it in a map. Read the buffer inside `receive`, then `queue` game-state work onto the main thread.

## Open questions

- Resolved (T18): persisting a changed extra-claim value uses the internal `ChunkTeamDataImpl#markDirty` / `#updateLimits` and `SendGeneralDataPacket.send`, mirroring FTB's own command. Acceptable because FTB Chunks is pinned to 2001.3.8.
