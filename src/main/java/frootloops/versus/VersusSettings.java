package frootloops.versus;

import frootloops.versus.mod.Combat;

public abstract class VersusSettings {

    public static void setAimAssist(boolean newValue) {
        Combat.CAN_AIM_ASSIST = newValue;
    }

    public static void setHoldToAttack(boolean newValue) {
        Combat.CAN_HOLD_TO_ATTACK = newValue;
    }

    public static void setSleepOverhaul(boolean newValue) {
        // TODO
    }

    public static void setFoodAndHealthOverhaul(boolean newValue) {
        // TODO
    }

    public static void setFoodInterruptedByDamage(boolean newValue) {
        // TODO
    }

    public static void setBedrockBlockBridging(boolean newValue) {
        // TODO
    }
}
