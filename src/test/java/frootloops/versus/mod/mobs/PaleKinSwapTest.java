package frootloops.versus.mod.mobs;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pale kin's swap in the natural spawner (NaturalSpawnerPaleKinMixin), as the owner chose it: no object made a check
 * (no @WrapOperation, whose operation is an object a call) and never fatal (each injection optional: a mod that rewrites
 * the spawner leaves the pale kin unswapped rather than the game failing to start). Read from its class file: a mixin
 * can't be loaded as a class. What it does is SpawnRulesGameTest's (the real spawner).
 */
class PaleKinSwapTest {

    private static final String WRAP = "Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;";
    private static final String MODIFY = "Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;";

    private static List<AnnotationNode> annotations(MethodNode method) {
        List<AnnotationNode> all = new ArrayList<>();
        if (method.visibleAnnotations != null) all.addAll(method.visibleAnnotations);
        if (method.invisibleAnnotations != null) all.addAll(method.invisibleAnnotations);
        return all;
    }

    @Test
    void theSwapMakesNoObjectAndIsNeverFatal() throws IOException {
        ClassNode mixin = new ClassNode();
        try (InputStream bytes = PaleKinSwapTest.class.getClassLoader().getResourceAsStream("frootloops/versus/mixin/mobs/NaturalSpawnerPaleKinMixin.class")) {
            assertNotNull(bytes, "no NaturalSpawnerPaleKinMixin class");
            new ClassReader(bytes).accept(mixin, ClassReader.SKIP_CODE);
        }
        int swaps = 0;
        for (MethodNode method : mixin.methods) {
            for (AnnotationNode annotation : annotations(method)) {
                assertTrue(!annotation.desc.equals(WRAP), method.name + " still wraps the call");
                if (!annotation.desc.equals(MODIFY)) continue;
                swaps++;
                Object require = null;
                for (int i = 0; annotation.values != null && i < annotation.values.size(); i += 2) {
                    if (annotation.values.get(i).equals("require")) require = annotation.values.get(i + 1);
                }
                assertEquals(0, require, method.name + "'s injection is one the game needs");
            }
        }
        assertEquals(2, swaps, "the swap isn't in both places the spawner reads the kind");
    }
}
