package io.github.chasehuegel.skilling.engine.mechanic.impl;

/**
 * Breaks connected blocks of the same material as the broken origin block,
 * expanding only on the XZ plane (four horizontal directions), never along the
 * Y axis. Intended for vein/strip mining where columns above or below the mined
 * layer must stay intact.
 *
 * <p>Reuses {@link ChainBreakMechanic}'s shared BFS, tool-durability cost,
 * {@code chain_limit} cap, and chaining guard.
 *
 * <p><b>YAML key:</b> {@code core:level_break}
 * <br>Params: {@code chain_limit} (max total blocks broken including the origin)
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
