package com.kamefrede.rpsideas.spells.selector;

import com.kamefrede.rpsideas.spells.base.SpellParams;
import com.kamefrede.rpsideas.spells.base.SpellRuntimeExceptions;
import com.kamefrede.rpsideas.util.helpers.SpellHelpers;
import vazkii.psi.api.spell.*;
import vazkii.psi.api.spell.param.ParamNumber;
import vazkii.psi.api.spell.piece.PieceSelector;
import vazkii.psi.common.core.handler.PlayerDataHandler;

public class PieceSelectorTransmission extends PieceSelector {

    private SpellParam channel;

    public PieceSelectorTransmission(Spell spell) {
        super(spell);
    }

    @Override
    public void initParams() {
        addParam(channel = new ParamNumber(SpellParams.GENERIC_NAME_CHANNEL, SpellParam.RED, true, true));
    }

    @Override
    public void addToMetadata(SpellMetadata meta) throws SpellCompilationException {
        super.addToMetadata(meta);

        Double channelVal = this.<Double>getParamEvaluation(channel);

        if (channelVal != null && channelVal <= 0)
            throw new SpellCompilationException(SpellCompilationException.NON_POSITIVE_VALUE, x, y);
    }

    @Override
    public Object execute(SpellContext context) throws SpellRuntimeException {
        if (context.caster.world.isRemote) return null;
        Double channelVal = this.<Double>getParamValue(context, channel);
        if (channelVal == null) channelVal = 0.0;
        int channel = channelVal.intValue();

        String key = "rpsideas:" + channel;

        PlayerDataHandler.PlayerData data = SpellHelpers.getPlayerData(context.caster);

        if (data != null && data.getCustomData() != null && data.getCustomData().hasKey(key)) {
            return data.getCustomData().getDouble(key);
        }

        throw new SpellRuntimeException(SpellRuntimeExceptions.NO_MESSAGE);
    }

    @Override
    public Class<?> getEvaluationType() {
        return Double.class;
    }
}