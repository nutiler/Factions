package com.massivecraft.factions.cmd.type;

import java.util.Collection;

import org.bukkit.command.CommandSender;

import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPermColl;
import com.massivecraft.massivecore.command.type.store.TypeEntity;
import com.massivecraft.massivecore.mson.Mson;

public class TypeMPerm extends TypeEntity<MPerm>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static TypeMPerm i = new TypeMPerm();
	public static TypeMPerm get() { return i; }
	public TypeMPerm()
	{
		super(MPermColl.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public String getName()
	{
		return "faction permission";
	}

	@Override
	public Collection<MPerm> getAll()
	{
		return MPerm.getAll();
	}

	@Override
	public Mson getVisualMsonInner(MPerm mperm, CommandSender sender)
	{
		String visual = this.getVisualColor(mperm, sender) + this.getNameInner(mperm);
		Mson ret = Mson.fromParsedMessage(visual);
		ret = ret.tooltip(mperm.getDesc());
		return ret;
	}

}
