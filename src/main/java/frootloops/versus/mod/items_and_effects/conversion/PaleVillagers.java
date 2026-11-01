package frootloops.versus.mod.items_and_effects.conversion;

/**
 * Illagers are villagers the paleness took: where it holds, a villager that would turn into a zombie villager (by
 * Conversion, or killed by a zombie) turns into the illager of its trade instead. No evokers: they'd make a totem farm
 * of any village near the deep caves.
 */
public final class PaleVillagers {
    public enum Illager { PILLAGER, VINDICATOR, ILLUSIONER }

    private PaleVillagers() {}

    /** The illager a villager of the trade (its profession's id path, e.g. "armorer") turns into. */
    public static Illager illagerFor(String profession) {
        return switch (profession) {
            // Those who work iron, stone, hide and meat take up the axe
            case "armorer", "weaponsmith", "toolsmith", "butcher", "mason", "leatherworker" -> Illager.VINDICATOR;
            // The learned turn to illusions
            case "cleric", "librarian", "cartographer" -> Illager.ILLUSIONER;
            // Field hands, the jobless and the rest take up the crossbow
            default -> Illager.PILLAGER;
        };
    }
}
