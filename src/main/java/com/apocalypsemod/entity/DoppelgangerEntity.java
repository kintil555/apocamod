package com.apocalypsemod.entity;

import com.apocalypsemod.ApocalypseMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.UUID;

public class DoppelgangerEntity extends HostileEntity {

    private UUID ownerUuid;
    private String ownerName = "???";

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
        // Attack the owner first (the player it resembles)
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8f));
        this.goalSelector.add(4, new LookAroundGoal(this));

        // Target the owner specifically
        this.targetSelector.add(1, new TargetOwnerGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    /**
     * Set the owner of this doppelganger (the player it should attack and resemble).
     */
    public void setOwner(ServerPlayerEntity player) {
        this.ownerUuid = player.getUuid();
        this.ownerName = player.getName().getString();
        this.setCustomName(
                Text.literal("☠ " + ownerName + " ☠")
                        .formatted(Formatting.DARK_RED, Formatting.BOLD)
        );
    }

    /**
     * Copy visual name from player for display.
     */
    public void copyAppearanceFrom(ServerPlayerEntity player) {
        this.ownerName = player.getName().getString();
        this.setCustomName(
                Text.literal("☠ " + ownerName + "'s Shadow ☠")
                        .formatted(Formatting.DARK_RED)
        );
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);

        if (!this.getWorld().isClient()) {
            ServerWorld serverWorld = (ServerWorld) this.getWorld();

            // Check if killed by owner
            if (damageSource.getAttacker() instanceof ServerPlayerEntity killer) {
                if (ownerUuid != null && ownerUuid.equals(killer.getUuid())) {
                    // Owner killed their own doppelganger — TRIGGER APOCALYPSE
                    ApocalypseMod.onDoppelgangerKilledByOwner(killer, serverWorld);
                } else {
                    // Killed by someone else — warn
                    killer.sendMessage(
                            Text.literal("Bayangan ini bukan milikmu... tapi mungkin kamu telah menyelamatkan seseorang.")
                                    .formatted(Formatting.YELLOW),
                            false
                    );
                }
            }

            // Spawn dramatic death effects (particles via lightning)
            net.minecraft.entity.LightningEntity lightning =
                    new net.minecraft.entity.LightningEntity(serverWorld, this.getX(), this.getY(), this.getZ(), true);
            serverWorld.spawnEntity(lightning);
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (ownerUuid != null) {
            nbt.putUuid("OwnerUuid", ownerUuid);
        }
        nbt.putString("OwnerName", ownerName);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.containsUuid("OwnerUuid")) {
            this.ownerUuid = nbt.getUuid("OwnerUuid");
        }
        if (nbt.contains("OwnerName")) {
            this.ownerName = nbt.getString("OwnerName");
        }
    }

    // =========================================================================
    // Inner class: custom goal to target the owner
    // =========================================================================
    private static class TargetOwnerGoal extends ActiveTargetGoal<PlayerEntity> {
        private final DoppelgangerEntity doppelganger;

        public TargetOwnerGoal(DoppelgangerEntity mob) {
            super(mob, PlayerEntity.class, true);
            this.doppelganger = mob;
        }

        @Override
        public boolean canStart() {
            if (doppelganger.ownerUuid == null) return false;
            // Find the owner in the world
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
