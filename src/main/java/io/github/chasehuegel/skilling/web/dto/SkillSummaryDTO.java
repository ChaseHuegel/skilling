package io.github.chasehuegel.skilling.web.dto;

public record SkillSummaryDTO(
    String id,
    String displayName,
    String icon,
    String color,
    int maxLevel,
    int abilityCount,
    int xpSourceCount
) {}
