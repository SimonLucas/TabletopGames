# Macro-piece architecture

## Implemented foundation

Atomic `TileDefinition` and generated `MacroPieceDefinition` implement the same `PieceDefinition` contract. Both expose:

- a stable definition ID;
- a variable-size dense cell grid, with `VOID` supporting irregular footprints;
- zero-depth ports with direction, width and local coordinates;
- quarter-turn rotation;
- a flattened physical-component inventory.

`MacroPieceComposer` accepts positioned, rotated child definitions and explicit internal port connections. Compilation:

1. validates unique child IDs and connected composition;
2. validates opposing, equal-width, zero-depth port alignment;
3. prevents multiple use of a port;
4. merges non-connector terrain while rejecting overlap;
5. consumes internal ports and exposes the remainder in normalized macro-local coordinates;
6. sums nested physical inventory;
7. calculates intrinsic metrics;
8. creates a translation- and rotation-independent canonical signature.

The compiled public geometry is flat even when immutable provenance contains nested macros. `CompositePieceCatalog` overlays generated definitions on the atomic `TileCatalog`, so the existing `BoardGenome`, `BoardLayoutEngine` and `PortGeometry` can place and connect a macro without knowing that it is composite.

## Inventory

`PieceInventory` is an immutable multiset keyed by physical component ID. A/B definitions count as alternate faces of the same component. `InventoryAssessment` reports total excess and exceeded types against an available inventory.

Three policies are available:

- `IGNORE`: permit unlimited reuse;
- `HARD_LIMIT`: reject excess inventory;
- `SOFT_PENALTY`: retain the candidate and scale quality by `1 / (1 + totalExcess)`.

Geometry and inventory policy remain independent.

## Quality diversity

The generic `qd` package provides N-dimensional `BehaviorCell`, axis metadata, a typed descriptor and a bounded one-elite-per-cell `QualityArchive`. It does not depend on spatial chromosomes or the existing two-dimensional board heatmap.

The initial scale/interface descriptor indexes atomic-piece count and exposed-port count. Complete hierarchical runs use `MacroShapeDescriptor` instead, because scale/interface alone strongly favours repeated straight modules and cannot distinguish them from bent compositions. Its axes are:

- **axial balance**: minor bounding extent divided by major extent, separating elongated modules from shapes occupying both axes;
- **compactness**: occupied terrain divided by bounding-box area, separating sparse/L-shaped modules from filled room-like modules.

Atomic count, exposed ports and directional port diversity remain intrinsic metrics and influence macro quality. They do not consume the two visible shape-diversity axes.

`PieceMetrics` also stores traversable size, extent, compactness, directional port counts, hierarchical branch/cycle counts, terrain composition and inventory. Further descriptors can index any combination without recompiling a macro.

`MacroEvolutionEngine` is a deterministic bounded demonstration. It selects atomic or archived definitions, uses `MacroGraftOperator` to enumerate valid one-port attachments, filters by maximum atomic size and inventory policy, and offers one seeded choice to the macro archive. `MacroPieceLibrary` retains only archive elites, so storage is bounded by the descriptor grid.

### Catalogue maintenance and feedback

`MacroPieceLibrary` is also the maintained catalogue used by hierarchical board evolution. Each active archive elite has immutable `MacroUsageStatistics` recording:

- graft attempts and successful grafts;
- admissions to the board archive;
- feasible board admissions;
- cumulative and mean admitted-board fitness.

The catalogue owns a mutable signature-to-statistics map and replaces an immutable statistics value after each event. Published values and snapshots therefore never change behind a reader's back. When a macro loses its QD niche, its statistics are removed; catalogue definitions and feedback storage are both bounded by the number of macro niches.

Usage is deliberately diagnostic in the first engine. It does not yet change parent probability, allowing intrinsic macro quality and downstream utility to be compared before feedback becomes selection pressure. A later curiosity/utility emitter can sample by graft success, feasible admissions or niche discovery without changing catalogue storage.

## Expanded physical evaluation

Complete board evaluation distinguishes two graphs:

- the **genetic graph**, whose vertices may be macros;
- the **expanded physical graph**, whose vertices are atomic cardboard pieces.

Every public macro port retains its immediate child-port source. `MacroBoardExpander` follows that lineage recursively, applies nested rotations, assigns stable depth-first atomic instance IDs and reconstructs every internal and board-level connection between atomic endpoints. It verifies the reconstructed origins against port-derived layout geometry rather than trusting provenance blindly.

`ExpandedBoardEvaluator` sends this physical genome and layout through the existing constraint and fitness interfaces. Required entrance/exit roles, atomic graph paths, branches, cycles, terrain and cell measurements therefore work through arbitrary macro nesting. Inventory is flattened independently and may be ignored, hard-limited or softly penalised. `ExpandedGraphStructureDescriptor` indexes the generic QD archive with branches and cycles from the expanded graph.

That provenance now enables the next operator and engine layer:

- macro collapse and expansion mutations;
- one-port donor graft crossover on connected subassemblies;
- two-port splice crossover for deliberate cycles;
- attribution of board success back to contributing macros;
- exact duplicate-inventory accounting under arbitrary nesting.

Intrinsic hierarchical branch/cycle metrics remain useful for inexpensive macro-library indexing. Complete board selection must use `ExpandedBoardEvaluation`, whose physical metrics are exact.

## Complete hierarchical runs

`HierarchicalMacroEvolutionEngine` co-evolves the macro catalogue and a complete-board MAP-Elites archive:

1. bootstrap the catalogue from non-role atomic definitions;
2. seed boards by placing an extensible three-or-more-port module between the one-port entrance and exit;
3. select an archived complete board;
4. graft either an atomic piece or an active catalogue macro onto an exposed board port;
5. expand the child to its physical atomic graph;
6. evaluate all board constraints, quality criteria and inventory policy;
7. offer it to physical branch/cycle niches;
8. record catalogue graft and board-admission outcomes.

Complete boards are currently represented as one root macro. This keeps every emitted board connected and makes copying a proven assembly constant at the genetic level, while physical evaluation remains exact. An ordinary graft uses one port pair as its placement anchor and consumes every other unambiguous coincident pair in that placement. It can therefore discover natural cycles without a special splice. Catalogue proposal pairing preserves reusable cyclic modules, and board mutation targets them until a non-zero cycle niche exists. A future two-port splice can add more deliberate loop construction rather than being the only route to cycles.

Entrance and exit are terminal one-port definitions. Connecting them directly would consume both ports and create a sealed two-piece macro that no emitter could extend. Bootstrap therefore uses a smallest available intermediate module with at least three exposed ports and accepts only initial boards that retain a public port. Choosing the smallest module preserves atomic-piece budget for later macro insertion. These invariants are covered by an end-to-end test which also requires evolution beyond bootstrap, catalogue-module admission, reusable cyclic macros and a non-zero board-cycle niche.

Run `games.descent2e.pcg_clean.ui.HierarchicalMacroBoardApplication` to execute the default 2,000-iteration demonstration. Evolution runs off the Swing thread and publishes immutable, coalesced snapshots to two live clickable heatmaps:

- physical board branching × independent cycles;
- macro axial balance × compactness.

Each occupied board niche opens its expanded physical elite. Each occupied macro niche opens the expanded catalogue definition and displays intrinsic quality, atomic size and exposed-port measurements. Painting and clicking do not consume randomness or affect deterministic evolution.
