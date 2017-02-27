package com.massivecraft.factions.cmd;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import com.massivecraft.factions.FactionListComparator;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Perm;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.pager.Msonifier;
import com.massivecraft.massivecore.pager.Pager;

public class CmdFactionsList extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsList()
	{
		// Aliases
		this.addAliases("l", "list");

		// Parameters
		this.addParameter(Parameter.getPage());

		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.LIST));
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		int page = this.readArg();
		final CommandSender sender = this.sender;
		final MPlayer msender = this.msender;
		
		// NOTE: The faction list is quite slow and mostly thread safe.
		// We run it asynchronously to spare the primary server thread.
		
		// Pager Create
		final Pager<Faction> pager = new Pager<>(this, "Faction List", page, new Msonifier<Faction>() {
			@Override
			public Mson toMson(Faction faction, int index)
			{
				if (faction.isNone())
				{
					return Mson.parse("<i>Factionless<i> %d online", FactionColl.get().getNone().getMPlayersWhereOnlineTo(sender).size());
				}
				else
				{
					Mson name = faction.getNameWithTooltip(msender);
					Mson rest = Mson.parse("<i> %d/%d online, %d/%d/%d",
						faction.getMPlayersWhereOnlineTo(sender).size(),
						faction.getMPlayers().size(),
						faction.getLandCount(),
						faction.getPowerRounded(),
						faction.getPowerMaxRounded()
					);

					return Mson.mson(name, rest);
				}
			}
		});
		
		Bukkit.getScheduler().runTaskAsynchronously(Factions.get(), new Runnable()
		{
			@Override
			public void run()
			{
				// Pager Items
				final List<Faction> factions = FactionColl.get().getAll(FactionListComparator.get(sender));
				pager.setItems(factions);
				
				// Pager Message
				pager.message();
			}
		});
	}
	
}
