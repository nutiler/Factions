package com.massivecraft.factions.cmd.type;

import java.util.Collection;

import org.bukkit.command.CommandSender;

import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MFlagColl;
import com.massivecraft.massivecore.command.type.store.TypeEntity;
import com.massivecraft.massivecore.mson.Mson;

public class TypeMFlag extends TypeEntity<MFlag>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static TypeMFlag i = new TypeMFlag();
	public static TypeMFlag get() { return i; }
	public TypeMFlag()
	{
		super(MFlagColl.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public String getName()
	{
		return "faction flag";
	}

	@Override
	public Collection<MFlag> getAll()
	{
		return MFlag.getAll();
	}

	@Override
	public Mson getVisualMsonInner(MFlag mflag, CommandSender sender)
	{
		String visual = this.getVisualColor(mflag, sender) + this.getNameInner(mflag);
		Mson ret = Mson.fromParsedMessage(visual);
		ret = ret.tooltip(mflag.getDesc());
		return ret;
	}

}
