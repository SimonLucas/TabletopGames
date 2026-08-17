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

## Important next integration

Board geometry already works with macro instances, but complete board evaluation must next distinguish two graphs:

- the **genetic graph**, whose vertices may be macros;
- the **expanded physical graph**, whose vertices are atomic cardboard pieces.

Cell-based criteria already see flattened macro terrain correctly. Required entrance/exit roles, exact atomic branching/cycles, physical-piece path length and board inventory must use expanded provenance. The next model should therefore assign stable leaf paths to atomic descendants and retain, for each exposed macro port, the descendant atomic port from which it originated. Expansion can then reconstruct a `BoardGenome` of atomic instances without geometric inference.

That provenance also enables:

- macro collapse and expansion mutations;
- one-port donor graft crossover on connected subassemblies;
- two-port splice crossover for deliberate cycles;
- attribution of board success back to contributing macros;
- exact duplicate-inventory accounting under arbitrary nesting.

Until expanded provenance is added, hierarchical branch/cycle metrics are suitable for macro-library exploration but should not replace flattened physical board metrics.
