package com.kamefrede.rpsideas.command;

import com.google.common.collect.Lists;
import com.kamefrede.rpsideas.RPSIdeas;
import com.kamefrede.rpsideas.util.helpers.SpellHelpers;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
import vazkii.arl.network.NetworkHandler;
import vazkii.psi.api.PsiAPI;
import vazkii.psi.api.spell.PieceGroup;
import vazkii.psi.common.core.handler.PlayerDataHandler;
import vazkii.psi.common.network.message.MessageDataSync;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author WireSegal
 * Created at 12:52 AM on 12/21/18.
 */
public class CommandPsiLearn extends CommandBase {

    public static final String level0 = "psidust";

    public static List<String> getGroups() {
        List<String> groups = Lists.newArrayList(level0);
        groups.addAll(PsiAPI.groupsForName.keySet());
        return groups;
    }


    public static void lockPieceGroup(PlayerDataHandler.PlayerData data, String group) {
        if (hasGroup(data, group)) {
            if (group.equals(level0)) {
                data.level = 0;
                data.lastSpellGroup = "";
                data.levelPoints = Math.min(0, data.levelPoints - 1);
            } else {
                data.spellGroupsUnlocked.remove(group);
                if (data.lastSpellGroup.equals(group))
                    data.lastSpellGroup = "";
                data.level--;

            }
            data.save();
        }
    }

    public static void unlockPieceGroupFree(PlayerDataHandler.PlayerData data, String group) {
        if (!hasGroup(data, group)) {
            if (group.equals(level0)) {
                if (data.level == 0) {
                    data.level++;
                    data.levelPoints = 1;
                    data.lastSpellGroup = "";
                }
            } else {
                data.spellGroupsUnlocked.add(group);
                data.lastSpellGroup = group;
                data.level++;

            }
            data.save();
        }
    }

    public static void unlockAll(PlayerDataHandler.PlayerData data) {
        for (String group : getGroups()) {
            if (!hasGroup(data, group))
                unlockPieceGroupFree(data, group);
        }
        data.lastSpellGroup = "";
        data.save();
    }

    public static void lockAll(PlayerDataHandler.PlayerData data) {
        List<String> unlocked = Lists.newArrayList(data.spellGroupsUnlocked);
        for (String group : unlocked)
            lockPieceGroup(data, group);
        data.level = 0;
        data.levelPoints = 0;
        data.lastSpellGroup = "";
        data.save();
    }

    public static boolean hasGroup(PlayerDataHandler.PlayerData data, String group) {
        if (data == null)
            return false;

        if (group.equals(level0))
            return data.level > 0;
        return data.isPieceGroupUnlocked(group);
    }

    public static boolean hasGroup(EntityPlayer player, String group) {
        return hasGroup(SpellHelpers.getPlayerData(player), group);
    }

    public static ITextComponent getGroupComponent(String group) {
        if (group.equals(level0)) {
            ITextComponent nameComponent = new TextComponentString("[")
                    .appendSibling(new TextComponentTranslation(RPSIdeas.MODID + ".misc.psidust"))
                    .appendText("]");
            nameComponent.getStyle().setColor(TextFormatting.AQUA);
            nameComponent.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("psimisc.levelDisplay", 0)));
            return nameComponent;
        }

        PieceGroup pieceGroup = PsiAPI.groupsForName.get(group);
        if (pieceGroup == null) {
            ITextComponent errorComponent = new TextComponentString("ERROR");
            errorComponent.getStyle().setColor(TextFormatting.RED);
            return errorComponent;
        }
        ITextComponent nameComponent = new TextComponentString("[")
                .appendSibling(new TextComponentTranslation(pieceGroup.getUnlocalizedName()))
                .appendText("]");

        nameComponent.getStyle().setColor(TextFormatting.AQUA);
        nameComponent.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("psimisc.levelDisplay", pieceGroup.levelRequirement)));
        return nameComponent;
    }

    public String localizationKey() {
        return RPSIdeas.MODID + ".learn";
    }

    @Nonnull
    @Override
    public String getName() {
        return "psi-learn";
    }

    @Nonnull
    @Override
    public String getUsage(@Nonnull ICommandSender sender) {
        return localizationKey() + ".usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    protected void notify(ICommandSender sender, String result, Object... format) {
        CommandBase.notifyCommandListener(sender, this, localizationKey() + "." + result, format);
    }

    protected void wrongUsage(ICommandSender sender) throws WrongUsageException {
        throw new WrongUsageException(getUsage(sender));
    }

    protected void error(String result, Object... format) throws CommandException {
        throw new CommandException(localizationKey() + "." + result, format);
    }

    public void applyPlayerData(EntityPlayer player, PlayerDataHandler.PlayerData data, String group, ICommandSender sender) {
        unlockPieceGroupFree(data, level0);

        if (group.equals(level0))
            notify(sender, "success", player.getDisplayName(), getGroupComponent(group));
        else if (getGroups().contains(group)) {
            PieceGroup pieceGroup = PsiAPI.groupsForName.get(group);
            if (pieceGroup != null && data.isPieceGroupUnlocked(group)) {
                for (String subGroup : pieceGroup.requirements) {
                    if (!data.isPieceGroupUnlocked(subGroup))
                        applyPlayerData(player, data, subGroup, sender);
                }
                unlockPieceGroupFree(data, group);

                notify(sender, "success", player.getDisplayName(), getGroupComponent(group));
            }
        }
    }

    public void applyAll(PlayerDataHandler.PlayerData data, EntityPlayer player, ICommandSender sender) {
        unlockAll(data);
        notify(sender, "success.all", player.getDisplayName());
    }

    public boolean shouldNotApply(EntityPlayer player, String group) {
        return hasGroup(player, group);
    }

    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args) throws CommandException {
        if (args.length == 0 || args.length > 3)
            wrongUsage(sender);
        else {

            Entity target;
            if (args.length == 2)
                target = CommandBase.getEntity(server, sender, args[1]);
            else
                target = sender.getCommandSenderEntity();

            if (!(target instanceof EntityPlayer))
                error("players", target == null ? null : target.getDisplayName());
            else {
                EntityPlayer player = (EntityPlayer) target;
                PlayerDataHandler.PlayerData data = SpellHelpers.getPlayerData(player);
                if (data != null) {
                    if (args[0].equals("*")) {
                        applyAll(data, player, sender);
                        if (player instanceof EntityPlayerMP)
                            NetworkHandler.INSTANCE.sendTo(new MessageDataSync(data), (EntityPlayerMP) player);
                    } else if (!getGroups().contains(args[0]))
                        error("not_a_group", args[0]);
                    else if (shouldNotApply(player, args[0]))
                        error("should_not", player.getDisplayName(), args[0]);
                    else {
                        applyPlayerData(player, data, args[0], sender);
                        if (player instanceof EntityPlayerMP)
                            NetworkHandler.INSTANCE.sendTo(new MessageDataSync(data), (EntityPlayerMP) player);
                    }
                } else
                    error("unknown");
            }
        }
    }

    @Nonnull
    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        switch (args.length) {
            case 1:
                return CommandBase.getListOfStringsMatchingLastWord(args, getGroups());
            case 2:
                return CommandBase.getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
            default:
                return Collections.emptyList();
        }
    }
}