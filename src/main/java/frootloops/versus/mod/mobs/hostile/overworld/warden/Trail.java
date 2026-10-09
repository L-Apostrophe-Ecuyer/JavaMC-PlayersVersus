package frootloops.versus.mod.mobs.hostile.overworld.warden;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Where someone has been over the last ticks, one position a tick, so a warden can head where they were a while ago. */
public final class Trail {
    private final Vec3[] positions;
    private int next;
    private int size;
    @Nullable
    private Object owner;

    public Trail(int ticks) {
        this.positions = new Vec3[ticks];
    }

    /** Records where {@code owner} is this tick; following someone else starts the trail over. */
    public void record(Object owner, Vec3 position) {
        if (owner != this.owner) {
            this.clear();
            this.owner = owner;
        }
        this.positions[this.next] = position;
        this.next = (this.next + 1) % this.positions.length;
        this.size = Math.min(this.size + 1, this.positions.length);
    }

    /** Where they were the longest ago the trail remembers, as many ticks ago as it is long once it's full; null if empty. */
    @Nullable
    public Vec3 oldest() {
        if (this.size == 0) return null;
        return this.size < this.positions.length ? this.positions[0] : this.positions[this.next];
    }

    public void clear() {
        this.owner = null;
        this.next = 0;
        this.size = 0;
    }
}
