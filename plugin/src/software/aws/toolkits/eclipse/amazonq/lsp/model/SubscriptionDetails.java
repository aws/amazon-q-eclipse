// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model representing subscription details received from the language server.
 * This contains information about the user's Amazon Q subscription including
 * usage limits, billing cycle, and subscription type.
 *
 * Note: The actual data structure received by Eclipse is flattened compared to
 * what the LSP server sends, so this model matches the flattened structure.
 */
public final class SubscriptionDetails {
    // Flattened subscription info
    @JsonProperty("subscriptionTier")
    private String subscriptionTier;

    // Flattened usage breakdown
    @JsonProperty("queryLimit")
    private int queryLimit;

    @JsonProperty("queryUsage")
    private int queryUsage;

    @JsonProperty("queryOverage")
    private int queryOverage;

    @JsonProperty("subscriptionPeriodReset")
    private String subscriptionPeriodReset;

    // Flattened overage configuration
    @JsonProperty("isOverageEnabled")
    private boolean isOverageEnabled;

    // Getters and setters for flattened structure
    public String getSubscriptionTier() {
        return subscriptionTier;
    }

    public void setSubscriptionTier(final String subscriptionTier) {
        this.subscriptionTier = subscriptionTier;
    }

    public int getQueryLimit() {
        return queryLimit;
    }

    public void setQueryLimit(final int queryLimit) {
        this.queryLimit = queryLimit;
    }

    public int getQueryUsage() {
        return queryUsage;
    }

    public void setQueryUsage(final int queryUsage) {
        this.queryUsage = queryUsage;
    }

    public int getQueryOverage() {
        return queryOverage;
    }

    public void setQueryOverage(final int queryOverage) {
        this.queryOverage = queryOverage;
    }

    public String getSubscriptionPeriodReset() {
        return subscriptionPeriodReset;
    }

    public void setSubscriptionPeriodReset(final String subscriptionPeriodReset) {
        this.subscriptionPeriodReset = subscriptionPeriodReset;
    }

    public boolean isOverageEnabled() {
        return isOverageEnabled;
    }

    public void setOverageEnabled(final boolean isOverageEnabled) {
        this.isOverageEnabled = isOverageEnabled;
    }

    public int getDaysUntilReset() {
        if (subscriptionPeriodReset != null) {
            try {
                final java.time.Instant resetInstant = java.time.Instant.parse(subscriptionPeriodReset);
                final java.time.Instant now = java.time.Instant.now();
                final long daysBetween = java.time.Duration.between(now, resetInstant).toDays();
                return (int) Math.max(0, daysBetween);
            } catch (final Exception e) {
                // If parsing fails, return 0
                return 0;
            }
        }
        return 0;
    }

    // Legacy compatibility methods for existing dialog code

    /**
     * Returns a list of usage limits. Since the language server only sends flattened data,
     * we create a single limit entry from the available data.
     */
    public java.util.List<UsageLimit> getLimits() {
        final java.util.List<UsageLimit> limits = new java.util.ArrayList<>();

        // Create a single usage limit from the flattened data
        final UsageLimit limit = new UsageLimit();
        limit.setType("AGENTIC_REQUEST"); // This is what the language server queries for
        limit.setCurrentUsage(queryUsage);
        limit.setTotalUsageLimit(queryLimit);

        // Calculate percentage if we have valid data
        if (queryLimit > 0) {
            limit.setPercentUsed((double) queryUsage / queryLimit * 100.0);
        } else {
            limit.setPercentUsed(0.0);
        }

        limits.add(limit);
        return limits;
    }

    /**
     * @deprecated Use getSubscriptionTier() instead. This method exists for backward compatibility.
     */
    @Deprecated
    public SubscriptionInfo getSubscriptionInfo() {
        if (subscriptionTier != null) {
            final SubscriptionInfo info = new SubscriptionInfo();
            info.setType(subscriptionTier);
            return info;
        }
        return null;
    }

    /**
     * @deprecated Use direct field access instead. This method exists for backward compatibility.
     */
    @Deprecated
    public UsageBreakdown getUsageBreakdown() {
        final UsageBreakdown breakdown = new UsageBreakdown();
        breakdown.setCurrentUsage(queryUsage);
        breakdown.setCurrentOverages(queryOverage);
        breakdown.setUsageLimit(queryLimit);
        breakdown.setNextDateReset(subscriptionPeriodReset);
        return breakdown;
    }

    /**
     * @deprecated Use isOverageEnabled() instead. This method exists for backward compatibility.
     */
    @Deprecated
    public OverageConfiguration getOverageConfiguration() {
        final OverageConfiguration config = new OverageConfiguration();
        config.setOverageStatus(isOverageEnabled ? "ENABLED" : "DISABLED");
        return config;
    }

    // Keep the nested classes for backward compatibility but mark as deprecated

    /**
     * Represents a usage limit for a specific feature type.
     * @deprecated This nested structure is no longer used. Use direct fields instead.
     */
    @Deprecated
    public static final class UsageLimit {
        private String type;
        private long currentUsage;
        private long totalUsageLimit;
        private double percentUsed;

        public String getType() {
            return type;
        }

        public void setType(final String type) {
            this.type = type;
        }

        public long getCurrentUsage() {
            return currentUsage;
        }

        public void setCurrentUsage(final long currentUsage) {
            this.currentUsage = currentUsage;
        }

        public long getTotalUsageLimit() {
            return totalUsageLimit;
        }

        public void setTotalUsageLimit(final long totalUsageLimit) {
            this.totalUsageLimit = totalUsageLimit;
        }

        public double getPercentUsed() {
            return percentUsed;
        }

        public void setPercentUsed(final double percentUsed) {
            this.percentUsed = percentUsed;
        }
    }

    /**
     * @deprecated This nested structure is no longer used. Use direct fields instead.
     */
    @Deprecated
    public static final class SubscriptionInfo {
        private String type;

        public String getType() {
            return type;
        }

        public void setType(final String type) {
            this.type = type;
        }
    }

    /**
     * @deprecated This nested structure is no longer used. Use direct fields instead.
     */
    @Deprecated
    public static final class UsageBreakdown {
        private int currentUsage;
        private int currentOverages;
        private int usageLimit;
        private String nextDateReset;

        public int getCurrentUsage() {
            return currentUsage;
        }

        public void setCurrentUsage(final int currentUsage) {
            this.currentUsage = currentUsage;
        }

        public int getCurrentOverages() {
            return currentOverages;
        }

        public void setCurrentOverages(final int currentOverages) {
            this.currentOverages = currentOverages;
        }

        public int getUsageLimit() {
            return usageLimit;
        }

        public void setUsageLimit(final int usageLimit) {
            this.usageLimit = usageLimit;
        }

        public String getNextDateReset() {
            return nextDateReset;
        }

        public void setNextDateReset(final String nextDateReset) {
            this.nextDateReset = nextDateReset;
        }
    }

    /**
     * @deprecated This nested structure is no longer used. Use direct fields instead.
     */
    @Deprecated
    public static final class OverageConfiguration {
        private String overageStatus;

        public String getOverageStatus() {
            return overageStatus;
        }

        public void setOverageStatus(final String overageStatus) {
            this.overageStatus = overageStatus;
        }
    }
}
