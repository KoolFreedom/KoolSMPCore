package eu.koolfreedom.punishment;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.util.FLog;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

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

	public Collection<Punishment> getPunishments()
	{
		return Collections.unmodifiableCollection(punishmentMap.values());
	}

	public Punishment getPunishment(long id)
	{
		return punishmentMap.get(id);
	}
}