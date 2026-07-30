package io.github.chasehuegel.skilling.engine.command;

import io.github.chasehuegel.skilling.engine.SkillDefinition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Resolves placeholders and dispatches level-up commands via the console sender.
 *
 * <p>Supports placeholders: {@code {player}}, {@code {level}}, {@code {skill_id}}, {@code {skill_name}}.
 * Commands are defined in the skill YAML under {@code level_up_commands}.
 */
public final class LevelUpCommandExecutor {

    private LevelUpCommandExecutor() {}

    /**
     * Executes all level-up commands for a skill, with placeholder resolution.
     *
     * @param skill    the skill definition containing level-up commands
     * @param player   the player who leveled up
     * @param newLevel the new level reached
     */
    public static void execute(SkillDefinition skill, Player player, int newLevel) {
        var commands = skill.levelUpCommands();
        if (commands == null || commands.isEmpty()) return;

        String displayName = skill.display() != null && skill.display().name() != null
                ? skill.display().name() : skill.id();

        for (var entry : commands) {
            String resolved = entry.command()
                    .replace("{player}", player.getName())
                    .replace("{level}", String.valueOf(newLevel))
                    .replace("{skill_id}", skill.id())
                    .replace("{skill_name}", displayName);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
        }
    }
}
