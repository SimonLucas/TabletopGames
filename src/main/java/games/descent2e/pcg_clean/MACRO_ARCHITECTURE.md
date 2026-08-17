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

The initial macro descriptor indexes:

- number of atomic physical pieces;
- number of exposed ports.

`PieceMetrics` also stores traversable size, extent, compactness, directional port counts, hierarchical branch/cycle counts, terrain composition and inventory. Further descriptors can index any combination without recompiling a macro.

`MacroEvolutionEngine` is a deterministic bounded demonstration. It selects atomic or archived definitions, uses `MacroGraftOperator` to enumerate valid one-port attachments, filters by maximum atomic size and inventory policy, and offers one seeded choice to the macro archive. `MacroPieceLibrary` retains only archive elites, so storage is bounded by the descriptor grid.

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

The next executable milestone is a macro-board MAP-Elites engine: seed entrance/exit assemblies, emit one-port grafts and two-port splices from archived macros, evaluate through `ExpandedBoardEvaluator`, and archive through `ExpandedGraphStructureDescriptor`. No further representation change is required for that run.
