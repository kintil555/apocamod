package com.apocalypsemod.entity;

import com.apocalypsemod.ApocalypseMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class DoppelgangerEntity extends HostileEntity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID ownerUuid;
    private String ownerName = "???";

    // Use thenLoop/thenPlay (no LoopType enum needed in GeckoLib 4.7+)
    private static final RawAnimation WALK  = RawAnimation.begin().thenLoop("animation.doppelganger.walk");
    private static final RawAnimation IDLE  = RawAnimation.begin().thenLoop("animation.doppelganger.idle");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.doppelganger.attack");

    public DoppelgangerEntity(EntityType<? extends DoppelgangerEntity> entityType, World world) {
        super(entityType, world);
        this.setCustomNameVisible(true);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
                .add(EntityAttributes.GENERIC_ARMOR, 2.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8f));
        this.goalSelector.add(4, new LookAroundGoal(this));
        this.targetSelector.add(1, new TargetOwnerGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    // ── GeckoLib ──────────────────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (state.isMoving()) return state.setAndContinue(WALK);
            return state.setAndContinue(IDLE);
        }));
        controllers.add(new AnimationController<>(this, "attack", 2, state -> {
            if (this.handSwinging) return state.setAndContinue(ATTACK);
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    // ── Owner ─────────────────────────────────────────────────────────────────

    public void setOwner(ServerPlayerEntity player) {
        this.ownerUuid = player.getUuid();
        this.ownerName = player.getName().getString();
        this.setCustomName(Text.literal("☠ Shadow of " + ownerName + " ☠").formatted(Formatting.DARK_RED));
    }

    public void copyAppearanceFrom(ServerPlayerEntity player) {
        this.ownerName = player.getName().getString();
        this.setCustomName(Text.literal("☠ Shadow of " + ownerName + " ☠").formatted(Formatting.DARK_RED));
    }

    public UUID getOwnerUuid() { return ownerUuid; }

    // ── Death ─────────────────────────────────────────────────────────────────

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        if (!this.getWorld().isClient()) {
            ServerWorld serverWorld = (ServerWorld) this.getWorld();
            if (damageSource.getAttacker() instanceof ServerPlayerEntity killer) {
                if (ownerUuid != null && ownerUuid.equals(killer.getUuid())) {
                    ApocalypseMod.onDoppelgangerKilledByOwner(killer, serverWorld);
                } else {
                    killer.sendMessage(Text.literal("This shadow is not yours... but you may have saved someone.").formatted(Formatting.YELLOW), false);
                }
            }
            net.minecraft.entity.LightningEntity lightning =
                    net.minecraft.entity.EntityType.LIGHTNING_BOLT.create(serverWorld, null,
                    this.getBlockPos(), net.minecraft.entity.SpawnReason.TRIGGERED, false, false);
            if (lightning != null) serverWorld.spawnEntity(lightning);
        }
    }

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (ownerUuid != null) nbt.putUuid("OwnerUuid", ownerUuid);
        nbt.putString("OwnerName", ownerName);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.containsUuid("OwnerUuid")) this.ownerUuid = nbt.getUuid("OwnerUuid");
        if (nbt.contains("OwnerName")) this.ownerName = nbt.getString("OwnerName");
    }

    // ── Target Owner Goal ────────────────────────────────────────────────────

    private static class TargetOwnerGoal extends ActiveTargetGoal<PlayerEntity> {
        private final DoppelgangerEntity doppelganger;

        public TargetOwnerGoal(DoppelgangerEntity mob) {
            super(mob, PlayerEntity.class, true);
            this.doppelganger = mob;
        }

        @Override
        public boolean canStart() {
            if (doppelganger.ownerUuid == null) return false;
            if (doppelganger.getWorld() instanceof ServerWorld serverWorld) {
                ServerPlayerEntity owner = serverWorld.getServer()
                        .getPlayerManager().getPlayer(doppelganger.ownerUuid);
                if (owner != null && !owner.isDead()) {
                    this.targetEntity = owner;
                    return true;
                }
            }
            return false;
        }
    }
}
