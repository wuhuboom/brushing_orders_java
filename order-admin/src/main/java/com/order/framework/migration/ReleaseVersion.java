package com.order.framework.migration;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A numeric dotted release version such as {@code 1.2.0}.
 */
public final class ReleaseVersion implements Comparable<ReleaseVersion>
{
    private final List<BigInteger> parts;

    private final String value;

    private ReleaseVersion(List<BigInteger> parts, String value)
    {
        this.parts = parts;
        this.value = value;
    }

    public static ReleaseVersion parse(String rawVersion)
    {
        if (rawVersion == null || rawVersion.trim().isEmpty())
        {
            throw new IllegalArgumentException("Application version must not be blank");
        }

        String normalized = rawVersion.trim();
        String[] tokens = normalized.split("\\.", -1);
        List<BigInteger> parsedParts = new ArrayList<>(tokens.length);
        for (String token : tokens)
        {
            if (!token.matches("\\d+"))
            {
                throw new IllegalArgumentException("Invalid numeric application version: " + normalized);
            }
            parsedParts.add(new BigInteger(token));
        }
        return new ReleaseVersion(List.copyOf(parsedParts), normalized);
    }

    @Override
    public int compareTo(ReleaseVersion other)
    {
        Objects.requireNonNull(other, "other");
        int length = Math.max(parts.size(), other.parts.size());
        for (int index = 0; index < length; index++)
        {
            BigInteger left = index < parts.size() ? parts.get(index) : BigInteger.ZERO;
            BigInteger right = index < other.parts.size() ? other.parts.get(index) : BigInteger.ZERO;
            int comparison = left.compareTo(right);
            if (comparison != 0)
            {
                return comparison;
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object value)
    {
        return value instanceof ReleaseVersion other && compareTo(other) == 0;
    }

    @Override
    public int hashCode()
    {
        int lastNonZero = parts.size() - 1;
        while (lastNonZero >= 0 && BigInteger.ZERO.equals(parts.get(lastNonZero)))
        {
            lastNonZero--;
        }
        return parts.subList(0, lastNonZero + 1).hashCode();
    }

    @Override
    public String toString()
    {
        return value;
    }
}
