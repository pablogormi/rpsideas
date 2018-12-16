package com.kamefrede.rpsideas.spells.operator.block;

import com.kamefrede.rpsideas.spells.base.SpellParams;
import com.kamefrede.rpsideas.spells.base.SpellRuntimeExceptions;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import vazkii.psi.api.internal.Vector3;
import vazkii.psi.api.spell.Spell;
import vazkii.psi.api.spell.SpellContext;
import vazkii.psi.api.spell.SpellParam;
import vazkii.psi.api.spell.SpellRuntimeException;
import vazkii.psi.api.spell.param.ParamVector;
import vazkii.psi.api.spell.piece.PieceOperator;

public class PieceOperatorGetBlockComparatorStrength extends PieceOperator {

    private SpellParam axisParam;
    private SpellParam target;

    public PieceOperatorGetBlockComparatorStrength(Spell spell) {
        super(spell);
    }

    @Override
    public void initParams() {
        addParam(target = new ParamVector(SpellParam.GENERIC_NAME_TARGET, SpellParam.RED, false, false));
        addParam(axisParam = new ParamVector(SpellParams.GENERIC_NAME_VECTOR, SpellParam.BLUE, false, false));
    }

    @Override
    public Object execute(SpellContext context) throws SpellRuntimeException {
        Vector3 ax = this.<Vector3>getParamValue(context, axisParam);
        EnumFacing whichWay;
        Vector3 vec = this.<Vector3>getParamValue(context, target);
        if (vec == null || vec.isZero()) throw new SpellRuntimeException(SpellRuntimeException.NULL_TARGET);

        if (ax == null || ax.isZero()) {
            throw new SpellRuntimeException(SpellRuntimeException.NULL_VECTOR);
        } else if (!ax.isAxial()) {
            throw new SpellRuntimeException(SpellRuntimeExceptions.NON_AXIAL_VECTOR);
        } else {
            whichWay = EnumFacing.getFacingFromVector((float) ax.x, (float) ax.y, (float) ax.z);
        }

        BlockPos pos = new BlockPos(vec.x, vec.y, vec.z);
        IBlockState state = Blocks.POWERED_COMPARATOR.getDefaultState()
                .withProperty(BlockHorizontal.FACING, whichWay.getOpposite());

        return Blocks.POWERED_COMPARATOR.calculateInputStrength(context.caster.world, pos.offset(whichWay), state);
    }

    @Override
    public Class<Double> getEvaluationType() {
        return Double.class;
    }
}