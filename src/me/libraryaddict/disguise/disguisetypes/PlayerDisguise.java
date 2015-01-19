package me.libraryaddict.disguise.disguisetypes;

import com.comphenix.protocol.wrappers.WrappedGameProfile;
import me.libraryaddict.disguise.LibsDisguises;
import me.libraryaddict.disguise.disguisetypes.watchers.PlayerWatcher;
import me.libraryaddict.disguise.utilities.DisguiseUtilities;
import me.libraryaddict.disguise.utilities.LibsProfileLookup;
import me.libraryaddict.disguise.utilities.ReflectionManager;
import net.minecraft.server.v1_8_R1.*;
import org.apache.commons.lang.Validate;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

public class PlayerDisguise extends TargetedDisguise {

    private LibsProfileLookup currentLookup;
    private WrappedGameProfile gameProfile;
    private String playerName;
    private String skinToUse;

    public PlayerDisguise(String name) {
        if (name.length() > 16)
            name = name.substring(0, 16);
        playerName = name;
        createDisguise(DisguiseType.PLAYER);
    }

    @Deprecated
    public PlayerDisguise(String name, boolean replaceSounds) {
        this(name);
        this.setReplaceSounds(replaceSounds);
    }

    public PlayerDisguise(String name, String skinToUse) {
        this(name);
        setSkin(skinToUse);
    }

    public PlayerDisguise(WrappedGameProfile gameProfile) {
        this(gameProfile.getName());
        this.gameProfile = gameProfile;
    }

    public PlayerDisguise addPlayer(Player player) {
        return (PlayerDisguise) super.addPlayer(player);
    }

    public PlayerDisguise addPlayer(String playername) {
        return (PlayerDisguise) super.addPlayer(playername);
    }

    @Override
    public PlayerDisguise clone() {
        PlayerDisguise disguise = new PlayerDisguise(getName());
        if (disguise.currentLookup == null && disguise.gameProfile != null) {
            disguise.skinToUse = getSkin();
            disguise.gameProfile = gameProfile;
        } else {
            disguise.setSkin(getSkin());
        }
        disguise.setReplaceSounds(isSoundsReplaced());
        disguise.setViewSelfDisguise(isSelfDisguiseVisible());
        disguise.setHearSelfDisguise(isSelfDisguiseSoundsReplaced());
        disguise.setHideArmorFromSelf(isHidingArmorFromSelf());
        disguise.setHideHeldItemFromSelf(isHidingHeldItemFromSelf());
        disguise.setVelocitySent(isVelocitySent());
        disguise.setModifyBoundingBox(isModifyBoundingBox());
        disguise.setWatcher(getWatcher().clone(disguise));
        return disguise;
    }

    public void setGameProfile(WrappedGameProfile gameProfile) {
        this.gameProfile = ReflectionManager.getGameProfileWithThisSkin(null, gameProfile.getName(), gameProfile);
    }

    public WrappedGameProfile getGameProfile() {
        if (gameProfile == null) {
            if (getSkin() != null) {
                gameProfile = ReflectionManager.getGameProfile(null, getName());
            } else {
                gameProfile = ReflectionManager.getGameProfileWithThisSkin(null, getName(),
                        DisguiseUtilities.getProfileFromMojang(this));
            }
        }
        return gameProfile;
    }

    public String getName() {
        return playerName;
    }

    public String getSkin() {
        return skinToUse;
    }

    @Override
    public PlayerWatcher getWatcher() {
        return (PlayerWatcher) super.getWatcher();
    }

    @Override
    public boolean isPlayerDisguise() {
        return true;
    }

    public PlayerDisguise removePlayer(Player player) {
        return (PlayerDisguise) super.removePlayer(player);
    }

    public PlayerDisguise removePlayer(String playername) {
        return (PlayerDisguise) super.removePlayer(playername);
    }

    public PlayerDisguise setDisguiseTarget(TargetType newTargetType) {
        return (PlayerDisguise) super.setDisguiseTarget(newTargetType);
    }

    @Override
    public PlayerDisguise setEntity(Entity entity) {
        return (PlayerDisguise) super.setEntity(entity);
    }

    public PlayerDisguise setHearSelfDisguise(boolean hearSelfDisguise) {
        return (PlayerDisguise) super.setHearSelfDisguise(hearSelfDisguise);
    }

    public PlayerDisguise setHideArmorFromSelf(boolean hideArmor) {
        return (PlayerDisguise) super.setHideArmorFromSelf(hideArmor);
    }

    public PlayerDisguise setHideHeldItemFromSelf(boolean hideHeldItem) {
        return (PlayerDisguise) super.setHideHeldItemFromSelf(hideHeldItem);
    }

    public PlayerDisguise setKeepDisguiseOnEntityDespawn(boolean keepDisguise) {
        return (PlayerDisguise) super.setKeepDisguiseOnEntityDespawn(keepDisguise);
    }

    public PlayerDisguise setKeepDisguiseOnPlayerDeath(boolean keepDisguise) {
        return (PlayerDisguise) super.setKeepDisguiseOnPlayerDeath(keepDisguise);
    }

    public PlayerDisguise setKeepDisguiseOnPlayerLogout(boolean keepDisguise) {
        return (PlayerDisguise) super.setKeepDisguiseOnPlayerLogout(keepDisguise);
    }

    public PlayerDisguise setModifyBoundingBox(boolean modifyBox) {
        return (PlayerDisguise) super.setModifyBoundingBox(modifyBox);
    }

    public PlayerDisguise setReplaceSounds(boolean areSoundsReplaced) {
        return (PlayerDisguise) super.setReplaceSounds(areSoundsReplaced);
    }

    public PlayerDisguise setSkin(String skinToUse) {
        this.skinToUse = skinToUse;
        if (skinToUse == null) {
            this.currentLookup = null;
            this.gameProfile = null;
        } else {
            if (skinToUse.length() > 16) {
                this.skinToUse = skinToUse.substring(0, 16);
            }
            currentLookup = new LibsProfileLookup() {

                @Override
                public void onLookup(WrappedGameProfile gameProfile) {
                    if (currentLookup == this && gameProfile != null) {
                        setSkin(gameProfile);
                        if (!gameProfile.getProperties().isEmpty() && DisguiseUtilities.isDisguiseInUse(PlayerDisguise.this)) {
                            DisguiseUtilities.refreshTrackers(PlayerDisguise.this);
                        }
                        currentLookup = null;
                    }
                }
            };
            WrappedGameProfile gameProfile = DisguiseUtilities.getProfileFromMojang(this.skinToUse, currentLookup);
            if (gameProfile != null) {
                setSkin(gameProfile);
            }
        }
        return this;
    }

    /**
     * Set the GameProfile, without tampering.
     *
     * @param gameProfile GameProfile
     * @return
     */
    public PlayerDisguise setSkin(WrappedGameProfile gameProfile) {
        if (gameProfile == null) {
            this.gameProfile = null;
            this.skinToUse = null;
            return this;
        }

        Validate.notEmpty(gameProfile.getName(), "Name must be set");
        this.skinToUse = gameProfile.getName();
        this.gameProfile = ReflectionManager.getGameProfileWithThisSkin(null, getName(), gameProfile);
        return this;
    }

    public PlayerDisguise setVelocitySent(boolean sendVelocity) {
        return (PlayerDisguise) super.setVelocitySent(sendVelocity);
    }

    public PlayerDisguise setViewSelfDisguise(boolean viewSelfDisguise) {
        return (PlayerDisguise) super.setViewSelfDisguise(viewSelfDisguise);
    }

    public PlayerDisguise setWatcher(FlagWatcher newWatcher) {
        return (PlayerDisguise) super.setWatcher(newWatcher);
    }

    public PlayerDisguise silentlyAddPlayer(String playername) {
        return (PlayerDisguise) super.silentlyAddPlayer(playername);
    }

    public PlayerDisguise silentlyRemovePlayer(String playername) {
        return (PlayerDisguise) super.silentlyRemovePlayer(playername);
    }

    public boolean removeDisguise() {
        boolean boo = super.removeDisguise();
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    EntityTrackerEntry entry = (EntityTrackerEntry) ReflectionManager.getEntityTrackerEntry(PlayerDisguise.this.getEntity());
                    for (Player player : LibsDisguises.instance().getServer().getOnlinePlayers()) {
                        updatePlayer(entry, ((CraftPlayer) player).getHandle());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.runTaskLater(LibsDisguises.instance(), 2L);
        return boo;
    }

    private static void updatePlayer(EntityTrackerEntry entry, EntityPlayer ep) {
        if (ep != entry.tracker) {
            ep.playerConnection.sendPacket(new PacketPlayOutEntityMetadata(entry.tracker.getId(), entry.tracker.getDataWatcher(), true));
            if ((entry.tracker instanceof EntityLiving)) {
                AttributeMapServer attributemapserver = (AttributeMapServer) ((EntityLiving) entry.tracker).getAttributeMap();
                Collection collection = attributemapserver.c();
                if (entry.tracker.getId() == ep.getId()) {
                    ((EntityPlayer) entry.tracker).getBukkitEntity().injectScaledMaxHealth(collection, false);
                }
                if (!collection.isEmpty()) {
                    ep.playerConnection.sendPacket(new PacketPlayOutUpdateAttributes(entry.tracker.getId(), collection));
                }
            }
            entry.j = entry.tracker.motX;
            entry.k = entry.tracker.motY;
            entry.l = entry.tracker.motZ;
            if (entry.tracker.vehicle != null) {
                ep.playerConnection.sendPacket(new PacketPlayOutAttachEntity(0, entry.tracker, entry.tracker.vehicle));
            }
            if (((entry.tracker instanceof EntityInsentient)) && (((EntityInsentient) entry.tracker).getLeashHolder() != null)) {
                ep.playerConnection.sendPacket(new PacketPlayOutAttachEntity(1, entry.tracker, ((EntityInsentient) entry.tracker).getLeashHolder()));
            }
            if ((entry.tracker instanceof EntityLiving)) {
                for (int i = 0; i < 5; i++) {
                    ItemStack itemstack = ((EntityLiving) entry.tracker).getEquipment(i);
                    if (itemstack != null) {
                        ep.playerConnection.sendPacket(new PacketPlayOutEntityEquipment(entry.tracker.getId(), i, itemstack));
                    }
                }
            }
            if ((entry.tracker instanceof EntityHuman)) {
                EntityHuman entityhuman = (EntityHuman) entry.tracker;
                if (entityhuman.isSleeping()) {
                    ep.playerConnection.sendPacket(new PacketPlayOutBed(entityhuman, new BlockPosition(entry.tracker)));
                }
            }
            entry.i = MathHelper.d(entry.tracker.getHeadRotation() * 256.0F / 360.0F);
            entry.broadcast(new PacketPlayOutEntityHeadRotation(entry.tracker, (byte) entry.i));
            if ((entry.tracker instanceof EntityLiving)) {
                EntityLiving entityliving = (EntityLiving) entry.tracker;
                for (Object o : entityliving.getEffects()) {
                    MobEffect mobeffect = (MobEffect) o;
                    ep.playerConnection.sendPacket(new PacketPlayOutEntityEffect(entry.tracker.getId(), mobeffect));
                }
            }
        }
    }

}