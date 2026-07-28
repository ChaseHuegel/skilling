package io.github.chasehuegel.skilling.web.dto;

import java.util.List;

public record SkillSummaryDTO(
    String id,
    String displayName,
    String icon,
    String color,
    int maxLevel,
    int abilityCount,
    int xpSourceCount,
    List<String> xpSourceTriggers,
    List<String> abilityIds,
    List<String> abilityNames
) {}
