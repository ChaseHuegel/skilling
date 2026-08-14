package io.github.chasehuegel.skilling.engine.mechanic.impl;

/**
 * Breaks connected blocks of the same material as the broken origin block,
 * expanding only on the XZ plane (four horizontal directions), never along the
 * Y axis. Intended for vein/strip mining where columns above or below the mined
 * layer must stay intact.
 *
 * <p>Reuses {@link ChainBreakMechanic}'s shared BFS, tool-durability cost,
 * {@code chain_limit} cap, and chaining guard. The optional {@code target}
 * parameter restricts the chain to a material or tag reference, exactly as in
 * {@code core:chain_break}.
 *
 * <p><b>YAML key:</b> {@code core:level_break}
 * <br>Params: {@code chain_limit} (max chained blocks broken; the origin does not
 * count against it), {@code target} (optional; a material or tag reference
 * restricting the chain, defaults to the origin block's own material)
 */
public final class LevelBreakMechanic extends ChainBreakMechanic {

    private static final int[][] XZ_PLANE_DIRECTIONS = {
        {1,0,0}, {-1,0,0}, {0,0,1}, {0,0,-1}
    };

    @Override
    protected int[][] directions() {
        return XZ_PLANE_DIRECTIONS;
    }
}
