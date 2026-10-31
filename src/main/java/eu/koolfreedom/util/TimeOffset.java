package eu.koolfreedom.util;

import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public enum TimeOffset
{
	MILLISECOND(1L, "ms"),
	SECOND(1000L, "s"),
	MINUTE(60L * 1000, "m"),
	HOUR(60L * MINUTE.multiplier, "h"),
	DAY(24L * HOUR.multiplier, "d"),
	WEEK(7L * DAY.multiplier, "w"),
	MONTH(30L * DAY.multiplier, "mo"),
	YEAR(365L * DAY.multiplier, "y"),
	CENTURY(100L * YEAR.multiplier, "c"),
	MILLENNIUM(10L * CENTURY.multiplier, "mil");

	private static final Pattern pattern = Pattern.compile("([0-9]+)([a-z]{1,3})");

	private final long multiplier;
	private final String delimiter;

	TimeOffset(long multiplier, String delimiter) 
	{
		this.multiplier = multiplier;
		this.delimiter = delimiter;
	}

	public long multiply(long amount) 
	{
		return amount * multiplier;
	}

	public static long getOffset(String str) 
	{
		Matcher matcher = pattern.matcher(str);
		AtomicLong amount = new AtomicLong(0);

		matcher.results().forEach(result -> {
			int amt = Integer.parseInt(result.group(1));
			String delimiter = result.group(2);

			Optional<TimeOffset> ouch = Arrays.stream(values())
					.filter(offset -> Objects.nonNull(offset.delimiter))
					.filter(offset -> offset.delimiter.equalsIgnoreCase(delimiter))
					.findAny();

			ouch.ifPresent(offset -> amount.addAndGet(offset.multiply(amt)));
		});

		return amount.get();
	}
}
