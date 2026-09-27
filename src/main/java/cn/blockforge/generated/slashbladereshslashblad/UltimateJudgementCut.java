package cn.blockforge.generated.slashbladereshslashblad;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** 阎魔专属 SSA“究极次元斩”的服务端状态机、区域冻结和伤害结算。 */
public final class UltimateJudgementCut {
    public static final double RADIUS = 17.0;
    /** 前 0.5 秒完成上斩与残影飞行，随后用 2.5 秒缓慢纳刀。 */
    public static final int DURATION_TICKS = 60;
    public static final int SLASH_PHASE_TICKS = 10;
    /** 纳刀完全结束的一刻才结算伤害并触发空间破碎。 */
    public static final int IMPACT_TICK = DURATION_TICKS;
    private static final int COOLDOWN_TICKS = DURATION_TICKS;
    private static final ResourceLocation UPPERSLASH_COMBO = ResourceLocation.fromNamespaceAndPath("slashblade", "upperslash");
    private static final ResourceLocation UPPERSLASH_END_COMBO = ResourceLocation.fromNamespaceAndPath("slashblade", "upperslash_end");
    private static final ResourceLocation JUDGEMENT_CUT_SHEATH_COMBO = ResourceLocation.fromNamespaceAndPath("slashblade", "judgement_cut_sheath");
    private static final int REQUIRED_PROUD_SOUL = 5000;
    private static final int PROUD_SOUL_COST = 2000;
    private static final ResourceLocation SHEATH_COMBO = ResourceLocation.fromNamespaceAndPath("slashblade", "judgement_cut_sheath");
    private static final ResourceLocation STANDBY_COMBO = ResourceLocation.fromNamespaceAndPath("slashblade", "standby");
    private static final Map<UUID, Session> SESSIONS = new LinkedHashMap<>();
    private static final Map<UUID, Long> COOLDOWNS = new LinkedHashMap<>();
    private static boolean resolvingUltimateDamage;

    private UltimateJudgementCut() {
    }

    /** 检视动画与 SSA 互斥，由服务端统一裁决。 */
    public static boolean isCasterActive(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    /** 检视打断 SSA：立即撤掉咏唱演出（不结算伤害），刀回到待机。 */
    public static boolean interrupt(ServerPlayer caster) {
        Session session = SESSIONS.remove(caster.getUUID());
        if (session == null) return false;
        setCombo(caster.getMainHandItem(), STANDBY_COMBO, caster.level().getGameTime());
        UltimateJudgementCutNetwork.broadcastCancel(caster);
        return true;
    }

    /** 客户端请求只表达按键意图；资格、冷却、刀和结算全部由服务器复查。 */
    public static void tryStart(ServerPlayer player) {
        ItemStack blade = player.getMainHandItem();
        if (!player.isAlive() || player.isSpectator() || !isYamato(blade)) return;
        if (InspectComboStates.isInspecting(player)) return;
        long now = player.server.getTickCount();
        if (COOLDOWNS.getOrDefault(player.getUUID(), 0L) > now || SESSIONS.containsKey(player.getUUID())) return;

        CompoundTag state = SlashBladeBridge.read(blade);
        int proudSoul = state.getInt("proudSoul");
        if (proudSoul < REQUIRED_PROUD_SOUL) {
            player.displayClientMessage(Component.literal("耀魂不足：究极次元斩至少需要 5000 耀魂"), true);
            return;
        }
        state.putInt("proudSoul", proudSoul - PROUD_SOUL_COST);
        SlashBladeBridge.write(blade, state);

        Session session = new Session(player, now);
        SESSIONS.put(player.getUUID(), session);
        COOLDOWNS.put(player.getUUID(), now + COOLDOWN_TICKS);
        // 使用重锋原有上斩，客户端将同一动作压缩到前十 tick。
        setCombo(player.getMainHandItem(), UPPERSLASH_COMBO, player.level().getGameTime());
        SlashBladeBridge.broadcastMotion(player, UPPERSLASH_COMBO, player.level().getGameTime());
        player.displayClientMessage(Component.literal("SSA：究极次元斩"), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.3f, 0.65f);
        session.captureTargets(player);
        UltimateJudgementCutNetwork.broadcastStart(session);
    }

    public static boolean isYamato(ItemStack stack) {
        if (!BladeData.isSlashBlade(stack)) return false;
        CompoundTag state = SlashBladeBridge.read(stack);
        if (state.getBoolean("isBroken") || state.getBoolean("isSealed")) return false;
        String identity = (state.getString("translationKey") + " "
                + state.getString("ModelName") + " " + state.getString("TextureName"))
                .toLowerCase(Locale.ROOT);
        return identity.contains("yamato");
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SESSIONS.clear();
        COOLDOWNS.clear();
        resolvingUltimateDamage = false;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        Iterator<Session> iterator = SESSIONS.values().iterator();
        while (iterator.hasNext()) {
            Session session = iterator.next();
            if (!session.tick(server)) iterator.remove();
        }
    }

    /** 取消生物本轮 tick，连 AI、寻路、攻击冷却和主动技能都暂停。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void freezeEntityTick(EntityTickEvent.Pre event) {
        Entity candidate = event.getEntity();
        if (!candidate.level().isClientSide && candidate instanceof Projectile) {
            for (Session session : SESSIONS.values()) {
                if (!candidate.level().dimension().equals(session.dimension)) continue;
                Vec3 start = candidate.position();
                Vec3 velocity = candidate.getDeltaMovement();
                double fraction = velocity.lengthSqr() < 1.0E-8 ? 0.0
                        : Math.max(0.0, Math.min(1.0, session.center.subtract(start).dot(velocity) / velocity.lengthSqr()));
                if (start.add(velocity.scale(fraction)).distanceToSqr(session.center) <= (RADIUS + 1.0) * (RADIUS + 1.0)) {
                    session.repel(candidate);
                    event.setCanceled(true);
                    return;
                }
            }
        }
        FrozenState frozen = frozenState(event.getEntity());
        if (frozen == null) return;
        Entity entity = event.getEntity();
        entity.setDeltaMovement(Vec3.ZERO);
        entity.moveTo(frozen.position.x, frozen.position.y, frozen.position.z, frozen.yRot, frozen.xRot);
        if (entity instanceof LivingEntity living) living.stopUsingItem();
        // 玩家要继续处理网络和状态同步；位置与所有主动操作仍由本类逐 Tick 锁死。
        if (!(entity instanceof Player)) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void blockFrozenAttack(AttackEntityEvent event) {
        if (!resolvingUltimateDamage && actionLocked(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void blockFrozenItemUse(PlayerInteractEvent.RightClickItem event) {
        if (!actionLocked(event.getEntity())) return;
        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void blockFrozenBlockUse(PlayerInteractEvent.RightClickBlock event) {
        if (!actionLocked(event.getEntity())) return;
        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void blockFrozenBreaking(BlockEvent.BreakEvent event) {
        if (actionLocked(event.getPlayer())) event.setCanceled(true);
    }

    /** 冻结目标造成的近战、弹射物和模组伤害都在伤害入口被挡下。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void blockFrozenDamage(LivingIncomingDamageEvent event) {
        if (resolvingUltimateDamage) return;
        Entity attacker = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();
        Session protection = SESSIONS.get(event.getEntity().getUUID());
        // 无实体的射线也需要保护；只放行由贴身生物直接造成的近战及环境伤害。
        if (protection != null && (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)
                || event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)
                || direct instanceof Projectile
                || (direct != null && !(direct instanceof LivingEntity))
                || (attacker != null && (attacker != direct
                    || attacker.position().distanceToSqr(event.getEntity().position()) > 16.0)))) {
            event.setCanceled(true);
            if (direct != null && !(direct instanceof LivingEntity)) protection.repel(direct);
            return;
        }
        if ((attacker != null && actionLocked(attacker)) || (direct != null && actionLocked(direct))) {
            event.setCanceled(true);
        }
    }

    private static boolean actionLocked(Entity entity) {
        if (entity.level().isClientSide) return false;
        return frozenState(entity) != null || (entity instanceof ServerPlayer player && SESSIONS.containsKey(player.getUUID()));
    }

    private static FrozenState frozenState(Entity entity) {
        if (entity.level().isClientSide) return null;
        for (Session session : SESSIONS.values()) {
            FrozenState state = session.frozen.get(entity.getUUID());
            if (state != null) return state;
        }
        return null;
    }

    private static void setCombo(ItemStack blade, ResourceLocation combo, long gameTime) {
        if (!BladeData.isSlashBlade(blade)) return;
        CompoundTag state = SlashBladeBridge.read(blade);
        state.putString("currentCombo", combo.toString());
        state.putLong("lastActionTime", gameTime);
        SlashBladeBridge.write(blade, state);
    }

    /**
     * 直接走服务端原版玩家伤害入口，避免 SlashBlade 的 AttackHelper 把被冻结目标再次过滤。
     * 五段各自结算，既能触发受击反馈，也确保范围内每个有效目标都会实际扣血。
     */
    private static void attackFivefold(ServerPlayer caster, LivingEntity target) {
        float damagePerHit = (float) Math.max(2.0, caster.getAttributeValue(Attributes.ATTACK_DAMAGE));
        resolvingUltimateDamage = true;
        try {
            for (int hit = 0; hit < 5 && target.isAlive(); hit++) {
                target.invulnerableTime = 0;
                target.hurt(caster.damageSources().playerAttack(caster), damagePerHit);
                target.invulnerableTime = 0;
            }
        } finally {
            resolvingUltimateDamage = false;
        }
    }

    private static boolean canAffect(ServerPlayer caster, LivingEntity target) {
        if (target == caster || !target.isAlive()) return false;
        if (target instanceof ServerPlayer other) {
            if (caster.isAlliedTo(other)) return false;
            MinecraftServer server = caster.getServer();
            return server != null && server.isPvpAllowed() && caster.canHarmPlayer(other);
        }
        // 究极次元斩只锁敌对生物：动物、村民、宠物与盔甲架等一律不进入领域，也不结算伤害。
        return isHostile(caster, target);
    }

    /** 怪物阵营（含模组怪物）算敌对；中立生物只有在正盯着施法者时才当作敌人。 */
    private static boolean isHostile(ServerPlayer caster, LivingEntity target) {
        if (target.getType().getCategory() == MobCategory.MONSTER) return true;
        if (target instanceof Monster) return true;
        return target instanceof Mob mob && mob.getTarget() == caster;
    }

    public static final class Session {
        private final UUID casterId;
        private final ResourceKey<Level> dimension;
        private final Vec3 center;
        private final float yaw;
        private final float pitch;
        private final long startTick;
        private final Map<UUID, FrozenState> frozen = new LinkedHashMap<>();
        private boolean impacted;

        private Session(ServerPlayer caster, long startTick) {
            this.casterId = caster.getUUID();
            this.dimension = caster.level().dimension();
            this.center = caster.position().add(0.0, caster.getBbHeight() * 0.5, 0.0);
            this.yaw = caster.getYRot();
            this.pitch = caster.getXRot();
            this.startTick = startTick;
        }

        public UUID casterId() {
            return casterId;
        }

        public ResourceKey<Level> dimension() {
            return dimension;
        }

        public Vec3 center() {
            return center;
        }

        public float yaw() {
            return yaw;
        }

        public long startTick() {
            return startTick;
        }

        private boolean tick(MinecraftServer server) {
            ServerPlayer caster = server.getPlayerList().getPlayer(casterId);
            ServerLevel level = server.getLevel(dimension);
            long age = server.getTickCount() - startTick;
            if (caster == null || level == null || !caster.isAlive() || age > DURATION_TICKS) {
                if (caster != null) setCombo(caster.getMainHandItem(), STANDBY_COMBO, caster.level().getGameTime());
                frozen.clear();
                return false;
            }

            caster.setDeltaMovement(Vec3.ZERO);
            caster.moveTo(center.x, center.y - caster.getBbHeight() * 0.5, center.z, yaw, pitch);
            caster.yBodyRot = caster.yBodyRotO = caster.yHeadRot = caster.yHeadRotO = yaw;
            caster.stopUsingItem();
            captureTargets(caster);
            restoreFrozenPositions(level);

            // 上斩与其收刀使用连续的原始动作帧，纳刀期间不再切换其他剑技。
            if (age == SLASH_PHASE_TICKS) {
                setCombo(caster.getMainHandItem(), UPPERSLASH_END_COMBO, caster.level().getGameTime());
                SlashBladeBridge.broadcastMotion(caster, UPPERSLASH_END_COMBO, caster.level().getGameTime());
                level.playSound(null, caster.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                        SoundSource.PLAYERS, 0.75f, 1.8f);
            }
            // 慢纳刀占后半段。持续刷新起始时间，阻止重锋在中途自动回到待机，从而保证模型完整收刀。
            if (age >= SLASH_PHASE_TICKS && age < DURATION_TICKS) {
                setCombo(caster.getMainHandItem(), UPPERSLASH_END_COMBO, caster.level().getGameTime());
            }
            if (!impacted && age >= IMPACT_TICK) {
                impacted = true;
                impact(caster, level);
            }
            if (age == DURATION_TICKS) setCombo(caster.getMainHandItem(), STANDBY_COMBO, caster.level().getGameTime());
            return true;
        }

        private void repel(Entity projectile) {
            Vec3 outward = projectile.position().subtract(center).normalize();
            if (outward.lengthSqr() < 1.0E-8) outward = new Vec3(0, 1, 0);
            Vec3 destination = center.add(outward.scale(Math.max(RADIUS + 2.0,
                    projectile.position().subtract(center).length() + 1.0)));
            projectile.moveTo(destination.x, destination.y, destination.z);
            projectile.setDeltaMovement(outward.scale(Math.max(1.5, projectile.getDeltaMovement().length())));
            projectile.hasImpulse = true;
            projectile.hurtMarked = true;
        }

        private void captureTargets(ServerPlayer caster) {
            if (!(caster.level() instanceof ServerLevel level)) return;
            AABB area = new AABB(center, center).inflate(RADIUS);
            double radiusSquared = RADIUS * RADIUS;
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                    target -> canAffect(caster, target)
                            && target.getBoundingBox().getCenter().distanceToSqr(center) <= radiusSquared);
            for (LivingEntity target : targets) {
                frozen.computeIfAbsent(target.getUUID(), ignored -> new FrozenState(
                        target.position(), target.getYRot(), target.getXRot()));
            }
        }

        private void restoreFrozenPositions(ServerLevel level) {
            Iterator<Map.Entry<UUID, FrozenState>> iterator = frozen.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, FrozenState> entry = iterator.next();
                Entity entity = level.getEntity(entry.getKey());
                if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                    iterator.remove();
                    continue;
                }
                FrozenState state = entry.getValue();
                living.setDeltaMovement(Vec3.ZERO);
                living.moveTo(state.position.x, state.position.y, state.position.z, state.yRot, state.xRot);
                living.stopUsingItem();
            }
        }

        private void impact(ServerPlayer caster, ServerLevel level) {
            List<LivingEntity> targets = new ArrayList<>();
            double radiusSquared = RADIUS * RADIUS;
            for (UUID id : frozen.keySet()) {
                Entity entity = level.getEntity(id);
                if (entity instanceof LivingEntity living && canAffect(caster, living)
                        && living.getBoundingBox().getCenter().distanceToSqr(center) <= radiusSquared) {
                    targets.add(living);
                }
            }
            for (LivingEntity target : targets) attackFivefold(caster, target);
            if (targets.isEmpty()) {
                caster.displayClientMessage(Component.literal("SSA：范围内没有敌对生物，友军未被波及"), true);
            }

            level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z,
                    4, 2.5, 2.5, 2.5, 0.0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
                    180, RADIUS * 0.55, RADIUS * 0.55, RADIUS * 0.55, 0.12);
            level.sendParticles(ParticleTypes.PORTAL, center.x, center.y, center.z,
                    220, RADIUS * 0.5, RADIUS * 0.5, RADIUS * 0.5, 0.7);
            level.playSound(null, caster.blockPosition(), SoundEvents.GLASS_BREAK,
                    SoundSource.PLAYERS, 2.0f, 0.55f);
            level.playSound(null, caster.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                    SoundSource.PLAYERS, 1.2f, 1.35f);
        }
    }

    private record FrozenState(Vec3 position, float yRot, float xRot) {
    }
}
