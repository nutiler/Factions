package com.massivecraft.factions.cmd;

import org.bukkit.ChatColor;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Perm;
import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeMPlayer;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.event.EventFactionsMembershipChange;
import com.massivecraft.factions.event.EventFactionsMembershipChange.MembershipChangeReason;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.util.Txt;

public class CmdFactionsJoin extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsJoin()
	{
		// Aliases
		this.addAliases("join");

		// Parameters
		this.addParameter(TypeFaction.get(), "faction");
		this.addParameter(TypeMPlayer.get(), "player", "you");

		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.JOIN));
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		Faction faction = this.readArg();		

		MPlayer mplayer = this.readArg(msender);
		Faction mplayerFaction = mplayer.getFaction();
		
		boolean samePlayer = mplayer == msender;
		
		// Validate
		if (!samePlayer  && ! Perm.JOIN_OTHERS.has(sender, false))
		{
			msg("<b>You do not have permission to move other players into a faction.");
			return;
		}

		if (faction == mplayerFaction)
		{
			String command = CmdFactions.get().cmdFactionsKick.getCommandLine(mplayer.getName());
			
			// Mson creation
			Mson alreadyMember = Mson.mson(
				Mson.parse(mplayer.describeTo(msender, true)),
				mson((
					samePlayer ? " are" : " is"),
					" already a member of ",
					faction.getNameWithTooltip(msender),
					Mson.DOT
					).color(ChatColor.YELLOW)
			);
			alreadyMember.suggest(command);
			throw new MassiveException().addMessage(alreadyMember);
		}

		if (MConf.get().factionMemberLimit > 0 && faction.getMPlayers().size() >= MConf.get().factionMemberLimit)
		{
			Mson error = mson(
				mson(" !").color(ChatColor.RED),
				" The faction ",
				faction.getNameWithTooltip(msender),
				" is at the limit of",
				String.valueOf(MConf.get().factionMemberLimit),
				" members, so ",
				mplayer.describeTo(msender, false),
				" cannot currently join."
			);
			throw new MassiveException().addMessage(error);
		}

		if (mplayerFaction.isNormal())
		{
			String command = CmdFactions.get().cmdFactionsLeave.getCommandLine(mplayer.getName());
			
			// Mson creation
			Mson leaveFirst = Mson.mson(
				Mson.parse(mplayer.describeTo(msender, true)),
				mson(" must leave " + (samePlayer ? "your" : "their") + " current faction first.").color(ChatColor.RED)
			);
			
			message(leaveFirst.suggest(command).tooltip(Txt.parse("<i>Click to <c>%s<i>.", command)));
			return;
		}

		if (!MConf.get().canLeaveWithNegativePower && mplayer.getPower() < 0)
		{
			msg("<b>%s cannot join a faction with a negative power level.", mplayer.describeTo(msender, true));
			return;
		}

		if( ! (faction.getFlag(MFlag.getFlagOpen()) || faction.isInvited(mplayer) || msender.isOverriding()))
		{
			msg("<i>This faction requires invitation.");
			if (samePlayer)
			{
				faction.msg("%s<i> tried to join your faction.", mplayer.describeTo(faction, true));
			}
			return;
		}

		// Event
		EventFactionsMembershipChange membershipChangeEvent = new EventFactionsMembershipChange(sender, msender, faction, MembershipChangeReason.JOIN);
		membershipChangeEvent.run();
		if (membershipChangeEvent.isCancelled()) return;
		
		// Inform
		if (!samePlayer)
		{
			Mson message = mson(
				msender.describeTo(mplayer, true),
				" moved you into the faction ",
				faction.getNameWithTooltip(mplayer),
				Mson.DOT
			).color(ChatColor.YELLOW);
			mplayer.message(message);
		}
		faction.msg("<i>%s <i>joined <lime>your faction<i>.", mplayer.describeTo(faction, true));

		Mson message = mson(
			mplayer.describeTo(msender, true),
			" successfully joined ",
			faction.getNameWithTooltip(mplayer),
			Mson.DOT
		).color(ChatColor.YELLOW);
		msender.message(message);
		
		// Apply
		mplayer.resetFactionData();
		mplayer.setFaction(faction);
		
		faction.setInvited(mplayer, false);

		// Derplog
		if (MConf.get().logFactionJoin)
		{
			if (samePlayer)
			{
				Factions.get().log(Txt.parse("%s joined the faction %s.", mplayer.getName(), faction.getName()));
			}
			else
			{
				Factions.get().log(Txt.parse("%s moved the player %s into the faction %s.", msender.getName(), mplayer.getName(), faction.getName()));
			}
		}
	}
	
}
