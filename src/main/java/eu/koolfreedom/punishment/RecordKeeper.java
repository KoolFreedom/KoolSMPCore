package eu.koolfreedom.punishment;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.util.FLog;

import java.util.*;

public class RecordKeeper
{
	private final Map<Long, Punishment> punishmentMap;

	public RecordKeeper()
	{
		punishmentMap = KoolSMPCore.getInstance().getPlayerRegistry().loadAllPunishments();
		FLog.info("{} punishment(s) loaded.", punishmentMap.size());
	}

	public void recordPunishment(Punishment punishment)
	{
		punishmentMap.put(punishment.getIssued(), punishment);
		KoolSMPCore.getInstance().getPlayerRegistry().savePunishment(punishment);
	}

	public Collection<Punishment> getPunishmentHistory(String target, int limit)
	{
		String normalizedTarget = target.trim();
		return punishmentMap.values().stream()
				.filter(punishment -> (punishment.getName() != null &&
						normalizedTarget.equalsIgnoreCase(punishment.getName())) ||
						(punishment.getUuid() != null && normalizedTarget.equalsIgnoreCase(punishment.getUuid().toString())))
				.sorted(Comparator.comparingLong(Punishment::getIssued).reversed())
				.limit(limit)
				.toList();
	}
}