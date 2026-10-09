package frootloops.versus.mod.mobs.hostile.overworld.crawling;

import net.minecraft.core.Direction;
import org.joml.Vector3f;

/** Implemented on spiders by SpiderMixin: how they cling to surfaces ({@link SurfaceGrip}). */
public interface SurfaceCrawler {
    /** The face the spider clings by, synced to clients. */
    Direction playersVersus$gripFace();

    /** Server: the sides of the spider's box that touched a block at the end of its last tick. */
    int playersVersus$touching();

    /** Clients: the way out of the spider's surface, smoothed over a few ticks, at the partial tick. */
    Vector3f playersVersus$surfaceNormal(float partialTick);

    /** Clients: the way the spider faces along its surface, smoothed over a few ticks, at the partial tick. */
    Vector3f playersVersus$surfaceForward(float partialTick);
}
