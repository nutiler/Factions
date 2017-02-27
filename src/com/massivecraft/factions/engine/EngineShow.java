package com.massivecraft.factions.engine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import com.massivecraft.factions.Const;
import com.massivecraft.factions.PlayerRoleComparator;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.event.EventFactionsChunkChangeType;
import com.massivecraft.factions.event.EventFactionsFactionShowAsync;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.MassiveCore;
import com.massivecraft.massivecore.PriorityLines;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.money.Money;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.util.TimeDiffUtil;
import com.massivecraft.massivecore.util.TimeUnit;
import com.massivecraft.massivecore.util.Txt;

public class EngineShow extends Engine
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static EngineShow i = new EngineShow();
	public static EngineShow get() { return i; }

	// -------------------------------------------- //
	// LISTENER
	// -------------------------------------------- //

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onFactionShow(EventFactionsFactionShowAsync event)
	{

		final CommandSender sender = event.getSender();
		final MPlayer mplayer = event.getMPlayer();
		final Faction faction = event.getFaction();
		final boolean normal = faction.isNormal();
		final Map<String, PriorityLines> idPriorityLiness = event.getIdPriorityLiness();
		String none = MassiveCore.NONE;

		// ID
		showId(idPriorityLiness, mplayer, faction);

		// DESCRIPTION
		showDescription(idPriorityLiness, faction);

		// SECTION: NORMAL
		if (normal)
		{
			// AGE
			showAge(idPriorityLiness, faction);

			// FLAGS
			showFlags(idPriorityLiness, faction);

			// POWER
			showPower(idPriorityLiness, faction);

			// SECTION: ECON
			if (Econ.isEnabled())
			{
				// LANDVALUES
				showLandvalues(idPriorityLiness, faction);

				// BANK
				showBank(idPriorityLiness, faction);
			}
		}

		// FOLLOWERS
		showFollowers(idPriorityLiness, sender, mplayer, faction);
	}

	// -------------------------------------------- //
	// SUB ROUTINE SHOW
	// -------------------------------------------- //

	public static void showId(Map<String, PriorityLines> idPriorityLiness, MPlayer mplayer, Faction faction)
	{
		if (mplayer.isOverriding())
		{
			String id = Const.SHOW_ID_FACTION_ID;
			int priority = Const.SHOW_PRIORITY_FACTION_ID;
			show(idPriorityLiness, id, priority, "ID", faction.getId());
		}
	}

	public static void showDescription(Map<String, PriorityLines> idPriorityLiness, Faction faction)
	{
		String id = Const.SHOW_ID_FACTION_DESCRIPTION;
		int priority = Const.SHOW_PRIORITY_FACTION_DESCRIPTION;
		show(idPriorityLiness, id, priority, "Description", faction.getDescription());
	}

	public static void showAge(Map<String, PriorityLines> idPriorityLiness, Faction faction)
	{
		long ageMillis = faction.getCreatedAtMillis() - System.currentTimeMillis();
		LinkedHashMap<TimeUnit, Long> ageUnitcounts = TimeDiffUtil.limit(TimeDiffUtil.unitcounts(ageMillis, TimeUnit.getAllButMillis()), 3);
		String ageDesc = TimeDiffUtil.formatedVerboose(ageUnitcounts, "<i>");

		String id = Const.SHOW_ID_FACTION_AGE;
		int priority = Const.SHOW_PRIORITY_FACTION_AGE;
		show(idPriorityLiness, id, priority, "Age", ageDesc);
	}

	public static void showFlags(Map<String, PriorityLines> idPriorityLiness, Faction faction)
	{
		// We display all editable and non default ones. The rest we skip.
		List<Mson> flagDescs = new LinkedList<>();
		for (Entry<MFlag, Boolean> entry : faction.getFlags().entrySet())
		{
			final MFlag mflag = entry.getKey();
			if (mflag == null) continue;

			final Boolean value = entry.getValue();
			if (value == null) continue;

			if ( ! mflag.isInteresting(value)) continue;

			Mson flagDesc = Mson.mson(
				mflag.getName()
			)
			.color(value ? ChatColor.GREEN : ChatColor.RED)
			.tooltip(value ? mflag.getDescYes() : mflag.getDescNo());
			flagDescs.add(flagDesc);
		}
		Mson flagsDesc = Mson.parse("<silver><italic>default");
		if ( ! flagDescs.isEmpty())
		{
			flagsDesc = Mson.implode(flagDescs, Mson.parse(" <i>| "));
		}

		String id = Const.SHOW_ID_FACTION_FLAGS;
		int priority = Const.SHOW_PRIORITY_FACTION_FLAGS;
		show(idPriorityLiness, id, priority, "Flags", flagsDesc);
	}

	public static void showPower(Map<String, PriorityLines> idPriorityLiness, Faction faction)
	{
		double powerBoost = faction.getPowerBoost();
		String boost = (powerBoost == 0.0) ? "" : (powerBoost > 0.0 ? " (bonus: " : " (penalty: ") + powerBoost + ")";
		String powerDesc = Txt.parse("%d/%d/%d%s", faction.getLandCount(), faction.getPowerRounded(), faction.getPowerMaxRounded(), boost);

		String id = Const.SHOW_ID_FACTION_POWER;
		int priority = Const.SHOW_PRIORITY_FACTION_POWER;
		show(idPriorityLiness, id, priority, "Land / Power / Maxpower", powerDesc);
	}

	public static void showLandvalues(Map<String, PriorityLines> idPriorityLiness, Faction faction)
	{
		List<Mson> landvalueLines = new LinkedList<>();
		long landCount = faction.getLandCount();
		for (EventFactionsChunkChangeType type : EventFactionsChunkChangeType.values())
		{
			Double money = MConf.get().econChunkCost.get(type);
			if (money == null) continue;
			if (money == 0) continue;
			money *= landCount;

			String word = "Cost";
			if (money <= 0)
			{
				word = "Reward";
				money *= -1;
			}

			String key = Txt.parse("Total Land %s %s", type.toString().toLowerCase(), word);
			String value = Txt.parse("<h>%s", Money.format(money));
			Mson line = show(key, value);
			landvalueLines.add(line);
		}
		idPriorityLiness.put(Const.SHOW_ID_FACTION_LANDVALUES, new PriorityLines(Const.SHOW_PRIORITY_FACTION_LANDVALUES, landvalueLines));

	}

	public static void showBank(Map<String, PriorityLines> idPriorityLiness, Faction faction)
	{
		if (MConf.get().bankEnabled)
		{
			double bank = Money.get(faction);
			String bankDesc = Txt.parse("<h>%s", Money.format(bank, true));
			show(idPriorityLiness, Const.SHOW_ID_FACTION_BANK, Const.SHOW_PRIORITY_FACTION_BANK, "Bank", bankDesc);
		}
	}

	public static void showFollowers(Map<String, PriorityLines> idPriorityLiness, CommandSender sender, MPlayer mplayer, Faction faction)
	{
		boolean normal = faction.isNormal();

		List<Object> followerLines = new MassiveList<>();

		List<Mson> followerNamesOnline = new MassiveList<>();
		List<Mson> followerNamesOffline = new MassiveList<>();

		List<MPlayer> followers = faction.getMPlayers();
		Collections.sort(followers, PlayerRoleComparator.get());
		for (MPlayer follower : followers)
		{
			if (follower.isOnline(sender))
			{
				followerNamesOnline.add(follower.getNameAndTitleWithTooltip(mplayer));
			}
			else if (normal)
			{
				// For the non-faction we skip the offline members since they are far to many (infinite almost)
				followerNamesOffline.add(follower.getNameAndTitleWithTooltip(mplayer));
			}
		}

		Mson headerOnline = Mson.parse("<a>Followers Online (%s):", followerNamesOnline.size());
		followerLines.add(headerOnline);
		if (followerNamesOnline.isEmpty())
		{
			followerLines.add(MassiveCore.NONE);
		}
		else
		{
			followerLines.add(Mson.implode(followerNamesOnline, Mson.parse(" <i>| ")));
		}

		if (normal)
		{
			Mson headerOffline = Mson.parse("<a>Followers Offline (%s):", followerNamesOffline.size());
			followerLines.add(headerOffline);
			if (followerNamesOffline.isEmpty())
			{
				followerLines.add(MassiveCore.NONE);
			}
			else
			{
				followerLines.add(Mson.implode(followerNamesOffline, Mson.parse(" <i>| ")));
			}
		}

		idPriorityLiness.put(Const.SHOW_ID_FACTION_FOLLOWERS, new PriorityLines(Const.SHOW_PRIORITY_FACTION_FOLLOWERS, followerLines));
	}

	// -------------------------------------------- //
	// UTIL SHOW
	// -------------------------------------------- //

	public static Mson show(String key, Object value)
	{
		value = Mson.mson().add(value).color(ChatColor.YELLOW);
		return Mson.mson(
			Mson.mson(key).color(ChatColor.GOLD),
			": ",
			value
		);
	}

	public static PriorityLines show(int priority, String key, Object value)
	{
		return new PriorityLines(priority, show(key, value));
	}

	public static void show(Map<String, PriorityLines> idPriorityLiness, String id, int priority, String key, Object value)
	{
		idPriorityLiness.put(id, show(priority, key, value));
	}

	/*public static List<String> table(List<String> strings, int cols)
	{
		List<String> ret = new MassiveList<>();

		StringBuilder row = new StringBuilder();
		int count = 0;

		Iterator<String> iter = strings.iterator();
		while (iter.hasNext())
		{
			String string = iter.next();
			row.append(string);
			count++;

			if (iter.hasNext() && count != cols)
			{
				row.append(Txt.parse(" <i>| "));
			}
			else
			{
				ret.add(row.toString());
				row = new StringBuilder();
				count = 0;
			}
		}

		return ret;
	}*/

}
