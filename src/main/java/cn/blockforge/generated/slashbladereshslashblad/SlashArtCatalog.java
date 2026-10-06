package cn.blockforge.generated.slashbladereshslashblad;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

/** 扫描前置拔刀剑注册的全部可用 SA；次元斩固定排在首位，作为制作台默认值。 */
public final class SlashArtCatalog {
    public static final ResourceLocation DEFAULT =
            ResourceLocation.fromNamespaceAndPath("slashblade", "judgement_cut");

    public record Entry(ResourceLocation id, String translationKey) {}

    private SlashArtCatalog() {}

    public static List<Entry> scan(net.minecraft.core.RegistryAccess access) {
        Registry<SlashArts> registry = access.registryOrThrow(SlashArts.REGISTRY_KEY);
        List<Entry> result = new ArrayList<>();
        for (ResourceLocation key : registry.keySet()) {
            SlashArts art = registry.get(key);
            if (art == null || key.getPath().equals("none")) continue;
            result.add(new Entry(key, art.getDescriptionId()));
        }
        result.sort(Comparator.comparing((Entry entry) -> !entry.id().equals(DEFAULT))
                .thenComparing(entry -> entry.id().toString()));
        return List.copyOf(result);
    }
}
