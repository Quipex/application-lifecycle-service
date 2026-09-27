package com.appmanagement.web.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "applications.listing")
public record ApplicationListingProperties(int defaultPageSize) {

    public ApplicationListingProperties {
        if (defaultPageSize < 1) {
            throw new IllegalArgumentException("applications.listing.default-page-size must be at least 1");
        }
    }
}