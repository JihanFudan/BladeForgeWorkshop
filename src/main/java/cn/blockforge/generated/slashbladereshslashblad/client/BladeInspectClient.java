package cn.blockforge.generated.slashbladereshslashblad.client;

import cn.blockforge.generated.slashbladereshslashblad.BladeData;
import cn.blockforge.generated.slashbladereshslashblad.BladeInspectNetwork;
import cn.blockforge.generated.slashbladereshslashblad.GeneratedMod;
import cn.blockforge.generated.slashbladereshslashblad.InspectComboStates;
import com.mojang.blaze3d.platform.InputConstants;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

/**
 * 检视客户端（与网站版同构的极简触发）：
 * 按 G → 主手是拔刀剑就把唯一的带拔刀检视动作发包给服务端，
 * 动画本身完全由重锋连招管线驱动。
 */
public final class BladeInspectClient {
    public static final KeyMapping INSPECT_KEY = new KeyMapping(
            "key." + GeneratedMod.MOD_ID + ".inspect",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G,
            "key." + GeneratedMod.MOD_ID + ".category");

    private BladeInspectClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        // 再次按 G：若正在检视则主动打断（切换式），否则发起检视。
        while (INSPECT_KEY.consumeClick()) {
            if (minecraft.screen != null) continue;
            ItemStack mainHandItem = minecraft.player.getMainHandItem();
            if (mainHandItem.isEmpty()) continue;
            if (!(mainHandItem.getItem() instanceof ItemSlashBlade)) continue;
            if (InspectComboStates.isInspecting(minecraft.player)) {
                BladeInspectNetwork.requestCancelInspect();
            } else if (InspectComboStates.isSkillInUse(mainHandItem)) {
                // 拔刀剑技能释放中不响应检视。
                continue;
            } else {
                BladeInspectNetwork.requestSetCombo(InspectComboStates.inspectId(mainHandItem));
            }
        }

        // 打开任意界面（背包/工作台等）时立即打断检视；取消后 comboSeq 归零，
        // isInspecting 变 false，本分支自然不再触发，无需额外去抖。
        if (minecraft.screen != null && InspectComboStates.isInspecting(minecraft.player)) {
            BladeInspectNetwork.requestCancelInspect();
        }
    }

    @SubscribeEvent
    public static void addTooltip(ItemTooltipEvent event) {
        if (!BladeData.isSlashBlade(event.getItemStack())) return;
        event.getToolTip().add(Component.translatable("tooltip." + GeneratedMod.MOD_ID + ".inspect")
                .withStyle(ChatFormatting.GRAY));
    }
}
