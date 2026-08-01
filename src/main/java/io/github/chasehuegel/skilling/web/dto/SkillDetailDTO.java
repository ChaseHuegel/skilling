package io.github.chasehuegel.skilling.web.dto;

import java.util.List;
import java.util.Map;

public record SkillDetailDTO(
    String id,
    String displayName,
    int maxLevel,
    String icon,
    int customModelData,
    String color,
    String style,
    List<String> lore,
    ProgressionDTO progression,
    List<XpSourceDTO> xpSources,
    List<AbilityDTO> abilities,
    List<LevelUpCommandDTO> levelUpCommands
) {
    public SkillDetailDTO(String id, String displayName, int maxLevel, String icon, int customModelData,
                          String color, String style, ProgressionDTO progression,
                          List<XpSourceDTO> xpSources, List<AbilityDTO> abilities) {
        this(id, displayName, maxLevel, icon, customModelData, color, style, List.of(),
             progression, xpSources, abilities, List.of());
    }

    public record LevelUpCommandDTO(
        String command
    ) {}

    public record ProgressionDTO(
        String curve,
        double baseXp,
        double exponent,
        Double base,
        Double step,
        Double min,
        Double max,
        Double value
    ) {
        public ProgressionDTO(String curve, double baseXp, double exponent) {
            this(curve, baseXp, exponent, null, null, null, null, null);
        }
    }

    public record XpSourceDTO(
        String trigger,
        List<FilterDTO> filters,
        EvaluatorDTO reward
    ) {}

    public record FilterDTO(
        String target,
        String state,
        String tool
    ) {}

    public record AbilityDTO(
        String id,
        String displayName,
        int unlockLevel,
        String trigger,
        AbilityDisplayDTO display,
        RequirementsDTO requirements,
        List<MechanicEntryDTO> mechanics,
        OnFailureDTO onFailure,
        FeedbackDTO feedback
    ) {}

    public record AbilityDisplayDTO(
        List<String> lore
    ) {}

    public record RequirementsDTO(
        EvaluatorDTO cooldown,
        List<String> state,
        List<ItemRequirementDTO> items,
        ExhaustionDTO exhaustion
    ) {
        public RequirementsDTO(double cooldown, List<String> state, List<ItemRequirementDTO> items) {
            this(new EvaluatorDTO("constant", Map.of("value", cooldown)), state, items, null);
        }
    }

    public record ExhaustionDTO(
        double amount,
        double minimum
    ) {}

    public record ItemRequirementDTO(
        String action,
        String tag,
        String slot,
        int amount,
        double itemCooldown
    ) {}

    public record MechanicEntryDTO(
        String type,
        List<FilterDTO> filters,
        Map<String, EvaluatorDTO> parameters
    ) {}

    public record FeedbackDTO(
        boolean actionBar,
        boolean chat,
        String message,
        List<Map<String, Object>> particles,
        List<Map<String, Object>> sounds
    ) {}

    public record OnFailureDTO(
        Map<String, FailureFeedbackDTO> reasons
    ) {}

    public record FailureFeedbackDTO(
        String actionBar,
        List<Map<String, Object>> sounds
    ) {}

    public record EvaluatorDTO(
        String type,
        Map<String, Object> params
    ) {}
}
