package eu.koolfreedom.punishment;

import eu.koolfreedom.banning.Ban;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Builder
@Getter
public class Punishment
{
	@Builder.Default private long issued = System.currentTimeMillis();
	@Builder.Default private UUID uuid = null;
	@Builder.Default private String name = null;
	@Builder.Default private String ip = null;
	@Builder.Default private String by = null;
	@Builder.Default private String reason = null;
	@Builder.Default private String type = null;

	public static Punishment fromBan(Ban ban)
	{
		return builder()
				.issued(ban.getId())
				.uuid(ban.getUuid())
				.name(ban.getName())
				.ip(!ban.getIps().isEmpty() ? ban.getIps().getFirst() : null)
				.by(ban.getBy())
				.reason(ban.getReason())
				.type("BAN")
				.build();
	}
}