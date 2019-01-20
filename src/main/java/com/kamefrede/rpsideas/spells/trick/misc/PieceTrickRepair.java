package com.kamefrede.rpsideas.spells.trick.misc;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import vazkii.psi.api.spell.*;
import vazkii.psi.api.spell.piece.PieceTrick;
import vazkii.psi.common.item.armor.ItemPsimetalArmor;
import vazkii.psi.common.item.tool.ItemPsimetalTool;

public class PieceTrickRepair extends PieceTrick {

    public PieceTrickRepair(Spell spell) {
        super(spell);
    }

    @Override
    public void addToMetadata(SpellMetadata meta) throws SpellCompilationException {
        super.addToMetadata(meta);

        meta.addStat(EnumSpellStat.POTENCY, 3);
        meta.addStat(EnumSpellStat.COST, 600);
    }

    @Override
    public Object execute(SpellContext context) throws SpellRuntimeException {
        if (!context.caster.world.isRemote) {
            if (!context.tool.isEmpty()) {
                ItemStack stack = context.tool;
                Item item = stack.getItem();
                if (item.isRepairable() && item.getDamage(stack) > 0 && (item instanceof ItemPsimetalTool || item instanceof ItemPsimetalArmor)) {
                    item.setDamage(stack, item.getDamage(stack) - 1);
                    return null;
                }
            }

        }
        return null;
    }
}
